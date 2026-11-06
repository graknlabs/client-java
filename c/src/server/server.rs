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

use std::{ffi::c_char, ptr::addr_of_mut};

use typedb_driver::{Replica, ReplicationRole, Server};

use crate::common::{
    iterator::{CIterator, iterator_next},
    memory::{borrow, free, release_optional_string},
};

/// Iterator over the <code>Server</code> corresponding to each server of a TypeDB cluster.
pub struct ServerIterator(pub(crate) CIterator<Server>);

/// Forwards the <code>ServerIterator</code> and returns the next <code>Server</code> if it exists,
/// or null if there are no more elements.
#[unsafe(no_mangle)]
pub extern "C" fn server_iterator_next(it: *mut ServerIterator) -> *mut Server {
    unsafe { iterator_next(addr_of_mut!((*it).0)) }
}

/// Frees the native rust <code>ServerIterator</code> object.
#[unsafe(no_mangle)]
pub extern "C" fn server_iterator_drop(it: *mut ServerIterator) {
    free(it);
}

/// Frees the native rust <code>Server</code> object.
#[unsafe(no_mangle)]
pub extern "C" fn server_drop(server_info: *mut Server) {
    free(server_info);
}

/// Returns the id of this server.
#[unsafe(no_mangle)]
pub extern "C" fn server_get_id(server_info: *const Server) -> i64 {
    borrow(server_info).id() as i64
}

/// Returns the address this server is hosted at.
#[unsafe(no_mangle)]
pub extern "C" fn server_get_address(server_info: *const Server) -> *mut c_char {
    release_optional_string(borrow(server_info).address().map(|address| address.to_string()))
}

/// Returns whether the role of this server is set.
#[unsafe(no_mangle)]
pub extern "C" fn server_has_role(server_info: *const Server) -> bool {
    borrow(server_info).role().is_some()
}

/// Returns whether this is the primary server of the cluster or any of the supporting roles.
#[unsafe(no_mangle)]
pub extern "C" fn server_get_role(server_info: *const Server) -> ReplicationRole {
    borrow(server_info).role().unwrap()
}

/// Checks whether this is the primary server of the cluster.
#[unsafe(no_mangle)]
pub extern "C" fn server_is_primary(server_info: *const Server) -> bool {
    borrow(server_info).is_primary()
}

/// Returns whether the cluster protocol 'term' of this server exists.
#[unsafe(no_mangle)]
pub extern "C" fn server_has_term(server_info: *const Server) -> bool {
    borrow(server_info).term().is_some()
}

/// Returns the cluster protocol 'term' of this server.
#[unsafe(no_mangle)]
pub extern "C" fn server_get_term(server_info: *const Server) -> i64 {
    borrow(server_info).term().unwrap() as i64
}
