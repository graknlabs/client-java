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

package com.typedb.driver.test.behaviour.util;

import com.typedb.driver.api.concept.Concept;
import com.typedb.driver.api.analyze.Conjunction;
import com.typedb.driver.api.analyze.Constraint;
import com.typedb.driver.api.analyze.ConstraintVertex;
import com.typedb.driver.api.analyze.Fetch;
import com.typedb.driver.api.analyze.Function;
import com.typedb.driver.api.analyze.Given;
import com.typedb.driver.api.analyze.Pipeline;
import com.typedb.driver.api.analyze.PipelineStage;
import com.typedb.driver.api.analyze.Reducer;
import com.typedb.driver.api.analyze.Variable;
import com.typedb.driver.api.analyze.VariableAnnotations;

import com.typedb.driver.jni.ConstraintExactness;
import com.typedb.driver.jni.ReturnOperationVariant;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class FunctorEncoder {

    protected final Pipeline pipeline;

    public FunctorEncoder(Pipeline pipeline) {
        this.pipeline = pipeline;
    }

    private static String encodeList(Stream<String> elements) {
        return String.format("[%s]", elements.collect(Collectors.joining(",")));
    }

    private static String makeFunctor(String functor, String... encodedArgs) {
        return String.format("%s(%s)", functor, String.join(",", encodedArgs));
    }

    public static String normalizeForCompare(String functor) {
        return functor.toLowerCase().replaceAll("\\s+", "");
    }

    // The few common ones
    public String encode(Variable variable) {
        return "$" + pipeline.getVariableName(variable).orElse("_");
    }

    public static class StructureEncoder extends FunctorEncoder {
        public StructureEncoder(Pipeline pipeline) {
            super(pipeline);
        }

        private String constraintFunctor(String variant, ConstraintExactness exactness, ConstraintVertex... args) {
            String variantWithExactness = exactness == ConstraintExactness.Exact ? variant + "Exact" : variant;
            return makeFunctor(variantWithExactness, Arrays.stream(args).map(this::encode).toArray(String[]::new));
        }

        private String encode(PipelineStage stage) {
            switch (stage.getVariant()) {
                case Match:
                    return makeFunctor("Match", encode(pipeline.conjunction(stage.asMatch().block()).get()));
                case Insert:
                    return makeFunctor("Insert", encode(pipeline.conjunction(stage.asInsert().block()).get()));

                case Put:
                    return makeFunctor("Put", encode(pipeline.conjunction(stage.asPut().block()).get()));

                case Update:
                    return makeFunctor("Update", encode(pipeline.conjunction(stage.asUpdate().block()).get()));
                case Delete:
                    return makeFunctor(
                            "Delete",
                            encodeList(stage.asDelete().deletedVariables().map(this::encode)),
                            encode(pipeline.conjunction(stage.asDelete().block()).get())
                    );

                case Select:
                    return makeFunctor("Select", encodeList(stage.asSelect().variables().map(this::encode)));
                case Sort:
                    return makeFunctor("Sort", encodeList(stage.asSort().variables().map(this::encode)));
                case Require:
                    return makeFunctor("Require", encodeList(stage.asRequire().variables().map(this::encode)));
                case Offset:
                    return makeFunctor("Offset", Long.toString(stage.asOffset().offset()));
                case Limit:
                    return makeFunctor("Limit", Long.toString(stage.asLimit().limit()));
                case Distinct:
                    return makeFunctor("Distinct");
                case Reduce:
                    return makeFunctor("Reduce", encodeList(stage.asReduce().reducerAssignments().map(this::encode)), encodeList(stage.asReduce().groupBy().map(this::encode)));
                default:
                    throw new IllegalArgumentException("Unhandled stage variant", null);
            }
        }

        private String encode(PipelineStage.ReduceStage.ReduceAssignment reduceAssignment) {
            return makeFunctor("ReduceAssign", encode(reduceAssignment.assigned()), encode(reduceAssignment.reducer()));
        }

        private String encode(PipelineStage.SortStage.SortVariable sortVariable) {
            String order = sortVariable.order() == com.typedb.driver.jni.SortOrder.Ascending ? "Asc" : "Desc";
            return makeFunctor(order, encode(sortVariable.variable()));
        }

        public String encode(ConstraintVertex vertex) {
            if (vertex.isVariable()) {
                return encode(vertex.asVariable());
            } else if (vertex.isLabel()) {
                return vertex.asLabel().getLabel();
            } else if (vertex.isValue()) {
                if (vertex.asValue().isString()) {
                    return "\"" + vertex.asValue().toString() + "\"";
                } else {
                    return vertex.asValue().toString();
                }
            } else if (vertex.isNamedRole()) {
                return vertex.asNamedRole().name();
            } else {
                throw new IllegalArgumentException("Unexpected ConstraintVertex variant");
            }
        }

        public String encode(Constraint constraint) {
            switch (constraint.variant()) {
                case Isa: {
                    Constraint.Isa isa = constraint.asIsa();
                    return constraintFunctor("Isa", isa.exactness(), isa.instance(), isa.type());
                }
                case Has: {
                    Constraint.Has has = constraint.asHas();
                    return constraintFunctor("Has", has.exactness(), has.owner(), has.attribute());
                }
                case Links: {
                    Constraint.Links links = constraint.asLinks();
                    return constraintFunctor("Links", links.exactness(), links.relation(), links.player(), links.role());
                }
                case Sub: {
                    Constraint.Sub sub = constraint.asSub();
                    return constraintFunctor("Sub", sub.exactness(), sub.subtype(), sub.supertype());
                }
                case Owns: {
                    Constraint.Owns owns = constraint.asOwns();
                    return constraintFunctor("Owns", owns.exactness(), owns.owner(), owns.attribute());
                }
                case Relates: {
                    Constraint.Relates relates = constraint.asRelates();
                    return constraintFunctor("Relates", relates.exactness(), relates.relation(), relates.role());
                }
                case Plays: {
                    Constraint.Plays plays = constraint.asPlays();
                    return constraintFunctor("Plays", plays.exactness(), plays.player(), plays.role());
                }
                case FunctionCall: {
                    Constraint.FunctionCall functionCall = constraint.asFunctionCall();
                    return makeFunctor(
                            "FunctionCall",
                            functionCall.name(),
                            encodeList(functionCall.assigned().map(this::encode)),
                            encodeList(functionCall.arguments().map(this::encode))
                    );
                }
                case Expression: {
                    Constraint.Expression expression = constraint.asExpression();
                    return makeFunctor(
                            "Expression",
                            expression.text(),
                            this.encode(expression.assigned()),
                            encodeList(expression.arguments().map(this::encode))
                    );
                }
                case Is: {
                    Constraint.Is is = constraint.asIs();
                    // Just pass Subtypes.
                    return makeFunctor("Is", encode(is.lhs()), encode(is.rhs()));
                }
                case Iid: {
                    Constraint.Iid iid = constraint.asIid();
                    return makeFunctor("Iid", encode(iid.variable()), iid.iid());
                }
                case DeleteConcepts: {
                    Constraint.DeleteConcepts deleteConcepts = constraint.asDeleteConcepts();
                    return makeFunctor("DeleteConcepts", encodeList(deleteConcepts.variables().map(this::encode)));
                }
                case Comparison: {
                    Constraint.Comparison comparison = constraint.asComparison();
                    return makeFunctor(
                            "Comparison",
                            encode(comparison.lhs()),
                            encode(comparison.rhs()),
                            comparison.comparatorSymbol()
                    );
                }
                case KindOf: {
                    Constraint.Kind kind = constraint.asKind();
                    return makeFunctor("Kind", kind.kind().toString(), encode(kind.type()));
                }
                case Label: {
                    Constraint.Label label = constraint.asLabel();
                    return makeFunctor("Label", encode(label.variable()), label.label());
                }
                case Value: {
                    Constraint.Value value = constraint.asValue();
                    return makeFunctor("Value",
                            encode(value.attributeType()),
                            value.valueType()
                    );
                }
                case IsSet: {
                    Constraint.IsSet isSet = constraint.asIsSet();
                    Stream<String> variables = isSet.variables().map(this::encode);
                    return makeFunctor("IsSet", encodeList(variables));
                }
                case Or: {
                    Constraint.Or or = constraint.asOr();
                    Stream<String> branchesEncoded = or.branches().map(conjunctionID -> {
                        Conjunction conjunction = pipeline.conjunction(conjunctionID).get();
                        return encode(conjunction);
                    });
                    return makeFunctor("Or", encodeList(branchesEncoded));
                }
                case Not: {
                    Constraint.Not not = constraint.asNot();
                    Conjunction conjunction = pipeline.conjunction(not.conjunction()).get();
                    return makeFunctor("Not", encode(conjunction));
                }
                case Try: {
                    Constraint.Try try_ = constraint.asTry();
                    Conjunction conjunction = pipeline.conjunction(try_.conjunction()).get();
                    return makeFunctor("Try", encode(conjunction));
                }
                default:
                    throw new IllegalArgumentException("Unhandled constraint variant: " + constraint.variant(), null);
            }
        }

        private String encode(Conjunction conjunction) {
            return encodeList(conjunction.constraints().map(this::encode));
        }

        public String encode(Pipeline pipeline) {
            return makeFunctor("Pipeline", encodeList(pipeline.stages().map(this::encode)));
        }

        public String encode(Function func) {
            return makeFunctor(
                    "Function",
                    encodeList(func.argument_variables().map(this::encode)),
                    encode(func.return_operation()),
                    encode(func.body())
            );
        }

        private String encode(Function.ReturnOperation returnOperation) {
            switch (returnOperation.variant()) {
                case StreamReturn:
                    return makeFunctor("Stream", encodeList(returnOperation.asStream().variables().map(this::encode)));
                case SingleReturn:
                    return makeFunctor("Single", returnOperation.asSingle().selector(), encodeList(returnOperation.asSingle().variables().map(this::encode)));
                case CheckReturn:
                    return makeFunctor("Check");
                case ReduceReturn:
                    return makeFunctor("Reduce", encodeList(returnOperation.asReduce().reducers().map(this::encode)));
                default:
                    throw new IllegalArgumentException("Unhandled return operation variant", null);
            }
        }

        private String encode(Reducer reducer) {
            return makeFunctor("Reducer", reducer.name(), encodeList(reducer.arguments().map(this::encode)));
        }

        public String encode(Given given) {
            return makeFunctor("Given", encodeList(given.variables().map(this::encode)));
        }
    }

    public static class AnnotationsEncoder extends FunctorEncoder {

        public AnnotationsEncoder(Pipeline pipeline) {
            super(pipeline);
        }

        public String encode(Pipeline pipeline) {
            return makeFunctor("Pipeline", encodeList(pipeline.stages().map(this::encode)));
        }

        public String encode(PipelineStage stage) {
            switch (stage.getVariant()) {
                case Match:
                    return makeFunctor("Match", encode(pipeline.conjunction(stage.asMatch().block()).get()));
                case Insert:
                    return makeFunctor("Insert", encode(pipeline.conjunction(stage.asInsert().block()).get()));

                case Put:
                    return makeFunctor("Put", encode(pipeline.conjunction(stage.asPut().block()).get()));

                case Update:
                    return makeFunctor("Update", encode(pipeline.conjunction(stage.asUpdate().block()).get()));
                case Delete:
                    return makeFunctor("Delete", encode(pipeline.conjunction(stage.asDelete().block()).get()));
                case Select:
                    return makeFunctor("Select");
                case Sort:
                    return makeFunctor("Sort");
                case Require:
                    return makeFunctor("Require");
                case Offset:
                    return makeFunctor("Offset");
                case Limit:
                    return makeFunctor("Limit");
                case Distinct:
                    return makeFunctor("Distinct");
                case Reduce:
                    return makeFunctor("Reduce");
                default:
                    throw new IllegalArgumentException("Unhandled stage variant", null);
            }
        }

        private String encode(VariableAnnotations variableAnnotations) {
            switch (variableAnnotations.variant()) {
                case InstanceAnnotations:
                    return FunctorEncoder.makeFunctor("Instance", FunctorEncoder.encodeList(variableAnnotations.asInstance().map(Concept::getLabel)));
                case TypeAnnotations:
                    return FunctorEncoder.makeFunctor("Type", FunctorEncoder.encodeList(variableAnnotations.asType().map(Concept::getLabel)));
                case ValueAnnotations:
                    return FunctorEncoder.makeFunctor("Value", FunctorEncoder.encodeList(variableAnnotations.asValue()));
                default:
                    throw new IllegalArgumentException("Unhandled return operation variant", null);
            }
        }

        private String encode(Conjunction conjunction) {
            Stream<String> trunkAnnotations = conjunction.annotated_variables()
                    .map(v -> this.encode(v) + ":" + encode(conjunction.variable_annotations(v)));
            String trunk = "{" + trunkAnnotations.sorted().collect(Collectors.joining(",")) + "}";
            Stream<String> branches = conjunction.constraints().map(constraint -> {
                switch (constraint.variant()) {
                    case Or:
                        return FunctorEncoder.makeFunctor(
                                "Or", FunctorEncoder.encodeList(constraint.asOr().branches().map(b -> encode(pipeline.conjunction(b).get())))
                        );
                    case Not:
                        return FunctorEncoder.makeFunctor("Not", encode(pipeline.conjunction(constraint.asNot().conjunction()).get()));
                    case Try:
                        return FunctorEncoder.makeFunctor("Try", encode(pipeline.conjunction(constraint.asNot().conjunction()).get()));
                    default:
                        return null;
                }
            }).filter(Objects::nonNull);
            return FunctorEncoder.makeFunctor("And", trunk, FunctorEncoder.encodeList(branches));
        }

        public String encode(Function func) {
            String returnStreamOrSingle = func.return_operation().variant() == ReturnOperationVariant.StreamReturn ? "Stream" : "Single";
            return FunctorEncoder.makeFunctor("Function",
                    FunctorEncoder.encodeList(func.argument_annotations().map(this::encode)),
                    FunctorEncoder.makeFunctor(returnStreamOrSingle, FunctorEncoder.encodeList(func.return_annotations().map(this::encode))),
                    encode(func.body())
            );
        }

        public String encode(Given given) {
            Stream<String> varAnnotations = given.variables()
                    .map(v -> this.encode(v) + ":" + encode(given.variable_annotations(v)));
            return makeFunctor("Given", "{" + varAnnotations.sorted().collect(Collectors.joining(",")) + "}");
        }

        public String encode(Fetch fetch) {
            switch (fetch.variant()) {
                case LeafDocument:
                    return FunctorEncoder.encodeList(fetch.asLeaf().annotations());
                case ListDocument:
                    return FunctorEncoder.makeFunctor("List", encode(fetch.asList().element()));
                case ObjectDocument:
                    Stream<String> fields = fetch.asObject().keys().map(field -> field + ":" + encode(fetch.asObject().get(field)));
                    return "{" + fields.sorted().collect(Collectors.joining(",")) + "}";
                default:
                    throw new IllegalArgumentException("Unhandled Fetch variant");
            }
        }
    }
}
