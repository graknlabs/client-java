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

import {When, Then} from "@cucumber/cucumber";
import assert from "assert";
import {analyzed, doAnalyze, setAnalyzed} from "./context";
import {assertNotError, EXPECT_ERROR_CONTAINING, MayError} from "./params";

import {
    AnalyzedFetch,
    AnalyzedFunction,
    AnalyzedPipeline,
    ConstraintAny,
    ConstraintVertexAny,
    FunctionReturnStructure,
    PipelineStage,
    Reducer,
    VariableAnnotations,
} from "../../../dist/index.cjs";


When('get answers of typeql analyze', async function (query: string) {
    const results = await doAnalyze(query).then(assertNotError);
    setAnalyzed(results.ok);
});

const analyzeMayError = async function (mayError: MayError, query: string) {
    const results = await doAnalyze(query);
    if (mayError) assert.notEqual(results.err, undefined);
    else assert.notEqual(results.ok, undefined);
}
When('typeql analyze{may_error}', analyzeMayError);
When(`typeql analyze${EXPECT_ERROR_CONTAINING}`, analyzeMayError);

Then('analyzed query given structure is:', function (expectedFunctor: string) {
    const context = new FunctorEncoder(analyzed.query);
    const actualFunctor = encodeGivenStructure(analyzed, context);
    assert.equal(normalizeFunctorForCompare(actualFunctor), normalizeFunctorForCompare(expectedFunctor))
});

Then('analyzed query given annotations are:', function (expectedFunctor: string) {
    const context = new FunctorEncoder(analyzed.query);
    const actualFunctor = encodeGivenAnnotations(analyzed, context);
    assert.equal(normalizeFunctorForCompare(actualFunctor), normalizeFunctorForCompare(expectedFunctor))
});

Then('analyzed query pipeline structure is:', function (expectedFunctor: string) {
    const context = new FunctorEncoder(analyzed.query);
    const actualFunctor = encodePipeline(analyzed.query, context);
    assert.equal(normalizeFunctorForCompare(actualFunctor), normalizeFunctorForCompare(expectedFunctor))
});

Then('analyzed query preamble contains:', function (expectedFunctor: string) {
    const preambleFunctors = analyzed.preamble.map(func => {
        const encoder = new FunctorEncoder(func.body);
        return encodeFunction(func, encoder);
    });

    const normalizedExpected = normalizeFunctorForCompare(expectedFunctor);
    const found = preambleFunctors.some(actual =>
        normalizeFunctorForCompare(actual) === normalizedExpected
    );
    assert.ok(found, `Expected to find functor in preamble: ${normalizedExpected}\nFound: ${preambleFunctors.join('\n')}`);
});

Then('analyzed query pipeline annotations are:', function (expectedFunctor: string) {
    const context = new FunctorEncoder(analyzed.query);
    const actualFunctor = encodePipelineAnnotations(analyzed.query, context);
    assert.equal(normalizeFunctorForCompare(actualFunctor), normalizeFunctorForCompare(expectedFunctor))
});

Then('analyzed preamble annotations contains:', function (expectedFunctor: string) {
    const preambleFunctors = analyzed.preamble.map(func => {
        const encoder = new FunctorEncoder(func.body);
        return encodeFunctionAnnotations(func, encoder);
    });

    const normalizedExpected = normalizeFunctorForCompare(expectedFunctor);
    const found = preambleFunctors.some(actual =>
        normalizeFunctorForCompare(actual) === normalizedExpected
    );
    assert.ok(found, `Expected to find functor in preamble: ${normalizedExpected}\nFound: ${preambleFunctors.join('\n')}`);
});

Then('analyzed fetch annotations are:', function (expectedFunctor: string) {
    encodeFetchAnnotations(analyzed.fetch);
});

export function normalizeFunctorForCompare(functor: string): string {
    return functor.toLowerCase().replace(/\s/g, "");
}

// FunctorEncoder class
export class FunctorEncoder {
    pipeline: AnalyzedPipeline;

    constructor(pipeline: AnalyzedPipeline) {
        this.pipeline = pipeline;
    }

    makeFunctor(name: string, ...args: any[]): string {
        const encodedArgs = args.join(", ");
        return `${name}(${encodedArgs})`;
    }

    encodeAsList(elements: any[]): string {
        return `[${elements.join(", ")}]`;
    }
}

