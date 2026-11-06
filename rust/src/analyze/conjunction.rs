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

use std::collections::HashMap;

use crate::{IID, analyze::VariableAnnotations, concept};

/// Holds the index of the conjunction in a <code>Pipeline</code>'s <code>conjunctions</code> field.
/// Used as indirection in the representation of a pipeline.
#[derive(Debug, Clone, Copy, PartialEq)]
pub struct ConjunctionID(pub usize);

/// A representation of the constraints involved in the query, and types inferred for each variable.
#[derive(Debug, Clone)]
pub struct Conjunction {
    /// The <code>Constraint</code>s in the conjunction.
    pub constraints: Vec<ConstraintWithSpan>,
    /// The annotations of each variable in the conjunction.
    pub variable_annotations: HashMap<Variable, VariableAnnotations>,
}

/// Tells apart exact variants of constraints from the ones allowing subtype-polymorphism.
/// e.g. <code>isa!</code> would be represented as an <code>Constraint::Isa</code>
/// with its exactness field <code>ConstraintExactness::Exact</code>.
#[repr(C)]
#[derive(Debug, Clone)]
pub enum ConstraintExactness {
    /// Indicates the constraint matches exactly the specified type - e.g. `isa!` or `sub!`
    Exact,
    /// Indicates the constraint matches the specified type and its subtypes - e.g. `isa!` or `sub!`
    Subtypes,
}

/// The span of a constraint in the <code>source</code> of the <code>AnalyzedQuery</code>.
#[derive(Debug, Clone)]
pub struct ConstraintSpan {
    /// The offset of the first character in the span
    pub begin: usize,
    /// The offset after the last character in the span
    pub end: usize,
}

#[derive(Debug, Clone)]
pub struct ConstraintWithSpan {
    pub constraint: Constraint,
    pub span: Option<ConstraintSpan>,
}

/// A representation of a TypeQL constraint.
#[derive(Debug, Clone)]
pub enum Constraint {
    /// &lt;instance&gt; isa(!) &lt;type&gt;
    Isa {
        instance: ConstraintVertex,
        r#type: ConstraintVertex,
        exactness: ConstraintExactness,
    },
    /// &lt;owner&gt; has &lt;attribute&gt;
    Has {
        owner: ConstraintVertex,
        attribute: ConstraintVertex,
        exactness: ConstraintExactness,
    },
    /// &lt;relation&gt; links (&lt;role&gt;: &lt;player&gt;)
    Links {
        relation: ConstraintVertex,
        player: ConstraintVertex,
        role: ConstraintVertex,
        exactness: ConstraintExactness,
    },
    /// &lt;subtype&gt; sub(!) &lt;supertype&gt;
    Sub {
        subtype: ConstraintVertex,
        supertype: ConstraintVertex,
        exactness: ConstraintExactness,
    },
    /// &lt;owner&gt; owns &lt;attribute&gt;
    Owns {
        owner: ConstraintVertex,
        attribute: ConstraintVertex,
        exactness: ConstraintExactness,
    },
    /// &lt;relation&gt; relates &lt;role&gt;
    Relates {
        relation: ConstraintVertex,
        role: ConstraintVertex,
        exactness: ConstraintExactness,
    },
    /// &lt;player&gt; plays &lt;role&gt;
    Plays {
        player: ConstraintVertex,
        role: ConstraintVertex,
        exactness: ConstraintExactness,
    },
    /// let &lt;assigned&gt; = name(&lt;arguments&gt;)
    /// e.g. let $x, $y = my_function($a, $b);
    FunctionCall {
        name: String,
        assigned: Vec<ConstraintVertex>,
        arguments: Vec<ConstraintVertex>,
    },
    /// let &lt;assigned&gt; = &lt;expression&gt;
    /// e.g. let $x = $y + 5;
    /// Here, arguments will be `&lsqb;$y&rsqb;`
    Expression {
        text: String,
        assigned: ConstraintVertex,
        arguments: Vec<ConstraintVertex>,
    },
    /// &lt;lhs&gt; is &lt;rhs&gt;
    /// $x is $y
    Is {
        lhs: ConstraintVertex,
        rhs: ConstraintVertex,
    },
    /// &lt;concept&gt; iid &lt;iid&gt;
    /// e.g. `$y iid 0x1f0005000000000000012f`
    Iid {
        concept: ConstraintVertex,
        iid: IID,
    },
    /// delete &lt;variables&gt;
    /// e.g. `delete $q;` deletes the concepts bound to `$q`
    DeleteConcepts {
        variables: Vec<ConstraintVertex>,
    },
    /// &lt;lhs&gt; &lt;comparator&gt; &lt;rhs&gt;
    /// e.g. `$x &lt; 5`
    Comparison {
        lhs: ConstraintVertex,
        rhs: ConstraintVertex,
        comparator: Comparator,
    },
    /// &lt;kind&gt; &lt;type&gt;
    /// e.g. `entity person`
    Kind {
        kind: concept::Kind,
        r#type: ConstraintVertex,
    },
    /// &lt;type&gt; label &lt;label&gt;
    /// e.g. `$t label person`
    Label {
        r#type: ConstraintVertex,
        label: String,
    },
    /// &lt;attribute_type&gt; value &lt;value_type&gt;
    /// e.g. $t value string
    Value {
        attribute_type: ConstraintVertex,
        value_type: concept::ValueType,
    },
    // isset $x, $y, ...;
    IsSet {
        variables: Vec<ConstraintVertex>,
    },
    /// { &lt;branches&lsqb;0&rsqb;&gt; } or { &lt;branches&lsqb;1&rsqb;&gt; } &lsqb;or ...&rsqb;
    Or {
        /// Index into <code>Pipeline.conjunctions</code>
        branches: Vec<ConjunctionID>,
    },
    /// not { &lt;conjunction&gt; }
    Not {
        /// Index into <code>Pipeline.conjunctions</code>
        conjunction: ConjunctionID,
    },
    /// try { &lt;conjunction&gt; }
    Try {
        /// Index into <code>Pipeline.conjunctions</code>
        conjunction: ConjunctionID,
    },
}

