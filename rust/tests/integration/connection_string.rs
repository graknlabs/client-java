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

use std::collections::{HashMap, HashSet};

use serde_json::Value;
use typedb_driver::{Addresses, ConnectionString};

/// The conformance vectors shared with the standalone HTTP driver, which parses connection strings
/// with its own implementation. See the file's own comment.
const VECTORS: &str = include_str!("../../../tool/test/connection-string-vectors.json");

/// The default port the Rust core applies to an address that does not specify one. The HTTP driver
/// substitutes its own port for the same placeholder.
const DEFAULT_PORT: &str = "1729";

#[test]
fn connection_string_vectors() {
    let vectors = serde_json::from_str::<Value>(VECTORS).expect("Expected parseable vectors");
    let vectors = vectors["vectors"].as_array().expect("Expected an array of vectors").clone();
    assert!(!vectors.is_empty(), "Expected at least one vector");

    let mut failures = Vec::new();
    for vector in vectors {
        let name = vector["name"].as_str().expect("Expected a vector name").to_owned();
        let connection_string = vector["connection_string"].as_str().expect("Expected a connection string");
        let parsed = connection_string.parse::<ConnectionString>();

        let outcome = match (&vector["expect"], &vector["error"]) {
            (Value::Object(expected), Value::Null) => check_success(parsed, expected),
            (Value::Null, Value::String(expected)) => check_error(parsed, expected),
            _ => panic!("Expected vector '{name}' to specify exactly one of 'expect' and 'error'"),
        };
        if let Err(failure) = outcome {
            failures.push(format!("  {name}: {failure}"));
        }
    }
    assert!(failures.is_empty(), "Connection string vectors failed:\n{}", failures.join("\n"));
}

fn check_success(
    parsed: typedb_driver::Result<ConnectionString>,
    expected: &serde_json::Map<String, Value>,
) -> Result<(), String> {
    let parsed = parsed.map_err(|error| format!("expected a successful parse, but got: {error}"))?;

    let expected_username = expected["username"].as_str().expect("Expected a username");
    if parsed.username() != expected_username {
        return Err(format!("expected username '{expected_username}', but got '{}'", parsed.username()));
    }
    let expected_password = expected["password"].as_str().expect("Expected a password");
    if parsed.password() != expected_password {
        return Err(format!("expected password '{expected_password}', but got '{}'", parsed.password()));
    }
    let expected_tls = expected["tls"].as_bool().expect("Expected a tls flag");
    if parsed.is_tls_enabled() != expected_tls {
        return Err(format!("expected tls '{expected_tls}', but got '{}'", parsed.is_tls_enabled()));
    }

    match (parsed.addresses(), expected.get("direct"), expected.get("translated")) {
        (Addresses::Direct(addresses), Some(expected_addresses), None) => {
            let addresses: Vec<String> = addresses.iter().map(|address| address.to_string()).collect();
            let expected_addresses: Vec<String> = expected_addresses
                .as_array()
                .expect("Expected an array of addresses")
                .iter()
                .map(|address| with_default_port(address.as_str().expect("Expected an address")))
                .collect();
            if addresses != expected_addresses {
                return Err(format!("expected addresses {expected_addresses:?}, but got {addresses:?}"));
            }
        }
        (Addresses::Translated(translation), None, Some(expected_translation)) => {
            let translation: HashMap<String, String> =
                translation.iter().map(|(public, private)| (public.to_string(), private.to_string())).collect();
            let expected_translation: HashMap<String, String> = expected_translation
                .as_object()
                .expect("Expected a translation map")
                .iter()
                .map(|(public, private)| {
                    (with_default_port(public), with_default_port(private.as_str().expect("Expected an address")))
                })
                .collect();
            if translation != expected_translation {
                return Err(format!("expected translation {expected_translation:?}, but got {translation:?}"));
            }
        }
        (addresses, _, _) => {
            return Err(format!("expected the vector's address form to match the parsed {addresses:?}"));
        }
    }
    Ok(())
}

fn check_error(parsed: typedb_driver::Result<ConnectionString>, expected: &str) -> Result<(), String> {
    const ERRORS: [(&str, &str); 4] = [
        ("invalid", "Invalid connection string:"),
        ("address_invalid_scheme", "must specify either the 'http://' or the 'https://' scheme"),
        ("inconsistent_schemes", "all addresses must use the same scheme"),
        ("inconsistent_address_translation", "either all addresses or none must be"),
    ];
    let errors: HashMap<&str, &str> = ERRORS.into_iter().collect();
    let expected_message =
        errors.get(expected).unwrap_or_else(|| panic!("Expected a known error name, but got '{expected}'"));

    match parsed {
        Ok(parsed) => Err(format!("expected the parse to fail with '{expected}', but got {parsed:?}")),
        Err(error) => {
            let message = error.to_string();
            // Every error message is prefixed with "Invalid connection string:", so a vector naming
            // a specific error must not be satisfied by the generic one.
            let unexpected: HashSet<&str> = ERRORS
                .iter()
                .filter(|(name, pattern)| *name != expected && *name != "invalid" && message.contains(pattern))
                .map(|(name, _)| *name)
                .collect();
            if !unexpected.is_empty() {
                return Err(format!("expected '{expected}', but the error matches {unexpected:?}: {message}"));
            }
            if !message.contains(expected_message) {
                return Err(format!("expected an error matching '{expected}', but got: {message}"));
            }
            Ok(())
        }
    }
}

fn with_default_port(address: &str) -> String {
    address.replace("{port}", DEFAULT_PORT)
}

#[test]
fn fallback_addresses_replace_only_the_http_default_port() {
    let parsed = "typedb://admin:password@https://node-1:8000,https://node-2:1730,https://node-3"
        .parse::<ConnectionString>()
        .expect("Expected a successful parse");
    let fallback = parsed.fallback_addresses().expect("Expected a fallback, since an address carries port 8000");
    let Addresses::Direct(addresses) = fallback else { panic!("Expected direct addresses") };
    let addresses: Vec<String> = addresses.iter().map(|address| address.to_string()).collect();
    assert_eq!(addresses, vec!["node-1:1729", "node-2:1730", "node-3:1729"]);

    let parsed = "typedb://admin:password@https://typedb-cloud.ext:8000;https://127.0.0.1:8000"
        .parse::<ConnectionString>()
        .expect("Expected a successful parse");
    let Some(Addresses::Translated(translation)) = parsed.fallback_addresses() else {
        panic!("Expected a translated fallback")
    };
    let translation: Vec<(String, String)> =
        translation.iter().map(|(public, private)| (public.to_string(), private.to_string())).collect();
    assert_eq!(translation, vec![("typedb-cloud.ext:1729".to_owned(), "127.0.0.1:1729".to_owned())]);

    let parsed = "typedb://admin:password@https://node-1:1729,https://node-2"
        .parse::<ConnectionString>()
        .expect("Expected a successful parse");
    assert!(parsed.fallback_addresses().is_none(), "Expected no fallback when no address carries port 8000");
}

/// The password must not reach a log, so it must not be in the `Debug` output either.
#[test]
fn debug_output_omits_the_password() {
    let connection_string =
        "typedb://admin:shibboleth@https://127.0.0.1:1729".parse::<ConnectionString>().expect("Expected a parse");
    let debugged = format!("{connection_string:?}");
    assert!(!debugged.contains("shibboleth"), "Expected the password to be absent from: {debugged}");
    assert!(debugged.contains("admin"), "Expected the username to be present in: {debugged}");
}
