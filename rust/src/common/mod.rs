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

pub use self::{
    address::{Address, Addresses, ConnectionString},
    error::Error,
    promise::{BoxPromise, Promise, box_promise},
    query_options::QueryOptions,
    stream::{BoxStream, box_stream},
    transaction_options::TransactionOptions,
};

pub(crate) mod address;
pub mod error;
mod id;
pub mod info;
#[cfg_attr(not(feature = "sync"), path = "promise_async.rs")]
#[cfg_attr(feature = "sync", path = "promise_sync.rs")]
mod promise;
mod query_options;
#[cfg_attr(not(feature = "sync"), path = "stream_async.rs")]
#[cfg_attr(feature = "sync", path = "stream_sync.rs")]
pub mod stream;
mod transaction_options;

pub(crate) type Callback = Box<dyn FnOnce() + Send>;

pub(crate) type StdResult<T, E> = std::result::Result<T, E>;
pub type Result<T = ()> = StdResult<T, Error>;

pub type IID = id::ID;
pub(crate) type RequestID = id::ID;

/// This enum is used to specify the type of transaction.
///
/// # Examples
///
/// ```rust
/// database.transaction(TransactionType::Read)
/// ```
#[repr(C)]
#[derive(Copy, Clone, Debug, Eq, PartialEq)]
pub enum TransactionType {
    /// Read-only transaction.
    Read = 0,
    /// Data-modifying transaction.
    Write = 1,
    /// Schema-modifying transaction.
    Schema = 2,
}
