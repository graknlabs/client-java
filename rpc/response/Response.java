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

package grakn.client.rpc.response;

import grakn.client.common.exception.GraknClientException;
import grakn.protocol.TransactionProto;
import io.grpc.StatusRuntimeException;

import javax.annotation.Nullable;

import static grakn.common.util.Objects.className;

public class Response {

    private final TransactionProto.Transaction.Res nullableOk;
    private final StatusRuntimeException nullableError;

    Response(@Nullable TransactionProto.Transaction.Res nullableOk, @Nullable StatusRuntimeException nullableError) {
        this.nullableOk = nullableOk;
        this.nullableError = nullableError;
    }

    private static Response create(@Nullable TransactionProto.Transaction.Res response, @Nullable StatusRuntimeException error) {
        if (!(response == null || error == null)) {
            throw new GraknClientException(new IllegalArgumentException("One of Transaction.Res or StatusRuntimeException must be null"));
        }
        return new Response(response, error);
    }

    static Response completed() {
        return create(null, null);
    }

    static Response error(StatusRuntimeException error) {
        return create(null, error);
    }

    static Response ok(TransactionProto.Transaction.Res response) {
        return create(response, null);
    }

    @Nullable
    public TransactionProto.Transaction.Res nullableOk() {
        return nullableOk;
    }

    @Nullable
    StatusRuntimeException nullableError() {
        return nullableError;
    }

    public final Type type() {
        if (nullableOk() != null) {
            return Type.OK;
        } else if (nullableError() != null) {
            return Type.ERROR;
        } else {
            return Type.COMPLETED;
        }
    }

    public final TransactionProto.Transaction.Res ok() {
        if (nullableOk != null) {
            return nullableOk;
        } else if (nullableError != null) {
            // TODO: parse different GRPC errors into specific GraknClientException
            throw new GraknClientException(nullableError);
        } else {
            throw new GraknClientException("Transaction interrupted, all running queries have been stopped.");
        }
    }

    @Override
    public String toString() {
        return className(getClass()) + "{nullableOk=" + nullableOk + ", nullableError=" + nullableError + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof Response) {
            Response that = (Response) o;
            return ((this.nullableOk == null) ? (that.nullableOk() == null) : this.nullableOk.equals(that.nullableOk()))
                    && ((this.nullableError == null) ? (that.nullableError() == null) : this.nullableError.equals(that.nullableError()));
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h = 1;
        h *= 1000003;
        h ^= (nullableOk == null) ? 0 : this.nullableOk.hashCode();
        h *= 1000003;
        h ^= (nullableError == null) ? 0 : this.nullableError.hashCode();
        return h;
    }

    public enum Type {
        OK, ERROR, COMPLETED
    }
}
