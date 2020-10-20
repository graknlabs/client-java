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

package grakn.client.rpc;

import com.google.protobuf.ByteString;
import grakn.client.Grakn.Session;
import grakn.client.Grakn.Transaction;
import grakn.client.GraknOptions;
import grakn.protocol.GraknGrpc;
import grakn.protocol.SessionProto;
import io.grpc.ManagedChannel;

import static grakn.client.common.ProtoBuilder.options;

public class RPCSession implements Session {

    private final String databaseName;
    private final Type type;
    private final GraknGrpc.GraknBlockingStub blockingGrpcStub;
    private final GraknGrpc.GraknStub asyncGrpcStub;
    private final ByteString sessionId;
    private boolean isOpen;

    RPCSession(final ManagedChannel channel, final String databaseName, final Type type, final GraknOptions options) {
        this.databaseName = databaseName;
        this.type = type;
        this.blockingGrpcStub = GraknGrpc.newBlockingStub(channel);
        this.asyncGrpcStub = GraknGrpc.newStub(channel);

        final SessionProto.Session.Open.Req openReq = SessionProto.Session.Open.Req.newBuilder()
                .setDatabase(databaseName).setType(sessionType(type)).setOptions(options(options)).build();

        // TODO: In theory we could generate a client-side session ID and remove the need to block here to wait for
        //       the server-side session ID to be generated.
        sessionId = blockingGrpcStub.sessionOpen(openReq).getSessionID();
        isOpen = true;
    }

    @Override
    public Transaction transaction(final Transaction.Type type) {
        return transaction(type, new GraknOptions());
    }

    @Override
    public Transaction transaction(final Transaction.Type type, final GraknOptions options) {
        return new RPCTransaction(this, sessionId, type, options);
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public boolean isOpen() {
        return isOpen;
    }

    public void close() {
        if (!isOpen) return;
        // TODO: this is dangerous - we can call close() on two concurrent threads and cause havoc, right?
        //       isOpen should be changed to an AtomicBoolean that we update at the start of this method
        final SessionProto.Session.Close.Req closeReq = SessionProto.Session.Close.Req.newBuilder().setSessionID(sessionId).build();

        blockingGrpcStub.sessionClose(closeReq);
        isOpen = false;
    }

    @Override
    public String databaseName() {
        return databaseName;
    }

    GraknGrpc.GraknStub getAsyncGrpcStub() {
        return asyncGrpcStub;
    }

    private static SessionProto.Session.Type sessionType(final Session.Type type) {
        switch (type) {
            case DATA:
                return SessionProto.Session.Type.DATA;
            case SCHEMA:
                return SessionProto.Session.Type.SCHEMA;
            default:
                return SessionProto.Session.Type.UNRECOGNIZED;
        }
    }
}
