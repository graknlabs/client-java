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

package grakn.client.concept;

import grakn.client.Grakn.Transaction;
import grakn.client.concept.type.impl.RuleImpl;
import grakn.client.concept.thing.impl.AttributeImpl;
import grakn.client.concept.thing.impl.EntityImpl;
import grakn.client.concept.thing.impl.RelationImpl;
import grakn.client.concept.type.Rule;
import grakn.client.concept.type.Type;
import grakn.client.concept.type.impl.AttributeTypeImpl;
import grakn.client.concept.type.impl.EntityTypeImpl;
import grakn.client.concept.thing.Attribute;
import grakn.client.concept.thing.Entity;
import grakn.client.concept.thing.Relation;
import grakn.client.concept.thing.Thing;
import grakn.client.concept.type.AttributeType;
import grakn.client.concept.type.EntityType;
import grakn.client.concept.type.impl.MetaTypeImpl;
import grakn.client.concept.type.RelationType;
import grakn.client.concept.type.impl.RelationTypeImpl;
import grakn.client.concept.type.RoleType;
import grakn.client.concept.type.impl.RoleTypeImpl;
import grakn.client.concept.type.ThingType;
import grakn.protocol.ConceptProto;

import javax.annotation.CheckReturnValue;

/**
 * The base concept implementation.
 * A concept which can be every object in the graph.
 * This class forms the basis of assuring the graph follows the Grakn object model.
 * It provides methods to retrieve information about the Concept, and determine if it is a Type
 * (EntityType, RoleType, RelationType, Rule or AttributeType)
 * or an Thing (Entity, Relation, Attribute).
 */
public interface Concept {

    /**
     * Return as a Type if the Concept is a Type.
     *
     * @return A Type if the Concept is a Type
     */
    @CheckReturnValue
    Type asType();

    /**
     * Return as a ThingType if the Concept is a ThingType.
     *
     * @return A ThingType if the Concept is a ThingType
     */
    @CheckReturnValue
    ThingType asThingType();

    /**
     * Return as an EntityType if the Concept is an EntityType.
     *
     * @return A EntityType if the Concept is an EntityType
     */
    @CheckReturnValue
    EntityType asEntityType();

    /**
     * Return as an AttributeType if the Concept is an AttributeType
     *
     * @return An AttributeType if the Concept is an AttributeType
     */
    @CheckReturnValue
    AttributeType asAttributeType();

    /**
     * Return as a RelationType if the Concept is a RelationType.
     *
     * @return A RelationType if the Concept is a RelationType
     */
    @CheckReturnValue
    RelationType asRelationType();

    /**
     * Return as a RoleType if the Concept is a RoleType.
     *
     * @return A RoleType if the Concept is a RoleType
     */
    @CheckReturnValue
    RoleType asRoleType();

    /**
     * Return as a Thing if the Concept is a Thing.
     *
     * @return A Thing if the Concept is a Thing
     */
    @CheckReturnValue
    Thing asThing();

    /**
     * Return as an Entity, if the Concept is an Entity Thing.
     *
     * @return An Entity if the Concept is a Thing
     */
    @CheckReturnValue
    Entity asEntity();

    /**
     * Return as a Attribute  if the Concept is a Attribute Thing.
     *
     * @return A Attribute if the Concept is a Attribute
     */
    @CheckReturnValue
    Attribute asAttribute();

    /**
     * Return as a Relation if the Concept is a Relation Thing.
     *
     * @return A Relation  if the Concept is a Relation
     */
    @CheckReturnValue
    Relation asRelation();

    /**
     * Return as a Rule if the Concept is a Rule.
     *
     * @return A Rule if the Concept is a Rule
     */
    @CheckReturnValue
    Rule asRule();

    /**
     * Return a Concept.Remote for this Concept.
     *
     * @param tx The transaction to use for the RPCs.
     * @return A remote concept using the given transaction to enable RPCs.
     */
    Remote asRemote(Transaction tx);

