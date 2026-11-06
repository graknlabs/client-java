# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

# Keys are suffixes of the fully qualified class name. The longest match is picked.
# The filename will be className.adoc
dir_mapping = {
    # Analyze
    "AnalyzedQuery.adoc": "analyze",
    "Comparator.adoc": "analyze",
    "Comparison.adoc": "analyze",
    "Conjunction.adoc": "analyze",
    "Constraint.adoc": "analyze",
    "ConstraintExactness.adoc": "analyze",
    "ConstraintVariant.adoc": "analyze",
    "ConstraintVertex.adoc": "analyze",
    "DeleteConcepts.adoc": "analyze",
    "DeleteStage.adoc": "analyze",
    "DistinctStage.adoc": "analyze",
    "Expression.adoc": "analyze",
    "Fetch.adoc": "analyze",
    "FetchLeaf.adoc": "analyze",
    "FetchList.adoc": "analyze",
    "FetchObject.adoc": "analyze",
    "FetchVariant.adoc": "analyze",
    "Function.adoc": "analyze",
    "FunctionCall.adoc": "analyze",
    "Given.adoc" : "analyze",
    "Has.adoc": "analyze",
    "Iid.adoc": "analyze",
    "InsertStage.adoc": "analyze",
    "Is.adoc": "analyze",
    "Isa.adoc": "analyze",
    "IsSet.adoc": "analyze",
    "constraint.Kind.adoc": "analyze",
    "enums.Kind.adoc": "concept",
    "KindVariant.adoc": "analyze",
    "Label.adoc": "analyze",
    "LimitStage.adoc": "analyze",
    "Links.adoc": "analyze",
    "MatchStage.adoc": "analyze",
    "NamedRole.adoc": "analyze",
    "Not.adoc": "analyze",
    "OffsetStage.adoc": "analyze",
    "Or.adoc": "analyze",
    "Owns.adoc": "analyze",
    "Pipeline.adoc": "analyze",
    "PipelineStage.adoc": "analyze",
    "PipelineStageVariant.adoc": "analyze",
    "Plays.adoc": "analyze",
    "PutStage.adoc": "analyze",
    "ReduceAssignment.adoc": "analyze",
    "ReduceStage.adoc": "analyze",
    "ReduceStage.ReduceAssignment.adoc": "analyze",
    "Reducer.adoc": "analyze",
    "Relates.adoc": "analyze",
    "RequireStage.adoc": "analyze",
    "ReturnOperation.adoc": "analyze",
    "ReturnOperationCheck.adoc": "analyze",
    "ReturnOperationReduce.adoc": "analyze",
    "ReturnOperationSingle.adoc": "analyze",
    "ReturnOperationStream.adoc": "analyze",
    "ReturnOperationVariant.adoc": "analyze",
    "ReturnOperation.Reduce.ReduceAssignment.adoc": "analyze",
    "SelectStage.adoc": "analyze",
    "SortOrder.adoc": "analyze",
    "SortOrderVariant.adoc": "analyze",
    "SortStage.adoc": "analyze",
    "SortStage.SortVariable.adoc": "analyze",
    "SortVariable.adoc": "analyze",
    "Span.adoc": "analyze",
    "Sub.adoc": "analyze",
    "Try.adoc": "analyze",
    "TypeAnnotations.adoc": "analyze",
    "UpdateStage.adoc": "analyze",
    "analyze.constraint.Value.adoc": "analyze",  # Longer suffix match to avoid ambiguity with the Value concept
    "VariableAnnotations.adoc": "analyze",
    "VariableAnnotationsVariant.adoc": "analyze",

    # Answer
    "ConceptDocument.adoc": "answer",
    "ConceptDocumentIterator.adoc": "answer",
    "ConceptRow.adoc": "answer",
    "ConceptRowIterator.adoc": "answer",
    "OkQueryAnswer.adoc": "answer",
    "Promise.adoc": "answer",
    "QueryAnswer.adoc": "answer",
    "QueryType.adoc": "answer",
    "ValueGroup.adoc": "answer",

    # Concept
    "Concept.adoc": "concept",

    # Connection
    "Auto.adoc": "connection",
    "Credentials.adoc": "connection",
    "Database.adoc": "connection",
    "DatabaseManager.adoc": "connection",
    "Direct.adoc": "connection",
    "Driver.adoc": "connection",
    "DriverOptions.adoc": "connection",
    "DriverTlsConfig.adoc": "connection",
    "ReplicationRole.adoc": "connection",
    "Server.adoc": "connection",
    "ServerRouting.adoc": "connection",
    "ServerVersion.adoc": "connection",
    "TypeDB.adoc": "connection",
    "User.adoc": "connection",
    "UserManager.adoc": "connection",

    # Data
    "Attribute.adoc": "data",
    "Entity.adoc": "data",
    "Instance.adoc": "data",
    "Relation.adoc": "data",
    "Value.adoc": "data",
    "TypeDB.Concept.adoc": "data",
    "GivenRows.adoc": "data",

    # Errors
    "TypeDBDriverException.adoc": "errors",

    # Schema
    "AttributeType.adoc": "schema",
    "EntityType.adoc": "schema",
    "RelationType.adoc": "schema",
    "RoleType.adoc": "schema",
    "Type.adoc": "schema",

    # Transaction
    "QueryOptions.adoc": "transaction",
    "Transaction.adoc": "transaction",
    "TransactionOptions.adoc": "transaction",
    "TransactionType.adoc": "transaction",

    # Value
    "Datetime.adoc": "value",
    "Duration.adoc": "value",
}
