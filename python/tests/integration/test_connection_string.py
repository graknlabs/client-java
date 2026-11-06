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

# The grammar itself is covered by the shared vectors in tool/test/connection-string-vectors.json,
# which the Rust core tests. These cases exercise the error paths through the FFI layer, and so need
# no running server.

import unittest
from unittest import TestCase

from hamcrest import *
from typedb.driver import *


class TestConnectionString(TestCase):

    def test_invalid_connection_string_raises(self):
        with self.assertRaises(TypeDBDriverException) as error:
            TypeDB.driver_from_connection_string("typedb://admin@https://127.0.0.1:1729")
        assert_that(str(error.exception), contains_string("Invalid connection string"))

    def test_address_without_a_scheme_raises(self):
        with self.assertRaises(TypeDBDriverException) as error:
            TypeDB.driver_from_connection_string("typedb://admin:password@127.0.0.1:1729")
        assert_that(str(error.exception), contains_string("http://"))

    def test_driver_options_contradicting_the_scheme_raise(self):
        with self.assertRaises(TypeDBDriverException) as error:
            TypeDB.driver_from_connection_string("typedb://admin:password@https://127.0.0.1:1729",
                                                 DriverOptions(DriverTlsConfig.disabled()))
        assert_that(str(error.exception), contains_string("not compatible"))


if __name__ == "__main__":
    unittest.main(verbosity=2)
