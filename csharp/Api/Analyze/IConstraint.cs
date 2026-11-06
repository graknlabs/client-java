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

using System.Collections.Generic;

namespace TypeDB.Driver.Api.Analyze
{
    /// <summary>
    /// A representation of a TypeQL constraint.
    /// </summary>
    public interface IConstraint
    {
        /// <summary>
        /// Gets the variant of this constraint.
        /// </summary>
        Pinvoke.ConstraintVariant Variant { get; }

        /// <summary>
        /// Gets the span of this constraint in the source query.
        /// </summary>
        ISpan Span { get; }

        /// <summary>
        /// Checks if this constraint is an isa constraint.
        /// </summary>
        bool IsIsa { get; }

        /// <summary>
        /// Checks if this constraint is a has constraint.
        /// </summary>
        bool IsHas { get; }

        /// <summary>
        /// Checks if this constraint is a links constraint.
        /// </summary>
        bool IsLinks { get; }

        /// <summary>
        /// Checks if this constraint is a sub constraint.
        /// </summary>
        bool IsSub { get; }

        /// <summary>
        /// Checks if this constraint is an owns constraint.
        /// </summary>
        bool IsOwns { get; }

        /// <summary>
        /// Checks if this constraint is a relates constraint.
        /// </summary>
        bool IsRelates { get; }

        /// <summary>
        /// Checks if this constraint is a plays constraint.
        /// </summary>
        bool IsPlays { get; }

        /// <summary>
        /// Checks if this constraint is a function call constraint.
        /// </summary>
        bool IsFunctionCall { get; }

        /// <summary>
        /// Checks if this constraint is an expression constraint.
        /// </summary>
        bool IsExpression { get; }

        /// <summary>
        /// Checks if this constraint is an is constraint.
        /// </summary>
        bool IsIs { get; }

        /// <summary>
        /// Checks if this constraint is an iid constraint.
        /// </summary>
        bool IsIid { get; }

        /// <summary>
        /// Checks if this constraint is a delete concepts constraint.
        /// </summary>
        bool IsDeleteConcepts { get; }

        /// <summary>
        /// Checks if this constraint is a comparison constraint.
        /// </summary>
        bool IsComparison { get; }

        /// <summary>
        /// Checks if this constraint is a kind constraint.
        /// </summary>
        bool IsKind { get; }

        /// <summary>
        /// Checks if this constraint is a label constraint.
        /// </summary>
        bool IsLabel { get; }

        /// <summary>
        /// Checks if this constraint is a value constraint.
        /// </summary>
        bool IsValue { get; }

        /// <summary>
        /// Checks if this constraint is an isset constraint.
        /// </summary>
        bool IsIsSet { get; }

        /// <summary>
        /// Checks if this constraint is an or constraint.
        /// </summary>
        bool IsOr { get; }

        /// <summary>
        /// Checks if this constraint is a not constraint.
        /// </summary>
        bool IsNot { get; }

        /// <summary>
        /// Checks if this constraint is a try constraint.
        /// </summary>
        bool IsTry { get; }

        /// <summary>
        /// Casts this constraint to an isa constraint.
        /// </summary>
        IIsa AsIsa();

        /// <summary>
        /// Casts this constraint to a has constraint.
        /// </summary>
        IHas AsHas();

        /// <summary>
        /// Casts this constraint to a links constraint.
        /// </summary>
        ILinks AsLinks();

        /// <summary>
        /// Casts this constraint to a sub constraint.
        /// </summary>
        ISub AsSub();

        /// <summary>
        /// Casts this constraint to an owns constraint.
        /// </summary>
        IOwns AsOwns();

        /// <summary>
        /// Casts this constraint to a relates constraint.
        /// </summary>
        IRelates AsRelates();

        /// <summary>
        /// Casts this constraint to a plays constraint.
        /// </summary>
        IPlays AsPlays();

        /// <summary>
        /// Casts this constraint to a function call constraint.
        /// </summary>
        IFunctionCall AsFunctionCall();

        /// <summary>
        /// Casts this constraint to an expression constraint.
        /// </summary>
        IExpression AsExpression();

        /// <summary>
        /// Casts this constraint to an is constraint.
        /// </summary>
        IIs AsIs();

        /// <summary>
        /// Casts this constraint to an iid constraint.
        /// </summary>
        IIid AsIid();

        /// <summary>
        /// Casts this constraint to a delete concepts constraint.
        /// </summary>
        IDeleteConcepts AsDeleteConcepts();

        /// <summary>
        /// Casts this constraint to a comparison constraint.
        /// </summary>
        IComparison AsComparison();

        /// <summary>
        /// Casts this constraint to a kind constraint.
        /// </summary>
        IKind AsKind();

        /// <summary>
        /// Casts this constraint to a label constraint.
        /// </summary>
        ILabelConstraint AsLabel();

