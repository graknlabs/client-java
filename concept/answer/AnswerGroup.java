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

package grakn.client.concept.answer;

import grakn.client.concept.Concept;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A type of Answer object that contains a List of Answers as the members and a RemoteConcept
 * as the owner.
 *
 * @param <T> the type of Answer being grouped
 */
public class AnswerGroup<T extends Answer> extends Answer {

    private final Concept owner;
    private final List<T> answers;
    private final Explanation explanation;

    public AnswerGroup(Concept owner, List<T> answers) {
        this(owner, answers, new Explanation());
    }

    public AnswerGroup(Concept owner, List<T> answers, Explanation explanation) {
        this.owner = owner;
        this.answers = answers;
        this.explanation = explanation;
    }

    @Nullable
    @Override
    public Explanation explanation() {
        return explanation;
    }

    public Concept owner() {
        return this.owner;
    }

    public List<T> answers() {
        return this.answers;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AnswerGroup a2 = (AnswerGroup) obj;
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