// Helper functions
function encodeConstraint(constraint: ConstraintAny, encoder: FunctorEncoder): string {
    switch (constraint.tag) {
        case "isa":
            return encoder.makeFunctor("Isa",
                encodeConstraintVertex(constraint.instance, encoder),
                encodeConstraintVertex(constraint.type, encoder));
        case "isa!":
            return encoder.makeFunctor("IsaExact",
                encodeConstraintVertex(constraint.instance, encoder),
                encodeConstraintVertex(constraint.type, encoder));
        case "has":
            return encoder.makeFunctor("Has",
                encodeConstraintVertex(constraint.owner, encoder),
                encodeConstraintVertex(constraint.attribute, encoder));
        case "links":
            return encoder.makeFunctor("Links",
                encodeConstraintVertex(constraint.relation, encoder),
                encodeConstraintVertex(constraint.player, encoder),
                encodeConstraintVertex(constraint.role, encoder));
        case "sub":
            return encoder.makeFunctor("Sub",
                encodeConstraintVertex(constraint.subtype, encoder),
                encodeConstraintVertex(constraint.supertype, encoder));
        case "sub!":
            return encoder.makeFunctor("SubExact",
                encodeConstraintVertex(constraint.subtype, encoder),
                encodeConstraintVertex(constraint.supertype, encoder));
        case "owns":
            return encoder.makeFunctor("Owns",
                encodeConstraintVertex(constraint.owner, encoder),
                encodeConstraintVertex(constraint.attribute, encoder));
        case "relates":
            return encoder.makeFunctor("Relates",
                encodeConstraintVertex(constraint.relation, encoder),
                encodeConstraintVertex(constraint.role, encoder));
        case "plays":
            return encoder.makeFunctor("Plays",
                encodeConstraintVertex(constraint.player, encoder),
                encodeConstraintVertex(constraint.role, encoder));
        case "expression":
            return encoder.makeFunctor("Expression",
                constraint.text,
                encodeConstraintVertex(constraint.assigned, encoder),
                encoder.encodeAsList(constraint.arguments.map(v => encodeConstraintVertex(v, encoder))));
        case "functionCall":
            return encoder.makeFunctor("FunctionCall",
                constraint.name,
                encoder.encodeAsList(constraint.assigned.map(v => encodeConstraintVertex(v, encoder))),
                encoder.encodeAsList(constraint.arguments.map(v => encodeConstraintVertex(v, encoder))));
        case "comparison":
            return encoder.makeFunctor("Comparison",
                encodeConstraintVertex(constraint.lhs, encoder),
                encodeConstraintVertex(constraint.rhs, encoder),
                constraint.comparator);
        case "is":
            return encoder.makeFunctor("Is",
                encodeConstraintVertex(constraint.lhs, encoder),
                encodeConstraintVertex(constraint.rhs, encoder));
        case "iid":
            return encoder.makeFunctor("Iid",
                encodeConstraintVertex(constraint.concept, encoder),
                constraint.iid);
        case "deleteConcepts":
            return encoder.makeFunctor("DeleteConcepts",
                encoder.encodeAsList(constraint.variables.map(v => encodeConstraintVertex(v, encoder))));
        case "kind":
            return encoder.makeFunctor("Kind",
                constraint.kind,
                encodeConstraintVertex(constraint.type, encoder)
            );
        case "value":
            return encoder.makeFunctor("Value",
                encodeConstraintVertex(constraint.attributeType, encoder),
                constraint.valueType);
        case "isSet":
            return encoder.makeFunctor("IsSet",
                encoder.encodeAsList(constraint.variables.map(v => encodeConstraintVertex(v, encoder))));
        case "label":
            return encoder.makeFunctor("Label",
                encodeConstraintVertex(constraint.type, encoder),
                constraint.label);
        case "or":
            return encoder.makeFunctor(
                "or",
                encoder.encodeAsList(constraint.branches.map(c => encodeConjunction(c, encoder)))
            );
        case "not":
            return encoder.makeFunctor("not", encodeConjunction(constraint.conjunction, encoder));
        case "try":
            return encoder.makeFunctor("try", encodeConjunction(constraint.conjunction, encoder));
    }
}

function encodeVariable(id: string, encoder: FunctorEncoder) {
    const v = encoder.pipeline.variables[id];
    if (v != null && v.name != null) {
        return "$" + v.name;
    } else {
        return "$_";
    }
}

function encodeConstraintVertex(vertex: ConstraintVertexAny, encoder: FunctorEncoder): string {
    switch (vertex.tag) {
        case "variable":
            return encodeVariable(vertex.id, encoder);
        case "label":
            return vertex.type.label;
        case "namedRole":
            return vertex.name;
        case "value": {
            if (vertex.valueType == "string") {
                return `"${vertex.value}"`;
            } else {
                return vertex.value;
            }
        }
    }
    throw new Error("Unknown constraint vertex type");
}

