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

use typedb_driver::{
    BoxPromise, Promise, Result,
    analyze::pipeline::Pipeline,
    answer::{ConceptRow, QueryAnswer, QueryType},
    box_stream,
    concept::Concept,
};

use crate::{
    analyze::ConjunctionIDIterator,
    common::{
        StringIterator,
        error::{try_release, try_release_optional},
        iterator::CIterator,
        memory::{borrow, free, release, release_optional, release_string, string_view, take_ownership},
    },
    concept::{ConceptIterator, ConceptRowIterator},
};

/// Promise object representing the result of an asynchronous operation.
/// Use \ref query_answer_promise_resolve(QueryAnswerPromise*) to wait for and retrieve the resulting boolean value.
pub struct QueryAnswerPromise(BoxPromise<'static, Result<QueryAnswer>>);

impl QueryAnswerPromise {
    pub fn new(promise: impl Promise<'static, Result<QueryAnswer>>) -> Self {
        Self(Box::new(|| promise.resolve()))
    }
}

/// Waits for and returns the result of the operation represented by the <code>QueryAnswer</code> object.
/// In case the operation failed, the error flag will only be set when the promise is resolved.
/// The native promise object is freed when it is resolved.
#[unsafe(no_mangle)]
pub extern "C" fn query_answer_promise_resolve(promise: *mut QueryAnswerPromise) -> *mut QueryAnswer {
    try_release(take_ownership(promise).0.resolve())
}

/// Frees the native rust <code>QueryAnswerPromise</code> object.
#[unsafe(no_mangle)]
pub extern "C" fn query_answer_promise_drop(promise: *mut QueryAnswerPromise) {
    drop(take_ownership(promise))
}

/// Retrieves the executed query's type of the <code>QueryAnswer</code>.
#[unsafe(no_mangle)]
pub extern "C" fn query_answer_get_query_type(query_answer: *const QueryAnswer) -> QueryType {
    borrow(query_answer).get_query_type()
}

/// Checks if the query answer is an <code>Ok</code>.
#[unsafe(no_mangle)]
pub extern "C" fn query_answer_is_ok(query_answer: *const QueryAnswer) -> bool {
    borrow(query_answer).is_ok()
}

/// Checks if the query answer is a <code>ConceptRowStream</code>.
#[unsafe(no_mangle)]
pub extern "C" fn query_answer_is_concept_row_stream(query_answer: *const QueryAnswer) -> bool {
    borrow(query_answer).is_row_stream()
}

/// Checks if the query answer is a <code>ConceptDocumentStream</code>.
#[unsafe(no_mangle)]
pub extern "C" fn query_answer_is_concept_document_stream(query_answer: *const QueryAnswer) -> bool {
    borrow(query_answer).is_document_stream()
}

/// Produces an <code>Iterator</code> over all <code>ConceptRow</code>s in this <code>QueryAnswer</code>.
#[unsafe(no_mangle)]
pub extern "C" fn query_answer_into_rows(query_answer: *mut QueryAnswer) -> *mut ConceptRowIterator {
    release(ConceptRowIterator(CIterator(take_ownership(query_answer).into_rows())))
}

/// Produces an <code>Iterator</code> over all JSON <code>ConceptDocument</code>s in this <code>QueryAnswer</code>.
#[unsafe(no_mangle)]
pub extern "C" fn query_answer_into_documents(query_answer: *mut QueryAnswer) -> *mut StringIterator {
    release(StringIterator(CIterator(box_stream(
        take_ownership(query_answer)
            .into_documents()
            .map(|result| result.map(|document| document.into_json().to_string())),
    ))))
}

/// Frees the native rust <code>QueryAnswer</code> object.
#[unsafe(no_mangle)]
pub extern "C" fn query_answer_drop(query_answer: *mut QueryAnswer) {
    free(query_answer);
}

/// Frees the native rust <code>ConceptRow</code> object.
#[unsafe(no_mangle)]
pub extern "C" fn concept_row_drop(concept_row: *mut ConceptRow) {
    free(concept_row);
}

/// Produces an <code>Iterator</code> over all <code>String</code> column names of the <code>ConceptRow</code>'s header.
#[unsafe(no_mangle)]
pub extern "C" fn concept_row_get_column_names(concept_row: *const ConceptRow) -> *mut StringIterator {
    release(StringIterator(CIterator(box_stream(borrow(concept_row).get_column_names().iter().cloned().map(Ok)))))
}

/// Retrieves the executed query's structure from the <code>ConceptRow</code>'s header, if set.
/// It must be requested via "include query structure" in <code>QueryOptions</code>
#[unsafe(no_mangle)]
pub extern "C" fn concept_row_get_query_structure(concept_row: *const ConceptRow) -> *mut Pipeline {
    release_optional(borrow(concept_row).get_query_structure().cloned())
}

/// Retrieves the executed query's type of the <code>ConceptRow</code>'s header.
#[unsafe(no_mangle)]
pub extern "C" fn concept_row_get_query_type(concept_row: *const ConceptRow) -> QueryType {
    borrow(concept_row).get_query_type()
}

/// Produces an <code>Iterator</code> over all <code>Concepts</code> in this <code>ConceptRow</code>.
#[unsafe(no_mangle)]
pub extern "C" fn concept_row_get_concepts(concept_row: *const ConceptRow) -> *mut ConceptIterator {
    release(ConceptIterator(CIterator(box_stream(borrow(concept_row).get_concepts().cloned().map(Ok)))))
}

/// Retrieves a concept for a given column name.
#[unsafe(no_mangle)]
pub extern "C" fn concept_row_get(concept_row: *const ConceptRow, column_name: *const c_char) -> *mut Concept {
    try_release_optional(borrow(concept_row).get(string_view(column_name)).map(|concept| concept.cloned()).transpose())
}

/// Retrieves a concept for a given column index.
#[unsafe(no_mangle)]
pub extern "C" fn concept_row_get_index(concept_row: *const ConceptRow, column_index: usize) -> *mut Concept {
    try_release_optional(borrow(concept_row).get_index(column_index).map(|concept| concept.cloned()).transpose())
}

/// Retrieve the <code>ConjunctionID</code>s of <code>Conjunction</code>s that answered this row.
/// May be null.
#[unsafe(no_mangle)]
pub extern "C" fn concept_row_involved_conjunctions(concept_row: *const ConceptRow) -> *mut ConjunctionIDIterator {
    let iter_opt = borrow(concept_row).clone().get_involved_conjunctions_cloned();
    release_optional(iter_opt.map(|iter| ConjunctionIDIterator(CIterator(box_stream(iter)))))
}

/// Checks whether the provided <code>ConceptRow</code> objects are equal
#[unsafe(no_mangle)]
pub extern "C" fn concept_row_equals(lhs: *const ConceptRow, rhs: *const ConceptRow) -> bool {
    borrow(lhs) == borrow(rhs)
}

/// A string representation of this ConceptRow.
#[unsafe(no_mangle)]
pub extern "C" fn concept_row_to_string(concept_row: *const ConceptRow) -> *mut c_char {
    release_string(format!("{:?}", borrow(concept_row)))
}
