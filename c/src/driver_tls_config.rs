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

use typedb_driver::DriverTlsConfig;

use crate::common::{
    error::try_release,
    memory::{borrow, free, release, release_string, string_view},
};

/// Creates a new <code>DriverTlsConfig</code> with TLS disabled.
#[unsafe(no_mangle)]
pub extern "C" fn driver_tls_config_new_disabled() -> *mut DriverTlsConfig {
    release(DriverTlsConfig::disabled())
}

/// Creates a new <code>DriverTlsConfig</code> with TLS enabled using system native trust roots.
#[unsafe(no_mangle)]
pub extern "C" fn driver_tls_config_new_enabled_with_native_root_ca() -> *mut DriverTlsConfig {
    release(DriverTlsConfig::enabled_with_native_root_ca())
}

/// Creates a new <code>DriverTlsConfig</code> with TLS enabled using a custom root CA certificate (PEM).
#[unsafe(no_mangle)]
pub extern "C" fn driver_tls_config_new_enabled_with_root_ca_path(tls_root_ca: *const c_char) -> *mut DriverTlsConfig {
    let tls_root_ca_path = Path::new(string_view(tls_root_ca));
    try_release(DriverTlsConfig::enabled_with_root_ca(tls_root_ca_path))
}

/// Frees the native rust <code>DriverTlsConfig</code> object.
#[unsafe(no_mangle)]
pub extern "C" fn driver_tls_config_drop(tls_config: *mut DriverTlsConfig) {
    free(tls_config);
}

/// Returns whether TLS is enabled.
#[unsafe(no_mangle)]
pub extern "C" fn driver_tls_config_is_enabled(tls_config: *const DriverTlsConfig) -> bool {
    borrow(tls_config).is_enabled()
}

/// Returns whether a custom root CA path is set.
#[unsafe(no_mangle)]
pub extern "C" fn driver_tls_config_has_root_ca_path(tls_config: *const DriverTlsConfig) -> bool {
    borrow(tls_config).root_ca_path().is_some()
}

/// Returns the TLS root CA set in this <code>DriverTlsConfig</code> object.
/// Panics if a custom root CA is absent.
#[unsafe(no_mangle)]
pub extern "C" fn driver_tls_config_get_root_ca_path(tls_config: *const DriverTlsConfig) -> *mut c_char {
    release_string(borrow(tls_config).root_ca_path().unwrap().to_string_lossy().to_string())
}
