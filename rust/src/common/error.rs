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

use std::{error::Error as StdError, fmt, num::ParseIntError, time::Duration};

use tonic::{Code, Status};
use tonic_types::{ErrorInfo, StatusExt};

use super::RequestID;
use crate::common::address::{Address, Addresses};

macro_rules! error_messages {
    {
        $name:ident code: $code_pfx:literal, type: $message_pfx:literal,
        $($error_name:ident $({ $($field:ident : $inner:ty),+ $(,)? })? = $code:literal: $body:literal),+ $(,)?
    } => {
        #[derive(Clone, Eq, PartialEq)]
        pub enum $name {$(
            $error_name$( { $($field: $inner),+ })?,
        )*}

        impl $name {
            pub const PREFIX: &'static str = $code_pfx;

            pub const fn code(&self) -> usize {
                match self {$(
                    Self::$error_name $({ $($field: _),+ })? => $code,
                )*}
            }

            pub fn format_code(&self) -> String {
                format!(concat!("[", $code_pfx, "{}{}]"), self.padding(), self.code())
            }

            pub fn message(&self) -> String {
                match self {$(
                    Self::$error_name $({$($field),+})? => format!($body $($(, $field = $field)+)?),
                )*}
            }

            const fn max_code() -> usize {
                let mut max = usize::MIN;
                $(max = if $code > max { $code } else { max };)*
                max
            }

            const fn num_digits(x: usize) -> usize {
                if (x < 10) { 1 } else { 1 + Self::num_digits(x/10) }
            }

            const fn padding(&self) -> &'static str {
                match Self::num_digits(Self::max_code()) - Self::num_digits(self.code()) {
                    0 => "",
                    1 => "0",
                    2 => "00",
                    3 => "000",
                    _ => unreachable!(),
                }
            }

            const fn name(&self) -> &'static str {
                match self {$(
                    Self::$error_name $({ $($field: _),+ })? => concat!(stringify!($name), "::", stringify!($error_name)),
                )*}
            }
        }

        impl fmt::Display for $name {
            fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
                write!(
                    f,
                    concat!("[", $code_pfx, "{}{}] ", $message_pfx, ": {}"),
                    self.padding(),
                    self.code(),
                    self.message()
                )
            }
        }

        impl fmt::Debug for $name {
            fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
                let mut debug_struct = f.debug_struct(self.name());
                debug_struct.field("message", &format!("{}", self));
                $(
                    $(
                        if let Self::$error_name { $($field),+ } = &self {
                            $(debug_struct.field(stringify!($field), &$field);)+
                        }
                    )?
                )*
                debug_struct.finish()
            }
        }

        impl std::error::Error for $name {
            fn source(&self) -> Option<&(dyn std::error::Error + 'static)> {
                None
            }
        }
    };
}

