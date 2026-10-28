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
import { createFetch, Fetch, TlsConfigError } from "./transport";
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
//   HDR2  - local driver error: could not reach the server
//   SRV14 - server returned "Not yet initialised". Happens on a freshly restarted secondary
//           that has not yet loaded the system DB, or briefly on a node mid-promotion. Either
//           way another origin is likely healthy, so fall back rather than retry in place.
//   CSV8/CSV9 - node is not the primary; another origin may be, so fall back.
const TRANSIENT_SIGNIN_ERROR_CODES = new Set(["HDR2", "SRV14", "CSV8", "CSV9"]);

// A misdirected (421) result that survived redirect-following and origin fallback means
// no reachable server is primary right now (typically mid-election); wait and retry.
const PRIMARY_FAILOVER_RETRIES = 3;
const PRIMARY_SELECTION_WAIT_MS = 2000;

// Result of attempting a single request against a single origin.
//   Response          - reached the server and got an HTTP response
//   ApiErrorResponse  - reached the server but signin returned an error (e.g. 401);
//                       distinct from a Response so callers don't retry on other origins
//   null              - could not reach the server (network/connection failure)
type TryResult = Response | ApiErrorResponse | null;

function isApiErrResp(value: TryResult): value is ApiErrorResponse {
    return value !== null && "err" in value;
}

// Surface transient signin errors as null so tryApiReq's caller falls back to other origins.
// Any other err response was returned by the server itself (e.g. 401 "Invalid credential
// supplied") and must be propagated, since fallback origins would reject the same credentials
// the same way.
function tokenErrToResult(tokenResp: ApiErrorResponse): ApiErrorResponse | null {
    return TRANSIENT_SIGNIN_ERROR_CODES.has(tokenResp.err.code) ? null : tokenResp;
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
    private lastConnectionError?: string;
    private readonly fetch: Fetch;

    constructor(private params: DriverParams) {
        this.currentOrigin = remoteOrigin(params);
        this.fetch = createFetch(params.tls);
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
        this.lastConnectionError = undefined;
        const triedOrigins = new Set([this.currentOrigin]);
        const initial = await this.tryApiReq(method, path, body, options);
        if (isApiErrResp(initial)) return initial;
        let result: Response | null = initial;
        if (result !== null && result.status === HTTP_MISDIRECTED) {
            const redirected = await this.followMisdirected(result, method, path, body, options);
            if (isApiErrResp(redirected)) return redirected;
            result = redirected;
        }
        if (result === null || !result.ok) {
            const startOrigin = this.currentOrigin;
            triedOrigins.add(startOrigin);
            const fallback = await this.tryFallbackOrigins(method, path, body, options, triedOrigins);
            if (isApiErrResp(fallback)) return fallback;
            if (fallback !== null && (result === null || fallback.ok)) {
                result = fallback;
            } else if (result !== null) {
                // We're returning result from startOrigin, not a fallback. tryFallbackOrigins may
                // have shifted currentOrigin while iterating; restore so the next request goes
                // back to the same server that produced this response. Skipped when result is
                // null (startOrigin was unreachable) so we don't re-pin the driver to a dead
                // server — the responsive fallback we landed on is a better next-attempt target.
                this.switchOrigin(startOrigin);
            }
        }
        if (result !== null) return result;
        const reason = this.lastConnectionError ? ` (last error: ${this.lastConnectionError})` : "";
        return driverError("HDR1", `Cannot connect to any server${reason}`);
    }

    private async tryApiReq<BODY>(method: string, path: string, body?: BODY, options?: { headers?: Record<string, string> }): Promise<TryResult> {
        const url = `${this.currentOrigin}${path}`;
        let tokenResp = await this.getToken();
        if ("err" in tokenResp) return tokenErrToResult(tokenResp);
        const bodyString = body !== undefined ? stringifyJson(body) : undefined;
        let headers = this.authHeaders(tokenResp.ok.token, options);
        let resp: Response;
        try {
            resp = await this.fetch(url, { method, body: bodyString, headers });
        } catch (error) {
            this.connectionFailed(error);
            return null;
        }
        if (resp.status === HTTP_UNAUTHORIZED) {
            tokenResp = await this.refreshToken();
            if ("err" in tokenResp) return tokenErrToResult(tokenResp);
            headers = this.authHeaders(tokenResp.ok.token, options);
            try {
                resp = await this.fetch(url, { method, body: bodyString, headers });
            } catch (error) {
                this.connectionFailed(error);
                return null;
            }
        }
        return resp;
    }

    private async followMisdirected<BODY>(resp: Response, method: string, path: string, body?: BODY, options?: { headers?: Record<string, string> }): Promise<TryResult> {
        if (await this.switchToRedirectTarget(resp)) {
            return await this.tryApiReq(method, path, body, options);
        }
        return null;
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

    private async tryFallbackOrigins<BODY>(method: string, path: string, body: BODY | undefined, options: { headers?: Record<string, string> } | undefined, triedOrigins: Set<string>): Promise<TryResult> {
        const origins = allOrigins(this.params);
        let lastError: Response | null = null;
        for (const origin of origins) {
            if (triedOrigins.has(origin)) continue;
            this.switchOrigin(origin);
            const initial = await this.tryApiReq(method, path, body, options);
            if (initial === null) continue;
            if (isApiErrResp(initial)) return initial;
            let result: Response = initial;
            if (result.status === HTTP_MISDIRECTED) {
                const redirected = await this.followMisdirected(result, method, path, body, options);
                if (redirected === null) continue;
                if (isApiErrResp(redirected)) return redirected;
                result = redirected;
            }
            if (result.ok) return result;
            lastError = result;
        }
        return lastError;
    }

    private connectionFailed(error: unknown): string {
        this.lastConnectionError = `Cannot connect to server at ${this.currentOrigin}: ${errorMessage(error)}`;
        return this.lastConnectionError;
    }

    private switchOrigin(origin: string): void {
        this.currentOrigin = origin;
        this.token = undefined;
    }

    private authHeaders(token: string, options?: { headers?: Record<string, string> }): Record<string, string> {
        return { "Authorization": `Bearer ${token}`, "Content-Type": "application/json", ...options?.headers };
    }

    private getToken(): Promise<ApiResponse<SignInResponse>> {
        if (this.token) {
            const resp: ApiResponse<SignInResponse> = { ok: { token: this.token } };
            return Promise.resolve(resp);
        } else return this.refreshToken();
    }

    private async refreshToken(): Promise<ApiResponse<SignInResponse>> {
        return this.signinOnce();
    }

    private async signinOnce(): Promise<ApiResponse<SignInResponse>> {
        const url = `${this.currentOrigin}/v1/signin`;
        const body = { username: this.params.username, password: this.params.password };
        let resp: Response;
        try {
            resp = await this.fetch(url, { method: "POST", body: stringifyJson(body), headers: { "Content-Type": "application/json" } });
        } catch (error) {
            if (error instanceof TlsConfigError) return driverError("HDR3", error.message);
            return driverError("HDR2", this.connectionFailed(error));
        }
        const json = await this.jsonOrNull(resp);
        if (resp.ok && typeof json?.token === "string") {
            this.token = json.token;
            return { ok: json };
        }
        if (isApiError(json)) return { err: json, status: resp.status };
        return driverError("HDR2", `Unexpected signin response from ${this.currentOrigin} (HTTP ${resp.status})`);
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
