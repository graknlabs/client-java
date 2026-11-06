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

use typedb_protocol::{analyze::res::analyzed_query as analyze_proto, analyzed_conjunction as conjunction_proto};

use crate::{
    analyze::{
        AnalyzedQuery, Fetch, FetchLeaf, Function, Given, ReturnOperation, TypeAnnotations, VariableAnnotations,
        conjunction::{
            Comparator, Conjunction, ConjunctionID, Constraint, ConstraintExactness, ConstraintSpan, ConstraintVertex,
            ConstraintWithSpan, NamedRole, Variable,
        },
        pipeline::{Pipeline, PipelineStage, ReduceAssignment, Reducer, SortOrder, SortVariable, VariableInfo},
    },
    common::Result,
    concept::{Kind, Value, ValueType, type_},
    connection::{
        message::AnalyzeResponse,
        network::proto::{FromProto, TryFromProto},
    },
    error::{AnalyzeError, ServerError},
};

pub(super) fn expect_try_from_proto<Src, Dst: TryFromProto<Src>>(x: Option<Src>, field: &'static str) -> Result<Dst> {
    Dst::try_from_proto(x.ok_or_else(|| crate::Error::Analyze(AnalyzeError::MissingResponseField { field }))?)
}

fn vec_from_proto<Src, Dst>(protocol_vec: Vec<Src>) -> Result<Vec<Dst>>
where
    Dst: TryFromProto<Src>,
{
    protocol_vec.into_iter().map(|x| Dst::try_from_proto(x)).collect()
}

fn enum_from_proto<T: TryFrom<i32, Error = prost::UnknownEnumValue>>(as_i32: i32) -> Result<T> {
    T::try_from(as_i32)
        .map_err(|_| AnalyzeError::UnknownEnumValue { enum_name: std::any::type_name::<T>(), value: as_i32 }.into())
}

impl TryFromProto<typedb_protocol::analyze::Res> for AnalyzeResponse {
    fn try_from_proto(proto: typedb_protocol::analyze::Res) -> Result<Self> {
        expect_try_from_proto(proto.result, "analyze.Res.result")
    }
}

impl TryFromProto<typedb_protocol::analyze::res::Result> for AnalyzeResponse {
    fn try_from_proto(proto: typedb_protocol::analyze::res::Result) -> Result<Self> {
        use typedb_protocol::analyze::res::Result as ResultProto;
        let result = match proto {
            ResultProto::Ok(analyzed_query) => AnalyzeResponse::Ok(AnalyzedQuery::try_from_proto(analyzed_query)?),
            ResultProto::Err(err) => AnalyzeResponse::Err(ServerError::from_proto(err)),
        };
        Ok(result)
    }
}

impl TryFromProto<typedb_protocol::analyze::res::AnalyzedQuery> for AnalyzedQuery {
    fn try_from_proto(value: typedb_protocol::analyze::res::AnalyzedQuery) -> Result<Self> {
        let typedb_protocol::analyze::res::AnalyzedQuery { source, preamble, given, query, fetch } = value;
        Ok(Self {
            source,
            preamble: vec_from_proto(preamble)?,
            given: given.map(Given::try_from_proto).transpose()?,
            query: expect_try_from_proto(query, "AnalyzedQuery.query")?,
            fetch: fetch.map(Fetch::try_from_proto).transpose()?,
        })
    }
}

impl TryFromProto<analyze_proto::Function> for Function {
    fn try_from_proto(proto: analyze_proto::Function) -> Result<Self> {
        let analyze_proto::Function { body, arguments, return_operation, arguments_annotations, return_annotations } =
            proto;
        Ok(Self {
            argument_variables: vec_from_proto(arguments)?,
            return_operation: expect_try_from_proto(
                return_operation.and_then(|r| r.return_operation),
                "Function.return_operation",
            )?,
            body: expect_try_from_proto(body, "Function.body")?,
            argument_annotations: vec_from_proto(arguments_annotations)?,
            return_annotations: vec_from_proto(return_annotations)?,
        })
    }
}

