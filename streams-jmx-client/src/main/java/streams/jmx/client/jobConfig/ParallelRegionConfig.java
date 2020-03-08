// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package streams.jmx.client.jobConfig;

public enum ParallelRegionConfig {
    NO_CHANNEL_INFLUENCE(0, "noChannelInfluence", "(default) Inclusion in this parallelRegion has no impact on the fusion process."),
    CHANNEL_ISOLATION(1, "channelIsolation", "Operators within a channel are fused into a processing element only with operators from the same channel."),
    CHANNEL_EXLOCATION(2, "channelExlocation", "Operators within a channel are fused with operators from the same channel or with operators from outside the region.");

    private final int code;
    private final String value;
    private final String description;

    private ParallelRegionConfig(int code, String value, String description) {
        this.code = code;
        this.value = value;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return value;
    }
}
