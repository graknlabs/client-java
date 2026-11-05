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

use std::{collections::HashMap, fmt, str::FromStr};

use http::{Uri, uri::Scheme};

use crate::{
    common::{
        Error, Result,
        address::{Address, Addresses},
    },
    error::ConnectionError,
};

/// The URI scheme that identifies a TypeDB connection string.
const CONNECTION_STRING_SCHEME: &str = "typedb://";

/// The port assumed for addresses that do not specify one.
pub const DEFAULT_PORT: u16 = 1729;

/// The HTTP endpoint's default port, which producers such as TypeDB Studio write into connection
/// strings. See [`ConnectionString::fallback_addresses`].
pub const HTTP_DEFAULT_PORT: u16 = 8000;

/// Separates the public and private halves of a translated address pair.
const ADDRESS_TRANSLATION_SEPARATOR: char = ';';

/// A parsed TypeDB connection string, bundling everything needed to open a driver into a single
/// value: `typedb://<username>:<password>@<addresses>[/<database>][?<params>]`.
///
/// Addresses are comma-separated, and each one must carry an explicit `http://` or `https://`
/// scheme; all addresses must agree on it, since one TLS configuration covers the whole
/// connection. Each address may instead be a `public;private` pair, which produces
/// [`Addresses::Translated`]; pairs cannot be mixed with plain addresses.
///
/// An address without a port is assumed to be on [`DEFAULT_PORT`]; an explicit port is kept as
/// written. The database path segment and any query parameters are accepted and discarded: they
/// are meaningful to connection-string producers such as TypeDB Studio, but not to the driver.
///
/// # Examples
///
/// ```rust
/// let connection_string: ConnectionString = "typedb://admin:password@https://127.0.0.1:1729".parse()?;
/// ```
#[derive(Clone, PartialEq, Eq)]
pub struct ConnectionString {
    addresses: Addresses,
    username: String,
    password: String,
    is_tls_enabled: bool,
}

impl ConnectionString {
    /// Retrieves the addresses to connect to.
    pub fn addresses(&self) -> &Addresses {
        &self.addresses
    }

    /// Retrieves the username to connect as.
    pub fn username(&self) -> &str {
        &self.username
    }

    /// Retrieves the password to connect with.
    pub fn password(&self) -> &str {
        &self.password
    }

    /// Retrieves whether the addresses' scheme requires TLS.
    pub fn is_tls_enabled(&self) -> bool {
        self.is_tls_enabled
    }

    /// The addresses with every occurrence of the HTTP endpoint's default port replaced by
    /// [`DEFAULT_PORT`], or `None` if no address carries it. A connection string produced for the
    /// HTTP endpoint, as by TypeDB Studio, names the same server as the gRPC endpoint, so a driver
    /// whose connection to the written port is answered with HTTP rather than gRPC retries here.
    pub fn fallback_addresses(&self) -> Option<Addresses> {
        let replace = |address: &Address| match address.port() {
            Some(HTTP_DEFAULT_PORT) => Some(address.with_port(DEFAULT_PORT)),
            _ => None,
        };
        match &self.addresses {
            Addresses::Direct(addresses) => {
                if !addresses.iter().any(|address| replace(address).is_some()) {
                    return None;
                }
                Some(Addresses::from_addresses(
                    addresses.iter().map(|address| replace(address).unwrap_or_else(|| address.clone())),
                ))
            }
            Addresses::Translated(translation) => {
                if !translation.iter().any(|(public, private)| replace(public).is_some() || replace(private).is_some())
                {
                    return None;
                }
                Some(Addresses::from_translation(
                    translation
                        .iter()
                        .map(|(public, private)| {
                            (
                                replace(public).unwrap_or_else(|| public.clone()),
                                replace(private).unwrap_or_else(|| private.clone()),
                            )
                        })
                        .collect(),
                ))
            }
        }
    }

    fn parse_with_default_port(value: &str, default_port: u16) -> Result<Self> {
        let Some(remainder) = value.strip_prefix(CONNECTION_STRING_SCHEME) else {
            return Err(Self::invalid(format!(
                "expected the connection string to start with '{CONNECTION_STRING_SCHEME}'"
            )));
        };

        let (credentials, remainder) = Self::split_credentials(remainder)?;
        let (username, password) = Self::parse_credentials(credentials)?;
        let addresses = Self::split_addresses(remainder)?;
        let (addresses, is_tls_enabled) = Self::parse_addresses(addresses, default_port)?;
        Ok(Self { addresses, username, password, is_tls_enabled })
    }

    /// Splits the credentials from the addresses at the last `@` preceding any query string. The
    /// last one, because a password may itself contain an unencoded `@`.
    fn split_credentials(remainder: &str) -> Result<(&str, &str)> {
        let searchable = match remainder.find('?') {
            Some(query_start) => &remainder[..query_start],
            None => remainder,
        };
        let Some(separator) = searchable.rfind('@') else {
            return Err(Self::invalid("expected credentials, separated from the addresses by '@'"));
        };
        Ok((&remainder[..separator], &remainder[separator + 1..]))
    }

    fn parse_credentials(credentials: &str) -> Result<(String, String)> {
        let Some((username, password)) = credentials.split_once(':') else {
            return Err(Self::invalid("expected a username and a password, separated by ':'"));
        };
        if username.is_empty() {
            return Err(Self::invalid("expected a non-empty username"));
        }
        if password.is_empty() {
            return Err(Self::invalid("expected a non-empty password"));
        }
        Ok((percent_decode(username, "username")?, percent_decode(password, "password")?))
    }