function encodeConjunction(index: number, encoder: FunctorEncoder): string {
    const conjunction = encoder.pipeline.conjunctions[index];
    return encoder.encodeAsList(conjunction.constraints.map(c => encodeConstraint(c, encoder)));
}

function encodeReducer(reducer: Reducer, encoder: FunctorEncoder): string {
    return encoder.makeFunctor(
        "Reducer",
        reducer.reducer,
        encoder.encodeAsList(reducer.arguments.map(v => encodeVariable(v, encoder)))
    );
}

function encodePipelineStage(stage: PipelineStage, encoder: FunctorEncoder): string {
    const variant = stage.tag;
    switch (stage.tag) {
        case "match":
        case "insert":
        case "put":
        case "update":
            return encoder.makeFunctor(variant, encodeConjunction(stage.block, encoder));
        case "delete":
            return encoder.makeFunctor(
                variant,
                encoder.encodeAsList(stage.deletedVariables.map(v => encodeVariable(v, encoder))),
                encodeConjunction(stage.block, encoder)
            );
        case "select":
            return encoder.makeFunctor(
                variant,
                encoder.encodeAsList(stage.variables.map(v => encodeVariable(v, encoder)))
            );
        case "sort": {
            const sortVariables = stage.variables.map(v => encoder.makeFunctor(
                v.tag == "ascending" ? "Asc" : "Desc",
                encodeVariable(v.variable, encoder),
            ));
            return encoder.makeFunctor(variant, encoder.encodeAsList(sortVariables));
        }
        case "require":
            return encoder.makeFunctor(
                variant,
                encoder.encodeAsList(stage.variables.map(v => encodeVariable(v, encoder)))
            );
        case "offset":
            return encoder.makeFunctor(variant, stage.offset);
        case "limit":
            return encoder.makeFunctor(variant, stage.limit);
        case "distinct":
            return encoder.makeFunctor(variant);
        case "reduce": {
            const reduceAssigns = stage.reducers.map(reduceAssign => encoder.makeFunctor(
                "ReduceAssign",
                encodeVariable(reduceAssign.assigned, encoder),
                encodeReducer(reduceAssign.reducer, encoder)
            ));
            return encoder.makeFunctor(
                variant,
                encoder.encodeAsList(reduceAssigns),
                encoder.encodeAsList(stage.groupby.map(v => encodeVariable(v, encoder)))
            );
        }
    }
    throw new Error(`Unknown pipeline stage variant: ${variant}`);
}

function encodeReturnOperation(returnOp: FunctionReturnStructure, encoder: FunctorEncoder): string {
    switch (returnOp.tag) {
        case "stream":
            return encoder.makeFunctor(
                "Stream",
                encoder.encodeAsList(returnOp.variables.map(v => encodeVariable(v, encoder)))
            );
        case "single":
            return encoder.makeFunctor(
                "Single",
                returnOp.selector,
                encoder.encodeAsList(returnOp.variables.map(v => encodeVariable(v, encoder)))
            );
        case "check":
            return encoder.makeFunctor("Check", "");
        case "reduce":
            return encoder.makeFunctor(
                "Reduce",
                encoder.encodeAsList(returnOp.reducers.map(r => encodeReducer(r, encoder)))
            );
    }
}

function encodeGivenStructure(analyzed: { query: AnalyzedPipeline, given: { variables: string[] } | null }, encoder: FunctorEncoder): string {
    const given = analyzed.given!;
    const variables = given.variables.map(v => encodeVariable(v, encoder));
    return encoder.makeFunctor("Given", encoder.encodeAsList(variables));
}

function encodeGivenAnnotations(analyzed: { query: AnalyzedPipeline, given: { variableAnnotations: { [name: string]: VariableAnnotations } } | null }, encoder: FunctorEncoder): string {
    const given = analyzed.given!;
    const variableAnnotations = Object.keys(given.variableAnnotations).map(v => {
        const annotations = given.variableAnnotations[v];
        return [encodeVariable(v, encoder), encodeVariableAnnotations(annotations, encoder)];
    }).sort().map(([k, v]) => `${k} : ${v}`);
    return encoder.makeFunctor("Given", "{" + variableAnnotations.join(",") + "}");
}

export function encodePipeline(pipeline: AnalyzedPipeline, encoder: FunctorEncoder): string {
    return encoder.makeFunctor("Pipeline", encoder.encodeAsList(pipeline.stages.map(stage => encodePipelineStage(stage, encoder))));
}

