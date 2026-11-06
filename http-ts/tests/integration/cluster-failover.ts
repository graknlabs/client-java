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

import { execSync } from "child_process";
import { TypeDBHttpDriver, isOkResponse, isApiErrorResponse, Server } from "../../dist/index.cjs";

const ADDRESSES = [
    "https://127.0.0.1:18000",
    "https://127.0.0.1:28000",
    "https://127.0.0.1:38000",
];
const USERNAME = "admin";
const PASSWORD = "password";
const DATABASE_NAME = "test-failover";
const FAILOVER_ITERATIONS = 10;
const PRIMARY_POLL_RETRIES = 60;
const PRIMARY_POLL_INTERVAL_MS = 2000;
const POST_KILL_STABILIZATION_MS = 5000;

function sleep(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
}

function clusterServer(command: string, nodeId: string): void {
    const script = process.env.CLUSTER_SERVER_SCRIPT;
    if (!script) throw new Error("CLUSTER_SERVER_SCRIPT environment variable must be set");
    const env: Record<string, string> = { ...process.env as Record<string, string> };
    if (!env.CLUSTER_DIR && env.BUILD_WORKSPACE_DIRECTORY) {
        env.CLUSTER_DIR = env.BUILD_WORKSPACE_DIRECTORY;
    }
    execSync(`${script} ${command} ${nodeId}`, { env, stdio: "inherit" });
}

function ensureAllNodesUp(): void {
    for (let i = 1; i <= ADDRESSES.length; i++) {
        const id = i.toString();
        clusterServer("start", id);
        clusterServer("await", id);
    }
}

function nodeIdFromAddress(address: string): string {
    // HTTP port pattern: <node_id>8000 → node_id is first digit of port
    const match = address.match(/:(\d)8000/);
    if (match) return match[1];
    // Fallback: extract from host:port
    const port = address.split(":").pop() || "";
    return port[0];
}

function createDriver(): TypeDBHttpDriver {
    const rootCAPath = process.env.ROOT_CA;
    if (!rootCAPath) throw new Error("ROOT_CA environment variable must be set");
    return new TypeDBHttpDriver({
        username: USERNAME, password: PASSWORD, addresses: ADDRESSES, tls: { rootCAPath }
    });
}

async function getPrimaryServer(driver: TypeDBHttpDriver): Promise<Server> {
    for (let attempt = 0; attempt < PRIMARY_POLL_RETRIES; attempt++) {
        const resp = await driver.getServers();
        if (isOkResponse(resp)) {
            const primary = resp.ok.servers.find(
                s => s.replicationStatus?.role === "primary" && s.address != null
            );
            if (primary) return primary;
        }
        if (attempt < PRIMARY_POLL_RETRIES - 1) {
            console.log(
                `  No primary server found (attempt ${attempt + 1}/${PRIMARY_POLL_RETRIES}). Retrying in ${PRIMARY_POLL_INTERVAL_MS}ms...`
            );
            await sleep(PRIMARY_POLL_INTERVAL_MS);
        }
    }
    throw new Error("Retry limit exceeded while seeking a primary server.");
}

async function setupDatabase(driver: TypeDBHttpDriver): Promise<void> {
    for (let attempt = 0; attempt < PRIMARY_POLL_RETRIES; attempt++) {
        try {
            await trySetupDatabase(driver);
            return;
        } catch (e) {
            if (attempt < PRIMARY_POLL_RETRIES - 1) {
                console.log(
                    `  Database setup failed (attempt ${attempt + 1}/${PRIMARY_POLL_RETRIES}): ${e}. Retrying in ${PRIMARY_POLL_INTERVAL_MS}ms...`
                );
                await sleep(PRIMARY_POLL_INTERVAL_MS);
            } else {
                throw new Error(`Database setup failed after ${PRIMARY_POLL_RETRIES} attempts: ${e}`);
            }
        }
    }
}

async function trySetupDatabase(driver: TypeDBHttpDriver): Promise<void> {
    const dbsResp = await driver.getDatabases();
    if (!isOkResponse(dbsResp)) throw new Error(`Failed to list databases: ${JSON.stringify(dbsResp)}`);
    if (dbsResp.ok.databases.some(db => db.name === DATABASE_NAME)) {
        const delResp = await driver.deleteDatabase(DATABASE_NAME);
        if (isApiErrorResponse(delResp)) throw new Error(`Failed to delete database: ${JSON.stringify(delResp)}`);
    }

    const createResp = await driver.createDatabase(DATABASE_NAME);
    if (isApiErrorResponse(createResp)) throw new Error(`Failed to create database: ${JSON.stringify(createResp)}`);

    const txResp = await driver.openTransaction(DATABASE_NAME, "schema");
    if (!isOkResponse(txResp)) throw new Error(`Failed to open schema transaction: ${JSON.stringify(txResp)}`);

    const qResp = await driver.query(txResp.ok.transactionId, "define entity person;");
    if (isApiErrorResponse(qResp)) throw new Error(`Failed to define schema: ${JSON.stringify(qResp)}`);

    const commitResp = await driver.commitTransaction(txResp.ok.transactionId);
    if (isApiErrorResponse(commitResp)) throw new Error(`Failed to commit: ${JSON.stringify(commitResp)}`);
}

