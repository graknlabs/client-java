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

package grakn.client.logic;

import grakn.client.api.answer.ConceptMap;
import grakn.client.api.logic.Explanation;
import grakn.client.api.logic.Rule;
import grakn.client.concept.answer.ConceptMapImpl;
import grakn.protocol.AnswerProto;
import grakn.protocol.LogicProto;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ExplanationImpl implements Explanation {

    private final Rule rule;
    private final Map<String, Set<String>> variableMapping;
    private final ConceptMap thenAnswer;
    private final ConceptMap whenAnswer;

    private ExplanationImpl(Rule rule, Map<String, Set<String>> variableMapping, ConceptMap thenAnswer, ConceptMap whenAnswer) {
        this.rule = rule; this.variableMapping = variableMapping;
        this.thenAnswer = thenAnswer;
        this.whenAnswer = whenAnswer;
    }

    public static Explanation of(LogicProto.Explanation explanation) {
        return new ExplanationImpl(
                RuleImpl.of(explanation.getRule()),
                of(explanation.getVarMappingMap()),
                ConceptMapImpl.of(explanation.getThenAnswer()),
                ConceptMapImpl.of(explanation.getWhenAnswer())
        );
    }

    private static Map<String, Set<String>> of(Map<String, LogicProto.Explanation.VarsList> varMapping) {
        Map<String, Set<String>> mapping = new HashMap<>();
        varMapping.forEach((from, tos) -> mapping.put(from, new HashSet<>(tos.getVarsList())));
        return mapping;
    }

    @Override
    public Rule rule() {
        return rule;
    }

    @Override
    public Map<String, Set<String>> variableMapping() {
        return variableMapping;
    }

    @Override
    public ConceptMap thenAnswer() {
        return thenAnswer;
    }

    @Override
    public ConceptMap whenAnswer() {
        return whenAnswer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true; if (o == null || getClass() != o.getClass()) return false;
        final ExplanationImpl that = (ExplanationImpl) o;
        return rule.equals(that.rule) && variableMapping.equals(that.variableMapping) &&
                thenAnswer.equals(that.thenAnswer) && whenAnswer.equals(that.whenAnswer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rule, variableMapping, thenAnswer, whenAnswer);
    }
}
