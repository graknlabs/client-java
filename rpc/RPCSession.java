/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package grakn.client.rpc;

import com.google.protobuf.ByteString;
import grakn.client.Grakn;
import grakn.client.GraknClient;
import grakn.client.GraknOptions;
import grakn.common.collection.Pair;
import grakn.protocol.GraknGrpc;
import grakn.protocol.SessionProto;
import grakn.protocol.cluster.ClusterGrpc;
import grakn.protocol.cluster.DatabaseProto;
import io.grpc.Channel;

import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static grakn.client.GraknProtoBuilder.options;
import static grakn.common.collection.Collections.pair;

public class RPCSession {
    public static class Core implements Grakn.Session {

        private final Channel channel;
        private final String database;
        private final Type type;
        private final ByteString sessionId;
        private final AtomicBoolean isOpen;
        private final Timer pulse;
        private final GraknGrpc.GraknBlockingStub blockingGrpcStub;

        public Core(GraknClient.Core client, String database, Type type, GraknOptions options) {
            this.channel = client.channel();
            this.database = database;
            this.type = type;
            blockingGrpcStub = GraknGrpc.newBlockingStub(channel);
            final SessionProto.Session.Open.Req openReq = SessionProto.Session.Open.Req.newBuilder()
                    .setDatabase(database).setType(sessionType(type)).setOptions(options(options)).build();

            sessionId = blockingGrpcStub.sessionOpen(openReq).getSessionId();
            pulse = new Timer();
            isOpen = new AtomicBoolean(true);
            pulse.scheduleAtFixedRate(this.new PulseTask(), 0, 5000);
        }

        @Override
        public Grakn.Transaction transaction(Grakn.Transaction.Type type) {
            return transaction(type, new GraknOptions());
        }

        @Override
        public Grakn.Transaction transaction(Grakn.Transaction.Type type, GraknOptions options) {
            return new RPCTransaction(this, sessionId, type, options);
        }

        @Override
        public Type type() {
            return type;
        }

        @Override
        public boolean isOpen() {
            return isOpen.get();
        }

        @Override
        public void close() {
            if (isOpen.compareAndSet(true, false)) {
                pulse.cancel();
                blockingGrpcStub.sessionClose(SessionProto.Session.Close.Req.newBuilder().setSessionId(sessionId).build());
            }
        }

        @Override
        public String database() {
            return database;
        }

        Channel channel() { return channel; }


        public void pulse() {
            final SessionProto.Session.Pulse.Res res = blockingGrpcStub.sessionPulse(
                    SessionProto.Session.Pulse.Req.newBuilder().setSessionId(sessionId).build());

            if (!res.getAlive()) {
                isOpen.set(false);
                pulse.cancel();
            }
        }

        private static SessionProto.Session.Type sessionType(Type type) {
            switch (type) {
                case DATA:
                    return SessionProto.Session.Type.DATA;
                case SCHEMA:
                    return SessionProto.Session.Type.SCHEMA;
                default:
                    return SessionProto.Session.Type.UNRECOGNIZED;
            }
        }

        private class PulseTask extends TimerTask {
            @Override
            public void run() {
                if (!isOpen()) return;
                pulse();
            }
        }
    }

    public static class Cluster implements Grakn.Session {
        private final String database;
        private final Type type;
        private final ReplicaSet replicas;
        private final ConcurrentMap<Replica.Id, RPCSession.Core> sessions;
        private final AtomicBoolean isOpen;

        public Cluster(GraknClient.Cluster client, String database, Grakn.Session.Type type, GraknOptions options) {
            this.database = database;
            this.type = type;
            this.replicas = new ReplicaSet(database, client);
            sessions = client.clients().entrySet().stream()
                    .map(entry -> pair(new Replica.Id(entry.getKey(), database), entry.getValue().session(database, type, options)))
                    .collect(Collectors.toConcurrentMap(Pair::first, Pair::second));
            isOpen = new AtomicBoolean(true);
        }

        @Override
        public Grakn.Transaction transaction(Grakn.Transaction.Type type) {
            return transaction(type, new GraknOptions());
        }

