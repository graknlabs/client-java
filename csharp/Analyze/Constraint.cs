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

using System;
using System.Collections.Generic;
using System.Linq;

using TypeDB.Driver.Api.Analyze;
using TypeDB.Driver.Common;

using AnalyzeError = TypeDB.Driver.Common.Error.Analyze;

namespace TypeDB.Driver.Analyze
{
    public abstract class Constraint : NativeObjectWrapper<Pinvoke.ConstraintWithSpan>, IConstraint
    {
        protected Constraint(Pinvoke.ConstraintWithSpan nativeObject)
            : base(nativeObject)
        {
        }

        public static Constraint Of(Pinvoke.ConstraintWithSpan constraint)
        {
            var variant = Pinvoke.typedb_driver.constraint_variant(constraint);
            switch (variant)
            {
                case Pinvoke.ConstraintVariant.Isa:
                    return new IsaImpl(constraint);
                case Pinvoke.ConstraintVariant.Has:
                    return new HasImpl(constraint);
                case Pinvoke.ConstraintVariant.Links:
                    return new LinksImpl(constraint);
                case Pinvoke.ConstraintVariant.Sub:
                    return new SubImpl(constraint);
                case Pinvoke.ConstraintVariant.Owns:
                    return new OwnsImpl(constraint);
                case Pinvoke.ConstraintVariant.Relates:
                    return new RelatesImpl(constraint);
                case Pinvoke.ConstraintVariant.Plays:
                    return new PlaysImpl(constraint);
                case Pinvoke.ConstraintVariant.FunctionCall:
                    return new FunctionCallImpl(constraint);
                case Pinvoke.ConstraintVariant.Expression:
                    return new ExpressionImpl(constraint);
                case Pinvoke.ConstraintVariant.Is:
                    return new IsImpl(constraint);
                case Pinvoke.ConstraintVariant.Iid:
                    return new IidImpl(constraint);
                case Pinvoke.ConstraintVariant.DeleteConcepts:
                    return new DeleteConceptsImpl(constraint);
                case Pinvoke.ConstraintVariant.Comparison:
                    return new ComparisonImpl(constraint);
                case Pinvoke.ConstraintVariant.KindOf:
                    return new KindImpl(constraint);
                case Pinvoke.ConstraintVariant.Label:
                    return new LabelImpl(constraint);
                case Pinvoke.ConstraintVariant.Value:
                    return new ValueImpl(constraint);
                case Pinvoke.ConstraintVariant.IsSet:
                    return new IsSetImpl(constraint);
                case Pinvoke.ConstraintVariant.Or:
                    return new OrImpl(constraint);
                case Pinvoke.ConstraintVariant.Not:
                    return new NotImpl(constraint);
                case Pinvoke.ConstraintVariant.Try:
                    return new TryImpl(constraint);
                default:
                    throw new InvalidOperationException("Unexpected constraint variant: " + variant);
            }
        }

        public Pinvoke.ConstraintVariant Variant
        {
            get { return Pinvoke.typedb_driver.constraint_variant(NativeObject); }
        }

        public ISpan Span
        {
            get
            {
                return new SpanImpl(
                    Pinvoke.typedb_driver.constraint_span_begin(NativeObject),
                    Pinvoke.typedb_driver.constraint_span_end(NativeObject));
            }
        }

        public virtual bool IsIsa => false;
        public virtual bool IsHas => false;
        public virtual bool IsLinks => false;
        public virtual bool IsSub => false;
        public virtual bool IsOwns => false;
        public virtual bool IsRelates => false;
        public virtual bool IsPlays => false;
        public virtual bool IsFunctionCall => false;
        public virtual bool IsExpression => false;
        public virtual bool IsIs => false;
        public virtual bool IsIid => false;
        public virtual bool IsDeleteConcepts => false;
        public virtual bool IsComparison => false;
        public virtual bool IsKind => false;
        public virtual bool IsLabel => false;
        public virtual bool IsValue => false;
        public virtual bool IsIsSet => false;
        public virtual bool IsOr => false;
        public virtual bool IsNot => false;
        public virtual bool IsTry => false;