error_messages! { AnalyzeError
    code: "ANZ", type: "Analyze Error",
    MissingResponseField { field: &'static str } =
        1: "Missing field in message received from server: '{field}'. This is either a version compatibility issue or a bug.",
    UnknownEnumValue { enum_name: &'static str, value: i32 } =
        2: "Value '{value}' is out of bounds for enum '{enum_name}'. This is either a version compatibility issue or a bug.",
}

error_messages! { ConnectionError
    code: "CXN", type: "Connection Error",
    RPCMethodUnavailable { message: String } =
        1: "The server does not support this method, please check the driver-server compatibility:\n'{message}'.",
    ServerConnectionFailed { configured_addresses: Addresses, accessed_addresses: Addresses, details: String } =
        2: "Unable to connect to TypeDB server(s).\nInitially configured addresses: {configured_addresses}.\nTried accessing addresses: {accessed_addresses}. Details: {details}",
    ServerConnectionFailedWithError { error: String } =
        3: "Unable to connect to TypeDB server(s), received errors: \n{error}",
    ServerConnectionFailedNetworking { error: String } =
        4: "Unable to connect to TypeDB server(s), received network or protocol error: \n{error}",
    ServerConnectionIsClosed =
        5: "The connection has been closed and no further operation is allowed.",
    ServerConnectionIsClosedUnexpectedly =
        6: "The connection has been closed unexpectedly and no further operation is allowed.",
    TransactionIsClosed =
        7: "The transaction is closed and no further operation is allowed.",
    TransactionIsClosedWithErrors { errors: String } =
        8: "The transaction is closed because of the error(s):\n{errors}",
    MissingResponseField { field: &'static str } =
        9: "Missing field in message received from server: '{field}'. This is either a version compatibility issue or a bug.",
    UnknownRequestId { request_id: RequestID } =
        10: "Received a response with unknown request id '{request_id}'",
    UnexpectedResponse { response: String } =
        11: "Received unexpected response from server: '{response}'. This is either a version compatibility issue or a bug.",
    QueryStreamNoResponse =
        12: "Didn't receive any server responses for the query.",
    UnexpectedQueryType { query_type: i32 } =
        13: "Unexpected query type in message received from server: {query_type}. This is either a version compatibility issue or a bug.",
    ClusterServerNotPrimary { hint_address: Option<Address> } =
        14: "The server is not primary (primary hint: {hint_address:?}).",
    UnknownDirectServerRouting { address: Address, known_addresses: Addresses } =
        15: "Could not execute operation against '{address}' since it's not in the list of known servers: {known_addresses}.",
    TokenCredentialInvalid =
        16: "Invalid credential supplied.",
    EncryptionSettingsMismatch =
        17: "Unable to connect to TypeDB: possible encryption settings mismatch.",
    SslCertificateNotValidated =
        18: "SSL handshake with TypeDB failed: the server's identity could not be verified. Possible CA mismatch.",
    BrokenPipe =
        19: "Stream closed because of a broken pipe. This could happen if you are attempting to connect to an unencrypted TypeDB server using a TLS-enabled credentials.",
    ConnectionRefusedNetworking =
        20: "Connection refused. Please check the server is running and the address is accessible. Encrypted TypeDB endpoints may also have misconfigured SSL certificates.",
    MissingPort { address: String } =
        21: "Invalid URL '{address}': missing port.",
    UnexpectedServerReplicationRole { replication_role: i32 } =
        22: "Unexpected replication role in message received from server: {replication_role}. This is either a version compatibility issue or a bug.",
    ValueTimeZoneNameNotRecognised { time_zone: String } =
        23: "Time zone provided by the server has name '{time_zone}', which is not an officially recognized timezone.",
    ValueTimeZoneOffsetNotRecognised { offset: i32 } =
        24: "Time zone provided by the server has numerical offset '{offset}', which is not recognised as a valid value for offset in seconds.",
    ValueStructNotImplemented =
        25: "Struct valued responses are not yet supported by the driver.",
    ListsNotImplemented =
        26: "Lists are not yet supported by the driver.",
    UnexpectedKind { kind: i32 } =
        27: "Unexpected kind in message received from server: {kind}. This is either a version compatibility issue or a bug.",
    UnexpectedConnectionClose =
        28: "Connection closed unexpectedly.",
    DatabaseImportChannelIsClosed =
        29: "The database import channel is closed and no further operation is allowed.",
    DatabaseImportStreamUnexpectedResponse =
        30: "The database import stream received an unexpected response in the process. It is either a version compatibility issue or a bug.",
    DatabaseExportChannelIsClosed =
        31: "The database export channel is closed and no further operation is allowed.",
    DatabaseExportStreamNoResponse =
        32: "Didn't receive any server responses for the database export command.",
    AbsentTlsConfigForTlsConnection =
        33: "Could not establish a TLS connection without a TLS config specified. Please verify your driver options.",
    ServerIsNotInitialised =
        34: "Server is not yet initialized.",
    AnalyzeNoResponse =
        35: "Didn't receive any server responses for the analyze request.",
    NoPrimaryServer =
        37: "Could not find a primary server.",
    SchemeTlsSettingsMismatch { scheme: http::uri::Scheme, is_tls_enabled: bool } =
        38: "Scheme {scheme} is not compatible with tls setting `enabled: {is_tls_enabled}`",
    RequestTimeout { timeout: String } =
        39: "Request timed out after {timeout}. The server may be unresponsive.",
}

impl ConnectionError {
    pub fn request_timeout(timeout: Duration) -> Self {
        let (value, decimal, unit) = if timeout.as_secs() > 0 {
            let decimal = timeout.subsec_millis();
            (timeout.as_secs(), (decimal > 0).then_some(decimal), "seconds")
        } else if timeout.subsec_millis() > 0 {
            let decimal = timeout.subsec_micros() % 1000;
            (timeout.subsec_millis() as u64, (decimal > 0).then_some(decimal), "milliseconds")
        } else if timeout.subsec_micros() > 0 {
            let decimal = timeout.subsec_nanos() % 1000;
            (timeout.subsec_micros() as u64, (decimal > 0).then_some(decimal), "microseconds")
        } else {
            (timeout.subsec_nanos() as u64, None, "nanoseconds")
        };

        let timeout_str = match decimal {
            Some(d) => format!("{value}.{d:03} {unit}"),
            None => format!("{value} {unit}"),
        };
        ConnectionError::RequestTimeout { timeout: timeout_str }
    }
}

error_messages! { ConceptError
    code: "CPT", type: "Concept Error",
    UnavailableRowVariable { variable: String } =
        1: "Cannot get concept from a concept row by variable '{variable}'.",
    UnavailableRowIndex { index: usize } =
        2: "Cannot get concept from a concept row by index '{index}'.",
    ErrorParsingDecimal { unparsed: String, reason: String } =
        3: "Could not parse the value '{unparsed}' as decimal: {reason}.",
}

error_messages! { MigrationError
    code: "MGT", type: "Migration Error",
    CannotOpenImportFile { path: String, reason: String } =
        1: "Cannot open import file '{path}': {reason}",
    CannotCreateExportFile { path: String, reason: String } =
        2: "Cannot create export file '{path}': {reason}",
    CannotExportToTheSameFile =
        3: "Cannot export both schema and data to the same file.",
    CannotDecodeImportedConcept =
        4: "Cannot decode a concept from the provided import file. Make sure to pass a correct database file produced by a TypeDB export operation.",
    CannotDecodeImportedConceptLength =
        5: "Cannot decode a concept length from the provided import file. Make sure to pass a correct database file produced by a TypeDB export operation.",
    CannotEncodeExportedConcept =
        6: "Cannot encode a concept for export. It's either a version compatibility error or a bug.",
    CannotReadImportFile { reason: String } =
        7: "Cannot read the import file: {reason}.",
}

error_messages! { InternalError
    code: "INT", type: "Internal Error",
    RecvError =
        1: "Channel is closed.",
    SendError =
        2: "Unable to send response over callback channel (receiver dropped).",
    UnexpectedRequestType { request_type: String } =
        3: "Unexpected request type for remote procedure call: {request_type}. This is either a version compatibility issue or a bug.",
    UnexpectedResponseType { response_type: String } =
        4: "Unexpected response type for remote procedure call: {response_type}. This is either a version compatibility issue or a bug.",
    Unimplemented { details: String } =
        5: "Unimplemented feature: {details}.",
}

error_messages! { QueryError
    code: "QRY", type: "Internal Error",
    GivenRowIndexOutOfBounds { index: usize, width: usize } =
        1: "Attempted to set a row entry at index: {index}. Row width is {width}.",
    GivenRowUnknownVariable { variable: String } =
        2: "Attempted to set a row entry for a variable {variable} which was not in the header.",
    GivenRowsSizeMismatch { actual: usize, expected: usize } =
        3: "Cannot push a row of width: {actual} . Expected width: {expected}",
    GivenRowsReceivedType { variable_or_index: String } =
        4: "A type was passed as given row entry for '{variable_or_index}'. Only instances and values are allowed.",
    InvalidTypeToGivenRow =
        5: "Types cannot be converted to GivenRowEntry. Only instances and values can.",
}

#[derive(Clone, PartialEq, Eq)]
pub struct ServerError {
    error_code: String,
    error_domain: String,
    message: String,
    stack_trace: Vec<String>,
}

impl ServerError {
    pub(crate) fn new(error_code: String, error_domain: String, message: String, stack_trace: Vec<String>) -> Self {
        Self { error_code, error_domain, message, stack_trace }
    }

    pub(crate) fn format_code(&self) -> &str {
        &self.error_code
    }

    pub(crate) fn message(&self) -> String {
        self.to_string()
    }
}

impl fmt::Display for ServerError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        if self.stack_trace.is_empty() {
            write!(f, "[{}] {}. {}", self.error_code, self.error_domain, self.message)
        } else {
            write!(f, "\n{}", self.stack_trace.join("\nCaused: "))
        }
    }
}

impl fmt::Debug for ServerError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        fmt::Display::fmt(self, f)
    }
}

