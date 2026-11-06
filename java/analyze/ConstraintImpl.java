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

package com.typedb.driver.analyze;

import com.typedb.driver.api.analyze.Constraint;
import com.typedb.driver.common.NativeIterator;
import com.typedb.driver.common.NativeObject;
import com.typedb.driver.common.exception.TypeDBDriverException;
import com.typedb.driver.jni.typedb_driver;

import java.util.stream.Stream;

import static com.typedb.driver.common.exception.ErrorMessage.Analyze.INVALID_CONSTRAINT_CASTING;
import static com.typedb.driver.common.util.Objects.className;

public abstract class ConstraintImpl extends NativeObject<com.typedb.driver.jni.ConstraintWithSpan> implements Constraint {
    public ConstraintImpl(com.typedb.driver.jni.ConstraintWithSpan nativeObject) {
        super(nativeObject);
    }

    public static ConstraintImpl of(com.typedb.driver.jni.ConstraintWithSpan constraint) {
        com.typedb.driver.jni.ConstraintVariant variant =
                com.typedb.driver.jni.typedb_driver.constraint_variant(constraint);
        switch (variant) {
            case Isa:
                return new IsaImpl(constraint);
            case Has:
                return new HasImpl(constraint);
            case Links:
                return new LinksImpl(constraint);
            case Sub:
                return new SubImpl(constraint);
            case Owns:
                return new OwnsImpl(constraint);
            case Relates:
                return new RelatesImpl(constraint);
            case Plays:
                return new PlaysImpl(constraint);
            case FunctionCall:
                return new FunctionCallImpl(constraint);
            case Expression:
                return new ExpressionImpl(constraint);
            case Is:
                return new IsImpl(constraint);
            case Iid:
                return new IidImpl(constraint);
            case DeleteConcepts:
                return new DeleteConceptsImpl(constraint);
            case Comparison:
                return new ComparisonImpl(constraint);
            case KindOf:
                return new KindImpl(constraint);
            case Label:
                return new LabelImpl(constraint);
            case Value:
                return new ValueImpl(constraint);
            case IsSet:
                return new IsSetImpl(constraint);
            case Or:
                return new OrImpl(constraint);
            case Not:
                return new NotImpl(constraint);
            case Try:
                return new TryImpl(constraint);
            default:
                throw new IllegalStateException("Unexpected constraint variant: " + variant);
        }
    }

    public SpanImpl span() {
        return new SpanImpl(
                com.typedb.driver.jni.typedb_driver.constraint_span_begin(nativeObject),
                com.typedb.driver.jni.typedb_driver.constraint_span_end(nativeObject)
        );
    }

    public static class SpanImpl implements Span {
        long begin;
        long end;

        SpanImpl(long begin, long end) {
            this.begin = begin;
            this.end = end;
        }

        @Override
        public long begin() {
            return begin;
        }

        @Override
        public long end() {
            return end;
        }
    }

    public com.typedb.driver.jni.ConstraintVariant variant() {
        return com.typedb.driver.jni.typedb_driver.constraint_variant(nativeObject);
    }

    public boolean isIsa() {
        return false;
    }

    public boolean isHas() {
        return false;
    }

    public boolean isLinks() {
        return false;
    }

    public boolean isSub() {
        return false;
    }

    public boolean isOwns() {
        return false;
    }

    public boolean isRelates() {
        return false;
    }

    public boolean isPlays() {
        return false;
    }

    public boolean isFunctionCall() {
        return false;
    }

    public boolean isExpression() {
        return false;
    }

    public boolean isIs() {
        return false;
    }

    public boolean isIid() {
        return false;
    }

    public boolean isDeleteConcepts() {
        return false;
    }

    public boolean isComparison() {
        return false;
    }

    public boolean isKind() {
        return false;
    }

    public boolean isLabel() {
        return false;
    }

    public boolean isValue() {
        return false;
    }

    public boolean isIsSet() {
        return false;
    }

    public boolean isOr() {
        return false;
    }

    public boolean isNot() {
        return false;
    }

    public boolean isTry() {
        return false;
    }

    @Override
    public String toString() {
        return typedb_driver.constraint_string_repr(nativeObject);
    }

    public IsaImpl asIsa() {
        throw new TypeDBDriverException(INVALID_CONSTRAINT_CASTING, className(this.getClass()), className(IsaImpl.class));
    }

    public HasImpl asHas() {
        throw new TypeDBDriverException(INVALID_CONSTRAINT_CASTING, className(this.getClass()), className(HasImpl.class));
    }