        public virtual IIsa AsIsa() => throw InvalidCast("Isa");
        public virtual IHas AsHas() => throw InvalidCast("Has");
        public virtual ILinks AsLinks() => throw InvalidCast("Links");
        public virtual ISub AsSub() => throw InvalidCast("Sub");
        public virtual IOwns AsOwns() => throw InvalidCast("Owns");
        public virtual IRelates AsRelates() => throw InvalidCast("Relates");
        public virtual IPlays AsPlays() => throw InvalidCast("Plays");
        public virtual IFunctionCall AsFunctionCall() => throw InvalidCast("FunctionCall");
        public virtual IExpression AsExpression() => throw InvalidCast("Expression");
        public virtual IIs AsIs() => throw InvalidCast("Is");
        public virtual IIid AsIid() => throw InvalidCast("Iid");
        public virtual IDeleteConcepts AsDeleteConcepts() => throw InvalidCast("DeleteConcepts");
        public virtual IComparison AsComparison() => throw InvalidCast("Comparison");
        public virtual IKind AsKind() => throw InvalidCast("Kind");
        public virtual ILabelConstraint AsLabel() => throw InvalidCast("Label");
        public virtual IValueConstraint AsValue() => throw InvalidCast("Value");
        public virtual IIsSet AsIsSet() => throw InvalidCast("IsSet");
        public virtual IOr AsOr() => throw InvalidCast("Or");
        public virtual INot AsNot() => throw InvalidCast("Not");
        public virtual ITry AsTry() => throw InvalidCast("Try");

        private TypeDBDriverException InvalidCast(string targetType)
        {
            return new TypeDBDriverException(AnalyzeError.INVALID_CONSTRAINT_CASTING, GetType().Name, targetType);
        }

        public override string ToString()
        {
            return Pinvoke.typedb_driver.constraint_string_repr(NativeObject);
        }

        public class SpanImpl : ISpan
        {
            private readonly long _begin;
            private readonly long _end;

            internal SpanImpl(long begin, long end)
            {
                _begin = begin;
                _end = end;
            }

            public long Begin => _begin;
            public long End => _end;
        }

        public class IsaImpl : Constraint, IIsa
        {
            internal IsaImpl(Pinvoke.ConstraintWithSpan nativeObject) : base(nativeObject) { }

            public override bool IsIsa => true;
            public override IIsa AsIsa() => this;

            public IConstraintVertex Instance => new ConstraintVertex(Pinvoke.typedb_driver.constraint_isa_get_instance(NativeObject));
            public IConstraintVertex Type => new ConstraintVertex(Pinvoke.typedb_driver.constraint_isa_get_type(NativeObject));
            public Pinvoke.ConstraintExactness Exactness => Pinvoke.typedb_driver.constraint_isa_get_exactness(NativeObject);
        }

        public class HasImpl : Constraint, IHas
        {
            internal HasImpl(Pinvoke.ConstraintWithSpan nativeObject) : base(nativeObject) { }

            public override bool IsHas => true;
            public override IHas AsHas() => this;

            public IConstraintVertex Owner => new ConstraintVertex(Pinvoke.typedb_driver.constraint_has_get_owner(NativeObject));
            public IConstraintVertex Attribute => new ConstraintVertex(Pinvoke.typedb_driver.constraint_has_get_attribute(NativeObject));
            public Pinvoke.ConstraintExactness Exactness => Pinvoke.typedb_driver.constraint_has_get_exactness(NativeObject);
        }

        public class LinksImpl : Constraint, ILinks
        {
            internal LinksImpl(Pinvoke.ConstraintWithSpan nativeObject) : base(nativeObject) { }

            public override bool IsLinks => true;
            public override ILinks AsLinks() => this;

            public IConstraintVertex Relation => new ConstraintVertex(Pinvoke.typedb_driver.constraint_links_get_relation(NativeObject));
            public IConstraintVertex Player => new ConstraintVertex(Pinvoke.typedb_driver.constraint_links_get_player(NativeObject));
            public IConstraintVertex Role => new ConstraintVertex(Pinvoke.typedb_driver.constraint_links_get_role(NativeObject));
            public Pinvoke.ConstraintExactness Exactness => Pinvoke.typedb_driver.constraint_links_get_exactness(NativeObject);
        }

        public class SubImpl : Constraint, ISub
        {
            internal SubImpl(Pinvoke.ConstraintWithSpan nativeObject) : base(nativeObject) { }

            public override bool IsSub => true;
            public override ISub AsSub() => this;

            public IConstraintVertex Subtype => new ConstraintVertex(Pinvoke.typedb_driver.constraint_sub_get_subtype(NativeObject));
            public IConstraintVertex Supertype => new ConstraintVertex(Pinvoke.typedb_driver.constraint_sub_get_supertype(NativeObject));
            public Pinvoke.ConstraintExactness Exactness => Pinvoke.typedb_driver.constraint_sub_get_exactness(NativeObject);
        }

        public class OwnsImpl : Constraint, IOwns
        {
            internal OwnsImpl(Pinvoke.ConstraintWithSpan nativeObject) : base(nativeObject) { }

            public override bool IsOwns => true;
            public override IOwns AsOwns() => this;

