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

load("@bazel_tools//tools/build_defs/repo:git.bzl", "git_repository")

def graknlabs_graql():
    git_repository(
        name = "graknlabs_graql",
        remote = "https://github.com/graknlabs/graql",
        commit = "f5036124b5cb7b2d2ad9209ad590ea8821240c6e" # sync-marker: do not remove this comment, this is used for sync-dependencies by @graknlabs_graql
    )

def graknlabs_common():
    git_repository(
        name = "graknlabs_common",
        remote = "https://github.com/graknlabs/common",
        commit = "cfd261fd5412a0b45cb5494555e4491dd3ce5f64" # sync-marker: do not remove this comment, this is used for sync-dependencies by @graknlabs_common
    )

def graknlabs_bazel_distribution():
    git_repository(
        name = "graknlabs_bazel_distribution",
        remote = "https://github.com/graknlabs/bazel-distribution",
        commit = "ba4a7d6e6fa98fcb44e7c6eae492aeeb9d85001b" # sync-marker: do not remove this comment, this is used for sync-dependencies by @graknlabs_bazel_distribution
    )

def graknlabs_dependencies():
    git_repository(
        name = "graknlabs_dependencies",
        remote = "https://github.com/graknlabs/dependencies",
        commit = "6ed5b544d1ebd7f650763fbe1aec67d48909d842", # sync-marker: do not remove this comment, this is used for sync-dependencies by @graknlabs_dependencies
    )

def graknlabs_protocol():
    git_repository(
        name = "graknlabs_protocol",
        remote = "https://github.com/graknlabs/protocol",
        commit = "ae9cb7adbf234a39d35f158652e290d7190b0cc6", # sync-marker: do not remove this comment, this is used for sync-dependencies by @graknlabs_protocol
    )

def graknlabs_behaviour():
    git_repository(
        name = "graknlabs_behaviour",
        remote = "https://github.com/graknlabs/behaviour",
        commit = "079842c5df916f9a7487686dde6f10f544806b59",
    )

def graknlabs_grabl_tracing():
    git_repository(
        name = "graknlabs_grabl_tracing",
        remote = "https://github.com/graknlabs/grabl-tracing",
        commit = "458dfde3bb041c68cf193b3200a344b833f47c4a"  # sync-marker: do not remove this comment, this is used for sync-dependencies by @graknlabs_grabl_tracing
    )
