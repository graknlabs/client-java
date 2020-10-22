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

package grakn.client;

import grakn.client.concept.ConceptManager;
import grakn.client.query.QueryManager;

import java.util.List;

public interface Grakn {

    interface Client extends AutoCloseable {

        Session session(String databaseName, Session.Type type);

        Session session(String databaseName, Session.Type type, GraknOptions options);

        DatabaseManager databases();

        boolean isOpen();

        void close();
    }

    interface DatabaseManager {

        boolean contains(String name);

        void create(String name);

        void delete(String name);

        List<String> all();
    }

    interface Session extends AutoCloseable {

        Transaction transaction(Transaction.Type type);

        Transaction transaction(Transaction.Type type, GraknOptions options);

        Session.Type type();

        boolean isOpen();

        void close();

        String databaseName();

        enum Type {
            DATA(0),
            SCHEMA(1);

            private final int id;
            private final boolean isSchema;

            Type(int id) {
                this.id = id;
                this.isSchema = id == 1;
            }

            public static Type of(int value) {
                for (Type t : values()) {
                    if (t.id == value) return t;
                }
                return null;
            }

            public int id() {
                return id;
            }

            public boolean isData() { return !isSchema; }

            public boolean isSchema() { return isSchema; }
        }
    }

    interface Transaction extends AutoCloseable {

        Transaction.Type type();

        boolean isOpen();

        ConceptManager concepts();

        QueryManager query();

        void commit();

        void rollback();

        void close();

        enum Type {
            READ(0),
            WRITE(1);

            private final int id;
            private final boolean isWrite;

            Type(int id) {
                this.id = id;
                this.isWrite = id == 1;
            }

            public static Type of(int value) {
                for (Type t : values()) {
                    if (t.id == value) return t;
                }
                return null;
            }

            public int id() {
                return id;
            }

            public boolean isRead() { return !isWrite; }

            public boolean isWrite() { return isWrite; }
        }
    }
}
