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

using System.Collections.Generic;

using TypeDB.Driver.Api;

namespace TypeDB.Driver.Api
{
    /**
     * Provides access to all database management methods.
     */
    public interface IDatabaseManager
    {
        /**
         * Retrieve the database with the given name.
         *
         * <h3>Examples</h3>
         * <pre>
         * driver.Databases.Get(name);
         * </pre>
         *
         * @param name The name of the database to retrieve
         */
        IDatabase Get(string name);

        /**
         * Checks if a database with the given name exists.
         *
         * <h3>Examples</h3>
         * <pre>
         * driver.Databases.Contains(name);
         * </pre>
         *
         * @param name The database name to be checked
         */
        bool Contains(string name);

        /**
         * Create a database with the given name.
         *
         * <h3>Examples</h3>
         * <pre>
         * driver.Databases.Create(name);
         * </pre>
         *
         * @param name The name of the database to be created
         */
        void Create(string name);

        /**
         * Retrieves all databases present on the TypeDB server.
         *
         * <h3>Examples</h3>
         * <pre>
         * driver.Databases.GetAll();
         * </pre>
         */
        IList<IDatabase> GetAll();

        /**
         * Creates a database with the given name based on previously exported data.
         * This is a blocking operation and may take a significant amount of time
         * depending on the database size.
         *
         * <h3>Examples</h3>
         * <pre>
         * driver.Databases.ImportFromFile("mydb", schemaString, "/path/to/data.typedb");
         * </pre>
         *
         * @param name The name of the database to be created.
         * @param schema The schema definition query string for the database.
         * @param dataFile The exported database file path to import the data from, plain or zstd-compressed.
         */
        void ImportFromFile(string name, string schema, string dataFile);
    }
}
