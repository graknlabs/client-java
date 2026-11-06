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

use typedb_driver::QueryOptions;

use crate::common::memory::{borrow, borrow_mut, free, release};

/// Produces a new <code>QueryOptions</code> object.
#[unsafe(no_mangle)]
pub extern "C" fn query_options_new() -> *mut QueryOptions {
    release(QueryOptions::new())
}

/// Frees the native Rust <code>QueryOptions</code> object.
#[unsafe(no_mangle)]
pub extern "C" fn query_options_drop(options: *mut QueryOptions) {
    free(options);
}

/// Explicitly sets the "include instance types" flag.
/// If set, specifies if types should be included in instance structs returned in ConceptRow answers.
/// This option allows reducing the amount of unnecessary data transmitted.
#[unsafe(no_mangle)]
pub extern "C" fn query_options_set_include_instance_types(options: *mut QueryOptions, include_instance_types: bool) {
    borrow_mut(options).include_instance_types = Some(include_instance_types);
}

/// Returns the value set for the "include instance types" flag in this <code>QueryOptions</code> object.
/// If set, specifies if types should be included in instance structs returned in ConceptRow answers.
/// This option allows reducing the amount of unnecessary data transmitted.
#[unsafe(no_mangle)]
pub extern "C" fn query_options_get_include_instance_types(options: *const QueryOptions) -> bool {
    borrow(options).include_instance_types.unwrap()
}

/// Checks whether the "include instance types" flag was explicitly set for this <code>QueryOptions</code> object.
#[unsafe(no_mangle)]
pub extern "C" fn query_options_has_include_instance_types(options: *const QueryOptions) -> bool {
    borrow(options).include_instance_types.is_some()
}

/// Explicitly sets the prefetch size.
/// If set, specifies the number of extra query responses sent before the client side has to re-request more responses.
/// Increasing this may increase performance for queries with a huge number of answers, as it can
/// reduce the number of network round-trips at the cost of more resources on the server side.
/// Minimal value: 1.
#[unsafe(no_mangle)]
pub extern "C" fn query_options_set_prefetch_size(options: *mut QueryOptions, prefetch_size: i64) {
    borrow_mut(options).prefetch_size = Some(prefetch_size as u64);
}

/// Returns the value set for the prefetch size in this <code>QueryOptions</code> object.
/// If set, specifies the number of extra query responses sent before the client side has to re-request more responses.
/// Increasing this may increase performance for queries with a huge number of answers, as it can
/// reduce the number of network round-trips at the cost of more resources on the server side.
/// Minimal value: 1.
#[unsafe(no_mangle)]
pub extern "C" fn query_options_get_prefetch_size(options: *const QueryOptions) -> i64 {
    borrow(options).prefetch_size.unwrap() as i64
}

/// Checks whether the prefetch size was explicitly set for this <code>QueryOptions</code> object.
#[unsafe(no_mangle)]
pub extern "C" fn query_options_has_prefetch_size(options: *const QueryOptions) -> bool {
    borrow(options).prefetch_size.is_some()
}

/// Explicitly set the prefetch size.
/// If set, it requests the server to include the query structure in the answer header.
#[unsafe(no_mangle)]
pub extern "C" fn query_options_set_include_query_structure(options: *mut QueryOptions, include_query_structure: bool) {
    borrow_mut(options).include_query_structure = Some(include_query_structure);
}

/// Returns the value set for 'include query structure' in this <code>QueryOptions</code> object.
#[unsafe(no_mangle)]
pub extern "C" fn query_options_get_include_query_structure(options: *const QueryOptions) -> bool {
    borrow(options).include_query_structure.unwrap()
}

/// Checks whether the prefetch size was explicitly set for this <code>QueryOptions</code> object.
#[unsafe(no_mangle)]
pub extern "C" fn query_options_has_include_query_structure(options: *const QueryOptions) -> bool {
    borrow(options).include_query_structure.is_some()
}
