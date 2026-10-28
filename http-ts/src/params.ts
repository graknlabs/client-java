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

export interface DriverParamsBasic {
    username: string;
    password: string;
    addresses: string[];
    tls?: DriverTlsConfig;
}

export interface DriverParamsTranslated {
    username: string;
    password: string;
    translatedAddresses: TranslatedAddress[];
    tls?: DriverTlsConfig;
}

/**
 * TLS settings for `https` addresses. Without them, the driver trusts the certificate authorities that the
 * runtime trusts by default.
 */
export interface DriverTlsConfig {
    /**
     * The path to the PEM file with the certificate of the CA that signed the servers' certificates. When set, the
     * driver trusts only this CA. The file is read on the first `https` request, and requests fail with `HDR3` if
     * it cannot be used. Supported in Node.js only: browsers trust the certificates of their own trust store.
     */
    rootCAPath?: string;
}

export interface TranslatedAddress {
    external: string;
    internal: string;
}

export type DriverParams = DriverParamsBasic | DriverParamsTranslated;

export function isBasicParams(params: DriverParams): params is DriverParamsBasic {
    return `addresses` in params;
}

export function isTranslatedParams(params: DriverParams): params is DriverParamsTranslated {
    return `translatedAddresses` in params;
}

export function remoteOrigin(params: DriverParams) {
    if (isBasicParams(params)) return `${params.addresses[0]}`;
    else return `${params.translatedAddresses[0].external}`;
}

const SCHEME_PREFIX_REGEX = /^[a-z][a-z0-9+.-]*:\/\//i;

export function hostPortFromOrigin(origin: string): string {
    if (!SCHEME_PREFIX_REGEX.test(origin)) return origin;
    try {
        return new URL(origin).host;
    } catch {
        return origin;
    }
}

export function resolveOrigin(params: DriverParams, primaryAddress: string): string {
    const target = hostPortFromOrigin(primaryAddress);
    if (isBasicParams(params)) {
        for (const addr of params.addresses) {
            if (hostPortFromOrigin(addr) === target) return addr;
        }
        return originWithScheme(primaryAddress, params.addresses[0]);
    } else {
        for (const ta of params.translatedAddresses) {
            if (hostPortFromOrigin(ta.internal) === target || hostPortFromOrigin(ta.external) === target) {
                return ta.external;
            }
        }
        return originWithScheme(primaryAddress, params.translatedAddresses[0].external);
    }
}

function originWithScheme(address: string, referenceOrigin: string): string {
    if (SCHEME_PREFIX_REGEX.test(address)) return address;
    try {
        return `${new URL(referenceOrigin).protocol}//${address}`;
    } catch {
        return `https://${address}`;
    }
}

/** Return all configured origins for connection error fallback. */
export function allOrigins(params: DriverParams): string[] {
    if (isBasicParams(params)) return params.addresses;
    else return params.translatedAddresses.map(ta => ta.external);
}
