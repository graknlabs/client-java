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

import grakn.client.GraknClient;
import grakn.client.concept.Concept;
import grakn.client.concept.ConceptIID;
import grakn.client.concept.Label;
import grakn.client.concept.thing.Relation;
import grakn.client.concept.type.AttributeType;
import grakn.client.concept.type.RelationType;
import grakn.client.concept.type.RoleType;
import grakn.protocol.ConceptProto;

import java.util.stream.Stream;

public class RelationTypeImpl {
    /**
     * Client implementation of RelationType
     */
    public static class Local extends ThingTypeImpl.Local<RelationType, Relation> implements RelationType.Local {

        public Local(ConceptProto.Concept concept) {
            super(concept);
        }
    }

    /**
     * Client implementation of RelationType
     */
    public static class Remote extends ThingTypeImpl.Remote<RelationType, Relation> implements RelationType.Remote {

        public Remote(GraknClient.Transaction tx, ConceptIID iid) {
            super(tx, iid);
        }

        @Override
        public final RelationType.Remote has(AttributeType<?> attributeType) {
            return (RelationType.Remote) super.has(attributeType);
        }

        @Override
        public final RelationType.Remote has(AttributeType<?> attributeType, boolean isKey) {
            return (RelationType.Remote) super.has(attributeType, isKey);
        }

        @Override
        public final RelationType.Remote has(AttributeType<?> attributeType, AttributeType<?> overriddenType) {
            return (RelationType.Remote) super.has(attributeType, overriddenType);
        }

        @Override
        public final RelationType.Remote has(AttributeType<?> attributeType, AttributeType<?> overriddenType, boolean isKey) {
            return (RelationType.Remote) super.has(attributeType, overriddenType, isKey);
        }

        @Override
        public Stream<? extends AttributeType.Remote<?>> attributes(boolean keysOnly) {
            return super.attributes(keysOnly);
        }

        @Override
        public final RelationType.Remote plays(RoleType role) {
            return (RelationType.Remote) super.plays(role);
        }

        @Override
        public final RelationType.Remote unhas(AttributeType<?> attributeType) {
            return (RelationType.Remote) super.unhas(attributeType);
        }

        @Override
        public final RelationType.Remote unplay(RoleType role) {
            return (RelationType.Remote) super.unplay(role);
        }

        @Override
        public final RelationType.Remote isAbstract(Boolean isAbstract) {
            return (RelationType.Remote) super.isAbstract(isAbstract);
        }

        @Override
        public final Stream<Relation.Remote> instances() {
            return super.instances().map(this::asInstance);
        }

        @Override
        public final Stream<RelationType.Remote> sups() {
            return super.sups().map(this::asCurrentBaseType);
        }

        @Override
        public final Stream<RelationType.Remote> subs() {
            return super.subs().map(this::asCurrentBaseType);
        }

        @Override
        public final RelationType.Remote label(Label label) {
            return (RelationType.Remote) super.label(label);
        }

        @Override
        public final RelationType.Remote sup(RelationType type) {
            return (RelationType.Remote) super.sup(type);
        }

        @Override
        public final Relation.Remote create() {
            ConceptProto.Method.Req method = ConceptProto.Method.Req.newBuilder()
                    .setRelationTypeCreateReq(ConceptProto.RelationType.Create.Req.getDefaultInstance()).build();

            return Concept.Remote.of(tx(), runMethod(method).getRelationTypeCreateRes().getRelation());
        }

        @Override
        public final RoleType.Remote role(Label role) {
            ConceptProto.Method.Req method = ConceptProto.Method.Req.newBuilder()
                    .setRelationTypeRoleReq(ConceptProto.RelationType.Role.Req.newBuilder().setLabel(role.getValue())).build();

            return Concept.Remote.of(tx(), runMethod(method).getRelationTypeRoleRes().getRole());
        }

        @Override
        public final Stream<RoleType.Remote> roles() {
            ConceptProto.Method.Iter.Req method = ConceptProto.Method.Iter.Req.newBuilder()
                    .setRelationTypeRolesIterReq(ConceptProto.RelationType.Roles.Iter.Req.getDefaultInstance()).build();

            return conceptStream(method, res -> res.getRelationTypeRolesIterRes().getRole()).map(Concept.Remote::asRoleType);
        }

        @Override
        public final RoleType.Remote relates(Label role) {
            ConceptProto.Method.Req method = ConceptProto.Method.Req.newBuilder()
                    .setRelationTypeRelatesReq(ConceptProto.RelationType.Relates.Req.newBuilder()
                                                       .setLabel(role.getValue())).build();

            runMethod(method);
            return Concept.Remote.of(tx(), runMethod(method).getRelationTypeRelatesRes().getRole());
        }

        @Override
        protected final Relation.Remote asInstance(Concept.Remote<?> concept) {
            return concept.asRelation();
        }

        @Override
        protected final RelationType.Remote asCurrentBaseType(Concept.Remote<?> other) {
            return other.asRelationType();
        }

        @Override
        protected final boolean equalsCurrentBaseType(Concept.Remote<?> other) {
            return other.isRelationType();
        }

    }
}
