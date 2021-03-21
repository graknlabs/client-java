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

package grakn.client.core;

import com.google.protobuf.ByteString;
import grakn.client.api.GraknOptions;
import grakn.client.api.GraknTransaction;
import grakn.client.api.concept.ConceptManager;
import grakn.client.api.logic.LogicManager;
import grakn.client.api.query.QueryFuture;
import grakn.client.api.query.QueryManager;
import grakn.client.common.exception.GraknClientException;
import grakn.client.concept.ConceptManagerImpl;
import grakn.client.logic.LogicManagerImpl;
import grakn.client.query.QueryManagerImpl;
import grakn.client.stream.BidirectionalStream;
import grakn.protocol.TransactionProto.Transaction.Req;
import grakn.protocol.TransactionProto.Transaction.Res;
import grakn.protocol.TransactionProto.Transaction.ResPart;
import io.grpc.StatusRuntimeException;

import java.util.stream.Stream;

import static grakn.client.common.exception.ErrorMessage.Client.TRANSACTION_CLOSED;
import static grakn.client.common.rpc.RequestBuilder.Transaction.commitReq;
import static grakn.client.common.rpc.RequestBuilder.Transaction.openReq;
import static grakn.client.common.rpc.RequestBuilder.Transaction.rollbackReq;

public class CoreTransaction implements GraknTransaction.Extended {

    private final GraknTransaction.Type type;
    private final GraknOptions options;
    private final ConceptManager conceptMgr;
    private final LogicManager logicMgr;
    private final QueryManager queryMgr;

    private final BidirectionalStream bidirectionalStream;

    CoreTransaction(CoreSession session, ByteString sessionId, Type type, GraknOptions options) {
        this.type = type;
        this.options = options;
        conceptMgr = new ConceptManagerImpl(this);
        logicMgr = new LogicManagerImpl(this);
        queryMgr = new QueryManagerImpl(this);
        bidirectionalStream = new BidirectionalStream(session.stub(), session.transmitter());
        execute(openReq(sessionId, type.proto(), options.proto(), session.networkLatencyMillis()), false);
    }

    @Override
    public Type type() { return type; }

    @Override
    public GraknOptions options() { return options; }

    @Override
    public boolean isOpen() { return bidirectionalStream.isOpen(); }

    @Override
    public ConceptManager concepts() { return conceptMgr; }

    @Override
    public LogicManager logic() { return logicMgr; }

    @Override
    public QueryManager query() { return queryMgr; }

    @Override
    public Res execute(Req.Builder request) {
        return execute(request, true);
    }

    private Res execute(Req.Builder request, boolean batch) {
        return query(request, batch).map(res -> res).get();
    }

    @Override
    public QueryFuture<Res> query(Req.Builder request) {
        return query(request, true);
    }

    private QueryFuture<Res> query(Req.Builder request, boolean batch) {
        if (!isOpen()) throw new GraknClientException(TRANSACTION_CLOSED);
        BidirectionalStream.Single<Res> single = bidirectionalStream.single(request, batch);
        return single::get;
    }

    @Override
    public Stream<ResPart> stream(Req.Builder request) {
        if (!isOpen()) throw new GraknClientException(TRANSACTION_CLOSED);
        return bidirectionalStream.stream(request);
    }

    @Override
    public void commit() {
        try {
            execute(commitReq());
        } finally {
            close();
        }
    }

    @Override
    public void rollback() {
        execute(rollbackReq());
    }

    @Override
    public void close() {
        bidirectionalStream.close();
    }
}
