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

using DataTable = Gherkin.Ast.DataTable;
using System;
using System.Collections.Generic;
using System.Linq;
using Xunit;
using Xunit.Gherkin.Quick;

using TypeDB.Driver;
using TypeDB.Driver.Api;
using TypeDB.Driver.Common;
using TypeDB.Driver.Test.Behaviour;

namespace TypeDB.Driver.Test.Behaviour
{
    public partial class BehaviourSteps : ConnectionStepsBase
    {
        // Default test credentials
        private const string DefaultUsername = "admin";
        private const string DefaultPassword = "password";

        public BehaviourSteps()
            : base()
        {
            // Reset query-level state between scenarios.
            // These static fields persist across scenarios and must be cleared.
            _queryOptions = null;
            _queryAnswer = null;
            _collectedRows = null;
            _collectedDocuments = null;
            _concurrentAnswers = null;
            _concurrentRowStreams = null;
        }

        public override IDriver CreateDefaultTypeDBDriver()
        {
            return TypeDB.Driver(
                TypeDB.DefaultAddress,
                DefaultCredentials,
                DriverOptions);
        }

        public override IDriver CreateTypeDBDriver(string address)
        {
            return TypeDB.Driver(
                address,
                new Credentials(DefaultUsername, DefaultPassword),
                DriverOptions);
        }

        protected override IEnumerable<string> DefaultAddresses => new[] { TypeDB.DefaultAddress };

        [Given(@"typedb starts")]
        [When(@"typedb starts")]
        public override void TypeDBStarts()
        {
            // TypeDB is assumed to be running externally for these tests
        }

        [Given(@"connection opens with default authentication")]
        [When(@"connection opens with default authentication")]
        public override void ConnectionOpensWithDefaultAuthentication()
        {
            Driver = CreateTypeDBDriver(TypeDB.DefaultAddress);
        }

        [When(@"connection opens with default authentication; fails with a message containing: ""(.*)""")]
        [Then(@"connection opens with default authentication; fails with a message containing: ""(.*)""")]
        public void ConnectionOpensWithDefaultAuthenticationFailsWithMessage(string expectedMessage)
        {
            var exception = Assert.ThrowsAny<Exception>(() =>
            {
                CreateTypeDBDriver(TypeDB.DefaultAddress);
            });
            Assert.Contains(expectedMessage, exception.Message);
        }

        [Given(@"connection opens with authentication: {}, {}")]
        [When(@"connection opens with authentication: {}, {}")]
        public void ConnectionOpensWithAuthentication(string username, string password)
        {
            Driver = TypeDB.Driver(
                TypeDB.DefaultAddress,
                new Credentials(username, password),
                new DriverOptions(DriverTlsConfig.Disabled()));
        }

        [Given(@"connection has been opened")]
        public override void ConnectionHasBeenOpened()
        {
            base.ConnectionHasBeenOpened();
        }

        [Given(@"connection closes")]
        [When(@"connection closes")]
        [Then(@"connection closes")]
        public override void ConnectionCloses()
        {
            base.ConnectionCloses();
        }

        [Given(@"connection is open: (.*)")]
        [Then(@"connection is open: (.*)")]
        public void ConnectionIsOpen(string expectedState)
        {
            bool expected = bool.Parse(expectedState);
            if (expected)
            {
                Assert.NotNull(Driver);
                Assert.True(Driver.IsOpen());
            }
            else
            {
                Assert.True(Driver == null || !Driver.IsOpen());
            }
        }

        [Then(@"connection contains distribution")]
        public void ConnectionContainsDistribution()
        {
            Assert.False(string.IsNullOrEmpty(GetServerVersion().Distribution));
        }

        [Then(@"connection contains version")]
        public void ConnectionContainsVersion()
        {
            Assert.False(string.IsNullOrEmpty(GetServerVersion().Version));
        }

        [Then(@"connection has (\d+) servers?")]
        public void ConnectionHasServerCount(int count)
        {
            Assert.Equal(count, GetServers().Count);
        }

        [Then(@"connection primary server exists")]
        public void ConnectionPrimaryServerExists()
        {
            Assert.NotNull(GetPrimaryServer());
        }

        [Given(@"connection has (\d+) databases?")]
        [Then(@"connection has (\d+) databases?")]
        public void ConnectionHasDatabaseCount(int expectedCount)
        {
            Assert.NotNull(Driver);
            Assert.Equal(expectedCount, Driver.Databases.GetAll().Count);
        }

        [When(@"set operation server routing to: (.+)")]
        public void SetOperationServerRouting(string routing)
        {
            if (routing.Equals("auto", StringComparison.OrdinalIgnoreCase))
            {
                OperationServerRouting = new ServerRouting.Auto();
            }
            else if (routing.ToLower().StartsWith("direct(") && routing.EndsWith(")"))
            {
                string address = routing.Substring("direct(".Length, routing.Length - "direct(".Length - 1);
                OperationServerRouting = new ServerRouting.Direct(address);
            }
            else
            {
                throw new ArgumentException($"Unknown server routing: {routing}");
            }
        }

        [When(@"set driver option primary_failover_retries to: (\d+)")]
        public void SetDriverOptionPrimaryFailoverRetriesTo(int value)
        {
            DriverOptions.PrimaryFailoverRetries = value;
        }

        [When(@"set driver option request_timeout_millis to: (\d+)")]
        public void SetDriverOptionRequestTimeoutMillisTo(int value)
        {
            DriverOptions.RequestTimeoutMillis = value;
        }

        [When(@"connection opens with a wrong port; fails")]
        [Then(@"connection opens with a wrong port; fails")]
        public void ConnectionOpensWithWrongPortFails()
        {
            Assert.ThrowsAny<Exception>(() =>
            {
                var wrongPortDriver = TypeDB.Driver(
                    "localhost:9999",
                    DefaultCredentials,
                    DriverOptions);
            });
        }

        [When(@"connection opens with a wrong host; fails with a message containing: ""(.*)""")]
        [Then(@"connection opens with a wrong host; fails with a message containing: ""(.*)""")]
        public void ConnectionOpensWithWrongHostFailsWithMessage(string expectedMessage)
        {
            var exception = Assert.ThrowsAny<Exception>(() =>
            {
                var wrongHostDriver = TypeDB.Driver(
                    "nonexistent-host.invalid:1729",
                    DefaultCredentials,
                    DriverOptions);
            });
            Assert.Contains(expectedMessage, exception.Message);
        }
    }
}
