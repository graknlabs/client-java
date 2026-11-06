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

use std::ffi::c_char;

use typedb_driver::Credentials;

use crate::common::memory::{free, release, string_view};

/// Creates a new <code>Credentials</code> for connecting to TypeDB Server.
///
/// @param username The name of the user to connect as
/// @param password The password for the user
#[unsafe(no_mangle)]
pub extern "C" fn credentials_new(username: *const c_char, password: *const c_char) -> *mut Credentials {
    release(Credentials::new(string_view(username), string_view(password)))
}

/// Frees the native rust <code>Credentials</code> object
#[unsafe(no_mangle)]
pub extern "C" fn credentials_drop(credentials: *mut Credentials) {
    free(credentials);
}
