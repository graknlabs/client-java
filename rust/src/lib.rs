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

#![deny(elided_lifetimes_in_paths)]
#![deny(unused_must_use)]
#![allow(clippy::collapsible_if, reason = "too many false positives")]
#![allow(clippy::collapsible_else_if, reason = "too many false positives")]
#![allow(clippy::module_inception, reason = "too many false positives")]
#![allow(clippy::too_many_arguments, reason = "too many false positives")]
#![allow(clippy::type_complexity, reason = "too many false positives")]

#[doc(hidden)]
pub use self::connection::database::import_stream::DatabaseImportStream;
pub use self::{
    common::{
        Address, Addresses, BoxPromise, BoxStream, ConnectionString, Error, IID, Promise, QueryOptions, Result,
        TransactionOptions,
        TransactionType, box_stream, error, info,
    },
    connection::{
        Credentials, DriverOptions, DriverTlsConfig,
        server::{
            AvailableServer, Replica, ReplicationRole, Server, server_routing::ServerRouting,
            server_version::ServerVersion,
        },
    },
    database::{Database, DatabaseManager},
    driver::TypeDBDriver,
    transaction::Transaction,
    user::{User, UserManager},
};

pub mod analyze;
pub mod answer;
mod common;
pub mod concept;
mod connection;
mod database;
pub mod driver;
pub mod given;
pub mod transaction;
mod user;
