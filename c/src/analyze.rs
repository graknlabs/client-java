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

use typedb_driver::{
    BoxPromise, Promise,
    analyze::{
        AnalyzedQuery, Fetch, Function, Given, ReturnOperation, TypeAnnotations, VariableAnnotations,
        conjunction::{
            Comparator, Conjunction, ConjunctionID, Constraint, ConstraintExactness, ConstraintVertex,
            ConstraintWithSpan, NamedRole, Variable,
        },
        pipeline::{Pipeline, PipelineStage, ReduceAssignment, Reducer, SortOrder, SortVariable},
    },
    box_stream,
    concept::{Concept, Kind, type_::Type},
};

use crate::{
    common::{
        StringIterator,
        error::try_release,
        iterator::{CIterator, iterator_next},
        memory::{
            borrow, free, release, release_optional, release_optional_string, release_string, string_view,
            take_ownership,
        },
    },
    concept::ConceptIterator,
};

// Iterators, promises & enums
pub struct AnalyzedQueryPromise(BoxPromise<'static, typedb_driver::Result<AnalyzedQuery>>);

impl AnalyzedQueryPromise {
    pub fn new(promise: impl Promise<'static, typedb_driver::Result<AnalyzedQuery>>) -> Self {
        Self(Box::new(|| promise.resolve()))
    }
}

pub struct VariableIterator(pub CIterator<Variable>);

pub struct ReducerIterator(pub CIterator<Reducer>);

pub struct PipelineStageIterator(pub CIterator<PipelineStage>);

pub struct SortVariableIterator(pub CIterator<SortVariable>);

pub struct ReduceAssignmentIterator(pub CIterator<ReduceAssignment>);

pub struct ConstraintWithSpanIterator(pub CIterator<ConstraintWithSpan>);

pub struct ConstraintVertexIterator(pub CIterator<ConstraintVertex>);

pub struct ConjunctionIDIterator(pub CIterator<ConjunctionID>);

pub struct FunctionIterator(pub CIterator<Function>);

pub struct VariableAnnotationsIterator(pub CIterator<VariableAnnotations>);

#[repr(C)]
enum ReturnOperationVariant {
    StreamReturn,
    SingleReturn,
    CheckReturn,
    ReduceReturn,
}

#[repr(C)]
enum PipelineStageVariant {
    Match,
    Insert,
    Put,
    Update,
    Delete,
    Select,
    Sort,
    Require,
    Offset,
    Limit,
    Distinct,
    Reduce,
}

#[repr(C)]
enum ConstraintVariant {
    Isa,
    Has,
    Links,

    Sub,
    Owns,
    Relates,
    Plays,

    FunctionCall,
    Expression,
    Is,
    Iid,
    DeleteConcepts,
    Comparison,
    KindOf,
    Label,
    Value,
    IsSet,

    Or,
    Not,
    Try,
}

#[repr(C)]
enum ConstraintVertexVariant {
    VariableVertex,
    LabelVertex,
    ValueVertex,
    NamedRoleVertex,
}

#[repr(C)]
enum VariableAnnotationsVariant {
    InstanceAnnotations,
    TypeAnnotations,
    ValueAnnotations,
}

#[repr(C)]
enum FetchVariant {
    LeafDocument,
    ListDocument,
    ObjectDocument,
}

/// Waits for and returns the result of the Analyze request.
/// In case the operation failed, the error flag will only be set when the promise is resolved.
/// The native promise object is freed when it is resolved.
#[unsafe(no_mangle)]
pub extern "C" fn analyzed_query_promise_resolve(promise: *mut AnalyzedQueryPromise) -> *mut AnalyzedQuery {
    try_release(take_ownership(promise).0.resolve())
}

/// Frees the native rust <code>AnalyzePromise</code> object.
#[unsafe(no_mangle)]
pub extern "C" fn analyzed_query_promise_drop(promise: *mut AnalyzedQueryPromise) {
    drop(take_ownership(promise))
}

/// Returns the structure of the query pipeline in the analyzed query.
#[unsafe(no_mangle)]
pub extern "C" fn analyzed_query_pipeline(analyzed_query: *const AnalyzedQuery) -> *mut Pipeline {
    release(borrow(analyzed_query).query.clone())
}

/// Returns the analyzed functions in the preamble of the query.
#[unsafe(no_mangle)]
pub extern "C" fn analyzed_preamble(analyzed_query: *const AnalyzedQuery) -> *mut FunctionIterator {
    release(FunctionIterator(CIterator(box_stream(borrow(analyzed_query).preamble.clone().into_iter()))))
}

/// Returns the analyzed given stage, or null if the query has no given stage.
#[unsafe(no_mangle)]
pub extern "C" fn analyzed_given(analyzed_query: *const AnalyzedQuery) -> *mut Given {
    release_optional(borrow(analyzed_query).given.clone())
}

/// Returns the analyzed fetch
#[unsafe(no_mangle)]
pub extern "C" fn analyzed_fetch(analyzed_query: *const AnalyzedQuery) -> *mut Fetch {
    release_optional(borrow(analyzed_query).fetch.clone())
}

/// Returns the variant of the <code>Fetch</code> instance.
#[unsafe(no_mangle)]
pub extern "C" fn fetch_variant(fetch: *const Fetch) -> FetchVariant {
    match borrow(fetch) {
        Fetch::List(_) => FetchVariant::ListDocument,
        Fetch::Leaf(_) => FetchVariant::LeafDocument,
        Fetch::Object(_) => FetchVariant::ObjectDocument,
    }
}

/// Unwraps the <code>Fetch</code> instance as a Leaf variant, and returns an iterator over possible value types.
/// Will panic if the instance is not a Leaf variant.
#[unsafe(no_mangle)]
pub extern "C" fn fetch_leaf_annotations(fetch: *const Fetch) -> *mut StringIterator {
    match borrow(fetch) {
        Fetch::Leaf(leaf) => {
            let iter = leaf.annotations.clone().into_iter().map(|a| Ok(a.name().to_owned()));
            release(StringIterator(CIterator(box_stream(iter))))
        }
        _ => unreachable!("Expected Fetch to be Leaf variant"),
    }
}

/// Unwraps the <code>Fetch</code> instance as a List variant, and returns the <code>Fetch</code> for an element of the list.
/// Will panic if the instance is not a List variant.
#[unsafe(no_mangle)]
pub extern "C" fn fetch_list_element(fetch: *const Fetch) -> *mut Fetch {
    match borrow(fetch) {
        Fetch::List(element) => release((**element).clone()),
        _ => unreachable!("Expected Fetch to be List variant"),
    }
}

/// Unwraps the <code>Fetch</code> instance as an Object variant, and returns the available fields.
/// Will panic if the instance is not an Object variant.
#[unsafe(no_mangle)]
pub extern "C" fn fetch_object_fields(fetch: *const Fetch) -> *mut StringIterator {
    match borrow(fetch) {
        Fetch::Object(map) => {
            let iter = map.keys().cloned().collect::<Vec<_>>().clone().into_iter().map(Ok);
            release(StringIterator(CIterator(box_stream(iter))))
        }
        _ => unreachable!("Expected Fetch to be Object variant"),
    }
}

/// Unwraps the <code>Fetch</code> instance as an Object variant, and returns the value of the specified field.
/// Will panic if the instance is not an Object variant.
#[unsafe(no_mangle)]
pub extern "C" fn fetch_object_get_field(fetch: *const Fetch, field: *const c_char) -> *mut Fetch {
    match borrow(fetch) {
        Fetch::Object(map) => release_optional(map.get(string_view(field)).cloned()),
        _ => unreachable!("Expected Fetch to be Object variant"),
    }
}

// Given
/// Returns the variables declared in the given stage.
#[unsafe(no_mangle)]
pub extern "C" fn given_variables(given: *const Given) -> *mut VariableIterator {
    release(VariableIterator(CIterator(box_stream(borrow(given).variables.clone().into_iter()))))
}

/// Returns the inferred types for the specified variable in the given stage.
#[unsafe(no_mangle)]
pub extern "C" fn given_variable_annotations(
    given: *const Given,
    variable: *const Variable,
) -> *mut VariableAnnotations {
    release_optional(borrow(given).variable_annotations.get(borrow(variable)).cloned())
}

// Functions
/// A representation of the <code>Pipeline</code> which forms the body of the function.
#[unsafe(no_mangle)]
pub extern "C" fn function_body(function: *const Function) -> *mut Pipeline {
    release(borrow(function).body.clone())
}

/// Returns the <code>Variable</code>s which are the arguments of the function.
#[unsafe(no_mangle)]
pub extern "C" fn function_argument_variables(function: *const Function) -> *mut VariableIterator {
    release(VariableIterator(CIterator(box_stream(borrow(function).argument_variables.clone().into_iter()))))
}

/// A representation of the <code>ReturnOperation</code> of the function.
#[unsafe(no_mangle)]
pub extern "C" fn function_return_operation(function: *const Function) -> *mut ReturnOperation {
    release(borrow(function).return_operation.clone())
}

/// Returns the variant of the <code>ReturnOperation</code>.
#[unsafe(no_mangle)]
pub extern "C" fn return_operation_variant(return_operation: *const ReturnOperation) -> ReturnOperationVariant {
    match borrow(return_operation) {
        ReturnOperation::Stream { .. } => ReturnOperationVariant::StreamReturn,
        ReturnOperation::Single { .. } => ReturnOperationVariant::SingleReturn,
        ReturnOperation::Check { .. } => ReturnOperationVariant::CheckReturn,
        ReturnOperation::Reduce { .. } => ReturnOperationVariant::ReduceReturn,
    }
}

/// Unwraps the <code>ReturnOperation</code> instance as a Stream variant, and returns the returned variables.
/// Will panic if the instance is not a Stream variant.
#[unsafe(no_mangle)]
pub extern "C" fn return_operation_stream_variables(return_operation: *const ReturnOperation) -> *mut VariableIterator {
    match borrow(return_operation) {
        ReturnOperation::Stream { variables, .. } => {
            release(VariableIterator(CIterator(box_stream(variables.clone().into_iter()))))
        }
        _ => unreachable!("Expected ReturnOperation to be Stream"),
    }
}

/// Unwraps the <code>ReturnOperation</code> instance as a Single variant, and returns the returned variables.
/// Will panic if the instance is not a Single variant.
#[unsafe(no_mangle)]
pub extern "C" fn return_operation_single_variables(return_operation: *const ReturnOperation) -> *mut VariableIterator {
    match borrow(return_operation) {
        ReturnOperation::Single { variables, .. } => {
            release(VariableIterator(CIterator(box_stream(variables.clone().into_iter()))))
        }
        _ => unreachable!("Expected ReturnOperation to be Single"),
    }
}

/// Unwraps the <code>ReturnOperation</code> instance as a Single variant, and returns the selector applied.
/// Will panic if the instance is not a Single variant.
#[unsafe(no_mangle)]
pub extern "C" fn return_operation_single_selector(return_operation: *const ReturnOperation) -> *mut c_char {
    match borrow(return_operation) {
        ReturnOperation::Single { selector, .. } => release_string(selector.clone()),
        _ => unreachable!("Expected ReturnOperation to be Single"),
    }
}

/// Unwraps the <code>ReturnOperation</code> instance as a Reduce variant, and returns the reducers applied.
/// Will panic if the instance is not a Reduce variant.
#[unsafe(no_mangle)]
pub extern "C" fn return_operation_reducers(return_operation: *const ReturnOperation) -> *mut ReducerIterator {
    match borrow(return_operation) {
        ReturnOperation::Reduce { reducers } => {
            release(ReducerIterator(CIterator(box_stream(reducers.clone().into_iter()))))
        }
        _ => unreachable!("Expected ReturnOperation to be Reducer"),
    }
}

/// Returns the inferred type for each argument of the function.
#[unsafe(no_mangle)]
pub extern "C" fn function_argument_annotations(function: *const Function) -> *mut VariableAnnotationsIterator {
    let iter = borrow(function).argument_annotations.clone().into_iter();
    release(VariableAnnotationsIterator(CIterator(box_stream(iter))))
}

/// Returns the inferred type for each concept returned by the function.
#[unsafe(no_mangle)]
pub extern "C" fn function_return_annotations(function: *const Function) -> *mut VariableAnnotationsIterator {
    let iter = borrow(function).return_annotations.clone().into_iter();
    release(VariableAnnotationsIterator(CIterator(box_stream(iter))))
}

// Stages
/// Returns an iterator over the stages making up the <code>Pipeline</code>
#[unsafe(no_mangle)]
pub extern "C" fn pipeline_stages(pipeline: *const Pipeline) -> *mut PipelineStageIterator {
    release(PipelineStageIterator(CIterator(box_stream(borrow(pipeline).stages.clone().into_iter()))))
}

// Stage field accessors
/// Returns the variant of the <code>Pipeline</code> stage.
#[unsafe(no_mangle)]
pub extern "C" fn pipeline_stage_variant(stage: *const PipelineStage) -> PipelineStageVariant {
    match borrow(stage) {
        PipelineStage::Match { .. } => PipelineStageVariant::Match,
        PipelineStage::Insert { .. } => PipelineStageVariant::Insert,
        PipelineStage::Put { .. } => PipelineStageVariant::Put,
        PipelineStage::Update { .. } => PipelineStageVariant::Update,
        PipelineStage::Delete { .. } => PipelineStageVariant::Delete,
        PipelineStage::Select { .. } => PipelineStageVariant::Select,
        PipelineStage::Sort { .. } => PipelineStageVariant::Sort,
        PipelineStage::Require { .. } => PipelineStageVariant::Require,
        PipelineStage::Offset { .. } => PipelineStageVariant::Offset,
        PipelineStage::Limit { .. } => PipelineStageVariant::Limit,
        PipelineStage::Distinct => PipelineStageVariant::Distinct,
        PipelineStage::Reduce { .. } => PipelineStageVariant::Reduce,
    }
}

/// Returns a string representation of the pipeline stage
#[unsafe(no_mangle)]
pub extern "C" fn pipeline_stage_string_repr(stage: *const PipelineStage) -> *mut c_char {
    release_string(format!("{:?}", &borrow(stage)))
}

/// Returns the block of the pipeline stage - this is the <code>ConjunctionID</code> of the root conjunction.
/// Will panic if the stage does not have a block.
#[unsafe(no_mangle)]
pub extern "C" fn pipeline_stage_get_block(stage: *const PipelineStage) -> *mut ConjunctionID {
    match borrow(stage) {
        PipelineStage::Match { block, .. }
        | PipelineStage::Insert { block, .. }
        | PipelineStage::Put { block, .. }
        | PipelineStage::Update { block, .. }
        | PipelineStage::Delete { block, .. } => release(*block),
        PipelineStage::Select { .. }
        | PipelineStage::Sort { .. }
        | PipelineStage::Require { .. }
        | PipelineStage::Offset { .. }
        | PipelineStage::Limit { .. }
        | PipelineStage::Distinct
        | PipelineStage::Reduce { .. } => unreachable!("PipelineStage {stage:?} has no block"),
    }
}

/// Unwraps the <code>PipelineStage</code> instance as a Delete stage, and returns the variables deleted.
/// Will panic if the stage is not a Delete.
#[unsafe(no_mangle)]
pub extern "C" fn pipeline_stage_delete_get_deleted_variables(stage: *const PipelineStage) -> *mut VariableIterator {
    let PipelineStage::Delete { deleted_variables, .. } = borrow(stage) else { unreachable!("Expected Delete stage") };
    release(VariableIterator(CIterator(box_stream(deleted_variables.clone().into_iter()))))
}

/// Unwraps the <code>PipelineStage</code> instance as a Select stage, and returns the variables selected.
/// Will panic if the stage is not a Select.
#[unsafe(no_mangle)]
pub extern "C" fn pipeline_stage_select_get_variables(stage: *const PipelineStage) -> *mut VariableIterator {
    let PipelineStage::Select { variables, .. } = borrow(stage) else { unreachable!("Expected Select stage") };
    release(VariableIterator(CIterator(box_stream(variables.clone().into_iter()))))
}

/// Unwraps the <code>PipelineStage</code> instance as a Require stage, and returns the required variables.
/// Will panic if the stage is not a Require stage.
#[unsafe(no_mangle)]
pub extern "C" fn pipeline_stage_require_get_variables(stage: *const PipelineStage) -> *mut VariableIterator {
    let PipelineStage::Require { variables, .. } = borrow(stage) else { unreachable!("Expected Require stage") };
    release(VariableIterator(CIterator(box_stream(variables.clone().into_iter()))))
}

/// Unwraps the <code>PipelineStage</code> instance as an Offset stage, and returns the offset applied.
/// Will panic if the stage is not an Offset stage.
#[unsafe(no_mangle)]
pub extern "C" fn pipeline_stage_offset_get_offset(stage: *const PipelineStage) -> i64 {
    let PipelineStage::Offset { offset, .. } = borrow(stage) else { unreachable!("Expected Offset stage") };
    *offset as i64
}

/// Unwraps the <code>PipelineStage</code> instance as a Limit stage, and returns the limit applied.
/// Will panic if the stage is not a Limit stage.
#[unsafe(no_mangle)]
pub extern "C" fn pipeline_stage_limit_get_limit(stage: *const PipelineStage) -> i64 {
    let PipelineStage::Limit { limit, .. } = borrow(stage) else { unreachable!("Expected Limit stage") };
    *limit as i64
}

/// Unwraps the <code>PipelineStage</code> instance as a Sort stage, and returns the <code>SortVariable</code>.
/// Will panic if the stage is not a Sort stage.
#[unsafe(no_mangle)]
pub extern "C" fn pipeline_stage_sort_get_sort_variables(stage: *const PipelineStage) -> *mut SortVariableIterator {
    let PipelineStage::Sort { variables, .. } = borrow(stage) else { unreachable!("Expected Sort stage") };
    release(SortVariableIterator(CIterator(box_stream(variables.clone().into_iter()))))
}

/// Unwraps the <code>PipelineStage</code> instance as a Reduce stage, and returns the variables being grouped on.
/// Will panic if the stage is not a Reduce stage.
#[unsafe(no_mangle)]
pub extern "C" fn pipeline_stage_reduce_get_groupby(stage: *const PipelineStage) -> *mut VariableIterator {
    let PipelineStage::Reduce { groupby, .. } = borrow(stage) else { unreachable!("Expected Reduce stage") };
    release(VariableIterator(CIterator(box_stream(groupby.clone().into_iter()))))
}

/// Unwraps the <code>PipelineStage</code> instance as a Reduce stage,
///  and returns the reducers applied and the variables their results are assigned to.
/// Will panic if the stage is not a Reduce stage.
#[unsafe(no_mangle)]
pub extern "C" fn pipeline_stage_reduce_get_reducer_assignments(
    stage: *const PipelineStage,
) -> *mut ReduceAssignmentIterator {
    let PipelineStage::Reduce { reducers, .. } = borrow(stage) else { unreachable!("Expected Reduce stage") };
    release(ReduceAssignmentIterator(CIterator(box_stream(reducers.clone().into_iter()))))
}

/// Returns the variable being sorted on.
#[unsafe(no_mangle)]
pub extern "C" fn sort_variable_get_variable(sort_variable: *const SortVariable) -> *mut Variable {
    release(borrow(sort_variable).variable.clone())
}

/// Returns the sort order for the variable being sorted on.
#[unsafe(no_mangle)]
pub extern "C" fn sort_variable_get_order(sort_variable: *const SortVariable) -> SortOrder {
    borrow(sort_variable).order.clone()
}

/// The variable being assigned to in this ReduceAssignment.
/// e.g. `$c` in <code>$c = sum($x)</code>
#[unsafe(no_mangle)]
pub extern "C" fn reduce_assignment_get_assigned(reduce_assignment: *const ReduceAssignment) -> *mut Variable {
    release(borrow(reduce_assignment).assigned.clone())
}

/// The reducer applied in this ReduceAssignment.
/// e.g. `sum($x)` in <code>$c = sum($x)</code>
#[unsafe(no_mangle)]
pub extern "C" fn reduce_assignment_get_reducer(reduce_assignment: *const ReduceAssignment) -> *mut Reducer {
    release(borrow(reduce_assignment).reducer.clone())
}

/// The name of the operation applied by this Reducer.
/// e.g. `sum` in <code>sum($x)</code>
#[unsafe(no_mangle)]
pub extern "C" fn reducer_get_name(reducer: *const Reducer) -> *mut c_char {
    release_string(borrow(reducer).reducer.clone())
}

/// The arguments to this Reducer.
/// e.g. `$x` in <code>sum($x)</code>
#[unsafe(no_mangle)]
pub extern "C" fn reducer_get_arguments(reducer: *const Reducer) -> *mut VariableIterator {
    release(VariableIterator(CIterator(box_stream(borrow(reducer).arguments.clone().into_iter()))))
}

/// Returns the name of the specified variable if it has one, and null otherwise.
/// The <code>Pipeline</code> must be the one that contains the variable.
#[unsafe(no_mangle)]
pub extern "C" fn variable_get_name(pipeline_structure: *const Pipeline, variable: *const Variable) -> *mut c_char {
    release_optional_string(borrow(pipeline_structure).variable_name(borrow(variable)).map(str::to_owned))
}

/// Returns the Variable id as u32 to use in hash, equality etc.
#[unsafe(no_mangle)]
pub extern "C" fn variable_id_as_u32(variable: *const Variable) -> u32 {
    borrow(variable).0
}

/// Returns a string representation of the variable. Does not resolve the name.
#[unsafe(no_mangle)]
pub extern "C" fn variable_string_repr(variable: *const Variable) -> *mut c_char {
    release_string(format!("{:?}", borrow(variable)))
}

/// Returns the <code>Conjunction</code> corresponding to the ConjunctionID.
/// The <code>Pipeline</code> must be the one that contains the Conjunction & ConjunctionID.
#[unsafe(no_mangle)]
pub extern "C" fn pipeline_get_conjunction(
    pipeline: *const Pipeline,
    conjunction_id: *const ConjunctionID,
) -> *mut Conjunction {
    release_optional(borrow(pipeline).conjunctions.get(borrow(conjunction_id).0).cloned())
}

/// Returns the ConjunctionID as u32 to use in hash, equality etc.
#[unsafe(no_mangle)]
pub extern "C" fn conjunction_id_as_u32(conjunction_id: *const ConjunctionID) -> u32 {
    borrow(conjunction_id).0 as u32
}

/// Returns a string representation of the ConjunctionID.
#[unsafe(no_mangle)]
pub extern "C" fn conjunction_id_string_repr(conjunction_id: *const ConjunctionID) -> *mut c_char {
    release_string(format!("{:?}", borrow(conjunction_id).0))
}

/// Returns the <code>Constraint</code>s in the given conjunction.
#[unsafe(no_mangle)]
pub extern "C" fn conjunction_get_constraints(conjunction: *const Conjunction) -> *mut ConstraintWithSpanIterator {
    release(ConstraintWithSpanIterator(CIterator(box_stream(borrow(conjunction).constraints.clone().into_iter()))))
}

/// Returns the variables in the current conjunction for which type annotations are available
#[unsafe(no_mangle)]
pub extern "C" fn conjunction_get_annotated_variables(conjunction: *const Conjunction) -> *mut VariableIterator {
    let iter = borrow(conjunction).variable_annotations.keys().cloned().collect::<Vec<_>>().into_iter();
    release(VariableIterator(CIterator(box_stream(iter))))
}

/// Returns the inferred types for the specified variable in the specified conjunction.
#[unsafe(no_mangle)]
pub extern "C" fn conjunction_get_variable_annotations(
    conjunction: *const Conjunction,
    variable: *const Variable,
) -> *mut VariableAnnotations {
    release_optional(borrow(conjunction).variable_annotations.get(borrow(variable)).cloned())
}

/// Returns the variant of the specified VariableAnnotations.
/// This tells us whether the variable holds an instance, a type or a raw value.
#[unsafe(no_mangle)]
pub extern "C" fn variable_annotations_variant(annotations: *const VariableAnnotations) -> VariableAnnotationsVariant {
    match &borrow(annotations).types {
        TypeAnnotations::Instance(_) => VariableAnnotationsVariant::InstanceAnnotations,
        TypeAnnotations::Type(_) => VariableAnnotationsVariant::TypeAnnotations,
        TypeAnnotations::Value(_) => VariableAnnotationsVariant::ValueAnnotations,
    }
}

/// Unwraps the <code>VariableAnnotations</code> instance as annotations for an Instance variable,
///  and returns the possible types of the instances the variable may hold.
/// Will panic if the variable is not an Instance variable.
#[unsafe(no_mangle)]
pub extern "C" fn variable_annotations_instance(annotations: *const VariableAnnotations) -> *mut ConceptIterator {
    match &borrow(annotations).types {
        TypeAnnotations::Instance(annotations) => release(ConceptIterator(CIterator(box_stream(
            annotations.clone().into_iter().map(|t| Ok(type_to_concept(t))),
        )))),
        _ => unreachable!("Expected variable to have instance annotations"),
    }
}

/// Unwraps the <code>VariableAnnotations</code> instance as annotations for a Type variable,
///  and returns the possible types the variable may be.
/// Will panic if the variable is not a Type variable.
#[unsafe(no_mangle)]
pub extern "C" fn variable_annotations_type(annotations: *const VariableAnnotations) -> *mut ConceptIterator {
    match &borrow(annotations).types {
        TypeAnnotations::Type(annotations) => release(ConceptIterator(CIterator(box_stream(
            annotations.clone().into_iter().map(|t| Ok(type_to_concept(t))),
        )))),
        _ => unreachable!("Expected variable to have type annotations"),
    }
}

/// Unwraps the <code>VariableAnnotations</code> instance as annotations for a Value variable,
///  and returns the possible ValueTypes the value may be.
/// Will panic if the variable is not a Value variable.
#[unsafe(no_mangle)]
pub extern "C" fn variable_annotations_value(annotations: *const VariableAnnotations) -> *mut StringIterator {
    match &borrow(annotations).types {
        TypeAnnotations::Value(annotations) => {
            release(StringIterator(CIterator(box_stream([Ok(annotations.name().to_owned())].into_iter()))))
        }
        _ => unreachable!("Expected variable to have value annotations"),
    }
}

/// The offset of the first character of the specified constraint in the source query .
#[unsafe(no_mangle)]
pub extern "C" fn constraint_span_begin(constraint: *const ConstraintWithSpan) -> i64 {
    borrow(constraint).span.as_ref().map_or(0, |c| c.begin) as i64
}

/// The offset after the last character of the specified constraint in the source query .
#[unsafe(no_mangle)]
pub extern "C" fn constraint_span_end(constraint: *const ConstraintWithSpan) -> i64 {
    borrow(constraint).span.as_ref().map_or(0, |c| c.end) as i64
}

/// The variant of the specified constraint
#[unsafe(no_mangle)]
pub extern "C" fn constraint_variant(constraint: *const ConstraintWithSpan) -> ConstraintVariant {
    match &borrow(constraint).constraint {
        Constraint::Isa { .. } => ConstraintVariant::Isa,
        Constraint::Has { .. } => ConstraintVariant::Has,
        Constraint::Links { .. } => ConstraintVariant::Links,
        Constraint::Sub { .. } => ConstraintVariant::Sub,
        Constraint::Owns { .. } => ConstraintVariant::Owns,
        Constraint::Relates { .. } => ConstraintVariant::Relates,
        Constraint::Plays { .. } => ConstraintVariant::Plays,
        Constraint::FunctionCall { .. } => ConstraintVariant::FunctionCall,
        Constraint::Expression { .. } => ConstraintVariant::Expression,
        Constraint::Is { .. } => ConstraintVariant::Is,
        Constraint::Iid { .. } => ConstraintVariant::Iid,
        Constraint::DeleteConcepts { .. } => ConstraintVariant::DeleteConcepts,
        Constraint::Comparison { .. } => ConstraintVariant::Comparison,
        Constraint::Kind { .. } => ConstraintVariant::KindOf,
        Constraint::Label { .. } => ConstraintVariant::Label,
        Constraint::Value { .. } => ConstraintVariant::Value,
        Constraint::IsSet { .. } => ConstraintVariant::IsSet,
        Constraint::Or { .. } => ConstraintVariant::Or,
        Constraint::Not { .. } => ConstraintVariant::Not,
        Constraint::Try { .. } => ConstraintVariant::Try,
    }
}

/// Unwraps the <code>Constraint</code> instance as an `Isa` constraint,
///  and returns the instance vertex.
/// Will panic if the Constraint is not an Isa constraint.
#[unsafe(no_mangle)]
pub extern "C" fn constraint_isa_get_instance(constraint: *const ConstraintWithSpan) -> *mut ConstraintVertex {
    let Constraint::Isa { instance, .. } = &borrow(constraint).constraint else {
        unreachable!("Expected constraint to be Isa");
    };
    release(instance.clone())
}

/// Unwraps the <code>Constraint</code> instance as an `Isa` constraint,
///  and returns the type vertex.
/// Will panic if the Constraint is not an Isa constraint.
#[unsafe(no_mangle)]
pub extern "C" fn constraint_isa_get_type(constraint: *const ConstraintWithSpan) -> *mut ConstraintVertex {
    let Constraint::Isa { r#type: type_, .. } = &borrow(constraint).constraint else {
        unreachable!("Expected constraint to be Isa");
    };
    release(type_.clone())
}

/// Unwraps the <code>Constraint</code> instance as a `Has` constraint,
/// and returns the owner vertex.
/// Will panic if the Constraint is not a Has constraint.
#[unsafe(no_mangle)]
pub extern "C" fn constraint_has_get_owner(constraint: *const ConstraintWithSpan) -> *mut ConstraintVertex {
    let Constraint::Has { owner, .. } = &borrow(constraint).constraint else {
        unreachable!("Expected constraint to be Has");
    };
    release(owner.clone())
}

/// Unwraps the <code>Constraint</code> instance as a `Has` constraint,
/// and returns the attribute vertex.
/// Will panic if the Constraint is not a Has constraint.
#[unsafe(no_mangle)]
pub extern "C" fn constraint_has_get_attribute(constraint: *const ConstraintWithSpan) -> *mut ConstraintVertex {
    let Constraint::Has { attribute, .. } = &borrow(constraint).constraint else {
        unreachable!("Expected constraint to be Has");
    };
    release(attribute.clone())
}

/// Unwraps the <code>Constraint</code> instance as a `Links` constraint,
/// and returns the relation vertex.
/// Will panic if the Constraint is not a Links constraint.
#[unsafe(no_mangle)]
pub extern "C" fn constraint_links_get_relation(constraint: *const ConstraintWithSpan) -> *mut ConstraintVertex {
    let Constraint::Links { relation, .. } = &borrow(constraint).constraint else {
        unreachable!("Expected constraint to be Links");
    };
    release(relation.clone())
}

/// Unwraps the <code>Constraint</code> instance as a `Links` constraint,
/// and returns the player vertex.
/// Will panic if the Constraint is not a Links constraint.
#[unsafe(no_mangle)]
pub extern "C" fn constraint_links_get_player(constraint: *const ConstraintWithSpan) -> *mut ConstraintVertex {
    let Constraint::Links { player, .. } = &borrow(constraint).constraint else {
        unreachable!("Expected constraint to be Links");
    };
    release(player.clone())
}

/// Unwraps the <code>Constraint</code> instance as a `Links` constraint,
/// and returns the role vertex.
/// Will panic if the Constraint is not a Links constraint.
#[unsafe(no_mangle)]
pub extern "C" fn constraint_links_get_role(constraint: *const ConstraintWithSpan) -> *mut ConstraintVertex {
    let Constraint::Links { role, .. } = &borrow(constraint).constraint else {
        unreachable!("Expected constraint to be Links");
    };
    release(role.clone())
}

/// Unwraps the <code>Constraint</code> instance as a `Sub` constraint,
/// and returns the subtype vertex.
/// Will panic if the Constraint is not a Sub constraint.
#[unsafe(no_mangle)]
pub extern "C" fn constraint_sub_get_subtype(constraint: *const ConstraintWithSpan) -> *mut ConstraintVertex {
    let Constraint::Sub { subtype, .. } = &borrow(constraint).constraint else {
        unreachable!("Expected constraint to be Sub");
    };
    release(subtype.clone())
}

/// Unwraps the <code>Constraint</code> instance as a `Sub` constraint,
/// and returns the supertype vertex.
/// Will panic if the Constraint is not a Sub constraint.
#[unsafe(no_mangle)]
pub extern "C" fn constraint_sub_get_supertype(constraint: *const ConstraintWithSpan) -> *mut ConstraintVertex {
    let Constraint::Sub { supertype, .. } = &borrow(constraint).constraint else {
        unreachable!("Expected constraint to be Sub");
    };
    release(supertype.clone())
}

/// Unwraps the <code>Constraint</code> instance as an `Owns` constraint,
/// and returns the owner-type vertex.
/// Will panic if the Constraint is not an Owns constraint.
#[unsafe(no_mangle)]
pub extern "C" fn constraint_owns_get_owner(constraint: *const ConstraintWithSpan) -> *mut ConstraintVertex {
    let Constraint::Owns { owner, .. } = &borrow(constraint).constraint else {
        unreachable!("Expected constraint to be Owns");
    };
    release(owner.clone())
}

/// Unwraps the <code>Constraint</code> instance as an `Owns` constraint,
/// and returns the attribute-type vertex.
/// Will panic if the Constraint is not an Owns constraint.
#[unsafe(no_mangle)]
pub extern "C" fn constraint_owns_get_attribute(constraint: *const ConstraintWithSpan) -> *mut ConstraintVertex {
    let Constraint::Owns { attribute, .. } = &borrow(constraint).constraint else {
        unreachable!("Expected constraint to be Owns");
    };
    release(attribute.clone())
}

/// Unwraps the <code>Constraint</code> instance as a `Relates` constraint,
/// and returns the relation-type vertex.
/// Will panic if the Constraint is not a Relates constraint.
#[unsafe(no_mangle)]
pub extern "C" fn constraint_relates_get_relation(constraint: *const ConstraintWithSpan) -> *mut ConstraintVertex {
    let Constraint::Relates { relation, .. } = &borrow(constraint).constraint else {
        unreachable!("Expected constraint to be Relates");
    };
    release(relation.clone())
}

/// Unwraps the <code>Constraint</code> instance as a `Relates` constraint,
/// and returns the role-type vertex.
/// Will panic if the Constraint is not a Relates constraint.
#[unsafe(no_mangle)]
pub extern "C" fn constraint_relates_get_role(constraint: *const ConstraintWithSpan) -> *mut ConstraintVertex {
    let Constraint::Relates { role, .. } = &borrow(constraint).constraint else {
        unreachable!("Expected constraint to be Relates");
    };
    release(role.clone())
}

/// Unwraps the <code>Constraint</code> instance as a `Plays` constraint,
/// and returns the player type vertex.
/// Will panic if the Constraint is not a Plays constraint.
#[unsafe(no_mangle)]
pub extern "C" fn constraint_plays_get_player(constraint: *const ConstraintWithSpan) -> *mut ConstraintVertex {
    let Constraint::Plays { player, .. } = &borrow(constraint).constraint else {
        unreachable!("Expected constraint to be Plays");
    };
    release(player.clone())
}

/// Unwraps the <code>Constraint</code> instance as a `Plays` constraint,
/// and returns the role-type vertex.
/// Will panic if the Constraint is not a Plays constraint.
#[unsafe(no_mangle)]
pub extern "C" fn constraint_plays_get_role(constraint: *const ConstraintWithSpan) -> *mut ConstraintVertex {
    let Constraint::Plays { role, .. } = &borrow(constraint).constraint else {
        unreachable!("Expected constraint to be Plays");
    };
    release(role.clone())
}

/// Unwraps the <code>Constraint</code> instance as an `Isa` constraint,
/// and returns the exactness of the type match.
/// Will panic if the Constraint is not an Isa constraint.
#[unsafe(no_mangle)]
pub extern "C" fn constraint_isa_get_exactness(constraint: *const ConstraintWithSpan) -> ConstraintExactness {
    let Constraint::Isa { exactness, .. } = &borrow(constraint).constraint else {
        unreachable!("Expected constraint to be Isa");
    };
    exactness.clone()
}

/// (FUTURE-USE: Currently always ConstraintExactness::Subtypes)
/// Unwraps the <code>Constraint</code> instance as a `Has` constraint,
/// and returns the exactness of the attribute match.
/// Will panic if the Constraint is not a Has constraint.
#[unsafe(no_mangle)]
pub extern "C" fn constraint_has_get_exactness(constraint: *const ConstraintWithSpan) -> ConstraintExactness {
    let Constraint::Has { exactness, .. } = &borrow(constraint).constraint else {
        unreachable!("Expected constraint to be Has");
    };
    exactness.clone()
}

/// (FUTURE-USE: Currently always ConstraintExactness::Subtypes)
/// Unwraps the <code>Constraint</code> instance as a `Links` constraint,
/// and returns the exactness of the role-type match.
/// Will panic if the Constraint is not a Links constraint.
#[unsafe(no_mangle)]
pub extern "C" fn constraint_links_get_exactness(constraint: *const ConstraintWithSpan) -> ConstraintExactness {
    let Constraint::Links { exactness, .. } = &borrow(constraint).constraint else {
        unreachable!("Expected constraint to be Links");
    };
    exactness.clone()
}

/// Unwraps the <code>Constraint</code> instance as a `Sub` constraint,
/// and returns the exactness of the subtype match.
/// If Exact (i.e. `sub!`), only the immediate subtype is returned
/// else, (i.e. `sub`) the type itself and all subtypes are returned.
/// Will panic if the Constraint is not a Sub constraint.
#[unsafe(no_mangle)]
pub extern "C" fn constraint_sub_get_exactness(constraint: *const ConstraintWithSpan) -> ConstraintExactness {
    let Constraint::Sub { exactness, .. } = &borrow(constraint).constraint else {
        unreachable!("Expected constraint to be Sub");
    };
    exactness.clone()
}

/// (FUTURE-USE: Currently always ConstraintExactness::Subtypes)
/// Will panic if the Constraint is not an Owns constraint.
#[unsafe(no_mangle)]
pub extern "C" fn constraint_owns_get_exactness(constraint: *const ConstraintWithSpan) -> ConstraintExactness {
    let Constraint::Owns { exactness, .. } = &borrow(constraint).constraint else {
        unreachable!("Expected constraint to be Owns");
    };
    exactness.clone()
}

/// Unwraps the <code>Constraint</code> instance as a `Relates` constraint,
/// and returns the exactness of the relation match.
/// Will panic if the Constraint is not a Relates constraint.
#[unsafe(no_mangle)]
pub extern "C" fn constraint_relates_get_exactness(constraint: *const ConstraintWithSpan) -> ConstraintExactness {
    let Constraint::Relates { exactness, .. } = &borrow(constraint).constraint else {
        unreachable!("Expected constraint to be Relates");
    };
    exactness.clone()
}

/// (FUTURE-USE: Currently always ConstraintExactness::Subtypes)
#[unsafe(no_mangle)]
pub extern "C" fn constraint_plays_get_exactness(constraint: *const ConstraintWithSpan) -> ConstraintExactness {
    let Constraint::Plays { exactness, .. } = &borrow(constraint).constraint else {
        unreachable!("Expected constraint to be Plays");
    };
    exactness.clone()
}

/// Unwraps the <code>Constraint</code> instance as a `FunctionCall` constraint,
/// and returns the function name.
/// Will panic if the Constraint is not a FunctionCall constraint.
#[unsafe(no_mangle)]
pub extern "C" fn constraint_function_call_get_name(constraint: *const ConstraintWithSpan) -> *mut c_char {
    let Constraint::FunctionCall { name, .. } = &borrow(constraint).constraint else {
        unreachable!("Expected constraint to be FunctionCall");
    };
    release_string(name.clone())
}

/// Unwraps the <code>Constraint</code> instance as a `FunctionCall` constraint,
/// and returns the variables assigned by the function call.
/// Will panic if the Constraint is not a FunctionCall constraint.
#[unsafe(no_mangle)]
pub extern "C" fn constraint_function_call_get_assigned(
    constraint: *const ConstraintWithSpan,
) -> *mut ConstraintVertexIterator {
    let Constraint::FunctionCall { assigned, .. } = &borrow(constraint).constraint else {
        unreachable!("Expected constraint to be FunctionCall");
    };
    release(ConstraintVertexIterator(CIterator(box_stream(assigned.clone().into_iter()))))
}

/// Unwraps the <code>Constraint</code> instance as a `FunctionCall` constraint,
/// and returns the variables passed as arguments to the function call .
/// Will panic if the Constraint is not a FunctionCall constraint.
#[unsafe(no_mangle)]
pub extern "C" fn constraint_function_call_get_arguments(
    constraint: *const ConstraintWithSpan,
) -> *mut ConstraintVertexIterator {
    let Constraint::FunctionCall { arguments, .. } = &borrow(constraint).constraint else {
        unreachable!("Expected constraint to be FunctionCall");
    };
    release(ConstraintVertexIterator(CIterator(box_stream(arguments.clone().into_iter()))))
}

/// Unwraps the <code>Constraint</code> instance as an `Expression` constraint,
/// and returns the expression text.
/// Will panic if the Constraint is not an Expression constraint.
#[unsafe(no_mangle)]
pub extern "C" fn constraint_expression_get_text(constraint: *const ConstraintWithSpan) -> *mut c_char {
    let Constraint::Expression { text, .. } = &borrow(constraint).constraint else {
        unreachable!("Expected constraint to be Expression");
    };
    release_string(text.clone())
}

/// Unwraps the <code>Constraint</code> instance as an `Expression` constraint,
/// and returns the variable assigned by the expression.
/// Will panic if the Constraint is not an Expression constraint.
#[unsafe(no_mangle)]
pub extern "C" fn constraint_expression_get_assigned(constraint: *const ConstraintWithSpan) -> *mut ConstraintVertex {
    let Constraint::Expression { assigned, .. } = &borrow(constraint).constraint else {
        unreachable!("Expected constraint to be Expression");
    };
    release(assigned.clone())
}

/// Unwraps the <code>Constraint</code> instance as an `Expression` constraint,
/// and returns the expression arguments -
/// These are any variables used in the right-hand side of the expression.
/// Will panic if the Constraint is not an Expression constraint.
#[unsafe(no_mangle)]
pub extern "C" fn constraint_expression_get_arguments(
    constraint: *const ConstraintWithSpan,
) -> *mut ConstraintVertexIterator {
    let Constraint::Expression { arguments, .. } = &borrow(constraint).constraint else {
        unreachable!("Expected constraint to be Expression");
    };
    release(ConstraintVertexIterator(CIterator(box_stream(arguments.clone().into_iter()))))
}

/// Unwraps the <code>Constraint</code> instance as an `Is` constraint,
/// and returns the left-hand side vertex.
/// Will panic if the Constraint is not an Is constraint.
#[unsafe(no_mangle)]
pub extern "C" fn constraint_is_get_lhs(constraint: *const ConstraintWithSpan) -> *mut ConstraintVertex {
    let Constraint::Is { lhs, .. } = &borrow(constraint).constraint else {
        unreachable!("Expected constraint to be Is");
    };
    release(lhs.clone())
}

/// Unwraps the <code>Constraint</code> instance as an `Is` constraint,
/// and returns the right-hand side vertex.
/// Will panic if the Constraint is not an Is constraint.
#[unsafe(no_mangle)]
pub extern "C" fn constraint_is_get_rhs(constraint: *const ConstraintWithSpan) -> *mut ConstraintVertex {
    let Constraint::Is { rhs, .. } = &borrow(constraint).constraint else {
        unreachable!("Expected constraint to be Is");
    };
    release(rhs.clone())
}

/// Unwraps the <code>Constraint</code> instance as an `Iid` constraint,
/// and returns the concept (variable) the iid applies to.
/// Will panic if the Constraint is not an Iid constraint.
#[unsafe(no_mangle)]
pub extern "C" fn constraint_iid_get_variable(constraint: *const ConstraintWithSpan) -> *mut ConstraintVertex {
    let Constraint::Iid { concept, .. } = &borrow(constraint).constraint else {
        unreachable!("Expected constraint to be Iid");
    };
    release(concept.clone())
}

/// Unwraps the <code>Constraint</code> instance as an `Iid` constraint,
/// and returns the iid value as a string.
/// Will panic if the Constraint is not an Iid constraint.
#[unsafe(no_mangle)]
pub extern "C" fn constraint_iid_get_iid(constraint: *const ConstraintWithSpan) -> *mut c_char {
    let Constraint::Iid { iid, .. } = &borrow(constraint).constraint else {
        unreachable!("Expected constraint to be Iid");
    };
    release_string(iid.to_string())
}

/// Unwraps the <code>Constraint</code> instance as a `DeleteConcepts` constraint,
/// and returns the variables whose concepts are deleted.
/// Will panic if the Constraint is not a DeleteConcepts constraint.
#[unsafe(no_mangle)]
pub extern "C" fn constraint_delete_concepts_get_variables(
    constraint: *const ConstraintWithSpan,
) -> *mut ConstraintVertexIterator {
    let Constraint::DeleteConcepts { variables, .. } = &borrow(constraint).constraint else {
        unreachable!("Expected constraint to be DeleteConcepts");
    };
    release(ConstraintVertexIterator(CIterator(box_stream(variables.clone().into_iter()))))
}

/// Unwraps the <code>Constraint</code> instance as a `Comparison` constraint,
/// and returns the left-hand side vertex.
/// Will panic if the Constraint is not a Comparison constraint.
#[unsafe(no_mangle)]
pub extern "C" fn constraint_comparison_get_lhs(constraint: *const ConstraintWithSpan) -> *mut ConstraintVertex {
    let Constraint::Comparison { lhs, .. } = &borrow(constraint).constraint else {
        unreachable!("Expected constraint to be Comparison");
    };
    release(lhs.clone())
}

/// Unwraps the <code>Constraint</code> instance as a `Comparison` constraint,
/// and returns the right-hand side vertex.
/// Will panic if the Constraint is not a Comparison constraint.
#[unsafe(no_mangle)]
pub extern "C" fn constraint_comparison_get_rhs(constraint: *const ConstraintWithSpan) -> *mut ConstraintVertex {
    let Constraint::Comparison { rhs, .. } = &borrow(constraint).constraint else {
        unreachable!("Expected constraint to be Comparison");
    };
    release(rhs.clone())
}

/// Unwraps the <code>Constraint</code> instance as a `Comparison` constraint,
/// and returns the comparator used in the comparison.
/// Will panic if the Constraint is not a Comparison constraint.
#[unsafe(no_mangle)]
pub extern "C" fn constraint_comparison_get_comparator(constraint: *const ConstraintWithSpan) -> Comparator {
    let Constraint::Comparison { comparator, .. } = &borrow(constraint).constraint else {
        unreachable!("Expected constraint to be Comparison");
    };
    comparator.clone()
}

/// Unwraps the <code>Comparator</code> and returns its name (or symbol).
#[unsafe(no_mangle)]
pub extern "C" fn comparator_get_name(comparator: Comparator) -> *mut c_char {
    release_string(comparator.symbol().to_owned())
}

/// Unwraps the <code>Constraint</code> instance as a `Kind` constraint,
/// and returns the `Kind` of the type-vertex.
/// Will panic if the Constraint is not a Kind constraint.
#[unsafe(no_mangle)]
pub extern "C" fn constraint_kind_get_kind(constraint: *const ConstraintWithSpan) -> Kind {
    let Constraint::Kind { kind, .. } = &borrow(constraint).constraint else {
        unreachable!("Expected constraint to be Kind");
    };
    *kind
}

/// Unwraps the <code>Constraint</code> instance as a `Kind` constraint,
/// and returns the associated type vertex.
/// Will panic if the Constraint is not a Kind constraint.
#[unsafe(no_mangle)]
pub extern "C" fn constraint_kind_get_type(constraint: *const ConstraintWithSpan) -> *mut ConstraintVertex {
    let Constraint::Kind { r#type: type_, .. } = &borrow(constraint).constraint else {
        unreachable!("Expected constraint to be Kind");
    };
    release(type_.clone())
}

/// Unwraps the <code>Constraint</code> instance as a `Label` constraint,
/// and returns the type-vertex the label applies to.
/// Will panic if the Constraint is not a Label constraint.
#[unsafe(no_mangle)]
pub extern "C" fn constraint_label_get_variable(constraint: *const ConstraintWithSpan) -> *mut ConstraintVertex {
    let Constraint::Label { r#type: type_, .. } = &borrow(constraint).constraint else {
        unreachable!("Expected constraint to be Label");
    };
    release(type_.clone())
}

/// Unwraps the <code>Constraint</code> instance as a `Label` constraint,
/// and returns the label string.
/// Will panic if the Constraint is not a Label constraint.
#[unsafe(no_mangle)]
pub extern "C" fn constraint_label_get_label(constraint: *const ConstraintWithSpan) -> *mut c_char {
    let Constraint::Label { label, .. } = &borrow(constraint).constraint else {
        unreachable!("Expected constraint to be Label");
    };
    release_string(label.clone())
}

/// Unwraps the <code>Constraint</code> instance as a `Value` constraint,
/// and returns the attribute type vertex.
/// Will panic if the Constraint is not a Value constraint.
#[unsafe(no_mangle)]
pub extern "C" fn constraint_value_get_attribute_type(constraint: *const ConstraintWithSpan) -> *mut ConstraintVertex {
    let Constraint::Value { attribute_type, .. } = &borrow(constraint).constraint else {
        unreachable!("Expected constraint to be Value");
    };
    release(attribute_type.clone())
}

/// Unwraps the <code>Constraint</code> instance as a `Value` constraint,
/// and returns the specified ValueType as a string.
/// Will panic if the Constraint is not a Value constraint.
#[unsafe(no_mangle)]
pub extern "C" fn constraint_value_get_value_type(constraint: *const ConstraintWithSpan) -> *mut c_char {
    let Constraint::Value { value_type, .. } = &borrow(constraint).constraint else {
        unreachable!("Expected constraint to be Value");
    };
    release_string(value_type.name().to_owned())
}

/// Unwraps the `Constraint` instance as a ``, and returns the specified variables
#[unsafe(no_mangle)]
pub extern "C" fn constraint_isset_get_variables(constraint: *const ConstraintWithSpan) -> *mut ConstraintVertexIterator {
    let Constraint::IsSet { variables } = &borrow(constraint).constraint else {
        unreachable!("Expected constraint to be IsSet");
    };
    release(ConstraintVertexIterator(CIterator(box_stream(variables.clone().into_iter()))))
}

/// Unwraps the <code>Constraint</code> instance as an `Or` constraint,
/// and returns the <code>ConjunctionID</code>s of the conjunction in each of the branches.
/// Will panic if the Constraint is not an Or constraint.
#[unsafe(no_mangle)]
pub extern "C" fn constraint_or_get_branches(constraint: *const ConstraintWithSpan) -> *mut ConjunctionIDIterator {
    let Constraint::Or { branches, .. } = &borrow(constraint).constraint else {
        unreachable!("Expected constraint to be Or");
    };
    release(ConjunctionIDIterator(CIterator(box_stream(branches.clone().into_iter()))))
}

/// Unwraps the <code>Constraint</code> instance as a `Not` constraint,
/// and returns the <code>ConjunctionID</code> of the negated conjunction.
/// Will panic if the Constraint is not a Not constraint.
#[unsafe(no_mangle)]
pub extern "C" fn constraint_not_get_conjunction(constraint: *const ConstraintWithSpan) -> *mut ConjunctionID {
    let Constraint::Not { conjunction, .. } = &borrow(constraint).constraint else {
        unreachable!("Expected constraint to be Not");
    };
    release(*conjunction)
}

/// Unwraps the <code>Constraint</code> instance as a `Try` constraint,
/// and returns the <code>ConjunctionID</code> of the optionally matched conjunction.
/// Will panic if the Constraint is not a Try constraint.
#[unsafe(no_mangle)]
pub extern "C" fn constraint_try_get_conjunction(constraint: *const ConstraintWithSpan) -> *mut ConjunctionID {
    let Constraint::Try { conjunction, .. } = &borrow(constraint).constraint else {
        unreachable!("Expected constraint to be Try");
    };
    release(*conjunction)
}

/// Returns a string representation of the constraint
#[unsafe(no_mangle)]
pub extern "C" fn constraint_string_repr(constraint: *const ConstraintWithSpan) -> *mut c_char {
    release_string(format!("{:?}", &borrow(constraint).constraint))
}

// ConstraintVertex accessors
/// Returns the variant of the constraint
#[unsafe(no_mangle)]
pub extern "C" fn constraint_vertex_variant(vertex: *const ConstraintVertex) -> ConstraintVertexVariant {
    match borrow(vertex) {
        ConstraintVertex::Variable(_) => ConstraintVertexVariant::VariableVertex,
        ConstraintVertex::Label(_) => ConstraintVertexVariant::LabelVertex,
        ConstraintVertex::Value(_) => ConstraintVertexVariant::ValueVertex,
        ConstraintVertex::NamedRole(_) => ConstraintVertexVariant::NamedRoleVertex,
    }
}

/// Unwraps the <code>ConstraintVertex</code> instance as a Variable.
///  Will panic if the instance is not a Variable variant.
#[unsafe(no_mangle)]
pub extern "C" fn constraint_vertex_as_variable(vertex: *const ConstraintVertex) -> *mut Variable {
    match borrow(vertex) {
        ConstraintVertex::Variable(var) => release(var.clone()),
        _ => unreachable!(),
    }
}

/// Unwraps the <code>ConstraintVertex</code> instance as a Label.
/// Will panic if the instance is not a Label variant.
#[unsafe(no_mangle)]
pub extern "C" fn constraint_vertex_as_label(vertex: *const ConstraintVertex) -> *mut Concept {
    let as_concept = match borrow(vertex) {
        ConstraintVertex::Label(t) => type_to_concept(t.clone()),
        _ => unreachable!(),
    };
    release(as_concept)
}

/// Unwraps the <code>ConstraintVertex</code> instance as a Value.
/// Will panic if the instance is not a Value variant.
#[unsafe(no_mangle)]
pub extern "C" fn constraint_vertex_as_value(vertex: *const ConstraintVertex) -> *mut Concept {
    match borrow(vertex) {
        ConstraintVertex::Value(value) => release(Concept::Value(value.clone())),
        _ => unreachable!(),
    }
}

/// Unwraps the <code>ConstraintVertex</code> instance as a NamedRole.
/// 'links' & 'relates' constraints accept unscoped role names.
/// Since an unscoped role-name does not uniquely identify a role-type,
///  (Different role-types belonging to different relation types may share the same name)
///  an internal variable is introduced to handle the ambiguity
/// Will panic if the instance is not a NamedRole variant.
#[unsafe(no_mangle)]
pub extern "C" fn constraint_vertex_as_named_role(vertex: *const ConstraintVertex) -> *mut NamedRole {
    match borrow(vertex) {
        ConstraintVertex::NamedRole(named_role) => release(named_role.clone()),
        _ => unreachable!(),
    }
}

/// Returns the role-type variable associated with the NamedRole.
#[unsafe(no_mangle)]
pub extern "C" fn named_role_get_variable(named_role: *const NamedRole) -> *mut Variable {
    release(borrow(named_role).variable.clone())
}

/// Returns the role name associated with the NamedRole.
#[unsafe(no_mangle)]
pub extern "C" fn named_role_get_name(named_role: *const NamedRole) -> *mut c_char {
    release_string(borrow(named_role).name.clone())
}

/// Returns a u32 identifying the named role instance.
/// Meant to be used to implement hash, equality etc.
#[unsafe(no_mangle)]
pub extern "C" fn named_role_as_u32(named_role: *const NamedRole) -> u32 {
    borrow(named_role).variable.0
}

/// Returns a string representation of the NamedRole
#[unsafe(no_mangle)]
pub extern "C" fn named_role_string_repr(named_role: *const NamedRole) -> *mut c_char {
    release_string(format!("{:?}", borrow(named_role)))
}

#[doc = "Forwards the <code>FunctionIterator</code> and returns the next <code>Function</code> if it exists, or null if there are no more elements."]
#[unsafe(no_mangle)]
pub extern "C" fn function_iterator_next(it: *mut FunctionIterator) -> *mut Function {
    unsafe { iterator_next(addr_of_mut!((*it).0)) }
}

#[doc = "Frees the native rust <code>FunctionIterator</code> object."]
#[unsafe(no_mangle)]
pub extern "C" fn function_iterator_drop(it: *mut FunctionIterator) {
    free(it);
}

#[doc = "Forwards the <code>ConjunctionIDIterator</code> and returns the next <code>ConjunctionID</code> if it exists, or null if there are no more elements."]
#[unsafe(no_mangle)]
pub extern "C" fn conjunction_id_iterator_next(it: *mut ConjunctionIDIterator) -> *mut ConjunctionID {
    unsafe { iterator_next(addr_of_mut!((*it).0)) }
}

#[doc = "Frees the native rust <code>ConjunctionIDIterator</code> object."]
#[unsafe(no_mangle)]
pub extern "C" fn conjunction_id_iterator_drop(it: *mut ConjunctionIDIterator) {
    free(it);
}

#[doc = "Forwards the <code>ConstraintWithSpanIterator</code> and returns the next <code>ConstraintWithSpan</code> if it exists, or null if there are no more elements."]
#[unsafe(no_mangle)]
pub extern "C" fn constraint_with_span_iterator_next(it: *mut ConstraintWithSpanIterator) -> *mut ConstraintWithSpan {
    unsafe { iterator_next(addr_of_mut!((*it).0)) }
}

#[doc = "Frees the native rust <code>ConstraintWithSpanIterator</code> object."]
#[unsafe(no_mangle)]
pub extern "C" fn constraint_with_span_iterator_drop(it: *mut ConstraintWithSpanIterator) {
    free(it);
}

#[doc = "Forwards the <code>ConstraintVertexIterator</code> and returns the next <code>ConstraintVertex</code> if it exists, or null if there are no more elements."]
#[unsafe(no_mangle)]
pub extern "C" fn constraint_vertex_iterator_next(it: *mut ConstraintVertexIterator) -> *mut ConstraintVertex {
    unsafe { iterator_next(addr_of_mut!((*it).0)) }
}

#[doc = "Frees the native rust <code>ConstraintVertexIterator</code> object."]
#[unsafe(no_mangle)]
pub extern "C" fn constraint_vertex_iterator_drop(it: *mut ConstraintVertexIterator) {
    free(it);
}

#[doc = "Forwards the <code>PipelineStageIterator</code> and returns the next <code>PipelineStage</code> if it exists, or null if there are no more elements."]
#[unsafe(no_mangle)]
pub extern "C" fn pipeline_stage_iterator_next(it: *mut PipelineStageIterator) -> *mut PipelineStage {
    unsafe { iterator_next(addr_of_mut!((*it).0)) }
}

#[doc = "Frees the native rust <code>SortVariableIterator</code> object."]
#[unsafe(no_mangle)]
pub extern "C" fn sort_variable_iterator_drop(it: *mut SortVariableIterator) {
    free(it);
}

#[doc = "Forwards the <code>ReduceAssignmentIterator</code> and returns the next <code>ReduceAssignment</code> if it exists, or null if there are no more elements."]
#[unsafe(no_mangle)]
pub extern "C" fn reduce_assignment_iterator_next(it: *mut ReduceAssignmentIterator) -> *mut ReduceAssignment {
    unsafe { iterator_next(addr_of_mut!((*it).0)) }
}

#[doc = "Frees the native rust <code>ReduceAssignmentIterator</code> object."]
#[unsafe(no_mangle)]
pub extern "C" fn reduce_assignment_iterator_drop(it: *mut ReduceAssignmentIterator) {
    free(it);
}

#[doc = "Forwards the <code>ReducerIterator</code> and returns the next <code>Reducer</code> if it exists, or null if there are no more elements."]
#[unsafe(no_mangle)]
pub extern "C" fn reducer_iterator_next(it: *mut ReducerIterator) -> *mut Reducer {
    unsafe { iterator_next(addr_of_mut!((*it).0)) }
}

#[doc = "Frees the native rust <code>ReducerIterator</code> object."]
#[unsafe(no_mangle)]
pub extern "C" fn reducer_iterator_drop(it: *mut ReducerIterator) {
    free(it);
}

#[doc = "Frees the native rust <code>PipelineStageIterator</code> object."]
#[unsafe(no_mangle)]
pub extern "C" fn pipeline_stage_iterator_drop(it: *mut PipelineStageIterator) {
    free(it);
}

#[doc = "Forwards the <code>SortVariableIterator</code> and returns the next <code>SortVariable</code> if it exists, or null if there are no more elements."]
#[unsafe(no_mangle)]
pub extern "C" fn sort_variable_iterator_next(it: *mut SortVariableIterator) -> *mut SortVariable {
    unsafe { iterator_next(addr_of_mut!((*it).0)) }
}

#[doc = "Forwards the <code>VariableAnnotationsIterator</code> and returns the next <code>VariableAnnotations</code> if it exists, or null if there are no more elements."]
#[unsafe(no_mangle)]
pub extern "C" fn variable_annotations_iterator_next(it: *mut VariableAnnotationsIterator) -> *mut VariableAnnotations {
    unsafe { iterator_next(addr_of_mut!((*it).0)) }
}

#[doc = "Frees the native rust <code>VariableAnnotationsIterator</code> object."]
#[unsafe(no_mangle)]
pub extern "C" fn variable_annotations_iterator_drop(it: *mut VariableAnnotationsIterator) {
    free(it);
}

#[doc = "Forwards the <code>VariableIterator</code> and returns the next <code>Variable</code> if it exists, or null if there are no more elements."]
#[unsafe(no_mangle)]
pub extern "C" fn variable_iterator_next(it: *mut VariableIterator) -> *mut Variable {
    unsafe { iterator_next(addr_of_mut!((*it).0)) }
}

#[doc = "Frees the native rust <code>VariableIterator</code> object."]
#[unsafe(no_mangle)]
pub extern "C" fn variable_iterator_drop(it: *mut VariableIterator) {
    free(it);
}

#[doc = "Frees the native rust <code>AnalyzedQuery</code> object."]
#[unsafe(no_mangle)]
pub extern "C" fn analyzed_query_drop(obj: *mut AnalyzedQuery) {
    free(obj);
}

#[doc = "Frees the native rust <code>Conjunction</code> object."]
#[unsafe(no_mangle)]
pub extern "C" fn conjunction_drop(obj: *mut Conjunction) {
    free(obj);
}

#[doc = "Frees the native rust <code>ConjunctionID</code> object."]
#[unsafe(no_mangle)]
pub extern "C" fn conjunction_id_drop(obj: *mut ConjunctionID) {
    free(obj);
}

#[doc = "Frees the native rust <code>Constraint</code> object."]
#[unsafe(no_mangle)]
pub extern "C" fn constraint_with_span_drop(obj: *mut ConstraintWithSpan) {
    free(obj);
}

#[doc = "Frees the native rust <code>ConstraintVertex</code> object."]
#[unsafe(no_mangle)]
pub extern "C" fn constraint_vertex_drop(obj: *mut ConstraintVertex) {
    free(obj);
}

#[doc = "Frees the native rust <code>Given</code> object."]
#[unsafe(no_mangle)]
pub extern "C" fn given_drop(obj: *mut Given) {
    free(obj);
}

#[doc = "Frees the native rust <code>Fetch</code> object."]
#[unsafe(no_mangle)]
pub extern "C" fn fetch_drop(obj: *mut Fetch) {
    free(obj);
}

#[doc = "Frees the native rust <code>Function</code> object."]
#[unsafe(no_mangle)]
pub extern "C" fn function_drop(obj: *mut Function) {
    free(obj);
}

#[doc = "Frees the native rust <code>NamedRole</code> object."]
#[unsafe(no_mangle)]
pub extern "C" fn named_role_drop(obj: *mut NamedRole) {
    free(obj);
}

#[doc = "Frees the native rust <code>ReturnOperation</code> object."]
#[unsafe(no_mangle)]
pub extern "C" fn return_operation_drop(obj: *mut ReturnOperation) {
    free(obj);
}

#[doc = "Frees the native rust <code>Pipeline</code> object."]
#[unsafe(no_mangle)]
pub extern "C" fn pipeline_drop(obj: *mut Pipeline) {
    free(obj);
}

#[doc = "Frees the native rust <code>PipelineStage</code> object."]
#[unsafe(no_mangle)]
pub extern "C" fn pipeline_stage_drop(obj: *mut PipelineStage) {
    free(obj);
}

#[doc = "Frees the native rust <code>Reducer</code> object."]
#[unsafe(no_mangle)]
pub extern "C" fn reducer_drop(obj: *mut Reducer) {
    free(obj);
}

#[doc = "Frees the native rust <code>ReduceAssignment</code> object."]
#[unsafe(no_mangle)]
pub extern "C" fn reduce_assignment_drop(obj: *mut ReduceAssignment) {
    free(obj);
}

#[doc = "Frees the native rust <code>SortVariable</code> object."]
#[unsafe(no_mangle)]
pub extern "C" fn sort_variable_drop(obj: *mut SortVariable) {
    free(obj);
}

#[doc = "Frees the native rust <code>VariableAnnotations</code> object."]
#[unsafe(no_mangle)]
pub extern "C" fn variable_annotations_drop(obj: *mut VariableAnnotations) {
    free(obj);
}

#[doc = "Frees the native rust <code>Variable</code> object."]
#[unsafe(no_mangle)]
pub extern "C" fn variable_drop(obj: *mut Variable) {
    free(obj);
}

fn type_to_concept(type_: Type) -> Concept {
    match type_ {
        Type::EntityType(t) => Concept::EntityType(t),
        Type::RelationType(t) => Concept::RelationType(t),
        Type::AttributeType(t) => Concept::AttributeType(t),
        Type::RoleType(t) => Concept::RoleType(t),
    }
}