impl TryFromProto<analyze_proto::function::return_operation::ReturnOperation> for ReturnOperation {
    fn try_from_proto(value: analyze_proto::function::return_operation::ReturnOperation) -> Result<Self> {
        use analyze_proto::function::return_operation::ReturnOperation as ReturnProto;
        let returns = match value {
            ReturnProto::Stream(stream) => Self::Stream { variables: vec_from_proto(stream.variables)? },
            ReturnProto::Single(single) => {
                Self::Single { selector: single.selector, variables: vec_from_proto(single.variables)? }
            }
            ReturnProto::Check(_check) => Self::Check {},
            ReturnProto::Reduce(reduce) => Self::Reduce { reducers: vec_from_proto(reduce.reducers)? },
        };
        Ok(returns)
    }
}

impl TryFromProto<analyze_proto::Pipeline> for Pipeline {
    fn try_from_proto(value: analyze_proto::Pipeline) -> Result<Self> {
        let analyze_proto::Pipeline { conjunctions, stages, variable_info, outputs } = value;
        let conjunctions = vec_from_proto(conjunctions)?;
        let stages = vec_from_proto(stages)?;
        let variable_info = variable_info
            .into_iter()
            .map(|(k, v)| Ok((Variable(k), VariableInfo::try_from_proto(v)?)))
            .collect::<Result<HashMap<_, _>>>()?;
        let outputs = vec_from_proto(outputs)?;
        Ok(Self { conjunctions, stages, variable_info, outputs })
    }
}

impl TryFromProto<analyze_proto::pipeline::VariableInfo> for VariableInfo {
    fn try_from_proto(proto: analyze_proto::pipeline::VariableInfo) -> Result<Self> {
        Ok(Self { name: proto.name })
    }
}

impl TryFromProto<analyze_proto::pipeline::PipelineStage> for PipelineStage {
    fn try_from_proto(value: analyze_proto::pipeline::PipelineStage) -> Result<Self> {
        use analyze_proto::pipeline::{pipeline_stage as stage_proto, pipeline_stage::Stage as StageProto};
        let unwrapped = value.stage.ok_or_else(|| {
            crate::Error::Analyze(AnalyzeError::MissingResponseField { field: "PipelineStage.stage" })
        })?;
        let stage = match unwrapped {
            StageProto::Match(stage_proto::Match { block }) => Self::Match { block: ConjunctionID(block as usize) },
            StageProto::Insert(stage_proto::Insert { block }) => Self::Insert { block: ConjunctionID(block as usize) },
            StageProto::Put(stage_proto::Put { block }) => Self::Put { block: ConjunctionID(block as usize) },
            StageProto::Update(stage_proto::Update { block }) => Self::Update { block: ConjunctionID(block as usize) },
            StageProto::Delete(stage_proto::Delete { block, deleted_variables }) => {
                let deleted_variables = vec_from_proto(deleted_variables)?;
                Self::Delete { block: ConjunctionID(block as usize), deleted_variables }
            }
            StageProto::Select(stage_proto::Select { variables }) => {
                Self::Select { variables: vec_from_proto(variables)? }
            }
            StageProto::Sort(stage_proto::Sort { sort_variables }) => {
                Self::Sort { variables: vec_from_proto(sort_variables)? }
            }
            StageProto::Require(stage_proto::Require { variables }) => {
                Self::Require { variables: vec_from_proto(variables)? }
            }
            StageProto::Offset(stage_proto::Offset { offset }) => Self::Offset { offset },
            StageProto::Limit(stage_proto::Limit { limit }) => Self::Limit { limit },
            StageProto::Distinct(stage_proto::Distinct {}) => Self::Distinct,
            StageProto::Reduce(stage_proto::Reduce { reducers, groupby }) => {
                let reducers = vec_from_proto(reducers)?;
                let groupby = vec_from_proto(groupby)?;
                Self::Reduce { reducers, groupby }
            }
        };
        Ok(stage)
    }
}