/// Uniquely identifies a variable in a <code>Pipeline</code>pipeline.
/// Its name (if any) can be retrieved from the <code>variable_names</code> field in <code>Pipeline</code>
#[derive(Debug, Hash, Clone, Eq, PartialEq)]
pub struct Variable(pub u32);

/// The answer to a TypeDB query is a set of concepts which satisfy the <code>Constraints</code> in the query.
/// A <code>ConstraintVertex</code> is either a variable, or some identifier of the concept.
///
/// * A <code>Variable</code> is a vertex the query must match and return.
/// * A <code>Label</code> uniquely identifies a type
/// * A <code>Value</code> represents a primitive value literal in TypeDB.
/// * A <code>NamedRole</code> vertex is used in links & relates constraints, as multiple relations may have roles with the same name.
///
/// The types inferred for <code>Variable</code>, <code>Label</code> and <code>NamedRole</code> vertices
/// can be read from the <code>variable_annotations</code> field of the <code>Conjunction</code> it is in.
#[derive(Debug, Clone, PartialEq)]
pub enum ConstraintVertex {
    Variable(Variable),
    Label(concept::type_::Type),
    Value(concept::Value),
    NamedRole(NamedRole),
}

/// A <code>NamedRole</code> vertex is used in links & relates constraints, as multiple relations may have roles with the same name.
#[derive(Debug, Clone)]
pub struct NamedRole {
    pub variable: Variable,
    pub name: String,
}

impl PartialEq for NamedRole {
    fn eq(&self, other: &Self) -> bool {
        self.variable == other.variable // Names will be equal unless they belong to different pipelines
    }
}

/// A representation of the comparator used in a comparison constraint.
#[repr(C)]
#[derive(Debug, Clone)]
pub enum Comparator {
    Equal,
    NotEqual,
    LessThan,
    LessOrEqual,
    Greater,
    GreaterOrEqual,
    Like,
    Contains,
}

impl Comparator {
    /// The symbol representing the comparator in TypeQL
    pub fn symbol(&self) -> &'static str {
        match self {
            Comparator::Equal => "==",
            Comparator::NotEqual => "!=",
            Comparator::LessThan => "<",
            Comparator::LessOrEqual => "<=",
            Comparator::Greater => ">",
            Comparator::GreaterOrEqual => ">=",
            Comparator::Like => "Like",
            Comparator::Contains => "Contains",
        }
    }
}
