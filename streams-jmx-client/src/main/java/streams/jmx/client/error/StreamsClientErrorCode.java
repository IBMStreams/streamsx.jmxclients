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

package streams.jmx.client.error;

public enum StreamsClientErrorCode {
    UNSPECIFIED_ERROR(0, "Unspecified Error Code"),
    JMX_MALFORMED_URL(1,
            "A JMX Malformed URL error has occured."),
    JMX_IOERROR(2,
            "A JMX IO Error has occured.  Usually means that the connection has been lost."),

    DOMAIN_NOT_FOUND(10,
            "The specified domain name is not found on the Streams JMX Server specified"), 
    INSTANCE_NOT_FOUND(
            11,
            "The specified streams instance was not found in the Streams domain"), 
    JOB_NOT_FOUND(
            12, "Specified Streams job was not found in the Streams instance"),

    STREAMS_MONITOR_UNAVAILABLE(50,
            "The Streams Monitor has not been created and initialized."), 
    ALL_JOBS_NOT_AVAILABLE(
            51,
            "The Streams jobs are not available at this time.  The Streams instance does not exist."), 
    ALL_METRICS_NOT_AVAILABLE(
            52,
            "The Metrics for all jobs is not available at this time.  Either the JMX Connection or the Instance is not avaiable."), 
    ALL_SNAPSHOTS_NOT_AVAILABLE(53,
    "The Snapshots for all jobs is not available at thjis time.  Either the JMX Connection or the Instance is not available."),

    OTHER_ERROR(99, "Unspecified Error Code");

    private final int code;
    private final String description;

    private StreamsClientErrorCode(int code, String description) {
        this.code = code;
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
        return code + ":" + description;
    }
}