            public IConstraintVertex Owner => new ConstraintVertex(Pinvoke.typedb_driver.constraint_owns_get_owner(NativeObject));
            public IConstraintVertex Attribute => new ConstraintVertex(Pinvoke.typedb_driver.constraint_owns_get_attribute(NativeObject));
            public Pinvoke.ConstraintExactness Exactness => Pinvoke.typedb_driver.constraint_owns_get_exactness(NativeObject);
        }

        public class RelatesImpl : Constraint, IRelates
        {
            internal RelatesImpl(Pinvoke.ConstraintWithSpan nativeObject) : base(nativeObject) { }

            public override bool IsRelates => true;
            public override IRelates AsRelates() => this;

            public IConstraintVertex Relation => new ConstraintVertex(Pinvoke.typedb_driver.constraint_relates_get_relation(NativeObject));
            public IConstraintVertex Role => new ConstraintVertex(Pinvoke.typedb_driver.constraint_relates_get_role(NativeObject));
            public Pinvoke.ConstraintExactness Exactness => Pinvoke.typedb_driver.constraint_relates_get_exactness(NativeObject);
        }

        public class PlaysImpl : Constraint, IPlays
        {
            internal PlaysImpl(Pinvoke.ConstraintWithSpan nativeObject) : base(nativeObject) { }

            public override bool IsPlays => true;
            public override IPlays AsPlays() => this;

            public IConstraintVertex Player => new ConstraintVertex(Pinvoke.typedb_driver.constraint_plays_get_player(NativeObject));
            public IConstraintVertex Role => new ConstraintVertex(Pinvoke.typedb_driver.constraint_plays_get_role(NativeObject));
            public Pinvoke.ConstraintExactness Exactness => Pinvoke.typedb_driver.constraint_plays_get_exactness(NativeObject);
        }

        public class FunctionCallImpl : Constraint, IFunctionCall
        {
            internal FunctionCallImpl(Pinvoke.ConstraintWithSpan nativeObject) : base(nativeObject) { }

            public override bool IsFunctionCall => true;
            public override IFunctionCall AsFunctionCall() => this;

            public string Name => Pinvoke.typedb_driver.constraint_function_call_get_name(NativeObject);

            public IEnumerable<IConstraintVertex> Arguments =>
                new NativeEnumerable<Pinvoke.ConstraintVertex>(
                    Pinvoke.typedb_driver.constraint_function_call_get_arguments(NativeObject))
                    .Select(v => new ConstraintVertex(v));

            public IEnumerable<IConstraintVertex> Assigned =>
                new NativeEnumerable<Pinvoke.ConstraintVertex>(
                    Pinvoke.typedb_driver.constraint_function_call_get_assigned(NativeObject))
                    .Select(v => new ConstraintVertex(v));
        }

        public class ExpressionImpl : Constraint, IExpression
        {
            internal ExpressionImpl(Pinvoke.ConstraintWithSpan nativeObject) : base(nativeObject) { }

            public override bool IsExpression => true;
            public override IExpression AsExpression() => this;

            public string Text => Pinvoke.typedb_driver.constraint_expression_get_text(NativeObject);

            public IEnumerable<IConstraintVertex> Arguments =>
                new NativeEnumerable<Pinvoke.ConstraintVertex>(
                    Pinvoke.typedb_driver.constraint_expression_get_arguments(NativeObject))
                    .Select(v => new ConstraintVertex(v));

            public IConstraintVertex Assigned =>
                new ConstraintVertex(Pinvoke.typedb_driver.constraint_expression_get_assigned(NativeObject));
        }

        public class IsImpl : Constraint, IIs
        {
            internal IsImpl(Pinvoke.ConstraintWithSpan nativeObject) : base(nativeObject) { }

            public override bool IsIs => true;
            public override IIs AsIs() => this;

            public IConstraintVertex Lhs => new ConstraintVertex(Pinvoke.typedb_driver.constraint_is_get_lhs(NativeObject));
            public IConstraintVertex Rhs => new ConstraintVertex(Pinvoke.typedb_driver.constraint_is_get_rhs(NativeObject));
        }

        public class IidImpl : Constraint, IIid
        {
            internal IidImpl(Pinvoke.ConstraintWithSpan nativeObject) : base(nativeObject) { }

            public override bool IsIid => true;
            public override IIid AsIid() => this;

            public IConstraintVertex Variable => new ConstraintVertex(Pinvoke.typedb_driver.constraint_iid_get_variable(NativeObject));
            public string Iid => Pinvoke.typedb_driver.constraint_iid_get_iid(NativeObject);
        }

        public class DeleteConceptsImpl : Constraint, IDeleteConcepts
        {
            internal DeleteConceptsImpl(Pinvoke.ConstraintWithSpan nativeObject) : base(nativeObject) { }

