package streams.jmx.ws.rest.serializers;

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

import streams.jmx.ws.monitor.InstanceInfo;
import streams.jmx.ws.monitor.JobInfo;
import streams.jmx.ws.monitor.StreamsInstanceJobMonitor;
import streams.jmx.ws.monitor.StreamsMonitorException;

/**
 * Serializes a StreamsInstanceJobMonitor instance to JSON.
 */
public class StreamsInstanceJobMonitorSerializer extends
        JsonSerializer<StreamsInstanceJobMonitor> {
    @Override
    public void serialize(StreamsInstanceJobMonitor monitor,
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
        } catch (StreamsMonitorException e) {
            throw new IOException(e);
        }

        jgen.writeBooleanField("jobMapAvailable", monitor.jobsAvailable());
        jgen.writeBooleanField("jobMetricsAvailable",
                monitor.metricsAvailable());
        jgen.writeStringField("instanceResourceMetricsLastUpdateTime", convertTime(monitor.getInstanceResourceMetricsLastUpdated()));

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
                jgen.writeStringField("metrics", entry.getValue()
                        .getJobMetrics());
                        
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