/// Represents errors encountered during operation.
#[derive(Clone, Debug, PartialEq, Eq)]
pub enum Error {
    Analyze(AnalyzeError),
    Connection(ConnectionError),
    Concept(ConceptError),
    Migration(MigrationError),
    Internal(InternalError),
    Query(QueryError),
    Server(ServerError),
    Other(String),
    FFI(String),
}

impl Error {
    pub fn code(&self) -> String {
        match self {
            Self::Analyze(error) => error.format_code(),
            Self::Connection(error) => error.format_code(),
            Self::Concept(error) => error.format_code(),
            Self::Migration(error) => error.format_code(),
            Self::Internal(error) => error.format_code(),
            Self::Query(error) => error.format_code(),
            Self::Server(error) => error.format_code().to_owned(),
            Self::FFI(_error) => String::new(),
            Self::Other(_error) => String::new(),
        }
    }

    pub fn message(&self) -> String {
        match self {
            Self::Analyze(error) => error.message(),
            Self::Connection(error) => error.message(),
            Self::Concept(error) => error.message(),
            Self::Migration(error) => error.message(),
            Self::Internal(error) => error.message(),
            Self::Query(error) => error.message(),
            Self::Server(error) => error.message(),
            Self::FFI(error) => error.clone(),
            Self::Other(error) => error.clone(),
        }
    }

