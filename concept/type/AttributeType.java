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

package grakn.client.concept.type;

import grakn.client.Grakn;
import grakn.client.common.exception.GraknClientException;
import grakn.client.concept.thing.Attribute;
import grakn.protocol.ConceptProto;

import javax.annotation.Nullable;
import java.time.LocalDateTime;
import java.util.stream.Stream;

import static grakn.client.common.exception.ErrorMessage.Concept.BAD_VALUE_TYPE;

public interface AttributeType extends ThingType {

    ValueType getValueType();

    boolean isKeyable();

    @Override
    AttributeType.Remote asRemote(Grakn.Transaction transaction);

    AttributeType.Boolean asBoolean();

    AttributeType.Long asLong();

    AttributeType.Double asDouble();

    AttributeType.String asString();

    AttributeType.DateTime asDateTime();

    enum ValueType {

        OBJECT(Object.class, false, false),
        BOOLEAN(Boolean.class, true, false),
        LONG(Long.class, true, true),
        DOUBLE(Double.class, true, false),
        STRING(String.class, true, true),
        DATETIME(LocalDateTime.class, true, true);

        private final Class<?> valueClass;
        private final boolean isWritable;
        private final boolean isKeyable;

        ValueType(Class<?> valueClass, boolean isWritable, boolean isKeyable) {
            this.valueClass = valueClass;
            this.isWritable = isWritable;
            this.isKeyable = isKeyable;
        }

        public static ValueType of(Class<?> valueClass) {
            for (final ValueType t : ValueType.values()) {
                if (t.valueClass == valueClass) {
                    return t;
                }
            }
            throw new GraknClientException(BAD_VALUE_TYPE);
        }

        public static ValueType of(ConceptProto.AttributeType.VALUE_TYPE valueType) {
            switch (valueType) {
                case STRING:
                    return AttributeType.ValueType.STRING;
                case BOOLEAN:
                    return AttributeType.ValueType.BOOLEAN;
                case LONG:
                    return AttributeType.ValueType.LONG;
                case DOUBLE:
                    return AttributeType.ValueType.DOUBLE;
                case DATETIME:
                    return AttributeType.ValueType.DATETIME;
                default:
                case UNRECOGNIZED:
                    throw new GraknClientException(BAD_VALUE_TYPE.message(valueType));
            }
        }

        public Class<?> valueClass() {
            return valueClass;
        }

        public boolean isWritable() {
            return isWritable;
        }

        public boolean isKeyable() {
            return isKeyable;
        }

        @Override
        public java.lang.String toString() {
            return valueClass.getName();
        }
    }

    interface Local extends ThingType.Local, AttributeType {

        @Override
        AttributeType.Local asAttributeType();

        @Override
        AttributeType.Boolean.Local asBoolean();

        @Override
        AttributeType.Long.Local asLong();

        @Override
        AttributeType.Double.Local asDouble();

        @Override
        AttributeType.String.Local asString();

        @Override
        AttributeType.DateTime.Local asDateTime();
    }

    interface Remote extends ThingType.Remote, AttributeType {

        void setSupertype(AttributeType type);

        @Override
        AttributeType.Local getSupertype();

        @Override
        Stream<? extends AttributeType.Local> getSupertypes();

        @Override
        Stream<? extends AttributeType.Local> getSubtypes();

        @Override
        Stream<? extends Attribute.Local<?>> getInstances();

        Stream<? extends ThingType.Local> getOwners();

        Stream<? extends ThingType.Local> getOwners(boolean onlyKey);

        @Override
        AttributeType.Remote asAttributeType();

        @Override
        AttributeType.Boolean.Remote asBoolean();

        @Override
        AttributeType.Long.Remote asLong();

        @Override
        AttributeType.Double.Remote asDouble();

        @Override
        AttributeType.String.Remote asString();

        @Override
        AttributeType.DateTime.Remote asDateTime();
    }

    interface Boolean extends AttributeType {

