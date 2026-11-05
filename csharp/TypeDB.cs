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

using System;
using System.Collections.Generic;
using TypeDB.Driver.Api;
using TypeDB.Driver.Common;
using TypeDB.Driver.Connection;
using ConceptGivenRows = TypeDB.Driver.Concept.GivenRows;
using ConceptValue = TypeDB.Driver.Concept.Value;

namespace TypeDB.Driver
{
    public static class TypeDB
    {
        /// <summary>
        /// The default address of the TypeDB server.
        /// </summary>
        public const string DefaultAddress = "127.0.0.1:1729";

        /// <summary>
        /// Open a TypeDB Driver to a TypeDB server available at the provided address.
        /// </summary>
        /// <param name="address">The address of the TypeDB server.</param>
        /// <param name="credentials">The credentials to connect with.</param>
        /// <param name="driverOptions">The driver connection options to connect with.</param>
        /// <example>
        /// <code>
        /// TypeDB.Driver(address, credentials, driverOptions);
        /// </code>
        /// </example>
        public static IDriver Driver(string address, Credentials credentials, DriverOptions driverOptions)
        {
            return new TypeDBDriver(address, credentials, driverOptions);
        }

        /// <summary>
        /// Open a TypeDB Driver to a TypeDB cluster available at the provided addresses.
        /// </summary>
        /// <param name="addresses">The addresses of TypeDB cluster servers for connection.</param>
        /// <param name="credentials">The credentials to connect with.</param>
        /// <param name="driverOptions">The driver connection options to connect with.</param>
        /// <example>
        /// <code>
        /// TypeDB.Driver(addresses, credentials, driverOptions);
        /// </code>
        /// </example>
        public static IDriver Driver(ISet<string> addresses, Credentials credentials, DriverOptions driverOptions)
        {
            return new TypeDBDriver(addresses, credentials, driverOptions);
        }

        /// <summary>
        /// Open a TypeDB Driver to a TypeDB cluster, using the provided address translation.
        /// </summary>
        /// <param name="addressTranslation">The translation of public TypeDB cluster server addresses (keys) to server-side private addresses (values).</param>
        /// <param name="credentials">The credentials to connect with.</param>
        /// <param name="driverOptions">The driver connection options to connect with.</param>
        /// <example>
        /// <code>
        /// TypeDB.Driver(addressTranslation, credentials, driverOptions);
        /// </code>
        /// </example>
        public static IDriver Driver(IDictionary<string, string> addressTranslation, Credentials credentials, DriverOptions driverOptions)
        {
            return new TypeDBDriver(addressTranslation, credentials, driverOptions);
        }

        /// <summary>
        /// Open a TypeDB Driver using the addresses and credentials held in a connection string of the
        /// form <c>typedb://&lt;username&gt;:&lt;password&gt;\@&lt;addresses&gt;</c>.
        /// </summary>
        /// <remarks>
        /// Whether the connection uses TLS is determined by the addresses' <c>http://</c> or
        /// <c>https://</c> scheme, using the system's native trust roots.
        /// The addresses are connected to as written, with port 1729 assumed where none is given. If
        /// an address carries the HTTP endpoint's default port 8000 and answers with HTTP rather than
        /// gRPC, the driver retries on port 1729 and logs a warning, so a connection string copied
        /// from TypeDB Studio works unchanged.
        /// </remarks>
        /// <param name="connectionString">The connection string holding the addresses and credentials to connect with.</param>
        /// <example>
        /// <code>
        /// TypeDB.DriverFromConnectionString(connectionString);
        /// </code>
        /// </example>
        public static IDriver DriverFromConnectionString(string connectionString)
        {
            return TypeDBDriver.FromConnectionString(connectionString, null);
        }

