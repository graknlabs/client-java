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
import grakn.client.concept.thing.impl.ThingImpl;
import grakn.client.concept.type.RoleType;
import grakn.protocol.ConceptProto;
import grakn.protocol.ConceptProto.RoleType.GetPlayers;
import grakn.protocol.ConceptProto.RoleType.GetRelation;
import grakn.protocol.ConceptProto.RoleType.GetRelations;
import grakn.protocol.ConceptProto.TypeMethod;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import static grakn.common.util.Objects.className;

public class RoleTypeImpl extends TypeImpl implements RoleType {

    private final String scope;
    private final int hash;

    public RoleTypeImpl(final String label, final String scope, final boolean root) {
        super(label, root);
        this.scope = scope;
        this.hash = Objects.hash(this.scope, label);
    }

    public static RoleTypeImpl of(final ConceptProto.Type typeProto) {
        return new RoleTypeImpl(typeProto.getLabel(), typeProto.getScope(), typeProto.getRoot());
    }

    @Override
    public final String getScope() {
        return scope;
    }

    @Override
    public RoleTypeImpl.Remote asRemote(Grakn.Transaction transaction) {
        return new RoleTypeImpl.Remote(transaction, getLabel(), getScope(), isRoot());
    }

    @Override
    public RoleTypeImpl asRoleType() {
        return this;
    }

    @Override
    public String toString() {
        return className(this.getClass()) + "[label: " + (scope != null ? scope + ":" : "") + getLabel() + "]";
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final RoleTypeImpl that = (RoleTypeImpl) o;
        return (this.getLabel().equals(that.getLabel()) && Objects.equals(this.scope, that.scope));
    }

    @Override
    public int hashCode() {
        return hash;
    }

    public static class Remote extends TypeImpl.Remote implements RoleType.Remote {

        private final String scope;
        private final int hash;

        public Remote(final Grakn.Transaction transaction, final String label,
                      final String scope, final boolean isRoot) {
            super(transaction, label, isRoot);
            this.scope = scope;
            this.hash = Objects.hash(transaction, label, scope);
        }

        public static RoleTypeImpl.Remote of(final Grakn.Transaction transaction, final ConceptProto.Type proto) {
            return new RoleTypeImpl.Remote(transaction, proto.getLabel(), proto.getScope(), proto.getRoot());
        }

        @Nullable
        @Override
        public RoleTypeImpl getSupertype() {
            return getSupertypeExecute(TypeImpl::asRoleType);
        }

        @Override
        public final Stream<RoleTypeImpl> getSupertypes() {
            return super.getSupertypes(TypeImpl::asRoleType);
        }

        @Override
        public final Stream<RoleTypeImpl> getSubtypes() {
            return super.getSubtypes(TypeImpl::asRoleType);
        }

        @Override
        public final String getScope() {
            return scope;
        }

        @Override
        public RoleType.Remote asRemote(final Grakn.Transaction transaction) {
            return new RoleTypeImpl.Remote(transaction, getLabel(), getScope(), isRoot());
        }

        @Override
        public final RelationTypeImpl getRelation() {
            final TypeMethod.Req method = TypeMethod.Req.newBuilder()
                    .setRoleTypeGetRelationReq(GetRelation.Req.getDefaultInstance()).build();
            final GetRelation.Res response = execute(method).getRoleTypeGetRelationRes();
            return TypeImpl.of(response.getRelationType()).asRelationType();
        }

        @Override
        public final Stream<RelationTypeImpl> getRelations() {
            return stream(
                    TypeMethod.Iter.Req.newBuilder().setRoleTypeGetRelationsIterReq(
                            GetRelations.Iter.Req.getDefaultInstance()).build(),
                    res -> res.getRoleTypeGetRelationsIterRes().getRelationType()
            ).map(TypeImpl::asRelationType);
        }

        @Override
        public final Stream<ThingTypeImpl> getPlayers() {
            return stream(
                    TypeMethod.Iter.Req.newBuilder().setRoleTypeGetPlayersIterReq(
                            GetPlayers.Iter.Req.getDefaultInstance()).build(),
                    res -> res.getRoleTypeGetPlayersIterRes().getThingType()
            ).map(TypeImpl::asThingType);
        }

        @Override
        public RoleTypeImpl.Remote asRoleType() {
            return this;
        }

        @Override
        Stream<TypeImpl> stream(final TypeMethod.Iter.Req request, final Function<TypeMethod.Iter.Res, ConceptProto.Type> typeGetter) {
            return tx().concepts().iterateTypeMethod(
                    getLabel(), scope, request, response -> TypeImpl.of(typeGetter.apply(response))
            );
        }

        @Override
        TypeMethod.Res execute(final TypeMethod.Req typeMethod) {
            return tx().concepts().runTypeMethod(getLabel(), scope, typeMethod)
                    .getConceptMethodTypeRes().getResponse();
        }

        @Override
        public String toString() {
            return className(this.getClass()) + "[label: " + scope + ":" + getLabel() + "]";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final RoleTypeImpl.Remote that = (RoleTypeImpl.Remote) o;
            return (this.tx().equals(that.tx()) &&
                    this.getLabel().equals(that.getLabel()) &&
                    Objects.equals(this.getScope(), that.getScope()));
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
}