    /**
     * Determine if the Concept is remote.
     *
     * @return true if the Concept is remote.
     */
    default boolean isRemote() {
        return false;
    }

    interface Local extends Concept {

        static Local of(ConceptProto.Concept concept) {
            switch (concept.getBaseType()) {
                case ENTITY:
                    return new EntityImpl.Local(concept);
                case RELATION:
                    return new RelationImpl.Local(concept);
                case ATTRIBUTE:
                    switch (concept.getValueTypeRes().getValueType()) {
                        case BOOLEAN:
                            return new AttributeImpl.Boolean.Local(concept);
                        case LONG:
                            return new AttributeImpl.Long.Local(concept);
                        case DOUBLE:
                            return new AttributeImpl.Double.Local(concept);
                        case STRING:
                            return new AttributeImpl.String.Local(concept);
                        case DATETIME:
                            return new AttributeImpl.DateTime.Local(concept);
                        default:
                        case UNRECOGNIZED:
                            throw new IllegalArgumentException("Unrecognised value type " + concept.getValueTypeRes().getValueType() + " for concept " + concept);
                    }
                case ENTITY_TYPE:
                    return new EntityTypeImpl.Local(concept);
                case RELATION_TYPE:
                    return new RelationTypeImpl.Local(concept);
                case ATTRIBUTE_TYPE:
                    switch (concept.getValueTypeRes().getValueType()) {
                        case BOOLEAN:
                            return new AttributeTypeImpl.Boolean.Local(concept);
                        case LONG:
                            return new AttributeImpl.Long.Local(concept);
                        case DOUBLE:
                            return new AttributeImpl.Double.Local(concept);
                        case STRING:
                            return new AttributeImpl.String.Local(concept);
                        case DATETIME:
                            return new AttributeImpl.DateTime.Local(concept);
                        default:
                        case UNRECOGNIZED:
                            throw new IllegalArgumentException("Unrecognised value type " + concept.getValueTypeRes().getValueType() + " for concept " + concept);
                    }
                case ROLE_TYPE:
                    return new RoleTypeImpl.Local(concept);
                case RULE:
                    return new RuleImpl.Local(concept);
                case META_TYPE:
                    return new MetaTypeImpl.Local(concept);
                default:
                case UNRECOGNIZED:
                    throw new IllegalArgumentException("Unrecognised " + concept);
            }
        }

        /**
         * Return as a Type if the Concept is a Type.
         *
         * @return A Type if the Concept is a Type
         */
        @CheckReturnValue
        @Override
        default Type.Local asType() {
            throw GraknConceptException.invalidCasting(this, Type.class);
        }

        /**
         * Return as a ThingType if the Concept is a ThingType.
         *
         * @return A ThingType if the Concept is a ThingType
         */
        @CheckReturnValue
        @Override
        default ThingType.Local asThingType() {
            throw GraknConceptException.invalidCasting(this, ThingType.class);
        }

        /**
         * Return as an EntityType if the Concept is an EntityType.
         *
         * @return A EntityType if the Concept is an EntityType
         */
        @CheckReturnValue
        @Override
        default EntityType.Local asEntityType() {
            throw GraknConceptException.invalidCasting(this, EntityType.class);
        }

        /**
         * Return as an AttributeType if the Concept is an AttributeType
         *
         * @return An AttributeType if the Concept is an AttributeType
         */
        @CheckReturnValue
        @Override
        default AttributeType.Local asAttributeType() {
            throw GraknConceptException.invalidCasting(this, AttributeType.class);
        }

        /**
         * Return as a RelationType if the Concept is a RelationType.
         *
         * @return A RelationType if the Concept is a RelationType
         */
        @CheckReturnValue
        @Override
        default RelationType.Local asRelationType() {
            throw GraknConceptException.invalidCasting(this, RelationType.class);
        }

