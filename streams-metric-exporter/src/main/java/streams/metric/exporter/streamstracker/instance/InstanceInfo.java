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

package streams.metric.exporter.streamstracker.instance;

import com.ibm.streams.management.instance.InstanceMXBean;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

/* 
 * InstanceInfo
 * Interface class used to retrieve information from the Monitor
 * Rest server will convert these to proper output format
 * including JSON.
 */
public class InstanceInfo {

    private String instanceName = null;
    private InstanceMXBean.Status instanceStatus = InstanceMXBean.Status.UNKNOWN;
    private Long instanceStartTime = null;

    @JsonIgnore
    private boolean instanceAvailable = false;

    @JsonIgnore
    private boolean instanceExists = false;

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public InstanceMXBean.Status getInstanceStatus() {
        return instanceStatus;
    }

    @JsonGetter("instanceStatus")
    public String getInstanceStatusString() {
        return instanceStatus.toString();
    }

    public void setInstanceStatus(InstanceMXBean.Status instanceStatus) {
        this.instanceStatus = instanceStatus;
    }

    public Long getInstanceStartTime() {
        return instanceStartTime;
    }

    public void setInstanceStartTime(Long instanceStartTime) {
        this.instanceStartTime = instanceStartTime;
    }

    public boolean isInstanceAvailable() {
        return instanceAvailable;
    }

    public void setInstanceAvailable(boolean instanceAvailable) {
        this.instanceAvailable = instanceAvailable;
    }

    public boolean isInstanceExists() {
        return instanceExists;
    }

    public void setInstanceExists(boolean instanceExists) {
        this.instanceExists = instanceExists;
    }
    
    public void close(){}
}
