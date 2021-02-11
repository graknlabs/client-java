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

package grakn.client.rpc.cluster;

import grakn.client.GraknClient;
import grakn.client.common.exception.GraknClientException;
import grakn.client.rpc.DatabaseManagerRPC;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static grakn.client.common.exception.ErrorMessage.Client.CLUSTER_ALL_NODES_FAILED;

public class DatabaseManagerClusterRPC implements GraknClient.DatabaseManager {
    private final Map<ServerAddress, DatabaseManagerRPC> databaseManagers;

    public DatabaseManagerClusterRPC(Map<ServerAddress, DatabaseManagerRPC> databaseManagers) {
        this.databaseManagers = databaseManagers;
    }

    @Override
    public boolean contains(String name) {
        List<GraknClientException> errors = new ArrayList<>();
        for (DatabaseManagerRPC databaseManager : databaseManagers.values()) {
            try {
                return databaseManager.contains(name);
            } catch (GraknClientException e) {
                errors.add(e);
            }
        }
        throw new GraknClientException(CLUSTER_ALL_NODES_FAILED.message(errors.toString()));
    }

    @Override
    public void create(String name) {
        for (DatabaseManagerRPC databaseManager : databaseManagers.values()) {
            if (!databaseManager.contains(name)) {
                databaseManager.create(name);
            }
        }
    }

    @Override
    public void delete(String name) {
        for (DatabaseManagerRPC databaseManager : databaseManagers.values()) {
            if (databaseManager.contains(name)) {
                databaseManager.delete(name);
            }
        }
    }

    @Override
    public List<String> all() {
        List<GraknClientException> errors = new ArrayList<>();
        for (DatabaseManagerRPC databaseManager : databaseManagers.values()) {
            try {
                return databaseManager.all();
            } catch (GraknClientException e) {
                errors.add(e);
            }
        }
        throw new GraknClientException(CLUSTER_ALL_NODES_FAILED.message(errors.toString()));
    }
}
