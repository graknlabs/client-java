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

mod analyze;
mod answer;
mod common;
mod concept;
mod credentials;
mod database;
mod driver;
mod driver_options;
mod driver_tls_config;
mod given_rows;
mod query_options;
mod server;
mod transaction;
mod transaction_options;
mod user;

use tracing_subscriber::{EnvFilter, filter::LevelFilter, fmt, layer::SubscriberExt, util::SubscriberInitExt};

/// Enables logging in the TypeDB driver.
///
/// This function sets up tracing with two environment variables:
///
/// - `TYPEDB_DRIVER_LOG`: Fine-grained control using the same syntax as `RUST_LOG`.
///   Example: `TYPEDB_DRIVER_LOG=typedb_driver=debug,typedb_driver_clib=trace`
///
/// - `TYPEDB_DRIVER_LOG_LEVEL`: Simple log level that applies to both `typedb_driver`
///   and `typedb_driver_clib` crates. Overrides any settings from `TYPEDB_DRIVER_LOG`.
///   Example: `TYPEDB_DRIVER_LOG_LEVEL=debug`
///
/// If neither is set, the default level is INFO for driver crates.
///
/// The logging is initialized only once to prevent multiple initializations
/// in applications that create multiple drivers.
#[unsafe(no_mangle)]
pub extern "C" fn init_logging() {
    const ENV_TYPEDB_DRIVER_LOG: &str = "TYPEDB_DRIVER_LOG";
    const ENV_TYPEDB_DRIVER_LOG_LEVEL: &str = "TYPEDB_DRIVER_LOG_LEVEL";
    const TYPEDB_DRIVER_TARGET: &str = "typedb_driver";
    const TYPEDB_DRIVER_CLIB_TARGET: &str = "typedb_driver_clib";

    use std::sync::Once;
    static INIT: Once = Once::new();
    INIT.call_once(|| {
        // 1. Start with default INFO level, then apply TYPEDB_DRIVER_LOG for fine-grained control
        //    parse_lossy ensures typos don't break initialization
        let mut filter = EnvFilter::builder()
            .with_default_directive(LevelFilter::INFO.into())
            .parse_lossy(std::env::var(ENV_TYPEDB_DRIVER_LOG).unwrap_or_default());

        // 2. If TYPEDB_DRIVER_LOG_LEVEL is set, override for both driver crates
        if let Ok(level) = std::env::var(ENV_TYPEDB_DRIVER_LOG_LEVEL) {
            if let Ok(directive) = format!("{}={}", TYPEDB_DRIVER_TARGET, level).parse() {
                filter = filter.add_directive(directive);
            }
            if let Ok(directive) = format!("{}={}", TYPEDB_DRIVER_CLIB_TARGET, level).parse() {
                filter = filter.add_directive(directive);
            }
        }

        if let Err(e) = tracing_subscriber::registry().with(filter).with(fmt::layer().with_target(false)).try_init() {
            eprintln!("Failed to initialize logging: {}", e);
        }
    });
}