        /**
         * Return as a RoleType if the Concept is a RoleType.
         *
         * @return A RoleType if the Concept is a RoleType
         */
        @CheckReturnValue
        @Override
        default RoleType.Local asRoleType() {
            throw GraknConceptException.invalidCasting(this, RoleType.class);
        }

        /**
         * Return as a Thing if the Concept is a Thing.
         *
         * @return A Thing if the Concept is a Thing
         */
        @CheckReturnValue
        @Override
        default Thing.Local asThing() {
            throw GraknConceptException.invalidCasting(this, Thing.class);
        }

        /**
         * Return as an Entity, if the Concept is an Entity Thing.
         *
         * @return An Entity if the Concept is a Thing
         */
        @CheckReturnValue
        @Override
        default Entity.Local asEntity() {
            throw GraknConceptException.invalidCasting(this, Entity.class);
        }

        /**
         * Return as a Attribute  if the Concept is a Attribute Thing.
         *
         * @return A Attribute if the Concept is a Attribute
         */
        @CheckReturnValue
        @Override
        default Attribute.Local asAttribute() {
            throw GraknConceptException.invalidCasting(this, Attribute.class);
        }

        /**
         * Return as a Relation if the Concept is a Relation Thing.
         *
         * @return A Relation  if the Concept is a Relation
         */
        @CheckReturnValue
        @Override
        default Relation.Local asRelation() {
            throw GraknConceptException.invalidCasting(this, Relation.class);
        }

        /**
         * Return as a Rule if the Concept is a Rule.
         *
         * @return A Rule if the Concept is a Rule
         */
        @CheckReturnValue
        @Override
        default Rule.Local asRule() {
            throw GraknConceptException.invalidCasting(this, Rule.class);
        }
    }

    /**
     * The base remote concept implementation.
     *
     * Provides the basic RPCs to delete a concept and check if it is deleted.
     */
    interface Remote extends Concept {

        static Remote of(Transaction tx, ConceptProto.Concept concept) {
            ConceptIID iid = ConceptIID.of(concept.getIid());
            switch (concept.getBaseType()) {
                case ENTITY:
                    return new EntityImpl.Remote(tx, iid);
                case RELATION:
                    return new RelationImpl.Remote(tx, iid);
                case ATTRIBUTE:
                    switch (concept.getValueTypeRes().getValueType()) {
                        case BOOLEAN:
                            return new AttributeImpl.Boolean.Remote(tx, iid);
                        case LONG:
                            return new AttributeImpl.Long.Remote(tx, iid);
                        case DOUBLE:
                            return new AttributeImpl.Double.Remote(tx, iid);
                        case STRING:
                            return new AttributeImpl.String.Remote(tx, iid);
                        case DATETIME:
                            return new AttributeImpl.DateTime.Remote(tx, iid);
                        default:
                        case UNRECOGNIZED:
                            throw new IllegalArgumentException("Unrecognised value type " + concept.getValueTypeRes().getValueType() + " for concept " + concept);
                    }
                case ENTITY_TYPE:
                    return new EntityTypeImpl.Remote(tx, iid);
                case RELATION_TYPE:
                    return new RelationTypeImpl.Remote(tx, iid);
                case ATTRIBUTE_TYPE:
                    switch (concept.getValueTypeRes().getValueType()) {
                        case BOOLEAN:
                            return new AttributeTypeImpl.Boolean.Remote(tx, iid);
                        case LONG:
                            return new AttributeTypeImpl.Long.Remote(tx, iid);
                        case DOUBLE:
                            return new AttributeTypeImpl.Double.Remote(tx, iid);
                        case STRING:
                            return new AttributeTypeImpl.String.Remote(tx, iid);
                        case DATETIME:
                            return new AttributeTypeImpl.DateTime.Remote(tx, iid);
                        default:
                        case UNRECOGNIZED:
                            throw new IllegalArgumentException("Unrecognised value type " + concept.getValueTypeRes().getValueType() + " for concept " + concept);
                    }
                case ROLE_TYPE:
                    return new RoleTypeImpl.Remote(tx, iid);
                case RULE:
                    return new RuleImpl.Remote(tx, iid);
                case META_TYPE:
                    return new MetaTypeImpl.Remote(tx, iid);
                default:
                case UNRECOGNIZED:
                    throw new IllegalArgumentException("Unrecognised " + concept);
            }
        }

