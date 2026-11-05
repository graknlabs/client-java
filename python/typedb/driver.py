# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

from collections.abc import Mapping as ABCMapping
from datetime import date
from decimal import Decimal
from typing import Any, Dict, Iterable, List, Mapping, Optional, Union

from typedb.api.answer.concept_document_iterator import *  # noqa # pylint: disable=unused-import
from typedb.api.answer.concept_row import *  # noqa # pylint: disable=unused-import
from typedb.api.answer.concept_row_iterator import *  # noqa # pylint: disable=unused-import
from typedb.api.answer.query_answer import *  # noqa # pylint: disable=unused-import
from typedb.api.answer.query_type import *  # noqa # pylint: disable=unused-import
from typedb.api.concept.concept import *  # noqa # pylint: disable=unused-import
from typedb.api.concept.given_rows import GivenRows
from typedb.api.concept.instance.attribute import *  # noqa # pylint: disable=unused-import
from typedb.api.concept.instance.entity import *  # noqa # pylint: disable=unused-import
from typedb.api.concept.instance.instance import *  # noqa # pylint: disable=unused-import
from typedb.api.concept.instance.relation import *  # noqa # pylint: disable=unused-import
from typedb.api.concept.type.attribute_type import *  # noqa # pylint: disable=unused-import
from typedb.api.concept.type.entity_type import *  # noqa # pylint: disable=unused-import
from typedb.api.concept.type.relation_type import *  # noqa # pylint: disable=unused-import
from typedb.api.concept.type.role_type import *  # noqa # pylint: disable=unused-import
from typedb.api.concept.type.type import *  # noqa # pylint: disable=unused-import
from typedb.api.concept.value import *  # noqa # pylint: disable=unused-import
from typedb.api.connection.server_routing import *  # noqa # pylint: disable=unused-import
from typedb.api.connection.credentials import *  # noqa # pylint: disable=unused-import
from typedb.api.connection.driver import *  # noqa # pylint: disable=unused-import
from typedb.api.connection.driver_options import *  # noqa # pylint: disable=unused-import
from typedb.api.connection.driver_tls_config import *  # noqa # pylint: disable=unused-import
from typedb.api.connection.query_options import *  # noqa # pylint: disable=unused-import
from typedb.api.connection.transaction import *  # noqa # pylint: disable=unused-import
from typedb.api.connection.transaction_options import *  # noqa # pylint: disable=unused-import
from typedb.api.database.database import *  # noqa # pylint: disable=unused-import
from typedb.api.database.database_manager import *  # noqa # pylint: disable=unused-import
from typedb.api.user.user import *  # noqa # pylint: disable=unused-import
from typedb.api.user.user_manager import *  # noqa # pylint: disable=unused-import
from typedb.common.datetime import *  # noqa # pylint: disable=unused-import
from typedb.common.duration import *  # noqa # pylint: disable=unused-import
from typedb.common.exception import *  # noqa # pylint: disable=unused-import
# Repackaging these symbols allows them to be imported from "typedb.driver"

from typedb.connection.driver import _Driver

