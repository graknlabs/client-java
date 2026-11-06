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
from __future__ import annotations

from abc import ABC
from typing import TYPE_CHECKING, Iterator

from typedb.common.native_wrapper import NativeWrapper
from typedb.common.iterator_wrapper import IteratorWrapper
from typedb.common.exception import TypeDBDriverException, ILLEGAL_STATE, INVALID_CONSTRAINT_CASTING, \
    UNEXPECTED_NATIVE_VALUE

from typedb.api.analyze.constraint import (
    Constraint, Span,
    Isa, Has, Links, Sub, Owns, Relates, Plays,
    FunctionCall, Expression, Is, Iid, DeleteConcepts, Comparison, Kind, Label, Value, IsSet,
    Or, Not, Try,
)
from typedb.analyze.constraint_vertex import _ConstraintVertex
from typedb.analyze.conjunction_id import _ConjunctionID
from typedb.analyze.variants import ConstraintVariant
from typedb.common.enums import Comparator, ConstraintExactness

from typedb.native_driver_wrapper import (
    ConstraintWithSpan as NativeConstraint,

    comparator_get_name,
    conjunction_id_iterator_next,
    constraint_variant,
    constraint_vertex_iterator_next,

    constraint_span_begin,
    constraint_span_end,

    constraint_isa_get_instance,
    constraint_isa_get_type,
    constraint_isa_get_exactness,
    constraint_has_get_owner,
    constraint_has_get_attribute,
    constraint_has_get_exactness,
    constraint_links_get_relation,
    constraint_links_get_player,
    constraint_links_get_role,
    constraint_links_get_exactness,
    constraint_sub_get_subtype,
    constraint_sub_get_supertype,
    constraint_sub_get_exactness,
    constraint_owns_get_owner,
    constraint_owns_get_attribute,
    constraint_owns_get_exactness,
    constraint_relates_get_relation,
    constraint_relates_get_role,
    constraint_relates_get_exactness,
    constraint_plays_get_player,
    constraint_plays_get_role,
    constraint_plays_get_exactness,
    constraint_function_call_get_name,
    constraint_function_call_get_arguments,
    constraint_function_call_get_assigned,
    constraint_expression_get_text,
    constraint_expression_get_arguments,
    constraint_expression_get_assigned,
    constraint_is_get_lhs,
    constraint_is_get_rhs,
    constraint_iid_get_variable,
    constraint_iid_get_iid,
    constraint_delete_concepts_get_variables,
    constraint_comparison_get_lhs,
    constraint_comparison_get_rhs,
    constraint_comparison_get_comparator,
    comparator_get_name,
    constraint_kind_get_kind,
    constraint_kind_get_type,
    constraint_label_get_variable,
    constraint_label_get_label,
    constraint_value_get_attribute_type,
    constraint_value_get_value_type,
    constraint_isset_get_variables,
    constraint_or_get_branches,
    constraint_not_get_conjunction,
    constraint_try_get_conjunction,
    constraint_string_repr,
)

if TYPE_CHECKING:
    from typedb.api.analyze.constraint import Constraint, Span
    from typedb.api.analyze.constraint_vertex import ConstraintVertex
    from typedb.api.analyze.conjunction_id import ConjunctionID
    import typedb