        /**
         * Delete the Concept
         */
        void delete();

        /**
         * Return whether the concept has been deleted.
         */
        boolean isDeleted();

        /**
         * Return as a SchemaConcept if the Concept is a SchemaConcept.
         *
         * @return A SchemaConcept if the Concept is a SchemaConcept
         */
        @Override
        @CheckReturnValue
        default Type.Remote asType() {
            throw GraknConceptException.invalidCasting(this, Type.Remote.class);
        }

        /**
         * Return as a Type if the Concept is a Type.
         *
         * @return A Type if the Concept is a Type
         */
        @Override
        @CheckReturnValue
        default ThingType.Remote asThingType() {
            throw GraknConceptException.invalidCasting(this, ThingType.Remote.class);
        }

        /**
         * Return as an Thing if the Concept is an Thing.
         *
         * @return An Thing if the Concept is an Thing
         */
        @Override
        @CheckReturnValue
        default Thing.Remote asThing() {
            throw GraknConceptException.invalidCasting(this, Thing.Remote.class);
        }

        /**
         * Return as an EntityType if the Concept is an EntityType.
         *
         * @return A EntityType if the Concept is an EntityType
         */
        @Override
        @CheckReturnValue
        default EntityType.Remote asEntityType() {
            throw GraknConceptException.invalidCasting(this, EntityType.Remote.class);
        }

        /**
         * Return as a RoleType if the Concept is a RoleType.
         *
         * @return A RoleType if the Concept is a RoleType
         */
        @Override
        @CheckReturnValue
        default RoleType.Remote asRoleType() {
            throw GraknConceptException.invalidCasting(this, RoleType.Remote.class);
        }

        /**
         * Return as a RelationType if the Concept is a RelationType.
         *
         * @return A RelationType if the Concept is a RelationType
         */
        @Override
        @CheckReturnValue
        default RelationType.Remote asRelationType() {
            throw GraknConceptException.invalidCasting(this, RelationType.Remote.class);
        }

        /**
         * Return as a AttributeType if the Concept is a AttributeType
         *
         * @return A AttributeType if the Concept is a AttributeType
         */
        @Override
        @CheckReturnValue
        default AttributeType.Remote asAttributeType() {
            throw GraknConceptException.invalidCasting(this, AttributeType.class);
        }

        /**
         * Return as a Rule if the Concept is a Rule.
         *
         * @return A Rule if the Concept is a Rule
         */
        @Override
        @CheckReturnValue
        default Rule.Remote asRule() {
            throw GraknConceptException.invalidCasting(this, Rule.Remote.class);
        }

        /**
         * Return as an Entity, if the Concept is an Entity Thing.
         *
         * @return An Entity if the Concept is a Thing
         */
        @Override
        @CheckReturnValue
        default Entity.Remote asEntity() {
            throw GraknConceptException.invalidCasting(this, Entity.Remote.class);
        }

        /**
         * Return as a Relation if the Concept is a Relation Thing.
         *
         * @return A Relation  if the Concept is a Relation
         */
        @Override
        @CheckReturnValue
        default Relation.Remote asRelation() {
            throw GraknConceptException.invalidCasting(this, Relation.Remote.class);
        }

        /**
         * Return as a Attribute  if the Concept is a Attribute Thing.
         *
         * @return A Attribute if the Concept is a Attribute
         */
        @Override
        @CheckReturnValue
        default Attribute.Remote asAttribute() {
            throw GraknConceptException.invalidCasting(this, Attribute.Remote.class);
        }

        @Override
        default boolean isRemote() {
            return true;
        }
    }
}