        @Override
        AttributeType.Boolean.Remote asRemote(Grakn.Transaction transaction);

        interface Local extends AttributeType.Boolean, AttributeType.Local {
        }

        interface Remote extends AttributeType.Boolean, AttributeType.Remote {

            void setSupertype(AttributeType.Boolean type);

            @Override
            AttributeType.Boolean.Local getSupertype();

            @Override
            Stream<? extends AttributeType.Boolean.Local> getSupertypes();

            @Override
            Stream<? extends AttributeType.Boolean.Local> getSubtypes();

            @Override
            Stream<? extends Attribute.Boolean.Local> getInstances();

            Attribute.Boolean.Local put(boolean value);

            @Nullable
            Attribute.Boolean.Local get(boolean value);
        }
    }

    interface Long extends AttributeType {

        @Override
        AttributeType.Long.Remote asRemote(Grakn.Transaction transaction);

        interface Local extends AttributeType.Long, AttributeType.Local {
        }

        interface Remote extends AttributeType.Long, AttributeType.Remote {

            void setSupertype(AttributeType.Long type);

            @Override
            AttributeType.Long.Local getSupertype();

            @Override
            Stream<? extends AttributeType.Long.Local> getSupertypes();

            @Override
            Stream<? extends AttributeType.Long.Local> getSubtypes();

            @Override
            Stream<? extends Attribute.Long.Local> getInstances();

            Attribute.Long.Local put(long value);

            @Nullable
            Attribute.Long.Local get(long value);
        }
    }

    interface Double extends AttributeType {

        @Override
        AttributeType.Double.Remote asRemote(Grakn.Transaction transaction);

        interface Local extends AttributeType.Double, AttributeType.Local {
        }

        interface Remote extends AttributeType.Double, AttributeType.Remote {

            void setSupertype(AttributeType.Double type);

            @Override
            AttributeType.Double.Local getSupertype();

            @Override
            Stream<? extends AttributeType.Double.Local> getSupertypes();

            @Override
            Stream<? extends AttributeType.Double.Local> getSubtypes();

            @Override
            Stream<? extends Attribute.Double.Local> getInstances();

            Attribute.Double.Local put(double value);

            @Nullable
            Attribute.Double.Local get(double value);
        }
    }

    interface String extends AttributeType {

        @Override
        AttributeType.String.Remote asRemote(Grakn.Transaction transaction);

        interface Local extends AttributeType.String, AttributeType.Local {
        }

        interface Remote extends AttributeType.String, AttributeType.Remote {

            void setSupertype(AttributeType.String type);

            @Override
            AttributeType.String.Local getSupertype();

            @Override
            Stream<? extends AttributeType.String.Local> getSupertypes();

            @Override
            Stream<? extends AttributeType.String.Local> getSubtypes();

            @Override
            Stream<? extends Attribute.String.Local> getInstances();

            Attribute.String.Local put(java.lang.String value);

            @Nullable
            Attribute.String.Local get(java.lang.String value);

            @Nullable
            java.lang.String getRegex();

            void setRegex(java.lang.String regex);
        }
    }

    interface DateTime extends AttributeType {

        @Override
        AttributeType.DateTime.Remote asRemote(Grakn.Transaction transaction);

        interface Local extends AttributeType.DateTime, AttributeType.Local {
        }

        interface Remote extends AttributeType.DateTime, AttributeType.Remote {

            void setSupertype(AttributeType.DateTime type);

            @Override
            AttributeType.DateTime.Local getSupertype();

            @Override
            Stream<? extends AttributeType.DateTime.Local> getSupertypes();

            @Override
            Stream<? extends AttributeType.DateTime.Local> getSubtypes();

            @Override
            Stream<? extends Attribute.DateTime.Local> getInstances();

            Attribute.DateTime.Local put(LocalDateTime value);

            @Nullable
            Attribute.DateTime.Local get(LocalDateTime value);
        }
    }
}
