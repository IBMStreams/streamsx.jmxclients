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

import java.text.Format;
import java.util.Date;
import java.text.SimpleDateFormat;
import com.ibm.streams.management.instance.InstanceMXBean;
import com.fasterxml.jackson.annotation.JsonGetter;

/* 
 * InstanceInfo
 * Interface class used to retrieve information from the Tracker
 * Rest server will convert these to proper output format
 * including JSON.
 */
public class InstanceInfo {

    private String instanceName = null;
    private boolean instanceExists = false;
    private boolean instanceAvailable = false;

    private InstanceMXBean.Status instanceStatus = InstanceMXBean.Status.UNKNOWN;
    private InstanceMXBean.JobHealthSummary instanceHealth = InstanceMXBean.JobHealthSummary.UNKNOWN;
    private Long instanceStartTime = null;
    private Long instanceCreationTime = null;



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

    public InstanceMXBean.JobHealthSummary getInstanceHealth() {
        return instanceHealth;
    }

    @JsonGetter("instanceHealth")
    public String getInstanceHealthString() {
        return instanceHealth.toString();
    }

    public void setInstanceHealth(InstanceMXBean.JobHealthSummary instanceHealth) {
        this.instanceHealth = instanceHealth;
    }


    public Long getInstanceStartTime() {
        return instanceStartTime;
    }

    public void setInstanceStartTime(Long instanceStartTime) {
        this.instanceStartTime = instanceStartTime;
    }

    public Long getInstanceCreationTime() {
        return instanceCreationTime;
    }

    public void setInstanceCreationTime(Long instanceCreationTime) {
        this.instanceCreationTime = instanceCreationTime;
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


    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        String newline = System.getProperty("line.separator");

        result.append("Instance: " + getInstanceName()
                + ", status: " + getInstanceStatusString()
                + ", health: " + getInstanceHealthString());
        result.append(newline);
        result.append("instanceCreationTime: " + convertTime(getInstanceCreationTime())
                + ", instanceStartTime: " + convertTime(getInstanceStartTime()));
        result.append(newline);
        result.append("instanceAvailable:" + isInstanceAvailable());
        result.append(newline);
        result.append("instanceExists:" + isInstanceExists());
        result.append(newline);
        return result.toString();
    }
    

    private String convertTime(Long time) {
        if (time != null) {
            Date date = new Date(time);
            Format format = new SimpleDateFormat("yyyy MM dd HH:mm:ss");
            return format.format(date);
        } else {
            return "null";
        }
    }

}
