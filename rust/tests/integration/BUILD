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

load("@rules_rust//rust:defs.bzl", "rust_test")
load("@typedb_dependencies//tool/checkstyle:rules.bzl", "checkstyle_test")

rust_test(
    name = "test_example",
    srcs = ["example.rs"],
    deps = [
        "//rust:typedb_driver",
        "@crates//:async-std",
        "@crates//:chrono",
        "@crates//:futures",
        "@crates//:itertools",
        "@crates//:regex",
        "@crates//:serde_json",
        "@crates//:serial_test",
        "@crates//:smol",
        "@crates//:tokio",
        "@crates//:uuid",
    ],
)

rust_test(
    name = "test_driver",
    srcs = ["driver.rs"],
    deps = [
        "//rust:typedb_driver",
        "@crates//:async-std",
        "@crates//:chrono",
        "@crates//:futures",
        "@crates//:itertools",
        "@crates//:regex",
        "@crates//:serde_json",
        "@crates//:serial_test",
        "@crates//:smol",
        "@crates//:tokio",
        "@crates//:uuid",
    ],
)

rust_test(
    name = "test_connection_string",
    srcs = ["connection_string.rs"],
    # The vectors count as a source for the Cargo sync aspect, which otherwise needs exactly one.
    crate_root = "connection_string.rs",
    compile_data = ["//tool/test:connection-string-vectors"],
    deps = [
        "//rust:typedb_driver",
        "@crates//:serde_json",
    ],
)

rust_test(
    name = "test_values",
    srcs = ["values.rs"],
    deps = [
        "//rust:typedb_driver",
        "@crates//:async-std",
        "@crates//:chrono",
        "@crates//:chrono-tz",
        "@crates//:futures",
        "@crates//:serial_test",
    ],
)

checkstyle_test(
    name = "checkstyle",
    include = glob(["*"]),
    license_type = "apache-header",
    size = "small",
)
