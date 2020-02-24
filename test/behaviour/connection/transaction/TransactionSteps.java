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

package grakn.client.test.behaviour.connection.transaction;

import grakn.client.GraknClient;
import grakn.client.test.behaviour.connection.ConnectionSteps;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static java.util.Objects.isNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TransactionSteps {

    // =============================================//
    // sequential sessions, sequential transactions //
    // =============================================//

    @When("for each session, open transaction(s) of type:")
    public void for_each_session_open_transactions_of_type(List<GraknClient.Transaction.Type> types) {
        for (GraknClient.Session session : ConnectionSteps.sessions) {
            List<GraknClient.Transaction> transactions = new ArrayList<>();
            for (GraknClient.Transaction.Type type : types) {
                GraknClient.Transaction transaction = session.transaction(type);
                transactions.add(transaction);
            }
            ConnectionSteps.sessionsToTransactions.put(session, transactions);
        }
    }

    @Then("for each session, transaction(s) is/are null: {bool}")
    public void for_each_session_transactions_are_null(boolean isNull) {
        for_each_session_transactions_are(transaction -> assertEquals(isNull, isNull(transaction)));
    }

    @Then("for each session, transaction(s) is/are open: {bool}")
    public void for_each_session_transactions_are_open(boolean isOpen) {
        for_each_session_transactions_are(transaction -> assertEquals(isOpen, transaction.isOpen()));
    }

    @Then("for each session, transaction commit")
    public void for_each_session_transaction_commit() {
        for (GraknClient.Session session : ConnectionSteps.sessions) {
            for (GraknClient.Transaction transaction : ConnectionSteps.sessionsToTransactions.get(session)) {
                transaction.commit();
            }
        }
    }

    @Then("for each session, transaction commit throws")
    public void for_each_session_transaction_commit_throws() {
        for (GraknClient.Session session : ConnectionSteps.sessions) {
            for (GraknClient.Transaction transaction : ConnectionSteps.sessionsToTransactions.get(session)) {
                boolean threw = false;
                try {
                    transaction.commit();
                } catch (RuntimeException commitException) {
                    threw = true;
                }
                assertTrue(threw);
            }
        }
    }

    @Then("for each session, transaction close")
    public void for_each_session_transaction_close() {
        for (GraknClient.Session session : ConnectionSteps.sessions) {
            for (GraknClient.Transaction transaction : ConnectionSteps.sessionsToTransactions.get(session)) {
                transaction.close();
            }
        }
    }

    private void for_each_session_transactions_are(Consumer<GraknClient.Transaction> assertion) {
        for (GraknClient.Session session : ConnectionSteps.sessions) {
            for (GraknClient.Transaction transaction : ConnectionSteps.sessionsToTransactions.get(session)) {
                assertion.accept(transaction);
            }
        }
    }

    @Then("for each session, transaction(s) has/have type:")
    public void for_each_session_transactions_have_type(List<GraknClient.Transaction.Type> types) {
        for (GraknClient.Session session : ConnectionSteps.sessions) {
            List<GraknClient.Transaction> transactions = ConnectionSteps.sessionsToTransactions.get(session);
            assertEquals(types.size(), transactions.size());

            Iterator<GraknClient.Transaction.Type> typesIterator = types.iterator();
            Iterator<GraknClient.Transaction> transactionIterator = transactions.iterator();
            while (typesIterator.hasNext()){
                assertEquals(typesIterator.next(), transactionIterator.next().type());
            }
        }
    }

    // ===========================================//
    // sequential sessions, parallel transactions //
    // ===========================================//

    @When("for each session, open transaction(s) in parallel of type:")
    public void for_each_session_open_transactions_in_parallel_of_type(List<GraknClient.Transaction.Type> types) {
        assertTrue(ConnectionSteps.THREAD_POOL_SIZE >= types.size());
        for (GraknClient.Session session : ConnectionSteps.sessions) {
            List<CompletableFuture<GraknClient.Transaction>> transactionsParallel = new ArrayList<>();
            for (GraknClient.Transaction.Type type : types) {
                transactionsParallel.add(CompletableFuture.supplyAsync(
                        () -> session.transaction(type),
                        ConnectionSteps.threadPool
                ));
            }
            ConnectionSteps.sessionsToTransactionsParallel.put(session, transactionsParallel);
        }
    }

    @Then("for each session, transactions in parallel are null: {bool}")
    public void for_each_session_transactions_in_parallel_are_null(boolean isNull) {
        for_each_session_transactions_in_parallel_are(transaction -> assertEquals(isNull, isNull(transaction)));
    }

    @Then("for each session, transactions in parallel are open: {bool}")
    public void for_each_session_transactions_in_parallel_are_open(boolean isOpen) {
        for_each_session_transactions_in_parallel_are(transaction -> assertEquals(isOpen, transaction.isOpen()));
    }

    private void for_each_session_transactions_in_parallel_are(Consumer<GraknClient.Transaction> assertion) {
        List<CompletableFuture<Void>> assertions = new ArrayList<>();
        for (GraknClient.Session session : ConnectionSteps.sessions) {
            for (CompletableFuture<GraknClient.Transaction> futureTransaction :
                    ConnectionSteps.sessionsToTransactionsParallel.get(session)) {

                assertions.add(futureTransaction.thenApply(transaction -> {
                    assertion.accept(transaction);
                    return null;
                }));
            }
        }
        CompletableFuture.allOf(assertions.toArray(new CompletableFuture[0]));
    }

    @Then("for each session, transactions in parallel have type:")
    public void for_each_session_transactions_in_parallel_have_type(List<GraknClient.Transaction.Type> types) {
        List<CompletableFuture<Void>> assertions = new ArrayList<>();
        for (GraknClient.Session session : ConnectionSteps.sessions) {
            List<CompletableFuture<GraknClient.Transaction>> futureTxs =
                    ConnectionSteps.sessionsToTransactionsParallel.get(session);

            assertEquals(types.size(), futureTxs.size());

            Iterator<GraknClient.Transaction.Type> typesIter = types.iterator();
            Iterator<CompletableFuture<GraknClient.Transaction>> futureTxsIter = futureTxs.iterator();

            while (typesIter.hasNext()) {
                GraknClient.Transaction.Type type = typesIter.next();
                futureTxsIter.next().thenApplyAsync(tx -> {
                    assertEquals(type, tx.type());
                    return null;
                });
            }
        }

        CompletableFuture.allOf(assertions.toArray(new CompletableFuture[0]));
    }

    // =========================================//
    // parallel sessions, parallel transactions //
    // =========================================//

    @Then("for each session in parallel, transactions in parallel are null: {bool}")
    public void for_each_session_in_parallel_transactions_in_parallel_are_null(boolean isNull) {
        for_each_session_in_parallel_transactions_in_parallel_are(transaction -> assertEquals(isNull, isNull(transaction)));
    }

    @Then("for each session in parallel, transactions in parallel are open: {bool}")
    public void for_each_session_in_parallel_transactions_in_parallel_are_open(boolean isOpen) {
        for_each_session_in_parallel_transactions_in_parallel_are(transaction -> assertEquals(isOpen, transaction.isOpen()));
    }

    private void for_each_session_in_parallel_transactions_in_parallel_are(Consumer<GraknClient.Transaction> assertion) {
        List<CompletableFuture<Void>> assertions = new ArrayList<>();
        for (CompletableFuture<GraknClient.Session> futureSession : ConnectionSteps.sessionsParallel) {
            for (CompletableFuture<GraknClient.Transaction> futureTransaction : ConnectionSteps.sessionsParallelToTransactionsParallel.get(futureSession)) {
                assertions.add(futureTransaction.thenApply(transaction -> {
                    assertion.accept(transaction);
                    return null;
                }));
            }
        }
        CompletableFuture.allOf(assertions.toArray(new CompletableFuture[0]));
    }
}
