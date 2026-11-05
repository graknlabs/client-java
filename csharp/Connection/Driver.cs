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

using System;
using System.Collections.Generic;
using System.Linq;

using TypeDB.Driver;
using TypeDB.Driver.Api;
using TypeDB.Driver.Common;
using TypeDB.Driver.Common.Validation;
using TypeDB.Driver.Connection;
using TypeDB.Driver.User;

namespace TypeDB.Driver.Connection
{
    public class TypeDBDriver : NativeObjectWrapper<Pinvoke.TypeDBDriver>, IDriver
    {
        private readonly IDatabaseManager _databaseManager;
        private readonly UserManager _userManager;

        public TypeDBDriver(string address, Credentials credentials, DriverOptions driverOptions)
            : base(Open(address, credentials, driverOptions))
        {
            _databaseManager = new DatabaseManager(NativeObject);
            _userManager = new UserManager(NativeObject);
        }

        public TypeDBDriver(ISet<string> addresses, Credentials credentials, DriverOptions driverOptions)
            : base(Open(addresses, credentials, driverOptions))
        {
            _databaseManager = new DatabaseManager(NativeObject);
            _userManager = new UserManager(NativeObject);
        }

        public TypeDBDriver(IDictionary<string, string> addressTranslation, Credentials credentials, DriverOptions driverOptions)
            : base(Open(addressTranslation, credentials, driverOptions))
        {
            _databaseManager = new DatabaseManager(NativeObject);
            _userManager = new UserManager(NativeObject);
        }

        private TypeDBDriver(Pinvoke.TypeDBDriver nativeDriver)
            : base(nativeDriver)
        {
            _databaseManager = new DatabaseManager(NativeObject);
            _userManager = new UserManager(NativeObject);
        }

        /// <summary>
        /// Opens a driver using the addresses and credentials held in a connection string. A static
        /// factory, because a connection string is not distinguishable from an address by its type.
        /// </summary>
        /// <param name="connectionString">The connection string holding the addresses and credentials to connect with.</param>
        /// <param name="driverOptions">The driver connection options to connect with, or null to derive them from the connection string.</param>
        public static TypeDBDriver FromConnectionString(string connectionString, DriverOptions? driverOptions)
        {
            Validator.RequireNonNull(connectionString, nameof(connectionString));
            try
            {
                return new TypeDBDriver(Pinvoke.typedb_driver.driver_new_from_connection_string(
                    connectionString,
                    driverOptions?.NativeObject,
                    IDriver.Language));
            }
            catch (Pinvoke.Error e)
            {
                throw new TypeDBDriverException(e);
            }
        }

        private static Pinvoke.TypeDBDriver Open(string address, Credentials credentials, DriverOptions driverOptions)
        {
            Validator.RequireNonNull(address, nameof(address));
            Validator.RequireNonNull(credentials, nameof(credentials));
            Validator.RequireNonNull(driverOptions, nameof(driverOptions));
            try
            {
                return Pinvoke.typedb_driver.driver_new(
                    address,
                    credentials.NativeObject,
                    driverOptions.NativeObject,
                    IDriver.Language);
            }
            catch (Pinvoke.Error e)
            {
                throw new TypeDBDriverException(e);
            }
        }

        private static Pinvoke.TypeDBDriver Open(ISet<string> addresses, Credentials credentials, DriverOptions driverOptions)
        {
            Validator.RequireNonNull(addresses, nameof(addresses));
            Validator.RequireNonNull(credentials, nameof(credentials));
            Validator.RequireNonNull(driverOptions, nameof(driverOptions));
            try
            {
                return Pinvoke.typedb_driver.driver_new_with_addresses(
                    addresses.ToArray(),
                    credentials.NativeObject,
                    driverOptions.NativeObject,
                    IDriver.Language);
            }
            catch (Pinvoke.Error e)
            {
                throw new TypeDBDriverException(e);
            }
        }

        private static Pinvoke.TypeDBDriver Open(IDictionary<string, string> addressTranslation, Credentials credentials, DriverOptions driverOptions)
        {
            Validator.RequireNonNull(addressTranslation, nameof(addressTranslation));
            Validator.RequireNonNull(credentials, nameof(credentials));
            Validator.RequireNonNull(driverOptions, nameof(driverOptions));
            try
            {
                string[] publicAddresses = addressTranslation.Keys.ToArray();
                string[] privateAddresses = addressTranslation.Values.ToArray();
                return Pinvoke.typedb_driver.driver_new_with_address_translation(
                    publicAddresses,
                    privateAddresses,
                    credentials.NativeObject,
                    driverOptions.NativeObject,
                    IDriver.Language);
            }
            catch (Pinvoke.Error e)
            {
                throw new TypeDBDriverException(e);
            }
        }

        public bool IsOpen()
        {
            if (!NativeObject.IsOwned())
            {
                return false;
            }
            return Pinvoke.typedb_driver.driver_is_open(NativeObject);
        }

        public IDatabaseManager Databases
        {
            get { return _databaseManager; }
        }

        public IUserManager Users
        {
            get { return _userManager; }
        }

        public ITransaction Transaction(string database, TransactionType type)
        {
            return Transaction(database, type, new TransactionOptions());
        }

        public ITransaction Transaction(string database, TransactionType type, TransactionOptions options)
        {
            Validator.RequireNonNull(database, nameof(database));
            Validator.RequireNonNull(options, nameof(options));
            try
            {
                Pinvoke.Transaction nativeTransaction = Pinvoke.typedb_driver.transaction_new(
                    NativeObject,
                    database,
                    (Pinvoke.TransactionType)type,
                    options.NativeObject);

                return new Transaction(this, nativeTransaction, type, options);
            }
            catch (Pinvoke.Error e)
            {
                throw new TypeDBDriverException(e);
            }
        }

        public ServerVersion GetServerVersion(ServerRouting? serverRouting = null)
        {
            try
            {
                return new ServerVersion(
                    Pinvoke.typedb_driver.driver_server_version(
                        NativeObject,
                        ServerRouting.GetNativeValue(serverRouting)));
            }
            catch (Pinvoke.Error e)
            {
                throw new TypeDBDriverException(e);
            }
        }

        public ISet<IServer> GetServers(ServerRouting? serverRouting = null)
        {
            try
            {
                return new NativeEnumerable<Pinvoke.Server>(
                    Pinvoke.typedb_driver.driver_servers(
                        NativeObject,
                        ServerRouting.GetNativeValue(serverRouting)))
                    .Select(s => (IServer)new ServerImpl(s))
                    .ToHashSet();
            }
            catch (Pinvoke.Error e)
            {
                throw new TypeDBDriverException(e);
            }
        }

        public IServer? GetPrimaryServer(ServerRouting? serverRouting = null)
        {
            try
            {
                Pinvoke.Server? nativeServer = Pinvoke.typedb_driver.driver_primary_server(
                    NativeObject,
                    ServerRouting.GetNativeValue(serverRouting));
                if (nativeServer != null)
                {
                    return new ServerImpl(nativeServer);
                }
                return null;
            }
            catch (Pinvoke.Error e)
            {
                throw new TypeDBDriverException(e);
            }
        }

        public void Close()
        {
            if (!NativeObject.IsOwned())
            {
                return;
            }

            try
            {
                Pinvoke.typedb_driver.driver_force_close(NativeObject);
            }
            catch (Pinvoke.Error e)
            {
                throw new TypeDBDriverException(e);
            }
        }

        public void Dispose()
        {
            Close();
        }
    }
}
