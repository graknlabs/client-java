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
import grakn.client.concept.Concept;
import grakn.client.concept.ConceptIID;
import grakn.client.concept.ValueType;
import grakn.client.concept.Label;
import grakn.client.exception.GraknClientException;
import grakn.protocol.DatabaseProto;
import grakn.protocol.ConceptProto;
import grakn.protocol.OptionsProto;
import grakn.protocol.SessionProto;
import grakn.protocol.TransactionProto;
import graql.lang.pattern.Pattern;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static grabl.tracing.client.GrablTracingThreadStatic.ThreadTrace;
import static grabl.tracing.client.GrablTracingThreadStatic.currentThreadTrace;
import static grabl.tracing.client.GrablTracingThreadStatic.isTracingEnabled;
import static java.util.stream.Collectors.toList;

/**
 * A utility class to build RPC Requests from a provided set of Grakn concepts.
 */
public class RequestBuilder {

    public static class Session {

        public static SessionProto.Session.Open.Req open(String database) {
            return SessionProto.Session.Open.Req.newBuilder().setDatabase(database).build();
        }

        public static SessionProto.Session.Close.Req close(ByteString sessionId) {
            return SessionProto.Session.Close.Req.newBuilder().setSessionID(sessionId).build();
        }
    }

    /**
     * An RPC Request Builder class for Transaction Service
     */
    public static class Transaction {

        public static TransactionProto.Transaction.Req open(ByteString sessionID, GraknClient.Transaction.Type txType) {
            TransactionProto.Transaction.Open.Req openRequest = TransactionProto.Transaction.Open.Req.newBuilder()
                    .setSessionID(sessionID)
                    .setType(TransactionProto.Transaction.Type.valueOf(txType.iid()))
                    .build();

            return TransactionProto.Transaction.Req.newBuilder().putAllMetadata(getTracingData()).setOpenReq(openRequest).build();
        }

        public static TransactionProto.Transaction.Req commit() {
            return TransactionProto.Transaction.Req.newBuilder()
                    .putAllMetadata(getTracingData())
                    .setCommitReq(TransactionProto.Transaction.Commit.Req.getDefaultInstance())
                    .build();
        }

        public static TransactionProto.Transaction.Iter.Req query(String queryString, GraknClient.Transaction.QueryOptions options) {
            OptionsProto.Options.Builder builder = OptionsProto.Options.newBuilder();
            options
                    .whenSet(GraknClient.Transaction.BooleanOption.INFER, builder::setInfer)
                    .whenSet(GraknClient.Transaction.BooleanOption.EXPLAIN, builder::setExplain);

            TransactionProto.Transaction.Iter.Req.Builder req = TransactionProto.Transaction.Iter.Req.newBuilder()
                    .setQueryIterReq(TransactionProto.Transaction.Query.Iter.Req.newBuilder()
                            .setQuery(queryString)
                            .setOptions(builder));

            options.whenSet(GraknClient.Transaction.BatchOption.BATCH_SIZE, req::setOptions);

            return req.build();
        }

        public static TransactionProto.Transaction.Req getType(Label label) {
            return TransactionProto.Transaction.Req.newBuilder()
                    .putAllMetadata(getTracingData())
                    .setGetTypeReq(TransactionProto.Transaction.GetType.Req.newBuilder().setLabel(label.getValue()))
                    .build();
        }

        public static TransactionProto.Transaction.Req getConcept(ConceptIID iid) {
            return TransactionProto.Transaction.Req.newBuilder()
                    .putAllMetadata(getTracingData())
                    .setGetConceptReq(TransactionProto.Transaction.GetConcept.Req.newBuilder().setId(iid.getValue()))
                    .build();
        }

        public static TransactionProto.Transaction.Req putEntityType(Label label) {
            return TransactionProto.Transaction.Req.newBuilder()
                    .putAllMetadata(getTracingData())
                    .setPutEntityTypeReq(TransactionProto.Transaction.PutEntityType.Req.newBuilder().setLabel(label.getValue()))
                    .build();
        }

        public static TransactionProto.Transaction.Req putAttributeType(Label label, ValueType<?> valueType) {
            TransactionProto.Transaction.PutAttributeType.Req request = TransactionProto.Transaction.PutAttributeType.Req.newBuilder()
                    .setLabel(label.getValue())
                    .setValueType(ConceptMessage.setValueType(valueType))
                    .build();

            return TransactionProto.Transaction.Req.newBuilder().putAllMetadata(getTracingData()).setPutAttributeTypeReq(request).build();
        }

        public static TransactionProto.Transaction.Req putRelationType(Label label) {
            TransactionProto.Transaction.PutRelationType.Req request = TransactionProto.Transaction.PutRelationType.Req.newBuilder()
                    .setLabel(label.getValue())
                    .build();
            return TransactionProto.Transaction.Req.newBuilder().putAllMetadata(getTracingData()).setPutRelationTypeReq(request).build();
        }

        public static TransactionProto.Transaction.Req putRule(Label label, Pattern when, Pattern then) {
            throw new UnsupportedOperationException();
//            TransactionProto.Transaction.PutRule.Req request = TransactionProto.Transaction.PutRule.Req.newBuilder()
//                    .setLabel(label.getValue())
//                    .setWhen(when.toString())
//                    .setThen(then.toString())
//                    .build();
//            return TransactionProto.Transaction.Req.newBuilder().putAllMetadata(getTracingData()).setPutRuleReq(request).build();
        }
    }

