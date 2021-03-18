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

package grakn.client.concept.thing;

import grakn.client.api.GraknTransaction;
import grakn.client.api.concept.thing.Attribute;
import grakn.client.api.concept.type.ThingType;
import grakn.client.common.GraknClientException;
import grakn.client.concept.type.AttributeTypeImpl;
import grakn.protocol.ConceptProto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.stream.Stream;

import static grakn.client.common.ErrorMessage.Concept.BAD_VALUE_TYPE;
import static grakn.client.common.ErrorMessage.Concept.INVALID_CONCEPT_CASTING;
import static grakn.client.common.RequestBuilder.Thing.Attribute.getOwnersReq;
import static grakn.client.concept.type.ThingTypeImpl.protoThingType;
import static grakn.common.collection.Bytes.bytesToHexString;
import static grakn.common.util.Objects.className;

public abstract class AttributeImpl<VALUE> extends ThingImpl implements Attribute<VALUE> {

    AttributeImpl(java.lang.String iid) {
        super(iid);
    }

    public static AttributeImpl<?> of(ConceptProto.Thing thingProto) {
        switch (thingProto.getType().getValueType()) {
            case BOOLEAN:
                return AttributeImpl.Boolean.of(thingProto);
            case LONG:
                return AttributeImpl.Long.of(thingProto);
            case DOUBLE:
                return AttributeImpl.Double.of(thingProto);
            case STRING:
                return AttributeImpl.String.of(thingProto);
            case DATETIME:
                return AttributeImpl.DateTime.of(thingProto);
            case UNRECOGNIZED:
            default:
                throw new GraknClientException(BAD_VALUE_TYPE, thingProto.getType().getValueType());
        }
    }

    @Override
    public abstract AttributeTypeImpl getType();

    @Override
    public AttributeImpl<VALUE> asAttribute() {
        return this;
    }

    @Override
    public AttributeImpl.Boolean asBoolean() {
        throw new GraknClientException(INVALID_CONCEPT_CASTING, className(this.getClass()), className(Attribute.Boolean.class));
    }

    @Override
    public AttributeImpl.Long asLong() {
        throw new GraknClientException(INVALID_CONCEPT_CASTING, className(this.getClass()), className(Attribute.Long.class));
    }

    @Override
    public AttributeImpl.Double asDouble() {
        throw new GraknClientException(INVALID_CONCEPT_CASTING, className(this.getClass()), className(Attribute.Double.class));
    }

    @Override
    public AttributeImpl.String asString() {
        throw new GraknClientException(INVALID_CONCEPT_CASTING, className(this.getClass()), className(Attribute.String.class));
    }

    @Override
    public AttributeImpl.DateTime asDateTime() {
        throw new GraknClientException(INVALID_CONCEPT_CASTING, className(this.getClass()), className(Attribute.DateTime.class));
    }

    public abstract VALUE getValue();

    public abstract static class Remote<VALUE> extends ThingImpl.Remote implements Attribute.Remote<VALUE> {

        Remote(GraknTransaction transaction, java.lang.String iid) {
            super(transaction, iid);
        }

        @Override
        public final Stream<ThingImpl> getOwners() {
            return stream(getOwnersReq(getIID()))
                    .flatMap(rp -> rp.getAttributeGetOwnersResPart().getThingsList().stream())
                    .map(ThingImpl::of);
        }

        @Override
        public Stream<ThingImpl> getOwners(ThingType ownerType) {
            return stream(getOwnersReq(getIID(), protoThingType(ownerType)))
                    .flatMap(rp -> rp.getAttributeGetOwnersResPart().getThingsList().stream())
                    .map(ThingImpl::of);
        }

        @Override
        public abstract AttributeTypeImpl getType();

        @Override
        public AttributeImpl.Remote<VALUE> asAttribute() {
            return this;
        }

        @Override
        public AttributeImpl.Boolean.Remote asBoolean() {
            throw new GraknClientException(INVALID_CONCEPT_CASTING, className(this.getClass()), className(Attribute.Boolean.class));
        }

        @Override
        public AttributeImpl.Long.Remote asLong() {
            throw new GraknClientException(INVALID_CONCEPT_CASTING, className(this.getClass()), className(Attribute.Long.class));
        }

        @Override
        public AttributeImpl.Double.Remote asDouble() {
            throw new GraknClientException(INVALID_CONCEPT_CASTING, className(this.getClass()), className(Attribute.Double.class));
        }

        @Override
        public AttributeImpl.String.Remote asString() {
            throw new GraknClientException(INVALID_CONCEPT_CASTING, className(this.getClass()), className(Attribute.String.class));
        }

        @Override
        public AttributeImpl.DateTime.Remote asDateTime() {
            throw new GraknClientException(INVALID_CONCEPT_CASTING, className(this.getClass()), className(Attribute.DateTime.class));
        }

        public abstract VALUE getValue();
    }