    public LinksImpl asLinks() {
        throw new TypeDBDriverException(INVALID_CONSTRAINT_CASTING, className(this.getClass()), className(LinksImpl.class));
    }

    public SubImpl asSub() {
        throw new TypeDBDriverException(INVALID_CONSTRAINT_CASTING, className(this.getClass()), className(SubImpl.class));
    }

    public OwnsImpl asOwns() {
        throw new TypeDBDriverException(INVALID_CONSTRAINT_CASTING, className(this.getClass()), className(OwnsImpl.class));
    }

    public RelatesImpl asRelates() {
        throw new TypeDBDriverException(INVALID_CONSTRAINT_CASTING, className(this.getClass()), className(RelatesImpl.class));
    }

    public PlaysImpl asPlays() {
        throw new TypeDBDriverException(INVALID_CONSTRAINT_CASTING, className(this.getClass()), className(PlaysImpl.class));
    }

    public FunctionCallImpl asFunctionCall() {
        throw new TypeDBDriverException(INVALID_CONSTRAINT_CASTING, className(this.getClass()), className(FunctionCallImpl.class));
    }

    public ExpressionImpl asExpression() {
        throw new TypeDBDriverException(INVALID_CONSTRAINT_CASTING, className(this.getClass()), className(ExpressionImpl.class));
    }

    public IsImpl asIs() {
        throw new TypeDBDriverException(INVALID_CONSTRAINT_CASTING, className(this.getClass()), className(IsImpl.class));
    }

    public IidImpl asIid() {
        throw new TypeDBDriverException(INVALID_CONSTRAINT_CASTING, className(this.getClass()), className(IidImpl.class));
    }

    public DeleteConceptsImpl asDeleteConcepts() {
        throw new TypeDBDriverException(INVALID_CONSTRAINT_CASTING, className(this.getClass()), className(DeleteConceptsImpl.class));
    }

    public ComparisonImpl asComparison() {
        throw new TypeDBDriverException(INVALID_CONSTRAINT_CASTING, className(this.getClass()), className(ComparisonImpl.class));
    }

    public KindImpl asKind() {
        throw new TypeDBDriverException(INVALID_CONSTRAINT_CASTING, className(this.getClass()), className(KindImpl.class));
    }

    public LabelImpl asLabel() {
        throw new TypeDBDriverException(INVALID_CONSTRAINT_CASTING, className(this.getClass()), className(LabelImpl.class));
    }

    public ValueImpl asValue() {
        throw new TypeDBDriverException(INVALID_CONSTRAINT_CASTING, className(this.getClass()), className(ValueImpl.class));
    }

    public IsSetImpl asIsSet() {
        throw new TypeDBDriverException(INVALID_CONSTRAINT_CASTING, className(this.getClass()), className(IsSetImpl.class));
    }

    public OrImpl asOr() {
        throw new TypeDBDriverException(INVALID_CONSTRAINT_CASTING, className(this.getClass()), className(OrImpl.class));
    }

    public NotImpl asNot() {
        throw new TypeDBDriverException(INVALID_CONSTRAINT_CASTING, className(this.getClass()), className(NotImpl.class));
    }

    public TryImpl asTry() {
        throw new TypeDBDriverException(INVALID_CONSTRAINT_CASTING, className(this.getClass()), className(TryImpl.class));
    }

    public static class IsaImpl extends ConstraintImpl implements Constraint.Isa {
        public IsaImpl(com.typedb.driver.jni.ConstraintWithSpan nativeObject) {
            super(nativeObject);
        }

        @Override
        public boolean isIsa() {
            return true;
        }

        @Override
        public IsaImpl asIsa() {
            return this;
        }

        public ConstraintVertexImpl instance() {
            return new ConstraintVertexImpl(typedb_driver.constraint_isa_get_instance(nativeObject));
        }

        public ConstraintVertexImpl type() {
            return new ConstraintVertexImpl(typedb_driver.constraint_isa_get_type(nativeObject));
        }

        public com.typedb.driver.jni.ConstraintExactness exactness() {
            return typedb_driver.constraint_isa_get_exactness(nativeObject);
        }
    }

    public static class HasImpl extends ConstraintImpl implements Constraint.Has {
        public HasImpl(com.typedb.driver.jni.ConstraintWithSpan nativeObject) {
            super(nativeObject);
        }

        @Override
        public boolean isHas() {
            return true;
        }

