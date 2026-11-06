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
use std::{collections::HashSet, time::Duration};

use cucumber::gherkin::Step;
use macro_rules_attribute::apply;
use typedb_driver::{Replica, ReplicationRole, Server, ServerVersion};

use crate::{Context, generic_step, params, params::check_boolean, util::iter_table};

mod database;
mod transaction;
mod user;

async fn get_server_version(context: &Context, may_error: params::MayError) -> Option<ServerVersion> {
    let res = match &context.operation_server_routing {
        Some(server_routing) => {
            context.driver.as_ref().unwrap().server_version_with_routing(server_routing.clone()).await
        }
        None => context.driver.as_ref().unwrap().server_version().await,
    };
    may_error.check(res)
}

async fn get_servers(context: &Context, may_error: params::MayError) -> HashSet<Server> {
    let res = match &context.operation_server_routing {
        Some(server_routing) => context.driver.as_ref().unwrap().servers_with_routing(server_routing.clone()).await,
        None => context.driver.as_ref().unwrap().servers().await,
    };
    may_error.check(res).unwrap()
}

#[apply(generic_step)]
#[step("typedb starts")]
async fn typedb_starts(_: &mut Context) {}

#[apply(generic_step)]
#[step(expr = "connection opens with default authentication{may_error}")]
async fn connection_opens_with_default_authentication(context: &mut Context, may_error: params::MayError) {
    if let Some(driver) = may_error.check(context.create_default_driver().await) {
        context.set_driver(driver)
    }
}

#[apply(generic_step)]
#[step(expr = "connection opens to single server with default authentication{may_error}")]
async fn connection_opens_to_single_server_with_default_authentication(
    context: &mut Context,
    may_error: params::MayError,
) {
    if let Some(driver) = may_error.check(context.create_default_single_driver().await) {
        context.set_driver(driver)
    }
}

#[apply(generic_step)]
#[step(expr = "connection opens with username '{word}', password '{word}'{may_error}")]
async fn connection_opens_with_authentication(
    context: &mut Context,
    username: String,
    password: String,
    may_error: params::MayError,
) {
    if let Some(driver) = may_error.check(context.create_driver(Some(&username), Some(&password)).await) {
        context.set_driver(driver)
    }
}

fn change_host(address: &str, new_host: &str) -> String {
    let parts: Vec<&str> = address.split(':').collect();
    assert_eq!(parts.len(), 2);
    format!("{}:{}", new_host, parts[1])
}

fn change_port(address: &str, new_port: &str) -> String {
    let parts: Vec<&str> = address.split(':').collect();
    assert_eq!(parts.len(), 2);
    format!("{}:{}", parts[0], new_port)
}

#[apply(generic_step)]
#[step(expr = "connection opens with a wrong host{may_error}")]
async fn connection_opens_with_a_wrong_host(context: &mut Context, may_error: params::MayError) {
    may_error.check(match context.is_cluster {
        false => {
            context
                .create_driver_core(
                    &change_host(Context::DEFAULT_ADDRESS, "surely-not-localhost"),
                    Context::ADMIN_USERNAME,
                    Context::ADMIN_PASSWORD,
                )
                .await
        }
        true => {
            let updated_address = change_host(Context::DEFAULT_CLUSTER_ADDRESSES[0], "surely-not-localhost");
            context.create_driver_cluster(&[&updated_address], Context::ADMIN_USERNAME, Context::ADMIN_PASSWORD).await
        }
    });
}

/// The HTTP endpoint's default port, which a connection string written for the HTTP driver names.
const OTHER_PROTOCOL_DEFAULT_PORT: &str = "8000";

async fn connection_opens_with_connection_string(
    context: &mut Context,
    connection_string: &str,
    may_error: params::MayError,
) {
    if let Some(driver) = may_error.check(context.create_driver_from_connection_string(connection_string).await) {
        context.set_driver(driver)
    }
}

