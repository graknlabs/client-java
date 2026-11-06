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

use std::{ffi::c_char, ptr::null_mut};

use typedb_driver::{
    Error, Promise, QueryOptions, Transaction, TransactionOptions, TransactionType, TypeDBDriver, given::GivenRows,
};

use crate::{
    analyze::AnalyzedQueryPromise,
    answer::QueryAnswerPromise,
    common::{
        error::try_release,
        memory::{borrow, borrow_mut, free, release, string_view, take_ownership},
        promise::VoidPromise,
    },
};

/// Opens a transaction to perform read or write queries on the database connected to the session.
///
/// @param databases The <code>DatabaseManager</code> object on this connection.
/// @param database_name The name of the database with which the transaction connects.
/// @param type_ The type of transaction to be created (Write / Read / Schema).
/// @param options <code>TransactionOptions</code> to configure the opened transaction.
#[unsafe(no_mangle)]
pub extern "C" fn transaction_new(
    driver: *mut TypeDBDriver,
    database_name: *const c_char,
    type_: TransactionType,
    options: *const TransactionOptions,
) -> *mut Transaction {
    try_release(borrow(driver).transaction_with_options(string_view(database_name), type_, *borrow(options)))
}

/// Performs a TypeQL query in the transaction.
///
/// @param transaction The <code>Transaction</code> to execute the query within.
/// @param query The query string.
/// @param options <code>QueryOptions</code> to configure the executed query.
#[unsafe(no_mangle)]
pub extern "C" fn transaction_query(
    transaction: *mut Transaction,
    query: *const c_char,
    options: *const QueryOptions,
) -> *mut QueryAnswerPromise {
    release(QueryAnswerPromise::new(Box::new(
        borrow(transaction).query_with_options(string_view(query), *borrow(options)),
    )))
}

/// Performs a TypeQL query in the transaction with the given rows
///
/// @param transaction The <code>Transaction</code> to execute the query within.
/// @param query The query string.
/// @param options <code>QueryOptions</code> to configure the executed query.
/// @param given_rows <code>QueryGivenRows</code> the rows given as input to the query.
#[unsafe(no_mangle)]
pub extern "C" fn transaction_query_given_rows(
    transaction: *mut Transaction,
    query: *const c_char,
    options: *const QueryOptions,
    given_rows: *mut GivenRows,
) -> *mut QueryAnswerPromise {
    release(QueryAnswerPromise::new(Box::new(borrow(transaction).query_with_options_and_rows(
        string_view(query),
        *borrow(options),
        Some(take_ownership(given_rows)),
    ))))
}

/// Analyzes a TypeQL query in the transaction.
///
/// @param transaction The <code>Transaction</code> to analyze the query within.
/// @param query The query string.
#[unsafe(no_mangle)]
pub extern "C" fn transaction_analyze(
    transaction: *mut Transaction,
    query: *const c_char,
) -> *mut AnalyzedQueryPromise {
    release(AnalyzedQueryPromise::new(Box::new(borrow(transaction).analyze(string_view(query)))))
}

/// Closes the transaction, waits for all callbacks to complete, then frees the memory.
#[unsafe(no_mangle)]
pub extern "C" fn transaction_drop_sync(txn: *mut Transaction) {
    if txn.is_null() {
        return;
    }

    // First, close the transaction and wait for callbacks to complete
    // This ensures all OnClose callbacks are invoked before we free the memory
    let _ = borrow_mut(txn).close().resolve();

    // Now safe to free the memory - all callbacks have been invoked
    free(txn);
}

/// Forcibly closes this transaction. Returns a resolvable promise.
#[unsafe(no_mangle)]
pub extern "C" fn transaction_close(txn: *mut Transaction) -> *mut VoidPromise {
    release(VoidPromise(Box::new(borrow_mut(txn).close())))
}

/// Commits the changes made via this transaction to the TypeDB database.
/// Whether or not the transaction is commited successfully, the transaction is closed after
/// the commit call and the native rust object is freed.
#[unsafe(no_mangle)]
pub extern "C" fn transaction_commit(txn: *mut Transaction) -> *mut VoidPromise {
    release(VoidPromise(Box::new(take_ownership(txn).commit())))
}

/// Rolls back the uncommitted changes made via this transaction.
#[unsafe(no_mangle)]
pub extern "C" fn transaction_rollback(txn: *const Transaction) -> *mut VoidPromise {
    release(VoidPromise(Box::new(borrow(txn).rollback())))
}

/// Checks whether this transaction is open.
#[unsafe(no_mangle)]
pub extern "C" fn transaction_is_open(txn: *const Transaction) -> bool {
    borrow(txn).is_open()
}

/// Registers a callback function which will be executed when this transaction is closed.
///
/// @param txn The transaction on which to register the callback
/// @param callback_id The argument to be passed to the callback function when it is executed.
/// @param callback The function to be called
#[unsafe(no_mangle)]
pub extern "C" fn transaction_on_close(
    txn: *const Transaction,
    callback_id: usize,
    callback: extern "C" fn(usize, *mut Error),
) -> *mut VoidPromise {
    release(VoidPromise(Box::new(
        borrow(txn).on_close(move |error| callback(callback_id, error.map(|err| release(err)).unwrap_or(null_mut()))),
    )))
}
