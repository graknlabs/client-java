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

package grakn.client;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableList;
import grabl.tracing.client.GrablTracingThreadStatic.ThreadTrace;
import grakn.client.answer.Answer;
import grakn.client.answer.AnswerGroup;
import grakn.client.answer.ConceptList;
import grakn.client.answer.ConceptMap;
import grakn.client.answer.ConceptSet;
import grakn.client.answer.ConceptSetMeasure;
import grakn.client.answer.Explanation;
import grakn.client.answer.Numeric;
import grakn.client.answer.Void;
import grakn.client.concept.Concept;
import grakn.client.concept.ValueType;
import grakn.client.concept.GraknConceptException;
import grakn.client.concept.type.Role;
import grakn.client.concept.Rule;
import grakn.client.concept.SchemaConcept;
import grakn.client.concept.ConceptId;
import grakn.client.concept.Label;
import grakn.client.concept.thing.Attribute;
import grakn.client.concept.type.AttributeType;
import grakn.client.concept.type.EntityType;
import grakn.client.concept.type.MetaType;
import grakn.client.concept.type.RelationType;
import grakn.client.exception.GraknClientException;
import grakn.client.rpc.RequestBuilder;
import grakn.client.rpc.ResponseReader;
import grakn.client.rpc.Transceiver;
import grakn.protocol.keyspace.KeyspaceProto;
import grakn.protocol.keyspace.KeyspaceServiceGrpc;
import grakn.protocol.keyspace.KeyspaceServiceGrpc.KeyspaceServiceBlockingStub;
import grakn.protocol.session.AnswerProto;
import grakn.protocol.session.ConceptProto;
import grakn.protocol.session.SessionProto;
import grakn.protocol.session.SessionServiceGrpc;
import graql.lang.Graql;
import graql.lang.pattern.Pattern;
import graql.lang.query.GraqlCompute;
import graql.lang.query.GraqlDefine;
import graql.lang.query.GraqlDelete;
import graql.lang.query.GraqlGet;
import graql.lang.query.GraqlInsert;
import graql.lang.query.GraqlQuery;
import graql.lang.query.GraqlUndefine;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static grabl.tracing.client.GrablTracingThreadStatic.traceOnThread;
import static grakn.client.rpc.Transceiver.Response.Type.OK;

/**
 * Entry-point which communicates with a running Grakn server using gRPC.
 */
public class GraknClient implements AutoCloseable {

    public static final String DEFAULT_URI = "localhost:48555";

    private ManagedChannel channel;
    private String username;
    private String password;
    private Keyspaces keyspaces;

    public GraknClient() {
        this(DEFAULT_URI);
    }

    public GraknClient(String address) {
        this(address, null, null);
    }

    public GraknClient(String address, String username, String password) {
        channel = ManagedChannelBuilder.forTarget(address)
                .usePlaintext().build();
        this.username = username;
        this.password = password;
        keyspaces = new Keyspaces(channel, this.username, this.password);
    }

    public GraknClient overrideChannel(ManagedChannel channel) {
        this.channel = channel;
        return this;
    }


