# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

import re
from enum import Enum

from typedb.analyze import (
    Constraint, ConstraintVertex, Variable,
    ConjunctionID, Pipeline, PipelineStage, Fetch, Given,
    Function, ReturnOperation,
    ReduceStage, Reducer, SortStage, VariableAnnotations,
)
from typedb.analyze.variants import ConstraintVariant, PipelineStageVariant, FetchVariant, ReturnOperationVariant
from typedb.common.enums import ConstraintExactness, SortOrder
from typedb.driver import Type

from typing import Iterable, Dict


class FunctorEncoder:
    def __init__(self, pipeline: Pipeline):
        self.pipeline = pipeline

    def make_functor(self, name: str, *args) -> str:
        args = ", ".join(map(self._may_encode, args))
        return f"{name}({args})"

    def _may_encode(self, e: any) -> str:
        if isinstance(e, str):
            return e
        elif isinstance(e, list) or isinstance(e, map):
            return self.encode_as_list(e)
        elif isinstance(e, dict):
            return self.encode_as_dict(e)
        else:
            return e.encode_as_functor(self)

    def encode_as_list(self, elements: Iterable[any]) -> str:
        return "[" + ", ".join(map(self._may_encode, elements)) + "]"

    def encode_as_dict(self, elements: Dict[any, any]) -> str:
        encoded_kv = sorted(list((self._may_encode(k), self._may_encode(v)) for (k, v) in elements.items()))
        return "{" + ",".join("%s : %s" % kv for kv in encoded_kv) + "}"


WHITE_SPACE_REGEX = re.compile(r'\s+')
SUBPATTERN_VARIANTS = [ConstraintVariant.Or, ConstraintVariant.Try, ConstraintVariant.Not]


def normalize_functor_for_compare(functor: str) -> str:
    return re.sub(WHITE_SPACE_REGEX, '', functor.lower())


def _encode_enum_as_name(self: Enum, encoder: FunctorEncoder):
    return self.name


def _encode_conjunction_id(conj_id: "ConjunctionID", encoder: FunctorEncoder) -> str:
    conjunction = encoder.pipeline.conjunction(conj_id)
    return encoder.encode_as_list(conjunction.constraints())


def _encode_constraint(constraint: Constraint, encoder: FunctorEncoder) -> str:
    variant = constraint.variant()

    def with_exactness(constraint_name: str, exactness: ConstraintExactness) -> str:
        suffix = "Exact" if exactness == ConstraintExactness.Exact else ""
        return constraint_name + suffix

    if constraint.is_isa():
        isa = constraint.as_isa()
        name = with_exactness("Isa", isa.exactness())
        return encoder.make_functor(name, isa.instance(), isa.type())

    elif constraint.is_has():
        has = constraint.as_has()
        name = with_exactness("Has", has.exactness())
        return encoder.make_functor(name, has.owner(), has.attribute())

    elif constraint.is_links():
        links = constraint.as_links()
        name = with_exactness("Links", links.exactness())
        return encoder.make_functor(name, links.relation(), links.player(), links.role())

    elif constraint.is_sub():
        sub = constraint.as_sub()
        name = with_exactness("Sub", sub.exactness())
        return encoder.make_functor(name, sub.subtype(), sub.supertype())

    elif constraint.is_owns():
        owns = constraint.as_owns()
        name = with_exactness("Owns", owns.exactness())
        return encoder.make_functor(name, owns.owner(), owns.attribute())

    elif constraint.is_relates():
        relates = constraint.as_relates()
        name = with_exactness("Relates", relates.exactness())
        return encoder.make_functor(name, relates.relation(), relates.role())

    elif constraint.is_plays():
        plays = constraint.as_plays()
        name = with_exactness("Plays", plays.exactness())
        return encoder.make_functor(name, plays.player(), plays.role())

    elif constraint.is_function_call():
        fc = constraint.as_function_call()
        return encoder.make_functor("FunctionCall", fc.name(), fc.assigned(), fc.arguments())

    elif constraint.is_expression():
        ex = constraint.as_expression()
        return encoder.make_functor("Expression", ex.text(), ex.assigned(), ex.arguments())

    elif constraint.is_is():
        is_ = constraint.as_is()
        return encoder.make_functor("Is", is_.lhs(), is_.rhs())

    elif constraint.is_iid():
        iid = constraint.as_iid()
        return encoder.make_functor("Iid", iid.variable(), iid.iid())

    elif constraint.is_delete_concepts():
        delete_concepts = constraint.as_delete_concepts()
        return encoder.make_functor("DeleteConcepts", delete_concepts.variables())

    elif constraint.is_comparison():
        comp = constraint.as_comparison()
        comparator = comp.comparator()
        comp_name = comparator.symbol()
        return encoder.make_functor("Comparison", comp.lhs(), comp.rhs(), comp_name)

    elif constraint.is_kind_of():
        k = constraint.as_kind()
        return encoder.make_functor("Kind", k.kind(), k.type())

    elif constraint.is_label():
        as_label = constraint.as_label()
        return encoder.make_functor("Label", as_label.variable(), as_label.label())

    elif constraint.is_value():
        val = constraint.as_value()
        return encoder.make_functor("Value", val.attribute_type(), val.value_type())

    elif constraint.is_isset():
        is_set = constraint.as_isset()
        return encoder.make_functor("IsSet", is_set.variables())

    elif constraint.is_or():
        or_ = constraint.as_or()
        return encoder.make_functor("Or", or_.branches())

    elif constraint.is_not():
        not_ = constraint.as_not()
        return encoder.make_functor("Not", not_.conjunction())

    elif constraint.is_try():
        try_ = constraint.as_try()
        return encoder.make_functor("Try", try_.conjunction())
    else:
        raise Exception(f"Unknown constraint variant: " + variant.name)


