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

import grakn.client.Grakn.Transaction;
import grakn.client.concept.Concept;
import grakn.client.concept.ConceptIID;
import grakn.client.concept.type.RoleType;
import grakn.client.concept.type.Rule;
import grakn.client.concept.type.AttributeType;
import grakn.client.concept.type.ThingType;

import javax.annotation.CheckReturnValue;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * A data instance in the graph belonging to a specific Type
 * Instances represent data in the graph.
 * Every instance belongs to a Type which serves as a way of categorising them.
 * Instances can relate to one another via Relation
 */
public interface Thing extends Concept {

    /**
     * Get the unique IID associated with the Thing.
     *
     * @return The thing's unique IID.
     */
    @CheckReturnValue
    ConceptIID getIID();

    /**
     * Return the Type of the Concept.
     *
     * @return A Type which is the type of this concept. This concept is an instance of that type.
     */
    @CheckReturnValue
    ThingType getType();

    /**
     * Used to indicate if this Thing has been created as the result of a Rule inference.
     *
     * @return true if this Thing exists due to a rule
     * @see Rule
     */
    boolean isInferred();

    interface Local extends Concept.Local, Thing {

        @CheckReturnValue
        @Override
        default Thing.Local asThing() {
            return this;
        }
    }

    /**
     * A data instance in the graph belonging to a specific Type
     * Instances represent data in the graph.
     * Every instance belongs to a Type which serves as a way of categorising them.
     * Instances can relate to one another via Relation
     */
    interface Remote extends Concept.Remote, Thing {

        /**
         * Creates an ownership from this Thing to the provided Attribute.
         *
         * @param attribute The Attribute to which an ownership is created
         */
        void setHas(Attribute attribute);

        /**
         * Removes the provided Attribute from this Thing
         *
         * @param attribute the Attribute to be removed
         */
        void unsetHas(Attribute attribute);

        /**
         * Return the Type of the Concept.
         *
         * @return A Type which is the type of this concept. This concept is an instance of that type.
         */
        @Override
        @CheckReturnValue
        ThingType.Remote getType();

        /**
         * Retrieves a collection of Attribute attached to this Thing, possibly specifying only keys.
         *
         * @param onlyKey If true, only fetch attributes which are keys.
         * @return A collection of Attributes attached to this Thing.
         * @see Attribute.Remote
         */
        @CheckReturnValue
        Stream<? extends Attribute.Remote> getHas(boolean onlyKey);

        @CheckReturnValue
        Stream<? extends Attribute.Boolean.Remote> getHas(AttributeType.Boolean attributeType);

        @CheckReturnValue
        Stream<? extends Attribute.Long.Remote> getHas(AttributeType.Long attributeType);

        @CheckReturnValue
        Stream<? extends Attribute.Double.Remote> getHas(AttributeType.Double attributeType);

        @CheckReturnValue
        Stream<? extends Attribute.String.Remote> getHas(AttributeType.String attributeType);

        @CheckReturnValue
        Stream<? extends Attribute.DateTime.Remote> getHas(AttributeType.DateTime attributeType);

        /**
         * Retrieves a collection of Attribute attached to this Thing
         *
         * @param attributeTypes AttributeTypes of the Attributes attached to this entity
         * @return A collection of Attributes attached to this Thing.
         * @see Attribute.Remote
         */
        @CheckReturnValue
        Stream<? extends Attribute.Remote> getHas(AttributeType... attributeTypes);

        /**
         * Determine the RoleTypes that this Thing is currently playing.
         *
         * @return A set of all the RoleTypes which this Thing is currently playing.
         * @see RoleType.Remote
         */
        @CheckReturnValue
        Stream<? extends RoleType.Remote> getPlays();

        /**
         * Get all {@code Relation} instances that this {@code Thing} is playing any of the specified roles in.
         * If no roles are specified, all Relations are retrieved regardless of role.
         *
         * @param roleTypes The role types that this {@code Thing} can play
         * @return a stream of {@code Relation} that this {@code Thing} plays a specified role in
         */
        Stream<? extends Relation> getRelations(RoleType... roleTypes);

        @CheckReturnValue
        @Override
        default Thing.Remote asThing() {
            return this;
        }
    }
}
