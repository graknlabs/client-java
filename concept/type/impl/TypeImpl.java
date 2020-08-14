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

import grakn.client.Grakn.Transaction;
import grakn.client.concept.Concept;
import grakn.client.concept.ConceptIID;
import grakn.client.concept.Label;
import grakn.client.concept.impl.ConceptImpl;
import grakn.client.concept.type.Type;
import grakn.client.exception.GraknClientException;
import grakn.client.rpc.RequestBuilder;
import grakn.protocol.ConceptProto;

import javax.annotation.Nullable;
import java.util.stream.Stream;

public abstract class TypeImpl {

    public abstract static class Local extends ConceptImpl.Local implements Type.Local {

        private final Label label;

        protected Local(ConceptProto.Concept concept) {
            super(concept);
            this.label = Label.of(concept.getLabelRes().getLabel());
        }

        @Override
        public final Label getLabel() {
            return label;
        }
    }

    public abstract static class Remote extends ConceptImpl.Remote implements Type.Remote {

        public Remote(Transaction tx, ConceptIID iid) {
            super(tx, iid);
        }

        public final Type.Remote setSupertype(Type type) {
            ConceptProto.Method.Req method = ConceptProto.Method.Req.newBuilder()
                    .setTypeSetSupertypeReq(ConceptProto.Type.SetSupertype.Req.newBuilder()
                                                       .setType(RequestBuilder.ConceptMessage.from(type))).build();

            runMethod(method);
            return this;
        }

        @Override
        public final Label getLabel() {
            ConceptProto.Method.Req method = ConceptProto.Method.Req.newBuilder()
                    .setTypeGetLabelReq(ConceptProto.Type.GetLabel.Req.getDefaultInstance()).build();

            return Label.of(runMethod(method).getTypeGetLabelRes().getLabel());
        }

        @Override
        public Type.Remote setLabel(Label label) {
            ConceptProto.Method.Req method = ConceptProto.Method.Req.newBuilder()
                    .setTypeSetLabelReq(ConceptProto.Type.SetLabel.Req.newBuilder()
                                                         .setLabel(label.getValue())).build();

            runMethod(method);
            return this;
        }

        @Nullable
        public Type.Remote getSupertype() {
            ConceptProto.Method.Req method = ConceptProto.Method.Req.newBuilder()
                    .setTypeGetSupertypeReq(ConceptProto.Type.GetSupertype.Req.getDefaultInstance()).build();

            ConceptProto.Type.GetSupertype.Res response = runMethod(method).getTypeGetSupertypeRes();

            switch (response.getResCase()) {
                case NULL:
                    return null;
                case TYPE:
                    return Concept.Remote.of(tx(), response.getType()).asType();
                default:
                    throw GraknClientException.unreachableStatement("Unexpected response " + response);
            }

        }

        @Override
        public Stream<? extends Type.Remote> getSupertypes() {
            ConceptProto.Method.Iter.Req method = ConceptProto.Method.Iter.Req.newBuilder()
                    .setTypeGetSupertypesIterReq(ConceptProto.Type.GetSupertypes.Iter.Req.getDefaultInstance()).build();

            return conceptStream(method, res -> res.getTypeGetSupertypesIterRes().getType());
        }

        @Override
        public Stream<? extends Type.Remote> getSubtypes() {
            ConceptProto.Method.Iter.Req method = ConceptProto.Method.Iter.Req.newBuilder()
                    .setTypeGetSubtypesIterReq(ConceptProto.Type.GetSubtypes.Iter.Req.getDefaultInstance()).build();

            return conceptStream(method, res -> res.getTypeGetSubtypesIterRes().getType());
        }

    }
}
