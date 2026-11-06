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

use typedb_driver::{Addresses, Credentials, DriverOptions, DriverTlsConfig, TypeDBDriver};

/// The server's HTTP endpoint, which TypeDB Studio writes into connection strings.
const HTTP_ADDRESS: &str = "http://127.0.0.1:8000";

/// Opening a gRPC connection to the HTTP endpoint is answered with a plain HTTP status rather than
/// a gRPC status. This is the signal the connection-string fallback relies on, so it is pinned here.
#[test]
fn grpc_open_on_http_port_is_answered_with_http() {
    async_std::task::block_on(async {
        let error = TypeDBDriver::new(
            Addresses::try_from_address_str(HTTP_ADDRESS).unwrap(),
            Credentials::new("admin", "password"),
            DriverOptions::new(DriverTlsConfig::disabled()),
        )
        .await
        .expect_err("Expected a gRPC open against the HTTP endpoint to fail");
        let message = error.to_string();
        assert!(message.contains("mapped from HTTP status code"), "Expected an HTTP-mapped status, got: {message}");
    });
}

/// A connection string written for the HTTP endpoint, as by TypeDB Studio, opens a gRPC connection
/// to the same server by falling back from port 8000 to port 1729.
#[test]
fn connection_string_for_http_port_falls_back_to_grpc_port() {
    async_std::task::block_on(async {
        let driver = TypeDBDriver::new_from_connection_string(
            format!("typedb://admin:password@{HTTP_ADDRESS}/some-db?name=Studio"),
            None,
        )
        .await
        .expect("Expected the connection string to connect through the fallback");
        assert!(driver.is_open());
        driver.databases().all().await.expect("Expected a working connection");
    });
}

/// Only an HTTP answer triggers the fallback: a port that nobody listens on is reported as such.
#[test]
fn connection_string_for_closed_port_does_not_fall_back() {
    async_std::task::block_on(async {
        let error = TypeDBDriver::new_from_connection_string("typedb://admin:password@http://127.0.0.1:8001", None)
            .await
            .expect_err("Expected a closed port to fail");
        let message = error.to_string();
        assert!(message.contains("Connection refused"), "Expected a refused connection, got: {message}");
        assert!(!message.contains("retried"), "Expected no fallback for a closed port, got: {message}");
    });
}
