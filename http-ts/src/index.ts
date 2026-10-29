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

import { DriverParams, remoteOrigin, resolveOrigin, allOrigins } from "./params";
import { createFetch, Fetch, FetchInit, TlsConfigError } from "./transport";
import {
    AnalyzeResponse,
    ApiErrorResponse,
    ApiResponse,
    DatabasesListResponse,
    driverError,
    isApiError,
    isMisdirectedError,
    QueryResponse,
    ServersListResponse,
    SignInResponse,
    TransactionOpenResponse,
    UsersListResponse,
    VersionResponse
} from "./response";
import {Attribute, Concept, Entity, Relation, Value} from "./concept";
import { parseJson, stringifyJson } from "./json";

const HTTP_UNAUTHORIZED = 401;
const HTTP_MISDIRECTED = 421;

// Codes that mean "this origin is not currently usable for signin" — treated like a
// connection failure so the caller falls back to another origin instead of waiting:
//   SRV14 - server returned "Not yet initialised". Happens on a freshly restarted secondary
//           that has not yet loaded the system DB, or briefly on a node mid-promotion. Either
//           way another origin is likely healthy, so fall back rather than retry in place.
//   CSV8/CSV9 - node is not the primary; another origin may be, so fall back.
const TRANSIENT_SIGNIN_ERROR_CODES = new Set(["SRV14", "CSV8", "CSV9"]);

// A misdirected (421) result that survived redirect-following and origin fallback means
// no reachable server is primary right now (typically mid-election); wait and retry.
const PRIMARY_FAILOVER_RETRIES = 3;
const PRIMARY_SELECTION_WAIT_MS = 2000;

// Result of attempting a single request against a single origin.
//   Response          - reached the server and got an HTTP response
//   ApiErrorResponse  - reached the server but signin returned an error (e.g. 401);
//                       distinct from a Response so callers don't retry on other origins
//   Unreachable       - could not use the server (e.g. a connection failure), and why
type Unreachable = { unreachable: string };
type TryResult = Response | ApiErrorResponse | Unreachable;
type SignInResult = ApiResponse<SignInResponse> | Unreachable;

function isApiErrResp(value: TryResult | SignInResult): value is ApiErrorResponse {
    return "err" in value;
}

function isUnreachable(value: TryResult | SignInResult): value is Unreachable {
    return "unreachable" in value;
}

// Surface transient signin errors as Unreachable so tryApiReq's caller falls back to other origins.
// Any other err response was returned by the server itself (e.g. 401 "Invalid credential
// supplied") and must be propagated, since fallback origins would reject the same credentials
// the same way.
function tokenErrToResult(tokenResp: ApiErrorResponse, origin: string): ApiErrorResponse | Unreachable {
    const { code, message } = tokenResp.err;
    if (!TRANSIENT_SIGNIN_ERROR_CODES.has(code)) return tokenResp;
    return { unreachable: `Server at ${origin} failed signin with [${code}] ${message}` };
}

function errorMessage(error: unknown): string {
    const reason = (error as { cause?: unknown })?.cause ?? error;
    const { message, code } = (reason ?? {}) as { message?: string, code?: string };
    return (message || code || String(reason)).trim();
}

export * from "./analyze";
export * from "./concept";
export { parseJson, stringifyJson } from "./json";
export * from "./params";
export * from "./analyzed-conjunction";
export * from "./response";

export * from "./legacy";

export class TypeDBHttpDriver {

    private token?: string;
    private currentOrigin: string;
    private readonly fetch: Fetch;

    constructor(private params: DriverParams) {
        this.currentOrigin = remoteOrigin(params);
        this.fetch = createFetch(params.tls, allOrigins(params));
    }

    getDatabases(): Promise<ApiResponse<DatabasesListResponse>> {
        return this.apiGet<DatabasesListResponse>(`/v1/databases`);
    }

    getDatabase(name: string): Promise<ApiResponse<Database>> {
        return this.apiGet<Database>(`/v1/databases/${encodeURIComponent(name)}`);
    }

    createDatabase(name: string): Promise<ApiResponse> {
        return this.apiPost(`/v1/databases/${encodeURIComponent(name)}`, {});
    }

    deleteDatabase(name: string): Promise<ApiResponse> {
        return this.apiDelete(`/v1/databases/${encodeURIComponent(name)}`);
    }

    getDatabaseSchema(name: string): Promise<ApiResponse<string>> {
        return this.apiGetString(`/v1/databases/${encodeURIComponent(name)}/schema`);
    }

    getDatabaseTypeSchema(name: string): Promise<ApiResponse<string>> {
        return this.apiGetString(`/v1/databases/${encodeURIComponent(name)}/type-schema`);
    }

