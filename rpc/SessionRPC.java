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
import grakn.client.GraknClient;
import grakn.client.GraknOptions;
import grakn.client.common.exception.GraknClientException;
import grakn.client.common.proto.OptionsProtoBuilder;
import grakn.protocol.GraknGrpc;
import grakn.protocol.SessionProto;
import io.grpc.StatusRuntimeException;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class SessionRPC implements GraknClient.Session {
    private final ClientRPC client;
    private final DatabaseRPC database;
    private final Type type;
    private final GraknOptions options;
    private final ByteString sessionId;
    private final AtomicBoolean isOpen;
    private final Timer pulse;
    private final GraknGrpc.GraknBlockingStub blockingGrpcStub;

    public SessionRPC(ClientRPC client, String database, Type type, GraknOptions options) {
        try {
            client.reconnect();
            this.client = client;
            this.type = type;
            this.options = options;
            blockingGrpcStub = GraknGrpc.newBlockingStub(client.channel());
            this.database = new DatabaseRPC(client.databases(), database);
            SessionProto.Session.Open.Req openReq = SessionProto.Session.Open.Req.newBuilder()
                    .setDatabase(database).setType(sessionType(type)).setOptions(OptionsProtoBuilder.options(options)).build();

            sessionId = blockingGrpcStub.sessionOpen(openReq).getSessionId();
            pulse = new Timer();
            isOpen = new AtomicBoolean(true);
            pulse.scheduleAtFixedRate(this.new PulseTask(), 0, 5000);
        } catch (StatusRuntimeException e) {
            throw GraknClientException.of(e);
        }
    }

    @Override
    public GraknClient.Transaction transaction(GraknClient.Transaction.Type type) {
        return transaction(type, GraknOptions.core());
    }

    @Override
    public GraknClient.Transaction transaction(GraknClient.Transaction.Type type, GraknOptions options) {
        return new TransactionRPC(this, sessionId, type, options);
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public GraknOptions options() {
        return options;
    }

    @Override
    public boolean isOpen() {
        return isOpen.get();
    }

    @Override
    public void close() {
        if (isOpen.compareAndSet(true, false)) {
            client.removeSession(this);
            pulse.cancel();
            try {
                client.reconnect();
                blockingGrpcStub.sessionClose(SessionProto.Session.Close.Req.newBuilder().setSessionId(sessionId).build());
            } catch (StatusRuntimeException e) {
                // Most likely the session is already closed or the server is no longer running.
            }
        }
    }

    @Override
    public DatabaseRPC database() {
        return database;
    }

    ClientRPC client() { return client; }

    ByteString id() { return sessionId; }

    public void pulse() {
        boolean alive;
        try {
            alive = blockingGrpcStub.sessionPulse(
                    SessionProto.Session.Pulse.Req.newBuilder().setSessionId(sessionId).build()).getAlive();
        } catch (StatusRuntimeException exception) {
            alive = false;
        }
        if (!alive) {
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
