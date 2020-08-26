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
 *
 */

package grakn.client.test.behaviour.concept.type.relationtype;

import grakn.common.test.server.GraknCoreRunner;
import grakn.common.test.server.GraknSingleton;
import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@RunWith(Cucumber.class)
@CucumberOptions(
        strict = true,
        plugin = "pretty",
        glue = "grakn.client.test.behaviour",
        features = "external/graknlabs_behaviour/concept/type/relationtype.feature",
        tags = "not @ignore and not @ignore-grakn"
)
public class RelationTypeTest {
    // ATTENTION:
    // When you click RUN from within this class through Intellij IDE, it will fail.
    // You can fix it by doing:
    //
    // 1) Go to 'Run'
    // 2) Select 'Edit Configurations...'
    // 3) Select 'Bazel test RelationTypeTest'
    //
    // 4) Ensure 'Target Expression' is set correctly: '//<this>/<package>/<name>:test'
    //
    // 5) Update 'Bazel Flags':
    //    a) Remove the line that says: '--test_filter=grakn.core.*'
    //    b) Use the following Bazel flags:
    //       --cache_test_results=no : to make sure you're not using cache
    //       --test_output=streamed : to make sure all output is printed
    //       --subcommands : to print the low-level commands and execution paths
    //       --sandbox_debug : to keep the sandbox not deleted after test runs
    //       --spawn_strategy=standalone : if you're on Mac and the tests need permission to access filesystem
    //
    // 6) Hit the RUN button by selecting the test from the dropdown menu on the top bar

    private static GraknCoreRunner runner;

    @BeforeClass
    public static void beforeClass() throws InterruptedException, IOException, TimeoutException {
        runner = new GraknCoreRunner();
        runner.start();
        GraknSingleton.setGraknRunner(runner);
    }

    @AfterClass
    public static void afterClass() throws InterruptedException, IOException, TimeoutException {
        runner.stop();
    }
}