#[apply(generic_step)]
#[step(expr = "connection opens with a connection string for the default address{may_error}")]
async fn connection_opens_with_a_connection_string_for_the_default_address(
    context: &mut Context,
    may_error: params::MayError,
) {
    let connection_string = context.default_connection_string(None).unwrap();
    connection_opens_with_connection_string(context, &connection_string, may_error).await
}

#[apply(generic_step)]
#[step(expr = "connection opens with connection string: {string}{may_error}")]
async fn connection_opens_with_connection_string_literal(
    context: &mut Context,
    connection_string: String,
    may_error: params::MayError,
) {
    connection_opens_with_connection_string(context, &connection_string, may_error).await
}

#[apply(generic_step)]
#[step(expr = "connection opens with a connection string for a wrong port{may_error}")]
async fn connection_opens_with_a_connection_string_for_a_wrong_port(
    context: &mut Context,
    may_error: params::MayError,
) {
    let connection_string = context.default_connection_string(Some("0")).unwrap();
    connection_opens_with_connection_string(context, &connection_string, may_error).await
}

#[apply(generic_step)]
#[step(expr = "connection opens with a connection string for the other protocol default port{may_error}")]
async fn connection_opens_with_a_connection_string_for_the_other_protocol_default_port(
    context: &mut Context,
    may_error: params::MayError,
) {
    let connection_string = context.default_connection_string(Some(OTHER_PROTOCOL_DEFAULT_PORT)).unwrap();
    connection_opens_with_connection_string(context, &connection_string, may_error).await
}

#[apply(generic_step)]
#[step(expr = "connection opens with a wrong port{may_error}")]
async fn connection_opens_with_a_wrong_port(context: &mut Context, may_error: params::MayError) {
    may_error.check(match context.is_cluster {
        false => {
            context
                .create_driver_core(
                    &change_port(Context::DEFAULT_ADDRESS, "0"),
                    Context::ADMIN_USERNAME,
                    Context::ADMIN_PASSWORD,
                )
                .await
        }
        true => {
            let updated_address = change_port(Context::DEFAULT_CLUSTER_ADDRESSES[0], "0");
            context.create_driver_cluster(&[&updated_address], Context::ADMIN_USERNAME, Context::ADMIN_PASSWORD).await
        }
    });
}

#[apply(generic_step)]
#[step(expr = "connection is open: {boolean}")]
async fn connection_has_been_opened(context: &mut Context, is_open: params::Boolean) {
    check_boolean!(is_open, context.driver.is_some() && context.driver.as_ref().unwrap().is_open());
}

#[apply(generic_step)]
#[step(expr = r"connection contains distribution{may_error}")]
async fn connection_has_distribution(context: &mut Context, may_error: params::MayError) {
    if let Some(server_version) = get_server_version(context, may_error).await {
        assert!(!server_version.distribution().is_empty());
    }
}

#[apply(generic_step)]
#[step(expr = r"connection contains version{may_error}")]
async fn connection_has_version(context: &mut Context, may_error: params::MayError) {
    if let Some(server_version) = get_server_version(context, may_error).await {
        assert!(!server_version.version().is_empty());
    }
}

#[apply(generic_step)]
#[step(expr = r"connection has {int} server(s)")]
async fn connection_has_count_servers(context: &mut Context, count: usize) {
    let servers = get_servers(context, params::MayError::False).await;
    assert_eq!(servers.len(), count);
}

#[apply(generic_step)]
#[step(expr = r"connection primary server exists")]
async fn connection_primary_server_exists(context: &mut Context) {
    let res = match &context.operation_server_routing {
        Some(server_routing) => {
            context.driver.as_ref().unwrap().primary_server_with_routing(server_routing.clone()).await
        }
        None => context.driver.as_ref().unwrap().primary_server().await,
    };
    assert!(res.unwrap().is_some());
}

#[apply(generic_step)]
#[step(expr = r"connection get server\({word}\) {exists_or_doesnt}")]
async fn connection_get_server_exists(
    context: &mut Context,
    address: String,
    exists_or_doesnt: params::ExistsOrDoesnt,
) {
    let servers = get_servers(context, params::MayError::False).await;
    let exists = servers.iter().any(|r| r.address().unwrap().to_string() == address);
    exists_or_doesnt.check_bool(exists, &format!("server {}", address));
}

