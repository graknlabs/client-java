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

load("@rules_dotnet//dotnet:defs.bzl", "csharp_nunit_test", "csharp_test")
load("//csharp:build_opts.bzl", "nullable_context")
load("//csharp/Test:build_opts.bzl", "behaviour_tests_deps")


def csharp_behaviour_test_all(name, test_files, step_files, features, deps, target_frameworks, targeting_packs, **kwargs):
    csharp_behaviour_test_core(name, test_files, step_files, features, deps, target_frameworks, targeting_packs, **kwargs)
    csharp_behaviour_test_cluster(name, test_files, step_files, features, deps, target_frameworks, targeting_packs, **kwargs)


def csharp_behaviour_test_core(name, test_files, step_files, features, deps, target_frameworks, targeting_packs, **kwargs):
    csharp_behaviour_test(
        name = name + "-core",
        test_files = test_files,
        step_files = step_files + ["//csharp/Test/Behaviour/Connection:steps-core"],
        features = features,
        deps = deps,
        target_frameworks = target_frameworks,
        targeting_packs = targeting_packs,
        add_certificates = False,
        **kwargs,
    )


def csharp_behaviour_test_cluster(name, test_files, step_files, features, deps, target_frameworks, targeting_packs, **kwargs):
    csharp_behaviour_test(
        name = name + "-cluster",
        test_files = test_files,
        step_files = step_files + ["//csharp/Test/Behaviour/Connection:steps-cluster"],
        features = features,
        deps = deps,
        target_frameworks = target_frameworks,
        targeting_packs = targeting_packs,
        add_certificates = True,
        defines = ["TYPEDB_CLUSTER_MODE"],
        **kwargs,
    )


def csharp_behaviour_test(
        name,
        test_files,
        step_files,
        features,
        deps,
        target_frameworks,
        targeting_packs,
        add_certificates = False,
        tags = [],
        **kwargs):
    certificates = ["//tool/test/resources:certificates"] if add_certificates else []

    csharp_test(
        name = name,
        srcs = test_files + step_files + [
            "//csharp/Test/Behaviour/Util:TestRunner.cs",
            "//csharp/Test/Behaviour/Util:FeatureFileFilter.cs",
            "//csharp/Test/Behaviour:TestValueHelper.cs",
        ],
        data = features + certificates,
        deps = deps + behaviour_tests_deps,
        target_frameworks = target_frameworks,
        targeting_packs = targeting_packs,
        runtime_identifier = "any",
        nullable = nullable_context,
        visibility = ["//visibility:public"],
        tags = tags + ["exclusive"],
        **kwargs,
    )


def csharp_integration_test(name, srcs, deps, target_frameworks, targeting_packs, tags = [], **kwargs):
    csharp_nunit_test(
        name = name,
        srcs = srcs,
        deps = deps,
        target_frameworks = target_frameworks,
        targeting_packs = targeting_packs,
        runtime_identifier = "any",
        nullable = nullable_context,
        visibility = ["//visibility:public"],
        tags = tags + ["exclusive"],
        **kwargs,
    )
