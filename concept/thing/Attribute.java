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
import grakn.client.concept.ConceptIID;
import grakn.client.concept.GraknConceptException;
import grakn.client.concept.thing.impl.AttributeImpl;
import grakn.client.concept.type.AttributeType;
import grakn.client.concept.type.ThingType;

import javax.annotation.CheckReturnValue;
import java.util.stream.Stream;

public interface Attribute extends Thing {

    @CheckReturnValue
    @Override
    default Attribute asAttribute() {
        return this;
    }

    /**
     * Retrieves the type of the Attribute, that is, the AttributeType of which this resource is a Thing.
     *
     * @return The AttributeType of which this resource is a Thing.
     */
    @Override
    AttributeType getType();

    @CheckReturnValue
    Attribute.Boolean asBoolean();

    @CheckReturnValue
    Attribute.Long asLong();

    @CheckReturnValue
    Attribute.Double asDouble();

    @CheckReturnValue
    Attribute.String asString();

    @CheckReturnValue
    Attribute.DateTime asDateTime();

    @CheckReturnValue
    @Override
    default Remote asRemote(Transaction tx) {
        return Attribute.Remote.of(tx, getIID());
    }

    /**
     * Represent a literal Attribute in the graph.
     * Acts as a Thing when relating to other instances except it has the added functionality of:
     * 1. It is unique to its AttributeType based on its value.
     * 2. It has an AttributeType.ValueType associated with it which constrains the allowed values.
     */
    interface Local extends Thing.Local, Attribute {

        @Override
        default Attribute.Boolean.Local asBoolean() {
            throw GraknConceptException.invalidCasting(this, java.lang.Boolean.class);
        }

        @Override
        default Attribute.Long.Local asLong() {
            throw GraknConceptException.invalidCasting(this, java.lang.Long.class);
        }

        @Override
        default Attribute.Double.Local asDouble() {
            throw GraknConceptException.invalidCasting(this, java.lang.Double.class);
        }

        @Override
        default Attribute.String.Local asString() {
            throw GraknConceptException.invalidCasting(this, java.lang.String.class);
        }

        @Override
        default Attribute.DateTime.Local asDateTime() {
            throw GraknConceptException.invalidCasting(this, java.time.LocalDateTime.class);
        }
    }

    /**
     * Represent a literal Attribute in the graph.
     * Acts as an Thing when relating to other instances except it has the added functionality of:
     * 1. It is unique to its AttributeType based on it's value.
     * 2. It has an AttributeType.ValueType associated with it which constrains the allowed values.
     */
    interface Remote extends Thing.Remote, Attribute {

        static Attribute.Remote of(Transaction tx, ConceptIID iid) {
            return new AttributeImpl.Remote<>(tx, iid);
        }

        /**
         * Retrieves the type of the Attribute, that is, the AttributeType of which this resource is an Thing.
         *
         * @return The AttributeType of which this resource is an Thing.
         */
        @Override
        AttributeType.Remote getType();

        /**
         * Creates an ownership from this instance to the provided Attribute.
         *
         * @param attribute The Attribute to which an ownership is created
         * @return The instance itself
         */
        @Override
        Attribute.Remote setHas(Attribute attribute);

        /**
         * Retrieves the set of all Instances that possess this Attribute.
         *
         * @return The list of all Instances that possess this Attribute.
         */
        @CheckReturnValue
        Stream<? extends Thing.Remote> getOwners();

        // TODO: this was in core, but not in client-java - how do we align it?
        /**
         * Retrieves the set of all Instances of the specified type that possess this Attribute.
         *
         * @return The list of all Instances of the specified type that possess this Attribute.
         */
        @CheckReturnValue
        default Stream<? extends Thing.Remote> getOwners(ThingType ownerType) {
            throw new UnsupportedOperationException();
        }

        @CheckReturnValue
        @Override
        default Attribute.Remote asAttribute() {
            return this;
        }

        @Override
        default Attribute.Boolean.Remote asBoolean() {
            throw GraknConceptException.invalidCasting(this, java.lang.Boolean.class);
        }

        @Override
        default Attribute.Long.Remote asLong() {
            throw GraknConceptException.invalidCasting(this, java.lang.Long.class);
        }

        @Override
        default Attribute.Double.Remote asDouble() {
            throw GraknConceptException.invalidCasting(this, java.lang.Double.class);
        }

        @Override
        default Attribute.String.Remote asString() {
            throw GraknConceptException.invalidCasting(this, java.lang.String.class);
        }

        @Override
        default Attribute.DateTime.Remote asDateTime() {
            throw GraknConceptException.invalidCasting(this, java.time.LocalDateTime.class);
        }
    }

    interface Boolean extends Attribute {

        /**
         * Retrieves the value of the Attribute.
         *
         * @return The value itself
         */
        @CheckReturnValue
        java.lang.Boolean getValue();

        interface Local extends Attribute.Boolean, Attribute.Local {
        }

        interface Remote extends Attribute.Boolean, Attribute.Remote {
        }
    }

    interface Long extends Attribute {

        /**
         * Retrieves the value of the Attribute.
         *
         * @return The value itself
         */
        @CheckReturnValue
        java.lang.Long getValue();

        interface Local extends Attribute.Long, Attribute.Local {
        }

        interface Remote extends Attribute.Long, Attribute.Remote {
        }
    }

    interface Double extends Attribute {

        /**
         * Retrieves the value of the Attribute.
         *
         * @return The value itself
         */
        @CheckReturnValue
        java.lang.Double getValue();

        interface Local extends Attribute.Double, Attribute.Local {
        }

        interface Remote extends Attribute.Double, Attribute.Remote {
        }
    }

    interface String extends Attribute {

        /**
         * Retrieves the value of the Attribute.
         *
         * @return The value itself
         */
        @CheckReturnValue
        java.lang.String getValue();

        interface Local extends Attribute.String, Attribute.Local {
        }

        interface Remote extends Attribute.String, Attribute.Remote {
        }
    }

    interface DateTime extends Attribute {

        /**
         * Retrieves the value of the Attribute.
         *
         * @return The value itself
         */
        @CheckReturnValue
        java.time.LocalDateTime getValue();

        interface Local extends Attribute.DateTime, Attribute.Local {
        }

        interface Remote extends Attribute.DateTime, Attribute.Remote {
        }
    }
}