    getUsers(): Promise<ApiResponse<UsersListResponse>> {
        return this.apiGet<UsersListResponse>(`/v1/users`);
    }

    getCurrentUser(): Promise<ApiResponse<User>> {
        return this.apiGet<User>(`/v1/users/${encodeURIComponent(this.params.username)}`);
    }

    getUser(username: string): Promise<ApiResponse<User>> {
        return this.apiGet<User>(`/v1/users/${encodeURIComponent(username)}`);
    }

    createUser(username: string, password: string): Promise<ApiResponse> {
        return this.apiPost(`/v1/users/${encodeURIComponent(username)}`, { password });
    }

    updateUser(username: string, password: string): Promise<ApiResponse> {
        return this.apiPut(`/v1/users/${encodeURIComponent(username)}`, { password });
    }

    deleteUser(username: string): Promise<ApiResponse> {
        return this.apiDelete(`/v1/users/${encodeURIComponent(username)}`);
    }

    openTransaction(databaseName: string, transactionType: TransactionType, transactionOptions?: TransactionOptions): Promise<ApiResponse<TransactionOpenResponse>> {
        return this.apiPost<TransactionOpenResponse>(`/v1/transactions/open`, { databaseName, transactionType, transactionOptions });
    }

    commitTransaction(transactionId: string): Promise<ApiResponse> {
        return this.apiPost(`/v1/transactions/${encodeURIComponent(transactionId)}/commit`, {});
    }

    closeTransaction(transactionId: string): Promise<ApiResponse> {
        return this.apiPost(`/v1/transactions/${encodeURIComponent(transactionId)}/close`, {});
    }

    rollbackTransaction(transactionId: string): Promise<ApiResponse> {
        return this.apiPost(`/v1/transactions/${encodeURIComponent(transactionId)}/rollback`, {});
    }

    analyze(transactionId: string, query: string, analyzeOptions?: AnalyzeOptions): Promise<ApiResponse<AnalyzeResponse>> {
        return this.apiPost<AnalyzeResponse>(`/v1/transactions/${encodeURIComponent(transactionId)}/analyze`, { query, analyzeOptions });
    }

    query(transactionId: string, query: string, queryOptions?: QueryOptions, givenRows?: GivenRows): Promise<ApiResponse<QueryResponse>> {
        return this.apiPost<QueryResponse>(`/v1/transactions/${encodeURIComponent(transactionId)}/query`, { query, queryOptions, givenRows });
    }

    oneShotQuery(query: string, commit: boolean, databaseName: string, transactionType: TransactionType, transactionOptions?: TransactionOptions, queryOptions?: QueryOptions, givenRows?: GivenRows) {
        return this.apiPost<QueryResponse>(`/v1/query`, { query, commit, databaseName, transactionType, transactionOptions, queryOptions, givenRows });
    }

    health(): Promise<ApiResponse> {
        return this.apiGet(`/v1/health`);
    }

    version(): Promise<ApiResponse<VersionResponse>> {
        return this.apiGet(`/v1/version`);
    }

    getServers(): Promise<ApiResponse<ServersListResponse>> {
        return this.apiGet<ServersListResponse>(`/v1/servers`);
    }

    private async apiGetString(path: string, options?: { headers?: Record<string, string> }): Promise<ApiResponse<string>> {
        return this.stringApiReq("GET", path, options);
    }

    private async apiGet<RES = Object>(path: string, options?: { headers?: Record<string, string> }): Promise<ApiResponse<RES>> {
        return this.jsonApiReqWithoutBody("GET", path, options);
    }

    private async apiDelete<RES = Object>(path: string, options?: { headers?: Record<string, string> }): Promise<ApiResponse<RES>> {
        return this.jsonApiReqWithoutBody("DELETE", path, options);
    }

    private async apiPost<RES = Object, BODY = Object>(path: string, body: BODY, options?: { headers?: Record<string, string> }): Promise<ApiResponse<RES>> {
        return this.jsonApiReq<RES, BODY>("POST", path, body, options);
    }

    private async apiPut<RES = Object, BODY = Object>(path: string, body: BODY, options?: { headers?: Record<string, string> }): Promise<ApiResponse<RES>> {
        return this.jsonApiReq<RES, BODY>("PUT", path, body, options);
    }

    private async jsonApiReqWithoutBody<RES>(method: string, path: string, options?: { headers?: Record<string, string> }): Promise<ApiResponse<RES>> {
        return this.jsonApiReq<RES, Object>(method, path, undefined, options);
    }

