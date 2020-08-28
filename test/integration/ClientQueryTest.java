/*
 * Copyright (C) 2020 Grakn Labs
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package grakn.client.test.integration;

import grakn.client.Grakn;
import grakn.client.Grakn.Client;
import grakn.client.Grakn.Session;
import grakn.client.Grakn.Transaction;
import grakn.client.concept.answer.ConceptMap;
import grakn.common.test.server.GraknCoreRunner;
import graql.lang.Graql;
import graql.lang.common.GraqlArg;
import graql.lang.query.GraqlCompute;
import graql.lang.query.GraqlDefine;
import graql.lang.query.GraqlDelete;
import graql.lang.query.GraqlMatch;
import graql.lang.query.GraqlInsert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static grakn.client.Grakn.Transaction.Type.WRITE;
import static graql.lang.Graql.and;
import static graql.lang.Graql.rel;
import static graql.lang.Graql.type;
import static graql.lang.Graql.var;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

/**
 * Performs various queries:
 * - define a schema with a rule
 * - match; get;
 * - match; get of an inferred relation
 * - match; insert;
 * - match; delete;
 * - match; aggregate;
 * - and compute count
 * <p>
 * The tests are highly interconnected hence why they are grouped into a single test.
 * If you split them into multiple tests, there is no guarantee that they are ran in the order they are defined,
 * and there is a chance that the match; get; test is performed before the define a schema test, which would cause it to fail.
 * <p>
 * The schema describes a lion family which consists of a lion, lioness, and the offspring - three young lions. The mating
 * relation captures the mating act between the male and female partners (ie., the lion and lioness). The child-bearing
 * relation captures the child-bearing act which results from the mating act.
 * <p>
 * The rule is one such that if there is an offspring which is the result of a certain child-bearing act, then
 * that offspring is the child of the male and female partners which are involved in the mating act.
 */
@SuppressWarnings("Duplicates")
public class ClientQueryTest {
    private static final Logger LOG = LoggerFactory.getLogger(ClientQueryTest.class);
    private static GraknCoreRunner grakn;
    private static Client graknClient;

    @BeforeClass
    public static void setUpClass() throws InterruptedException, IOException, TimeoutException {
        grakn = new GraknCoreRunner();
        grakn.start();
        graknClient = Grakn.client(grakn.address());
    }

    @AfterClass
    public static void closeSession() throws Exception {
        graknClient.close();
        grakn.stop();
    }