class _Constraint(Constraint, NativeWrapper[NativeConstraint], ABC):
    @property
    def _native_object_not_owned_exception(self) -> TypeDBDriverException:
        return TypeDBDriverException(ILLEGAL_STATE)

    @staticmethod
    def of(native: NativeConstraint) -> "_Constraint":
        variant = ConstraintVariant(constraint_variant(native))
        if variant == ConstraintVariant.Isa:
            return _Isa(native)
        if variant == ConstraintVariant.Has:
            return _Has(native)
        if variant == ConstraintVariant.Links:
            return _Links(native)
        if variant == ConstraintVariant.Sub:
            return _Sub(native)
        if variant == ConstraintVariant.Owns:
            return _Owns(native)
        if variant == ConstraintVariant.Relates:
            return _Relates(native)
        if variant == ConstraintVariant.Plays:
            return _Plays(native)
        if variant == ConstraintVariant.FunctionCall:
            return _FunctionCall(native)
        if variant == ConstraintVariant.Expression:
            return _Expression(native)
        if variant == ConstraintVariant.Is:
            return _Is(native)
        if variant == ConstraintVariant.Iid:
            return _Iid(native)
        if variant == ConstraintVariant.DeleteConcepts:
            return _DeleteConcepts(native)
        if variant == ConstraintVariant.Comparison:
            return _Comparison(native)
        if variant == ConstraintVariant.KindOf:
            return _Kind(native)
        if variant == ConstraintVariant.Label:
            return _Label(native)
        if variant == ConstraintVariant.Value:
            return _Value(native)
        if variant == ConstraintVariant.IsSet:
            return _IsSet(native)
        if variant == ConstraintVariant.Or:
            return _Or(native)
        if variant == ConstraintVariant.Not:
            return _Not(native)
        if variant == ConstraintVariant.Try:
            return _Try(native)
        raise TypeDBDriverException(UNEXPECTED_NATIVE_VALUE)

    def variant(self) -> "ConstraintVariant":
        return ConstraintVariant(constraint_variant(self.native_object))

    def span(self) -> "Span":
        return _Constraint.SpanImpl(constraint_span_begin(self.native_object), constraint_span_end(self.native_object))

    def is_isa(self) -> bool:
        return False

    def is_has(self) -> bool:
        return False

    def is_links(self) -> bool:
        return False

    def is_sub(self) -> bool:
        return False

    def is_owns(self) -> bool:
        return False

    def is_relates(self) -> bool:
        return False

    def is_plays(self) -> bool:
        return False

    def is_function_call(self) -> bool:
        return False

    def is_expression(self) -> bool:
        return False

    def is_is(self) -> bool:
        return False

    def is_iid(self) -> bool:
        return False

    def is_delete_concepts(self) -> bool:
        return False

    def is_comparison(self) -> bool:
        return False

    def is_kind_of(self) -> bool:
        return False

    def is_label(self) -> bool:
        return False

    def is_value(self) -> bool:
        return False

    def is_isset(self) -> bool:
        return False

    def is_or(self) -> bool:
        return False

    def is_not(self) -> bool:
        return False

    def is_try(self) -> bool:
        return False

    def as_isa(self):
        raise TypeDBDriverException(INVALID_CONSTRAINT_CASTING, (self.__class__.__name__, "Isa"))

    def as_has(self):
        raise TypeDBDriverException(INVALID_CONSTRAINT_CASTING, (self.__class__.__name__, "Has"))

    def as_links(self):
        raise TypeDBDriverException(INVALID_CONSTRAINT_CASTING, (self.__class__.__name__, "Links"))

    def as_sub(self):
        raise TypeDBDriverException(INVALID_CONSTRAINT_CASTING, (self.__class__.__name__, "Sub"))

    def as_owns(self):
        raise TypeDBDriverException(INVALID_CONSTRAINT_CASTING, (self.__class__.__name__, "Owns"))

    def as_relates(self):
        raise TypeDBDriverException(INVALID_CONSTRAINT_CASTING, (self.__class__.__name__, "Relates"))

    def as_plays(self):
        raise TypeDBDriverException(INVALID_CONSTRAINT_CASTING, (self.__class__.__name__, "Plays"))

    def as_function_call(self):
        raise TypeDBDriverException(INVALID_CONSTRAINT_CASTING, (self.__class__.__name__, "FunctionCall"))

    def as_expression(self):
        raise TypeDBDriverException(INVALID_CONSTRAINT_CASTING, (self.__class__.__name__, "Expression"))

    def as_is(self):
        raise TypeDBDriverException(INVALID_CONSTRAINT_CASTING, (self.__class__.__name__, "Is"))

    def as_iid(self):
        raise TypeDBDriverException(INVALID_CONSTRAINT_CASTING, (self.__class__.__name__, "Iid"))

    def as_delete_concepts(self):
        raise TypeDBDriverException(INVALID_CONSTRAINT_CASTING, (self.__class__.__name__, "DeleteConcepts"))

    def as_comparison(self):
        raise TypeDBDriverException(INVALID_CONSTRAINT_CASTING, (self.__class__.__name__, "Comparison"))

    def as_kind(self):
        raise TypeDBDriverException(INVALID_CONSTRAINT_CASTING, (self.__class__.__name__, "Kind"))

    def as_label(self):
        raise TypeDBDriverException(INVALID_CONSTRAINT_CASTING, (self.__class__.__name__, "Label"))

    def as_value(self):
        raise TypeDBDriverException(INVALID_CONSTRAINT_CASTING, (self.__class__.__name__, "Value"))

    def as_isset(self):
        raise TypeDBDriverException(INVALID_CONSTRAINT_CASTING, (self.__class__.__name__, "IsSet"))

    def as_or(self):
        raise TypeDBDriverException(INVALID_CONSTRAINT_CASTING, (self.__class__.__name__, "Or"))

    def as_not(self):
        raise TypeDBDriverException(INVALID_CONSTRAINT_CASTING, (self.__class__.__name__, "Not"))

    def as_try(self):
        raise TypeDBDriverException(INVALID_CONSTRAINT_CASTING, (self.__class__.__name__, "Try"))

    def __repr__(self):
        return constraint_string_repr(self.native_object)

    class SpanImpl(Span):
        def __init__(self, begin: int, end: int):
            self._begin = begin
            self._end = end

        def begin(self) -> int:
            return self._begin

        def end(self) -> int:
            return self._end