        /// <summary>
        /// Open a TypeDB Driver using the addresses and credentials held in a connection string, with
        /// explicit driver options.
        /// </summary>
        /// <remarks>
        /// The connection string's <c>http://</c> or <c>https://</c> scheme is authoritative: the
        /// given options' TLS configuration must agree with it.
        /// </remarks>
        /// <param name="connectionString">The connection string holding the addresses and credentials to connect with.</param>
        /// <param name="driverOptions">The driver connection options to connect with.</param>
        /// <example>
        /// <code>
        /// TypeDB.DriverFromConnectionString(connectionString, driverOptions);
        /// </code>
        /// </example>
        public static IDriver DriverFromConnectionString(string connectionString, DriverOptions driverOptions)
        {
            return TypeDBDriver.FromConnectionString(connectionString, driverOptions);
        }

        /// <summary>
        /// Factory class to instantiate new <c>IValue</c> concepts and build <c>IGivenRows</c> for use as query inputs.
        /// </summary>
        public static class Concept
        {
            /// <summary>Constructs an <c>IGivenRows</c> instance for use as inputs to queries (index-based).</summary>
            public static IGivenRows GivenRows(List<string> givenVariables, List<List<IConcept?>> givenRows)
                => ConceptGivenRows.Of(givenVariables, givenRows);

            /// <summary>Constructs an <c>IGivenRows</c> instance from dict-based rows for use as inputs to queries.</summary>
            public static IGivenRows GivenRows(List<Dictionary<string, IConcept?>> givenRows)
                => ConceptGivenRows.Of(givenRows);

            /// <summary>Constructs an <c>IGivenRows</c> instance from rows containing raw .NET values or concepts.</summary>
            public static IGivenRows GivenRowsFromObjects(List<Dictionary<string, object?>> givenRows)
                => ConceptGivenRows.OfObjects(givenRows);

            /// <summary>
            /// Converts a raw host-language object to an <c>IValue</c> concept.
            /// Accepted types: <c>bool</c>, <c>long</c>, <c>int</c>, <c>double</c>, <c>float</c>,
            /// <c>decimal</c>, <c>string</c>, <c>DateOnly</c>, <c>Datetime</c>, <c>DatetimeTZ</c>, <c>Duration</c>.
            /// </summary>
            /// <exception cref="TypeDBDriverException">if the object's type is not supported.</exception>
            public static IValue TryConvertToValue(object value) => ConceptValue.TryConvertToValue(value);

            /// <summary>Creates a new <c>IValue</c> wrapping the specified <c>bool</c> value.</summary>
            public static IValue NewBoolean(bool value) => ConceptValue.NewBoolean(value);

            /// <summary>Creates a new <c>IValue</c> wrapping the specified <c>long</c> value.</summary>
            public static IValue NewInteger(long value) => ConceptValue.NewInteger(value);

            /// <summary>Creates a new <c>IValue</c> wrapping the specified <c>double</c> value.</summary>
            public static IValue NewDouble(double value) => ConceptValue.NewDouble(value);

            /// <summary>Creates a new <c>IValue</c> wrapping the specified <c>decimal</c> value.</summary>
            public static IValue NewDecimal(decimal value) => ConceptValue.NewDecimal(value);

            /// <summary>Creates a new <c>IValue</c> wrapping the specified <c>string</c> value.</summary>
            public static IValue NewString(string value) => ConceptValue.NewString(value);

            /// <summary>Creates a new <c>IValue</c> wrapping the specified <c>DateOnly</c> value.</summary>
            public static IValue NewDate(DateOnly value) => ConceptValue.NewDate(value);

            /// <summary>Creates a new <c>IValue</c> wrapping the specified <c>Datetime</c> value.</summary>
            public static IValue NewDatetime(Datetime value) => ConceptValue.NewDatetime(value);

            /// <summary>Creates a new <c>IValue</c> wrapping the specified <c>DatetimeTZ</c> value.</summary>
            public static IValue NewDatetimeTz(DatetimeTZ value) => ConceptValue.NewDatetimeTz(value);

            /// <summary>Creates a new <c>IValue</c> wrapping the specified <c>Duration</c> value.</summary>
            public static IValue NewDuration(Duration value) => ConceptValue.NewDuration(value);
        }
    }
}