    private async jsonApiReq<RES, BODY>(method: string, path: string, body?: BODY, options?: { headers?: Record<string, string> }): Promise<ApiResponse<RES>> {
        const resp = await this.apiReq(method, path, body, options);
        if ("err" in resp) return resp;
        const json = await this.jsonOrNull(resp);
        if (resp.ok) return { ok: json as RES };
        else if (isApiError(json)) return { err: json, status: resp.status };
        else throw resp;
    }

    private async stringApiReq(method: string, path: string, options?: { headers?: Record<string, string> }): Promise<ApiResponse<string>> {
        const resp = await this.apiReq(method, path, undefined, options);
        if ("err" in resp) return resp;
        if (resp.ok) return { ok: await this.stringOrNull(resp) ?? "" };
        else {
            const json = await this.jsonOrNull(resp);
            if (isApiError(json)) return { err: json, status: resp.status };
            else throw resp;
        }
    }

    private async apiReq<BODY>(method: string, path: string, body?: BODY, options?: { headers?: Record<string, string> }): Promise<ApiErrorResponse | Response> {
        let result = await this.apiReqOnce(method, path, body, options);
        for (let retry = 0; retry < PRIMARY_FAILOVER_RETRIES && result.status === HTTP_MISDIRECTED; retry++) {
            await new Promise(resolve => setTimeout(resolve, PRIMARY_SELECTION_WAIT_MS));
            result = await this.apiReqOnce(method, path, body, options);
        }
        return result;
    }

    private async apiReqOnce<BODY>(method: string, path: string, body?: BODY, options?: { headers?: Record<string, string> }): Promise<ApiErrorResponse | Response> {
        const triedOrigins = new Set([this.currentOrigin]);
        const initial = await this.tryApiReq(method, path, body, options);
        if (isApiErrResp(initial)) return initial;
        let result: Response | Unreachable = initial;
        if (!isUnreachable(result) && result.status === HTTP_MISDIRECTED) {
            const redirected = await this.followMisdirected(result, method, path, body, options);
            if (isApiErrResp(redirected)) return redirected;
            result = redirected;
        }
        if (isUnreachable(result) || !result.ok) {
            const startOrigin = this.currentOrigin;
            triedOrigins.add(startOrigin);
            const startUnreachable = isUnreachable(result)
                ? result
                : { unreachable: `Server at ${startOrigin} returned HTTP ${result.status}` };
            const fallback = await this.tryFallbackOrigins(method, path, body, options, triedOrigins, startUnreachable);
            if (isApiErrResp(fallback)) return fallback;
            if (isUnreachable(result) || (!isUnreachable(fallback) && fallback.ok)) {
                result = fallback;
            } else {
                // We're returning result from startOrigin, not a fallback. tryFallbackOrigins may
                // have shifted currentOrigin while iterating; restore so the next request goes
                // back to the same server that produced this response. Skipped when result is
                // Unreachable (startOrigin was unreachable) so we don't re-pin the driver to a dead
                // server — the responsive fallback we landed on is a better next-attempt target.
                this.switchOrigin(startOrigin);
            }
        }
        if (isUnreachable(result)) {
            return driverError("HDR1", `Cannot connect to any server (last error: ${result.unreachable})`);
        }
        return result;
    }

    private async tryApiReq<BODY>(method: string, path: string, body?: BODY, options?: { headers?: Record<string, string> }): Promise<TryResult> {
        const origin = this.currentOrigin;
        let tokenResp = await this.getToken();
        if (isUnreachable(tokenResp)) return tokenResp;
        if (isApiErrResp(tokenResp)) return tokenErrToResult(tokenResp, origin);
        const bodyString = body !== undefined ? stringifyJson(body) : undefined;
        let headers = this.authHeaders(tokenResp.ok.token, options);
        let resp = await this.fetchFrom(origin, path, { method, body: bodyString, headers });
        if (!isApiErrResp(resp) && !isUnreachable(resp) && resp.status === HTTP_UNAUTHORIZED) {
            tokenResp = await this.refreshToken();
            if (isUnreachable(tokenResp)) return tokenResp;
            if (isApiErrResp(tokenResp)) return tokenErrToResult(tokenResp, origin);
            headers = this.authHeaders(tokenResp.ok.token, options);
            resp = await this.fetchFrom(origin, path, { method, body: bodyString, headers });
        }
        return resp;
    }

    private async fetchFrom(origin: string, path: string, init: FetchInit): Promise<TryResult> {
        try {
            return await this.fetch(`${origin}${path}`, init);
        } catch (error) {
            if (error instanceof TlsConfigError) return driverError("HDR3", error.message);
            return { unreachable: `Cannot connect to server at ${origin}: ${errorMessage(error)}` };
        }
    }