    /// Takes the addresses from the start of `remainder`, discarding the database path segment and
    /// the query parameters that may follow them.
    fn split_addresses(remainder: &str) -> Result<&str> {
        // Addresses carry their own "://", so only look for the terminating '/' beyond the last one.
        let search_start = remainder.rfind("://").map(|index| index + "://".len()).unwrap_or(0);
        let terminator =
            remainder[search_start..].find(['/', '?']).map(|index| index + search_start).unwrap_or(remainder.len());
        let addresses = &remainder[..terminator];
        if addresses.is_empty() {
            return Err(Self::invalid("expected at least one address"));
        }
        Ok(addresses)
    }

    fn parse_addresses(addresses: &str, default_port: u16) -> Result<(Addresses, bool)> {
        let entries: Vec<&str> = addresses.split(',').collect();
        let translated_count = entries.iter().filter(|entry| entry.contains(ADDRESS_TRANSLATION_SEPARATOR)).count();
        if translated_count != 0 && translated_count != entries.len() {
            return Err(Error::Connection(ConnectionError::ConnectionStringInconsistentAddressTranslation {}));
        }

        let mut schemes = Vec::with_capacity(entries.len());
        let addresses = if translated_count == 0 {
            let mut parsed = Vec::with_capacity(entries.len());
            for entry in entries {
                let (address, scheme) = parse_address(entry, default_port)?;
                parsed.push(address);
                schemes.push(scheme);
            }
            Addresses::from_addresses(parsed)
        } else {
            let mut parsed = HashMap::with_capacity(entries.len());
            for entry in entries {
                let Some((public, private)) = entry.split_once(ADDRESS_TRANSLATION_SEPARATOR) else {
                    unreachable!("every entry was found to contain the translation separator")
                };
                let (public, public_scheme) = parse_address(public, default_port)?;
                let (private, private_scheme) = parse_address(private, default_port)?;
                parsed.insert(public, private);
                schemes.push(public_scheme);
                schemes.push(private_scheme);
            }
            Addresses::from_translation(parsed)
        };

        let is_tls_enabled = schemes[0] == Scheme::HTTPS;
        if schemes.iter().any(|scheme| (scheme == &Scheme::HTTPS) != is_tls_enabled) {
            return Err(Error::Connection(ConnectionError::ConnectionStringInconsistentSchemes {}));
        }
        Ok((addresses, is_tls_enabled))
    }

    fn invalid(reason: impl Into<String>) -> Error {
        Error::Connection(ConnectionError::InvalidConnectionString { reason: reason.into() })
    }
}

impl FromStr for ConnectionString {
    type Err = Error;

    fn from_str(value: &str) -> Result<Self> {
        Self::parse_with_default_port(value, DEFAULT_PORT)
    }
}

/// Deliberately omits the password, so that a connection string cannot be leaked by logging it.
impl fmt::Debug for ConnectionString {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("ConnectionString")
            .field("addresses", &self.addresses)
            .field("username", &self.username)
            .field("is_tls_enabled", &self.is_tls_enabled)
            .finish_non_exhaustive()
    }
}

/// Parses a single address, requiring an explicit `http://` or `https://` scheme and supplying
/// `default_port` when the address does not specify one.
fn parse_address(address: &str, default_port: u16) -> Result<(Address, Scheme)> {
    if address.is_empty() {
        return Err(ConnectionString::invalid("expected a non-empty address"));
    }
    let uri = address
        .parse::<Uri>()
        .map_err(|_| ConnectionString::invalid(format!("expected '{address}' to be a valid address")))?;

    let scheme = match uri.scheme() {
        Some(scheme) if scheme == &Scheme::HTTP || scheme == &Scheme::HTTPS => scheme.clone(),
        _ => {
            return Err(Error::Connection(ConnectionError::ConnectionStringAddressInvalidScheme {
                address: address.to_owned(),
            }));
        }
    };

    if uri.port().is_some() {
        return Ok((address.parse::<Address>()?, scheme));
    }

    let Some(host) = uri.host() else {
        return Err(ConnectionString::invalid(format!("expected '{address}' to specify a host")));
    };
    // An IPv6 literal must keep the brackets that separate its colons from the port's.
    let host = if host.contains(':') && !host.starts_with('[') { format!("[{host}]") } else { host.to_owned() };
    Ok((format!("{scheme}://{host}:{default_port}").parse::<Address>()?, scheme))
}

/// Decodes the percent-encoding that connection strings apply to credentials, so that a username
/// or password may contain the string's own delimiters.
fn percent_decode(value: &str, field: &str) -> Result<String> {
    let bytes = value.as_bytes();
    let mut decoded = Vec::with_capacity(bytes.len());
    let mut index = 0;
    while index < bytes.len() {
        if bytes[index] == b'%' {
            let hex = bytes
                .get(index + 1..index + 3)
                .and_then(|hex| std::str::from_utf8(hex).ok())
                .and_then(|hex| u8::from_str_radix(hex, 16).ok());
            match hex {
                Some(byte) => {
                    decoded.push(byte);
                    index += 3;
                    continue;
                }
                None => return Err(invalid_percent_encoding(field)),
            }
        }
        decoded.push(bytes[index]);
        index += 1;
    }
    String::from_utf8(decoded).map_err(|_| invalid_percent_encoding(field))
}

fn invalid_percent_encoding(field: &str) -> Error {
    ConnectionString::invalid(format!("expected the {field} to be valid percent-encoded UTF-8"))
}