impl TryFromProto<analyze_proto::pipeline::pipeline_stage::reduce::ReduceAssign> for ReduceAssignment {
    fn try_from_proto(value: analyze_proto::pipeline::pipeline_stage::reduce::ReduceAssign) -> Result<Self> {
        Ok(Self {
            assigned: expect_try_from_proto(value.assigned, "ReduceAssign.assigned")?,
            reducer: expect_try_from_proto(value.reducer, "ReduceAssign.reducer")?,
        })
    }
}

impl TryFromProto<analyze_proto::Reducer> for Reducer {
    fn try_from_proto(value: analyze_proto::Reducer) -> Result<Self> {
        Ok(Self { arguments: vec_from_proto(value.variables)?, reducer: value.reducer })
    }
}

impl TryFromProto<conjunction_proto::Constraint> for ConstraintWithSpan {
    fn try_from_proto(proto: conjunction_proto::Constraint) -> Result<Self> {
        let constraint = expect_try_from_proto(proto.constraint, "Constraint.constraint")?;
        let span = proto.span.map(ConstraintSpan::try_from_proto).transpose()?;
        Ok(Self { constraint, span })
    }
}

impl TryFromProto<conjunction_proto::constraint::ConstraintSpan> for ConstraintSpan {
    fn try_from_proto(proto: conjunction_proto::constraint::ConstraintSpan) -> Result<Self> {
        Ok(Self { begin: proto.begin as usize, end: proto.end as usize })
    }
}