    fn try_extracting_connection_error_from_info(error_info: &ErrorInfo) -> Option<ConnectionError> {
        match error_info.reason.as_str() {
            "AUT1" | "AUT2" | "AUT3" => Some(ConnectionError::TokenCredentialInvalid {}),
            "SRV14" | "RFT1" | "CSV7" => Some(ConnectionError::ServerIsNotInitialised {}),
            "CSV8" => Some(ConnectionError::ClusterServerNotPrimary { hint_address: None }),
            "CSV9" => Some(ConnectionError::ClusterServerNotPrimary {
                hint_address: error_info.metadata.get("redirect_address").and_then(|addr| addr.parse::<Address>().ok()),
            }),
            _ => None,
        }
    }

    fn try_extracting_connection_error_message(message: &str) -> Option<ConnectionError> {
        if is_rst_stream(message) || is_tcp_connect_error(message) || is_connection_error(message) {
            Some(ConnectionError::ServerConnectionFailedNetworking { error: message.to_string() })
        } else if is_reading_body_from_connection_error(message) {
            Some(ConnectionError::ServerConnectionIsClosedUnexpectedly)
        } else {
            None
        }
    }

    fn parse_unavailable(status_message: &str) -> Error {
        if status_message == "broken pipe" {
            Error::Connection(ConnectionError::BrokenPipe)
        } else if status_message.contains("received corrupt message") {
            Error::Connection(ConnectionError::EncryptionSettingsMismatch)
        } else if status_message.contains("UnknownIssuer") {
            Error::Connection(ConnectionError::SslCertificateNotValidated)
        } else if status_message.contains("Connection refused") {
            Error::Connection(ConnectionError::ConnectionRefusedNetworking)
        } else {
            Error::Connection(ConnectionError::ServerConnectionFailedNetworking { error: status_message.to_owned() })
        }
    }
}

impl fmt::Display for Error {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::Analyze(error) => write!(f, "{error}"),
            Self::Connection(error) => write!(f, "{error}"),
            Self::Concept(error) => write!(f, "{error}"),
            Self::Migration(error) => write!(f, "{error}"),
            Self::Internal(error) => write!(f, "{error}"),
            Self::Query(error) => write!(f, "{error}"),
            Self::Server(error) => write!(f, "{error}"),
            Self::FFI(message) => write!(f, "{message}"),
            Self::Other(message) => write!(f, "{message}"),
        }
    }
}

impl StdError for Error {
    fn source(&self) -> Option<&(dyn StdError + 'static)> {
        match self {
            Self::Analyze(error) => Some(error),
            Self::Connection(error) => Some(error),
            Self::Concept(error) => Some(error),
            Self::Migration(error) => Some(error),
            Self::Internal(error) => Some(error),
            Self::Query(_) => None,
            Self::Server(_) => None,
            Self::FFI(_) => None,
            Self::Other(_) => None,
        }
    }
}

impl From<AnalyzeError> for Error {
    fn from(error: AnalyzeError) -> Self {
        Self::Analyze(error)
    }
}

impl From<ConnectionError> for Error {
    fn from(error: ConnectionError) -> Self {
        Self::Connection(error)
    }
}

impl From<ConceptError> for Error {
    fn from(error: ConceptError) -> Self {
        Self::Concept(error)
    }
}

