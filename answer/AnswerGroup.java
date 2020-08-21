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

package grakn.client.answer;

import grakn.client.Grakn.Transaction;
import grakn.client.concept.Concept;
import grakn.protocol.AnswerProto;

import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * A type of Answer object that contains a List of Answers as the members and a RemoteConcept
 * as the owner.
 *
 * @param <T> the type of Answer being grouped
 */
public class AnswerGroup<T extends Answer> implements Answer {

    private final Concept.Remote owner;
    private final List<T> answers;

    public AnswerGroup(Concept.Remote owner, List<T> answers) {
        this.owner = owner;
        this.answers = answers;
    }

    public static AnswerGroup<? extends Answer> of(final Transaction tx, final AnswerProto.AnswerGroup res) {
        return new AnswerGroup<>(
                Concept.Remote.of(tx, res.getOwner()),
                res.getAnswersList().stream().map(answer -> Answer.of(tx, answer)).collect(toList())
        );
    }

    @Override
    public boolean hasExplanation() {
        return false;
    }

    public Concept.Remote owner() {
        return this.owner;
    }

    public List<T> answers() {
        return this.answers;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AnswerGroup<?> a2 = (AnswerGroup<?>) obj;
        return this.owner.equals(a2.owner) &&
                this.answers.equals(a2.answers);
    }

    @Override
    public int hashCode() {
        int hash = owner.hashCode();
        hash = 31 * hash + answers.hashCode();

        return hash;
    }
}
