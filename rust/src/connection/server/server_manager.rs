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
#[cfg(not(feature = "sync"))]
use std::future::Future;
use std::{
    collections::{HashMap, HashSet},
    fmt,
    sync::{Arc, RwLock, RwLockReadGuard, RwLockWriteGuard},
    thread::sleep,
    time::Duration,
};

use itertools::Itertools;
use tracing::debug;

use crate::{
    Credentials, DriverOptions, Error, Result,
    common::address::{Address, Addresses, address_translation::AddressTranslation},
    connection::{
        runtime::BackgroundRuntime,
        server::{
            AvailableServer, Replica, Server, server_connection::ServerConnection, server_routing::ServerRouting,
        },
    },
    error::ConnectionError,
};

macro_rules! filter_available_replicas {
    ($iter:expr) => {{
        $iter.into_iter().filter_map(|replica| match replica {
            Server::Available(available) => Some(available),
            Server::Unavailable { .. } => None,
        })
    }};
}

macro_rules! find_primary_replica {
    ($iter:expr) => {{ $iter.filter(|replica| replica.is_primary()).max_by_key(|replica| replica.term().unwrap_or_default()) }};
}

pub(crate) struct ServerManager {
    configured_addresses: Addresses,
    replicas: RwLock<HashSet<AvailableServer>>,
    replica_connections: RwLock<HashMap<Address, ServerConnection>>,
    address_translation: RwLock<AddressTranslation>,

    background_runtime: Arc<BackgroundRuntime>,
    credentials: Credentials,
    driver_options: DriverOptions,
    driver_lang: String,
    driver_version: String,
}

impl ServerManager {
    const PRIMARY_REPLICA_SELECTION_TIMEOUT: Duration = Duration::from_secs(2);

    // TODO: Introduce a timer-based connections update to have a more actualized connections list

    #[cfg_attr(feature = "sync", maybe_async::must_be_sync)]
    pub(crate) async fn new(
        background_runtime: Arc<BackgroundRuntime>,
        addresses: Addresses,
        credentials: Credentials,
        driver_options: DriverOptions,
        driver_lang: impl AsRef<str>,
        driver_version: impl AsRef<str>,
    ) -> Result<Self> {
        let (source_connections, replicas) = Self::fetch_servers_from_addresses(
            background_runtime.clone(),
            &addresses,
            credentials.clone(),
            driver_options.clone(),
            driver_lang.as_ref(),
            driver_version.as_ref(),
        )
        .await?;
        let address_translation = addresses.address_translation();
        let server_manager = Self {
            configured_addresses: addresses,
            replicas: RwLock::new(filter_available_replicas!(replicas).collect()),
            replica_connections: RwLock::new(source_connections),
            address_translation: RwLock::new(address_translation),
            background_runtime,
            credentials,
            driver_options,
            driver_lang: driver_lang.as_ref().to_string(),
            driver_version: driver_version.as_ref().to_string(),
        };
        server_manager.refresh_replica_connections().await?;
        Ok(server_manager)
    }

    pub(crate) fn driver_options(&self) -> &DriverOptions {
        &self.driver_options
    }

    pub(crate) fn configured_addresses(&self) -> &Addresses {
        &self.configured_addresses
    }

    #[cfg_attr(feature = "sync", maybe_async::must_be_sync)]
    async fn refresh_replica_connections(&self) -> Result {
        let replicas = self.read_replicas().clone();
        let mut connection_errors = HashMap::with_capacity(replicas.len());
        let mut new_replica_connections = HashMap::new();
        let connection_addresses: HashSet<Address> = self.read_replica_connections().keys().cloned().collect();
        for replica in &replicas {
            let private_address = replica.private_address().clone();
            if !connection_addresses.contains(&private_address) {
                match self.new_replica_connection(replica.address().clone()).await {
                    Ok(replica_connection) => {
                        new_replica_connections.insert(private_address, replica_connection);
                    }
                    Err(err) => {
                        connection_errors.insert(replica.address().clone(), err);
                    }
                }
            }
        }

        let replica_addresses: HashSet<Address> =
            replicas.into_iter().map(|replica| replica.private_address().clone()).collect();
        let mut replica_connections = self.write_replica_connections();
        replica_connections.retain(|address, _| replica_addresses.contains(address));
        replica_connections.extend(new_replica_connections);

        if replica_connections.is_empty() { Err(self.server_connection_failed_err(connection_errors)) } else { Ok(()) }
    }

    #[cfg_attr(feature = "sync", maybe_async::must_be_sync)]
    async fn new_replica_connection(&self, address: Address) -> Result<ServerConnection> {
        ServerConnection::new(
            self.background_runtime.clone(),
            address,
            self.credentials.clone(),
            self.driver_options.clone(),
            self.driver_lang.as_ref(),
            self.driver_version.as_ref(),
        )
        .await
        .map(|(replica_connection, _)| replica_connection)
    }

