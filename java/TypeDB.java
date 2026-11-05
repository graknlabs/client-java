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

package com.typedb.driver;

import com.typedb.driver.api.Credentials;
import com.typedb.driver.api.Driver;
import com.typedb.driver.api.DriverOptions;
import com.typedb.driver.api.concept.GivenRows;
import com.typedb.driver.api.concept.value.Value;
import com.typedb.driver.common.Duration;
import com.typedb.driver.common.exception.TypeDBDriverException;
import com.typedb.driver.concept.GivenRowsImpl;
import com.typedb.driver.concept.value.ValueImpl;
import com.typedb.driver.connection.DriverImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TypeDB {
    public static final String DEFAULT_ADDRESS = "127.0.0.1:1729";

    /**
     * Open a TypeDB Driver to a TypeDB server available at the provided address.
     *
     * <h3>Examples</h3>
     * <pre>
     * TypeDB.driver(address);
     * </pre>
     *
     * @param address       The address of the TypeDB server
     * @param credentials   The credentials to connect with
     * @param driverOptions The driver connection options to connect with
     */
    public static Driver driver(String address, Credentials credentials, DriverOptions driverOptions) throws TypeDBDriverException {
        return new DriverImpl(address, credentials, driverOptions);
    }

    /**
     * Open a TypeDB Driver to a TypeDB cluster available at the provided addresses.
     *
     * <h3>Examples</h3>
     * <pre>
     * TypeDB.driver(address);
     * </pre>
     *
     * @param addresses     The addresses of TypeDB cluster servers for connection
     * @param credentials   The credentials to connect with
     * @param driverOptions The driver connection options to connect with
     */
    public static Driver driver(Set<String> addresses, Credentials credentials, DriverOptions driverOptions) throws TypeDBDriverException {
        return new DriverImpl(addresses, credentials, driverOptions);
    }

    /**
     * Open a TypeDB Driver to a TypeDB cluster, using the provided address translation.
     *
     * <h3>Examples</h3>
     * <pre>
     * TypeDB.driver(addresses);
     * </pre>
     *
     * @param addressTranslation The translation of public TypeDB cluster server addresses (keys) to server-side private addresses (values)
     * @param credentials        The credentials to connect with
     * @param driverOptions      The driver connection options to connect with
     */
    public static Driver driver(Map<String, String> addressTranslation, Credentials credentials, DriverOptions driverOptions) throws TypeDBDriverException {
        return new DriverImpl(addressTranslation, credentials, driverOptions);
    }

    /**
     * Open a TypeDB Driver using the addresses and credentials held in a connection string of the
     * form <code>typedb://&lt;username&gt;:&lt;password&gt;@&lt;addresses&gt;</code>.
     *
     * <p>Whether the connection uses TLS is determined by the addresses' <code>http://</code> or
     * <code>https://</code> scheme, using the system's native trust roots.
     *
     * <p>The addresses are connected to as written, with port 1729 assumed where none is given. If
     * an address carries the HTTP endpoint's default port 8000 and answers with HTTP rather than
     * gRPC, the driver retries on port 1729 and logs a warning, so a connection string copied from
     * TypeDB Studio works unchanged.
     *
     * <h3>Examples</h3>
     * <pre>
     * TypeDB.driverFromConnectionString(connectionString);
     * </pre>
     *
     * @param connectionString The connection string holding the addresses and credentials to connect with
     */
    public static Driver driverFromConnectionString(String connectionString) throws TypeDBDriverException {
        return DriverImpl.fromConnectionString(connectionString, null);
    }

    /**
     * Open a TypeDB Driver using the addresses and credentials held in a connection string, with
     * explicit driver options.
     *
     * <p>The connection string's <code>http://</code> or <code>https://</code> scheme is
     * authoritative: the given options' TLS configuration must agree with it.
     *
     * <h3>Examples</h3>
     * <pre>
     * TypeDB.driverFromConnectionString(connectionString, driverOptions);
     * </pre>
     *
     * @param connectionString The connection string holding the addresses and credentials to connect with
     * @param driverOptions    The driver connection options to connect with
     */
    public static Driver driverFromConnectionString(String connectionString, DriverOptions driverOptions) throws TypeDBDriverException {
        return DriverImpl.fromConnectionString(connectionString, driverOptions);
    }

    /**
     * Factory class to instantiate new <code>Value</code> concepts from raw values.
     * These can then be used in the <code>GivenRows</code> of a query.
     */
    public static class Concept {

        /**
         * Constructs a <code>GivenRows</code> instance for use as inputs to queries.
         *
         * @param variables The variables describing the content of the givenRows.
         * @param rows Input rows for the query; each inner iterable is one row, {@code Optional.empty()} entries represent empty variables.
         */
        public static GivenRows givenRows(List<String> variables, List<? extends List<? extends com.typedb.driver.api.concept.Concept>> rows) throws TypeDBDriverException {
            return GivenRowsImpl.of(variables, rows);
        }

        /**
         * Constructs a <code>GivenRows</code> instance from the dictionary for use as inputs to queries.
         *
         * @param givenRows A list of input rows for the query. Each row is a dictionary mapping a variable to its value.
         */
        public static GivenRows givenRows(List<? extends Map<String, ? extends com.typedb.driver.api.concept.Concept>> givenRows) throws TypeDBDriverException {
            return GivenRowsImpl.of(givenRows);
        }

        /**
         * Constructs a <code>GivenRows</code> instance from a list of rows given as plain Java objects.
         * Each map entry value may be a {@link com.typedb.driver.api.concept.Concept} (used directly) or a raw host-language value
         * which is converted via {@link #tryConvertToValue}.
         *
         * @param givenRows A list of input rows; each row maps variable names to concepts or raw values.
         */
        public static GivenRows givenRowsFromObjects(List<? extends Map<String, Object>> givenRows) throws TypeDBDriverException {
            return GivenRowsImpl.ofObjects(givenRows);
        }

        /**
         * Converts a raw host-language object to a {@link Value} concept.
         * Accepted types: {@code Boolean}, {@code Long}, {@code Integer}, {@code Double}, {@code Float},
         * {@code BigDecimal}, {@code String}, {@code LocalDate}, {@code LocalDateTime}, {@code ZonedDateTime}, {@code Duration}.
         *
         * @throws TypeDBDriverException if the object's type is not supported
         */
        public static Value tryConvertToValue(Object value) {
            return ValueImpl.tryConvertToValue(value);
        }

        /** Creates a new {@code Value} wrapping the specified {@code boolean} value. */
        public static Value newBoolean(boolean value) {
            return ValueImpl.newBoolean(value);
        }

        /** Creates a new {@code Value} wrapping the specified {@code long} value. */
        public static Value newInteger(long value) { return ValueImpl.newInteger(value); }

        /** Creates a new {@code Value} wrapping the specified {@code double} value. */
        public static Value newDouble(double value) { return ValueImpl.newDouble(value); }

        /** Creates a new {@code Value} wrapping the specified {@code BigDecimal} value. */
        public static Value newDecimal(BigDecimal value) { return ValueImpl.newDecimal(value); }

        /** Creates a new {@code Value} wrapping the specified {@code String} value. */
        public static Value newString(String value) { return ValueImpl.newString(value); }

        /** Creates a new {@code Value} wrapping the specified {@code LocalDate} value. */
        public static Value newDate(LocalDate value) { return ValueImpl.newDate(value); }

        /** Creates a new {@code Value} wrapping the specified {@code LocalDateTime} value. */
        public static Value newDatetime(LocalDateTime value) { return ValueImpl.newDatetime(value); }

        /** Creates a new {@code Value} wrapping the specified {@code ZonedDateTime} value. */
        public static Value newDatetimeTz(ZonedDateTime value) { return ValueImpl.newDatetimeTz(value); }

        /** Creates a new {@code Value} wrapping the specified {@code Duration} value. */
        public static Value newDuration(Duration value) { return ValueImpl.newDuration(value); }
    }
}