class _Isa(_Constraint, Isa):
    def __init__(self, native):
        super().__init__(native)

    def is_isa(self) -> bool:
        return True

    def as_isa(self):
        return self

    def instance(self) -> "ConstraintVertex":
        native = constraint_isa_get_instance(self.native_object)
        return _ConstraintVertex(native)

    def type(self) -> "ConstraintVertex":
        native = constraint_isa_get_type(self.native_object)
        return _ConstraintVertex(native)

    def exactness(self) -> "ConstraintExactness":
        return ConstraintExactness(constraint_isa_get_exactness(self.native_object))


class _Has(_Constraint, Has):
    def __init__(self, native):
        super().__init__(native)

    def is_has(self) -> bool:
        return True

    def as_has(self):
        return self

    def owner(self) -> "ConstraintVertex":
        native = constraint_has_get_owner(self.native_object)
        return _ConstraintVertex(native)

    def attribute(self) -> "ConstraintVertex":
        native = constraint_has_get_attribute(self.native_object)
        return _ConstraintVertex(native)

    def exactness(self) -> "ConstraintExactness":
        return ConstraintExactness(constraint_has_get_exactness(self.native_object))


class _Links(_Constraint, Links):
    def __init__(self, native):
        super().__init__(native)

    def is_links(self) -> bool:
        return True

    def as_links(self):
        return self

    def relation(self) -> "ConstraintVertex":
        native = constraint_links_get_relation(self.native_object)
        return _ConstraintVertex(native)

    def player(self) -> "ConstraintVertex":
        native = constraint_links_get_player(self.native_object)
        return _ConstraintVertex(native)

    def role(self) -> "ConstraintVertex":
        native = constraint_links_get_role(self.native_object)
        return _ConstraintVertex(native)

    def exactness(self) -> "ConstraintExactness":
        return ConstraintExactness(constraint_links_get_exactness(self.native_object))


class _Sub(_Constraint, Sub):
    def __init__(self, native):
        super().__init__(native)

    def is_sub(self) -> bool:
        return True

    def as_sub(self):
        return self

    def subtype(self) -> "ConstraintVertex":
        native = constraint_sub_get_subtype(self.native_object)
        return _ConstraintVertex(native)

    def supertype(self) -> "ConstraintVertex":
        native = constraint_sub_get_supertype(self.native_object)
        return _ConstraintVertex(native)

    def exactness(self) -> "ConstraintExactness":
        return ConstraintExactness(constraint_sub_get_exactness(self.native_object))


