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
load("@typedb_driver_pip//:requirements.bzl", "requirement")
load("@rules_python//python:defs.bzl", "py_test")

exports_files(
    ["test_example.py"],
    visibility = ["//python:__subpackages__"],
)

py_test(
    name = "test_example",
    srcs = ["test_example.py"],
    deps = [
        "//python:driver_python",
        requirement("PyHamcrest"),
    ],
    data = ["//python:native-driver-binary-link", "//python:native-driver-wrapper-link"],
    python_version = "PY3",
    tags = ["exclusive"],
)

py_test(
    name = "test_driver",
    srcs = ["test_driver.py"],
    deps = [
        "//python:driver_python",
        requirement("PyHamcrest"),
    ],
    data = ["//python:native-driver-binary-link", "//python:native-driver-wrapper-link"],
    python_version = "PY3",
    tags = ["exclusive"],
)

py_test(
    name = "test_values",
    srcs = ["test_values.py"],
    deps = [
        "//python:driver_python",
        requirement("PyHamcrest"),
        "@rules_python//python/cc:current_py_cc_libs",
    ],
    data = ["//python:native-driver-binary-link", "//python:native-driver-wrapper-link"],
    python_version = "PY3",
    tags = ["exclusive"],
)

py_test(
    name = "test_debug",
    srcs = ["test_debug.py"],
    deps = [
        "//python:driver_python",
    ],
    data = ["//python:native-driver-binary-link", "//python:native-driver-wrapper-link"],
    python_version = "PY3",
    tags = ["exclusive"],
)

checkstyle_test(
    name = "checkstyle",
    include = glob(["*"]),
    license_type = "apache-header",
    size = "small",
)
