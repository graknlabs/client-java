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

using NUnit.Framework;

using TypeDB.Driver;
using TypeDB.Driver.Api;
using TypeDB.Driver.Common;

namespace TypeDB.Driver.Test.Integration
{
    /// <summary>
    /// The grammar itself is covered by the shared vectors in tool/test/connection-string-vectors.json,
    /// which the Rust core tests. These cases exercise the error paths through the PInvoke layer, and so
    /// need no running server.
    /// </summary>
    [TestFixture]
    public class ConnectionStringTest
    {
        [Test]
        public void InvalidConnectionStringThrows()
        {
            var error = Assert.Throws<TypeDBDriverException>(
                () => TypeDB.DriverFromConnectionString("typedb://admin@https://127.0.0.1:1729"));
            Assert.That(error!.Message, Does.Contain("Invalid connection string"));
        }

        [Test]
        public void AddressWithoutASchemeThrows()
        {
            var error = Assert.Throws<TypeDBDriverException>(
                () => TypeDB.DriverFromConnectionString("typedb://admin:password@127.0.0.1:1729"));
            Assert.That(error!.Message, Does.Contain("http://"));
        }

        [Test]
        public void DriverOptionsContradictingTheSchemeThrow()
        {
            var error = Assert.Throws<TypeDBDriverException>(
                () => TypeDB.DriverFromConnectionString(
                    "typedb://admin:password@https://127.0.0.1:1729",
                    new DriverOptions(DriverTlsConfig.Disabled())));
            Assert.That(error!.Message, Does.Contain("not compatible"));
        }
    }
}
