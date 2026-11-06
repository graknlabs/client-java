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

from abc import ABC, abstractmethod
from typing import TYPE_CHECKING, Iterator

if TYPE_CHECKING:
    from typedb.api.analyze.constraint_vertex import ConstraintVertex
    from typedb.api.analyze.conjunction_id import ConjunctionID
    from typedb.common.enums import Comparator, ConstraintExactness
    import typedb


class Constraint(ABC):
    """
    A representation of a TypeQL constraint.
    """

    @abstractmethod
    def span(self) -> "Span":
        """Gets the span of this constraint in the source query."""
        pass

    @abstractmethod
    def is_isa(self) -> bool:
        pass

    @abstractmethod
    def is_has(self) -> bool:
        pass

    @abstractmethod
    def is_links(self) -> bool:
        pass

    @abstractmethod
    def is_sub(self) -> bool:
        pass

    @abstractmethod
    def is_owns(self) -> bool:
        pass

    @abstractmethod
    def is_relates(self) -> bool:
        pass

    @abstractmethod
    def is_plays(self) -> bool:
        pass

    @abstractmethod
    def is_function_call(self) -> bool:
        pass

    @abstractmethod
    def is_expression(self) -> bool:
        pass

    @abstractmethod
    def is_is(self) -> bool:
        pass

    @abstractmethod
    def is_iid(self) -> bool:
        pass

    @abstractmethod
    def is_delete_concepts(self) -> bool:
        pass

    @abstractmethod
    def is_comparison(self) -> bool:
        pass

    @abstractmethod
    def is_kind_of(self) -> bool:
        pass

    @abstractmethod
    def is_label(self) -> bool:
        pass

    @abstractmethod
    def is_value(self) -> bool:
        pass

    @abstractmethod
    def is_isset(self) -> bool:
        pass

    @abstractmethod
    def is_or(self) -> bool:
        pass

    @abstractmethod
    def is_not(self) -> bool:
        pass

    @abstractmethod
    def is_try(self) -> bool:
        pass

    @abstractmethod
    def as_isa(self) -> "Isa":
        pass

    @abstractmethod
    def as_has(self) -> "Has":
        pass

    @abstractmethod
    def as_links(self) -> "Links":
        pass

    @abstractmethod
    def as_sub(self) -> "Sub":
        pass

    @abstractmethod
    def as_owns(self) -> "Owns":
        pass

    @abstractmethod
    def as_relates(self) -> "Relates":
        pass

    @abstractmethod
    def as_plays(self) -> "Plays":
        pass

    @abstractmethod
    def as_function_call(self) -> "FunctionCall":
        pass

    @abstractmethod
    def as_expression(self) -> "Expression":
        pass

    @abstractmethod
    def as_is(self) -> "Is":
        pass

    @abstractmethod
    def as_iid(self) -> "Iid":
        pass

    @abstractmethod
    def as_delete_concepts(self) -> "DeleteConcepts":
        pass

    @abstractmethod
    def as_comparison(self) -> "Comparison":
        pass

    @abstractmethod
    def as_kind(self) -> "Kind":
        pass

    @abstractmethod
    def as_label(self) -> "Label":
        pass

    @abstractmethod
    def as_value(self) -> "Value":
        pass

    @abstractmethod
    def as_isset(self) -> "IsSet":
        pass

    @abstractmethod
    def as_or(self) -> "Or":
        pass

    @abstractmethod
    def as_not(self) -> "Not":
        pass

    @abstractmethod
    def as_try(self) -> "Try":
        pass


class Isa(Constraint, ABC):
    """Represents an 'isa' constraint: <instance> isa(!) <type>"""

    @abstractmethod
    def instance(self) -> "ConstraintVertex":
        pass

    @abstractmethod
    def type(self) -> "ConstraintVertex":
        pass

    @abstractmethod
    def exactness(self) -> "ConstraintExactness":
        pass


class Has(Constraint, ABC):
    """Represents a 'has' constraint: <owner> has <attribute>"""

    @abstractmethod
    def owner(self) -> "ConstraintVertex":
        pass

    @abstractmethod
    def attribute(self) -> "ConstraintVertex":
        pass

    @abstractmethod
    def exactness(self) -> "ConstraintExactness":
        pass


class Links(Constraint, ABC):
    """Represents a 'links' constraint: <relation> links (<role>: <player>)"""

    @abstractmethod
    def relation(self) -> "ConstraintVertex":
        pass

    @abstractmethod
    def player(self) -> "ConstraintVertex":
        pass

    @abstractmethod
    def role(self) -> "ConstraintVertex":
        pass

    @abstractmethod
    def exactness(self) -> "ConstraintExactness":
        pass


class Sub(Constraint, ABC):
    """Represents a 'sub' constraint: <subtype> sub(!) <supertype>"""

    @abstractmethod
    def subtype(self) -> "ConstraintVertex":
        pass

    @abstractmethod
    def supertype(self) -> "ConstraintVertex":
        pass

    @abstractmethod
    def exactness(self) -> "ConstraintExactness":
        pass


