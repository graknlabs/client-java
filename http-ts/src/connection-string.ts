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
import { DriverParams, TranslatedAddress } from "./params";

/** The URI scheme that identifies a TypeDB connection string. */
const CONNECTION_STRING_SCHEME = "typedb://";

/** The port assumed for addresses that do not specify one. */
export const CONNECTION_STRING_DEFAULT_PORT = 8000;

/** The gRPC endpoint's default port, which a connection string written for a gRPC driver carries. See `fallbackParams`. */
export const GRPC_DEFAULT_PORT = 1729;


/** Separates the public and private halves of a translated address pair. */
const ADDRESS_TRANSLATION_SEPARATOR = ";";

const SCHEME_REGEX = /^([a-z][a-z0-9+.-]*):\/\/(.*)$/i;
/** A bracketed IPv6 literal or a colon-free host, with an optional port. */
const AUTHORITY_REGEX = /^(\[[^\]]*\]|[^:\[\]]+)(?::(\d+))?$/;

export type ConnectionStringErrorKind =
    | "invalid"
    | "address_invalid_scheme"
    | "inconsistent_schemes"
    | "inconsistent_address_translation";

/** Thrown when a connection string cannot be parsed. `kind` distinguishes the grammar violations. */
export class ConnectionStringError extends Error {
    constructor(readonly kind: ConnectionStringErrorKind, message: string) {
        super(message);
        this.name = "ConnectionStringError";
    }
}

function invalid(reason: string): ConnectionStringError {
    return new ConnectionStringError(
        "invalid",
        `Invalid connection string: ${reason}. Expected the format 'typedb://<username>:<password>@<addresses>'.`
    );
}

/**
 * Parses a TypeDB connection string of the form
 * `typedb://<username>:<password>@<addresses>[/<database>][?<params>]` into driver parameters.
 *
 * Addresses are comma-separated, and each one must carry an explicit `http://` or `https://`
 * scheme; all addresses must agree on it. Each address may instead be a `public;private` pair,
 * which produces translated addresses; pairs cannot be mixed with plain addresses.
 *
 * An address without a port is assumed to be on `CONNECTION_STRING_DEFAULT_PORT`; an explicit port
 * is kept as written. The database path segment and any query parameters are accepted and discarded.
 */
export function parseConnectionString(connectionString: string): DriverParams {
    if (!connectionString.startsWith(CONNECTION_STRING_SCHEME)) {
        throw invalid(`expected the connection string to start with '${CONNECTION_STRING_SCHEME}'`);
    }
    const remainder = connectionString.slice(CONNECTION_STRING_SCHEME.length);
    const [credentials, rest] = splitCredentials(remainder);
    const { username, password } = parseCredentials(credentials);
    return { username, password, ...parseAddresses(splitAddresses(rest), CONNECTION_STRING_DEFAULT_PORT) };
}

/**
 * The parameters with every occurrence of the gRPC endpoint's default port replaced by
 * `CONNECTION_STRING_DEFAULT_PORT`, or `undefined` if no address carries it. A connection string
 * written for the gRPC endpoint names the same server as the HTTP endpoint, so a driver whose
 * connection to the written port is not answered by TypeDB's HTTP API retries here.
 */
export function fallbackParams(params: DriverParams): DriverParams | undefined {
    const replace = (address: string): string | undefined => {
        const match = /^(https?:\/\/(?:\[[^\]]*\]|[^:\[\]]+)):(\d+)$/.exec(address);
        return match && Number(match[2]) === GRPC_DEFAULT_PORT ? `${match[1]}:${CONNECTION_STRING_DEFAULT_PORT}` : undefined;
    };
    if ("addresses" in params) {
        if (!params.addresses.some(address => replace(address) != null)) return undefined;
        return { ...params, addresses: params.addresses.map(address => replace(address) ?? address) };
    } else {
        if (!params.translatedAddresses.some(ta => replace(ta.external) != null || replace(ta.internal) != null)) return undefined;
        return {
            ...params,
            translatedAddresses: params.translatedAddresses.map(ta => ({
                external: replace(ta.external) ?? ta.external,
                internal: replace(ta.internal) ?? ta.internal,
            })),
        };
    }
}

/**
 * Splits the credentials from the addresses at the last `@` preceding any query string. The last
 * one, because a password may itself contain an unencoded `@`.
 */
