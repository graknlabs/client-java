# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

from __future__ import annotations

from typing import TYPE_CHECKING, Optional, Tuple

from typedb.api.connection.server_routing import ServerRouting
from typedb.api.connection.driver import Driver
from typedb.api.connection.transaction_options import TransactionOptions
from typedb.api.server.server_version import ServerVersion
from typedb.common.exception import TypeDBDriverException, DRIVER_CLOSED, INVALID_ADDRESS_FORMAT
from typedb.common.iterator_wrapper import IteratorWrapper
from typedb.common.native_wrapper import NativeWrapper
from typedb.common.validation import require_non_null, require_non_negative
from typedb.database.database_manager import _DatabaseManager
from typedb.connection.server import _Server
from typedb.connection.transaction import _Transaction
from typedb.native_driver_wrapper import driver_new, driver_new_with_addresses, \
    driver_new_with_address_translation, driver_new_with_connection_string, init_logging, \
    driver_is_open, driver_force_close, \
    driver_servers, driver_primary_server, driver_server_version, \
    server_iterator_next, TypeDBDriver as NativeDriver, \
    TypeDBDriverExceptionNative
from typedb.user.user_manager import _UserManager

if TYPE_CHECKING:
    from typedb.api.connection.driver_options import DriverOptions
    from typedb.api.connection.credentials import Credentials
    from typedb.api.connection.transaction import Transaction, TransactionType
    from typedb.api.user.user_manager import UserManager
    from typedb.api.server.server import Server


class _Driver(Driver, NativeWrapper[NativeDriver]):

    def __init__(self, addresses: str | list[str] | dict[str, str], credentials: Credentials,
                 driver_options: DriverOptions):
        require_non_null(addresses, "addresses")
        require_non_null(credentials, "credentials")
        require_non_null(driver_options, "driver_options")

        try:
            if isinstance(addresses, str):
                native_driver = driver_new(addresses, credentials.native_object,
                                           driver_options.native_object,
                                           Driver.LANGUAGE)
            elif isinstance(addresses, list):
                native_driver = driver_new_with_addresses(addresses, credentials.native_object,
                                                          driver_options.native_object,
                                                          Driver.LANGUAGE)
            elif isinstance(addresses, dict):
                public_addresses, private_addresses = _Driver._get_translated_addresses(addresses)
                native_driver = driver_new_with_address_translation(public_addresses,
                                                                    private_addresses,
                                                                    credentials.native_object,
                                                                    driver_options.native_object,
                                                                    Driver.LANGUAGE)
            else:
                raise TypeDBDriverException(INVALID_ADDRESS_FORMAT)
        except TypeDBDriverExceptionNative as e:
            raise TypeDBDriverException.of(e) from None
        super().__init__(native_driver)

    @classmethod
    def from_connection_string(cls, connection_string: str,
                               driver_options: Optional[DriverOptions] = None) -> _Driver:
        require_non_null(connection_string, "connection_string")
        init_logging()

        try:
            native_driver = driver_new_with_connection_string(
                connection_string,
                driver_options.native_object if driver_options else None,
                Driver.LANGUAGE)
        except TypeDBDriverExceptionNative as e:
            raise TypeDBDriverException.of(e) from None
        # The addresses and credentials come from the string, so __init__'s dispatch does not apply.
        driver = cls.__new__(cls)
        NativeWrapper.__init__(driver, native_driver)
        return driver

    @property
    def _native_object_not_owned_exception(self) -> TypeDBDriverException:
        return TypeDBDriverException(DRIVER_CLOSED)

    @property
    def _native_driver(self) -> NativeDriver:
        return self.native_object

    def is_open(self) -> bool:
        return driver_is_open(self._native_driver)

    @property
    def databases(self) -> _DatabaseManager:
        return _DatabaseManager(self._native_driver)

    @property
    def users(self) -> UserManager:
        return _UserManager(self._native_driver)

    def server_version(self, server_routing: Optional[ServerRouting] = None) -> ServerVersion:
        try:
            server_routing = ServerRouting.native_value(server_routing)
            return ServerVersion(driver_server_version(self._native_driver, server_routing))
        except TypeDBDriverExceptionNative as e:
            raise TypeDBDriverException.of(e) from None

    def transaction(self, database_name: str, transaction_type: TransactionType,
                    options: Optional[TransactionOptions] = None) -> Transaction:
        require_non_null(database_name, "database_name")
        require_non_null(transaction_type, "transaction_type")
        return _Transaction(self, database_name, transaction_type, options if options else TransactionOptions())

    def servers(self, server_routing: Optional[ServerRouting] = None) -> set[Server]:
        try:
            server_routing = ServerRouting.native_value(server_routing)
            server_iter = IteratorWrapper(driver_servers(self._native_driver, server_routing),
                                          server_iterator_next)
            return set(_Server(server) for server in server_iter)
        except TypeDBDriverExceptionNative as e:
            raise TypeDBDriverException.of(e) from None

    def primary_server(self, server_routing: Optional[ServerRouting] = None) -> Optional[Server]:
        server_routing = ServerRouting.native_value(server_routing)
        if res := driver_primary_server(self._native_driver, server_routing):
            return _Server(res)
        return None

    def close(self) -> None:
        driver_force_close(self._native_driver)

    @classmethod
    def _get_translated_addresses(cls, address_translation: dict[str, str]) -> Tuple[list[str], list[str]]:
        public_addresses = list(address_translation.keys())
        private_addresses = [address_translation[public] for public in public_addresses]
        return public_addresses, private_addresses

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.close()
        if exc_tb is not None:
            return False
