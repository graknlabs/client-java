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

use std::{ffi::c_char, path::Path};

use typedb_driver::{Credentials, DriverOptions, TypeDBDriver};

use super::{
    error::{try_release, unwrap_void},
    memory::{borrow, free, string_view},
};
use crate::memory::release;

const DRIVER_LANG: &str = "c";

/// Open a TypeDB C Driver to a TypeDB server available at the provided address.
///
/// @param address The address (host:port) on which the TypeDB Server is running
/// @param credentials The <code>Credentials</code> to connect with
/// @param driver_options The <code>DriverOptions</code> to connect with
#[unsafe(no_mangle)]
pub extern "C" fn driver_open(
    address: *const c_char,
    credentials: *const Credentials,
    driver_options: *const DriverOptions,
) -> *mut TypeDBDriver {
    try_release(TypeDBDriver::new_with_description(
        string_view(address),
        borrow(credentials).clone(),
        borrow(driver_options).clone(),
        DRIVER_LANG,
    ))
}

/// Open a TypeDB Driver to a TypeDB server available at the provided address.
/// This method allows driver language specification for drivers built on top of the native C layer.
///
/// @param address The address (host:port) on which the TypeDB Server is running
/// @param credentials The <code>Credentials</code> to connect with
/// @param driver_options The <code>DriverOptions</code> to connect with
/// @param driver_lang The language of the driver connecting to the server
#[unsafe(no_mangle)]
pub extern "C" fn driver_open_with_description(
    address: *const c_char,
    credentials: *const Credentials,
    driver_options: *const DriverOptions,
    driver_lang: *const c_char,
) -> *mut TypeDBDriver {
    try_release(TypeDBDriver::new_with_description(
        string_view(address),
        borrow(credentials).clone(),
        borrow(driver_options).clone(),
        string_view(driver_lang),
    ))
}

/// Closes the driver. Before instantiating a new driver, the driver that's currently open should first be closed.
/// Closing a driver frees the underlying Rust object.
#[unsafe(no_mangle)]
pub extern "C" fn driver_close(driver: *mut TypeDBDriver) {
    free(driver);
}

/// Checks whether this connection is presently open.
#[unsafe(no_mangle)]
pub extern "C" fn driver_is_open(driver: *const TypeDBDriver) -> bool {
    borrow(driver).is_open()
}

/// Forcibly closes the driver. To be used in exceptional cases.
#[unsafe(no_mangle)]
pub extern "C" fn driver_force_close(driver: *mut TypeDBDriver) {
    unwrap_void(borrow(driver).force_close());
}

// Creates a new <code>Credentials</code> for connecting to TypeDB Server.
//
// @param username The name of the user to connect as
// @param password The password for the user
#[unsafe(no_mangle)]
pub extern "C" fn credentials_new(username: *const c_char, password: *const c_char) -> *mut Credentials {
    release(Credentials::new(string_view(username), string_view(password)))
}

// Frees the native rust <code>Credentials</code> object
#[unsafe(no_mangle)]
pub extern "C" fn credentials_drop(credentials: *mut Credentials) {
    free(credentials);
}

// Creates a new <code>DriverOptions</code> for connecting to TypeDB Server.
//
// @param tls_root_ca Path to the CA certificate to use for authenticating server certificates.
// @param with_tls Specify whether the connection to TypeDB Cloud must be done over TLS
#[unsafe(no_mangle)]
pub extern "C" fn driver_options_new(is_tls_enabled: bool, tls_root_ca: *const c_char) -> *mut DriverOptions {
    let tls_root_ca_path = unsafe { tls_root_ca.as_ref().map(|str| Path::new(string_view(str))) };
    try_release(DriverOptions::new(is_tls_enabled, tls_root_ca_path))
}

// Frees the native rust <code>DriverOptions</code> object
#[unsafe(no_mangle)]
pub extern "C" fn driver_options_drop(driver_options: *mut DriverOptions) {
    free(driver_options);
}
