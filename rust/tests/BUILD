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

package(default_visibility = ["//visibility:public"])

load("@rules_rust//rust:defs.bzl", "rustfmt_test")
load("@typedb_dependencies//tool/checkstyle:rules.bzl", "checkstyle_test")

rustfmt_test(
    name = "rustfmt_test",
    targets = [
        "//rust/tests/behaviour/steps:steps",
        "//rust/tests/behaviour/connection:test_database",
        "//rust/tests/behaviour/connection:test_transaction",
        "//rust/tests/behaviour/driver:test_concept",
        "//rust/tests/behaviour/driver:test_connection",
        "//rust/tests/behaviour/driver:test_migration",
        "//rust/tests/behaviour/driver:test_query",
        "//rust/tests/behaviour/driver:test_user",

        "//rust/tests/integration:test_connection_string",
        "//rust/tests/integration:test_connection_string_connect",
        "//rust/tests/integration:test_driver",
        "//rust/tests/integration:test_example",
        "//rust/tests/integration:test_values",
        "//rust/tests/integration/cluster:test_failover",
    ],
    size = "small",
)

checkstyle_test(
    name = "checkstyle",
    include = glob(["*", "**/*"]),
    exclude = glob(["**/Cargo.*"], allow_empty = True),
    license_type = "apache-header",
    size = "small",
)
