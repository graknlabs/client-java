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
import grakn.client.concept.Concept;
import grakn.client.concept.type.AttributeType;
import grakn.client.test.behaviour.connection.ConnectionSteps;
import graql.lang.Graql;
import graql.lang.query.GraqlDefine;
import graql.lang.query.GraqlGet;
import graql.lang.query.GraqlInsert;
import graql.lang.query.GraqlQuery;
import graql.lang.query.GraqlUndefine;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GraqlSteps {

    private static GraknClient.Session session = null;
    private static GraknClient.Transaction tx = null;

    private static List<ConceptMap> answers;
    private static String answerConceptKey = null;

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


    @Then("answer concepts all have key: {word}")
    public void answer_concepts_have_key_labeled(String label) {
        answerConceptKey = label;
    }

    @Then("answer keys are")
    public void answer_keys_are(List<Map<String, String>> conceptKeys) {
      assertEquals(conceptKeys.size(), answers.size());

        for (ConceptMap answer : answers) {

            Map<String, String> answerKeys = new HashMap<>();
            AttributeType.Remote<?> keyType = tx.getAttributeType(answerConceptKey);
            // remap each concept and save its key value into the map from variable to key value
            answer.map().forEach((var, concept) -> {
                            answerKeys.put(var.name(), concept.asThing().asRemote(tx)
                                    .attributes(keyType).findFirst().get().value().toString());
            });

            int matchingAnswers = 0;
            for (Map<String, String> expectedKeys : conceptKeys) {
                if (expectedKeys.equals(answerKeys)) {
                    matchingAnswers++;
                }
            }

            // we expect exactly one matching answer from the expected answer keys
            assertEquals(1, matchingAnswers);
        }
    }


    @Then("answers are labeled")
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

    @Then("concept identifiers are")
    public void concept_identifiers_are(Map<String, Map<String, String>> identifiers) {
        // TODO: to implement
    }

    @Then("uniquely identify answer concepts")
    public void uniquely_identify_answer_concepts(List<Map<String, String>> answersIdentifiers) {
        // TODO: to implement
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
            Concept<?> concept = templateFiller.get(requiredVariable);

            builder.append(template, i, matcher.start());
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
