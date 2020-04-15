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

import grakn.client.GraknClient;
import grakn.client.concept.Label;
import grakn.client.concept.SchemaConcept;
import grakn.client.concept.thing.Thing;

import javax.annotation.CheckReturnValue;
import java.util.stream.Stream;

/**
 * A Type represents any ontological element in the graph.
 * Types are used to model the behaviour of Thing and how they relate to each other.
 * They also aid in categorising Thing to different types.
 */
public interface Type<SomeType extends Type<SomeType, SomeThing>,
                      SomeThing extends Thing<SomeThing, SomeType>>
        extends SchemaConcept<SomeType> {

    @Deprecated
    @CheckReturnValue
    @Override
    default Type<SomeType, SomeThing> asType() {
        return this;
    }

    @Override
    Remote<SomeType, SomeThing> asRemote(GraknClient.Transaction tx);

    @Deprecated
    @CheckReturnValue
    @Override
    default boolean isType() {
        return true;
    }

    interface Local<
            SomeType extends Type<SomeType, SomeThing>,
            SomeThing extends Thing<SomeThing, SomeType>>
            extends SchemaConcept.Local<SomeType>, Type<SomeType, SomeThing> {
    }

    /**
     * A Type represents any ontological element in the graph.
     * Types are used to model the behaviour of Thing and how they relate to each other.
     * They also aid in categorising Thing to different types.
     */
    interface Remote<
            SomeRemoteType extends Type<SomeRemoteType, SomeRemoteThing>,
            SomeRemoteThing extends Thing<SomeRemoteThing, SomeRemoteType>>
            extends SchemaConcept.Remote<SomeRemoteType>, Type<SomeRemoteType, SomeRemoteThing> {

        //------------------------------------- Modifiers ----------------------------------

        /**
         * Changes the Label of this Concept to a new one.
         *
         * @param label The new Label.
         * @return The Concept itself
         */
        Type.Remote<SomeRemoteType, SomeRemoteThing> label(Label label);

        /**
         * Sets the Type to be abstract - which prevents it from having any instances.
         *
         * @param isAbstract Specifies if the concept is to be abstract (true) or not (false).
         * @return The concept itself
         */
        Type.Remote<SomeRemoteType, SomeRemoteThing> isAbstract(Boolean isAbstract);

        /**
         * @param role The Role Type which the instances of this Type are allowed to play.
         * @return The Type itself.
         */
        Type.Remote<SomeRemoteType, SomeRemoteThing> plays(Role role);

        /**
         * Creates a RelationType which allows this type and a AttributeType to be linked in a strictly one-to-one mapping.
         *
         * @param attributeType The AttributeType which instances of this type should be allowed to play.
         * @return The Type itself.
         */
        Type.Remote<SomeRemoteType, SomeRemoteThing> key(AttributeType<?> attributeType);

        /**
         * Creates a RelationType which allows this type and a AttributeType  to be linked.
         *
         * @param attributeType The AttributeType  which instances of this type should be allowed to play.
         * @return The Type itself.
         */
        Type.Remote<SomeRemoteType, SomeRemoteThing> has(AttributeType<?> attributeType);

        //------------------------------------- Accessors ---------------------------------

        /**
         * @return A list of Role Types which instances of this Type can indirectly play.
         */
        Stream<Role.Remote> playing();

        /**
         * @return The AttributeTypes which this Type is linked with.
         */
        @CheckReturnValue
        Stream<? extends AttributeType.Remote<?>> attributes();

        /**
         * @return The AttributeTypes which this Type is linked with as a key.
         */
        @CheckReturnValue
        Stream<? extends AttributeType.Remote<?>> keys();

        /**
         * @return All the the super-types of this Type
         */
        @Override
        Stream<? extends Type.Remote<SomeRemoteType, SomeRemoteThing>> sups();

        /**
         * Get all indirect sub-types of this type.
         * The indirect sub-types are the type itself and all indirect sub-types of direct sub-types.
         *
         * @return All the indirect sub-types of this Type
         */
        @Override
        @CheckReturnValue
        Stream<? extends Type.Remote<SomeRemoteType, SomeRemoteThing>> subs();

        /**
         * Get all indirect instances of this type.
         * The indirect instances are the direct instances and all indirect instances of direct sub-types.
         *
         * @return All the indirect instances of this type.
         */
        @CheckReturnValue
        Stream<? extends Thing.Remote<SomeRemoteThing, SomeRemoteType>> instances();

        /**
         * Return if the type is set to abstract.
         * By default, types are not abstract.
         *
         * @return returns true if the type is set to be abstract.
         */
        @CheckReturnValue
        Boolean isAbstract();

        //------------------------------------- Other ----------------------------------

        /**
         * Removes the ability of this Type to play a specific Role
         *
         * @param role The Role which the Things of this Type should no longer be allowed to play.
         * @return The Type itself.
         */
        Type.Remote<SomeRemoteType, SomeRemoteThing> unplay(Role role);

        /**
         * Removes the ability for Things of this Type to have Attributes of type AttributeType
         *
         * @param attributeType the AttributeType which this Type can no longer have
         * @return The Type itself.
         */
        Type.Remote<SomeRemoteType, SomeRemoteThing> unhas(AttributeType<?> attributeType);

        /**
         * Removes AttributeType as a key to this Type
         *
         * @param attributeType the AttributeType which this Type can no longer have as a key
         * @return The Type itself.
         */
        Type.Remote<SomeRemoteType, SomeRemoteThing> unkey(AttributeType<?> attributeType);

        @Deprecated
        @CheckReturnValue
        @Override
        default Type.Remote<SomeRemoteType, SomeRemoteThing> asType() {
            return this;
        }

        @Deprecated
        @CheckReturnValue
        @Override
        default boolean isType() {
            return true;
        }
    }
}