    @Test
    public void applicationTest() {
        LOG.info("clientJavaE2E() - starting client-java E2E...");

        localhostGraknTx(tx -> {
            GraqlDefine defineQuery = Graql.define(
                    type("child-bearing").sub("relation").relates("offspring").relates("child-bearer"),
                    type("mating").sub("relation").relates("male-partner").relates("female-partner").plays("mating", "child-bearer"),
                    type("parentship").sub("relation").relates("parent").relates("child"),

                    type("name").sub("attribute").value(GraqlArg.ValueType.STRING),
                    type("lion").sub("entity").owns("name").plays("mating", "male-partner").plays("mating", "female-partner").plays("child-bearing", "offspring").plays("parentship", "parent").plays("parentship", "child")
            );

            GraqlDefine ruleQuery = Graql.define(type("infer-parentship-from-mating-and-child-bearing").sub("rule")
                                                         .when(and(
                                                                 rel("male-partner", var("male")).rel("female-partner", var("female")).isa("mating"),
                                                                 var("childbearing").rel("child-bearer").rel("offspring", var("offspring")).isa("child-bearing")
                                                         ))
                                                         .then(and(
                                                                 rel("parent", var("male")).rel("parent", var("female")).rel("child", var("offspring")).isa("parentship")
                                                         )));
            LOG.info("clientJavaE2E() - define a schema...");
            LOG.info("clientJavaE2E() - '" + defineQuery + "'");
            tx.execute(defineQuery);
            tx.execute(ruleQuery);
            tx.commit();
            LOG.info("clientJavaE2E() - done.");
        });

        localhostGraknTx(tx -> {
            GraqlMatch getThingQuery = Graql.match(var("t").sub("thing")).get();
            LOG.info("clientJavaE2E() - assert if schema defined...");
            LOG.info("clientJavaE2E() - '" + getThingQuery + "'");
            List<String> definedSchema = tx.execute(getThingQuery).get().stream()
                    .map(answer -> answer.get("t").asType().asThingType().getLabel()).collect(Collectors.toList());
            String[] correctSchema = new String[]{"thing", "entity", "relation", "attribute",
                    "lion", "mating", "parentship", "child-bearing", "name"};
            assertThat(definedSchema, hasItems(correctSchema));
            LOG.info("clientJavaE2E() - done.");
        });

        localhostGraknTx(tx -> {
            String[] names = lionNames();
            GraqlInsert insertLionQuery = Graql.insert(
                    var().isa("lion").has("name", names[0]),
                    var().isa("lion").has("name", names[1]),
                    var().isa("lion").has("name", names[2])
            );
            LOG.info("clientJavaE2E() - insert some data...");
            LOG.info("clientJavaE2E() - '" + insertLionQuery + "'");
            tx.execute(insertLionQuery);
            tx.commit();
            LOG.info("clientJavaE2E() - done.");
        });

        localhostGraknTx(tx -> {
            String[] familyMembers = lionNames();
            LOG.info("clientJavaE2E() - inserting mating relations...");
            GraqlInsert insertMatingQuery = Graql.match(
                    var("lion").isa("lion").has("name", familyMembers[0]),
                    var("lioness").isa("lion").has("name", familyMembers[1]))
                    .insert(rel("male-partner", "lion").rel("female-partner", var("lioness")).isa("mating"));
            LOG.info("clientJavaE2E() - '" + insertMatingQuery + "'");
            List<ConceptMap> insertedMating = tx.execute(insertMatingQuery).get();

            LOG.info("clientJavaE2E() - inserting child-bearing relations...");
            GraqlInsert insertChildBearingQuery = Graql.match(
                    var("lion").isa("lion").has("name", familyMembers[0]),
                    var("lioness").isa("lion").has("name", familyMembers[1]),
                    var("offspring").isa("lion").has("name", familyMembers[2]),
                    var("mating").rel("male-partner", var("lion")).rel("female-partner", var("lioness")).isa("mating")
            )
                    .insert(var("childbearing").rel("child-bearer", var("mating")).rel("offspring", var("offspring")).isa("child-bearing"));
            LOG.info("clientJavaE2E() - '" + insertChildBearingQuery + "'");
            List<ConceptMap> insertedChildBearing = tx.execute(insertChildBearingQuery).get();

            tx.commit();

            assertThat(insertedMating, hasSize(1));
            assertThat(insertedChildBearing, hasSize(1));
            LOG.info("clientJavaE2E() - done.");
        });

        localhostGraknTx(tx -> {
            LOG.info("clientJavaE2E() - execute match get on the lion instances...");
            GraqlMatch getLionQuery = Graql.match(var("p").isa("lion").has("name", var("n"))).get();
            LOG.info("clientJavaE2E() - '" + getLionQuery + "'");
            List<ConceptMap> insertedLions = tx.execute(getLionQuery).get();
            List<String> insertedNames = insertedLions.stream().map(answer -> answer.get("n").asThing().asAttribute().asString().getValue()).collect(Collectors.toList());
            assertThat(insertedNames, containsInAnyOrder(lionNames()));

            LOG.info("clientJavaE2E() - execute match get on the mating relations...");
            GraqlMatch getMatingQuery = Graql.match(var().isa("mating")).get();
            LOG.info("clientJavaE2E() - '" + getMatingQuery + "'");
            List<ConceptMap> insertedMating = tx.execute(getMatingQuery).get();
            assertThat(insertedMating, hasSize(1));

            LOG.info("clientJavaE2E() - execute match get on the child-bearing...");
            GraqlMatch getChildBearingQuery = Graql.match(var().isa("child-bearing")).get();
            LOG.info("clientJavaE2E() - '" + getChildBearingQuery + "'");
            List<ConceptMap> insertedChildBearing = tx.execute(getChildBearingQuery).get();
            assertThat(insertedChildBearing, hasSize(1));
            LOG.info("clientJavaE2E() - done.");
        });

        localhostGraknTx(tx -> {
            LOG.info("clientJavaE2E() - match get inferred relations...");
            GraqlMatch getParentship = Graql.match(
                    var("parentship")
                            .rel("parent", var("parent"))
                            .rel("child", var("child"))
                            .isa("parentship")).get();
            LOG.info("clientJavaE2E() - '" + getParentship + "'");
            List<ConceptMap> parentship = tx.execute(getParentship).get();
            //2 answers - single answer for each parent
            assertThat(parentship, hasSize(2));
            LOG.info("clientJavaE2E() - done.");
        });

        localhostGraknTx(tx -> {
            LOG.info("clientJavaE2E() - match aggregate...");
            GraqlMatch.Aggregate aggregateQuery = Graql.match(var("p").isa("lion")).get().count();
            LOG.info("clientJavaE2E() - '" + aggregateQuery + "'");
            int aggregateCount = tx.execute(aggregateQuery).get().get(0).number().intValue();
            assertThat(aggregateCount, equalTo(lionNames().length));
            LOG.info("clientJavaE2E() - done.");
        });

        localhostGraknTx(tx -> {
            LOG.info("clientJavaE2E() - compute count...");
            final GraqlCompute.Statistics computeQuery = Graql.compute().count().in("lion");
            LOG.info("clientJavaE2E() - '" + computeQuery + "'");
            int computeCount = tx.execute(computeQuery).get().get(0).number().intValue();
            assertThat(computeCount, equalTo(lionNames().length));
            LOG.info("clientJavaE2E() - done.");
        });

        localhostGraknTx(tx -> {
            LOG.info("clientJavaE2E() - match delete...");
            GraqlDelete deleteQuery = Graql.match(var("m").isa("mating")).delete(var("m").isa("mating"));
            LOG.info("clientJavaE2E() - '" + deleteQuery + "'");
            tx.execute(deleteQuery);
            List<ConceptMap> matings = tx.execute(Graql.match(var("m").isa("mating")).get()).get();
            assertThat(matings, hasSize(0));
            LOG.info("clientJavaE2E() - done.");
        });

        LOG.info("clientJavaE2E() - client-java E2E test done.");
    }

    private String[] lionNames() {
        return new String[]{"male-partner", "female-partner", "young-lion"};
    }

    private void localhostGraknTx(Consumer<Transaction> fn) {
        String database = "grakn";
        try (Session session = graknClient.session(database)) {
            try (Transaction transaction = session.transaction(WRITE)) {
                fn.accept(transaction);
            }
        }
    }
}
