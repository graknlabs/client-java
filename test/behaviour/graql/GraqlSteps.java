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

package grakn.client.test.behaviour.graql;

import com.google.common.collect.Iterators;
import grakn.client.GraknClient;
import grakn.client.answer.Answer;
import grakn.client.answer.ConceptMap;
import grakn.client.answer.Explanation;
import grakn.client.concept.Attribute;
import grakn.client.concept.Concept;
import grakn.client.concept.Rule;
import grakn.client.test.behaviour.connection.ConnectionSteps;
import graql.lang.Graql;
import graql.lang.query.GraqlDefine;
import graql.lang.query.GraqlGet;
import graql.lang.query.GraqlInsert;
import graql.lang.query.GraqlQuery;
import graql.lang.query.GraqlUndefine;
import graql.lang.statement.Variable;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class GraqlSteps {

    private static GraknClient.Session session = null;
    private static GraknClient.Transaction tx = null;

    private static List<ConceptMap> answers;
    HashMap<String, UniquenessCheck> identifierChecks = new HashMap<>();
    private Map<String, Map<String, String>> rules;

    @After
    public void close_transaction() {
        tx.close();
    }

    @Given("transaction is initialised")
    public void transaction_is_initialised() {
        session = Iterators.getOnlyElement(ConnectionSteps.sessions.iterator());
        tx = session.transaction().write();
        assertTrue(tx.isOpen());
    }

    @Given("the integrity is validated")
    public void integrity_is_validated(){

        // TODO

    }

    @Given("graql define")
    public void graql_define(String defineQueryStatements) {
        GraqlDefine graqlQuery = Graql.parse(String.join("\n", defineQueryStatements)).asDefine();
        tx.execute(graqlQuery);
        tx.commit();
        tx = session.transaction().write();
    }

    @Given("graql undefine")
    public void graql_undefine(String undefineQueryStatements) {
        GraqlUndefine graqlQuery = Graql.parse(String.join("\n", undefineQueryStatements)).asUndefine();
        tx.execute(graqlQuery);
        tx.commit();
        tx = session.transaction().write();
    }

    @Given("graql undefine throws")
    public void graql_undefine_throws(String undefineQueryStatements) {
        GraqlUndefine graqlQuery = Graql.parse(String.join("\n", undefineQueryStatements)).asUndefine();
        boolean threw = false;
        try {
            tx.execute(graqlQuery);
            tx.commit();
        } catch (RuntimeException e) {
            threw = true;
        } finally {
            tx.close();
            tx = session.transaction().write();
        }

        assertTrue(threw);
    }

    @Given("graql insert")
    public void graql_insert(String insertQueryStatements) {
        GraqlQuery graqlQuery = Graql.parse(String.join("\n", insertQueryStatements));
        tx.execute(graqlQuery);
        tx.commit();
        tx = session.transaction().write();
    }


    @Given("graql insert throws")
    public void graql_insert_throws(String insertQueryStatements) {
        GraqlQuery graqlQuery = Graql.parse(String.join("\n", insertQueryStatements));
        boolean threw = false;
        try {
            tx.execute(graqlQuery);
            tx.commit();
        } catch (RuntimeException e) {
            threw = true;
        } finally {
            tx.close();
            tx = session.transaction().write();
        }
        assertTrue(threw);
    }

    @When("get answers of graql query")
    public void graql_query(String graqlQueryStatements) {
        GraqlQuery graqlQuery = Graql.parse(String.join("\n", graqlQueryStatements));
        if (graqlQuery instanceof GraqlGet) {
            answers = tx.execute(graqlQuery.asGet());
        } else if (graqlQuery instanceof GraqlInsert) {
            answers = tx.execute(graqlQuery.asInsert());
        } else {
            // TODO specialise exception
            throw new RuntimeException("Only match-get and inserted supported for now");
        }
    }

    @Then("answer size is: {number}")
    public void answer_quantity_assertion(int expectedAnswers) {
        assertEquals(expectedAnswers, answers.size());
    }

    @Then("concept identifier symbols are")
    public void concept_identifier_symbols_are(Map<String, Map<String, String>> identifiers) {
        for (Map.Entry<String, Map<String, String>> entry : identifiers.entrySet()) {
            String identifier = entry.getKey();
            String type = entry.getValue().get("Identifier Type");
            String value = entry.getValue().get("Identifier Value");

            switch (type) {
                case "key":
                    identifierChecks.put(identifier, new KeyUniquenessCheck(value));
                    break;
                case "value":
                    identifierChecks.put(identifier, new ValueUniquenessCheck(value));
                    break;
                case "label":
                    identifierChecks.put(identifier, new LabelUniquenessCheck(value));
                    break;
                default:
                    throw new RuntimeException(String.format("Unrecognised Identifier Type \"%s\"", type));
            }
        }
    }

    private interface UniquenessCheck {
        boolean check(Concept concept);
    }

    public static class LabelUniquenessCheck implements UniquenessCheck {

        private final String label;

        LabelUniquenessCheck(String label) {
            this.label = label;
        }

        @Override
        public boolean check(Concept concept) {
            return concept.isType() && label.equals(concept.asType().label().toString());
        }
    }

    public static class AttributeUniquenessCheck {

        protected final String type;
        protected final String value;

        AttributeUniquenessCheck(String typeAndValue) {
            String[] s = typeAndValue.split(":");
            assertEquals(2, s.length);
            type = s[0];
            value = s[1];
        }
    }

    public static class ValueUniquenessCheck extends AttributeUniquenessCheck implements UniquenessCheck {
        ValueUniquenessCheck(String typeAndValue) {
            super(typeAndValue);
        }

        public boolean check(Concept concept) {
            return concept.isAttribute()
            && type.equals(concept.asAttribute().type().label().toString())
            && value.equals(concept.asAttribute().value().toString());
        }
    }

    public static class KeyUniquenessCheck extends AttributeUniquenessCheck implements UniquenessCheck {
        KeyUniquenessCheck(String typeAndValue) {
            super(typeAndValue);
        }

        /**
         * Check that the given key is in the concept's keys
         * @param concept to check
         * @return whether the given key matches a key belonging to the concept
         */
        @Override
        public boolean check(Concept concept) {
            if(!concept.isThing()) { return false; }

            Set<Attribute<?>> keys = concept.asThing().keys().collect(Collectors.toSet());
            HashMap<String, String> keyMap = new HashMap<>();

            for (Attribute<?> key : keys) {
                keyMap.put(
                        key.type().label().toString(),
                        key.value().toString());
            }
            return value.equals(keyMap.get(type));
        }
    }

    @Then("uniquely identify answer concepts by symbols")
    public void uniquely_identify_answer_concepts_by_symbols(List<Map<String, String>> answersIdentifiers) {
      assertEquals(answersIdentifiers.size(), answers.size());

        for (ConceptMap answer : answers) {

            List<Map<String, String>> matchingIdentifiers = matchingAnswers(answersIdentifiers, answer);

            // we expect exactly one matching answer from the expected answer keys TODO This may no longer be true
            assertEquals(1, matchingIdentifiers.size());
        }
    }

    private List<Map<String, String>> matchingAnswers(List<Map<String, String>> answersIdentifiers, ConceptMap answer) {
        List<Map<String, String>> matchingIdentifiers = new ArrayList<>();

        for (Map<String, String> answerIdentifiers : answersIdentifiers) {

            if (matchAnswer(answerIdentifiers, answer)) {
                matchingIdentifiers.add(answerIdentifiers);
            }
        }
        return matchingIdentifiers;
    }

    private boolean matchAnswer(Map<String, String> answerIdentifiers, ConceptMap answer) {
        for (Map.Entry<String, String> entry : answerIdentifiers.entrySet()) {
            String varName = entry.getKey();
            String identifier = entry.getValue();

            if(!answer.map().containsKey(new Variable(varName))){
                return false;
            }

            if(!identifierChecks.get(identifier).check(answer.get(varName))) {
                return false;
            }
        }
        return true;
    }

    @Then("rules are")
    public void rules_are(Map<String, Map<String, String>> rules) {
        this.rules = rules;
    }

    @Then("answers contain explanation tree")
    public void answers_contain_explanation_tree(Map<Integer, Map<String, String>> explanationTree) {
        checkExplanationContains(answers, explanationTree, 0);
    }

    private void checkExplanationContains(List<ConceptMap> answers, Map<Integer, Map<String, String>> explanationTree, Integer entryId) {
        Map<String, String> explanationEntry = explanationTree.get(entryId);
        String[] vars = explanationEntry.get("Vars").split(", ");
        String[] identifiers = explanationEntry.get("Identifiers").split(", ");
        String[] children = explanationEntry.get("Children").split(", ");

        // TODO Assert or throw when its the test that's been defined incorrectly?
//        assertEquals(vars.length, identifiers.length);
        if (vars.length != identifiers.length) {
            throw new RuntimeException(String.format("Vars and Identifiers should correspond. Found %d Vars and %s Identifiers", vars.length, identifiers.length));
        }

        Map<String, String> answerIdentifiers = IntStream.range(0, vars.length).boxed().collect(Collectors.toMap(i -> vars[i], i -> identifiers[i]));

        Optional<ConceptMap> matchingAnswer = answers.stream().filter(answer -> matchAnswer(answerIdentifiers, answer)).findFirst();

        assertTrue(matchingAnswer.isPresent());

        ConceptMap answer = matchingAnswer.get();

        String queryWithIds = applyQueryTemplate(explanationEntry.get("Pattern"), answer);
        assertEquals(Graql.and(Graql.parsePatternList(queryWithIds)), answer.queryPattern());

        String expectedExplType = explanationEntry.get("Explanation");
        boolean hasExplanation = answer.hasExplanation();

        if (!(expectedExplType.equals("lookup") | expectedExplType.equals("join") | expectedExplType.equals("rule"))) {
            throw new RuntimeException(String.format("Explanation type %s not recognised", expectedExplType));
        }

        if (expectedExplType.equals("lookup")) {
            if(!explanationEntry.get("Rule Label").equals("-")) {
                throw new RuntimeException("Rule Label should be \"-\" for lookup explanations");
            }
            assertFalse(hasExplanation);
            String[] expectedChildren = {"-"};
            assertArrayEquals(expectedChildren, children);
        } else {

            Explanation explanation = answer.explanation();
            List<ConceptMap> explAnswers = explanation.getAnswers();

            assertEquals(children.length, explAnswers.size());

            if (expectedExplType.equals("join")) {
                if(!explanationEntry.get("Rule Label").equals("-")) {
                    throw new RuntimeException("Rule Label should be \"-\" for join explanations");
                }
                assertNull(explanation.getRule());
            } else {
                // rule
                Rule rule = explanation.getRule();
                assertEquals(explanationEntry.get("Rule Label"), rule.label().toString());

                Map<String, String> expectedRule = rules.get(explanationEntry.get("Rule Label"));
                assertEquals(expectedRule.get("when"), Objects.requireNonNull(rule.when()).toString());
                assertEquals(expectedRule.get("then"), Objects.requireNonNull(rule.then()).toString());
            }
            for (String child : children) {
                // Recurse
                checkExplanationContains(explAnswers, explanationTree, Integer.valueOf(child));
            }
        }
//      TODO Validate that in the given table rule labels should be "-" unless the Explanation is "rule"
    }

    @Then("answers are labeled") // TODO Update this with the latest structure
    public void answers_satisfy_labels(List<Map<String, String>> conceptLabels) {
        assertEquals(conceptLabels.size(), answers.size());

        for (ConceptMap answer : answers) {

            // convert the concept map into a map from variable to type label
            Map<String, String> answerAsLabels = new HashMap<>();
            answer.map().forEach((var, concept) -> answerAsLabels.put(var.name(), concept.asSchemaConcept().label().toString()));

            int matchingAnswers = 0;
            for (Map<String, String> expectedLabels : conceptLabels) {
                if (expectedLabels.equals(answerAsLabels)) {
                    matchingAnswers++;
                }
            }

            // we expect exactly one matching answer from the expected answer set
            assertEquals(1, matchingAnswers);
        }
    }

    @Then("each answer satisfies")
    public void each_answer_satisfies(String templatedGraqlQuery) {
        String templatedQuery = String.join("\n", templatedGraqlQuery);
        for (ConceptMap answer : answers) {
            String query = applyQueryTemplate(templatedQuery, answer);
            GraqlQuery graqlQuery = Graql.parse(query);
            List<? extends Answer> answers = tx.execute(graqlQuery);
            assertEquals(1, answers.size());
        }
    }

    private String applyQueryTemplate(String template, ConceptMap templateFiller) {
        // find shortest matching strings between <>
        Pattern pattern = Pattern.compile("<.+?>");
        Matcher matcher = pattern.matcher(template);

        StringBuilder builder = new StringBuilder();
        int i = 0;
        while (matcher.find()) {
            String matched = matcher.group(0);
            String requiredVariable = variableFromTemplatePlaceholder(matched.substring(1, matched.length() - 1));
            Concept concept = templateFiller.get(requiredVariable);

            builder.append(template.substring(i, matcher.start()));
            if (concept == null) {
                throw new RuntimeException(String.format("No ID available for template placeholder: %s", matched));
            } else {
                String conceptId = concept.id().toString();
                builder.append(conceptId);
            }
            i = matcher.end();
        }
        builder.append(template.substring(i));
        return builder.toString();
    }

    private String variableFromTemplatePlaceholder(String placeholder) {
        if (placeholder.endsWith(".id")) {
            String stripped = placeholder.replace(".id", "");
            String withoutPrefix = stripped.replace("answer.", "");
            return withoutPrefix;
        } else {
            throw new RuntimeException("Cannot replace template not based on ID");
        }
    }
}