function splitCredentials(remainder: string): [string, string] {
    const queryStart = remainder.indexOf("?");
    const searchable = queryStart === -1 ? remainder : remainder.slice(0, queryStart);
    const separator = searchable.lastIndexOf("@");
    if (separator === -1) throw invalid("expected credentials, separated from the addresses by '@'");
    return [remainder.slice(0, separator), remainder.slice(separator + 1)];
}

function parseCredentials(credentials: string): { username: string, password: string } {
    const separator = credentials.indexOf(":");
    if (separator === -1) throw invalid("expected a username and a password, separated by ':'");
    const username = credentials.slice(0, separator);
    const password = credentials.slice(separator + 1);
    if (username.length === 0) throw invalid("expected a non-empty username");
    if (password.length === 0) throw invalid("expected a non-empty password");
    return { username: percentDecode(username, "username"), password: percentDecode(password, "password") };
}

function percentDecode(value: string, field: string): string {
    try {
        return decodeURIComponent(value);
    } catch {
        throw invalid(`expected the ${field} to be valid percent-encoded UTF-8`);
    }
}

/**
 * Takes the addresses from the start of `remainder`, discarding the database path segment and the
 * query parameters that may follow them.
 */
function splitAddresses(remainder: string): string {
    // Addresses carry their own "://", so only look for the terminating '/' beyond the last one.
    const lastScheme = remainder.lastIndexOf("://");
    const searchStart = lastScheme === -1 ? 0 : lastScheme + "://".length;
    const terminator = remainder.slice(searchStart).search(/[/?]/);
    const addresses = terminator === -1 ? remainder : remainder.slice(0, searchStart + terminator);
    if (addresses.length === 0) throw invalid("expected at least one address");
    return addresses;
}

function parseAddresses(addresses: string, defaultPort: number): { addresses: string[] } | { translatedAddresses: TranslatedAddress[] } {
    const entries = addresses.split(",");
    const translatedCount = entries.filter(entry => entry.includes(ADDRESS_TRANSLATION_SEPARATOR)).length;
    if (translatedCount !== 0 && translatedCount !== entries.length) {
        throw new ConnectionStringError(
            "inconsistent_address_translation",
            "Invalid connection string: either all addresses or none must be 'public;private' translation pairs."
        );
    }

    const schemes: string[] = [];
    let result: { addresses: string[] } | { translatedAddresses: TranslatedAddress[] };
    if (translatedCount === 0) {
        result = { addresses: entries.map(entry => parseAddress(entry, defaultPort, schemes)) };
    } else {
        result = {
            translatedAddresses: entries.map(entry => {
                const separator = entry.indexOf(ADDRESS_TRANSLATION_SEPARATOR);
                return {
                    external: parseAddress(entry.slice(0, separator), defaultPort, schemes),
                    internal: parseAddress(entry.slice(separator + 1), defaultPort, schemes),
                };
            }),
        };
    }

    if (schemes.some(scheme => scheme !== schemes[0])) {
        throw new ConnectionStringError(
            "inconsistent_schemes",
            "Invalid connection string: all addresses must use the same scheme, as one TLS configuration applies to the whole connection."
        );
    }
    return result;
}

/**
 * Parses a single address, requiring an explicit `http://` or `https://` scheme and supplying
 * `defaultPort` when the address does not specify one. Records the scheme in `schemes`.
 */
function parseAddress(address: string, defaultPort: number, schemes: string[]): string {
    if (address.length === 0) throw invalid("expected a non-empty address");
    const schemeMatch = SCHEME_REGEX.exec(address);
    const scheme = schemeMatch?.[1].toLowerCase();
    if (scheme !== "http" && scheme !== "https") {
        throw new ConnectionStringError(
            "address_invalid_scheme",
            `Invalid connection string: address '${address}' must specify either the 'http://' or the 'https://' scheme.`
        );
    }
    schemes.push(scheme);

    const authority = schemeMatch[2];
    const authorityMatch = AUTHORITY_REGEX.exec(authority);
    let host: string;
    let port: string;
    if (authorityMatch) {
        host = authorityMatch[1];
        port = authorityMatch[2] ?? String(defaultPort);
    } else if (authority.includes(":") && !authority.includes("[")) {
        // An unbracketed IPv6 literal must gain the brackets that separate its colons from the port's.
        host = `[${authority}]`;
        port = String(defaultPort);
    } else {
        throw invalid(`expected '${address}' to be a valid address`);
    }
    if (host.length === 0) throw invalid(`expected '${address}' to specify a host`);

    const normalized = `${scheme}://${host}:${port}`;
    try {
        new URL(normalized);
    } catch {
        throw invalid(`expected '${address}' to be a valid address`);
    }
    return normalized;
}
