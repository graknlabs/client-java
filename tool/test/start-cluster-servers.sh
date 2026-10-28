#!/usr/bin/env bash
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

set -e

NODE_COUNT="${1:-1}"
ENCRYPTION_ENABLED="${2:-true}"
export ENCRYPTION_ENABLED

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CLUSTER_SERVER="${SCRIPT_DIR}/cluster-server.sh"

ROOT_CA_PATH="$(realpath tool/test/resources/encryption/ext-grpc-root-ca.pem)"

rm -rf $(seq 1 $NODE_COUNT) typedb-cluster-all

if [ -n "${TYPEDB_CLUSTER_TARBALL:-}" ]; then
  echo "Using local typedb-cluster tarball: ${TYPEDB_CLUSTER_TARBALL}"
  mkdir -p typedb-cluster-all
  tar -xzf "${TYPEDB_CLUSTER_TARBALL}" -C typedb-cluster-all --strip-components=1
else
  bazel run //tool/test:typedb-cluster-extractor -- typedb-cluster-all
fi

# Local-binary override (session-only): if TYPEDB_CLUSTER_SERVER_BIN / TYPEDB_CLUSTER_ADMIN_BIN
# point at executables, replace the bundled binaries before copying to node dirs.
if [ -n "${TYPEDB_CLUSTER_SERVER_BIN:-}" ]; then
  if [ ! -x "$TYPEDB_CLUSTER_SERVER_BIN" ]; then
    echo "TYPEDB_CLUSTER_SERVER_BIN='$TYPEDB_CLUSTER_SERVER_BIN' is not executable"; exit 1
  fi
  echo "Overriding bundled typedb_server_bin with $TYPEDB_CLUSTER_SERVER_BIN"
  cp -f "$TYPEDB_CLUSTER_SERVER_BIN" typedb-cluster-all/server/typedb_server_bin
fi
if [ -n "${TYPEDB_CLUSTER_ADMIN_BIN:-}" ]; then
  if [ ! -x "$TYPEDB_CLUSTER_ADMIN_BIN" ]; then
    echo "TYPEDB_CLUSTER_ADMIN_BIN='$TYPEDB_CLUSTER_ADMIN_BIN' is not executable"; exit 1
  fi
  echo "Overriding bundled typedb_admin_bin with $TYPEDB_CLUSTER_ADMIN_BIN"
  cp -f "$TYPEDB_CLUSTER_ADMIN_BIN" typedb-cluster-all/admin/typedb_admin_bin
fi

echo Successfully unarchived a TypeDB distribution. Creating $NODE_COUNT copies.
for i in $(seq 1 $NODE_COUNT); do
  rm -rf $i
  cp -r typedb-cluster-all $i || exit 1
done

echo Starting a cluster consisting of $NODE_COUNT servers...
for i in $(seq 1 $NODE_COUNT); do
  if [ "$i" -eq 1 ]; then
    INIT_CREATE_CLUSTER=true "${CLUSTER_SERVER}" start $i
  else
    INIT_CREATE_CLUSTER=false "${CLUSTER_SERVER}" start $i
  fi
done

for i in $(seq 1 $NODE_COUNT); do
  "${CLUSTER_SERVER}" await $i
done
echo $NODE_COUNT TypeDB Cluster database servers started

# Register peer replicas via admin tool on node 1 (with retry for leader election)
REGISTER_MAX_RETRIES=10
REGISTER_RETRY_INTERVAL=2
if [ "$NODE_COUNT" -gt 1 ]; then
  for i in $(seq 2 $NODE_COUNT); do
    clustering_port="${i}1730"
    for attempt in $(seq 1 $REGISTER_MAX_RETRIES); do
      if ./1/typedb admin --socket-path=./1/data/admin.sock --command "servers register ${i} 127.0.0.1:${clustering_port}" 2>&1; then
        break
      fi
      if [ "$attempt" -eq "$REGISTER_MAX_RETRIES" ]; then
        echo "Failed to register replica ${i} after ${REGISTER_MAX_RETRIES} attempts"
        for n in $(seq 1 $NODE_COUNT); do
          echo "----- ./${n}/server.log -----"
          tail -n 50 "./${n}/server.log" 2>/dev/null || echo "(no log)"
        done
        exit 1
      fi
      echo "  Retrying registration of replica ${i} (attempt ${attempt}/${REGISTER_MAX_RETRIES})..."
      sleep $REGISTER_RETRY_INTERVAL
    done
  done
fi

# Wait for each replica to become a secondary
SECONDARY_MAX_RETRIES=60
if [ "$NODE_COUNT" -gt 1 ]; then
  for attempt in $(seq 1 $SECONDARY_MAX_RETRIES); do
    voters=$(./1/typedb admin --socket-path=./1/data/admin.sock --command "servers status" 2>&1 |
      awk -F'|' '{ gsub(/ /, "", $3) } $3 == "primary" || $3 == "secondary" { n++ } END { print n + 0 }')
    if [ "$voters" -eq "$NODE_COUNT" ]; then
      break
    fi
    if [ "$attempt" -eq "$SECONDARY_MAX_RETRIES" ]; then
      echo "Replicas did not become secondaries after ${SECONDARY_MAX_RETRIES} seconds"
      ./1/typedb admin --socket-path=./1/data/admin.sock --command "servers status"
      exit 1
    fi
    sleep 1
  done
  echo "$((NODE_COUNT - 1)) replicas are secondaries"
fi

ROOT_CA=$ROOT_CA_PATH
export ROOT_CA
export CLUSTER_SERVER_SCRIPT="${CLUSTER_SERVER}"
export CLUSTER_DIR="$(pwd)"

# Tail server logs in background so they appear in CI output. Track PIDs so
# stop-cluster-servers.sh can reap them — orphaned `tail -f` processes inherit
# the CI runner's stdout pipe and keep it open after the main command exits,
# leaving the job hanging.
declare -a _cluster_tail_pids=()
for i in $(seq 1 $NODE_COUNT); do
  if [ -f "./${i}/server.log" ]; then
    tail -f "./${i}/server.log" | sed "s/^/[${i}] /" &
    _cluster_tail_pids+=("$!")
  fi
done
export CLUSTER_TAIL_PIDS="${_cluster_tail_pids[*]}"
