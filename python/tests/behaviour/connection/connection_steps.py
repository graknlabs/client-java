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

from behave import *
from hamcrest import *
from tests.behaviour.config.parameters import MayError, check_is_none
from tests.behaviour.context import Context

from tests.behaviour.config.parameters import parse_list
from typedb.api.connection.server_routing import ServerRouting
from typedb.api.server.replication_role import ReplicationRole
from typedb.driver import TypeDB

# The HTTP endpoint's default port, which a connection string written for the HTTP driver names.
OTHER_PROTOCOL_DEFAULT_PORT = "8000"


def get_server_version(context: Context):
    if context.operation_server_routing:
        return context.driver.server_version(context.operation_server_routing)
    else:
        return context.driver.server_version()


def get_primary_server(context: Context):
    if context.operation_server_routing:
        return context.driver.primary_server(context.operation_server_routing)
    else:
        return context.driver.primary_server()


def get_servers(context: Context):
    if context.operation_server_routing:
        return context.driver.servers(context.operation_server_routing)
    else:
        return context.driver.servers()


def replace_host(address: str, new_host: str) -> str:
    return address.replace("127.0.0.1", new_host)


def replace_port(address: str, new_port: str) -> str:
    address_parts = address.rsplit(":", 1)
    address_parts[-1] = new_port
    return ":".join(address_parts)


@step(u'typedb starts')
def step_impl(context: Context):
    # TODO: start TypeDB via a python typedb-runner once one is available
    pass


@step(u'connection opens with default authentication{may_error:MayError}')
def step_impl(context: Context, may_error: MayError):
    may_error.check(lambda: context.setup_context_driver_fn())


@step(u'connection opens to single server with default authentication')
def step_impl(context: Context):
    context.setup_context_driver_fn(address=[context.default_address[0]])


@step(u'connection opens with a wrong host{may_error:MayError}')
def step_impl(context: Context, may_error: MayError):
    address = context.default_address
    if isinstance(address, str):
        address = replace_host(address, "surely-not-localhost")
    elif isinstance(address, list):
        address = [replace_host(addr, "surely-not-localhost") for addr in address]
    else:
        raise Exception("Unexpected default address: cannot finish the test")
    may_error.check(lambda: context.setup_context_driver_fn(address=address))


@step(u'connection opens with a wrong port{may_error:MayError}')
def step_impl(context: Context, may_error: MayError):
    address = context.default_address
    if isinstance(address, str):
        address = replace_port(address, "0")
    elif isinstance(address, list):
        address = [replace_port(addr, "0") for addr in address]
    else:
        raise Exception("Unexpected default address: cannot finish the test")
    may_error.check(lambda: context.setup_context_driver_fn(address=address))


def default_connection_string(context: Context, port: str = None) -> str:
    """A connection string for this mode's default addresses with admin credentials, using the scheme
    that agrees with the context's TLS setting. Each port is replaced by `port` if given."""
    scheme = "https" if context.driver_options.tls_config.is_enabled else "http"
    addresses = context.default_address
    if isinstance(addresses, str):
        addresses = [addresses]
    addresses = [f"{scheme}://{replace_port(address, port) if port else address}" for address in addresses]
    return f"typedb://{context.DEFAULT_USERNAME}:{context.DEFAULT_PASSWORD}@{','.join(addresses)}"


def setup_context_driver_from_connection_string(context: Context, connection_string: str):
    context.driver = TypeDB.driver_from_connection_string(connection_string, context.driver_options)


@step(u'connection opens with a connection string for the default address{may_error:MayError}')
def step_impl(context: Context, may_error: MayError):
    may_error.check(lambda: setup_context_driver_from_connection_string(context, default_connection_string(context)))


@step(u'connection opens with connection string: "{connection_string:NonSemicolon}"{may_error:MayError}')
def step_impl(context: Context, connection_string: str, may_error: MayError):
    may_error.check(lambda: setup_context_driver_from_connection_string(context, connection_string))


@step(u'connection opens with a connection string for a wrong port{may_error:MayError}')
def step_impl(context: Context, may_error: MayError):
    may_error.check(lambda: setup_context_driver_from_connection_string(context, default_connection_string(context, "0")))


