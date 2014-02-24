/*
 *
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
 *
 */
package org.apache.qpid.server.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public interface Consumer<X extends Consumer<X>> extends ConfiguredObject<X>
{
    public String DISTRIBUTION_MODE = "distributionMode";
    public String EXCLUSIVE = "exclusive";
    public String NO_LOCAL = "noLocal";
    public String SELECTOR = "selector";
    public String SETTLEMENT_MODE = "settlementMode";
    public String DURABLE = "durable";
    public String LIFETIME_POLICY = "lifetimePolicy";
    public String STATE = "state";
    public String TIME_TO_LIVE = "timeToLive";

    @ManagedAttribute
    String getDistributionMode();

    @ManagedAttribute
    String getSettlementMode();

    @ManagedAttribute
    boolean isExclusive();

    @ManagedAttribute
    boolean isNoLocal();

    @ManagedAttribute
    String getSelector();



    public String BYTES_OUT = "bytesOut";
    public String MESSAGES_OUT = "messagesOut";
    public String STATE_CHANGED = "stateChanged";
    public String UNACKNOWLEDGED_BYTES = "unacknowledgedBytes";
    public String UNACKNOWLEDGED_MESSAGES = "unacknowledgedMessages";

    public Collection<String> AVAILABLE_STATISTICS =
            Collections.unmodifiableCollection(
                    Arrays.asList(BYTES_OUT,
                                  MESSAGES_OUT,
                                  STATE_CHANGED,
                                  UNACKNOWLEDGED_BYTES,
                                  UNACKNOWLEDGED_MESSAGES)
            );
}