def _encode_constraint_vertex(constraint_vertex: ConstraintVertex, encoder: FunctorEncoder) -> str:
    if constraint_vertex.is_variable():
        return constraint_vertex.as_variable().encode_as_functor(encoder)
    elif constraint_vertex.is_label():
        type_ = constraint_vertex.as_label()
        return type_.get_label()
    elif constraint_vertex.is_value():
        value = constraint_vertex.as_value()
        return f"\"{value}\"" if value.is_string() else str(value)
    elif constraint_vertex.is_named_role():
        return constraint_vertex.as_named_role().name()


def _encode_variable(self, encoder: FunctorEncoder) -> str:
    name = encoder.pipeline.get_variable_name(self)
    return "$" + ("_" if name is None else name)


def _encode_sort_variable(self, encoder: FunctorEncoder) -> str:
    order = "Asc" if self.order() == SortOrder.Ascending else "Desc"
    return encoder.make_functor(order, self.variable().encode_as_functor(encoder))


def _encode_reduce_assignment(self: ReduceStage.ReduceAssignment, encoder: FunctorEncoder) -> str:
    return encoder.make_functor(
        "ReduceAssign",
        self.assigned().encode_as_functor(encoder),
        self.reducer().encode_as_functor(encoder)
    )


def _encode_reducer(self: Reducer, encoder: FunctorEncoder) -> str:
    return encoder.make_functor("Reducer", self.name(), self.arguments())


def _encode_pipeline_stage(self: PipelineStage, encoder: FunctorEncoder) -> str:
    variant = self.variant()
    name = variant.name
    if variant in [PipelineStageVariant.Match, PipelineStageVariant.Insert, PipelineStageVariant.Put,
                   PipelineStageVariant.Update]:
        return encoder.make_functor(name, self.block())
    elif variant == PipelineStageVariant.Delete:
        return encoder.make_functor(name, self.as_delete().deleted_variables(), self.as_delete().block())
    elif variant == PipelineStageVariant.Select:
        return encoder.make_functor(name, self.as_select().variables())
    elif variant == PipelineStageVariant.Sort:
        return encoder.make_functor(name, self.as_sort().variables())
    elif variant == PipelineStageVariant.Require:
        return encoder.make_functor(name, self.as_require().variables())
    elif variant == PipelineStageVariant.Offset:
        return encoder.make_functor(name, str(self.as_offset().offset()))
    elif variant == PipelineStageVariant.Limit:
        return encoder.make_functor(name, str(self.as_limit().limit()))
    elif variant == PipelineStageVariant.Distinct:
        return encoder.make_functor(name)
    elif variant == PipelineStageVariant.Reduce:
        return encoder.make_functor(name, self.as_reduce().reduce_assignments(), self.as_reduce().group_by())


def _encode_pipeline(self: Pipeline, encoder: FunctorEncoder) -> str:
    return encoder.make_functor("Pipeline", self.stages())


def _encode_return_operation(self: ReturnOperation, encoder: FunctorEncoder) -> str:
    variant = self.variant()
    if variant == ReturnOperationVariant.StreamReturn:
        return encoder.make_functor("Stream", self.as_stream().variables())
    elif variant == ReturnOperationVariant.SingleReturn:
        return encoder.make_functor("Single", self.as_single().selector(), self.as_single().variables())
    elif variant == ReturnOperationVariant.CheckReturn:
        return encoder.make_functor("Check", "")
    elif variant == ReturnOperationVariant.ReduceReturn:
        return encoder.make_functor("Reduce", self.as_reduce().reducers())
    else:
        raise Exception(f"Unknown return-operation variant: " + variant.name)


def _encode_function(self: Function, encoder: FunctorEncoder) -> str:
    return encoder.make_functor(
        "Function",
        self.argument_variables(),
        self.return_operation(),
        self.body()
    )


def _encode_type(self: Type, encoder: FunctorEncoder) -> str:
    return self.get_label()


