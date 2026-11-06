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

use typedb_driver::ServerRouting as NativeServerRouting;

use crate::common::{
    error::unwrap_or_default,
    memory::{borrow_optional, free, release, release_string, string_free, string_view},
};

/// <code>ServerRoutingType</code> is used to represent server routing directives in FFI.
/// It is the tag part, which is combined with optional fields to form an instance of the original
/// enum.
#[repr(C)]
#[derive(Debug, Clone, Copy)]
pub enum ServerRoutingType {
    /// Routing decisions are made automatically by the driver.
    Auto,
    /// The driver is pinned to a specific server address.
    Direct,
}

/// <code>ServerRouting</code> is used to represent server routing directives in FFI.
/// It combines <code>ServerRoutingType</code> and optional fields to form an instance of the
/// original enum.
/// <code>address</code> is not null only when tag is <code>Direct</code>.
#[repr(C)]
#[derive(Debug, Clone)]
pub struct ServerRouting {
    /// The routing variant.
    pub(crate) type_: ServerRoutingType,
    /// Target server address; non-null only when <code>type_</code> is <code>Direct</code>.
    pub(crate) address: *mut c_char,
}

impl ServerRouting {
    fn new_auto() -> Self {
        ServerRouting { type_: ServerRoutingType::Auto, address: std::ptr::null_mut() }
    }

    fn new_direct(address: *mut c_char) -> Self {
        ServerRouting { type_: ServerRoutingType::Direct, address }
    }

    fn to_native(&self) -> NativeServerRouting {
        match self.type_ {
            ServerRoutingType::Auto => NativeServerRouting::Auto,
            ServerRoutingType::Direct => {
                let address = unwrap_or_default(string_view(self.address).parse());
                NativeServerRouting::Direct { address }
            }
        }
    }
}

impl Drop for ServerRouting {
    fn drop(&mut self) {
        string_free(self.address);
    }
}

/// Creates an automatic <code>ServerRouting</code> object.
#[unsafe(no_mangle)]
pub extern "C" fn server_routing_auto() -> *mut ServerRouting {
    release(ServerRouting::new_auto())
}

/// Creates a server-specific <code>ServerRouting</code> object.
///
/// @param address The address of the server to route to.
#[unsafe(no_mangle)]
pub extern "C" fn server_routing_direct(address: *const c_char) -> *mut ServerRouting {
    release(ServerRouting::new_direct(release_string(string_view(address.clone()).to_string())))
}

/// Drops the <code>ServerRouting</code> object.
#[unsafe(no_mangle)]
pub extern "C" fn server_routing_drop(server_routing: *mut ServerRouting) {
    free(server_routing)
}

pub(crate) fn native_server_routing(server_routing: *const ServerRouting) -> Option<NativeServerRouting> {
    borrow_optional(server_routing).map(ServerRouting::to_native)
}

impl Into<NativeServerRouting> for ServerRouting {
    fn into(self) -> NativeServerRouting {
        self.to_native()
    }
}

impl From<NativeServerRouting> for ServerRouting {
    fn from(value: NativeServerRouting) -> Self {
        match value {
            NativeServerRouting::Auto => ServerRouting::new_auto(),
            NativeServerRouting::Direct { address } => ServerRouting::new_direct(release_string(address.to_string())),
        }
    }
}