            public override bool IsDeleteConcepts => true;
            public override IDeleteConcepts AsDeleteConcepts() => this;

            public IEnumerable<IConstraintVertex> Variables =>
                new NativeEnumerable<Pinvoke.ConstraintVertex>(
                    Pinvoke.typedb_driver.constraint_delete_concepts_get_variables(NativeObject))
                    .Select(v => new ConstraintVertex(v));
        }

        public class ComparisonImpl : Constraint, IComparison
        {
            internal ComparisonImpl(Pinvoke.ConstraintWithSpan nativeObject) : base(nativeObject) { }

            public override bool IsComparison => true;
            public override IComparison AsComparison() => this;

            public IConstraintVertex Lhs => new ConstraintVertex(Pinvoke.typedb_driver.constraint_comparison_get_lhs(NativeObject));
            public IConstraintVertex Rhs => new ConstraintVertex(Pinvoke.typedb_driver.constraint_comparison_get_rhs(NativeObject));
            public Pinvoke.Comparator Comparator => Pinvoke.typedb_driver.constraint_comparison_get_comparator(NativeObject);
            public string ComparatorSymbol => Pinvoke.typedb_driver.comparator_get_name(Comparator);
        }

        public class KindImpl : Constraint, IKind
        {
            internal KindImpl(Pinvoke.ConstraintWithSpan nativeObject) : base(nativeObject) { }

            public override bool IsKind => true;
            public override IKind AsKind() => this;

            public Pinvoke.Kind Kind => Pinvoke.typedb_driver.constraint_kind_get_kind(NativeObject);
            public IConstraintVertex Type => new ConstraintVertex(Pinvoke.typedb_driver.constraint_kind_get_type(NativeObject));
        }

        public class LabelImpl : Constraint, ILabelConstraint
        {
            internal LabelImpl(Pinvoke.ConstraintWithSpan nativeObject) : base(nativeObject) { }

            public override bool IsLabel => true;
            public override ILabelConstraint AsLabel() => this;

            public IConstraintVertex Variable => new ConstraintVertex(Pinvoke.typedb_driver.constraint_label_get_variable(NativeObject));
            public string LabelValue => Pinvoke.typedb_driver.constraint_label_get_label(NativeObject);
        }

        public class ValueImpl : Constraint, IValueConstraint
        {
            internal ValueImpl(Pinvoke.ConstraintWithSpan nativeObject) : base(nativeObject) { }

            public override bool IsValue => true;
            public override IValueConstraint AsValue() => this;

            public IConstraintVertex AttributeType => new ConstraintVertex(Pinvoke.typedb_driver.constraint_value_get_attribute_type(NativeObject));
            public string ValueType => Pinvoke.typedb_driver.constraint_value_get_value_type(NativeObject);
        }

        public class IsSetImpl : Constraint, IIsSet
        {
            internal IsSetImpl(Pinvoke.ConstraintWithSpan nativeObject) : base(nativeObject) { }

            public override bool IsIsSet => true;
            public override IIsSet AsIsSet() => this;

            public IEnumerable<IConstraintVertex> Variables =>
                new NativeEnumerable<Pinvoke.ConstraintVertex>(
                    Pinvoke.typedb_driver.constraint_isset_get_variables(NativeObject))
                    .Select(v => new ConstraintVertex(v));
        }

        public class OrImpl : Constraint, IOr
        {
            internal OrImpl(Pinvoke.ConstraintWithSpan nativeObject) : base(nativeObject) { }

            public override bool IsOr => true;
            public override IOr AsOr() => this;

            public IEnumerable<IConjunctionID> Branches =>
                new NativeEnumerable<Pinvoke.ConjunctionID>(
                    Pinvoke.typedb_driver.constraint_or_get_branches(NativeObject))
                    .Select(c => new ConjunctionID(c));
        }

        public class NotImpl : Constraint, INot
        {
            internal NotImpl(Pinvoke.ConstraintWithSpan nativeObject) : base(nativeObject) { }

            public override bool IsNot => true;
            public override INot AsNot() => this;

            public IConjunctionID Conjunction =>
                new ConjunctionID(Pinvoke.typedb_driver.constraint_not_get_conjunction(NativeObject));
        }

        public class TryImpl : Constraint, ITry
        {
            internal TryImpl(Pinvoke.ConstraintWithSpan nativeObject) : base(nativeObject) { }

            public override bool IsTry => true;
            public override ITry AsTry() => this;

            public IConjunctionID Conjunction =>
                new ConjunctionID(Pinvoke.typedb_driver.constraint_try_get_conjunction(NativeObject));
        }
    }
}