impl From<MigrationError> for Error {
    fn from(error: MigrationError) -> Self {
        Self::Migration(error)
    }
}

impl From<InternalError> for Error {
    fn from(error: InternalError) -> Self {
        Self::Internal(error)
    }
}

impl From<ServerError> for Error {
    fn from(error: ServerError) -> Self {
        Self::Server(error)
    }
}

impl From<Status> for Error {
    fn from(status: Status) -> Self {
        if let Ok(details) = status.check_error_details() {
            if let Some(bad_request) = details.bad_request() {
                return Self::Connection(ConnectionError::ServerConnectionFailedWithError {
                    error: format!("{:?}", bad_request),
                });
            }

            let message = concat_source_messages(&status);
            if let Some(connection_error) = Self::try_extracting_connection_error_message(&message) {
                return Self::Connection(connection_error);
            }

            if let Some(error_info) = details.error_info() {
                if let Some(connection_error) = Self::try_extracting_connection_error_from_info(error_info) {
                    Self::Connection(connection_error)
                } else {
                    let code = error_info.reason.clone();
                    let domain = error_info.domain.clone();
                    let stack_trace =
                        details.debug_info().map(|debug_info| debug_info.stack_entries.clone()).unwrap_or_default();
                    Self::Server(ServerError::new(code, domain, status.message().to_owned(), stack_trace))
                }
            } else {
                Self::Other(message)
            }
        } else {
            match status.code() {
                Code::Unavailable => Self::parse_unavailable(status.message()),
                Code::Unknown | Code::FailedPrecondition | Code::AlreadyExists => {
                    Self::Connection(ConnectionError::ServerConnectionFailedNetworking {
                        error: status.message().to_owned(),
                    })
                }
                Code::Unimplemented => {
                    Self::Connection(ConnectionError::RPCMethodUnavailable { message: status.message().to_owned() })
                }
                _ => {
                    let message = concat_source_messages(&status);
                    Self::try_extracting_connection_error_message(&message)
                        .map(|error| Self::Connection(error))
                        .unwrap_or_else(|| Self::Other(message))
                }
            }
        }
    }
}

fn is_rst_stream(message: &str) -> bool {
    // "Received Rst Stream" occurs if the server is in the process of shutting down.
    message.contains("Received Rst Stream")
}

fn is_reading_body_from_connection_error(message: &str) -> bool {
    // This error can be returned when the server crashes
    message.contains("error reading a body from connection")
}

fn is_tcp_connect_error(message: &str) -> bool {
    // No TCP connection
    message.contains("tcp connect error")
}

fn is_connection_error(message: &str) -> bool {
    // Transport-level connection errors
    message.contains("connection error")
}

fn concat_source_messages(status: &Status) -> String {
    let mut errors = String::new();
    errors.push_str(status.message());
    let mut err: Option<&dyn std::error::Error> = status.source();
    while let Some(e) = err {
        errors.push_str(": ");
        errors.push_str(e.to_string().as_str());
        err = e.source();
    }
    errors
}

impl From<http::uri::InvalidUri> for Error {
    fn from(err: http::uri::InvalidUri) -> Self {
        Self::Other(err.to_string())
    }
}

impl From<tonic::transport::Error> for Error {
    fn from(err: tonic::transport::Error) -> Self {
        Self::Other(err.to_string())
    }
}

impl<T> From<tokio::sync::mpsc::error::SendError<T>> for Error {
    fn from(_err: tokio::sync::mpsc::error::SendError<T>) -> Self {
        Self::Internal(InternalError::SendError)
    }
}

impl From<tokio::sync::oneshot::error::RecvError> for Error {
    fn from(_err: tokio::sync::oneshot::error::RecvError) -> Self {
        Self::Internal(InternalError::RecvError)
    }
}

impl From<crossbeam::channel::RecvError> for Error {
    fn from(_err: crossbeam::channel::RecvError) -> Self {
        Self::Internal(InternalError::RecvError)
    }
}

impl<T> From<crossbeam::channel::SendError<T>> for Error {
    fn from(_err: crossbeam::channel::SendError<T>) -> Self {
        Self::Internal(InternalError::SendError)
    }
}

impl From<String> for Error {
    fn from(err: String) -> Self {
        Self::Other(err)
    }
}

impl From<std::io::Error> for Error {
    fn from(err: std::io::Error) -> Self {
        Self::Other(err.to_string())
    }
}