        @Override
        public Grakn.Transaction transaction(Grakn.Transaction.Type type, GraknOptions options) {
            Replica.Id replica = replicas.leader();
            return sessions.get(replica).transaction(type, options);
        }

        @Override
        public Type type() {
            return type;
        }

        @Override
        public boolean isOpen() {
            return isOpen.get();
        }

        @Override
        public void close() {
            sessions.values().forEach(Core::close);
        }

        @Override
        public String database() {
            return database;
        }

        public static class ReplicaSet {
            private final String name;
            private final GraknClient.Cluster client;
            private final ClusterGrpc.ClusterBlockingStub clusterBlockingStub;
            private final ConcurrentMap<Cluster.Replica.Id, Cluster.Replica> replicaMap;

            public ReplicaSet(String name, GraknClient.Cluster client) {
                this.name = name;
                this.client = client;
                clusterBlockingStub = ClusterGrpc.newBlockingStub(randomReplicaClient().channel());
                replicaMap = createReplicaMap();
            }

            private GraknClient.Core randomReplicaClient() {
                return client.clients().get(randomReplica().address());
            }

            private Cluster.Replica.Id randomReplica() {
                String randomAddress = this.client.clients().keySet().iterator().next();
                return new Cluster.Replica.Id(randomAddress, name);
            }

            private Cluster.Replica.Id leader() {
                Map.Entry<Cluster.Replica.Id, Cluster.Replica> initial = replicaMap.entrySet().iterator().next();
                return replicaMap.entrySet().stream()
                        .filter(entry -> entry.getValue().role == Cluster.Replica.Role.LEADER)
                        .reduce(initial, (acc, e) -> e.getValue().term > acc.getValue().term ? e : acc)
                        .getKey();
            }

            private ConcurrentMap<Cluster.Replica.Id, Cluster.Replica> createReplicaMap() {
                ConcurrentMap<Cluster.Replica.Id, Cluster.Replica> replicaMap = new ConcurrentHashMap<>();
                Cluster.Replica.Id replica = randomReplica();
                DatabaseProto.Database.Replica.Res res = clusterBlockingStub
                        .databaseReplicaInfo(DatabaseProto.Database.Replica.Req.newBuilder().setDatabase(replica.database()).build());
                for (DatabaseProto.Database.Replica.Res.Info info: res.getInfosList()) {
                    replicaMap.put(new Cluster.Replica.Id(info.getAddress(), info.getDatabase()), new Cluster.Replica(info.getTerm(), info.getRole()));
                }
                return replicaMap;
            }
        }

        public static class Replica {
            enum Role { LEADER, CANDIDATE, FOLLOWER }

            private final Role role;
            private final long term;

            Replica(long term, String role) {
                this(term, parseRole(role));
            }

            Replica(long term, Role role) {
                this.term = term;
                this.role = role;
            }

            public long term() {
                return term;
            }

            public Role role() {
                return role;
            }

            private static Role parseRole(String role) {
                Role role_;
                switch (role) {
                    case "leader":
                        role_ = Role.LEADER;
                        break;
                    case "candidate":
                        role_ = Role.CANDIDATE;
                        break;
                    case "follower":
                        role_ = Role.FOLLOWER;
                        break;
                    default:
                        throw new IllegalArgumentException("invalid role: '" + role + "'");
                }
                return role_;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Replica replica = (Replica) o;
                return term == replica.term &&
                        role == replica.role;
            }

            @Override
            public int hashCode() {
                return Objects.hash(role, term);
            }

            public static class Id {
                private final String address;
                private final String database;

                Id(String address, String database) {
                    this.address = address;
                    this.database = database;
                }

                public String address() {
                    return address;
                }

                public String database() {
                    return database;
                }

                @Override
                public boolean equals(Object o) {
                    if (this == o) return true;
                    if (o == null || getClass() != o.getClass()) return false;
                    Id id = (Id) o;
                    return Objects.equals(address, id.address) &&
                            Objects.equals(database, id.database);
                }

                @Override
                public int hashCode() {
                    return Objects.hash(address, database);
                }
            }
        }
    }
}
