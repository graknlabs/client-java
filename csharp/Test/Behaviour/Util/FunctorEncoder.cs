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
using System.Text.RegularExpressions;

using TypeDB.Driver.Api;
using TypeDB.Driver.Api.Analyze;

using Pinvoke = TypeDB.Driver.Pinvoke;

namespace TypeDB.Driver.Test.Behaviour
{
    /// <summary>
    /// Encodes analyzed query structures into a functor-based string representation for comparison in tests.
    /// </summary>
    public abstract class FunctorEncoder
    {
        protected readonly IPipeline Pipeline;

        protected FunctorEncoder(IPipeline pipeline)
        {
            Pipeline = pipeline;
        }

        private static string EncodeList(IEnumerable<string> elements)
        {
            return $"[{string.Join(",", elements)}]";
        }

        private static string MakeFunctor(string functor, params string[] encodedArgs)
        {
            return $"{functor}({string.Join(",", encodedArgs)})";
        }

        public static string NormalizeForCompare(string functor)
        {
            return Regex.Replace(functor.ToLower(), @"\s+", "");
        }

        // The common variable encoding
        public string Encode(IVariable variable)
        {
            var name = Pipeline.GetVariableName(variable);
            return "$" + (name ?? "_");
        }

        /// <summary>
        /// Encodes the structure of analyzed queries.
        /// </summary>
        public class StructureEncoder : FunctorEncoder
        {
            public StructureEncoder(IPipeline pipeline) : base(pipeline) { }

            private string ConstraintFunctor(string variant, Pinvoke.ConstraintExactness exactness, params IConstraintVertex[] args)
            {
                string variantWithExactness = exactness == Pinvoke.ConstraintExactness.Exact ? variant + "Exact" : variant;
                return MakeFunctor(variantWithExactness, args.Select(Encode).ToArray());
            }

            private string Encode(IPipelineStage stage)
            {
                if (stage.IsMatch)
                {
                    var conjunction = Pipeline.GetConjunction(stage.AsMatch().Block);
                    return MakeFunctor("Match", Encode(conjunction!));
                }
                else if (stage.IsInsert)
                {
                    var conjunction = Pipeline.GetConjunction(stage.AsInsert().Block);
                    return MakeFunctor("Insert", Encode(conjunction!));
                }
                else if (stage.IsPut)
                {
                    var conjunction = Pipeline.GetConjunction(stage.AsPut().Block);
                    return MakeFunctor("Put", Encode(conjunction!));
                }
                else if (stage.IsUpdate)
                {
                    var conjunction = Pipeline.GetConjunction(stage.AsUpdate().Block);
                    return MakeFunctor("Update", Encode(conjunction!));
                }
                else if (stage.IsDelete)
                {
                    var deleteStage = stage.AsDelete();
                    var conjunction = Pipeline.GetConjunction(deleteStage.Block);
                    return MakeFunctor("Delete",
                        EncodeList(deleteStage.DeletedVariables.Select(Encode)),
                        Encode(conjunction!));
                }
                else if (stage.IsSelect)
                {
                    return MakeFunctor("Select", EncodeList(stage.AsSelect().Variables.Select(Encode)));
                }
                else if (stage.IsSort)
                {
                    return MakeFunctor("Sort", EncodeList(stage.AsSort().Variables.Select(Encode)));
                }
                else if (stage.IsRequire)
                {
                    return MakeFunctor("Require", EncodeList(stage.AsRequire().Variables.Select(Encode)));
                }
                else if (stage.IsOffset)
                {
                    return MakeFunctor("Offset", stage.AsOffset().Offset.ToString());
                }
                else if (stage.IsLimit)
                {
                    return MakeFunctor("Limit", stage.AsLimit().Limit.ToString());
                }
                else if (stage.IsDistinct)
                {
                    return MakeFunctor("Distinct");
                }
                else if (stage.IsReduce)
                {
                    var reduceStage = stage.AsReduce();
                    return MakeFunctor("Reduce",
                        EncodeList(reduceStage.ReducerAssignments.Select(Encode)),
                        EncodeList(reduceStage.GroupBy.Select(Encode)));
                }
                else
                {
                    throw new ArgumentException($"Unhandled stage variant: {stage.Variant}");
                }
            }

