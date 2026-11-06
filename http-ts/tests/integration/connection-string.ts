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
import * as fs from "fs";
import {
    ConnectionStringError, CONNECTION_STRING_DEFAULT_PORT, DriverParams, hostPortFromOrigin, isBasicParams,
    parseConnectionString,
} from "../../dist/index.cjs";

// The conformance vectors shared with the Rust core, which parses connection strings with its own
// implementation. See the file's own comment. No server is needed.
const VECTORS_PATH = process.env.CONNECTION_STRING_VECTORS || "../tool/test/connection-string-vectors.json";

interface Vector {
    name: string;
    connection_string: string;
    expect?: {
        username: string;
        password: string;
        tls: boolean;
        direct?: string[];
        translated?: Record<string, string>;
    };
    error?: string;
}

function withDefaultPort(address: string): string {
    return address.replace("{port}", String(CONNECTION_STRING_DEFAULT_PORT));
}

function checkSuccess(params: DriverParams, expected: NonNullable<Vector["expect"]>): void {
    assert.equal(params.username, expected.username, "username");
    assert.equal(params.password, expected.password, "password");
    const origins = isBasicParams(params)
        ? params.addresses
        : params.translatedAddresses.flatMap(ta => [ta.external, ta.internal]);
    for (const origin of origins) {
        assert.equal(origin.startsWith("https://"), expected.tls, `tls of '${origin}'`);
    }
    if (expected.direct) {
        assert.ok(isBasicParams(params), "expected direct addresses");
        assert.deepEqual(params.addresses.map(hostPortFromOrigin), expected.direct.map(withDefaultPort));
    } else {
        assert.ok(!isBasicParams(params), "expected translated addresses");
        const translation = Object.fromEntries(
            params.translatedAddresses.map(ta => [hostPortFromOrigin(ta.external), hostPortFromOrigin(ta.internal)])
        );
        const expectedTranslation = Object.fromEntries(
            Object.entries(expected.translated).map(([pub, priv]) => [withDefaultPort(pub), withDefaultPort(priv)])
        );
        assert.deepEqual(translation, expectedTranslation);
    }
}

function checkError(error: unknown, expected: string): void {
    assert.ok(error instanceof ConnectionStringError, `expected a ConnectionStringError, got: ${error}`);
    const connectionStringError = error as ConnectionStringError;
    assert.equal(connectionStringError.kind, expected, `error kind (${connectionStringError.message})`);
}

function main(): void {
    const vectors: Vector[] = JSON.parse(fs.readFileSync(VECTORS_PATH, "utf8")).vectors;
    assert.ok(vectors.length > 0, "Expected at least one vector");

    const failures: string[] = [];
    for (const vector of vectors) {
        try {
            let params: DriverParams;
            try {
                params = parseConnectionString(vector.connection_string);
            } catch (error) {
                if (vector.error === undefined) throw error;
                checkError(error, vector.error);
                continue;
            }
            if (vector.error !== undefined) assert.fail(`expected '${vector.error}', but parsed: ${JSON.stringify(params)}`);
            checkSuccess(params, vector.expect);
        } catch (error) {
            failures.push(`  ${vector.name}: ${(error as Error).message}`);
        }
    }
    if (failures.length > 0) {
        console.error(`Connection string vectors failed:\n${failures.join("\n")}`);
        process.exit(1);
    }
    console.log(`All ${vectors.length} connection string vectors passed`);
}

main();
