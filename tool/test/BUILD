#!/usr/bin/env bash
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

load("@typedb_dependencies//tool/checkstyle:rules.bzl", "checkstyle_test")
load("@typedb_bazel_distribution//artifact:rules.bzl", "artifact_extractor")
load("@typedb_dependencies//builder/java:rules.bzl", "native_typedb_artifact")

exports_files(
    ["cluster-server.sh"],
    visibility = ["//visibility:public"],
)

# Shared by the Rust core and the standalone HTTP driver, which parse connection strings separately.
filegroup(
    name = "connection-string-vectors",
    srcs = ["connection-string-vectors.json"],
    visibility = ["//visibility:public"],
)

checkstyle_test(
    name = "checkstyle",
    include = glob(["*"]),
    exclude = ["connection-string-vectors.json"],
    license_type = "apache-header",
)

native_typedb_artifact(
    name = "native-typedb-artifact",
    native_artifacts = {
        "@typedb_bazel_distribution//platform:is_linux_arm64": ["@typedb_artifact_linux-arm64//file"],
        "@typedb_bazel_distribution//platform:is_linux_x86_64": ["@typedb_artifact_linux-x86_64//file"],
        "@typedb_bazel_distribution//platform:is_mac_arm64": ["@typedb_artifact_mac-arm64//file"],
        "@typedb_bazel_distribution//platform:is_mac_x86_64": ["@typedb_artifact_mac-x86_64//file"],
        "@typedb_bazel_distribution//platform:is_windows_x86_64": ["@typedb_artifact_windows-x86_64//file"],
    },
    output = "typedb-artifact.tar.gz",
    visibility = ["//visibility:public"],
)

native_typedb_artifact(
    name = "native-typedb-cluster-artifact",
    native_artifacts = {
        # TODO: Uncomment all platforms
        "@typedb_bazel_distribution//platform:is_linux_arm64": ["@typedb_cluster_artifact_linux-arm64//file"],
        "@typedb_bazel_distribution//platform:is_linux_x86_64": ["@typedb_cluster_artifact_linux-x86_64//file"],
        "@typedb_bazel_distribution//platform:is_mac_arm64": ["@typedb_cluster_artifact_mac-arm64//file"],
        "@typedb_bazel_distribution//platform:is_mac_x86_64": ["@typedb_cluster_artifact_mac-x86_64//file"],
#        "@typedb_bazel_distribution//platform:is_windows_x86_64": ["@typedb_cluster_artifact_windows-x86_64//file"],
    },
    output = "typedb-cluster-artifact.tar.gz",
    visibility = ["//visibility:public"],
)

artifact_extractor(
    name = "typedb-extractor",
    artifact = ":native-typedb-artifact",
)

artifact_extractor(
    name = "typedb-cluster-extractor",
    artifact = ":native-typedb-cluster-artifact",
)
