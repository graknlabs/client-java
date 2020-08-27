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

package grakn.client.connection;

import grakn.client.Grakn.Client;
import grakn.client.Grakn.DatabaseManager;
import grakn.client.Grakn.QueryOptions;
import grakn.client.Grakn.Session;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.concurrent.TimeUnit;

import static grakn.client.Grakn.Session.Type.DATA;
import static grakn.client.Grakn.Session.Type.SCHEMA;

/**
 * Entry-point which communicates with a running Grakn server using gRPC.
 */
public class GraknClient implements Client {

    public static final String DEFAULT_URI = "localhost:48555";

    private ManagedChannel channel;
    private final DatabaseManager databases;

    public GraknClient() {
        this(DEFAULT_URI);
    }

    public GraknClient(final String address) {
        channel = ManagedChannelBuilder.forTarget(address)
                .usePlaintext().build();
        databases = new GraknDatabaseManager(channel);
    }

    @Override
    public Client overrideChannel(final ManagedChannel channel) {
        this.channel = channel;
        return this;
    }

    public void close() {
        channel.shutdown();
        try {
            channel.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public boolean isOpen() {
        return !channel.isShutdown() && !channel.isTerminated();
    }

    @Override
    public Session session(final String databaseName) {
        return session(databaseName, DATA);
    }

    @Override
    public Session session(final String databaseName, final Session.Type type) {
        return new GraknSession(channel, databaseName, type);
    }

    @Override
    public Session session(final String databaseName, final QueryOptions options) {
        return session(databaseName, DATA, options);
    }

    @Override
    public Session session(final String databaseName, final Session.Type type, final QueryOptions options) {
        return new GraknSession(channel, databaseName, type, options);
    }

    @Override
    public Session schemaSession(final String databaseName) {
        return session(databaseName, SCHEMA);
    }

    @Override
    public Session schemaSession(final String databaseName, final QueryOptions options) {
        return session(databaseName, SCHEMA, options);
    }

    @Override
    public DatabaseManager databases() {
        return databases;
    }
}
