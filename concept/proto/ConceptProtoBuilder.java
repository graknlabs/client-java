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

package grakn.client.concept.proto;

import com.google.protobuf.ByteString;
import grakn.client.common.exception.GraknException;
import grakn.client.concept.Concept;
import grakn.client.concept.thing.Attribute;
import grakn.client.concept.thing.Entity;
import grakn.client.concept.thing.Relation;
import grakn.client.concept.thing.Thing;
import grakn.client.concept.type.AttributeType;
import grakn.client.concept.type.AttributeType.ValueType;
import grakn.client.concept.type.EntityType;
import grakn.client.concept.type.RelationType;
import grakn.client.concept.type.RoleType;
import grakn.client.concept.type.Rule;
import grakn.client.concept.type.ThingType;
import grakn.client.concept.type.Type;
import grakn.protocol.ConceptProto;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;

import static grakn.client.common.exception.ErrorMessage.Concept.UNRECOGNISED_CONCEPT;
import static grakn.common.collection.Bytes.hexStringToBytes;
import static java.util.stream.Collectors.toList;

/**
 * An RPC Request Builder class for Concept messages
 */
public abstract class ConceptProtoBuilder {

    public static ConceptProto.Concept concept(Concept concept) {
        final ConceptProto.Concept.Builder builder = ConceptProto.Concept.newBuilder();
        if (concept instanceof Thing) {
            builder.setThing(thing(concept.asThing()));
        } else {
            builder.setType(type(concept.asType()));
        }
        return builder.build();
    }

    public static ConceptProto.Thing thing(Thing thing) {
        return ConceptProto.Thing.newBuilder()
                .setIid(iid(thing.getIID()))
                .setSchema(schema(thing))
                .build();
    }

    public static ConceptProto.Type type(Type type) {
        final ConceptProto.Type.Builder builder = ConceptProto.Type.newBuilder()
                .setLabel(type.getLabel())
                .setSchema(schema(type));

        if (type instanceof RoleType) {
            builder.setScopedLabel(type.asRoleType().getScopedLabel());
        }

        return builder.build();
    }

    public static Collection<ConceptProto.Type> types(Collection<? extends Type> types) {
        return types.stream().map(ConceptProtoBuilder::type).collect(toList());
    }

    public static ConceptProto.Attribute.Value attributeValue(Object value) {
        ConceptProto.Attribute.Value.Builder builder = ConceptProto.Attribute.Value.newBuilder();
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
            throw new GraknException(UNRECOGNISED_CONCEPT.message("attribute value", value));
        }
        return builder.build();
    }

    public static ConceptProto.AttributeType.VALUE_TYPE valueType(ValueType valueType) {
        switch (valueType) {
            case OBJECT:
                return ConceptProto.AttributeType.VALUE_TYPE.OBJECT;
            case STRING:
                return ConceptProto.AttributeType.VALUE_TYPE.STRING;
            case BOOLEAN:
                return ConceptProto.AttributeType.VALUE_TYPE.BOOLEAN;
            case LONG:
                return ConceptProto.AttributeType.VALUE_TYPE.LONG;
            case DOUBLE:
                return ConceptProto.AttributeType.VALUE_TYPE.DOUBLE;
            case DATETIME:
                return ConceptProto.AttributeType.VALUE_TYPE.DATETIME;
            default:
                return ConceptProto.AttributeType.VALUE_TYPE.UNRECOGNIZED;
        }
    }

    public static ByteString iid(final String iid) {
        return ByteString.copyFrom(hexStringToBytes(iid));
    }

    private static ConceptProto.Thing.SCHEMA schema(final Thing thing) {
        if (thing instanceof Entity) {
            return ConceptProto.Thing.SCHEMA.ENTITY;
        } else if (thing instanceof Relation) {
            return ConceptProto.Thing.SCHEMA.RELATION;
        } else if (thing instanceof Attribute) {
            return ConceptProto.Thing.SCHEMA.ATTRIBUTE;
        } else {
            return ConceptProto.Thing.SCHEMA.UNRECOGNIZED;
        }
    }

    private static ConceptProto.Type.SCHEMA schema(final Type type) {
        if (type instanceof EntityType) {
            return ConceptProto.Type.SCHEMA.ENTITY_TYPE;
        } else if (type instanceof RelationType) {
            return ConceptProto.Type.SCHEMA.RELATION_TYPE;
        } else if (type instanceof AttributeType) {
            return ConceptProto.Type.SCHEMA.ATTRIBUTE_TYPE;
        } else if (type instanceof RoleType) {
            return ConceptProto.Type.SCHEMA.ROLE_TYPE;
        } else if (type instanceof Rule) {
            return ConceptProto.Type.SCHEMA.RULE;
        } else if (type instanceof ThingType) {
            return ConceptProto.Type.SCHEMA.THING_TYPE;
        } else {
            return ConceptProto.Type.SCHEMA.UNRECOGNIZED;
        }
    }
}
