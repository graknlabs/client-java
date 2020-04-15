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

import grakn.client.GraknClient;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.util.stream.Stream;

/**
 * Facilitates construction of ontological elements.
 * Allows you to create schema or ontological elements.
 * These differ from normal graph constructs in two ways:
 * 1. They have a unique Label which identifies them
 * 2. You can link them together into a hierarchical structure
 */
public interface SchemaConcept<BaseType extends SchemaConcept<BaseType>> extends Concept<BaseType> {
    //------------------------------------- Accessors ---------------------------------

    /**
     * Returns the unique label of this Type.
     *
     * @return The unique label of this type
     */
    @CheckReturnValue
    Label label();

    //------------------------------------- Other ---------------------------------
    @Deprecated
    @CheckReturnValue
    @Override
    default SchemaConcept<BaseType> asSchemaConcept() {
        return this;
    }

    @Override
    Remote<BaseType> asRemote(GraknClient.Transaction tx);

    @Deprecated
    @CheckReturnValue
    @Override
    default boolean isSchemaConcept() {
        return true;
    }

    interface Local<T extends SchemaConcept<T>> extends SchemaConcept<T>, Concept.Local<T> {
    }

    /**
     * Facilitates construction of ontological elements.
     * Allows you to create schema or ontological elements.
     * These differ from normal graph constructs in two ways:
     * 1. They have a unique Label which identifies them
     * 2. You can link them together into a hierarchical structure
     */
    interface Remote<BaseType extends SchemaConcept<BaseType>>
            extends SchemaConcept<BaseType>,
            Concept.Remote<BaseType> {
        //------------------------------------- Modifiers ----------------------------------

        /**
         * Changes the Label of this Concept to a new one.
         *
         * @param label The new Label.
         * @return The Concept itself
         */
        SchemaConcept.Remote<BaseType> label(Label label);

        //------------------------------------- Accessors ---------------------------------

        /**
         * @return The direct super of this concept
         */
        @CheckReturnValue
        @Nullable
        SchemaConcept.Remote<BaseType> sup();

        /**
         * @return All super-concepts of this SchemaConcept  including itself and excluding the meta
         * Schema.MetaSchema#THING.
         * If you want to include Schema.MetaSchema#THING, use Transaction.sups().
         */
        Stream<? extends SchemaConcept.Remote<BaseType>> sups();

        /**
         * Get all indirect subs of this concept.
         * The indirect subs are the concept itself and all indirect subs of direct subs.
         *
         * @return All the indirect sub-types of this SchemaConcept
         */
        @CheckReturnValue
        Stream<? extends SchemaConcept.Remote<BaseType>> subs();

        /**
         * Return whether the SchemaConcept was created implicitly.
         * By default, SchemaConcept are not implicit.
         *
         * @return returns true if the type was created implicitly through the Attribute syntax
         */
        @CheckReturnValue
        Boolean isImplicit();


        //------------------------------------- Other ---------------------------------
        @Deprecated
        @CheckReturnValue
        @Override
        default SchemaConcept.Remote<BaseType> asSchemaConcept() {
            return this;
        }

        @Deprecated
        @CheckReturnValue
        @Override
        default boolean isSchemaConcept() {
            return true;
        }
    }
}
