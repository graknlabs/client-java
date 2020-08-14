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

import grakn.client.Grakn.Transaction;
import grakn.client.concept.Concept;
import grakn.client.concept.Label;

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
public interface Type extends Concept {
    //------------------------------------- Accessors ---------------------------------

    /**
     * Returns the unique label of this Type.
     *
     * @return The unique label of this type
     */
    @CheckReturnValue
    Label getLabel();

    //------------------------------------- Other ---------------------------------
    @Deprecated
    @CheckReturnValue
    @Override
    default Type asType() {
        return this;
    }

    @Override
    Remote asRemote(Transaction tx);

    interface Local extends Type, Concept.Local {
    }

    /**
     * Facilitates construction of ontological elements.
     * Allows you to create schema or ontological elements.
     * These differ from normal graph constructs in two ways:
     * 1. They have a unique Label which identifies them
     * 2. You can link them together into a hierarchical structure
     */
    interface Remote extends Type, Concept.Remote {
        //------------------------------------- Modifiers ----------------------------------

        /**
         * Changes the Label of this Concept to a new one.
         *
         * @param label The new Label.
         * @return The Concept itself
         */
        Type.Remote setLabel(Label label);

        //------------------------------------- Accessors ---------------------------------

        /**
         * @return The direct supertype of this concept
         */
        @CheckReturnValue
        @Nullable
        Type.Remote getSupertype();

        /**
         * @return All super-concepts of this SchemaConcept, including itself and excluding the meta type THING.
         * If you want to include THING, use Transaction.sups().
         */
        Stream<? extends Type.Remote> getSupertypes();

        /**
         * Get all indirect subs of this concept.
         * The indirect subs are the concept itself and all indirect subs of direct subs.
         *
         * @return All the indirect sub-types of this SchemaConcept
         */
        @CheckReturnValue
        Stream<? extends Type.Remote> getSubtypes();


        //------------------------------------- Other ---------------------------------
        @Deprecated
        @CheckReturnValue
        @Override
        default Type.Remote asType() {
            return this;
        }

    }
}
