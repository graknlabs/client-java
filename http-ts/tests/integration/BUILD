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

load("@aspect_rules_ts//ts:defs.bzl", "ts_project")
load("@aspect_rules_js//js:defs.bzl", "js_test")
load("@typedb_dependencies//tool/checkstyle:rules.bzl", "checkstyle_test")
load("//http-ts/tests/behaviour:rules.bzl", "behaviour_test_ts_config")

ts_project(
    name = "cluster-failover-lib",
    srcs = ["cluster-failover.ts"] + ["//http-ts:driver-lib"],
    tsconfig = behaviour_test_ts_config(),
    deps = [
        "//http-ts:node_modules/@types/node",
    ],
    transpiler = "tsc",
    declaration = True,
)

js_test(
    name = "cluster-failover",
    entry_point = ":cluster-failover.js",
    data = [
        ":cluster-failover-lib",
        "//tool/test:cluster-server.sh",
        "//tool/test/resources:certificates",
        "//tool/test/resources:encryption/ext-grpc-root-ca.pem",
    ],
    no_copy_to_bin = [
        "//tool/test:cluster-server.sh",
        "//tool/test/resources:certificates",
        "//tool/test/resources:encryption/ext-grpc-root-ca.pem",
    ],
    env = {
        "CLUSTER_SERVER_SCRIPT": "$(rootpath //tool/test:cluster-server.sh)",
        "ROOT_CA": "$(rootpath //tool/test/resources:encryption/ext-grpc-root-ca.pem)",
    },
    size = "large",
    local = True,  # Test manages external server processes via CLUSTER_DIR
    tags = ["exclusive"],
)

ts_project(
    name = "test-given-lib",
    srcs = ["test-given.ts"] + ["//http-ts:driver-lib"],
    tsconfig = behaviour_test_ts_config(),
    deps = [
        "//http-ts:node_modules/@types/node",
    ],
    transpiler = "tsc",
    declaration = True,
)

js_test(
    name = "test-given",
    entry_point = ":test-given.js",
    data = [
        ":test-given-lib",
    ],
    size = "medium",
    local = True,
)

ts_project(
    name = "connection-string-lib",
    srcs = ["connection-string.ts"] + ["//http-ts:driver-lib"],
    tsconfig = behaviour_test_ts_config(),
    deps = [
        "//http-ts:node_modules/@types/node",
    ],
    transpiler = "tsc",
    declaration = True,
)

js_test(
    name = "connection-string",
    entry_point = ":connection-string.js",
    data = [
        ":connection-string-lib",
        "//tool/test:connection-string-vectors",
    ],
    no_copy_to_bin = ["//tool/test:connection-string-vectors"],
    env = {
        "CONNECTION_STRING_VECTORS": "$(rootpath //tool/test:connection-string-vectors)",
    },
    size = "small",
)

checkstyle_test(
    name = "checkstyle",
    include = glob(["*"]),
    license_type = "apache-header",
)
