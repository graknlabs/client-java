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

package grakn.client.concept.impl;

import grakn.client.GraknClient;
import grakn.client.concept.Concept;
import grakn.client.concept.ConceptId;
import grakn.client.concept.Label;
import grakn.client.concept.Rule;
import grakn.client.exception.GraknClientException;
import grakn.protocol.session.ConceptProto;
import graql.lang.Graql;
import graql.lang.pattern.Pattern;

import javax.annotation.Nullable;
import java.util.stream.Stream;

public class RuleImpl {
    /**
     * Client implementation of Rule
     */
    public static class Local extends SchemaConceptImpl.Local<Rule> implements Rule.Local {

        public Local(ConceptProto.Concept concept) {
            super(concept);
        }
    }

    /**
     * Client implementation of Rule
     */
    public static class Remote extends SchemaConceptImpl.Remote<Rule> implements Rule.Remote {

        public Remote(GraknClient.Transaction tx, ConceptId id) {
            super(tx, id);
        }

        @Override
        public final Stream<Rule.Remote> sups() {
            return super.sups().map(this::asCurrentBaseType);
        }

        @Override
        public final Stream<Rule.Remote> subs() {
            return super.subs().map(this::asCurrentBaseType);
        }

        @Override
        public final Rule.Remote label(Label label) {
            return (Rule.Remote) super.label(label);
        }

        @Override
        public Rule.Remote sup(Rule superRule) {
            return (Rule.Remote) super.sup(superRule);
        }

        @Override
        @Nullable
        @SuppressWarnings("Duplicates") // response.getResCase() does not return the same type
        public final Pattern when() {
            ConceptProto.Method.Req method = ConceptProto.Method.Req.newBuilder()
                    .setRuleWhenReq(ConceptProto.Rule.When.Req.getDefaultInstance()).build();

            ConceptProto.Rule.When.Res response = runMethod(method).getRuleWhenRes();
            switch (response.getResCase()) {
                case NULL:
                    return null;
                case PATTERN:
                    return Graql.parsePattern(response.getPattern());
                default:
                    throw GraknClientException.unreachableStatement("Unexpected response " + response);
            }
        }

        @Override
        @Nullable
        @SuppressWarnings("Duplicates") // response.getResCase() does not return the same type
        public final Pattern then() {
            ConceptProto.Method.Req method = ConceptProto.Method.Req.newBuilder()
                    .setRuleThenReq(ConceptProto.Rule.Then.Req.getDefaultInstance()).build();

            ConceptProto.Rule.Then.Res response = runMethod(method).getRuleThenRes();
            switch (response.getResCase()) {
                case NULL:
                    return null;
                case PATTERN:
                    return Graql.parsePattern(response.getPattern());
                default:
                    throw GraknClientException.unreachableStatement("Unexpected response " + response);
            }
        }

        @Override
        protected final Rule.Remote asCurrentBaseType(Concept.Remote<?> other) {
            return other.asRule();
        }

        @Override
        protected final boolean equalsCurrentBaseType(Concept.Remote<?> other) {
            return other.isRule();
        }

    }
}