        /// <summary>
        /// Casts this constraint to a value constraint.
        /// </summary>
        IValueConstraint AsValue();

        /// <summary>
        /// Casts this constraint to an isset constraint.
        /// </summary>
        IIsSet AsIsSet();

        /// <summary>
        /// Casts this constraint to an or constraint.
        /// </summary>
        IOr AsOr();

        /// <summary>
        /// Casts this constraint to a not constraint.
        /// </summary>
        INot AsNot();

        /// <summary>
        /// Casts this constraint to a try constraint.
        /// </summary>
        ITry AsTry();
    }

    /// <summary>
    /// The span of a constraint in the source query.
    /// </summary>
    public interface ISpan
    {
        /// <summary>
        /// Gets the offset of the first character.
        /// </summary>
        long Begin { get; }

        /// <summary>
        /// Gets the offset after the last character.
        /// </summary>
        long End { get; }
    }

    /// <summary>
    /// Represents an "isa" constraint: instance isa(!) type
    /// </summary>
    public interface IIsa : IConstraint
    {
        /// <summary>
        /// The instance vertex of the constraint.
        /// </summary>
        IConstraintVertex Instance { get; }

        /// <summary>
        /// The type vertex of the constraint.
        /// </summary>
        IConstraintVertex Type { get; }

        /// <summary>
        /// The exactness of the constraint.
        /// </summary>
        Pinvoke.ConstraintExactness Exactness { get; }
    }

    /// <summary>
    /// Represents a "has" constraint: owner has attribute
    /// </summary>
    public interface IHas : IConstraint
    {
        /// <summary>
        /// The owner vertex of the constraint.
        /// </summary>
        IConstraintVertex Owner { get; }

        /// <summary>
        /// The attribute vertex of the constraint.
        /// </summary>
        IConstraintVertex Attribute { get; }

        /// <summary>
        /// The exactness of the constraint.
        /// </summary>
        Pinvoke.ConstraintExactness Exactness { get; }
    }

    /// <summary>
    /// Represents a "links" constraint: relation links (role: player)
    /// </summary>
    public interface ILinks : IConstraint
    {
        /// <summary>
        /// The relation vertex of the constraint.
        /// </summary>
        IConstraintVertex Relation { get; }

        /// <summary>
        /// The player vertex of the constraint.
        /// </summary>
        IConstraintVertex Player { get; }

        /// <summary>
        /// The role vertex of the constraint.
        /// </summary>
        IConstraintVertex Role { get; }

        /// <summary>
        /// The exactness of the constraint.
        /// </summary>
        Pinvoke.ConstraintExactness Exactness { get; }
    }

    /// <summary>
    /// Represents a "sub" constraint: subtype sub(!) supertype
    /// </summary>
    public interface ISub : IConstraint
    {
        /// <summary>
        /// The subtype vertex of the constraint.
        /// </summary>
        IConstraintVertex Subtype { get; }

        /// <summary>
        /// The supertype vertex of the constraint.
        /// </summary>
        IConstraintVertex Supertype { get; }

        /// <summary>
        /// The exactness of the constraint.
        /// </summary>
        Pinvoke.ConstraintExactness Exactness { get; }
    }

    /// <summary>
    /// Represents an "owns" constraint: owner owns attribute
    /// </summary>
    public interface IOwns : IConstraint
    {
        /// <summary>
        /// The owner vertex of the constraint.
        /// </summary>
        IConstraintVertex Owner { get; }

        /// <summary>
        /// The attribute vertex of the constraint.
        /// </summary>
        IConstraintVertex Attribute { get; }

        /// <summary>
        /// The exactness of the constraint.
        /// </summary>
        Pinvoke.ConstraintExactness Exactness { get; }
    }

    /// <summary>
    /// Represents a "relates" constraint: relation relates role
    /// </summary>
    public interface IRelates : IConstraint
    {
        /// <summary>
        /// The relation vertex of the constraint.
        /// </summary>
        IConstraintVertex Relation { get; }

        /// <summary>
        /// The role vertex of the constraint.
        /// </summary>
        IConstraintVertex Role { get; }

        /// <summary>
        /// The exactness of the constraint.
        /// </summary>
        Pinvoke.ConstraintExactness Exactness { get; }
    }

    /// <summary>
    /// Represents a "plays" constraint: player plays role
    /// </summary>
    public interface IPlays : IConstraint
    {
        /// <summary>
        /// The player vertex of the constraint.
        /// </summary>
        IConstraintVertex Player { get; }

        /// <summary>
        /// The role vertex of the constraint.
        /// </summary>
        IConstraintVertex Role { get; }

        /// <summary>
        /// The exactness of the constraint.
        /// </summary>
        Pinvoke.ConstraintExactness Exactness { get; }
    }

    /// <summary>
    /// Represents a function call: let assigned = name(arguments)
    /// </summary>
    public interface IFunctionCall : IConstraint
    {
        /// <summary>
        /// The name of the function being called.
        /// </summary>
        string Name { get; }