@step(u'connection opens with a connection string for the other protocol default port{may_error:MayError}')
def step_impl(context: Context, may_error: MayError):
    may_error.check(lambda: setup_context_driver_from_connection_string(
        context, default_connection_string(context, OTHER_PROTOCOL_DEFAULT_PORT)))


@step(
    u"connection opens with username '{username:NonSemicolon}', password '{password:NonSemicolon}'{may_error:MayError}")
def step_impl(context: Context, username: str, password: str, may_error: MayError):
    may_error.check(lambda: context.setup_context_driver_fn(username=username, password=password))


@step(u'connection closes')
def step_impl(context: Context):
    context.driver.close()


@step(u'typedb stops')
def step_impl(context: Context):
    # TODO: stop TypeDB via a python typedb-runner once one is available
    pass


@step("connection is open: {is_open:Bool}")
def step_impl(context: Context, is_open: bool):
    real_is_open = hasattr(context, 'driver') and context.driver and context.driver.is_open()
    assert_that(real_is_open, equal_to(is_open))


@step(u'connection contains distribution{may_error:MayError}')
def step_impl(context: Context, may_error: MayError):
    may_error.check(lambda: assert_that(len(get_server_version(context).distribution), greater_than(0)))


@step(u'connection contains version{may_error:MayError}')
def step_impl(context: Context, may_error: MayError):
    may_error.check(lambda: assert_that(len(get_server_version(context).version), greater_than(0)))


@step("connection has {count:Int} server")
@step("connection has {count:Int} servers")
def step_impl(context: Context, count: int):
    assert_that(len(get_servers(context)), equal_to(count))


@step(u'connection primary server exists')
def step_impl(context: Context):
    check_is_none(get_primary_server(context), False)


@step(u'connection get server({address}) {exists_or_doesnt:ExistsOrDoesnt}')
def step_impl(context: Context, address: str, exists_or_doesnt: bool):
    servers = get_servers(context)
    exists = any(r.address == address for r in servers)
    assert_that(exists, equal_to(exists_or_doesnt), f"server {address}")


@step(u'connection get server({address}) has term')
def step_impl(context: Context, address: str):
    servers = get_servers(context)
    server = next((s for s in servers if s.address == address), None)
    check_is_none(server, False)
    check_is_none(server.term, False)


@step(u'connection servers have roles')
def step_impl(context: Context):
    servers = get_servers(context)
    expected_roles = parse_list(context)
    expected_primary_count = 0
    expected_secondary_count = 0
    expected_candidate_count = 0
    for role in expected_roles:
        if role == 'primary':
            expected_primary_count += 1
        elif role == 'secondary':
            expected_secondary_count += 1
        elif role == 'candidate':
            expected_candidate_count += 1
        else:
            raise ValueError(f"Unknown server replication role: {role}")

    actual_primary_count = sum(1 for r in servers if r.role.is_primary())
    actual_secondary_count = sum(1 for r in servers if r.role.is_secondary())
    actual_candidate_count = sum(1 for r in servers if r.role.is_candidate())

    assert_that(actual_primary_count, equal_to(expected_primary_count),
                f"Expected {expected_primary_count} primary servers, found {actual_primary_count}")
    assert_that(actual_secondary_count, equal_to(expected_secondary_count),
                f"Expected {expected_secondary_count} secondary servers, found {actual_secondary_count}")
    assert_that(actual_candidate_count, equal_to(expected_candidate_count),
                f"Expected {expected_candidate_count} candidate servers, found {actual_candidate_count}")


@step("connection has {count:Int} database")
@step("connection has {count:Int} databases")
def step_impl(context: Context, count: int):
    assert_that(len(context.driver.databases.all()), equal_to(count))


@step("connection has {count:Int} user")
@step("connection has {count:Int} users")
def step_impl(context: Context, count: int):
    assert_that(len(context.driver.users.all()), equal_to(count))


@step("set operation server routing to: {server_routing:ServerRouting}")
def step_impl(context: Context, server_routing: ServerRouting):
    context.operation_server_routing = server_routing.server_routing


@step("set driver option primary_failover_retries to: {value:Int}")
def step_impl(context: Context, value: int):
    context.driver_options.primary_failover_retries = value


@step("set driver option request_timeout_millis to: {value:Int}")
def step_impl(context: Context, value: int):
    context.driver_options.request_timeout_millis = value



