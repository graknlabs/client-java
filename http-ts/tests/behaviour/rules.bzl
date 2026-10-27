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

load("@http-ts_npm//http-ts:@cucumber/cucumber/package_json.bzl", cucumber_bin = "bin")

def behaviour_test_ts_config():
    return {
        "compilerOptions": {
            "target": "es2021",
            "module": "commonjs",
            "moduleResolution": "node",
            "esModuleInterop": True,
            "skipLibCheck": True,
            "forceConsistentCasingInFileNames": True,
        }
    }

def typedb_behaviour_http_ts_test(name, **kwargs):
    """Creates both core and cluster test targets for HTTP TS behaviour tests."""
    typedb_behaviour_http_ts_core_test(name, **kwargs)
    typedb_behaviour_http_ts_cluster_test(name, **kwargs)

def typedb_behaviour_http_ts_core_test(name, features, data = [], tags = [], **kwargs):
    cucumber_bin.cucumber_js_test(
        name = name + "-core",
        data = [
            "//http-ts:node_modules/@cucumber/cucumber",
        ] + data + features + ["//http-ts/tests/behaviour/steps"],
        no_copy_to_bin = features,
        fixed_args = [
            "--publish-quiet", "--strict",
            "--tags 'not @ignore and not @ignore-typedb-driver and not @ignore-typedb-driver-nodejs and not @ignore-typedb-http'",
            "--require", "http-ts/tests/**/*.js",
        ] + ["$(location {})".format(feature) for feature in features],
        tags = tags + ["exclusive"],
        **kwargs,
    )

def typedb_behaviour_http_ts_cluster_test(name, features, data = [], tags = [], **kwargs):
    cucumber_bin.cucumber_js_test(
        name = name + "-cluster",
        data = [
            "//http-ts:node_modules/@cucumber/cucumber",
            "//tool/test/resources:encryption/ext-grpc-root-ca.pem",
        ] + data + features + ["//http-ts/tests/behaviour/steps"],
        no_copy_to_bin = features + ["//tool/test/resources:encryption/ext-grpc-root-ca.pem"],
        fixed_args = [
            "--publish-quiet", "--strict",
            "--tags 'not @ignore and not @ignore-typedb-driver and not @ignore-typedb-driver-nodejs and not @ignore-typedb-http and not @ignore-typedb-http-driver and not @ignore-typedb-driver-cluster'",
            "--require", "http-ts/tests/**/*.js",
        ] + ["$(location {})".format(feature) for feature in features],
        env = {
            "TYPEDB_CLUSTER_MODE": "true",
            "ROOT_CA": "$(rootpath //tool/test/resources:encryption/ext-grpc-root-ca.pem)",
        },
        tags = tags + ["exclusive"],
        **kwargs,
    )
