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
use std::{ffi::c_char, ptr::null_mut, sync::Arc};

use typedb_driver::{
    concept::Concept,
    error::QueryError,
    given::{GivenRow, GivenRowEntry, GivenRows, GivenRowsHeader},
};

use crate::common::{
    error::{try_release, unwrap_void},
    memory::{
        borrow, borrow_mut, decrement_arc, free, release, release_arc, release_optional, string_view, take_arc,
        take_ownership,
    },
};
// We use builders to make the FFI directives simpler.

/// Helper type for constructing a <code>GivenRowsHeader</code> instance across FFI.
pub struct GivenRowsHeaderBuilder(Vec<String>);

/// Helper type for constructing <code>GivenRows</code> instance across FFI.
pub struct GivenRowsBuilder {
    rows: GivenRows,
    active_row: Option<GivenRow>,
}

impl GivenRowsBuilder {
    fn active_row(&mut self) -> typedb_driver::Result<&mut GivenRow> {
        self.active_row.as_mut().ok_or_else(|| {
            typedb_driver::error::Error::FFI("given_rows_builder_commit_row: No active row found".to_owned())
        })
    }

    fn finish(self) -> typedb_driver::Result<GivenRows> {
        if self.active_row.is_some() {
            Err(typedb_driver::error::Error::FFI(
                "given_rows_builder_finish: There is an active, uncommitted row.".to_owned(),
            ))
        } else {
            Ok(self.rows)
        }
    }
}

fn to_given_row_entry(
    concept: &Concept,
    variable_or_index: impl std::fmt::Display,
) -> typedb_driver::Result<GivenRowEntry> {
    match borrow(concept).clone() {
        Concept::Entity(entity) => Ok(GivenRowEntry::Entity(entity)),
        Concept::Relation(relation) => Ok(GivenRowEntry::Relation(relation)),
        Concept::Attribute(attribute) => Ok(GivenRowEntry::Attribute(attribute)),
        Concept::Value(value) => Ok(GivenRowEntry::Value(value)),
        Concept::EntityType(_) | Concept::RelationType(_) | Concept::RoleType(_) | Concept::AttributeType(_) => {
            Err(typedb_driver::error::Error::Query(QueryError::GivenRowsReceivedType {
                variable_or_index: format!("{variable_or_index}"),
            }))
        }
    }
}

/// Creates a new <code>GivenRowsHeaderBuilder</code> of the specified capacity,
#[unsafe(no_mangle)]
pub extern "C" fn given_rows_header_builder_new(width: usize) -> *mut GivenRowsHeaderBuilder {
    release(GivenRowsHeaderBuilder(Vec::with_capacity(width)))
}

/// Adds a variable to the header being built by this <code>GivenRowsHeaderBuilder</code>
#[unsafe(no_mangle)]
pub extern "C" fn given_rows_header_builder_push(builder: *mut GivenRowsHeaderBuilder, variable: *mut c_char) {
    borrow_mut(builder).0.push(string_view(variable).to_owned())
}

/// Converts the builder into a finished <code>GivenRowsHeader</code> consisting of the pushed variables.
#[unsafe(no_mangle)]
pub extern "C" fn given_rows_header_builder_finish(builder: *mut GivenRowsHeaderBuilder) -> *const GivenRowsHeader {
    release_arc(Arc::new(GivenRowsHeader::new(take_ownership(builder).0)))
}

/// Creates a new <code>givenRow</code> of the specified capacity,
#[unsafe(no_mangle)]
pub extern "C" fn given_rows_builder_new(
    header: *const GivenRowsHeader,
    row_count_hint: usize,
) -> *mut GivenRowsBuilder {
    let arced_header = take_arc(header);
    let cloned_header = arced_header.clone();
    let _ = release_arc(arced_header); // Give ownership back before anything bad can happen
    let rows = GivenRows::new_with_headers(cloned_header, row_count_hint);
    let active_row = None;
    release(GivenRowsBuilder { rows, active_row })
}

#[unsafe(no_mangle)]
pub extern "C" fn given_rows_builder_start_new_row(builder: *mut GivenRowsBuilder) {
    let builder = borrow_mut(builder);
    builder.active_row = Some(GivenRow::new(builder.rows.header.clone()))
}

#[unsafe(no_mangle)]
pub extern "C" fn given_rows_builder_commit_row(builder: *mut GivenRowsBuilder) {
    let builder = borrow_mut(builder);
    unwrap_void(match builder.active_row.take() {
        Some(row) => builder.rows.push(row),
        None => Err(typedb_driver::error::Error::FFI("given_rows_builder_commit_row: No active row found".to_owned())),
    });
}

#[unsafe(no_mangle)]
pub extern "C" fn given_rows_builder_finish(builder: *mut GivenRowsBuilder) -> *mut GivenRows {
    try_release(take_ownership(builder).finish())
}

/// Sets the entry at `index` in the given row to the specified entity
#[unsafe(no_mangle)]
pub extern "C" fn given_rows_builder_set_index_to_concept(
    builder: *mut GivenRowsBuilder,
    index: usize,
    concept: *const Concept,
) {
    let result = borrow_mut(builder)
        .active_row()
        .and_then(|mut row| row.set_at(index, to_given_row_entry(borrow(concept), index)?));
    unwrap_void(result);
}

/// Sets the entry for `variable` in the given row to the specified entity
#[unsafe(no_mangle)]
pub extern "C" fn given_rows_builder_set_variable_to_concept(
    builder: *mut GivenRowsBuilder,
    variable: *const c_char,
    concept: *const Concept,
) {
    let result = borrow_mut(builder).active_row().and_then(|mut row| {
        let var_name = string_view(variable);
        row.set(var_name.to_owned(), to_given_row_entry(borrow(concept), var_name)?)
    });
    unwrap_void(result);
}

/// Sets the entry at `index` in the given row to the Empty Optional value
#[unsafe(no_mangle)]
pub extern "C" fn given_rows_builder_set_index_to_empty(builder: *mut GivenRowsBuilder, index: usize) {
    let result = borrow_mut(builder).active_row().and_then(|mut row| row.set_at(index, GivenRowEntry::Empty));
    unwrap_void(result);
}

/// Sets the entry for `variable` in the given row to the Empty optional value
#[unsafe(no_mangle)]
pub extern "C" fn given_rows_builder_set_variable_to_empty(builder: *mut GivenRowsBuilder, variable: *const c_char) {
    let result = borrow_mut(builder).active_row().and_then(|mut row| {
        let var_name = string_view(variable);
        row.set(var_name.to_owned(), GivenRowEntry::Empty)
    });
    unwrap_void(result);
}

/// Frees the native rust <code>GivenRowsHeaderBuilder</code> object
#[unsafe(no_mangle)]
pub extern "C" fn given_rows_header_builder_drop(builder: *mut GivenRowsHeaderBuilder) {
    free(builder)
}

/// Releases this instance of the arc of the native rust <code>GivenRowsHeader</code> object
#[unsafe(no_mangle)]
pub extern "C" fn given_rows_header_drop(header: *const GivenRowsHeader) {
    decrement_arc(header)
}

/// Frees the native rust <code>GivenRowsBuilder</code> object
#[unsafe(no_mangle)]
pub extern "C" fn given_rows_builder_drop(builder: *mut GivenRowsBuilder) {
    free(builder);
}

/// Frees the native rust <code>QueryGivenRows</code> object
#[unsafe(no_mangle)]
pub extern "C" fn given_rows_drop(rows: *mut GivenRows) {
    free(rows);
}