        /// <summary>
        /// The arguments to the function call.
        /// </summary>
        IEnumerable<IConstraintVertex> Arguments { get; }

        /// <summary>
        /// The variables being assigned to.
        /// </summary>
        IEnumerable<IConstraintVertex> Assigned { get; }
    }

    /// <summary>
    /// Represents an expression: let assigned = expression
    /// </summary>
    public interface IExpression : IConstraint
    {
        /// <summary>
        /// The text of the expression.
        /// </summary>
        string Text { get; }

        /// <summary>
        /// The arguments to the expression.
        /// </summary>
        IEnumerable<IConstraintVertex> Arguments { get; }

        /// <summary>
        /// The variable being assigned to.
        /// </summary>
        IConstraintVertex Assigned { get; }
    }

    /// <summary>
    /// Represents an "is" constraint: lhs is rhs
    /// </summary>
    public interface IIs : IConstraint
    {
        /// <summary>
        /// The left-hand side of the constraint.
        /// </summary>
        IConstraintVertex Lhs { get; }

        /// <summary>
        /// The right-hand side of the constraint.
        /// </summary>
        IConstraintVertex Rhs { get; }
    }

    /// <summary>
    /// Represents an IID constraint: concept iid iid_value
    /// </summary>
    public interface IIid : IConstraint
    {
        /// <summary>
        /// The variable being constrained.
        /// </summary>
        IConstraintVertex Variable { get; }

        /// <summary>
        /// The internal identifier value.
        /// </summary>
        string Iid { get; }
    }

    /// <summary>
    /// Represents the deletion of concepts bound to a set of variables: delete variables
    /// </summary>
    public interface IDeleteConcepts : IConstraint
    {
        /// <summary>
        /// The variables whose concepts are deleted.
        /// </summary>
        IEnumerable<IConstraintVertex> Variables { get; }
    }

    /// <summary>
    /// Represents a comparison: lhs comparator rhs
    /// </summary>
    public interface IComparison : IConstraint
    {
        /// <summary>
        /// The left-hand side of the comparison.
        /// </summary>
        IConstraintVertex Lhs { get; }

        /// <summary>
        /// The right-hand side of the comparison.
        /// </summary>
        IConstraintVertex Rhs { get; }

        /// <summary>
        /// The comparator operator.
        /// </summary>
        Pinvoke.Comparator Comparator { get; }

        /// <summary>
        /// The symbol representation of the comparator.
        /// </summary>
        string ComparatorSymbol { get; }
    }

    /// <summary>
    /// Represents a kind constraint: kind type
    /// </summary>
    public interface IKind : IConstraint
    {
        /// <summary>
        /// The kind of the type.
        /// </summary>
        Pinvoke.Kind Kind { get; }

        /// <summary>
        /// The type vertex of the constraint.
        /// </summary>
        IConstraintVertex Type { get; }
    }

    /// <summary>
    /// Represents a label constraint: type label label_value
    /// </summary>
    public interface ILabelConstraint : IConstraint
    {
        /// <summary>
        /// The variable being constrained.
        /// </summary>
        IConstraintVertex Variable { get; }

        /// <summary>
        /// The label value.
        /// </summary>
        string LabelValue { get; }
    }

    /// <summary>
    /// Represents a value constraint: attribute_type value value_type
    /// </summary>
    public interface IValueConstraint : IConstraint
    {
        /// <summary>
        /// The attribute type vertex of the constraint.
        /// </summary>
        IConstraintVertex AttributeType { get; }

        /// <summary>
        /// The value type.
        /// </summary>
        string ValueType { get; }
    }

    /// <summary>
    /// Represents an isset constraint: isset variables
    /// </summary>
    public interface IIsSet : IConstraint
    {
        /// <summary>
        /// The variables being checked.
        /// </summary>
        IEnumerable<IConstraintVertex> Variables { get; }
    }

    /// <summary>
    /// Represents an "or" constraint: { branches[0] } or { branches[1] } [or ...]
    /// </summary>
    public interface IOr : IConstraint
    {
        /// <summary>
        /// The conjunction branches, as indices into the pipeline's conjunctions.
        /// </summary>
        IEnumerable<IConjunctionID> Branches { get; }
    }

    /// <summary>
    /// Represents a "not" constraint: not { conjunction }
    /// </summary>
    public interface INot : IConstraint
    {
        /// <summary>
        /// The index into the pipeline's conjunctions.
        /// </summary>
        IConjunctionID Conjunction { get; }
    }

    /// <summary>
    /// Represents a "try" constraint: try { conjunction }
    /// </summary>
    public interface ITry : IConstraint
    {
        /// <summary>
        /// The index into the pipeline's conjunctions.
        /// </summary>
        IConjunctionID Conjunction { get; }
    }
}
