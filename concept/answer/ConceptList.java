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

import grakn.client.concept.ConceptId;

import java.util.Collections;
import java.util.List;

/**
 * A type of Answer object that contains a List of Concepts.
 */
public class ConceptList extends Answer {

    // TODO: change to store List<Concept> once we are able to construct Concept without a database look up
    private final List<ConceptId> list;
    private final Explanation explanation;

    public ConceptList(List<ConceptId> list) {
        this.list = Collections.unmodifiableList(list);
        this.explanation = new Explanation();
    }

    @Override
    public Explanation explanation() {
        return explanation;
    }

    public List<ConceptId> list() {
        return list;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ConceptList a2 = (ConceptList) obj;
        return this.list.equals(a2.list);
    }

    @Override
    public int hashCode() {
        return list.hashCode();
    }
}