    #[cfg_attr(feature = "sync", maybe_async::must_be_sync)]
    async fn record_new_replica_connection(
        &self,
        public_address: Address,
        private_address: Address,
    ) -> Result<ServerConnection> {
        let replica_connection = self.new_replica_connection(public_address).await?;
        let mut replica_connections = self.write_replica_connections();
        replica_connections.insert(private_address, replica_connection.clone());
        Ok(replica_connection)
    }

    fn read_replica_connections(&self) -> RwLockReadGuard<'_, HashMap<Address, ServerConnection>> {
        self.replica_connections.read().expect("Expected server connections read access")
    }

    fn write_replica_connections(&self) -> RwLockWriteGuard<'_, HashMap<Address, ServerConnection>> {
        self.replica_connections.write().expect("Expected server connections write access")
    }

    fn read_address_translation(&self) -> RwLockReadGuard<'_, AddressTranslation> {
        self.address_translation.read().expect("Expected address translation read access")
    }

    pub(crate) fn force_close(&self) -> Result {
        self.read_replica_connections().values().map(ServerConnection::force_close).try_collect().map_err(Into::into)
    }

    pub(crate) fn username(&self) -> Result<String> {
        match self.read_replica_connections().iter().next() {
            Some((_, replica_connection)) => Ok(replica_connection.username().to_string()),
            None => Err(ConnectionError::ServerConnectionIsClosed {}.into()),
        }
    }

    #[cfg_attr(feature = "sync", maybe_async::must_be_sync)]
    pub(crate) async fn execute<F, P, R>(&self, server_routing: ServerRouting, task: F) -> Result<R>
    where
        F: Fn(ServerConnection) -> P,
        P: Future<Output = Result<R>>,
    {
        match server_routing {
            ServerRouting::Auto => self.execute_strongly_consistent(task).await,
            ServerRouting::Direct { address } => {
                if self.read_replicas().iter().find(|replica| replica.address() == &address).is_none() {
                    let known_addresses =
                        Addresses::from_addresses(self.read_replicas().iter().map(|replica| replica.address().clone()));
                    return Err(ConnectionError::UnknownDirectServerRouting { address, known_addresses }.into());
                }
                let private_address =
                    self.read_address_translation().to_private(&address).unwrap_or_else(|| address.clone());
                self.execute_on(&address, &private_address, &task).await
            }
        }
    }

    #[cfg_attr(feature = "sync", maybe_async::must_be_sync)]
    async fn execute_strongly_consistent<F, P, R>(&self, task: F) -> Result<R>
    where
        F: Fn(ServerConnection) -> P,
        P: Future<Output = Result<R>>,
    {
        let retries = self.driver_options.primary_failover_retries;
        let mut primary = self.get_or_seek_primary_replica(retries).await?;

        let mut connection_errors = HashMap::new();
        for _ in 0..=retries {
            let private_address = primary.private_address().clone();
            match self.execute_on(primary.address(), &private_address, &task).await {
                Ok(result) => return Ok(result),
                Err(Error::Connection(connection_error)) => {
                    debug!("Failed on primary {}: {connection_error:?}", primary.address());
                    connection_errors.insert(primary.address().clone(), connection_error.clone().into());
                    if let ConnectionError::ClusterServerNotPrimary { hint_address: Some(hint) } = &connection_error {
                        if let Some(hinted) = self.find_replica_by_private_address(hint) {
                            primary = hinted;
                            continue;
                        }
                    }
                    let candidates = self.failover_candidates(&private_address, &connection_error);
                    match self.seek_primary_replica_in(candidates, retries, Some(&private_address)).await {
                        Ok(replica) => primary = replica,
                        Err(_) => break,
                    }
                }
                Err(err) => return Err(err),
            }
        }
        Err(self.server_connection_failed_err(connection_errors))
    }

    fn find_replica_by_private_address(&self, private_address: &Address) -> Option<AvailableServer> {
        self.read_replicas().iter().find(|replica| replica.private_address() == private_address).cloned()
    }

    #[cfg_attr(feature = "sync", maybe_async::must_be_sync)]
    async fn get_or_seek_primary_replica(&self, retries: usize) -> Result<AvailableServer> {
        if let Some(replica) = self.read_primary_replica() {
            return Ok(replica);
        }
        let replicas: HashSet<_> = self.read_replicas().iter().cloned().collect();
        self.seek_primary_replica_in(replicas, retries, None).await
    }

    fn failover_candidates(&self, failed_address: &Address, error: &ConnectionError) -> HashSet<AvailableServer> {
        let replicas: HashSet<_> = self.read_replicas().iter().cloned().collect();
        if matches!(
            error,
            ConnectionError::ClusterServerNotPrimary { .. } | ConnectionError::ServerUnavailable { .. }
        ) {
            replicas // Server is alive — include it as a candidate to query
        } else {
            replicas.into_iter().filter(|r| r.private_address() != failed_address).collect()
            // Server is dead — exclude it
        }
    }

    #[cfg_attr(feature = "sync", maybe_async::must_be_sync)]
    async fn execute_on_any<F, P, R>(&self, replicas: impl IntoIterator<Item = AvailableServer>, task: F) -> Result<R>
    where
        F: Fn(ServerConnection) -> P,
        P: Future<Output = Result<R>>,
    {
        let mut connection_errors = HashMap::new();
        for replica in replicas.into_iter() {
            // TODO: We won't ever reconnect to disconnected / new replicas when using execute_on_any.
            // We need to think how to update the connections in this case.
            match self.execute_on(replica.address(), replica.private_address(), &task).await {
                Err(Error::Connection(error)) => {
                    debug!("Unable to connect to {}: {error:?}. May attempt the next server.", replica.address());
                    connection_errors.insert(replica.address().clone(), error.into());
                }
                res => return res,
            }
        }
        Err(self.server_connection_failed_err(connection_errors))
    }

    #[cfg_attr(feature = "sync", maybe_async::must_be_sync)]
    async fn execute_on<F, P, R>(&self, public_address: &Address, private_address: &Address, task: &F) -> Result<R>
    where
        F: Fn(ServerConnection) -> P,
        P: Future<Output = Result<R>>,
    {
        let existing_connection = { self.read_replica_connections().get(private_address).cloned() };
        let replica_connection = match existing_connection {
            Some(replica_connection) => replica_connection,
            None => self.record_new_replica_connection(public_address.clone(), private_address.clone()).await?,
        };
        task(replica_connection).await
    }

    #[cfg_attr(feature = "sync", maybe_async::must_be_sync)]
    async fn seek_primary_replica_in(
        &self,
        source_replicas: HashSet<AvailableServer>,
        retries: usize,
        expect_different_from: Option<&Address>,
    ) -> Result<AvailableServer> {
        let mut last_error = None;
        for attempt in 0..=retries {
            match self.try_seek_primary_replica_in(source_replicas.iter().cloned()).await {
                Ok(replica) => {
                    if let Some(stale) = expect_different_from {
                        if replica.private_address() == stale && attempt < retries {
                            debug!(
                                "Seek returned stale primary (attempt {}/{}), retrying...",
                                attempt + 1,
                                retries + 1
                            );
                            Self::wait_for_primary_replica_selection().await;
                            continue;
                        }
                    }
                    return Ok(replica);
                }
                Err(err) => {
                    last_error = Some(err);
                    if attempt < retries {
                        debug!("Primary not found (attempt {}/{}), retrying...", attempt + 1, retries + 1);
                        Self::wait_for_primary_replica_selection().await;
                    }
                }
            }
        }
        Err(last_error.unwrap())
    }

    #[cfg_attr(feature = "sync", maybe_async::must_be_sync)]
    async fn try_seek_primary_replica_in(
        &self,
        source_replicas: impl IntoIterator<Item = AvailableServer>,
    ) -> Result<AvailableServer> {
        self.execute_on_any(source_replicas, |replica_connection| async {
            self.seek_primary_replica(replica_connection).await
        })
        .await
    }

    #[cfg_attr(feature = "sync", maybe_async::must_be_sync)]
    async fn seek_primary_replica(&self, replica_connection: ServerConnection) -> Result<AvailableServer> {
        let address_translation = self.read_address_translation().clone();
        let replicas = Self::fetch_servers_from_connection(&replica_connection, &address_translation).await?;
        *self.replicas.write().expect("Expected replicas write lock") = filter_available_replicas!(replicas).collect();
        if let Some(replica) = self.read_primary_replica() {
            self.refresh_replica_connections().await?;
            Ok(replica)
        } else {
            Err(ConnectionError::NoPrimaryServer {}.into())
        }
    }

    #[cfg_attr(feature = "sync", maybe_async::must_be_sync)]
    async fn wait_for_primary_replica_selection() {
        // FIXME: blocking sleep! Can't do agnostic async sleep.
        sleep(Self::PRIMARY_REPLICA_SELECTION_TIMEOUT);
    }

    #[cfg_attr(feature = "sync", maybe_async::must_be_sync)]
    async fn fetch_servers_from_addresses(
        background_runtime: Arc<BackgroundRuntime>,
        addresses: &Addresses,
        credentials: Credentials,
        driver_options: DriverOptions,
        driver_lang: impl AsRef<str>,
        driver_version: impl AsRef<str>,
    ) -> Result<(HashMap<Address, ServerConnection>, HashSet<Server>)> {
        let address_translation = addresses.address_translation();
        let mut errors = Vec::with_capacity(addresses.len());
        for address in addresses.addresses() {
            let replica_connection = ServerConnection::new(
                background_runtime.clone(),
                address.clone(),
                credentials.clone(),
                driver_options.clone(),
                driver_lang.as_ref(),
                driver_version.as_ref(),
            )
            .await;
            match replica_connection {
                Ok((replica_connection, replicas)) => {
                    debug!("Fetched replicas from configured address '{address}': {replicas:?}");
                    let translated_replicas =
                        Self::translate_replicas(replicas, &address_translation, &replica_connection);
                    let mut source_connections = HashMap::with_capacity(translated_replicas.len());
                    source_connections.insert(address.clone(), replica_connection);
                    return Ok((source_connections, translated_replicas));
                }
                Err(Error::Connection(err)) => {
                    debug!("Unable to fetch replicas from {}: {err:?}. Attempting next server.", address);
                    errors.push(err);
                }
                Err(err) => {
                    return Err(err);
                }
            }
        }
        Err(ConnectionError::ServerConnectionFailed {
            configured_addresses: addresses.clone(),
            accessed_addresses: addresses.clone(),
            details: errors.into_iter().join(";\n"),
        }
        .into())
    }

    #[cfg_attr(feature = "sync", maybe_async::must_be_sync)]
    pub(crate) async fn fetch_servers(&self, server_routing: ServerRouting) -> Result<HashSet<Server>> {
        let is_auto = server_routing == ServerRouting::Auto;
        let replicas = self
            .execute(server_routing, |replica_connection| {
                let address_translation = self.read_address_translation().clone();
                async move { Self::fetch_servers_from_connection(&replica_connection, &address_translation).await }
            })
            .await?;

        if is_auto {
            // Update cached replicas since it's the most recent info
            *self.replicas.write().expect("Expected replicas write lock") =
                filter_available_replicas!(replicas.clone()).collect();
        }
        Ok(replicas)
    }

    #[cfg_attr(feature = "sync", maybe_async::must_be_sync)]
    async fn fetch_servers_from_connection(
        replica_connection: &ServerConnection,
        address_translation: &AddressTranslation,
    ) -> Result<HashSet<Server>> {
        replica_connection
            .servers_all()
            .await
            .map(|replicas| Self::translate_replicas(replicas, address_translation, replica_connection))
    }

    fn translate_replicas(
        replicas: impl IntoIterator<Item = Server>,
        address_translation: &AddressTranslation,
        source: &ServerConnection,
    ) -> HashSet<Server> {
        replicas
            .into_iter()
            .map(|replica| match replica {
                // standalone server without advertised address: use connection address directly
                Server::Unavailable { replication_status: None } => {
                    Server::available_from_private(source.address().clone(), None)
                }
                other => other,
            })
            .map(|replica| replica.translated(address_translation))
            .collect()
    }

    #[cfg_attr(feature = "sync", maybe_async::must_be_sync)]
    pub(crate) async fn fetch_primary_server(&self, server_routing: ServerRouting) -> Result<Option<AvailableServer>> {
        let replicas = self.fetch_servers(server_routing).await?;
        Ok(find_primary_replica!(filter_available_replicas!(replicas.iter())).cloned())
    }

    #[cfg_attr(feature = "sync", maybe_async::must_be_sync)]
    async fn refresh_replicas(&self) -> Result {
        self.fetch_servers(ServerRouting::Auto).await.map(|_| ())
    }

    fn read_replicas(&self) -> RwLockReadGuard<'_, HashSet<AvailableServer>> {
        self.replicas.read().expect("Expected a read replica lock")
    }

    fn read_primary_replica(&self) -> Option<AvailableServer> {
        let replicas = self.read_replicas();
        find_primary_replica!(replicas.iter())
            .or_else(|| replicas.iter().find(|replica| replica.replication_status().is_none()))
            .cloned()
    }

    fn server_connection_failed_err(&self, errors: HashMap<Address, Error>) -> Error {
        let accessed_addresses =
            Addresses::from_addresses(self.read_replicas().iter().map(|replica| replica.address().clone()));
        let details = if errors.is_empty() {
            "none".to_string()
        } else {
            errors.into_iter().map(|(address, error)| format!("'{address}': '{error}'")).join(";\n")
        };
        ConnectionError::ServerConnectionFailed {
            configured_addresses: self.configured_addresses.clone(),
            accessed_addresses,
            details,
        }
        .into()
    }
}

impl fmt::Debug for ServerManager {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("ServerConnection").field("replicas", &self.replicas).finish()
    }
}
