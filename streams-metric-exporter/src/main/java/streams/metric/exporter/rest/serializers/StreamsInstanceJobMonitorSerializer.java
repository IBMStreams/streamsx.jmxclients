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

package streams.metric.exporter.rest.serializers;

import java.io.IOException;

import java.math.BigInteger;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import java.text.SimpleDateFormat;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import streams.metric.exporter.error.StreamsTrackerException;
import streams.metric.exporter.streamstracker.StreamsDomainTracker;
import streams.metric.exporter.streamstracker.instance.InstanceInfo;
import streams.metric.exporter.streamstracker.job.JobInfo;

/**
 * Serializes a StreamsInstanceJobMonitor instance to JSON.
 */
public class StreamsInstanceJobMonitorSerializer extends
        JsonSerializer<StreamsDomainTracker> {
    @Override
    public void serialize(StreamsDomainTracker monitor,
            JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException {
        jgen.writeStartObject();
        jgen.writeStringField("domain", monitor.getDomainName());
        jgen.writeObjectFieldStart("instance");

        try {
            InstanceInfo instanceInfo = monitor.getInstanceInfo();
            jgen.writeBooleanField("available",
                    instanceInfo.isInstanceAvailable());
            jgen.writeStringField("name", instanceInfo.getInstanceName());
            jgen.writeStringField("status", instanceInfo.getInstanceStatus()
                    .toString());
            jgen.writeStringField("instanceStartTime",
                    convertTime(instanceInfo.getInstanceStartTime()));
            jgen.writeEndObject();
        } catch (StreamsTrackerException e) {
            throw new IOException(e);
        }

        jgen.writeBooleanField("jobMapAvailable", monitor.jobsAvailable());
        jgen.writeBooleanField("jobMetricsAvailable",
                monitor.metricsAvailable());
        jgen.writeStringField("instanceResourceMetricsLastUpdateTime", convertTime(monitor.getInstanceResourceMetricsLastUpdated()));
        jgen.writeBooleanField("jobSnapshotsAvailable",
                monitor.snapshotsAvailable());
        
        if (monitor.jobsAvailable()) {
            Iterator<Map.Entry<BigInteger, JobInfo>> it = monitor
                    .getCurrentJobMap().entrySet().iterator();
            jgen.writeNumberField("jobCount", monitor.getCurrentJobMap().size());
            jgen.writeArrayFieldStart("jobMap");
            while (it.hasNext()) {
                Map.Entry<BigInteger, JobInfo> entry = it.next();
                //jgen.writeObjectFieldStart("mapEntry");
                jgen.writeStartObject();
                jgen.writeObjectFieldStart("jobInfo");

                jgen.writeStringField("id", entry.getValue().getId().toString());
                jgen.writeStringField("status", entry.getValue().getStatus()
                        .toString());
                jgen.writeStringField("applicationName", entry.getValue()
                        .getApplicationName());
//                jgen.writeStringField("metrics", entry.getValue()
//                        .getJobMetrics());
//                jgen.writeStringField("snapshot", entry.getValue()
//                        .getJobSnapshot());
                jgen.writeFieldName("metrics");
                jgen.writeRawValue(entry.getValue().getJobMetrics());
                jgen.writeFieldName("snapshot");
                jgen.writeRawValue(entry.getValue().getJobSnapshot());
                        
                jgen.writeEndObject();
                jgen.writeEndObject();
            }

            //jgen.writeEndObject();
            jgen.writeEndArray();
        }

        if (monitor.metricsAvailable()) {
            jgen.writeArrayFieldStart("jobNameIndex");

            Iterator<Map.Entry<String, BigInteger>> it = monitor
                    .getCurrentJobNameIndex().entrySet().iterator();

            while (it.hasNext()) {
                Map.Entry<String, BigInteger> entry = it.next();
                //jgen.writeObjectFieldStart("mapEntry");
                jgen.writeStartObject();
                jgen.writeStringField("key", entry.getKey().toString());
                jgen.writeStringField("value", entry.getValue().toString());
                jgen.writeEndObject();
            }

            jgen.writeEndArray();
        }
    }

    private String convertTime(Long time) {
        if (time != null) {
            Date date = new Date(time);
            SimpleDateFormat format = new SimpleDateFormat(
                    "yyyy MM dd HH:mm:ss");

            return format.format(date);
        } else {
            return "null";
        }
    }
}
