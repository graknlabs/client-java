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

import grakn.client.answer.Answer;
import grakn.client.answer.AnswerGroup;
import grakn.client.answer.ConceptList;
import grakn.client.answer.ConceptMap;
import grakn.client.answer.ConceptSet;
import grakn.client.answer.ConceptSetMeasure;
import grakn.client.answer.Explanation;
import grakn.client.answer.Numeric;
import grakn.client.answer.Void;
import grakn.client.common.parameters.Options;
import grakn.client.concept.Concepts;
import grakn.client.connection.GraknClient;
import grakn.client.connection.GraknDatabase;
import graql.lang.query.GraqlCompute;
import graql.lang.query.GraqlDefine;
import graql.lang.query.GraqlDelete;
import graql.lang.query.GraqlGet;
import graql.lang.query.GraqlInsert;
import graql.lang.query.GraqlQuery;
import graql.lang.query.GraqlUndefine;
import io.grpc.ManagedChannel;

import javax.annotation.CheckReturnValue;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public interface Grakn {

    static Client client() {
        return new GraknClient();
    }

    static Client client(String address) {
        return new GraknClient(address);
    }

    interface Client extends AutoCloseable {

        GraknClient overrideChannel(ManagedChannel channel);

        boolean isOpen();

        void close();

        Session session(String databaseName);

        Session session(String databaseName, Session.Type type);

        Session session(String databaseName, Options.Session options);

        Session session(String databaseName, Session.Type type, Options.Session options);

        Session schemaSession(String databaseName);

        Session schemaSession(String databaseName, Options.Session options);

        DatabaseManager databases();
    }

    /**
     * Manages a collection of Grakn databases.
     */
    interface DatabaseManager {

        boolean contains(String name);

        void create(String name);

        void delete(String name);

        List<String> all();
    }

    interface Database extends Serializable {

        @CheckReturnValue
        static Database of(String name) {
            return new GraknDatabase(name);
        }

        @CheckReturnValue
        String name();
    }

    /**
     * @see Transaction
     * @see Client
     */
    interface Session extends AutoCloseable {

        Transaction.Builder transaction();

        Transaction.Builder transaction(Options.Transaction options);

        Transaction transaction(Transaction.Type type);

        Transaction transaction(Transaction.Type type, Options.Transaction options);

        boolean isOpen();

        void close();

        Database database();

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

        Session session();

        Database database();

        void close();

        QueryFuture<List<ConceptMap>> execute(GraqlDefine query);

        QueryFuture<List<ConceptMap>> execute(GraqlUndefine query);

        QueryFuture<List<ConceptMap>> execute(GraqlInsert query, Options.Query options);

        QueryFuture<List<ConceptMap>> execute(GraqlInsert query);

        QueryFuture<List<Void>> execute(GraqlDelete query, Options.Query options);

        QueryFuture<List<Void>> execute(GraqlDelete query);

        QueryFuture<List<ConceptMap>> execute(GraqlGet query, Options.Query options);

        QueryFuture<List<ConceptMap>> execute(GraqlGet query);

        QueryFuture<Stream<ConceptMap>> stream(GraqlDefine query);

        QueryFuture<Stream<ConceptMap>> stream(GraqlUndefine query);

        QueryFuture<Stream<ConceptMap>> stream(GraqlInsert query, Options.Query options);

        QueryFuture<Stream<ConceptMap>> stream(GraqlInsert query);

        QueryFuture<Stream<Void>> stream(GraqlDelete query, Options.Query options);

        QueryFuture<Stream<Void>> stream(GraqlDelete query);

        QueryFuture<Stream<ConceptMap>> stream(GraqlGet query, Options.Query options);

        QueryFuture<Stream<ConceptMap>> stream(GraqlGet query);

        QueryFuture<List<Numeric>> execute(GraqlGet.Aggregate query);

        QueryFuture<List<Numeric>> execute(GraqlGet.Aggregate query, Options.Query options);

        QueryFuture<Stream<Numeric>> stream(GraqlGet.Aggregate query);

        QueryFuture<Stream<Numeric>> stream(GraqlGet.Aggregate query, Options.Query options);

        QueryFuture<List<AnswerGroup<ConceptMap>>> execute(GraqlGet.Group query);

        QueryFuture<List<AnswerGroup<ConceptMap>>> execute(GraqlGet.Group query, Options.Query options);

        QueryFuture<Stream<AnswerGroup<ConceptMap>>> stream(GraqlGet.Group query);

        QueryFuture<Stream<AnswerGroup<ConceptMap>>> stream(GraqlGet.Group query, Options.Query options);

        QueryFuture<List<AnswerGroup<Numeric>>> execute(GraqlGet.Group.Aggregate query);

        QueryFuture<List<AnswerGroup<Numeric>>> execute(GraqlGet.Group.Aggregate query, Options.Query options);

        QueryFuture<Stream<AnswerGroup<Numeric>>> stream(GraqlGet.Group.Aggregate query);

        QueryFuture<Stream<AnswerGroup<Numeric>>> stream(GraqlGet.Group.Aggregate query, Options.Query options);

        QueryFuture<List<Numeric>> execute(GraqlCompute.Statistics query);

        QueryFuture<Stream<Numeric>> stream(GraqlCompute.Statistics query);

        QueryFuture<List<ConceptList>> execute(GraqlCompute.Path query);

        QueryFuture<Stream<ConceptList>> stream(GraqlCompute.Path query);

        QueryFuture<List<ConceptSetMeasure>> execute(GraqlCompute.Centrality query);

        QueryFuture<Stream<ConceptSetMeasure>> stream(GraqlCompute.Centrality query);

        QueryFuture<List<ConceptSet>> execute(GraqlCompute.Cluster query);

        QueryFuture<Stream<ConceptSet>> stream(GraqlCompute.Cluster query);

        QueryFuture<? extends List<? extends Answer>> execute(GraqlQuery query);

        QueryFuture<? extends List<? extends Answer>> execute(GraqlQuery query, Options.Query options);

        QueryFuture<? extends Stream<? extends Answer>> stream(GraqlQuery query);

        QueryFuture<? extends Stream<? extends Answer>> stream(GraqlQuery query, Options.Query options);

        boolean isOpen();

        Concepts concepts();

        void commit();

        Explanation getExplanation(ConceptMap explainable);

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

        interface Builder {

            /**
             * Read-only transaction, where database mutation is prohibited
             */
            Transaction read();

            /**
             * Write transaction, where database mutation is allowed
             */
            Transaction write();
        }

        /**
         * An extension of @code Future that catches @code InterruptedException and @code ExecutionException.
         */
        interface QueryFuture<T> extends Future<T> {
            @Override
            T get();

            @Override
            T get(long timeout, TimeUnit unit);
        }
    }
}
