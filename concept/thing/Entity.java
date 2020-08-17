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
import grakn.client.concept.thing.impl.EntityImpl;
import grakn.client.concept.type.EntityType;

import javax.annotation.CheckReturnValue;

/**
 * An instance of Entity Type EntityType
 * This represents an entity in the graph.
 * Entities are objects which are defined by their Attribute and their links to
 * other entities via Relation
 */
public interface Entity extends Thing {

    /**
     * @return The EntityType of this Entity
     * @see EntityType
     */
    @Override
    EntityType getType();

    @CheckReturnValue
    @Override
    default Remote asRemote(Transaction tx) {
        return Entity.Remote.of(tx, getIID());
    }

    interface Local extends Thing.Local, Entity {

        @CheckReturnValue
        @Override
        default Entity.Local asEntity() {
            return this;
        }
    }

    /**
     * An instance of Entity Type EntityType
     * This represents an entity in the graph.
     * Entities are objects which are defined by their Attribute and their links to
     * other entities via Relation
     */
    interface Remote extends Thing.Remote, Entity {

        static Entity.Remote of(Transaction tx, ConceptIID iid) {
            return new EntityImpl.Remote(tx, iid);
        }

        /**
         * @return The EntityType of this Entity
         * @see EntityType.Remote
         */
        @Override
        EntityType.Remote getType();

        @CheckReturnValue
        @Override
        default Entity.Remote asEntity() {
            return this;
        }
    }
}
