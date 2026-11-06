/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
import assert from "assert";
import { TypeDBHttpDriver, isOkResponse } from "../../dist/index.cjs";

// The server's HTTP endpoint, and the gRPC endpoint a connection string written for a gRPC driver names.
const HTTP_ADDRESS = process.env.TYPEDB_HTTP_ADDRESS || "http://127.0.0.1:8000";
const GRPC_ADDRESS = HTTP_ADDRESS.replace(/:8000$/, ":1729");

async function main(): Promise<void> {
    // A string written for the HTTP endpoint connects directly, without a probe.
    const direct = await TypeDBHttpDriver.fromConnectionString(`typedb://admin:password@${HTTP_ADDRESS}/some-db?name=Studio`);
    assert.ok(isOkResponse(await direct.getDatabases()), "expected the HTTP address to connect");

    // A string written for the gRPC endpoint falls back from port 1729 to port 8000.
    const fallback = await TypeDBHttpDriver.fromConnectionString(`typedb://admin:password@${GRPC_ADDRESS}`);
    assert.ok(isOkResponse(await fallback.getDatabases()), "expected the gRPC address to connect through the fallback");

    // Only port 1729 is retried: a closed port elsewhere is used as written and fails on first use.
    const closed = await TypeDBHttpDriver.fromConnectionString("typedb://admin:password@http://127.0.0.1:8001");
    assert.ok(!isOkResponse(await closed.getDatabases()), "expected a closed port to fail");

    console.log("Connection string connect checks passed");
}

main().catch(error => { console.error(error); process.exit(1); });