    public void close() {
        channel.shutdown();
        try {
            channel.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public boolean isOpen() {
        return !channel.isShutdown() || !channel.isTerminated();
    }

    public Session session(String keyspace) {
        return new Session(channel, username, password, keyspace);
    }

    public Keyspaces keyspaces() {
        return keyspaces;
    }

    /**
     * @see Transaction
     * @see GraknClient
     */
    public static class Session implements AutoCloseable {

        protected ManagedChannel channel;
        private String username; // TODO: Do we need to save this? It's not used.
        private String password; // TODO: Do we need to save this? It's not used.
        protected String keyspace;
        protected SessionServiceGrpc.SessionServiceBlockingStub sessionStub;
        protected String sessionId;
        protected boolean isOpen;

        private Session(ManagedChannel channel, String username, String password, String keyspace) {
            this.username = username;
            this.password = password;
            this.keyspace = keyspace;
            this.channel = channel;
            this.sessionStub = SessionServiceGrpc.newBlockingStub(channel);

            SessionProto.Session.Open.Req.Builder open = RequestBuilder.Session.open(keyspace).newBuilderForType();
            if (username != null) {
                open = open.setUsername(username);
            }
            if (password != null) {
                open = open.setPassword(password);
            }
            open = open.setKeyspace(keyspace);

            SessionProto.Session.Open.Res response = sessionStub.open(open.build());
            sessionId = response.getSessionId();
            isOpen = true;
        }

        public GraknClient.Transaction.Builder transaction() {
            return new Transaction.Builder(channel, this, sessionId);
        }

        public GraknClient.Transaction transaction(Transaction.Type type) {
            return new Transaction(channel, this, sessionId, type);
        }

        public boolean isOpen() {
            return isOpen;
        }

        public void close() {
            if (!isOpen) return;
            sessionStub.close(RequestBuilder.Session.close(sessionId));
            isOpen = false;
        }

        public Keyspace keyspace() {
            return Keyspace.of(keyspace);
        }
    }

    public static class Transaction implements AutoCloseable {
        private final Session session;
        private final Type type;
        private final Transceiver transceiver;

        private int currentIteratorId = 1;

        public static class Builder {

            private ManagedChannel channel;
            private GraknClient.Session session;
            private String sessionId;

            public Builder(ManagedChannel channel, GraknClient.Session session, String sessionId) {
                this.channel = channel;
                this.session = session;
                this.sessionId = sessionId;
            }

            public GraknClient.Transaction read() {
                return new GraknClient.Transaction(channel, session, sessionId, Transaction.Type.READ);
            }

            public GraknClient.Transaction write() {
                return new GraknClient.Transaction(channel, session, sessionId, Transaction.Type.WRITE);
            }
        }

        public enum Type {
            READ(0),  //Read only transaction where mutations to the graph are prohibited
            WRITE(1); //Write transaction where the graph can be mutated

            private final int type;

            Type(int type) {
                this.type = type;
            }

            public int id() {
                return type;
            }

            @Override
            public String toString() {
                return this.name();
            }

            public static Type of(int value) {
                for (Type t : Type.values()) {
                    if (t.type == value) return t;
                }
                return null;
            }

            public static Type of(String value) {
                for (Type t : Type.values()) {
                    if (t.name().equalsIgnoreCase(value)) return t;
                }
                return null;
            }
        }

        private Transaction(ManagedChannel channel, Session session, String sessionId, Type type) {
            try (ThreadTrace trace = traceOnThread(type == Type.WRITE ? "tx.write" : "tx.read")) {
                this.transceiver = Transceiver.create(SessionServiceGrpc.newStub(channel));
                this.session = session;
                this.type = type;
                sendAndReceiveOrThrow(RequestBuilder.Transaction.open(sessionId, type));
            }
        }

        public Type type() {
            return type;
        }

        public GraknClient.Session session() {
            return session;
        }

        public Keyspace keyspace() {
            return session.keyspace();
        }

        public List<ConceptMap> execute(GraqlDefine query) {
            try (ThreadTrace trace = traceOnThread("tx.execute.define")) {
                return executeInternal(query, true);
            }
        }

        public List<ConceptMap> execute(GraqlUndefine query) {
            try (ThreadTrace trace = traceOnThread("tx.execute.undefine")) {
                return executeInternal(query, true);
            }
        }

        public List<ConceptMap> execute(GraqlInsert query, boolean infer) {
            try (ThreadTrace trace = traceOnThread("tx.execute.insert")) {
                return executeInternal(query, infer);
            }
        }
        public List<ConceptMap> execute(GraqlInsert query) {
            return execute(query, true);
        }

        public List<Void> execute(GraqlDelete query, boolean infer) {
            try (ThreadTrace trace = traceOnThread("tx.execute.delete")) {
                return executeInternal(query, infer);
            }
        }
        public List<Void> execute(GraqlDelete query) {
            return execute(query, true);
        }

        public List<ConceptMap> execute(GraqlGet query, boolean infer) {
            try (ThreadTrace trace = traceOnThread("tx.execute.get")) {
                return executeInternal(query, infer);
            }
        }
        public List<ConceptMap> execute(GraqlGet query) {
            return execute(query, true);
        }

        public Stream<ConceptMap> stream(GraqlDefine query) {
            try (ThreadTrace trace = traceOnThread("tx.stream.define")) {
                return streamInternal(query, true);
            }
        }

        public Stream<ConceptMap> stream(GraqlUndefine query) {
            try (ThreadTrace trace = traceOnThread("tx.stream.undefine")) {
                return streamInternal(query, true);
            }
        }

        public Stream<ConceptMap> stream(GraqlInsert query, boolean infer) {
            try (ThreadTrace trace = traceOnThread("tx.stream.insert")) {
                return streamInternal(query, infer);
            }
        }
        public Stream<ConceptMap> stream(GraqlInsert query) {
            return stream(query, true);
        }

        public Stream<Void> stream(GraqlDelete query, boolean infer) {
            try (ThreadTrace trace = traceOnThread("tx.stream.delete")) {
                return streamInternal(query, infer);
            }
        }
        public Stream<Void> stream(GraqlDelete query) {
            return stream(query, true);
        }

        public Stream<ConceptMap> stream(GraqlGet query, boolean infer) {
            try (ThreadTrace trace = traceOnThread("tx.stream.get")) {
                return streamInternal(query, infer);
            }
        }
        public Stream<ConceptMap> stream(GraqlGet query) {
            return stream(query, true);
        }

        // Aggregate Query

        public List<Numeric> execute(GraqlGet.Aggregate query) {
            return execute(query, true);
        }

        public List<Numeric> execute(GraqlGet.Aggregate query, boolean infer) {
            try (ThreadTrace trace = traceOnThread("tx.execute.get.aggregate")) {
                return executeInternal(query, infer);
            }
        }

        public Stream<Numeric> stream(GraqlGet.Aggregate query) {
            return stream(query, true);
        }

        public Stream<Numeric> stream(GraqlGet.Aggregate query, boolean infer) {
            try (ThreadTrace trace = traceOnThread("tx.stream.get.aggregate")) {
                return streamInternal(query, infer);
            }
        }

        // Group Query

        public List<AnswerGroup<ConceptMap>> execute(GraqlGet.Group query) {
            return execute(query, true);
        }

        public List<AnswerGroup<ConceptMap>> execute(GraqlGet.Group query, boolean infer) {
            try (ThreadTrace trace = traceOnThread("tx.execute.get.group")) {
                return executeInternal(query, infer);
            }
        }

        public Stream<AnswerGroup<ConceptMap>> stream(GraqlGet.Group query) {
            return stream(query, true);
        }

        public Stream<AnswerGroup<ConceptMap>> stream(GraqlGet.Group query, boolean infer) {
            try (ThreadTrace trace = traceOnThread("tx.stream.get.group")) {
                return streamInternal(query, infer);
            }
        }


        // Group Aggregate Query

        public List<AnswerGroup<Numeric>> execute(GraqlGet.Group.Aggregate query) {
            return execute(query, true);
        }

        public List<AnswerGroup<Numeric>> execute(GraqlGet.Group.Aggregate query, boolean infer) {
            try (ThreadTrace trace = traceOnThread("tx.execute.get.group.aggregate")) {
                return executeInternal(query, infer);
            }
        }


        public Stream<AnswerGroup<Numeric>> stream(GraqlGet.Group.Aggregate query) {
            return stream(query, true);
        }

        public Stream<AnswerGroup<Numeric>> stream(GraqlGet.Group.Aggregate query, boolean infer) {
            try (ThreadTrace trace = traceOnThread("tx.stream.get.group.aggregate")) {
                return streamInternal(query, infer);
            }
        }

        // Compute Query

        public List<Numeric> execute(GraqlCompute.Statistics query) {
            try (ThreadTrace trace = traceOnThread("tx.execute.compute.statistics")) {
                return executeInternal(query, false);
            }
        }

        public Stream<Numeric> stream(GraqlCompute.Statistics query) {
            try (ThreadTrace trace = traceOnThread("tx.stream.compute.statistics")) {
                return streamInternal(query, false);
            }
        }

        public List<ConceptList> execute(GraqlCompute.Path query) {
            try (ThreadTrace trace = traceOnThread("tx.execute.compute.path")) {
                return executeInternal(query, false);
            }
        }

        public Stream<ConceptList> stream(GraqlCompute.Path query) {
            try (ThreadTrace trace = traceOnThread("tx.stream.compute.path")) {
                return streamInternal(query, false);
            }
        }

        public List<ConceptSetMeasure> execute(GraqlCompute.Centrality query) {
            try (ThreadTrace trace = traceOnThread("tx.execute.compute.centrality")) {
                return executeInternal(query, false);
            }
        }

        public Stream<ConceptSetMeasure> stream(GraqlCompute.Centrality query) {
            try (ThreadTrace trace = traceOnThread("tx.stream.compute.centrality")) {
                return streamInternal(query, false);
            }
        }

        public List<ConceptSet> execute(GraqlCompute.Cluster query) {
            try (ThreadTrace trace = traceOnThread("tx.execute.compute.cluster")) {
                return executeInternal(query, false);
            }
        }

        public Stream<ConceptSet> stream(GraqlCompute.Cluster query) {
            try (ThreadTrace trace = traceOnThread("tx.stream.compute.cluster")) {
                return streamInternal(query, false);
            }
        }

        // Generic queries

        public List<? extends Answer> execute(GraqlQuery query) {
            return execute(query, true);
        }

        public List<? extends Answer> execute(GraqlQuery query, boolean infer) {
            if (query instanceof GraqlDefine) {
                return execute((GraqlDefine) query);

            } else if (query instanceof GraqlUndefine) {
                return execute((GraqlUndefine) query);

            } else if (query instanceof GraqlInsert) {
                return execute((GraqlInsert) query, infer);

            } else if (query instanceof GraqlDelete) {
                return execute((GraqlDelete) query, infer);

            } else if (query instanceof GraqlGet) {
                return execute((GraqlGet) query, infer);

            } else if (query instanceof GraqlGet.Aggregate) {
                return execute((GraqlGet.Aggregate) query, infer);

            } else if (query instanceof GraqlGet.Group.Aggregate) {
                return execute((GraqlGet.Group.Aggregate) query, infer);

            } else if (query instanceof GraqlGet.Group) {
                return execute((GraqlGet.Group) query, infer);

            } else if (query instanceof GraqlCompute.Statistics) {
                return execute((GraqlCompute.Statistics) query);

            } else if (query instanceof GraqlCompute.Path) {
                return execute((GraqlCompute.Path) query);

            } else if (query instanceof GraqlCompute.Centrality) {
                return execute((GraqlCompute.Centrality) query);

            } else if (query instanceof GraqlCompute.Cluster) {
                return execute((GraqlCompute.Cluster) query);

            } else {
                throw new IllegalArgumentException("Unrecognised Query object");
            }
        }

        public Stream<? extends Answer> stream(GraqlQuery query) {
            return stream(query, true);
        }

        public Stream<? extends Answer> stream(GraqlQuery query, boolean infer) {
            if (query instanceof GraqlDefine) {
                return stream((GraqlDefine) query);

            } else if (query instanceof GraqlUndefine) {
                return stream((GraqlUndefine) query);

            } else if (query instanceof GraqlInsert) {
                return stream((GraqlInsert) query, infer);

            } else if (query instanceof GraqlDelete) {
                return stream((GraqlDelete) query, infer);

            } else if (query instanceof GraqlGet) {
                return stream((GraqlGet) query, infer);

            } else if (query instanceof GraqlGet.Aggregate) {
                return stream((GraqlGet.Aggregate) query, infer);

            } else if (query instanceof GraqlGet.Group.Aggregate) {
                return stream((GraqlGet.Group.Aggregate) query, infer);

            } else if (query instanceof GraqlGet.Group) {
                return stream((GraqlGet.Group) query, infer);

            } else if (query instanceof GraqlCompute.Statistics) {
                return stream((GraqlCompute.Statistics) query);

            } else if (query instanceof GraqlCompute.Path) {
                return stream((GraqlCompute.Path) query);

            } else if (query instanceof GraqlCompute.Centrality) {
                return stream((GraqlCompute.Centrality) query);

            } else if (query instanceof GraqlCompute.Cluster) {
                return stream((GraqlCompute.Cluster) query);

            } else {
                throw new IllegalArgumentException("Unrecognised Query object");
            }
        }

        private <T extends Answer> List<T> executeInternal(GraqlQuery query, boolean infer) {
            return this.<T>streamInternal(query, infer).collect(Collectors.toList());
        }

        private <T extends Answer> Stream<T> streamInternal(GraqlQuery query, boolean infer) {
            return iterate(RequestBuilder.Transaction.query(query.toString(), infer),
                    response -> ResponseReader.answer(response.getQueryIterRes().getAnswer(), this));
        }

        public void close() {
            transceiver.close();
        }

        public boolean isOpen() {
            return transceiver.isOpen();
        }

        // TODO remove - backwards compatibility
        public boolean isClosed() {
            return !isOpen();
        }

        private ReentrantLock lock = new ReentrantLock();

        private SessionProto.Transaction.Res sendAndReceiveOrThrow(SessionProto.Transaction.Req request) {
            Transceiver.Response response;

            try {
                response = transceiver.sendAndReceive(request);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                // This is called from classes like Transaction, that impl methods which do not throw InterruptedException
                // Therefore, we have to wrap it in a RuntimeException.
                throw new RuntimeException(e);
            }

            switch (response.type()) {
                case OK:
                    return response.ok();
                case ERROR:
                    // TODO: parse different GRPC errors into specific GraknClientException
                    throw GraknClientException.create(response.error().getMessage(), response.error());
                case COMPLETED:
                    // This will occur when interrupting a running query/operation on the current transaction
                    throw GraknClientException.create("Transaction interrupted, all running queries have been stopped.");
                default:
                    throw GraknClientException.unreachableStatement("Unexpected response " + response);
            }
        }

        private static class TransactionIterator extends AbstractIterator<SessionProto.Transaction.Iter.Res> implements Transceiver.ResponseReceiver {
            private final BlockingQueue<Transceiver.Response> queue = new LinkedBlockingQueue<>();

            @Override
            public boolean onResponse(Transceiver.Response response) {
                queue.add(response);
                if (response.type() == OK) {
                    SessionProto.Transaction.Iter.Res iterRes = response.ok().getIterRes();
                    switch (iterRes.getResCase()) {
                        case DONE:
                        case ITERATORID:
                            return false;
                        default:
                            return true;
                    }
                }
                return false;
            }

            @Override
            protected SessionProto.Transaction.Iter.Res computeNext() {
                Transceiver.Response response;
                try {
                    response = queue.take();
                } catch (InterruptedException e) {
                    throw GraknConceptException.create("Iteration Interrupted");
                }

                switch (response.type()) {
                    case OK:
                        return response.ok().getIterRes();
                    case ERROR:
                        throw GraknClientException.create(response.error().getMessage(), response.error());
                    case COMPLETED:
                        throw GraknClientException.create("Transaction interrupted, all running queries have been stopped.");
                    default:
                        throw GraknClientException.unreachableStatement("Unexpected response " + response);
                }
            }
        }

        private Iterator<SessionProto.Transaction.Iter.Res> sendAndReceiveIter(SessionProto.Transaction.Iter.Req request) {
            TransactionIterator iterator = new TransactionIterator();
            transceiver.sendAndReceiveAsync(SessionProto.Transaction.Req.newBuilder().setIterReq(request).build(), iterator);
            return iterator;
        }

        public void commit() {
            sendAndReceiveOrThrow(RequestBuilder.Transaction.commit());
            close();
        }

        @Nullable
        public grakn.client.concept.type.Type.Remote<?, ?> getType(Label label) {
            SchemaConcept.Remote<?> concept = getSchemaConcept(label);
            if (concept instanceof grakn.client.concept.type.Type.Remote) {
                return (grakn.client.concept.type.Type.Remote<?, ?>) concept;
            } else {
                return null;
            }
        }

        @Nullable
        public EntityType.Remote getEntityType(String label) {
            SchemaConcept.Remote<?> concept = getSchemaConcept(Label.of(label));
            if (concept instanceof EntityType.Remote) {
                return (EntityType.Remote) concept;
            } else {
                return null;
            }
        }

        @Nullable
        public RelationType.Remote getRelationType(String label) {
            SchemaConcept.Remote<?> concept = getSchemaConcept(Label.of(label));
            if (concept instanceof RelationType.Remote) {
                return (RelationType.Remote) concept;
            } else {
                return null;
            }
        }

        @SuppressWarnings("unchecked")
        @Nullable
        public <V> AttributeType.Remote<V> getAttributeType(String label) {
            SchemaConcept.Remote<?> concept = getSchemaConcept(Label.of(label));
            if (concept instanceof AttributeType.Remote) {
                return (AttributeType.Remote<V>) concept;
            } else {
                return null;
            }
        }

        @Nullable
        public Role.Remote getRole(String label) {
            SchemaConcept.Remote<?> concept = getSchemaConcept(Label.of(label));
            if (concept instanceof Role.Remote) {
                return (Role.Remote) concept;
            } else {
                return null;
            }
        }

        @Nullable
        public Rule.Remote getRule(String label) {
            SchemaConcept.Remote<?> concept = getSchemaConcept(Label.of(label));
            if (concept instanceof Rule.Remote) {
                return (Rule.Remote) concept;
            } else {
                return null;
            }
        }

        @SuppressWarnings("unchecked")
        @Nullable
        public SchemaConcept.Remote<?> getSchemaConcept(Label label) {
            SessionProto.Transaction.Res response = sendAndReceiveOrThrow(RequestBuilder.Transaction.getSchemaConcept(label));
            switch (response.getGetSchemaConceptRes().getResCase()) {
                case NULL:
                    return null;
                case SCHEMACONCEPT:
                    return Concept.Remote.of(response.getGetSchemaConceptRes().getSchemaConcept(), this).asSchemaConcept();
                default:
                    throw GraknClientException.resultNotPresent();
            }
        }

        public SchemaConcept<?> getMetaConcept() {
            return getSchemaConcept(Label.of(Graql.Token.Type.THING.toString()));
        }

        public MetaType.Remote<?, ?> getMetaRelationType() {
            return getSchemaConcept(Label.of(Graql.Token.Type.RELATION.toString())).asMetaType();
        }

        public MetaType.Remote<?, ?> getMetaRole() {
            return getSchemaConcept(Label.of(Graql.Token.Type.ROLE.toString())).asMetaType();
        }

        public MetaType.Remote<?, ?> getMetaAttributeType() {
            return getSchemaConcept(Label.of(Graql.Token.Type.ATTRIBUTE.toString())).asMetaType();
        }

        public MetaType.Remote<?, ?> getMetaEntityType() {
            return getSchemaConcept(Label.of(Graql.Token.Type.ENTITY.toString())).asMetaType();
        }

        public MetaType.Remote<?, ?> getMetaRule() {
            return getSchemaConcept(Label.of(Graql.Token.Type.RULE.toString())).asMetaType();
        }

        @Nullable
        public Concept.Remote<?> getConcept(ConceptId id) {
            SessionProto.Transaction.Res response = sendAndReceiveOrThrow(RequestBuilder.Transaction.getConcept(id));
            switch (response.getGetConceptRes().getResCase()) {
                case NULL:
                    return null;
                case CONCEPT:
                    return Concept.Remote.of(response.getGetConceptRes().getConcept(), this);
                default:
                    throw GraknClientException.resultNotPresent();
            }
        }

        @SuppressWarnings("unchecked")
        public <V> Collection<Attribute.Remote<V>> getAttributesByValue(V value) {
            return iterate(RequestBuilder.Transaction.getAttributes(value),
                    response -> (Attribute.Remote<V>) Concept.Remote.of(response.getGetAttributesIterRes().getAttribute(), this).asAttribute()).collect(Collectors.toSet());
        }

        public EntityType.Remote putEntityType(String label) {
            return putEntityType(Label.of(label));
        }

        public EntityType.Remote putEntityType(Label label) {
            return Concept.Remote.of(sendAndReceiveOrThrow(RequestBuilder.Transaction.putEntityType(label)).getPutEntityTypeRes().getEntityType(), this).asEntityType();
        }

        public <V> AttributeType.Remote<V> putAttributeType(String label, ValueType<V> valueType) {
            return putAttributeType(Label.of(label), valueType);
        }
        @SuppressWarnings("unchecked")
        public <V> AttributeType.Remote<V> putAttributeType(Label label, ValueType<V> valueType) {
            return (AttributeType.Remote<V>) Concept.Remote.of(sendAndReceiveOrThrow(RequestBuilder.Transaction.putAttributeType(label, valueType))
                    .getPutAttributeTypeRes().getAttributeType(), this).asAttributeType();
        }

        public RelationType.Remote putRelationType(String label) {
            return putRelationType(Label.of(label));
        }
        public RelationType.Remote putRelationType(Label label) {
            return Concept.Remote.of(sendAndReceiveOrThrow(RequestBuilder.Transaction.putRelationType(label))
                    .getPutRelationTypeRes().getRelationType(), this).asRelationType();
        }

        public Role.Remote putRole(String label) {
            return putRole(Label.of(label));
        }
        public Role.Remote putRole(Label label) {
            return Concept.Remote.of(sendAndReceiveOrThrow(RequestBuilder.Transaction.putRole(label))
                    .getPutRoleRes().getRole(), this).asRole();
        }

        public Rule.Remote putRule(String label, Pattern when, Pattern then) {
            return putRule(Label.of(label), when, then);
        }
        public Rule.Remote putRule(Label label, Pattern when, Pattern then) {
            return Concept.Remote.of(sendAndReceiveOrThrow(RequestBuilder.Transaction.putRule(label, when, then))
                    .getPutRuleRes().getRule(), this).asRule();
        }

        public Stream<SchemaConcept.Remote<?>> sups(SchemaConcept.Remote<?> schemaConcept) {
            ConceptProto.Method.Iter.Req method = ConceptProto.Method.Iter.Req.newBuilder()
                    .setSchemaConceptSupsIterReq(ConceptProto.SchemaConcept.Sups.Iter.Req.getDefaultInstance()).build();

            return iterateConceptMethod(schemaConcept.id(), method,
                    res -> Concept.Remote.of(res.getSchemaConceptSupsIterRes().getSchemaConcept(), this).asSchemaConcept());
        }

        public SessionProto.Transaction.Res runConceptMethod(ConceptId id, ConceptProto.Method.Req method) {
            SessionProto.Transaction.ConceptMethod.Req conceptMethod = SessionProto.Transaction.ConceptMethod.Req.newBuilder()
                    .setId(id.getValue()).setMethod(method).build();
            SessionProto.Transaction.Req request = SessionProto.Transaction.Req.newBuilder().setConceptMethodReq(conceptMethod).build();

            return sendAndReceiveOrThrow(request);
        }

        public <T> Stream<T> iterateConceptMethod(ConceptId id, ConceptProto.Method.Iter.Req method, Function<ConceptProto.Method.Iter.Res, T> responseReader) {
            SessionProto.Transaction.ConceptMethod.Iter.Req conceptIterMethod = SessionProto.Transaction.ConceptMethod.Iter.Req.newBuilder()
                    .setId(id.getValue()).setMethod(method).build();
            SessionProto.Transaction.Iter.Req request = SessionProto.Transaction.Iter.Req.newBuilder().setConceptMethodIterReq(conceptIterMethod).build();

            return iterate(request, res -> responseReader.apply(res.getConceptMethodIterRes().getResponse()));
        }

        public Explanation getExplanation(ConceptMap explainable) {
            AnswerProto.ConceptMap conceptMapProto = conceptMap(explainable);
            AnswerProto.Explanation.Req explanationReq = AnswerProto.Explanation.Req.newBuilder().setExplainable(conceptMapProto).build();
            SessionProto.Transaction.Req request = SessionProto.Transaction.Req.newBuilder().setExplanationReq(explanationReq).build();
            SessionProto.Transaction.Res response = sendAndReceiveOrThrow(request);
            return ResponseReader.explanation(response.getExplanationRes(), this);
        }

        private AnswerProto.ConceptMap conceptMap(ConceptMap conceptMap) {
            AnswerProto.ConceptMap.Builder conceptMapProto = AnswerProto.ConceptMap.newBuilder();
            conceptMap.map().forEach((var, concept) -> {
                ConceptProto.Concept conceptProto = RequestBuilder.ConceptMessage.from(concept);
                conceptMapProto.putMap(var.name(), conceptProto);
            });
            conceptMapProto.setHasExplanation(conceptMap.hasExplanation());
            conceptMapProto.setPattern(conceptMap.queryPattern().toString());
            return conceptMapProto.build();
        }

        public <T> Stream<T> iterate(SessionProto.Transaction.Iter.Req request, Function<SessionProto.Transaction.Iter.Res, T> responseReader) {
            return Objects.requireNonNull(StreamSupport.stream(((Iterable<T>) () -> new RPCIterator<>(request, responseReader)).spliterator(), false));
        }

        /**
         * A client-side iterator over gRPC messages. Will send SessionProto.Transaction.Iter.Req messages until
         * SessionProto.Transaction.Iter.Res returns done as a message.
         *
         * @param <T> class type of objects being iterated
         */
        public class RPCIterator<T> extends AbstractIterator<T> {
            private SessionProto.Transaction.Iter.Req request;
            private SessionProto.Transaction.Iter.Req.Options requestOptions;
            private Function<SessionProto.Transaction.Iter.Res, T> responseReader;
            private int iteratorId = 0;

            private Iterator<SessionProto.Transaction.Iter.Res> currentIterator = null;

            private RPCIterator(SessionProto.Transaction.Iter.Req request, Function<SessionProto.Transaction.Iter.Res, T> responseReader) {
                this.request = request;
                this.responseReader = responseReader;
                this.requestOptions = request.getOptions();
            }

            private void startIterating() {
                currentIterator = sendAndReceiveIter(request);
            }

            private void requestBatch() {
                currentIterator = sendAndReceiveIter(SessionProto.Transaction.Iter.Req.newBuilder()
                                .setOptions(requestOptions)
                                .setIteratorId(iteratorId).build());
            }

            private T processNext(SessionProto.Transaction.Iter.Res response) {
                switch (response.getResCase()) {
                    case DONE:
                        return endOfData();
                    case ITERATORID:
                        iteratorId = response.getIteratorId();
                        requestBatch();
                        return computeNext();
                    case RES_NOT_SET:
                        throw GraknClientException.unreachableStatement("Unexpected " + response);
                    default:
                        return responseReader.apply(response);
                }
            }

            protected final T computeNext() {
                if (currentIterator == null) {
                    startIterating();
                }

                if (currentIterator.hasNext()) {
                    return processNext(currentIterator.next());
                } else {
                    // processNext should handle the iteration end due to a done or batch done response
                    throw GraknClientException.create("Unexpected end of iteration");
                }
            }
        }
    }

    /**
     * Internal class used to handle keyspace related operations
     */

    public static final class Keyspaces {
        private String username;
        private String password;

        private KeyspaceServiceBlockingStub keyspaceBlockingStub;

        Keyspaces(ManagedChannel channel, String username, String password) {
            keyspaceBlockingStub = KeyspaceServiceGrpc.newBlockingStub(channel);
            this.username = username;
            this.password = password;
        }

        public void delete(String name) {
            try {
                KeyspaceProto.Keyspace.Delete.Req request = RequestBuilder.KeyspaceMessage.delete(name, this.username, this.password);
                keyspaceBlockingStub.delete(request);
            } catch (StatusRuntimeException e) {
                throw GraknClientException.create(e.getMessage(), e);
            }
        }

        public List<String> retrieve() {
            try {
                KeyspaceProto.Keyspace.Retrieve.Req request = RequestBuilder.KeyspaceMessage.retrieve(this.username, this.password);
                return ImmutableList.copyOf(keyspaceBlockingStub.retrieve(request).getNamesList().iterator());
            } catch (StatusRuntimeException e) {
                throw GraknClientException.create(e.getMessage(), e);
            }
        }
    }

    /**
     * An identifier for an isolated scope of a data in the database.
     */
    public static class Keyspace implements Serializable {

        private static final long serialVersionUID = 2726154016735929123L;
        public static final String DEFAULT = "grakn";

        private final String name;

        Keyspace(String name) {
            if (name == null) {
                throw new NullPointerException("Null name");
            }
            this.name = name;
        }

        @CheckReturnValue
        public static Keyspace of(String name) {
            return new Keyspace(name);
        }

        @CheckReturnValue
        public String name() {
            return name;
        }

        public final String toString() {
            return name();
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Keyspace that = (Keyspace) o;
            return this.name.equals(that.name);
        }

        public int hashCode() {
            int h = 1;
            h *= 1000003;
            h ^= this.name.hashCode();
            return h;
        }
    }
}
