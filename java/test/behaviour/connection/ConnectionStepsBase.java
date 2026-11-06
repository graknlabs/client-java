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

package com.typedb.driver.test.behaviour.connection;

import com.typedb.driver.TypeDB;
import com.typedb.driver.api.Credentials;
import com.typedb.driver.api.Driver;
import com.typedb.driver.api.DriverOptions;
import com.typedb.driver.api.DriverTlsConfig;
import com.typedb.driver.api.QueryOptions;
import com.typedb.driver.api.ServerRouting;
import com.typedb.driver.api.Transaction;
import com.typedb.driver.api.TransactionOptions;
import com.typedb.driver.api.server.ReplicationRole;
import com.typedb.driver.api.server.Server;
import com.typedb.driver.api.server.ServerVersion;
import com.typedb.driver.test.behaviour.config.Parameters;
import com.typedb.driver.test.behaviour.util.Util;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static com.typedb.driver.test.behaviour.util.Util.createTempDir;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public abstract class ConnectionStepsBase {
    public static final String ADMIN_USERNAME = "admin";
    public static final String ADMIN_PASSWORD = "password";
    public static final Credentials DEFAULT_CREDENTIALS = new Credentials(ADMIN_USERNAME, ADMIN_PASSWORD);
    public static int THREAD_POOL_SIZE = 32;
    public static ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    public static Driver driver;
    public static Driver backgroundDriver;
    private static Optional<Path> tempDir = Optional.empty();
    public static List<Transaction> transactions = new ArrayList<>();
    public static List<Transaction> backgroundTransactions = new ArrayList<>();
    public static List<CompletableFuture<Transaction>> transactionsParallel = new ArrayList<>();

    public static Optional<ServerRouting> operationServerRouting = Optional.empty();
    public static DriverOptions driverOptions = new DriverOptions(DriverTlsConfig.disabled());
    public static Optional<TransactionOptions> transactionOptions = Optional.empty();
    public static Optional<QueryOptions> queryOptions = Optional.empty();
    static boolean isBeforeAllRan = false;
    static final int BEFORE_TIMEOUT_MILLIS = 10;

    public static Path tempDir() {
        if (tempDir.isEmpty()) {
            tempDir = Optional.of(createTempDir());
        }
        return tempDir.get();
    }

    public static Path fullPath(String fileName) {
        return tempDir().resolve(fileName);
    }

    public static Transaction tx() {
        return transactions.get(0);
    }

    public static Transaction txPop() {
        return transactions.remove(0);
    }

    public static Optional<Transaction> txOpt() {
        return transactions.isEmpty() ? Optional.empty() : Optional.of(transactions.get(0));
    }

    void beforeAll() {
    } // Can add "before all" setup steps here

    DriverOptions defaultDriverOptions() {
        return new DriverOptions(DriverTlsConfig.disabled());
    }

    void before() {
        try {
            // Sleep between scenarios to let the driver close completely
            // (`close` is not synced and can cause lock failures in CI)
            // TODO: This might be a bug requiring an investigation. For some reason, it only happens in Java (even not in Python!)
            Thread.sleep(BEFORE_TIMEOUT_MILLIS);
        } catch (InterruptedException e) {
            throw new RuntimeException("Unexpected exception while sleeping in before:" + e);
        }

        if (!isBeforeAllRan) {
            try {
                beforeAll();
            } finally {
                isBeforeAllRan = true;
            }
        }

        backgroundDriver = createDefaultTypeDBDriver();
    }

    void after() {
        tempDir.ifPresent(Util::deleteDir);
        tempDir = Optional.empty();

        cleanupTransactions();
        cleanupBackgroundTransactions();
        operationServerRouting = Optional.empty();
        transactionOptions = Optional.empty();
        queryOptions = Optional.empty();

        if (driver.isOpen()) {
            driver.close();
        }
        backgroundDriver.close();
        driverOptions = defaultDriverOptions();
        driver = createDefaultTypeDBDriver();
        driver.users().all().stream().filter(user -> !user.name().equals(ADMIN_USERNAME)).forEach(user -> driver.users().get(user.name()).delete());
        driver.users().get(ADMIN_USERNAME).updatePassword(ADMIN_PASSWORD);
        driver.databases().all().forEach(database -> driver.databases().get(database.name()).delete());
        driver.close();
    }

    void cleanupTransactions() {
        transactions.parallelStream().forEach(Transaction::close);
        transactions.clear();

        Stream<CompletableFuture<Void>> closures = transactionsParallel
                .stream().map(futureTransaction -> futureTransaction.thenApplyAsync(transaction -> {
                    transaction.close();
                    return null;
                }));
        CompletableFuture.allOf(closures.toArray(CompletableFuture[]::new)).join();
        transactionsParallel.clear();
    }

    void cleanupBackgroundTransactions() {
        backgroundTransactions.parallelStream().forEach(Transaction::close);
        backgroundTransactions.clear();
    }

    abstract Driver createDefaultTypeDBDriver();

    /** The HTTP endpoint's default port, which a connection string written for the HTTP driver names. */
    static final String OTHER_PROTOCOL_DEFAULT_PORT = "8000";

    /**
     * A connection string for the given addresses with admin credentials, using the scheme that agrees
     * with the current driver options' TLS setting. Each port is replaced by {@code port} if given.
     */
    String connectionString(Iterable<String> addresses, String port) {
        String scheme = driverOptions.tlsConfig().isEnabled() ? "https" : "http";
        StringJoiner joined = new StringJoiner(",");
        for (String address : addresses) {
            int separator = address.lastIndexOf(':');
            String host = address.substring(0, separator);
            joined.add(scheme + "://" + host + ":" + (port == null ? address.substring(separator + 1) : port));
        }
        return "typedb://" + ADMIN_USERNAME + ":" + ADMIN_PASSWORD + "@" + joined;
    }

    Driver createTypeDBDriverFromConnectionString(String connectionString) {
        return TypeDB.driverFromConnectionString(connectionString, driverOptions);
    }

    public static void initTransactionOptionsIfNeeded() {
        if (transactionOptions.isEmpty()) {
            transactionOptions = Optional.of(new TransactionOptions());
        }
    }

    public static void initQueryOptionsIfNeeded() {
        if (queryOptions.isEmpty()) {
            queryOptions = Optional.of(new QueryOptions());
        }
    }

    ServerVersion getServerVersion() {
        if (operationServerRouting.isPresent()) {
            return driver.serverVersion(operationServerRouting.get());
        } else {
            return driver.serverVersion();
        }
    }

    Set<? extends Server> getServers() {
        if (operationServerRouting.isPresent()) {
            return driver.servers(operationServerRouting.get());
        } else {
            return driver.servers();
        }
    }

    Optional<? extends Server> getPrimaryServer() {
        if (operationServerRouting.isPresent()) {
            return driver.primaryServer(operationServerRouting.get());
        } else {
            return driver.primaryServer();
        }
    }

    abstract void connection_opens_with_default_authentication(Parameters.MayError mayError);

    abstract void connection_opens_with_username_password(String username, String password, Parameters.MayError mayError);

    void connection_closes() {
        cleanupTransactions();
        driver.close();
    }

    void connection_is_open(boolean isOpen) {
        assertEquals(isOpen, driver != null && driver.isOpen());
    }

    void connection_contains_distribution(Parameters.MayError mayError) {
        mayError.check(() -> assertFalse(getServerVersion().getDistribution().isEmpty()));
    }

    void connection_contains_version(Parameters.MayError mayError) {
        mayError.check(() -> assertFalse(getServerVersion().getVersion().isEmpty()));
    }

    void connection_has_count_servers(int count) {
        assertEquals(getServers().size(), count);
    }

    void connection_primary_server_exists() {
        assertTrue(getPrimaryServer().isPresent());
    }

    void connection_get_server_exists(String address, Parameters.ExistsOrDoesnt existsOrDoesnt) {
        boolean exists = getServers().stream().anyMatch(r -> r.getAddress().equals(address));
        existsOrDoesnt.check(exists);
    }

    void connection_get_server_has_term(String address) {
        var server = getServers().stream().filter(r -> r.getAddress().equals(address)).findFirst();
        Parameters.ExistsOrDoesnt.DOES.check(server.isPresent());
        // term should exist (can be any value >= 0)
    }

    void connection_servers_have_roles(List<String> roles) {
        int expectedPrimaryCount = 0;
        int expectedSecondaryCount = 0;
        int expectedCandidateCount = 0;

        for (String role : roles) {
            switch (role.toLowerCase()) {
                case "primary":
                    expectedPrimaryCount++;
                    break;
                case "secondary":
                    expectedSecondaryCount++;
                    break;
                case "candidate":
                    expectedCandidateCount++;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown server replication role: " + role);
            }
        }

        var servers = getServers();
        int actualPrimaryCount = (int) servers.stream()
                .filter(r -> r.getRole().map(ReplicationRole::isPrimary).orElse(false)).count();
        int actualSecondaryCount = (int) servers.stream()
                .filter(r -> r.getRole().map(ReplicationRole::isSecondary).orElse(false)).count();
        int actualCandidateCount = (int) servers.stream()
                .filter(r -> r.getRole().map(ReplicationRole::isCandidate).orElse(false)).count();

        assertEquals("Primary server count mismatch", expectedPrimaryCount, actualPrimaryCount);
        assertEquals("Secondary server count mismatch", expectedSecondaryCount, actualSecondaryCount);
        assertEquals("Candidate server count mismatch", expectedCandidateCount, actualCandidateCount);
    }

    void connection_has_count_databases(int count) {
        assertEquals(count, driver.databases().all().size());
    }

    void connection_has_count_users(int count) {
        assertEquals(count, driver.users().all().size());
    }

    void set_operation_server_routing(Parameters.Routing serverRouting) {
        operationServerRouting = Optional.of(serverRouting.serverRouting());
    }

    void set_driver_option_primary_failover_retries_to(int value) {
        driverOptions = driverOptions.primaryFailoverRetries(value);
    }

    void set_driver_option_request_timeout_millis_to(int value) {
        driverOptions = driverOptions.requestTimeoutMillis(value);
    }

}
