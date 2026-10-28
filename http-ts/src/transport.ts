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

import type * as NodeFs from "node:fs/promises";
import type * as NodeHttps from "node:https";
import { DriverTlsConfig } from "./params";

export interface FetchInit {
    method: string;
    body?: string;
    headers: Record<string, string>;
}

export type Fetch = (url: string, init: FetchInit) => Promise<Response>;

type HttpsClient = { https: typeof NodeHttps, agent: NodeHttps.Agent };

const CONNECT_TIMEOUT_MILLIS = 10_000;
const IDLE_TIMEOUT_MILLIS = 300_000;

export class TlsConfigError extends Error {}

export function createFetch(tls?: DriverTlsConfig): Fetch {
    const rootCAPath = tls?.rootCAPath;
    if (rootCAPath === undefined) return (url, init) => fetch(url, init);
    if (!isNodeJs()) {
        throw new TlsConfigError(
            "A TLS root CA can only be set in Node.js: in browsers, add the CA to the trust store.",
        );
    }
    let httpsClient: Promise<HttpsClient> | undefined;
    return async (url, init) => {
        if (!url.startsWith("https:")) return fetch(url, init);
        httpsClient ??= createHttpsClient(rootCAPath).catch((error) => {
            httpsClient = undefined;
            throw error;
        });
        const { https, agent } = await httpsClient;
        return requestWithAgent(https, agent, url, init);
    };
}

async function createHttpsClient(rootCAPath: string): Promise<HttpsClient> {
    let https: typeof NodeHttps;
    let fs: typeof NodeFs;
    try {
        [https, fs] = await Promise.all([
            importNodeModule<typeof NodeHttps>("node:https"),
            importNodeModule<typeof NodeFs>("node:fs/promises"),
        ]);
    } catch (error) {
        throw new TlsConfigError(`Cannot load the Node.js modules for TLS: ${(error as Error).message}`);
    }
    let rootCA: string;
    try {
        rootCA = await fs.readFile(rootCAPath, "utf8");
    } catch (error) {
        throw new TlsConfigError(`Cannot read the TLS root CA file '${rootCAPath}': ${(error as Error).message}`);
    }
    if (!rootCA.includes("-----BEGIN CERTIFICATE-----")) {
        throw new TlsConfigError(`The TLS root CA file '${rootCAPath}' does not contain a PEM certificate.`);
    }
    return { https, agent: new https.Agent({ ca: rootCA, keepAlive: true }) };
}

function isNodeJs(): boolean {
    return typeof process !== "undefined" && typeof process.versions?.node === "string";
}

function importNodeModule<MODULE>(specifier: string): Promise<MODULE> {
    // A specifier that bundlers cannot resolve statically keeps Node's modules out of browser bundles.
    return import(/* @vite-ignore */ /* webpackIgnore: true */ specifier);
}

function requestWithAgent(
    https: typeof NodeHttps, agent: NodeHttps.Agent, url: string, init: FetchInit,
): Promise<Response> {
    return new Promise((resolve, reject) => {
        const options = { method: init.method, headers: init.headers, agent };
        const request = https.request(url, options, (response) => {
            const chunks: Uint8Array[] = [];
            response.on("data", (chunk: Uint8Array) => chunks.push(chunk));
            response.on("error", reject);
            response.on("end", () => {
                const status = response.statusCode ?? 0;
                const headers = new Headers();
                for (const [name, value] of Object.entries(response.headers)) {
                    if (value !== undefined) headers.set(name, Array.isArray(value) ? value.join(", ") : value);
                }
                const body = status === 204 || status === 304 ? null : Buffer.concat(chunks);
                resolve(new Response(body, { status, statusText: response.statusMessage, headers }));
            });
        });
        request.on("error", reject);
        request.on("socket", (socket) => {
            if (!socket.connecting) return;
            const connectTimeout = setTimeout(
                () => request.destroy(new Error("Connection timed out")), CONNECT_TIMEOUT_MILLIS,
            );
            socket.once("connect", () => clearTimeout(connectTimeout));
            socket.once("close", () => clearTimeout(connectTimeout));
        });
        request.setTimeout(IDLE_TIMEOUT_MILLIS, () => request.destroy(new Error("Request timed out")));
        request.end(init.body);
    });
}
