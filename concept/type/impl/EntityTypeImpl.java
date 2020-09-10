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

package grakn.client.concept.type.impl;

import grakn.client.Grakn;
import grakn.client.concept.thing.impl.EntityImpl;
import grakn.client.concept.thing.impl.ThingImpl;
import grakn.client.concept.type.EntityType;
import grakn.protocol.ConceptProto;
import grakn.protocol.ConceptProto.EntityType.Create;
import grakn.protocol.ConceptProto.TypeMethod;

import java.util.stream.Stream;

public class EntityTypeImpl {

    public static class Local extends ThingTypeImpl.Local implements EntityType.Local {

        public Local(final String label, final boolean isRoot) {
            super(label, isRoot);
        }

        public static EntityTypeImpl.Local of(final ConceptProto.Type typeProto) {
            return new EntityTypeImpl.Local(typeProto.getLabel(), typeProto.getRoot());
        }

        @Override
        public EntityTypeImpl.Remote asRemote(final Grakn.Transaction transaction) {
            return new EntityTypeImpl.Remote(transaction, getLabel(), isRoot());
        }

        @Override
        public EntityTypeImpl.Local asEntityType() {
            return this;
        }
    }

    public static class Remote extends ThingTypeImpl.Remote implements EntityType.Remote {

        public Remote(final Grakn.Transaction transaction, final String label, final boolean isRoot) {
            super(transaction, label, isRoot);
        }

        public static EntityTypeImpl.Remote of(final Grakn.Transaction transaction, final ConceptProto.Type proto) {
            return new EntityTypeImpl.Remote(transaction, proto.getLabel(), proto.getRoot());
        }

        @Override
        public final void setSupertype(final EntityType superEntityType) {
            this.setSupertypeExecute(superEntityType);
        }

        @Override
        public EntityTypeImpl.Local getSupertype() {
            return super.getSupertypeExecute(TypeImpl.Local::asEntityType);
        }

        @Override
        public final Stream<EntityImpl.Local> getInstances() {
            return super.getInstances(EntityImpl.Local::of);
        }

        @Override
        public EntityTypeImpl.Remote asRemote(final Grakn.Transaction transaction) {
            return new EntityTypeImpl.Remote(transaction, getLabel(), isRoot());
        }

        @Override
        public final Stream<EntityTypeImpl.Local> getSupertypes() {
            return super.getSupertypes(TypeImpl.Local::asEntityType);
        }

        @Override
        public final Stream<EntityTypeImpl.Local> getSubtypes() {
            return super.getSubtypes(TypeImpl.Local::asEntityType);
        }

        @Override
        public final EntityImpl.Local create() {
            final TypeMethod.Req method = TypeMethod.Req.newBuilder()
                    .setEntityTypeCreateReq(Create.Req.getDefaultInstance()).build();
            return ThingImpl.Local.of(execute(method).getEntityTypeCreateRes().getEntity()).asEntity();
        }

        @Override
        public EntityTypeImpl.Remote asEntityType() {
            return this;
        }
    }
}
