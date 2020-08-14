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

package grakn.client.concept.thing.impl;

import grakn.client.Grakn.Transaction;
import grakn.client.concept.Concept;
import grakn.client.concept.ConceptIID;
import grakn.client.concept.thing.Attribute;
import grakn.client.concept.thing.Entity;
import grakn.client.concept.type.EntityType;
import grakn.protocol.ConceptProto;

public class EntityImpl {
    /**
     * Client implementation of Entity
     */
    public static class Local extends ThingImpl.Local implements Entity.Local {

        public Local(ConceptProto.Concept concept) {
            super(concept);
        }
    }

    /**
     * Client implementation of Entity
     */
    public static class Remote extends ThingImpl.Local.Remote implements Entity.Remote {

        public Remote(Transaction tx, ConceptIID iid) {
            super(tx, iid);
        }

        @Override
        public final EntityType.Remote getType() {
            return (EntityType.Remote) super.getType();
        }

        @Override
        public Entity.Remote setHas(Attribute attribute) {
            return (Entity.Remote) super.setHas(attribute);
        }

        @Override
        public Entity.Remote unsetHas(Attribute attribute) {
            return (Entity.Remote) super.unsetHas(attribute);
        }

        @Override
        protected final Entity.Remote asCurrentBaseType(Concept.Remote other) {
            return other.asEntity();
        }
    }
}