#[apply(generic_step)]
#[step(expr = r"connection get server\({word}\) has term")]
async fn connection_get_server_has_term(context: &mut Context, address: String) {
    let servers = get_servers(context, params::MayError::False).await;
    let server = servers.iter().find(|r| r.address().unwrap().to_string() == address);
    params::ExistsOrDoesnt::Exists.check(&server, &format!("server {}", address));
    let term = server.unwrap().term();
    params::ExistsOrDoesnt::Exists.check(&term, &format!("term {:?}", term));
    assert!(term.unwrap() > 0, "Term expected");
}

#[apply(generic_step)]
#[step("connection servers have roles:")]
async fn connection_servers_have_roles(context: &mut Context, step: &Step) {
    let servers = get_servers(context, params::MayError::False).await;

    let mut expected_primary_count = 0;
    let mut expected_secondary_count = 0;
    let mut expected_candidate_count = 0;
    for expected_role in iter_table(step) {
        match expected_role {
            "primary" => expected_primary_count += 1,
            "secondary" => expected_secondary_count += 1,
            "candidate" => expected_candidate_count += 1,
            other => panic!("Unknown server replication role: {}", other),
        }
    }

    let actual_primary_count = servers.iter().filter(|r| matches!(r.role(), Some(ReplicationRole::Primary))).count();
    let actual_secondary_count =
        servers.iter().filter(|r| matches!(r.role(), Some(ReplicationRole::Secondary))).count();
    let actual_candidate_count =
        servers.iter().filter(|r| matches!(r.role(), Some(ReplicationRole::Candidate))).count();

    assert_eq!(
        expected_primary_count, actual_primary_count,
        "Expected {} primary servers, found {}",
        expected_primary_count, actual_primary_count
    );
    assert_eq!(
        expected_secondary_count, actual_secondary_count,
        "Expected {} secondary servers, found {}",
        expected_secondary_count, actual_secondary_count
    );
    assert_eq!(
        expected_candidate_count, actual_candidate_count,
        "Expected {} candidate servers, found {}",
        expected_candidate_count, actual_candidate_count
    );
}

#[apply(generic_step)]
#[step(expr = r"connection has {int} database(s)")]
async fn connection_has_count_databases(context: &mut Context, count: usize) {
    assert_eq!(context.driver.as_ref().unwrap().databases().all().await.unwrap().len(), count);
}

#[apply(generic_step)]
#[step(expr = r"connection has {int} user(s)")]
async fn connection_has_count_users(context: &mut Context, count: usize) {
    assert_eq!(context.driver.as_ref().unwrap().users().all().await.unwrap().len(), count);
}

#[apply(generic_step)]
#[step(expr = r"connection closes{may_error}")]
async fn driver_closes(context: &mut Context, may_error: params::MayError) {
    may_error.check(context.driver.as_ref().unwrap().force_close());
    context.cleanup_transactions().await;
}

#[apply(generic_step)]
#[step(expr = "set operation server routing to: {server_routing}")]
pub async fn set_operation_server_routing(context: &mut Context, server_routing: params::ServerRouting) {
    context.operation_server_routing = Some(server_routing.into_typedb());
}

#[apply(generic_step)]
#[step(expr = "set driver option primary_failover_retries to: {int}")]
pub async fn set_transaction_option_primary_failover_retries(context: &mut Context, value: usize) {
    context.init_driver_options_if_needed();
    context.driver_options_mut().unwrap().primary_failover_retries = value;
}

#[apply(generic_step)]
#[step(expr = "set driver option request_timeout_millis to: {int}")]
pub async fn set_driver_option_request_timeout_millis(context: &mut Context, value: u64) {
    context.init_driver_options_if_needed();
    context.driver_options_mut().unwrap().request_timeout = Duration::from_millis(value);
}