            private string Encode(IReduceAssignment reduceAssignment)
            {
                return MakeFunctor("ReduceAssign", Encode(reduceAssignment.Assigned), Encode(reduceAssignment.Reducer));
            }

            private string Encode(IReducer reducer)
            {
                return MakeFunctor("Reducer", reducer.Name, EncodeList(reducer.Arguments.Select(Encode)));
            }

            private string Encode(ISortVariable sortVariable)
            {
                string orderStr = sortVariable.Order == Pinvoke.SortOrder.Ascending ? "Asc" : "Desc";
                return MakeFunctor(orderStr, Encode(sortVariable.Variable));
            }

            public string Encode(IConstraintVertex vertex)
            {
                if (vertex.IsVariable)
                {
                    return Encode(vertex.AsVariable());
                }
                else if (vertex.IsLabel)
                {
                    return vertex.AsLabel().GetLabel();
                }
                else if (vertex.IsValue)
                {
                    var value = vertex.AsValue();
                    if (value.IsString())
                    {
                        return "\"" + value.GetString() + "\"";
                    }
                    return value.ToString()!;
                }
                else if (vertex.IsNamedRole)
                {
                    return vertex.AsNamedRole().Name;
                }
                else
                {
                    throw new ArgumentException("Unexpected ConstraintVertex variant");
                }
            }

            public string Encode(IConstraint constraint)
            {
                if (constraint.IsIsa)
                {
                    var isa = constraint.AsIsa();
                    return ConstraintFunctor("Isa", isa.Exactness, isa.Instance, isa.Type);
                }
                else if (constraint.IsHas)
                {
                    var has = constraint.AsHas();
                    return ConstraintFunctor("Has", has.Exactness, has.Owner, has.Attribute);
                }
                else if (constraint.IsLinks)
                {
                    var links = constraint.AsLinks();
                    return ConstraintFunctor("Links", links.Exactness, links.Relation, links.Player, links.Role);
                }
                else if (constraint.IsSub)
                {
                    var sub = constraint.AsSub();
                    return ConstraintFunctor("Sub", sub.Exactness, sub.Subtype, sub.Supertype);
                }
                else if (constraint.IsOwns)
                {
                    var owns = constraint.AsOwns();
                    return ConstraintFunctor("Owns", owns.Exactness, owns.Owner, owns.Attribute);
                }
                else if (constraint.IsRelates)
                {
                    var relates = constraint.AsRelates();
                    return ConstraintFunctor("Relates", relates.Exactness, relates.Relation, relates.Role);
                }
                else if (constraint.IsPlays)
                {
                    var plays = constraint.AsPlays();
                    return ConstraintFunctor("Plays", plays.Exactness, plays.Player, plays.Role);
                }
                else if (constraint.IsFunctionCall)
                {
                    var functionCall = constraint.AsFunctionCall();
                    return MakeFunctor("FunctionCall", functionCall.Name,
                        EncodeList(functionCall.Assigned.Select(Encode)),
                        EncodeList(functionCall.Arguments.Select(Encode)));
                }
                else if (constraint.IsExpression)
                {
                    var expression = constraint.AsExpression();
                    return MakeFunctor("Expression", expression.Text, Encode(expression.Assigned),
                        EncodeList(expression.Arguments.Select(Encode)));
                }
                else if (constraint.IsIs)
                {
                    var isConstraint = constraint.AsIs();
                    return MakeFunctor("Is", Encode(isConstraint.Lhs), Encode(isConstraint.Rhs));
                }
                else if (constraint.IsIid)
                {
                    var iid = constraint.AsIid();
                    return MakeFunctor("Iid", Encode(iid.Variable), iid.Iid);
                }
                else if (constraint.IsDeleteConcepts)
                {
                    var deleteConcepts = constraint.AsDeleteConcepts();
                    return MakeFunctor("DeleteConcepts", EncodeList(deleteConcepts.Variables.Select(Encode)));
                }
                else if (constraint.IsComparison)
                {
                    var comparison = constraint.AsComparison();
                    return MakeFunctor("Comparison", Encode(comparison.Lhs), Encode(comparison.Rhs), comparison.ComparatorSymbol);
                }
                else if (constraint.IsKind)
                {
                    var kind = constraint.AsKind();
                    return MakeFunctor("Kind", kind.Kind.ToString(), Encode(kind.Type));
                }
                else if (constraint.IsLabel)
                {
                    var label = constraint.AsLabel();
                    return MakeFunctor("Label", Encode(label.Variable), label.LabelValue);
                }
                else if (constraint.IsValue)
                {
                    var value = constraint.AsValue();
                    return MakeFunctor("Value", Encode(value.AttributeType), value.ValueType);
                }
                else if (constraint.IsIsSet)
                {
                    var isSet = constraint.AsIsSet();
                    return MakeFunctor("IsSet", EncodeList(isSet.Variables.Select(Encode)));
                }
                else if (constraint.IsOr)
                {
                    var or = constraint.AsOr();
                    var branchesEncoded = or.Branches.Select(conjunctionId =>
                    {
                        var conjunction = Pipeline.GetConjunction(conjunctionId);
                        return Encode(conjunction!);
                    });
                    return MakeFunctor("Or", EncodeList(branchesEncoded));
                }
                else if (constraint.IsNot)
                {
                    var not = constraint.AsNot();
                    var conjunction = Pipeline.GetConjunction(not.Conjunction);
                    return MakeFunctor("Not", Encode(conjunction!));
                }
                else if (constraint.IsTry)
                {
                    var tryConstraint = constraint.AsTry();
                    var conjunction = Pipeline.GetConjunction(tryConstraint.Conjunction);
                    return MakeFunctor("Try", Encode(conjunction!));
                }
                else
                {
                    throw new ArgumentException($"Unhandled constraint variant: {constraint.Variant}");
                }
            }