        @Override
        public HasImpl asHas() {
            return this;
        }

        public ConstraintVertexImpl owner() {
            return new ConstraintVertexImpl(typedb_driver.constraint_has_get_owner(nativeObject));
        }

        public ConstraintVertexImpl attribute() {
            return new ConstraintVertexImpl(typedb_driver.constraint_has_get_attribute(nativeObject));
        }

        public com.typedb.driver.jni.ConstraintExactness exactness() {
            return typedb_driver.constraint_has_get_exactness(nativeObject);
        }
    }

    public static class LinksImpl extends ConstraintImpl implements Constraint.Links {
        public LinksImpl(com.typedb.driver.jni.ConstraintWithSpan nativeObject) {
            super(nativeObject);
        }

        @Override
        public boolean isLinks() {
            return true;
        }

        @Override
        public LinksImpl asLinks() {
            return this;
        }

        public ConstraintVertexImpl relation() {
            return new ConstraintVertexImpl(typedb_driver.constraint_links_get_relation(nativeObject));
        }

        public ConstraintVertexImpl player() {
            return new ConstraintVertexImpl(typedb_driver.constraint_links_get_player(nativeObject));
        }

        public ConstraintVertexImpl role() {
            return new ConstraintVertexImpl(typedb_driver.constraint_links_get_role(nativeObject));
        }

        public com.typedb.driver.jni.ConstraintExactness exactness() {
            return typedb_driver.constraint_links_get_exactness(nativeObject);
        }
    }

    public static class SubImpl extends ConstraintImpl implements Constraint.Sub {
        public SubImpl(com.typedb.driver.jni.ConstraintWithSpan nativeObject) {
            super(nativeObject);
        }

        @Override
        public boolean isSub() {
            return true;
        }

        @Override
        public SubImpl asSub() {
            return this;
        }

        public ConstraintVertexImpl subtype() {
            return new ConstraintVertexImpl(typedb_driver.constraint_sub_get_subtype(nativeObject));
        }

        public ConstraintVertexImpl supertype() {
            return new ConstraintVertexImpl(typedb_driver.constraint_sub_get_supertype(nativeObject));
        }

        public com.typedb.driver.jni.ConstraintExactness exactness() {
            return typedb_driver.constraint_sub_get_exactness(nativeObject);
        }
    }

    public static class OwnsImpl extends ConstraintImpl implements Constraint.Owns {
        public OwnsImpl(com.typedb.driver.jni.ConstraintWithSpan nativeObject) {
            super(nativeObject);
        }

        @Override
        public boolean isOwns() {
            return true;
        }

        @Override
        public OwnsImpl asOwns() {
            return this;
        }

        public ConstraintVertexImpl owner() {
            return new ConstraintVertexImpl(typedb_driver.constraint_owns_get_owner(nativeObject));
        }

        public ConstraintVertexImpl attribute() {
            return new ConstraintVertexImpl(typedb_driver.constraint_owns_get_attribute(nativeObject));
        }

        public com.typedb.driver.jni.ConstraintExactness exactness() {
            return typedb_driver.constraint_owns_get_exactness(nativeObject);
        }
    }

    public static class RelatesImpl extends ConstraintImpl implements Constraint.Relates {
        public RelatesImpl(com.typedb.driver.jni.ConstraintWithSpan nativeObject) {
            super(nativeObject);
        }

        @Override
        public boolean isRelates() {
            return true;
        }

        @Override
        public RelatesImpl asRelates() {
            return this;
        }

        public ConstraintVertexImpl relation() {
            return new ConstraintVertexImpl(typedb_driver.constraint_relates_get_relation(nativeObject));
        }

        public ConstraintVertexImpl role() {
            return new ConstraintVertexImpl(typedb_driver.constraint_relates_get_role(nativeObject));
        }

        public com.typedb.driver.jni.ConstraintExactness exactness() {
            return typedb_driver.constraint_relates_get_exactness(nativeObject);
        }
    }

    public static class PlaysImpl extends ConstraintImpl implements Constraint.Plays {
        public PlaysImpl(com.typedb.driver.jni.ConstraintWithSpan nativeObject) {
            super(nativeObject);
        }

        @Override
        public boolean isPlays() {
            return true;
        }

        @Override
        public PlaysImpl asPlays() {
            return this;
        }

        public ConstraintVertexImpl player() {
            return new ConstraintVertexImpl(typedb_driver.constraint_plays_get_player(nativeObject));
        }

