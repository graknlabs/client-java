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

package com.typedb.driver.test.integration;

import com.typedb.driver.TypeDB;
import com.typedb.driver.api.DriverOptions;
import com.typedb.driver.api.DriverTlsConfig;
import com.typedb.driver.common.exception.TypeDBDriverException;
import org.junit.Test;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * The grammar itself is covered by the shared vectors in tool/test/connection-string-vectors.json,
 * which the Rust core tests. These cases exercise the error paths through the JNI layer, and so need
 * no running server.
 */
public class ConnectionStringTest {
    @Test
    public void invalidConnectionStringThrows() {
        TypeDBDriverException error = assertThrows(TypeDBDriverException.class,
                () -> TypeDB.driverFromConnectionString("typedb://admin@https://127.0.0.1:1729"));
        assertTrue(error.getMessage(), error.getMessage().contains("Invalid connection string"));
    }

    @Test
    public void addressWithoutASchemeThrows() {
        TypeDBDriverException error = assertThrows(TypeDBDriverException.class,
                () -> TypeDB.driverFromConnectionString("typedb://admin:password@127.0.0.1:1729"));
        assertTrue(error.getMessage(), error.getMessage().contains("http://"));
    }

    @Test
    public void driverOptionsContradictingTheSchemeThrow() {
        TypeDBDriverException error = assertThrows(TypeDBDriverException.class,
                () -> TypeDB.driverFromConnectionString("typedb://admin:password@https://127.0.0.1:1729",
                        new DriverOptions(DriverTlsConfig.disabled())));
        assertTrue(error.getMessage(), error.getMessage().contains("not compatible"));
    }
}
