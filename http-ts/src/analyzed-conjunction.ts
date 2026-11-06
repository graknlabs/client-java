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

import {Type, Value} from "./concept";
import {AnalyzedPipeline} from "./analyze";
import {QueryStructureLegacy} from "./legacy";

export function getVariableName(structure: AnalyzedPipeline | QueryStructureLegacy, variable: ConstraintVertexVariable): string | null {
    return structure.variables[variable.id]?.name;
}

export type ConstraintVertexAny =
    ConstraintVertexVariable
    | ConstraintVertexLabel
    | ConstraintVertexValue
    | ConstraintVertexNamedRole;

export interface ConstraintVertexVariable {
    tag: "variable";
    id: string,
}

export interface ConstraintVertexLabel {
    tag: "label";
    type: Type;
}

export interface ConstraintVertexNamedRole {
    tag: "namedRole";
    variable: string;
    name: string;
}

export interface ConstraintVertexValue extends Value {
    tag: "value";
}

type ConjunctionIndex = number;

export type VariableInfo = { name: string | null };

export type ConstraintAny =
    ConstraintIsa
    | ConstraintIsaExact
    | ConstraintHas
    | ConstraintLinks
    |
    ConstraintSub
    | ConstraintSubExact
    | ConstraintOwns
    | ConstraintRelates
    | ConstraintPlays
    |
    ConstraintExpression
    | ConstraintFunction
    | ConstraintComparison
    |
    ConstraintIs
    | ConstraintIid
    | ConstraintDeleteConcepts
    | ConstraintKind
    | ConstraintValue
    | ConstraintIsSet
    | ConstraintLabel
    |
    ConstraintOr
    | ConstraintNot
    | ConstraintTry;

export type ConstraintSpan = { begin: number, end: number };

// Instance
export interface ConstraintIsa {
    tag: "isa",
    textSpan: ConstraintSpan,

    instance: ConstraintVertexVariable,
    type: ConstraintVertexVariable | ConstraintVertexLabel,
}

export interface ConstraintIsaExact {
    tag: "isa!",
    textSpan: ConstraintSpan,

    instance: ConstraintVertexVariable,
    type: ConstraintVertexVariable | ConstraintVertexLabel,
}

export interface ConstraintHas {
    tag: "has",
    textSpan: ConstraintSpan,

    owner: ConstraintVertexVariable
    attribute: ConstraintVertexVariable,
}

export interface ConstraintLinks {
    tag: "links",
    textSpan: ConstraintSpan,

    relation: ConstraintVertexVariable,
    player: ConstraintVertexVariable,
    role: ConstraintVertexVariable | ConstraintVertexNamedRole,
}

// Type
export interface ConstraintSub {
    tag: "sub",
    textSpan: ConstraintSpan,

    subtype: ConstraintVertexVariable | ConstraintVertexLabel,
    supertype: ConstraintVertexVariable | ConstraintVertexLabel,
}

export interface ConstraintSubExact {
    tag: "sub!",
    textSpan: ConstraintSpan,

    subtype: ConstraintVertexVariable | ConstraintVertexLabel,
    supertype: ConstraintVertexVariable | ConstraintVertexLabel,
}

export interface ConstraintOwns {
    tag: "owns",
    textSpan: ConstraintSpan,

    owner: ConstraintVertexVariable | ConstraintVertexLabel,
    attribute: ConstraintVertexVariable | ConstraintVertexLabel,
}

export interface ConstraintRelates {
    tag: "relates",
    textSpan: ConstraintSpan,

    relation: ConstraintVertexVariable | ConstraintVertexLabel,
    role: ConstraintVertexVariable | ConstraintVertexLabel,
}

export interface ConstraintPlays {
    tag: "plays",
    textSpan: ConstraintSpan,

    player: ConstraintVertexVariable | ConstraintVertexLabel,
    role: ConstraintVertexVariable | ConstraintVertexLabel,
}

// Function
export interface ConstraintExpression {
    tag: "expression",
    textSpan: ConstraintSpan,

    text: string,
    arguments: ConstraintVertexVariable[],
    assigned: ConstraintVertexVariable,
}

export interface ConstraintFunction {
    tag: "functionCall",
    textSpan: ConstraintSpan,

    name: string,
    arguments: ConstraintVertexVariable[],
    assigned: ConstraintVertexVariable[],
}

export interface ConstraintComparison {
    tag: "comparison",
    textSpan: ConstraintSpan,

    lhs: ConstraintVertexVariable | ConstraintVertexValue,
    rhs: ConstraintVertexVariable | ConstraintVertexValue,
    comparator: string,
}

export interface ConstraintIs {
    tag: "is",
    textSpan: ConstraintSpan,

    lhs: ConstraintVertexVariable,
    rhs: ConstraintVertexVariable,
}

export interface ConstraintIid {
    tag: "iid",
    textSpan: ConstraintSpan,

    concept: ConstraintVertexVariable,
    iid: string,
}

export interface ConstraintDeleteConcepts {
    tag: "deleteConcepts",
    textSpan: ConstraintSpan,

    variables: ConstraintVertexVariable[],
}

export interface ConstraintLabel {
    tag: "label",
    textSpan: ConstraintSpan,

    type: ConstraintVertexVariable,
    label: string,
}

export interface ConstraintValue {
    tag: "value",
    textSpan: ConstraintSpan,

    attributeType: ConstraintVertexVariable,
    valueType: string,
}

export interface ConstraintIsSet {
    tag: "isSet",
    textSpan: ConstraintSpan,

    variables: ConstraintVertexVariable[],
}

export interface ConstraintKind {
    tag: "kind",
    textSpan: ConstraintSpan,

    type: ConstraintVertexVariable,
    kind: string,
}

export interface ConstraintOr {
    tag: "or",
    branches: ConjunctionIndex[],
}

export interface ConstraintNot {
    tag: "not",
    conjunction: ConjunctionIndex,
}

export interface ConstraintTry {
    tag: "try",
    conjunction: ConjunctionIndex,
}
