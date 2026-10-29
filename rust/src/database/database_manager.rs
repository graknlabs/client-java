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

use std::{path::Path, sync::Arc};

use itertools::Itertools;
use typedb_protocol::migration::Item;

use super::Database;
use crate::{
    common::Result,
    connection::{
        database::import_stream::DatabaseImportStream,
        server::{server_manager::ServerManager, server_routing::ServerRouting},
    },
    database::migration::{ProtoMessageIterator, try_open_import_file},
    info::DatabaseInfo,
    resolve,
};

/// Provides access to all database management methods.
#[derive(Debug)]
pub struct DatabaseManager {
    server_manager: Arc<ServerManager>,
}

/// Provides access to all database management methods.
impl DatabaseManager {
    pub(crate) fn new(server_manager: Arc<ServerManager>) -> Result<Self> {
        Ok(Self { server_manager })
    }

    /// Retrieves all databases present on the TypeDB server.
    ///
    /// # Examples
    ///
    /// ```rust
    #[cfg_attr(feature = "sync", doc = "driver.databases().all();")]
    #[cfg_attr(not(feature = "sync"), doc = "driver.databases().all().await;")]
    /// ```
    #[cfg_attr(feature = "sync", maybe_async::must_be_sync)]
    pub async fn all(&self) -> Result<Vec<Arc<Database>>> {
        self.server_manager
            .execute(ServerRouting::Auto, move |server_connection| async move {
                server_connection
                    .all_databases()
                    .await?
                    .into_iter()
                    .map(|database_info| self.try_build_database(database_info))
                    .try_collect()
            })
            .await
    }

    /// Retrieves the database with the given name.
    ///
    /// # Arguments
    ///
    /// * `name` — The name of the database to retrieve
    ///
    /// # Examples
    ///
    /// ```rust
    #[cfg_attr(feature = "sync", doc = "driver.databases().get(name);")]
    #[cfg_attr(not(feature = "sync"), doc = "driver.databases().get(name).await;")]
    /// ```
    #[cfg_attr(feature = "sync", maybe_async::must_be_sync)]
    pub async fn get(&self, name: impl Into<String>) -> Result<Arc<Database>> {
        let name = name.into();
        let database_info = self
            .server_manager
            .execute(ServerRouting::Auto, move |server_connection| {
                let name = name.clone();
                async move { server_connection.get_database(name).await }
            })
            .await?;
        self.try_build_database(database_info)
    }

    /// Checks if a database with the given name exists.
    ///
    /// # Arguments
    ///
    /// * `name` — The database name to be checked
    ///
    /// # Examples
    ///
    /// ```rust
    #[cfg_attr(feature = "sync", doc = "driver.databases().contains(name);")]
    #[cfg_attr(not(feature = "sync"), doc = "driver.databases().contains(name).await;")]
    /// ```
    #[cfg_attr(feature = "sync", maybe_async::must_be_sync)]
    pub async fn contains(&self, name: impl Into<String>) -> Result<bool> {
        let name = name.into();
        self.server_manager
            .execute(ServerRouting::Auto, move |server_connection| {
                let name = name.clone();
                async move { server_connection.contains_database(name).await }
            })
            .await
    }

    /// Creates a database with the given name.
    ///
    /// # Arguments
    ///
    /// * `name` — The name of the database to be created
    ///
    /// # Examples
    ///
    /// ```rust
    #[cfg_attr(feature = "sync", doc = "driver.databases().create(name);")]
    #[cfg_attr(not(feature = "sync"), doc = "driver.databases().create(name).await;")]
    /// ```
    #[cfg_attr(feature = "sync", maybe_async::must_be_sync)]
    pub async fn create(&self, name: impl Into<String>) -> Result {
        let name = name.into();
        self.server_manager
            .execute(ServerRouting::Auto, move |server_connection| {
                let name = name.clone();
                async move { server_connection.create_database(name).await }
            })
            .await
    }

    // Opens a raw import stream, exposed for behaviour tests only
    #[doc(hidden)]
    #[cfg_attr(feature = "sync", maybe_async::must_be_sync)]
    pub async fn import_stream(
        &self,
        name: impl Into<String>,
        schema: impl Into<String>,
    ) -> Result<DatabaseImportStream> {
        let name = name.into();
        let schema: String = schema.into();
        let schema_ref: &str = schema.as_ref();
        self.server_manager
            .execute(ServerRouting::Auto, move |server_connection| {
                let name = name.clone();
                async move { server_connection.import_database(name, schema_ref.to_string()).await }
            })
            .await
    }

    /// Creates a database with the given name based on previously exported another database's data
    /// loaded from a file.
    /// This is a blocking operation and may take a significant amount of time depending on the
    /// database size.
    ///
    /// # Arguments
    ///
    /// * `name` — The name of the database to be created
    /// * `schema` — The schema definition query string for the database
    /// * `data_file_path` — The exported database file to import the data from, plain or zstd-compressed
    ///
    /// # Examples
    ///
    /// ```rust
    #[cfg_attr(feature = "sync", doc = "driver.databases().import_from_file(name, schema, data_path);")]
    #[cfg_attr(not(feature = "sync"), doc = "driver.databases().import_from_file(name, schema, data_path).await;")]
    /// ```
    #[cfg_attr(feature = "sync", maybe_async::must_be_sync)]
    pub async fn import_from_file(
        &self,
        name: impl Into<String>,
        schema: impl Into<String>,
        data_file_path: impl AsRef<Path>,
    ) -> Result {
        const ITEM_BATCH_SIZE: usize = 250;

        let name = name.into();
        let schema: String = schema.into();
        let schema_ref: &str = schema.as_ref();
        let data_file_path = data_file_path.as_ref();

        self.server_manager
            .execute(ServerRouting::Auto, move |server_connection| {
                let name = name.clone();
                async move {
                    let reader = try_open_import_file(data_file_path)?;
                    let mut import_stream = server_connection.import_database(name, schema_ref.to_string()).await?;

                    let mut item_buffer = Vec::with_capacity(ITEM_BATCH_SIZE);
                    let mut read_item_iterator = ProtoMessageIterator::<Item, _>::new(reader);

                    while let Some(item) = read_item_iterator.next() {
                        let item = item?;
                        item_buffer.push(item);
                        if item_buffer.len() >= ITEM_BATCH_SIZE {
                            import_stream.send_items(item_buffer.split_off(0))?;
                        }
                    }

                    if !item_buffer.is_empty() {
                        import_stream.send_items(item_buffer)?;
                    }

                    resolve!(import_stream.done())
                }
            })
            .await
    }

    fn try_build_database(&self, database_info: DatabaseInfo) -> Result<Arc<Database>> {
        Database::new(database_info, self.server_manager.clone()).map(Arc::new)
    }
}