class _Owns(_Constraint, Owns):
    def __init__(self, native):
        super().__init__(native)

    def is_owns(self) -> bool:
        return True

    def as_owns(self):
        return self

    def owner(self) -> "ConstraintVertex":
        native = constraint_owns_get_owner(self.native_object)
        return _ConstraintVertex(native)

    def attribute(self) -> "ConstraintVertex":
        native = constraint_owns_get_attribute(self.native_object)
        return _ConstraintVertex(native)

    def exactness(self) -> "ConstraintExactness":
        return ConstraintExactness(constraint_owns_get_exactness(self.native_object))


class _Relates(_Constraint, Relates):
    def __init__(self, native):
        super().__init__(native)

    def is_relates(self) -> bool:
        return True

    def as_relates(self):
        return self

    def relation(self) -> "ConstraintVertex":
        native = constraint_relates_get_relation(self.native_object)
        return _ConstraintVertex(native)

    def role(self) -> "ConstraintVertex":
        native = constraint_relates_get_role(self.native_object)
        return _ConstraintVertex(native)

    def exactness(self) -> "ConstraintExactness":
        return ConstraintExactness(constraint_relates_get_exactness(self.native_object))


class _Plays(_Constraint, Plays):
    def __init__(self, native):
        super().__init__(native)

    def is_plays(self) -> bool:
        return True

    def as_plays(self):
        return self

    def player(self) -> "ConstraintVertex":
        native = constraint_plays_get_player(self.native_object)
        return _ConstraintVertex(native)

    def role(self) -> "ConstraintVertex":
        native = constraint_plays_get_role(self.native_object)
        return _ConstraintVertex(native)

    def exactness(self) -> "ConstraintExactness":
        return ConstraintExactness(constraint_plays_get_exactness(self.native_object))


class _FunctionCall(_Constraint, FunctionCall):
    def __init__(self, native):
        super().__init__(native)

    def is_function_call(self) -> bool:
        return True

    def as_function_call(self):
        return self

    def name(self) -> str:
        return constraint_function_call_get_name(self.native_object)

    def arguments(self) -> Iterator["ConstraintVertex"]:
        native_iter = constraint_function_call_get_arguments(self.native_object)
        wrapper = IteratorWrapper(native_iter, constraint_vertex_iterator_next)
        return map(lambda n: _ConstraintVertex(n), wrapper)

    def assigned(self) -> Iterator["ConstraintVertex"]:
        native_iter = constraint_function_call_get_assigned(self.native_object)
        wrapper = IteratorWrapper(native_iter, constraint_vertex_iterator_next)
        return map(lambda n: _ConstraintVertex(n), wrapper)


class _Expression(_Constraint, Expression):
    def __init__(self, native):
        super().__init__(native)

    def is_expression(self) -> bool:
        return True

    def as_expression(self):
        return self

    def text(self) -> str:
        return constraint_expression_get_text(self.native_object)

    def arguments(self) -> Iterator["ConstraintVertex"]:
        native_iter = constraint_expression_get_arguments(self.native_object)
        wrapper = IteratorWrapper(native_iter, constraint_vertex_iterator_next)
        return map(lambda n: _ConstraintVertex(n), wrapper)

    def assigned(self) -> "ConstraintVertex":
        native = constraint_expression_get_assigned(self.native_object)
        return _ConstraintVertex(native)


class _Is(_Constraint, Is):
    def __init__(self, native):
        super().__init__(native)

    def is_is(self) -> bool:
        return True

    def as_is(self):
        return self

    def lhs(self) -> "ConstraintVertex":
        native = constraint_is_get_lhs(self.native_object)
        return _ConstraintVertex(native)

    def rhs(self) -> "ConstraintVertex":
        native = constraint_is_get_rhs(self.native_object)
        return _ConstraintVertex(native)


class _Iid(_Constraint, Iid):
    def __init__(self, native):
        super().__init__(native)

    def is_iid(self) -> bool:
        return True

    def as_iid(self):
        return self

    def variable(self) -> "ConstraintVertex":
        native = constraint_iid_get_variable(self.native_object)
        return _ConstraintVertex(native)

    def iid(self) -> str:
        return constraint_iid_get_iid(self.native_object)