impl TryFromProto<conjunction_proto::constraint::Constraint> for Constraint {
    fn try_from_proto(proto: conjunction_proto::constraint::Constraint) -> Result<Self> {
        use conjunction_proto::{constraint as constraint_proto, constraint::Constraint as ConstraintProto};
        let constraint = match proto {
            ConstraintProto::Or(constraint_proto::Or { branches }) => {
                let branches = branches.iter().map(|branch| ConjunctionID(*branch as usize)).collect();
                Constraint::Or { branches }
            }
            ConstraintProto::Not(constraint_proto::Not { conjunction }) => {
                Constraint::Not { conjunction: ConjunctionID(conjunction as usize) }
            }
            ConstraintProto::Try(constraint_proto::Try { conjunction }) => {
                Constraint::Try { conjunction: ConjunctionID(conjunction as usize) }
            }
            ConstraintProto::Isa(constraint_proto::Isa { instance, r#type, exactness }) => {
                let exactness_proto = enum_from_proto::<constraint_proto::ConstraintExactness>(exactness)?;
                Constraint::Isa {
                    instance: expect_try_from_proto(instance, "structure_constraint::Isa.instance")?,
                    r#type: expect_try_from_proto(r#type, "structure_constraint::Isa.type")?,
                    exactness: ConstraintExactness::try_from_proto(exactness_proto)?,
                }
            }
            ConstraintProto::Has(constraint_proto::Has { owner, attribute, exactness }) => {
                let exactness_proto = enum_from_proto::<constraint_proto::ConstraintExactness>(exactness)?;
                Constraint::Has {
                    owner: expect_try_from_proto(owner, "structure_constraint::Has.owner")?,
                    attribute: expect_try_from_proto(attribute, "structure_constraint::Has.attribute")?,
                    exactness: ConstraintExactness::try_from_proto(exactness_proto)?,
                }
            }
            ConstraintProto::Links(constraint_proto::Links { relation, player, role, exactness }) => {
                let exactness_proto = enum_from_proto::<constraint_proto::ConstraintExactness>(exactness)?;
                Constraint::Links {
                    relation: expect_try_from_proto(relation, "structure_constraint::Links.relation")?,
                    player: expect_try_from_proto(player, "structure_constraint::Links.player")?,
                    role: expect_try_from_proto(role, "structure_constraint::Links.role")?,
                    exactness: ConstraintExactness::try_from_proto(exactness_proto)?,
                }
            }
            ConstraintProto::Kind(constraint_proto::Kind { kind, r#type }) => Constraint::Kind {
                kind: Kind::try_from_proto(kind)?,
                r#type: expect_try_from_proto(r#type, "structure_constraint::Kind.kind")?,
            },
            ConstraintProto::Sub(constraint_proto::Sub { subtype, supertype, exactness }) => {
                let exactness_proto = enum_from_proto::<constraint_proto::ConstraintExactness>(exactness)?;
                Constraint::Sub {
                    subtype: expect_try_from_proto(subtype, "structure_constraint::Sub.subtype")?,
                    supertype: expect_try_from_proto(supertype, "structure_constraint::Sub.supertype")?,
                    exactness: ConstraintExactness::try_from_proto(exactness_proto)?,
                }
            }
            ConstraintProto::Owns(constraint_proto::Owns { owner, attribute, exactness }) => {
                let exactness_proto = enum_from_proto::<constraint_proto::ConstraintExactness>(exactness)?;
                Constraint::Owns {
                    owner: expect_try_from_proto(owner, "structure_constraint::Owns.owner")?,
                    attribute: expect_try_from_proto(attribute, "structure_constraint::Owns.attribute")?,
                    exactness: ConstraintExactness::try_from_proto(exactness_proto)?,
                }
            }
            ConstraintProto::Relates(constraint_proto::Relates { relation, role, exactness }) => {
                let exactness_proto = enum_from_proto::<constraint_proto::ConstraintExactness>(exactness)?;
                Constraint::Relates {
                    relation: expect_try_from_proto(relation, "structure_constraint::Relates.relation")?,
                    role: expect_try_from_proto(role, "structure_constraint::Relates.role")?,
                    exactness: ConstraintExactness::try_from_proto(exactness_proto)?,
                }
            }
            ConstraintProto::Plays(constraint_proto::Plays { player, role, exactness }) => {
                let exactness_proto = enum_from_proto::<constraint_proto::ConstraintExactness>(exactness)?;
                Constraint::Plays {
                    player: expect_try_from_proto(player, "structure_constraint::Plays.player")?,
                    role: expect_try_from_proto(role, "structure_constraint::Plays.role")?,
                    exactness: ConstraintExactness::try_from_proto(exactness_proto)?,
                }
            }
            ConstraintProto::Value(constraint_proto::Value { attribute_type, value_type }) => Constraint::Value {
                attribute_type: expect_try_from_proto(attribute_type, "structure_constraint::Value.attribute_type")?,
                value_type: expect_try_from_proto(value_type, "structure_constraint::Value.value_type")?,
            },
            ConstraintProto::Label(constraint_proto::Label { r#type, label }) => {
                Constraint::Label { r#type: expect_try_from_proto(r#type, "structure_constraint::Label.type")?, label }
            }
            ConstraintProto::Comparison(constraint_proto::Comparison { lhs, rhs, comparator }) => {
                let comparator = enum_from_proto::<constraint_proto::comparison::Comparator>(comparator)?;
                Constraint::Comparison {
                    lhs: expect_try_from_proto(lhs, "structure_constraint::Comparison.lhs")?,
                    rhs: expect_try_from_proto(rhs, "structure_constraint::Comparison.rhs")?,
                    comparator: Comparator::try_from_proto(comparator)?,
                }
            }
            ConstraintProto::Expression(constraint_proto::Expression { assigned, arguments, text }) => {
                Constraint::Expression {
                    assigned: expect_try_from_proto(assigned, "Expression.assigned")?,
                    arguments: vec_from_proto(arguments)?,
                    text,
                }
            }
            ConstraintProto::FunctionCall(constraint_proto::FunctionCall { name, assigned, arguments }) => {
                Constraint::FunctionCall {
                    name,
                    assigned: vec_from_proto(assigned)?,
                    arguments: vec_from_proto(arguments)?,
                }
            }
            ConstraintProto::Is(constraint_proto::Is { lhs, rhs }) => Constraint::Is {
                lhs: expect_try_from_proto(lhs, "structure_constraint::Is.lhs")?,
                rhs: expect_try_from_proto(rhs, "structure_constraint::Is.rhs")?,
            },
            ConstraintProto::Iid(constraint_proto::Iid { concept, iid }) => Constraint::Iid {
                concept: expect_try_from_proto(concept, "structure_constraint::Iid.concept")?,
                iid: iid.into(),
            },
            ConstraintProto::DeleteConcepts(constraint_proto::DeleteConcepts { variables }) => {
                Constraint::DeleteConcepts { variables: vec_from_proto(variables)? }
            }
            ConstraintProto::IsSet(_) => todo!("IsSet constraint is not covered yet"),
        };
        Ok(constraint)
    }
}

impl TryFromProto<typedb_protocol::AnalyzedConjunction> for Conjunction {
    fn try_from_proto(proto: typedb_protocol::AnalyzedConjunction) -> Result<Self> {
        let variable_annotations = proto
            .variable_annotations
            .into_iter()
            .map(|(id, annotations_proto)| Ok((Variable(id), VariableAnnotations::try_from_proto(annotations_proto)?)))
            .collect::<Result<HashMap<_, _>>>()?;
        Ok(Self { constraints: vec_from_proto(proto.constraints)?, variable_annotations })
    }
}

impl TryFromProto<conjunction_proto::constraint::comparison::Comparator> for Comparator {
    fn try_from_proto(value: conjunction_proto::constraint::comparison::Comparator) -> Result<Self> {
        use conjunction_proto::constraint::comparison::Comparator as ComparatorProto;
        Ok(match value {
            ComparatorProto::Equal => Comparator::Equal,
            ComparatorProto::NotEqual => Comparator::NotEqual,
            ComparatorProto::Less => Comparator::LessThan,
            ComparatorProto::LessOrEqual => Comparator::LessOrEqual,
            ComparatorProto::Greater => Comparator::Greater,
            ComparatorProto::GreaterOrEqual => Comparator::GreaterOrEqual,
            ComparatorProto::Like => Comparator::Like,
            ComparatorProto::Contains => Comparator::Contains,
        })
    }
}

impl TryFromProto<conjunction_proto::constraint::ConstraintExactness> for ConstraintExactness {
    fn try_from_proto(value: conjunction_proto::constraint::ConstraintExactness) -> Result<Self> {
        use conjunction_proto::constraint::ConstraintExactness as ConstraintExactnessProto;
        Ok(match value {
            ConstraintExactnessProto::Exact => ConstraintExactness::Exact,
            ConstraintExactnessProto::Subtypes => ConstraintExactness::Subtypes,
        })
    }
}

impl TryFromProto<analyze_proto::pipeline::pipeline_stage::sort::SortVariable> for SortVariable {
    fn try_from_proto(value: analyze_proto::pipeline::pipeline_stage::sort::SortVariable) -> Result<Self> {
        use analyze_proto::pipeline::pipeline_stage::sort::sort_variable::SortDirection;
        Ok(Self {
            variable: expect_try_from_proto(value.variable, "SortVariable.variable")?,
            order: match enum_from_proto(value.direction)? {
                SortDirection::Asc => SortOrder::Ascending,
                SortDirection::Desc => SortOrder::Descending,
            },
        })
    }
}

impl TryFromProto<conjunction_proto::constraint_vertex::NamedRole> for NamedRole {
    fn try_from_proto(proto: conjunction_proto::constraint_vertex::NamedRole) -> Result<Self> {
        let variable = expect_try_from_proto(proto.variable, "NamedRole.variable")?;
        let name = proto.name;
        Ok(NamedRole { variable, name })
    }
}

impl TryFromProto<conjunction_proto::ConstraintVertex> for ConstraintVertex {
    fn try_from_proto(value: conjunction_proto::ConstraintVertex) -> Result<Self> {
        use conjunction_proto::constraint_vertex::Vertex as VertexProto;
        match value.vertex {
            Some(VertexProto::Variable(variable)) => {
                Ok(ConstraintVertex::Variable(Variable::try_from_proto(variable)?))
            }
            Some(VertexProto::Label(type_)) => Ok(ConstraintVertex::Label(type_::Type::try_from_proto(type_)?)),
            Some(VertexProto::Value(value)) => Ok(ConstraintVertex::Value(Value::try_from_proto(value)?)),
            Some(VertexProto::NamedRole(role)) => Ok(ConstraintVertex::NamedRole(NamedRole::try_from_proto(role)?)),
            None => Err(AnalyzeError::MissingResponseField { field: "StructureVertex.vertex" }.into()),
        }
    }
}

impl TryFromProto<conjunction_proto::Variable> for Variable {
    fn try_from_proto(value: conjunction_proto::Variable) -> Result<Self> {
        Ok(Self(value.id))
    }
}

// Annotations

impl TryFromProto<conjunction_proto::VariableAnnotations> for VariableAnnotations {
    fn try_from_proto(proto: conjunction_proto::VariableAnnotations) -> Result<Self> {
        Ok(Self {
            is_optional: proto.is_optional,
            types: expect_try_from_proto(proto.annotations, "VariableAnnotations.annotations")?,
        })
    }
}

impl TryFromProto<conjunction_proto::variable_annotations::Annotations> for TypeAnnotations {
    fn try_from_proto(proto: conjunction_proto::variable_annotations::Annotations) -> Result<Self> {
        use conjunction_proto::variable_annotations::Annotations as AnnotationsProto;
        Ok(match proto {
            AnnotationsProto::Instance(types) => Self::Instance(vec_from_proto(types.types)?),
            AnnotationsProto::Type(types) => Self::Type(vec_from_proto(types.types)?),
            AnnotationsProto::ValueAnnotations(value_type) => {
                let value_type_proto = value_type.value_type.ok_or_else(|| {
                    crate::Error::Analyze(AnalyzeError::MissingResponseField {
                        field: "Value.value_type in VariableAnnotations::ValueAnnotations",
                    })
                })?;
                Self::Value(ValueType::from_proto(value_type_proto))
            }
        })
    }
}

impl TryFromProto<analyze_proto::AnalyzedGiven> for Given {
    fn try_from_proto(proto: analyze_proto::AnalyzedGiven) -> Result<Self> {
        let variables = vec_from_proto(proto.variables)?;
        let variable_annotations = proto
            .variable_annotations
            .into_iter()
            .map(|(id, annotations_proto)| Ok((Variable(id), VariableAnnotations::try_from_proto(annotations_proto)?)))
            .collect::<Result<HashMap<_, _>>>()?;
        Ok(Self { variables, variable_annotations })
    }
}
impl TryFromProto<analyze_proto::Fetch> for Fetch {
    fn try_from_proto(proto: analyze_proto::Fetch) -> Result<Self> {
        use analyze_proto::fetch::Node as NodeProto;
        let unwrapped = proto
            .node
            .ok_or_else(|| crate::Error::Analyze(AnalyzeError::MissingResponseField { field: "Fetch.node" }))?;
        let fetch_annotations = match unwrapped {
            NodeProto::Object(object) => {
                let fields = object
                    .fetch
                    .into_iter()
                    .map(|(key, annotations)| Ok((key, Fetch::try_from_proto(annotations)?)))
                    .collect::<Result<HashMap<_, _>>>()?;
                Fetch::Object(fields)
            }
            NodeProto::List(elements) => Fetch::List(Box::new(Fetch::try_from_proto(*elements)?)),
            NodeProto::Leaf(leaf) => Fetch::Leaf(FetchLeaf::try_from_proto(leaf)?),
        };
        Ok(fetch_annotations)
    }
}

impl TryFromProto<analyze_proto::fetch::Leaf> for FetchLeaf {
    fn try_from_proto(proto: analyze_proto::fetch::Leaf) -> Result<Self> {
        Ok(FetchLeaf { annotations: vec_from_proto(proto.annotations)? })
    }
}
