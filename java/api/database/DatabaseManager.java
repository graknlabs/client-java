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

package com.typedb.driver.api.database;

import com.typedb.driver.common.exception.TypeDBDriverException;

import javax.annotation.CheckReturnValue;
import java.util.List;

/**
 * Provides access to all database management methods.
 */
public interface DatabaseManager {
    /**
     * Retrieves all databases present on the TypeDB server.
     *
     * <h3>Examples</h3>
     * <pre>
     * driver.databases().all()
     * </pre>
     */
    @CheckReturnValue
    List<Database> all() throws TypeDBDriverException;

    /**
     * Checks if a database with the given name exists.
     *
     * <h3>Examples</h3>
     * <pre>
     * driver.databases().contains(name)
     * </pre>
     *
     * @param name The database name to be checked
     */
    @CheckReturnValue
    boolean contains(String name) throws TypeDBDriverException;

    /**
     * Retrieves the database with the given name.
     *
     * <h3>Examples</h3>
     * <pre>
     * driver.databases().get(name)
     * </pre>
     *
     * @param name The name of the database to retrieve
     */
    @CheckReturnValue
    Database get(String name) throws TypeDBDriverException;

    /**
     * Creates a database with the given name.
     *
     * <h3>Examples</h3>
     * <pre>
     * driver.databases().create(name)
     * </pre>
     *
     * @param name The name of the database to be created
     */
    void create(String name) throws TypeDBDriverException;

    /**
     * Creates a database with the given name based on previously exported another database's data loaded from a file.
     * This is a blocking operation and may take a significant amount of time depending on the database size.
     *
     * <h3>Examples</h3>
     * <pre>
     * driver.databases().importFromFile(name, schema, "data.typedb")
     * </pre>
     *
     * @param name         The name of the database to be created
     * @param schema       The schema definition query string for the database
     * @param dataFilePath The exported database file path to import the data from, plain or zstd-compressed
     */
    void importFromFile(String name, String schema, String dataFilePath) throws TypeDBDriverException;
}
