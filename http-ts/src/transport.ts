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

import type * as NodeHttps from "node:https";
import { DriverTlsConfig } from "./params";

export type Fetch = (url: string, init: RequestInit) => Promise<Response>;

const CONNECT_TIMEOUT_MILLIS = 10_000;
const IDLE_TIMEOUT_MILLIS = 300_000;

export function createFetch(tls?: DriverTlsConfig): Fetch {
    const rootCA = tls?.rootCA;
    if (rootCA === undefined) return (url, init) => fetch(url, init);
    if (!isNodeJs()) {
        throw new Error("A TLS root CA can only be set in Node.js: in browsers, add the CA to the trust store.");
    }
    if (!rootCA.includes("-----BEGIN CERTIFICATE-----")) {
        throw new Error("The TLS root CA must be the contents of a PEM certificate file, not its path.");
    }
    let httpsAgent: Promise<{ https: typeof NodeHttps, agent: NodeHttps.Agent }> | undefined;
    return async (url, init) => {
        if (!url.startsWith("https:")) return fetch(url, init);
        httpsAgent ??= importNodeHttps().then(https => ({
            https,
            agent: new https.Agent({ ca: rootCA, keepAlive: true }),
        }));
        const { https, agent } = await httpsAgent;
        return requestWithAgent(https, agent, url, init);
    };
}

function isNodeJs(): boolean {
    return typeof process !== "undefined" && typeof process.versions?.node === "string";
}

function importNodeHttps(): Promise<typeof NodeHttps> {
    // A specifier that bundlers cannot resolve statically keeps Node's https module out of browser bundles.
    const specifier = "node:https";
    return import(/* @vite-ignore */ /* webpackIgnore: true */ specifier);
}

function requestWithAgent(
    https: typeof NodeHttps, agent: NodeHttps.Agent, url: string, init: RequestInit,
): Promise<Response> {
    return new Promise((resolve, reject) => {
        const options = { method: init.method, headers: init.headers as Record<string, string>, agent };
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
        request.end(typeof init.body === "string" ? init.body : undefined);
    });
}
