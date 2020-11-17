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

package grakn.client.common;

import grabl.tracing.client.GrablTracingThreadStatic;
import grakn.client.GraknOptions;
import grakn.protocol.OptionsProto;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static grabl.tracing.client.GrablTracingThreadStatic.currentThreadTrace;
import static grabl.tracing.client.GrablTracingThreadStatic.isTracingEnabled;

public abstract class ProtoBuilder {

    public static OptionsProto.Options options(GraknOptions options) {
        final OptionsProto.Options.Builder builder = OptionsProto.Options.newBuilder();
        options.infer().ifPresent(builder::setInfer);
        options.explain().ifPresent(builder::setExplain);
        options.batchSize().ifPresent(builder::setBatchSize);
        return builder.build();
    }

    public static Map<String, String> tracingData() {
        if (isTracingEnabled()) {
            final GrablTracingThreadStatic.ThreadTrace threadTrace = currentThreadTrace();
            if (threadTrace == null) {
                return Collections.emptyMap();
            }

            if (threadTrace.getId() == null || threadTrace.getRootId() == null) {
                return Collections.emptyMap();
            }

            final Map<String, String> metadata = new HashMap<>(2);
            metadata.put("traceParentId", threadTrace.getId().toString());
            metadata.put("traceRootId", threadTrace.getRootId().toString());
            return metadata;
        } else {
            return Collections.emptyMap();
        }
    }
}
