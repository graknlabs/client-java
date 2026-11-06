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

import { Given, Then, When, DataTable } from "@cucumber/cucumber";
import { assertNotError, checkMayError, EXPECT_ERROR_CONTAINING, MayError } from "./params";
import {
    closeConnection, DEFAULT_HOST,
    DEFAULT_PASSWORD, DEFAULT_PORT,
    DEFAULT_USERNAME, defaultConnectionString,
    driver, OTHER_PROTOCOL_DEFAULT_PORT,
    openAndTestConnection, openAndTestConnectionWithAddresses,
    openAndTestConnectionWithConnectionString,
    openAndTestConnectionWithHostPort, openAndTestSingleConnection
} from "./context";
import assert from "assert";
import { Server } from "../../../dist/index.cjs";

Given("typedb starts", () => {});
Given("connection is open: {boolean}", (_) => {});

async function connectionOpens(username: string, password: string, mayError: MayError) {
    await openAndTestConnection(username, password).then(checkMayError(mayError));
}
Given("connection opens with username '{word}', password {string}{may_error}", connectionOpens);
Given(`connection opens with username '{word}', password {string}${EXPECT_ERROR_CONTAINING}`, connectionOpens);
Given("connection opens with default authentication", () => connectionOpens(DEFAULT_USERNAME, DEFAULT_PASSWORD, false))

async function connectionOpensSingle(username: string, password: string, mayError: MayError) {
    await openAndTestSingleConnection(username, password).then(checkMayError(mayError));
}
Given("connection opens to single server with default authentication{may_error}", (mayError: MayError) => connectionOpensSingle(DEFAULT_USERNAME, DEFAULT_PASSWORD, mayError))

// The expected message (_) is the underlying network library's text, which differs
// across drivers; we only assert that the connection failed.
When(`connection opens with a wrong host${EXPECT_ERROR_CONTAINING}`, async (_: string) => {
    await openAndTestConnectionWithHostPort(DEFAULT_USERNAME, DEFAULT_PASSWORD, "surely-not-localhost", DEFAULT_PORT).then(checkMayError(true));
});
When("connection opens with a wrong port; fails", async () => {
    await openAndTestConnectionWithHostPort(DEFAULT_USERNAME, DEFAULT_PASSWORD, DEFAULT_HOST, 0).then(checkMayError(true));
});

When("connection opens with a connection string for the default address{may_error}", async (mayError: MayError) => {
    await openAndTestConnectionWithConnectionString(defaultConnectionString()).then(checkMayError(mayError));
});
When("connection opens with connection string: {string}{may_error}", async (connectionString: string, mayError: MayError) => {
    await openAndTestConnectionWithConnectionString(connectionString).then(checkMayError(mayError));
});
When(`connection opens with connection string: {string}${EXPECT_ERROR_CONTAINING}`, async (connectionString: string, message: string) => {
    await openAndTestConnectionWithConnectionString(connectionString).then(checkMayError(message));
});
When("connection opens with a connection string for a wrong port{may_error}", async (mayError: MayError) => {
    await openAndTestConnectionWithConnectionString(defaultConnectionString(0)).then(checkMayError(mayError));
});
When("connection opens with a connection string for the other protocol default port{may_error}", async (mayError: MayError) => {
    await openAndTestConnectionWithConnectionString(defaultConnectionString(OTHER_PROTOCOL_DEFAULT_PORT)).then(checkMayError(mayError));
});

Then("connection has {int} user(s)", async (expectedUserCount: number) => {
    const usersListRes = await driver.getUsers().then(assertNotError);
    const userCount = usersListRes.ok.users.length;
    assert.equal(userCount, expectedUserCount);
});

When("connection closes", closeConnection);

async function getServers(): Promise<Server[]> {
    const res = await driver.getServers().then(assertNotError);
    return res.ok.servers;
}

// The shared cluster.feature uses gRPC addresses (e.g. 127.0.0.1:11729) as canonical node
// identifiers across drivers. The http-ts driver speaks HTTP, so its `Server.address` is
// the HTTP advertise address (e.g. 127.0.0.1:18000). The test cluster fixture follows a
// fixed port convention — gRPC `<id>1729`, HTTP `<id>8000` — so we translate gRPC→HTTP
// at the test boundary rather than diverging from the shared feature file.
function grpcAddressToHttp(address: string): string {
    return address.replace(/:(\d)1729$/, (_, id) => `:${id}8000`);
}

Then("connection has {int} server(s)", async (expectedCount: number) => {
    const servers = await getServers();
    assert.equal(servers.length, expectedCount, `Expected ${expectedCount} servers but got ${servers.length}`);
});

Then("connection primary server exists", async () => {
    const servers = await getServers();
    const primary = servers.find(s => s.replicationStatus?.role === "primary");
    assert.ok(primary, "No primary server found");
});

Then("connection get server\\({word}) exists", async (address: string) => {
    const servers = await getServers();
    const expected = grpcAddressToHttp(address);
    const server = servers.find(s => s.address === expected);
    assert.ok(server, `Server with address ${address} (mapped to ${expected}) not found`);
});

Then("connection get server\\({word}) does not exist", async (address: string) => {
    const servers = await getServers();
    const expected = grpcAddressToHttp(address);
    const server = servers.find(s => s.address === expected);
    assert.ok(!server, `Server with address ${address} (mapped to ${expected}) should not exist`);
});

Then("connection get server\\({word}) has term", async (address: string) => {
    const servers = await getServers();
    const expected = grpcAddressToHttp(address);
    const server = servers.find(s => s.address === expected);
    assert.ok(server, `Server with address ${address} (mapped to ${expected}) not found`);
    assert.ok(typeof server.replicationStatus?.term === "number", `Server ${address} has no term`);
});

// The data table is a single column of role names with no header, e.g.:
//     | primary   |
//     | secondary |
//     | secondary |
// We count the expected occurrences of each role and compare to the actual counts —
// matching the rust step's logic. We don't care which address has which role.
Then("connection servers have roles:", async (dataTable: DataTable) => {
    const servers = await getServers();
    const expected = { primary: 0, secondary: 0, candidate: 0 };
    for (const [role] of dataTable.raw()) {
        if (!(role in expected)) throw new Error(`Unknown server replication role: ${role}`);
        expected[role as keyof typeof expected]++;
    }
    const actual = { primary: 0, secondary: 0, candidate: 0 };
    for (const s of servers) {
        const role = s.replicationStatus?.role;
        if (role && role in actual) actual[role]++;
    }
    assert.deepStrictEqual(actual, expected, `Server role counts mismatch: expected ${JSON.stringify(expected)}, got ${JSON.stringify(actual)}`);
});