class TypeDB:
    DEFAULT_ADDRESS = "127.0.0.1:1729"
    ADDRESSES = Union[Mapping[str, str], Iterable[str], str]

    @staticmethod
    def driver(addresses: ADDRESSES, credentials: Credentials, driver_options: DriverOptions) -> Driver:
        """
        Creates a connection to TypeDB.

        :param addresses: Address(-es) of the TypeDB server(-s).
        Can be a single string, multiple strings, or multiple pairs of strings.
        :param credentials: The credentials to connect with.
        :param driver_options: The driver connection options to connect with.
        """
        if isinstance(addresses, str):
            return _Driver(addresses, credentials, driver_options)
        elif isinstance(addresses, ABCMapping):
            return _Driver(dict(addresses), credentials, driver_options)
        else:
            return _Driver(list(addresses), credentials, driver_options)

    @staticmethod
    def driver_from_connection_string(connection_string: str,
                                      driver_options: Optional[DriverOptions] = None) -> Driver:
        """
        Creates a connection to TypeDB from a connection string of the form
        ``typedb://<username>:<password>@<addresses>``.

        Whether the connection uses TLS is determined by the addresses' ``http://`` or ``https://``
        scheme. If driver options are given, their TLS configuration must agree with that scheme;
        otherwise, TLS is configured from the scheme alone, using the system's native trust roots.

        The addresses are connected to as written, with port 1729 assumed where none is given. If an
        address carries the HTTP endpoint's default port 8000 and answers with HTTP rather than gRPC,
        the driver retries on port 1729 and logs a warning, so a connection string copied from TypeDB
        Studio works unchanged.

        :param connection_string: The connection string holding the addresses and credentials.
        :param driver_options: The driver connection options to connect with. Optional: defaults are
        derived from the connection string.
        """
        return _Driver.from_connection_string(connection_string, driver_options)

    class Concept:
        """Factory class to instantiate new ``Value`` concepts and build ``GivenRows`` for use as query inputs."""

        @staticmethod
        def given_rows(variables: List[str], rows: "List[List[Optional[typedb.api.concept.concept.Concept]]]") -> GivenRows:
            """Constructs a ``GivenRows`` instance for use as inputs to queries."""
            from typedb.concept.given_rows import _GivenRows
            return _GivenRows.of(variables, rows)

        @staticmethod
        def given_rows_from_map(rows: "List[Dict[str, typedb.api.concept.concept.Concept | Any]]") -> GivenRows:
            """Constructs a ``GivenRows`` instance from dict-based rows for use as inputs to queries."""
            from typedb.concept.given_rows import _GivenRows
            return _GivenRows.of_map(rows)

        @staticmethod
        def try_convert_to_value(value: Any):
            """Converts a raw host-language object to a ``Value`` concept.
            Accepted types: ``bool``, ``int``, ``float``, ``Decimal``, ``str``, ``date``, ``Datetime``, ``Duration``.

            :raises TypeDBDriverException: if the object's type is not supported.
            """
            return _Value.try_convert_to_value(value)

        @staticmethod
        def new_boolean(value: bool):
            """Creates a new ``boolean`` Value concept."""
            from typedb.concept.value.value import _Value
            return _Value.new_boolean(value)

        @staticmethod
        def new_integer(value: int):
            """Creates a new ``integer`` Value concept."""
            from typedb.concept.value.value import _Value
            return _Value.new_integer(value)

        @staticmethod
        def new_double(value: float):
            """Creates a new ``double`` Value concept."""
            from typedb.concept.value.value import _Value
            return _Value.new_double(value)

        @staticmethod
        def new_decimal(value: Decimal):
            """Creates a new ``decimal`` Value concept."""
            from typedb.concept.value.value import _Value
            return _Value.new_decimal(value)

        @staticmethod
        def new_string(value: str):
            """Creates a new ``string`` Value concept."""
            from typedb.concept.value.value import _Value
            return _Value.new_string(value)

        @staticmethod
        def new_date(value: date):
            """Creates a new ``date`` Value concept."""
            from typedb.concept.value.value import _Value
            return _Value.new_date(value)

        @staticmethod
        def new_datetime(value: Datetime):
            """Creates a new ``datetime`` Value concept."""
            from typedb.concept.value.value import _Value
            return _Value.new_datetime(value)

        @staticmethod
        def new_datetime_tz(value: Datetime):
            """Creates a new ``datetime-tz`` Value concept."""
            from typedb.concept.value.value import _Value
            return _Value.new_datetime_tz(value)

        @staticmethod
        def new_duration(value: Duration):
            """Creates a new ``duration`` Value concept."""
            from typedb.concept.value.value import _Value
            return _Value.new_duration(value)