    public static class Boolean extends AttributeImpl<java.lang.Boolean> implements Attribute.Boolean {

        private final AttributeTypeImpl.Boolean type;
        private final java.lang.Boolean value;

        Boolean(java.lang.String iid, AttributeTypeImpl.Boolean type, boolean value) {
            super(iid);
            this.type = type;
            this.value = value;
        }

        public static AttributeImpl.Boolean of(ConceptProto.Thing thingProto) {
            return new AttributeImpl.Boolean(
                    bytesToHexString(thingProto.getIid().toByteArray()),
                    AttributeTypeImpl.Boolean.of(thingProto.getType()),
                    thingProto.getValue().getBoolean()
            );
        }

        @Override
        public final AttributeTypeImpl.Boolean getType() {
            return type;
        }

        @Override
        public final java.lang.Boolean getValue() {
            return value;
        }

        @Override
        public final AttributeImpl.Boolean asBoolean() {
            return this;
        }

        @Override
        public AttributeImpl.Boolean.Remote asRemote(GraknTransaction transaction) {
            return new AttributeImpl.Boolean.Remote(transaction, getIID(), type, value);
        }

        public static class Remote extends AttributeImpl.Remote<java.lang.Boolean> implements Attribute.Boolean.Remote {

            private final AttributeTypeImpl.Boolean type;
            private final java.lang.Boolean value;

            Remote(GraknTransaction transaction, java.lang.String iid, AttributeTypeImpl.Boolean type, java.lang.Boolean value) {
                super(transaction, iid);
                this.type = type;
                this.value = value;
            }

            @Override
            public Attribute.Boolean.Remote asRemote(GraknTransaction transaction) {
                return new AttributeImpl.Boolean.Remote(transaction, getIID(), type, value);
            }

            @Override
            public final java.lang.Boolean getValue() {
                return value;
            }

            @Override
            public AttributeTypeImpl.Boolean getType() {
                return type;
            }

            @Override
            public final AttributeImpl.Boolean.Remote asBoolean() {
                return this;
            }
        }
    }

    public static class Long extends AttributeImpl<java.lang.Long> implements Attribute.Long {

        private final AttributeTypeImpl.Long type;
        private final long value;

        Long(java.lang.String iid, AttributeTypeImpl.Long type, long value) {
            super(iid);
            this.type = type;
            this.value = value;
        }

        public static AttributeImpl.Long of(ConceptProto.Thing thingProto) {
            return new AttributeImpl.Long(
                    bytesToHexString(thingProto.getIid().toByteArray()),
                    AttributeTypeImpl.Long.of(thingProto.getType()),
                    thingProto.getValue().getLong()
            );
        }

        @Override
        public final AttributeTypeImpl.Long getType() {
            return type;
        }

        @Override
        public final java.lang.Long getValue() {
            return value;
        }

        @Override
        public final AttributeImpl.Long asLong() {
            return this;
        }

        @Override
        public AttributeImpl.Long.Remote asRemote(GraknTransaction transaction) {
            return new AttributeImpl.Long.Remote(transaction, getIID(), type, value);
        }

        public static class Remote extends AttributeImpl.Remote<java.lang.Long> implements Attribute.Long.Remote {

            private final AttributeTypeImpl.Long type;
            private final long value;

            Remote(GraknTransaction transaction, java.lang.String iid, AttributeTypeImpl.Long type, long value) {
                super(transaction, iid);
                this.type = type;
                this.value = value;
            }

            @Override
            public Attribute.Long.Remote asRemote(GraknTransaction transaction) {
                return new AttributeImpl.Long.Remote(transaction, getIID(), type, value);
            }

            @Override
            public final java.lang.Long getValue() {
                return value;
            }

            @Override
            public AttributeTypeImpl.Long getType() {
                return type;
            }

            @Override
            public final AttributeImpl.Long.Remote asLong() {
                return this;
            }
        }
    }

    public static class Double extends AttributeImpl<java.lang.Double> implements Attribute.Double {

        private final AttributeTypeImpl.Double type;
        private final double value;

        Double(java.lang.String iid, AttributeTypeImpl.Double type, double value) {
            super(iid);
            this.type = type;
            this.value = value;
        }

        public static AttributeImpl.Double of(ConceptProto.Thing thingProto) {
            return new AttributeImpl.Double(
                    bytesToHexString(thingProto.getIid().toByteArray()),
                    AttributeTypeImpl.Double.of(thingProto.getType()),
                    thingProto.getValue().getDouble()
            );
        }

        @Override
        public final AttributeTypeImpl.Double getType() {
            return type;
        }

        @Override
        public final java.lang.Double getValue() {
            return value;
        }

        @Override
        public final AttributeImpl.Double asDouble() {
            return this;
        }

        @Override
        public AttributeImpl.Double.Remote asRemote(GraknTransaction transaction) {
            return new AttributeImpl.Double.Remote(transaction, getIID(), type, value);
        }