function encodeFunction(func: AnalyzedFunction, encoder: FunctorEncoder): string {
    const encodedArgs = func.arguments.map(arg =>
        encodeVariable(arg, encoder)
    );

    const encodedBody = encodePipeline(func.body, encoder);
    const encodedReturn = encodeReturnOperation(func.returns, encoder);

    return encoder.makeFunctor(
        "Function",
        encoder.encodeAsList(encodedArgs),
        encodedReturn,
        encodedBody
    );
}

function encodeVariableAnnotations(annotations: VariableAnnotations, encoder: FunctorEncoder) {
    const variant = annotations.tag;
    switch (annotations.tag) {
        case "value":
            return encoder.makeFunctor("Value", encoder.encodeAsList(annotations.valueTypes));
        case "type":
            return encoder.makeFunctor("Type", encoder.encodeAsList(annotations.annotations.map(t => t.label)));
        case "instance":
            return encoder.makeFunctor("Instance", encoder.encodeAsList(annotations.annotations.map(t => t.label)));
    }
    throw new Error(`Unknown VariableAnnotations variant: ${variant}`);
}

function encodeConjunctionAnnotations(conjunctionIndex: number, encoder: FunctorEncoder): string {
    const constraints = encoder.pipeline.conjunctions[conjunctionIndex].constraints;
    const subpatternAnnotations = constraints
        .map(c => {
            switch (c.tag) {
                case "or": {
                    const branchAnnotations = c.branches
                        .map(b => encodeConjunctionAnnotations(b, encoder));
                    return encoder.makeFunctor("Or", encoder.encodeAsList(branchAnnotations));
                }
                case "not":
                    return encoder.makeFunctor("Not", encodeConjunctionAnnotations(c.conjunction, encoder));
                case "try":
                    return encoder.makeFunctor("Try", encodeConjunctionAnnotations(c.conjunction, encoder));
                default:
                    return null;
            }
        }).filter(x => x != null);
    const conjunctionAnnotations = encoder.pipeline.conjunctions[conjunctionIndex].annotations;
    const variableAnnotations = Object.keys(conjunctionAnnotations.variableAnnotations).map(v => {
        const annotations = conjunctionAnnotations.variableAnnotations[v];
        return [encodeVariable(v, encoder), encodeVariableAnnotations(annotations, encoder)]
    }).sort().map(([k, v]) => `${k} : ${v}`);
    return encoder.makeFunctor(
        "And",
        "{" + variableAnnotations.join(",") + "}",
        encoder.encodeAsList(subpatternAnnotations)
    );
}

function encodeStageAnnotations(stage: PipelineStage, encoder: FunctorEncoder): string {
    const variant = stage.tag;
    switch (stage.tag) {
        case "match":
        case "insert":
        case "put":
        case "update":
        case "delete":
            return encoder.makeFunctor(variant, encodeConjunctionAnnotations(stage.block, encoder));
        case "select":
        case "sort":
        case "require":
        case "offset":
        case "limit":
        case "distinct":
        case "reduce":
            return encoder.makeFunctor(variant);
    }
    throw new Error(`Unknown pipeline stage variant: ${variant}`);
}

function encodePipelineAnnotations(pipeline: AnalyzedPipeline, encoder: FunctorEncoder): string {
    return encoder.makeFunctor(
        "Pipeline",
        encoder.encodeAsList(pipeline.stages.map(stageAnnotation => encodeStageAnnotations(stageAnnotation, encoder)))
    );
}

function encodeFunctionAnnotations(func: AnalyzedFunction, encoder: FunctorEncoder): string {
    const args = func.argumentAnnotations.map(arg => encodeVariableAnnotations(arg, encoder));
    const ret = func.returnAnnotations.annotations.map(arg => encodeVariableAnnotations(arg, encoder));
    return encoder.makeFunctor(
        "Function",
        encoder.encodeAsList(args),
        encoder.makeFunctor(func.returnAnnotations.tag, encoder.encodeAsList(ret)),
        encodePipelineAnnotations(func.body, encoder)
    );
}

function encodeFetchAnnotations(fetch: AnalyzedFetch): string {
    switch (fetch.tag) {
        case "list": {
            const inner = encodeFetchAnnotations(fetch.elements);
            return `List(${inner})`;
        }
        case "object": {
            const kv = fetch.possibleFields.map(inner => `${inner.key}: ${encodeFetchAnnotations(inner)}`);
            return "{" + kv.join(",") + "}";
        }
        case "value":
            return "[" + fetch.valueTypes.join(",") + "]";
    }
    throw new Error("Unknown fetch annotations variant");
}
