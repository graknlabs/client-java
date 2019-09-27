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
import grakn.protocol.session.ConceptProto;

import javax.annotation.CheckReturnValue;

/**
 * Client implementation of a MetaType, a special type of Type
 * TODO: This class is not defined in Concept API, and at server side implementation.
 * TODO: we should remove this class, or implement properly on server side.
 */
public class EntityType extends Type<EntityType, Entity> {

    private EntityType(GraknClient.Transaction tx, ConceptId id) {
        super(tx, id);
    }

    static EntityType construct(GraknClient.Transaction tx, ConceptId id) {
        return new EntityType(tx, id);
    }

    public final Entity create() {
        ConceptProto.Method.Req method = ConceptProto.Method.Req.newBuilder()
                .setEntityTypeCreateReq(ConceptProto.EntityType.Create.Req.getDefaultInstance()).build();

        Concept concept = Concept.of(runMethod(method).getEntityTypeCreateRes().getEntity(), tx());
        return asInstance(concept);
    }

    @Override
    final EntityType asCurrentBaseType(Concept other) {
        return other.asEntityType();
    }

    @Override
    final boolean equalsCurrentBaseType(Concept other) {
        return other.isEntityType();
    }

    @Override
    protected final Entity asInstance(Concept concept) {
        return concept.asEntity();
    }

    @Deprecated
    @CheckReturnValue
    @Override
    public EntityType asEntityType() {
        return this;
    }

    @Deprecated
    @CheckReturnValue
    @Override
    public boolean isEntityType() {
        return true;
    }
}