        public ConstraintVertexImpl role() {
            return new ConstraintVertexImpl(typedb_driver.constraint_plays_get_role(nativeObject));
        }

        public com.typedb.driver.jni.ConstraintExactness exactness() {
            return typedb_driver.constraint_plays_get_exactness(nativeObject);
        }
    }

    public static class FunctionCallImpl extends ConstraintImpl implements Constraint.FunctionCall {
        public FunctionCallImpl(com.typedb.driver.jni.ConstraintWithSpan nativeObject) {
            super(nativeObject);
        }

        @Override
        public boolean isFunctionCall() {
            return true;
        }

        @Override
        public FunctionCallImpl asFunctionCall() {
            return this;
        }

        public String name() {
            return typedb_driver.constraint_function_call_get_name(nativeObject);
        }

        public Stream<ConstraintVertexImpl> arguments() {
            return new NativeIterator<>(typedb_driver.constraint_function_call_get_arguments(nativeObject)).stream().map(ConstraintVertexImpl::new);
        }

        public Stream<ConstraintVertexImpl> assigned() {
            return new NativeIterator<>(typedb_driver.constraint_function_call_get_assigned(nativeObject)).stream().map(ConstraintVertexImpl::new);
        }
    }

    public static class ExpressionImpl extends ConstraintImpl implements Constraint.Expression {
        public ExpressionImpl(com.typedb.driver.jni.ConstraintWithSpan nativeObject) {
            super(nativeObject);
        }

        @Override
        public boolean isExpression() {
            return true;
        }

        @Override
        public ExpressionImpl asExpression() {
            return this;
        }

        public String text() {
            return typedb_driver.constraint_expression_get_text(nativeObject);
        }

        public Stream<ConstraintVertexImpl> arguments() {
            return new NativeIterator<>(typedb_driver.constraint_expression_get_arguments(nativeObject)).stream().map(ConstraintVertexImpl::new);
        }

        public ConstraintVertexImpl assigned() {
            return new ConstraintVertexImpl(typedb_driver.constraint_expression_get_assigned(nativeObject));
        }
    }

    public static class IsImpl extends ConstraintImpl implements Constraint.Is {
        public IsImpl(com.typedb.driver.jni.ConstraintWithSpan nativeObject) {
            super(nativeObject);
        }

        @Override
        public boolean isIs() {
            return true;
        }

        @Override
        public IsImpl asIs() {
            return this;
        }

        public ConstraintVertexImpl lhs() {
            return new ConstraintVertexImpl(typedb_driver.constraint_is_get_lhs(nativeObject));
        }

        public ConstraintVertexImpl rhs() {
            return new ConstraintVertexImpl(typedb_driver.constraint_is_get_rhs(nativeObject));
        }
    }

    public static class IidImpl extends ConstraintImpl implements Constraint.Iid {
        public IidImpl(com.typedb.driver.jni.ConstraintWithSpan nativeObject) {
            super(nativeObject);
        }

        @Override
        public boolean isIid() {
            return true;
        }

        @Override
        public IidImpl asIid() {
            return this;
        }

        public ConstraintVertexImpl variable() {
            return new ConstraintVertexImpl(typedb_driver.constraint_iid_get_variable(nativeObject));
        }

        public String iid() {
            return typedb_driver.constraint_iid_get_iid(nativeObject);
        }
    }

    public static class DeleteConceptsImpl extends ConstraintImpl implements Constraint.DeleteConcepts {
        public DeleteConceptsImpl(com.typedb.driver.jni.ConstraintWithSpan nativeObject) {
            super(nativeObject);
        }

        @Override
        public boolean isDeleteConcepts() {
            return true;
        }

        @Override
        public DeleteConceptsImpl asDeleteConcepts() {
            return this;
        }

        public Stream<ConstraintVertexImpl> variables() {
            return new NativeIterator<>(typedb_driver.constraint_delete_concepts_get_variables(nativeObject)).stream().map(ConstraintVertexImpl::new);
        }
    }

    public static class ComparisonImpl extends ConstraintImpl implements Constraint.Comparison {
        public ComparisonImpl(com.typedb.driver.jni.ConstraintWithSpan nativeObject) {
            super(nativeObject);
        }

        @Override
        public boolean isComparison() {
            return true;
        }

        @Override
        public ComparisonImpl asComparison() {
            return this;
        }

        public ConstraintVertexImpl lhs() {
            return new ConstraintVertexImpl(typedb_driver.constraint_comparison_get_lhs(nativeObject));
        }