def _encode_variable_annotations(self: VariableAnnotations, encoder: FunctorEncoder) -> str:
    if self.is_instance():
        return encoder.make_functor("Instance", self.as_instance())
    elif self.is_type():
        return encoder.make_functor("Type", self.as_type())
    elif self.is_value():
        return encoder.make_functor("Value", self.as_value())
    else:
        raise Exception(f"Unknown VariableAnnoations variant: " + self.variant().name)


def _encode_subpattern_annotations(self: Constraint, encoder: FunctorEncoder) -> str:
    if self.is_or():
        branches = [_encode_conjunction_annotations(branch, encoder) for branch in self.as_or().branches()]
        return encoder.make_functor("Or", branches)
    elif self.is_not():
        inner_annotations = _encode_conjunction_annotations(self.as_not().conjunction(), encoder)
        return encoder.make_functor("Not", inner_annotations)
    elif self.is_try():
        inner_annotations = _encode_conjunction_annotations(self.as_try().conjunction(), encoder)
        return encoder.make_functor("Try", inner_annotations)
    else:
        raise Exception(f"Illegal Subpattern constraint variant: " + self.variant().name)


def _encode_conjunction_annotations(self: "ConjunctionID", encoder: FunctorEncoder) -> str:
    conj = encoder.pipeline.conjunction(self)
    trunk_annotations = {var: conj.variable_annotations(var) for var in conj.annotated_variables()}
    subpattern_annotations = [_encode_subpattern_annotations(c, encoder)
                              for c in conj.constraints() if c.variant() in SUBPATTERN_VARIANTS]
    return encoder.make_functor("And", trunk_annotations, subpattern_annotations)


def _encode_stage_annotations(self: PipelineStage, encoder: FunctorEncoder) -> str:
    variant = self.variant()
    if variant in [PipelineStageVariant.Match, PipelineStageVariant.Insert, PipelineStageVariant.Update,
                   PipelineStageVariant.Put, PipelineStageVariant.Delete]:
        return encoder.make_functor(variant.name, _encode_conjunction_annotations(self.block(), encoder))
    else:
        return encoder.make_functor(variant.name, "")


def _encode_pipeline_annotations(self: Pipeline, encoder: FunctorEncoder) -> str:
    stage_annotations = [_encode_stage_annotations(s, encoder) for s in self.stages()]
    return encoder.make_functor("Pipeline", stage_annotations)


def _encode_function_annotations(self: Function, encoder: FunctorEncoder) -> str:
    argument_annotations = encoder.encode_as_list(self.argument_annotations())
    return_op_name = self.return_operation().variant().name.replace("Return", "")
    return_annotations = encoder.make_functor(return_op_name, self.return_annotations())
    body_annotations = _encode_pipeline_annotations(self.body(), encoder)
    return encoder.make_functor("Function", argument_annotations, return_annotations, body_annotations)


def _encode_given_structure(self: Given, encoder: FunctorEncoder) -> str:
    return encoder.make_functor("Given", self.variables())


def _encode_given_annotations(self: Given, encoder: FunctorEncoder) -> str:
    variable_annotations = {var: self.variable_annotations(var) for var in self.variables()}
    return encoder.make_functor("Given", variable_annotations)


def _encode_fetch_annotations(self: Fetch, encoder: FunctorEncoder) -> str:
    if self.is_leaf():
        return encoder.encode_as_list(self.as_leaf().annotations())
    elif self.is_list():
        return encoder.make_functor("List", self.as_list().element().encode_as_functor(encoder))
    if self.is_object():
        as_object = self.as_object()
        kv = {k: self.get(k) for k in as_object.keys()}
        return encoder.encode_as_dict(kv)
    else:
        raise Exception(f"Unknown fetch variant: " + self.variant().name)


def _encode_iterator_wrapper(self: "IteratorWrapper", encoder: FunctorEncoder) -> str:
    return encoder.encode_as_list(e for e in self)


# When this file is imported, we inject the encode_as_functor implementations
def monkey_patch_all():
    import typedb
    Enum.encode_as_functor = _encode_enum_as_name

    Constraint.encode_as_functor = _encode_constraint
    ConstraintVertex.encode_as_functor = _encode_constraint_vertex
    Fetch.encode_as_functor = _encode_fetch_annotations
    Function.encode_as_functor = _encode_function
    Pipeline.encode_as_functor = _encode_pipeline
    PipelineStage.encode_as_functor = _encode_pipeline_stage
    ReturnOperation.encode_as_functor = _encode_return_operation
    ReduceStage.ReduceAssignment.encode_as_functor = _encode_reduce_assignment
    Reducer.encode_as_functor = _encode_reducer
    SortStage.SortVariable.encode_as_functor = _encode_sort_variable
    Type.encode_as_functor = _encode_type
    VariableAnnotations.encode_as_functor = _encode_variable_annotations

    ConjunctionID.encode_as_functor = _encode_conjunction_id
    typedb.common.iterator_wrapper.IteratorWrapper.encode_as_functor = _encode_iterator_wrapper
    Variable.encode_as_functor = _encode_variable


monkey_patch_all()