    private async followMisdirected<BODY>(resp: Response, method: string, path: string, body?: BODY, options?: { headers?: Record<string, string> }): Promise<TryResult> {
        const origin = this.currentOrigin;
        if (await this.switchToRedirectTarget(resp)) {
            return await this.tryApiReq(method, path, body, options);
        }
        return { unreachable: `Server at ${origin} returned 421 Misdirected Request without another primary address` };
    }

    private async switchToRedirectTarget(resp: Response): Promise<boolean> {
        let json: any;
        try {
            json = parseJson(await resp.text());
        } catch {
            return false;
        }
        if (!isMisdirectedError(json)) return false;
        const newOrigin = resolveOrigin(this.params, json.primaryAddress);
        if (newOrigin === this.currentOrigin) return false;
        this.switchOrigin(newOrigin);
        return true;
    }

    // Returns the first ok response, else the last other response, else the last reason a server was unreachable,
    // which is startUnreachable if no other origin is reachable
    private async tryFallbackOrigins<BODY>(method: string, path: string, body: BODY | undefined, options: { headers?: Record<string, string> } | undefined, triedOrigins: Set<string>, startUnreachable: Unreachable): Promise<TryResult> {
        const origins = allOrigins(this.params);
        let lastError: Response | null = null;
        let lastUnreachable = startUnreachable;
        for (const origin of origins) {
            if (triedOrigins.has(origin)) continue;
            this.switchOrigin(origin);
            const initial = await this.tryApiReq(method, path, body, options);
            if (isUnreachable(initial)) {
                lastUnreachable = initial;
                continue;
            }
            if (isApiErrResp(initial)) return initial;
            let result: Response = initial;
            if (result.status === HTTP_MISDIRECTED) {
                const redirected = await this.followMisdirected(result, method, path, body, options);
                if (isUnreachable(redirected)) {
                    lastUnreachable = redirected;
                    continue;
                }
                if (isApiErrResp(redirected)) return redirected;
                result = redirected;
            }
            if (result.ok) return result;
            lastError = result;
        }
        return lastError ?? lastUnreachable;
    }

    private switchOrigin(origin: string): void {
        this.currentOrigin = origin;
        this.token = undefined;
    }

    private authHeaders(token: string, options?: { headers?: Record<string, string> }): Record<string, string> {
        return { "Authorization": `Bearer ${token}`, "Content-Type": "application/json", ...options?.headers };
    }

    private getToken(): Promise<SignInResult> {
        if (this.token) {
            const resp: SignInResult = { ok: { token: this.token } };
            return Promise.resolve(resp);
        } else return this.refreshToken();
    }

    private async refreshToken(): Promise<SignInResult> {
        return this.signinOnce();
    }

    private async signinOnce(): Promise<SignInResult> {
        const body = { username: this.params.username, password: this.params.password };
        const resp = await this.fetchFrom(this.currentOrigin, "/v1/signin", { method: "POST", body: stringifyJson(body), headers: { "Content-Type": "application/json" } });
        if (isApiErrResp(resp) || isUnreachable(resp)) return resp;
        const json = await this.jsonOrNull(resp);
        if (resp.ok && typeof json?.token === "string") {
            this.token = json.token;
            return { ok: json };
        }
        if (isApiError(json)) return { err: json, status: resp.status };
        return { unreachable: `Unexpected signin response from ${this.currentOrigin} (HTTP ${resp.status})` };
    }

    private async jsonOrNull(resp: Response): Promise<any> {
        try {
            return parseJson(await resp.text());
        } catch {
            return null;
        }
    }

    private async stringOrNull(resp: Response) {
        const contentLengthRaw = resp.headers.get("Content-Length");
        if (!contentLengthRaw) return null;
        const contentLength = parseInt(contentLengthRaw || "");
        if (isNaN(contentLength)) throw `Received invalid Content-Length header: ${contentLengthRaw}`;
        return contentLength > 0 ? await resp.text() : null;
    }
}

export interface Database {
    name: string;
}

export type TransactionType = "read" | "write" | "schema";

export interface TransactionOptions {
    schemaLockAcquireTimeoutMillis?: number;
    transactionTimeoutMillis?: number;
}

export interface QueryOptions {
    includeInstanceTypes?: boolean;
    includeQueryStructure?: boolean;
    answerCountLimit?: number;
}

export interface AnalyzeOptions {
    include_plan?: boolean,
}

export interface User {
    username: string;
}

export type GivenRowEntry = Value | Entity | Relation | Attribute
    | boolean // for boolean values
    | number  // For doubles, and for integers below 2^53
    | bigint  // For integers from 2^53 up, which a number cannot hold exactly
    | string; // For all other types, as well as IIDs.
export type GivenRows = { [varName: string]: GivenRowEntry }[];