async function verifyReadQuery(driver: TypeDBHttpDriver): Promise<void> {
    for (let attempt = 0; attempt < PRIMARY_POLL_RETRIES; attempt++) {
        const txResp = await driver.openTransaction(DATABASE_NAME, "read");
        if (!isOkResponse(txResp)) {
            if (attempt < PRIMARY_POLL_RETRIES - 1) {
                console.log(`    Read query attempt ${attempt + 1}/${PRIMARY_POLL_RETRIES} failed (transaction open). Retrying...`);
                await sleep(PRIMARY_POLL_INTERVAL_MS);
                continue;
            }
            throw new Error(`Failed to open read transaction after ${PRIMARY_POLL_RETRIES} attempts: ${JSON.stringify(txResp)}`);
        }

        const qResp = await driver.query(txResp.ok.transactionId, "match entity $t;");
        await driver.closeTransaction(txResp.ok.transactionId);

        if (!isOkResponse(qResp)) {
            if (attempt < PRIMARY_POLL_RETRIES - 1) {
                console.log(`    Read query attempt ${attempt + 1}/${PRIMARY_POLL_RETRIES} failed (query). Retrying...`);
                await sleep(PRIMARY_POLL_INTERVAL_MS);
                continue;
            }
            throw new Error(`Read query failed after ${PRIMARY_POLL_RETRIES} attempts: ${JSON.stringify(qResp)}`);
        }
        if (qResp.ok.answerType === "conceptRows" && qResp.ok.answers.length === 0) {
            throw new Error("Expected at least one entity type in read query results");
        }
        return;
    }
}

async function waitForClusterHealthy(driver: TypeDBHttpDriver): Promise<void> {
    for (let attempt = 0; attempt < PRIMARY_POLL_RETRIES; attempt++) {
        const resp = await driver.getServers();
        if (isOkResponse(resp)) {
            const activeNodes = resp.ok.servers.filter(
                (s: Server) => s.address != null && s.replicationStatus != null && s.replicationStatus.role != null
            );
            if (activeNodes.length >= ADDRESSES.length) return;
            if (attempt % 10 === 0 && attempt > 0) {
                console.log(`    Waiting for cluster health: ${activeNodes.length}/${ADDRESSES.length} nodes active`);
            }
        }
        await sleep(PRIMARY_POLL_INTERVAL_MS);
    }
    throw new Error(`Cluster did not reach ${ADDRESSES.length} healthy nodes within ${PRIMARY_POLL_RETRIES * PRIMARY_POLL_INTERVAL_MS / 1000}s timeout`);
}

async function cleanupDatabase(): Promise<void> {
    try {
        const driver = createDriver();
        const dbsResp = await driver.getDatabases();
        if (isOkResponse(dbsResp) && dbsResp.ok.databases.some(db => db.name === DATABASE_NAME)) {
            await driver.deleteDatabase(DATABASE_NAME);
        }
    } catch {
        // Best-effort cleanup
    }
}

async function main(): Promise<void> {
    ensureAllNodesUp();

    console.log("=== Cluster Failover Test (HTTP-TS) ===");

    console.log("Connecting driver...");
    const driver = createDriver();

    console.log("Setting up database and schema...");
    await setupDatabase(driver);
    await verifyReadQuery(driver);
    console.log("Initial setup verified.");

    for (let iteration = 1; iteration <= FAILOVER_ITERATIONS; iteration++) {
        console.log(`\n--- Failover iteration ${iteration}/${FAILOVER_ITERATIONS} ---`);

        const primary = await getPrimaryServer(driver);
        const primaryAddress = primary.address!;
        const nodeId = nodeIdFromAddress(primaryAddress);
        console.log(`  Primary server: ${primaryAddress} (node ${nodeId})`);

        console.log("  Read query before kill...");
        await verifyReadQuery(driver);

        console.log(`  Killing node ${nodeId}...`);
        clusterServer("kill", nodeId);

        // Allow cluster time to detect failure and begin election before polling
        console.log(`  Waiting ${POST_KILL_STABILIZATION_MS / 1000}s for cluster to stabilize...`);
        await sleep(POST_KILL_STABILIZATION_MS);

        console.log("  Read query after kill (driver auto-failover)...");
        await verifyReadQuery(driver);
        console.log("  Auto-failover read succeeded.");

        console.log("  Confirming new primary...");
        const newPrimary = await getPrimaryServer(driver);
        const newNodeId = nodeIdFromAddress(newPrimary.address!);
        console.log(`  New primary: ${newPrimary.address} (node ${newNodeId})`);

        console.log("  Read query on confirmed primary...");
        await verifyReadQuery(driver);
        console.log("  Confirmed primary read succeeded.");

        console.log(`  Restarting node ${nodeId}...`);
        clusterServer("start", nodeId);
        clusterServer("await", nodeId);
        // Wait for the restarted node to fully rejoin the cluster
        await waitForClusterHealthy(driver);
        console.log(`  Node ${nodeId} restarted, cluster fully healthy.`);
    }

    console.log(`\n=== All ${FAILOVER_ITERATIONS} failover iterations passed! ===`);

    await cleanupDatabase();
}

main().then(
    () => process.exit(0),
    (err) => {
        console.error("FAILOVER TEST FAILED:", err);
        process.exit(1);
    }
);