class _DeleteConcepts(_Constraint, DeleteConcepts):
    def __init__(self, native):
        super().__init__(native)

    def is_delete_concepts(self) -> bool:
        return True

    def as_delete_concepts(self):
        return self

    def variables(self) -> Iterator["ConstraintVertex"]:
        native_iter = constraint_delete_concepts_get_variables(self.native_object)
        wrapper = IteratorWrapper(native_iter, constraint_vertex_iterator_next)
        return map(lambda n: _ConstraintVertex(n), wrapper)


class _Comparison(_Constraint, Comparison):
    def __init__(self, native):
        super().__init__(native)

    def is_comparison(self) -> bool:
        return True

    def as_comparison(self):
        return self

    def lhs(self) -> "ConstraintVertex":
        native = constraint_comparison_get_lhs(self.native_object)
        return _ConstraintVertex(native)

    def rhs(self) -> "ConstraintVertex":
        native = constraint_comparison_get_rhs(self.native_object)
        return _ConstraintVertex(native)

    def comparator(self) -> "Comparator":
        return Comparator(constraint_comparison_get_comparator(self.native_object))


class _Kind(_Constraint, Kind):
    def __init__(self, native):
        super().__init__(native)

    def is_kind_of(self) -> bool:
        return True

    def as_kind(self):
        return self

    def kind(self) -> "typedb.common.enums.Kind":
        from typedb.common.enums import Kind as KindEnum
        return KindEnum(constraint_kind_get_kind(self.native_object))

    def type(self) -> "ConstraintVertex":
        native = constraint_kind_get_type(self.native_object)
        return _ConstraintVertex(native)


class _Label(_Constraint, Label):
    def __init__(self, native):
        super().__init__(native)

    def is_label(self) -> bool:
        return True

    def as_label(self):
        return self

    def variable(self) -> "ConstraintVertex":
        native = constraint_label_get_variable(self.native_object)
        return _ConstraintVertex(native)

    def label(self) -> str:
        return constraint_label_get_label(self.native_object)


class _Value(_Constraint, Value):
    def __init__(self, native):
        super().__init__(native)

    def is_value(self) -> bool:
        return True

    def as_value(self):
        return self

    def attribute_type(self) -> "ConstraintVertex":
        native = constraint_value_get_attribute_type(self.native_object)
        return _ConstraintVertex(native)

    def value_type(self) -> str:
        return constraint_value_get_value_type(self.native_object)


class _IsSet(_Constraint, IsSet):
    def __init__(self, native):
        super().__init__(native)

    def is_isset(self) -> bool:
        return True

    def as_isset(self):
        return self

    def variables(self) -> Iterator["ConstraintVertex"]:
        native_iter = constraint_isset_get_variables(self.native_object)
        return map(lambda n: _ConstraintVertex(n), IteratorWrapper(native_iter, constraint_vertex_iterator_next))


class _Or(_Constraint, Or):
    def __init__(self, native):
        super().__init__(native)

    def is_or(self) -> bool:
        return True

    def as_or(self):
        return self

    def branches(self) -> Iterator["ConjunctionID"]:
        native_iter = constraint_or_get_branches(self.native_object)
        return map(_ConjunctionID, IteratorWrapper(native_iter, conjunction_id_iterator_next))


class _Not(_Constraint, Not):
    def __init__(self, native):
        super().__init__(native)

    def is_not(self) -> bool:
        return True

    def as_not(self):
        return self

    def conjunction(self) -> "ConjunctionID":
        return _ConjunctionID(constraint_not_get_conjunction(self.native_object))


class _Try(_Constraint, Try):
    def __init__(self, native):
        super().__init__(native)

    def is_try(self) -> bool:
        return True

    def as_try(self):
        return self

    def conjunction(self) -> "ConjunctionID":
        return _ConjunctionID(constraint_try_get_conjunction(self.native_object))