    /**
     * An RPC Request Builder class for Concept messages
     */
    public static class ConceptMessage {

        public static ConceptProto.Concept from(Concept<?> concept) {
            return ConceptProto.Concept.newBuilder()
                    .setIid(concept.iid().getValue())
                    .setBaseType(getBaseType(concept))
                    .build();
        }

        private static ConceptProto.Concept.SCHEMA getBaseType(Concept<?> concept) {
            if (concept.isEntityType()) {
                return ConceptProto.Concept.SCHEMA.ENTITY_TYPE;
            } else if (concept.isRelationType()) {
                return ConceptProto.Concept.SCHEMA.RELATION_TYPE;
            } else if (concept.isAttributeType()) {
                return ConceptProto.Concept.SCHEMA.ATTRIBUTE_TYPE;
            } else if (concept.isEntity()) {
                return ConceptProto.Concept.SCHEMA.ENTITY;
            } else if (concept.isRelation()) {
                return ConceptProto.Concept.SCHEMA.RELATION;
            } else if (concept.isAttribute()) {
                return ConceptProto.Concept.SCHEMA.ATTRIBUTE;
            } else if (concept.isRole()) {
                return ConceptProto.Concept.SCHEMA.ROLE;
            } else if (concept.isRule()) {
                return ConceptProto.Concept.SCHEMA.RULE;
            } else if (concept.isType()) {
                return ConceptProto.Concept.SCHEMA.META_TYPE;
            } else {
                throw GraknClientException.unreachableStatement("Unrecognised concept " + concept);
            }
        }

        public static Collection<ConceptProto.Concept> concepts(Collection<? extends Concept<?>> concepts) {
            return concepts.stream().map(ConceptMessage::from).collect(toList());
        }

        public static ConceptProto.ValueObject attributeValue(Object value) {
            // TODO: this conversion method should use Serialiser class, once it's moved to grakn.common

            ConceptProto.ValueObject.Builder builder = ConceptProto.ValueObject.newBuilder();
            if (value instanceof String) {
                builder.setString((String) value);
            } else if (value instanceof Boolean) {
                builder.setBoolean((boolean) value);
            } else if (value instanceof Long) {
                builder.setLong((long) value);
            } else if (value instanceof Double) {
                builder.setDouble((double) value);
            } else if (value instanceof LocalDateTime) {
                builder.setDatetime(((LocalDateTime) value).atZone(ZoneId.of("Z")).toInstant().toEpochMilli());
            } else {
                throw GraknClientException.unreachableStatement("Unrecognised " + value);
            }

            return builder.build();
        }

        @SuppressWarnings("unchecked")
        public static <D> ValueType<D> valueType(ConceptProto.AttributeType.VALUE_TYPE valueType) {
            switch (valueType) {
                case STRING:
                    return (ValueType<D>) ValueType.STRING;
                case BOOLEAN:
                    return (ValueType<D>) ValueType.BOOLEAN;
                case LONG:
                    return (ValueType<D>) ValueType.LONG;
                case DOUBLE:
                    return (ValueType<D>) ValueType.DOUBLE;
                case DATETIME:
                    return (ValueType<D>) ValueType.DATETIME;
                default:
                case UNRECOGNIZED:
                    throw new IllegalArgumentException("Unrecognised " + valueType);
            }
        }

        public static ConceptProto.AttributeType.VALUE_TYPE setValueType(ValueType<?> valueType) {
            if (valueType.equals(ValueType.STRING)) {
                return ConceptProto.AttributeType.VALUE_TYPE.STRING;
            } else if (valueType.equals(ValueType.BOOLEAN)) {
                return ConceptProto.AttributeType.VALUE_TYPE.BOOLEAN;
            } else if (valueType.equals(ValueType.LONG)) {
                return ConceptProto.AttributeType.VALUE_TYPE.LONG;
            } else if (valueType.equals(ValueType.DOUBLE)) {
                return ConceptProto.AttributeType.VALUE_TYPE.DOUBLE;
            } else if (valueType.equals(ValueType.DATETIME)) {
                return ConceptProto.AttributeType.VALUE_TYPE.DATETIME;
            } else {
                throw GraknClientException.unreachableStatement("Unrecognised " + valueType);
            }
        }
    }

    /**
     * An RPC Request Builder class for Database Service
     */
    public static class DatabaseMessage {

        public static DatabaseProto.Database.Contains.Req contains(String name) {
            return DatabaseProto.Database.Contains.Req.newBuilder().setName(name).build();
        }

        public static DatabaseProto.Database.Create.Req create(String name) {
            return DatabaseProto.Database.Create.Req.newBuilder().setName(name).build();
        }

        public static DatabaseProto.Database.Delete.Req delete(String name) {
            return DatabaseProto.Database.Delete.Req.newBuilder().setName(name).build();
        }

        public static DatabaseProto.Database.All.Req all() {
            return DatabaseProto.Database.All.Req.getDefaultInstance();
        }
    }

    public static Map<String, String> getTracingData() {
        if (isTracingEnabled()) {
            ThreadTrace threadTrace = currentThreadTrace();
            if (threadTrace == null) {
                return Collections.emptyMap();
            }

            if (threadTrace.getId() == null || threadTrace.getRootId() == null) {
                return Collections.emptyMap();
            }

            Map<String, String> metadata = new HashMap<>(2);
            metadata.put("traceParentId", threadTrace.getId().toString());
            metadata.put("traceRootId", threadTrace.getRootId().toString());
            return metadata;
        } else {
            return Collections.emptyMap();
        }
    }
}
