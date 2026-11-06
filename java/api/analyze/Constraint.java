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

package com.typedb.driver.api.analyze;

import java.util.stream.Stream;

/**
 * A representation of a TypeQL constraint.
 */
public interface Constraint {
    /**
     * Gets the variant of this constraint.
     *
     * @return the constraint variant
     */
    com.typedb.driver.jni.ConstraintVariant variant();

    /**
     * Gets the span of this constraint.
     *
     * @return the constraint span
     */
    Span span();

    boolean isIsa();

    boolean isHas();

    boolean isLinks();

    boolean isSub();

    boolean isOwns();

    boolean isRelates();

    boolean isPlays();

    boolean isFunctionCall();

    boolean isExpression();

    boolean isIs();

    boolean isIid();

    boolean isDeleteConcepts();

    boolean isComparison();

    boolean isKind();

    boolean isLabel();

    boolean isValue();

    boolean isIsSet();

    boolean isOr();

    boolean isNot();

    boolean isTry();

    Isa asIsa();

    Has asHas();

    Links asLinks();

    Sub asSub();

    Owns asOwns();

    Relates asRelates();

    Plays asPlays();

    FunctionCall asFunctionCall();

    Expression asExpression();

    Is asIs();

    Iid asIid();

    DeleteConcepts asDeleteConcepts();

    Comparison asComparison();

    Kind asKind();

    Label asLabel();

    Value asValue();

    IsSet asIsSet();

    Or asOr();

    Not asNot();

    Try asTry();

    /**
     * Represents an "isa" constraint: <instance> isa(!) <type>
     */
    interface Isa extends Constraint {
        ConstraintVertex instance();

        ConstraintVertex type();

        com.typedb.driver.jni.ConstraintExactness exactness();
    }

    /**
     * Represents a "has" constraint: <owner> has <attribute>
     */
    interface Has extends Constraint {
        ConstraintVertex owner();

        ConstraintVertex attribute();

        com.typedb.driver.jni.ConstraintExactness exactness();
    }

    /**
     * Represents a "links" constraint: <relation> links (<role>: <player>)
     */
    interface Links extends Constraint {
        ConstraintVertex relation();

        ConstraintVertex player();

        ConstraintVertex role();

        com.typedb.driver.jni.ConstraintExactness exactness();
    }

    /**
     * Represents a "sub" constraint: <subtype> sub(!) <supertype>
     */
    interface Sub extends Constraint {
        ConstraintVertex subtype();

        ConstraintVertex supertype();

        com.typedb.driver.jni.ConstraintExactness exactness();
    }

    /**
     * Represents an "owns" constraint: <owner> owns <attribute>
     */
    interface Owns extends Constraint {
        ConstraintVertex owner();

        ConstraintVertex attribute();

        com.typedb.driver.jni.ConstraintExactness exactness();
    }

    /**
     * Represents a "relates" constraint: <relation> relates <role>
     */
    interface Relates extends Constraint {
        ConstraintVertex relation();

        ConstraintVertex role();

        com.typedb.driver.jni.ConstraintExactness exactness();
    }

    /**
     * Represents a "plays" constraint: <player> plays <role>
     */
    interface Plays extends Constraint {
        ConstraintVertex player();

        ConstraintVertex role();

        com.typedb.driver.jni.ConstraintExactness exactness();
    }

    /**
     * Represents a function call: let <assigned> = name(<arguments>)
     * e.g., let $x, $y = my_function($a, $b);
     */
    interface FunctionCall extends Constraint {
        String name();

        Stream<? extends ConstraintVertex> arguments();

        Stream<? extends ConstraintVertex> assigned();
    }

    /**
     * Represents an expression: let <assigned> = <expression>
     * e.g., let $x = $y + 5;
     * Here, arguments will be `[$y]`
     */
    interface Expression extends Constraint {
        String text();

        Stream<? extends ConstraintVertex> arguments();

        ConstraintVertex assigned();
    }

    /**
     * Represents an "is" constraint: <lhs> is <rhs>
     * e.g., $x is $y
     */
    interface Is extends Constraint {
        ConstraintVertex lhs();

        ConstraintVertex rhs();
    }

    /**
     * Represents an IID constraint: <concept> iid <iid>
     * e.g., `$y iid 0x1f0005000000000000012f`
     */
    interface Iid extends Constraint {
        ConstraintVertex variable();

        String iid();
    }

    /**
     * Represents the deletion of concepts bound to a set of variables: delete <variables>
     */
    interface DeleteConcepts extends Constraint {
        Stream<? extends ConstraintVertex> variables();
    }

    /**
     * Represents a comparison: <lhs> <comparator> <rhs>
     * e.g., `$x < 5`
     */
    interface Comparison extends Constraint {
        ConstraintVertex lhs();

        ConstraintVertex rhs();

        com.typedb.driver.jni.Comparator comparator();

        String comparatorSymbol();
    }

    /**
     * Represents a kind constraint: <kind> <type>
     * e.g., `entity person`
     */
    interface Kind extends Constraint {
        com.typedb.driver.jni.Kind kind();

        ConstraintVertex type();
    }

    /**
     * Represents a label constraint: <type> label <label>
     * e.g., `$t label person`
     */
    interface Label extends Constraint {
        ConstraintVertex variable();

        String label();
    }

    /**
     * Represents an isset constraint: isset <variables>
     */
    interface IsSet extends Constraint {
        Stream<? extends ConstraintVertex> variables();
    }

    /**
     * Represents a value constraint: <attribute_type> value <value_type>
     * e.g., `$t value string`
     */
    interface Value extends Constraint {
        ConstraintVertex attributeType();

        String valueType();
    }

    /**
     * Represents an "or" constraint: { <branches[0]> } or { <branches[1]> } [or ...]
     */
    interface Or extends Constraint {
        /**
         * Index into <code>Pipeline.conjunctions</code>
         */
        Stream<? extends ConjunctionID> branches();
    }

    /**
     * Represents a "not" constraint: not { <conjunction> }
     */
    interface Not extends Constraint {
        /**
         * Index into <code>Pipeline.conjunctions</code>
         */
        ConjunctionID conjunction();
    }

    /**
     * Represents a "try" constraint: try { <conjunction> }
     */
    interface Try extends Constraint {
        /**
         * Index into <code>Pipeline.conjunctions</code>
         */
        ConjunctionID conjunction();
    }

    /**
     * The span of a constraint in the source query
     */
    interface Span {
        /**
         * @return The offset of the first character.
         */
        long begin();

        /**
         * @return The offset after the last character.
         */
        long end();
    }
}
