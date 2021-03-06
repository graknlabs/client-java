#
# Copyright (C) 2021 Vaticle
#
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
#

package(default_visibility = ["//test/behaviour:__subpackages__"])

load("@vaticle_dependencies//tool/checkstyle:rules.bzl", "checkstyle_test")
load("//test/behaviour:rules.bzl", "typedb_behaviour_java_test")

java_library(
    name = "steps",
    srcs = [
        "TransactionSteps.java",
    ],
    visibility = ["//visibility:public"],
    deps = [
        # Internal Package Dependencies
        "//api:api",
        "//test/behaviour/connection:steps-base",
        "//test/behaviour/util:util",

        # Internal Repository Dependencies
        "@vaticle_typedb_common//:common",
        "@vaticle_typeql_lang_java//:typeql-lang",

        # External Maven Dependencies
        "@maven//:junit_junit",
        "@maven//:io_cucumber_cucumber_java",
        "@maven//:org_hamcrest_hamcrest_library",
    ],
)

typedb_behaviour_java_test(
    name = "test",
    srcs = [
        "TransactionTest.java",
    ],
    test_class = "com.vaticle.typedb.client.test.behaviour.connection.transaction.TransactionTest",
    data = [
        "@vaticle_typedb_behaviour//connection:transaction.feature",
    ],
    typedb_artifact_mac = "@vaticle_typedb_artifact_mac//file",
    typedb_artifact_linux = "@vaticle_typedb_artifact_linux//file",
    typedb_artifact_windows = "@vaticle_typedb_artifact_windows//file",
    typedb_cluster_artifact_mac = "@vaticle_typedb_cluster_artifact_mac//file",
    typedb_cluster_artifact_linux = "@vaticle_typedb_cluster_artifact_linux//file",
    typedb_cluster_artifact_windows = "@vaticle_typedb_cluster_artifact_windows//file",
    connection_steps_core = "//test/behaviour/connection:steps-core",
    connection_steps_cluster = "//test/behaviour/connection:steps-cluster",
    steps = [
        ":steps",
        "//test/behaviour/connection/database:steps",
        "//test/behaviour/connection/session:steps",
        "//test/behaviour/typeql:steps",
    ],
    deps = [
        # Internal Package Dependencies
        "//api:api",
        "//test/behaviour:behaviour",

        # External dependencies from Maven
        "@maven//:io_cucumber_cucumber_junit",
    ],
    runtime_deps = [
        "//test/behaviour/config:parameters",
    ],
    size = "medium",
)

checkstyle_test(
    name = "checkstyle",
    include = glob(["*"]),
    license_type = "apache",
)
