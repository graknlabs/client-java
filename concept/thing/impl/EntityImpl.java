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

import grakn.client.Grakn;
import grakn.client.concept.thing.Entity;
import grakn.client.concept.type.impl.EntityTypeImpl;
import grakn.common.collection.Bytes;
import grakn.protocol.ConceptProto;

public class EntityImpl extends ThingImpl implements Entity {

    EntityImpl(final String iid) {
        super(iid);
    }

    public static EntityImpl of(final ConceptProto.Thing protoThing) {
        return new EntityImpl(Bytes.bytesToHexString(protoThing.getIid().toByteArray()));
    }

    @Override
    public EntityImpl.Remote asRemote(final Grakn.Transaction transaction) {
        return new EntityImpl.Remote(transaction, getIID());
    }

    @Override
    public final EntityImpl asEntity() {
        return this;
    }

    public static class Remote extends ThingImpl.Remote implements Entity.Remote {

        public Remote(final Grakn.Transaction transaction, final String iid) {
            super(transaction, iid);
        }

        public static EntityImpl.Remote of(final Grakn.Transaction transaction, final ConceptProto.Thing protoThing) {
            return new EntityImpl.Remote(transaction, Bytes.bytesToHexString(protoThing.getIid().toByteArray()));
        }

        @Override
        public EntityImpl.Remote asRemote(final Grakn.Transaction transaction) {
            return new EntityImpl.Remote(transaction, getIID());
        }

        @Override
        public EntityTypeImpl getType() {
            return super.getType().asEntityType();
        }

        @Override
        public final EntityImpl.Remote asEntity() {
            return this;
        }
    }
}