        public static class Remote extends AttributeImpl.Remote<java.lang.Double> implements Attribute.Double.Remote {

            private final AttributeTypeImpl.Double type;
            private final double value;

            Remote(GraknTransaction transaction, java.lang.String iid, AttributeTypeImpl.Double type, double value) {
                super(transaction, iid);
                this.type = type;
                this.value = value;
            }

            @Override
            public Attribute.Double.Remote asRemote(GraknTransaction transaction) {
                return new AttributeImpl.Double.Remote(transaction, getIID(), type, value);
            }

            @Override
            public final java.lang.Double getValue() {
                return value;
            }

            @Override
            public AttributeTypeImpl.Double getType() {
                return type;
            }

            @Override
            public final AttributeImpl.Double.Remote asDouble() {
                return this;
            }
        }
    }

    public static class String extends AttributeImpl<java.lang.String> implements Attribute.String {

        private final AttributeTypeImpl.String type;
        private final java.lang.String value;

        String(java.lang.String iid, AttributeTypeImpl.String type, java.lang.String value) {
            super(iid);
            this.type = type;
            this.value = value;
        }

        public static AttributeImpl.String of(ConceptProto.Thing thingProto) {
            return new AttributeImpl.String(
                    bytesToHexString(thingProto.getIid().toByteArray()),
                    AttributeTypeImpl.String.of(thingProto.getType()),
                    thingProto.getValue().getString()
            );
        }

        @Override
        public final AttributeTypeImpl.String getType() {
            return type;
        }

        @Override
        public final java.lang.String getValue() {
            return value;
        }

        @Override
        public final AttributeImpl.String asString() {
            return this;
        }

        @Override
        public AttributeImpl.String.Remote asRemote(GraknTransaction transaction) {
            return new AttributeImpl.String.Remote(transaction, getIID(), type, value);
        }

        public static class Remote extends AttributeImpl.Remote<java.lang.String> implements Attribute.String.Remote {

            private final AttributeTypeImpl.String type;
            private final java.lang.String value;

            Remote(GraknTransaction transaction, java.lang.String iid, AttributeTypeImpl.String type, java.lang.String value) {
                super(transaction, iid);
                this.type = type;
                this.value = value;
            }

            @Override
            public Attribute.String.Remote asRemote(GraknTransaction transaction) {
                return new AttributeImpl.String.Remote(transaction, getIID(), type, value);
            }

            @Override
            public final java.lang.String getValue() {
                return value;
            }

            @Override
            public AttributeTypeImpl.String getType() {
                return type;
            }

            @Override
            public final AttributeImpl.String.Remote asString() {
                return this;
            }
        }
    }

    public static class DateTime extends AttributeImpl<LocalDateTime> implements Attribute.DateTime {

        private final AttributeTypeImpl.DateTime type;
        private final LocalDateTime value;

        DateTime(java.lang.String iid, AttributeTypeImpl.DateTime type, LocalDateTime value) {
            super(iid);
            this.type = type;
            this.value = value;
        }

        public static AttributeImpl.DateTime of(ConceptProto.Thing thingProto) {
            return new AttributeImpl.DateTime(
                    bytesToHexString(thingProto.getIid().toByteArray()),
                    AttributeTypeImpl.DateTime.of(thingProto.getType()),
                    toLocalDateTime(thingProto.getValue().getDateTime())
            );
        }

        @Override
        public final AttributeTypeImpl.DateTime getType() {
            return type;
        }

        @Override
        public final LocalDateTime getValue() {
            return value;
        }

        @Override
        public final AttributeImpl.DateTime asDateTime() {
            return this;
        }

        @Override
        public AttributeImpl.DateTime.Remote asRemote(GraknTransaction transaction) {
            return new AttributeImpl.DateTime.Remote(transaction, getIID(), type, value);
        }

        private static LocalDateTime toLocalDateTime(long rpcDatetime) {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(rpcDatetime), ZoneId.of("Z"));
        }

        public static class Remote extends AttributeImpl.Remote<LocalDateTime> implements Attribute.DateTime.Remote {

            private final AttributeTypeImpl.DateTime type;
            private final LocalDateTime value;

            Remote(GraknTransaction transaction, java.lang.String iid, AttributeTypeImpl.DateTime type, LocalDateTime value) {
                super(transaction, iid);
                this.type = type;
                this.value = value;
            }

            @Override
            public Attribute.DateTime.Remote asRemote(GraknTransaction transaction) {
                return new AttributeImpl.DateTime.Remote(transaction, getIID(), type, value);
            }

            @Override
            public final LocalDateTime getValue() {
                return value;
            }

            @Override
            public AttributeTypeImpl.DateTime getType() {
                return type;
            }

            @Override
            public final AttributeImpl.DateTime.Remote asDateTime() {
                return this;
            }
        }
    }
}
