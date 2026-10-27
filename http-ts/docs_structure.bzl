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

static_function_mapping = {
    "getVariableName": "analyze",

    "isApiError": "response",
    "isApiErrorResponse": "response",
    "isMisdirectedError": "response",
    "isOkResponse": "response",
    "parseJson": "response",
    "stringifyJson": "response",
    "isQueryResponseLegacy": "legacy",
    "driverError": "response",

    "isBasicParams": "connection",
    "isTranslatedParams": "connection",
    "remoteOrigin": "connection",
    "hostPortFromOrigin": "connection",
    "resolveOrigin": "connection",
    "allOrigins": "connection",
}

class_nesting_prefixes = [
    "DriverParams",
    "Constraint",
    "ConstraintVertex",
]


dir_mapping = {
    "TypeDBHttpDriver.adoc": "connection",

    "AnalyzeOptions.adoc": "analyze",
    "AnalyzedFetch.adoc": "analyze",
    "AnalyzedFunction.adoc": "analyze",
    "AnalyzedPipeline.adoc": "analyze",
    "AnalyzedConjunction.adoc": "analyze",
    "FunctionSingleReturnSelector.adoc": "analyze",
    "FunctionReturnStructure.adoc": "analyze",
    "FetchAnnotationFieldEntry.adoc": "analyze",
    "Given.adoc": "analyze",
    "PipelineStage.adoc": "analyze",
    "Reducer.adoc": "analyze",
    "ConjunctionAnnotations.adoc": "analyze",
    "VariableAnnotations.adoc": "analyze",

    "QueryStructureForStudio.adoc": "analyze",
    "QueryConstraintAnyForStudio.adoc": "analyze",
    "QueryConstraintExpressionForStudio.adoc": "analyze",
    "QueryConstraintLinksForStudio.adoc": "analyze",
    "ConceptRowsQueryResponseForStudio.adoc": "analyze",


    "AnalyzeResponse.adoc": "response",
    "ApiErrorResponse.adoc": "response",
    "Attribute.adoc": "concept",
    "ConceptDocumentsQueryResponse.adoc": "response",
    "ConceptRow.adoc": "response",
    "ConceptRowAnswer.adoc": "response",
    "ConceptRowsQueryResponse.adoc": "response",
    "Database.adoc": "connection",
    "DatabasesListResponse.adoc": "response",
    "DriverParamsBasic.adoc": "connection",
    "DriverParamsTranslated.adoc": "connection",
    "DriverTlsConfig.adoc": "connection",
    "Entity.adoc": "concept",
    "EntityType.adoc": "concept",
    "OkQueryResponse.adoc": "response",
    "ConstraintSpan.adoc": "analyze",
    "ConstraintAny.adoc": "analyze",
    "ConstraintComparison.adoc": "analyze",
    "ConstraintDeleteConcepts.adoc": "analyze",
    "ConstraintExpression.adoc": "analyze",
    "ConstraintFunction.adoc": "analyze",
    "ConstraintHas.adoc": "analyze",
    "ConstraintIid.adoc": "analyze",
    "ConstraintIs.adoc": "analyze",
    "ConstraintIsa.adoc": "analyze",
    "ConstraintIsaExact.adoc": "analyze",
    "ConstraintKind.adoc": "analyze",
    "ConstraintLabel.adoc": "analyze",
    "ConstraintLinks.adoc": "analyze",
    "ConstraintNot.adoc": "analyze",
    "ConstraintOr.adoc": "analyze",
    "ConstraintOwns.adoc": "analyze",
    "ConstraintPlays.adoc": "analyze",
    "ConstraintRelates.adoc": "analyze",
    "ConstraintSub.adoc": "analyze",
    "ConstraintSubExact.adoc": "analyze",
    "ConstraintTry.adoc": "analyze",
    "ConstraintValue.adoc": "analyze",

    "ConstraintExpressionLegacy.adoc": "legacy",
    "ConstraintLinksLegacy.adoc": "legacy",
    "ConceptRowsQueryResponseLegacy.adoc": "legacy",
    "QueryConjunctionLegacy.adoc": "legacy",
    "QueryStructureLegacy.adoc": "legacy",

    "QueryOptions.adoc": "response",
    "QueryResponseBase.adoc": "response",
    "GivenRowEntry.adoc": "response",
    "GivenRows.adoc": "response",
    "ConstraintVertexAny.adoc": "analyze",
    "ConstraintVertexLabel.adoc": "analyze",
    "ConstraintVertexNamedRole.adoc": "analyze",
    "ConstraintVertexValue.adoc": "analyze",
    "ConstraintVertexVariable.adoc": "analyze",
    "VariableId.adoc": "analyze",
    "VariableInfo.adoc": "analyze",

    "MisdirectedError.adoc": "response",
    "Relation.adoc": "concept",
    "RelationType.adoc": "concept",
    "ReplicationRole.adoc": "connection",
    "ReplicationStatus.adoc": "connection",
    "RoleType.adoc": "concept",
    "SignInResponse.adoc": "response",
    "TransactionOpenResponse.adoc": "response",
    "ServersListResponse.adoc": "response",
    "Server.adoc": "connection",
    "TransactionOptions.adoc": "connection",
    "TranslatedAddress.adoc": "connection",
    "User.adoc": "connection",
    "UsersListResponse.adoc": "response",
    "Value.adoc": "concept",
    "VersionResponse.adoc": "response",

    "Answer.adoc": "response",
    "AnswerType.adoc": "response",
    "ApiError.adoc": "response",
    "ApiOkResponse.adoc": "response",
    "ApiResponse.adoc": "response",
    "AttributeType.adoc": "concept",
    "Concept.adoc": "concept",
    "ConceptDocument.adoc": "concept",
    "Distribution.adoc": "response",
    "DriverParams.adoc": "connection",
    "EdgeKind.adoc": "concept",
    "InstantiableType.adoc": "concept",
    "QueryResponse.adoc": "response",
    "QueryType.adoc": "response",
    "ThingKind.adoc": "concept",
    "TransactionType.adoc": "connection",
    "Type.adoc": "concept",
    "TypeKind.adoc": "concept",
    "ValueKind.adoc": "concept",
    "ValueType.adoc": "concept",

    "analyzeStaticFunctions.adoc": "analyze",
    "analyzed-conjunctionStaticFunctions.adoc": "analyze",
    "responseStaticFunctions.adoc": "response",
    "connectionStaticFunctions.adoc": "connection",
    "legacyStaticFunctions.adoc": "legacy",
}
