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

use typedb_driver::{TypeDBDriver, User, box_stream};

use crate::common::{
    error::{try_release, try_release_optional, unwrap_or_default, unwrap_void},
    iterator::{CIterator, iterator_next},
    memory::{borrow, free, string_view},
};

/// Iterator over a set of <code>User</code>s.
pub struct UserIterator(CIterator<User>);

/// Forwards the <code>UserIterator</code> and returns the next <code>User</code> if it exists,
/// or null if there are no more elements.
#[unsafe(no_mangle)]
pub extern "C" fn user_iterator_next(it: *mut UserIterator) -> *mut User {
    unsafe { iterator_next(addr_of_mut!((*it).0)) }
}

/// Frees the native rust <code>UserIterator</code> object.
#[unsafe(no_mangle)]
pub extern "C" fn user_iterator_drop(it: *mut UserIterator) {
    free(it);
}

/// Retrieves all users which exist on the TypeDB server.
///
/// @param driver The <code>TypeDBDriver</code> object.
#[unsafe(no_mangle)]
pub extern "C" fn users_all(driver: *const TypeDBDriver) -> *mut UserIterator {
    try_release(borrow(driver).users().all().map(|users| UserIterator(CIterator(box_stream(users.into_iter())))))
}

/// Checks if a user with the given name exists.
///
/// @param driver The <code>TypeDBDriver</code> object.
/// @param username The username of the user.
#[unsafe(no_mangle)]
pub extern "C" fn users_contains(driver: *const TypeDBDriver, username: *const c_char) -> bool {
    unwrap_or_default(borrow(driver).users().contains(string_view(username)))
}

/// Retrieves a user with the given name.
///
/// @param driver The <code>TypeDBDriver</code> object.
/// @param username The username of the user.
#[unsafe(no_mangle)]
pub extern "C" fn users_get(driver: *const TypeDBDriver, username: *const c_char) -> *mut User {
    try_release_optional(borrow(driver).users().get(string_view(username)).transpose())
}

/// Retrieves the username of the user who opened this connection.
///
/// @param driver The <code>TypeDBDriver</code> object.
#[unsafe(no_mangle)]
pub extern "C" fn users_get_current(driver: *const TypeDBDriver) -> *mut User {
    try_release_optional(borrow(driver).users().get_current().transpose())
}

/// Creates a user with the given name &amp; password.
///
/// @param username The username of the created user.
/// @param password The password of the created user.
#[unsafe(no_mangle)]
pub extern "C" fn users_create(driver: *const TypeDBDriver, username: *const c_char, password: *const c_char) {
    unwrap_void(borrow(driver).users().create(string_view(username), string_view(password)));
}