            private string Encode(IConjunction conjunction)
            {
                return EncodeList(conjunction.Constraints.Select(Encode));
            }

            public string Encode(IPipeline pipeline)
            {
                return MakeFunctor("Pipeline", EncodeList(pipeline.Stages.Select(Encode)));
            }

            public string Encode(IFunction func)
            {
                return MakeFunctor("Function",
                    EncodeList(func.ArgumentVariables.Select(Encode)),
                    Encode(func.ReturnOperation),
                    Encode(func.Body));
            }

            public string Encode(IGiven given)
            {
                return MakeFunctor("Given", EncodeList(given.Variables.Select(Encode)));
            }

            private string Encode(IReturnOperation returnOp)
            {
                if (returnOp.IsStream)
                {
                    return MakeFunctor("Stream", EncodeList(returnOp.AsStream().Variables.Select(Encode)));
                }
                else if (returnOp.IsSingle)
                {
                    var single = returnOp.AsSingle();
                    return MakeFunctor("Single", single.Selector, EncodeList(single.Variables.Select(Encode)));
                }
                else if (returnOp.IsCheck)
                {
                    return MakeFunctor("Check");
                }
                else if (returnOp.IsReduce)
                {
                    return MakeFunctor("Reduce", EncodeList(returnOp.AsReduce().Reducers.Select(Encode)));
                }
                else
                {
                    throw new ArgumentException($"Unhandled return operation variant: {returnOp.Variant}");
                }
            }
        }

        /// <summary>
        /// Encodes the annotations of analyzed queries.
        /// </summary>
        public class AnnotationsEncoder : FunctorEncoder
        {
            public AnnotationsEncoder(IPipeline pipeline) : base(pipeline) { }

            public string Encode(IPipeline pipeline)
            {
                return MakeFunctor("Pipeline", EncodeList(pipeline.Stages.Select(Encode)));
            }

            public string Encode(IPipelineStage stage)
            {
                if (stage.IsMatch)
                {
                    var conjunction = Pipeline.GetConjunction(stage.AsMatch().Block);
                    return MakeFunctor("Match", Encode(conjunction!));
                }
                else if (stage.IsInsert)
                {
                    var conjunction = Pipeline.GetConjunction(stage.AsInsert().Block);
                    return MakeFunctor("Insert", Encode(conjunction!));
                }
                else if (stage.IsPut)
                {
                    var conjunction = Pipeline.GetConjunction(stage.AsPut().Block);
                    return MakeFunctor("Put", Encode(conjunction!));
                }
                else if (stage.IsUpdate)
                {
                    var conjunction = Pipeline.GetConjunction(stage.AsUpdate().Block);
                    return MakeFunctor("Update", Encode(conjunction!));
                }
                else if (stage.IsDelete)
                {
                    var conjunction = Pipeline.GetConjunction(stage.AsDelete().Block);
                    return MakeFunctor("Delete", Encode(conjunction!));
                }
                else if (stage.IsSelect)
                {
                    return MakeFunctor("Select");
                }
                else if (stage.IsSort)
                {
                    return MakeFunctor("Sort");
                }
                else if (stage.IsRequire)
                {
                    return MakeFunctor("Require");
                }
                else if (stage.IsOffset)
                {
                    return MakeFunctor("Offset");
                }
                else if (stage.IsLimit)
                {
                    return MakeFunctor("Limit");
                }
                else if (stage.IsDistinct)
                {
                    return MakeFunctor("Distinct");
                }
                else if (stage.IsReduce)
                {
                    return MakeFunctor("Reduce");
                }
                else
                {
                    throw new ArgumentException($"Unhandled stage variant: {stage.Variant}");
                }
            }