class Owns(Constraint, ABC):
    """Represents an 'owns' constraint: <owner> owns <attribute>"""

    @abstractmethod
    def owner(self) -> "ConstraintVertex":
        pass

    @abstractmethod
    def attribute(self) -> "ConstraintVertex":
        pass

    @abstractmethod
    def exactness(self) -> "ConstraintExactness":
        pass


class Relates(Constraint, ABC):
    """Represents a 'relates' constraint: <relation> relates <role>"""

    @abstractmethod
    def relation(self) -> "ConstraintVertex":
        pass

    @abstractmethod
    def role(self) -> "ConstraintVertex":
        pass

    @abstractmethod
    def exactness(self) -> "ConstraintExactness":
        pass


class Plays(Constraint, ABC):
    """Represents a 'plays' constraint: <player> plays <role>"""

    @abstractmethod
    def player(self) -> "ConstraintVertex":
        pass

    @abstractmethod
    def role(self) -> "ConstraintVertex":
        pass

    @abstractmethod
    def exactness(self) -> "ConstraintExactness":
        pass


class FunctionCall(Constraint, ABC):
    """Represents a function call: let <assigned> = name(<arguments>)"""

    @abstractmethod
    def name(self) -> str:
        pass

    @abstractmethod
    def arguments(self) -> Iterator["ConstraintVertex"]:
        pass

    @abstractmethod
    def assigned(self) -> Iterator["ConstraintVertex"]:
        pass


class Expression(Constraint, ABC):
    """Represents an expression: let <assigned> = <expression>"""

    @abstractmethod
    def text(self) -> str:
        pass

    @abstractmethod
    def arguments(self) -> Iterator["ConstraintVertex"]:
        pass

    @abstractmethod
    def assigned(self) -> "ConstraintVertex":
        pass


class Is(Constraint, ABC):
    """Represents an 'is' constraint: <lhs> is <rhs>"""

    @abstractmethod
    def lhs(self) -> "ConstraintVertex":
        pass

    @abstractmethod
    def rhs(self) -> "ConstraintVertex":
        pass


class Iid(Constraint, ABC):
    """Represents an IID constraint: <concept> iid <iid>"""

    @abstractmethod
    def variable(self) -> "ConstraintVertex":
        pass

    @abstractmethod
    def iid(self) -> str:
        pass


class DeleteConcepts(Constraint, ABC):
    """Represents the deletion of concepts bound to a set of variables: delete <variables>"""

    @abstractmethod
    def variables(self) -> Iterator["ConstraintVertex"]:
        pass


class Comparison(Constraint, ABC):
    """Represents a comparison: <lhs> <comparator> <rhs>"""

    @abstractmethod
    def lhs(self) -> "ConstraintVertex":
        pass

    @abstractmethod
    def rhs(self) -> "ConstraintVertex":
        pass

    @abstractmethod
    def comparator(self) -> "Comparator":
        pass


class Kind(Constraint, ABC):
    """Represents a kind constraint: <kind> <type>"""

    @abstractmethod
    def kind(self) -> "typedb.common.enums.Kind":
        pass

    @abstractmethod
    def type(self) -> "ConstraintVertex":
        pass


class Label(Constraint, ABC):
    """Represents a label constraint: <type> label <label>"""

    @abstractmethod
    def variable(self) -> "ConstraintVertex":
        pass

    @abstractmethod
    def label(self) -> str:
        pass


class Value(Constraint, ABC):
    """Represents a value constraint: <attribute_type> value <value_type>"""

    @abstractmethod
    def attribute_type(self) -> "ConstraintVertex":
        pass

    @abstractmethod
    def value_type(self) -> str:
        pass


class IsSet(Constraint, ABC):
    """Represents an isset constraint: isset <variables>"""

    @abstractmethod
    def variables(self) -> Iterator["ConstraintVertex"]:
        pass


class Or(Constraint, ABC):
    """Represents an 'or' constraint: { <branches[0]> } or { <branches[1]> } [or ...]"""

    @abstractmethod
    def branches(self) -> Iterator["ConjunctionID"]:
        """
        Index into Pipeline.conjunctions
        """
        pass


class Not(Constraint, ABC):
    """Represents a 'not' constraint: not { <conjunction> }"""

    @abstractmethod
    def conjunction(self) -> "ConjunctionID":
        """
        Index into Pipeline.conjunctions
        """
        pass


class Try(Constraint, ABC):
    """Represents a 'try' constraint: try { <conjunction> }"""

    @abstractmethod
    def conjunction(self) -> "ConjunctionID":
        """
        Index into Pipeline.conjunctions
        """
        pass


class Span(ABC):
    """The span of a constraint in the source query."""

    @abstractmethod
    def begin(self) -> int:
        """The offset of the first character."""
        pass

    @abstractmethod
    def end(self) -> int:
        """The offset after the last character."""
        pass