        public ConstraintVertexImpl rhs() {
            return new ConstraintVertexImpl(typedb_driver.constraint_comparison_get_rhs(nativeObject));
        }

        public com.typedb.driver.jni.Comparator comparator() {
            return typedb_driver.constraint_comparison_get_comparator(nativeObject);
        }

        public String comparatorSymbol() {
            return typedb_driver.comparator_get_name(comparator());
        }
    }

    public static class KindImpl extends ConstraintImpl implements Constraint.Kind {
        public KindImpl(com.typedb.driver.jni.ConstraintWithSpan nativeObject) {
            super(nativeObject);
        }

        @Override
        public boolean isKind() {
            return true;
        }

        @Override
        public KindImpl asKind() {
            return this;
        }

        public com.typedb.driver.jni.Kind kind() {
            return typedb_driver.constraint_kind_get_kind(nativeObject);
        }

        public ConstraintVertexImpl type() {
            return new ConstraintVertexImpl(typedb_driver.constraint_kind_get_type(nativeObject));
        }
    }

    public static class LabelImpl extends ConstraintImpl implements Constraint.Label {
        public LabelImpl(com.typedb.driver.jni.ConstraintWithSpan nativeObject) {
            super(nativeObject);
        }

        @Override
        public boolean isLabel() {
            return true;
        }

        @Override
        public LabelImpl asLabel() {
            return this;
        }

        public ConstraintVertexImpl variable() {
            return new ConstraintVertexImpl(typedb_driver.constraint_label_get_variable(nativeObject));
        }

        public String label() {
            return typedb_driver.constraint_label_get_label(nativeObject);
        }
    }

    public static class ValueImpl extends ConstraintImpl implements Constraint.Value {
        public ValueImpl(com.typedb.driver.jni.ConstraintWithSpan nativeObject) {
            super(nativeObject);
        }

        @Override
        public boolean isValue() {
            return true;
        }

        @Override
        public ValueImpl asValue() {
            return this;
        }

        public ConstraintVertexImpl attributeType() {
            return new ConstraintVertexImpl(typedb_driver.constraint_value_get_attribute_type(nativeObject));
        }

        public String valueType() {
            return typedb_driver.constraint_value_get_value_type(nativeObject);
        }
    }

    public static class IsSetImpl extends ConstraintImpl implements Constraint.IsSet {
        public IsSetImpl(com.typedb.driver.jni.ConstraintWithSpan nativeObject) {
            super(nativeObject);
        }

        @Override
        public boolean isIsSet() {
            return true;
        }

        @Override
        public IsSetImpl asIsSet() {
            return this;
        }

        public Stream<ConstraintVertexImpl> variables() {
            return new NativeIterator<>(typedb_driver.constraint_isset_get_variables(nativeObject)).stream().map(ConstraintVertexImpl::new);
        }
    }

    public static class OrImpl extends ConstraintImpl implements Constraint.Or {
        public OrImpl(com.typedb.driver.jni.ConstraintWithSpan nativeObject) {
            super(nativeObject);
        }

        @Override
        public boolean isOr() {
            return true;
        }

        @Override
        public OrImpl asOr() {
            return this;
        }

        public Stream<ConjunctionIDImpl> branches() {
            return new NativeIterator<>(typedb_driver.constraint_or_get_branches(nativeObject)).stream().map(ConjunctionIDImpl::new);
        }
    }

    public static class NotImpl extends ConstraintImpl implements Constraint.Not {
        public NotImpl(com.typedb.driver.jni.ConstraintWithSpan nativeObject) {
            super(nativeObject);
        }

        @Override
        public boolean isNot() {
            return true;
        }

        @Override
        public NotImpl asNot() {
            return this;
        }

        public ConjunctionIDImpl conjunction() {
            return new ConjunctionIDImpl(typedb_driver.constraint_not_get_conjunction(nativeObject));
        }
    }

    public static class TryImpl extends ConstraintImpl implements Constraint.Try {
        public TryImpl(com.typedb.driver.jni.ConstraintWithSpan nativeObject) {
            super(nativeObject);
        }

        @Override
        public boolean isTry() {
            return true;
        }

        @Override
        public TryImpl asTry() {
            return this;
        }

        public ConjunctionIDImpl conjunction() {
            return new ConjunctionIDImpl(typedb_driver.constraint_try_get_conjunction(nativeObject));
        }
    }
}