            private string Encode(IVariableAnnotations variableAnnotations)
            {
                if (variableAnnotations.IsInstance)
                {
                    return MakeFunctor("Instance", EncodeList(variableAnnotations.AsInstance().Select(t => t.GetLabel())));
                }
                else if (variableAnnotations.IsType)
                {
                    return MakeFunctor("Type", EncodeList(variableAnnotations.AsType().Select(t => t.GetLabel())));
                }
                else if (variableAnnotations.IsValue)
                {
                    return MakeFunctor("Value", EncodeList(variableAnnotations.AsValue()));
                }
                else
                {
                    throw new ArgumentException($"Unhandled variable annotations variant: {variableAnnotations.Variant}");
                }
            }

            private string Encode(IConjunction conjunction)
            {
                var trunkAnnotations = conjunction.AnnotatedVariables
                    .Select(v => base.Encode(v) + ":" + Encode(conjunction.GetVariableAnnotations(v)))
                    .OrderBy(x => x);
                string trunk = "{" + string.Join(",", trunkAnnotations) + "}";

                var branches = conjunction.Constraints
                    .Select(constraint =>
                    {
                        if (constraint.IsOr)
                        {
                            return MakeFunctor("Or", EncodeList(constraint.AsOr().Branches.Select(b =>
                            {
                                var conj = Pipeline.GetConjunction(b);
                                return Encode(conj!);
                            })));
                        }
                        else if (constraint.IsNot)
                        {
                            var conj = Pipeline.GetConjunction(constraint.AsNot().Conjunction);
                            return MakeFunctor("Not", Encode(conj!));
                        }
                        else if (constraint.IsTry)
                        {
                            var conj = Pipeline.GetConjunction(constraint.AsTry().Conjunction);
                            return MakeFunctor("Try", Encode(conj!));
                        }
                        else
                        {
                            return null;
                        }
                    })
                    .Where(x => x != null)
                    .Select(x => x!);

                return MakeFunctor("And", trunk, EncodeList(branches));
            }

            public string Encode(IFunction func)
            {
                string returnStreamOrSingle = func.ReturnOperation.Variant == Pinvoke.ReturnOperationVariant.StreamReturn ? "Stream" : "Single";
                return MakeFunctor("Function",
                    EncodeList(func.ArgumentAnnotations.Select(Encode)),
                    MakeFunctor(returnStreamOrSingle, EncodeList(func.ReturnAnnotations.Select(Encode))),
                    Encode(func.Body));
            }

            public string Encode(IGiven given)
            {
                var varAnnotations = given.Variables
                    .Select(v => base.Encode(v) + ":" + Encode(given.GetVariableAnnotations(v)))
                    .OrderBy(x => x);
                return MakeFunctor("Given", "{" + string.Join(",", varAnnotations) + "}");
            }

            public string Encode(IFetch fetch)
            {
                if (fetch.IsLeaf)
                {
                    return EncodeList(fetch.AsLeaf().Annotations);
                }
                else if (fetch.IsList)
                {
                    return MakeFunctor("List", Encode(fetch.AsList().Element));
                }
                else if (fetch.IsObject)
                {
                    var obj = fetch.AsObject();
                    var fields = obj.Keys.Select(field => field + ":" + Encode(obj.Get(field))).OrderBy(x => x);
                    return "{" + string.Join(",", fields) + "}";
                }
                else
                {
                    throw new ArgumentException($"Unhandled Fetch variant: {fetch.Variant}");
                }
            }
        }
    }
}
