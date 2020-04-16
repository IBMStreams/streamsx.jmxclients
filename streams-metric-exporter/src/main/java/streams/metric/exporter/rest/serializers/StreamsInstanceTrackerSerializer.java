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
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import java.text.SimpleDateFormat;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import streams.metric.exporter.streamstracker.StreamsInstanceTracker;
import streams.metric.exporter.streamstracker.InstanceInfo;

/**
 * Serializes a StreamsInstanceJobMonitor instance to JSON.
 */
public class StreamsInstanceTrackerSerializer extends JsonSerializer<StreamsInstanceTracker> {
	@Override
	public void serialize(StreamsInstanceTracker monitor, JsonGenerator jgen, SerializerProvider provider)
			throws IOException, JsonProcessingException {

		jgen.writeStartObject();
		jgen.writeStringField("instance", monitor.getInstanceInfo().getInstanceName());
		try {
			jgen.writeObjectField("instanceInfo", monitor.getInstanceInfo());
		} catch (Exception e) {
		}
		InstanceInfo instanceInfo = monitor.getInstanceInfo();
		jgen.writeBooleanField("available", instanceInfo.isInstanceAvailable());
		jgen.writeStringField("name", instanceInfo.getInstanceName());
		jgen.writeStringField("status", instanceInfo.getInstanceStatus().toString());
		jgen.writeStringField("instanceStartTime", convertTime(instanceInfo.getInstanceStartTime()));

		jgen.writeStringField("instanceResourceMetricsLastUpdateTime",
				convertTime(monitor.getInstanceResourceMetricsLastUpdated()));

		jgen.writeArrayFieldStart("jobNameIndex");

		Iterator<Map.Entry<String, String>> it = monitor.getCurrentJobNameIndex().entrySet().iterator();

		while (it.hasNext()) {
			Map.Entry<String, String> entry = it.next();
			jgen.writeStartObject();
			jgen.writeStringField("key", entry.getKey().toString());
			jgen.writeStringField("value", entry.getValue().toString());
			jgen.writeEndObject();
		}
		jgen.writeEndArray();
		

		jgen.writeEndObject(); // instances


	}

	private String convertTime(Long time) {
		if (time != null) {
			Date date = new Date(time);
			SimpleDateFormat format = new SimpleDateFormat("yyyy MM dd HH:mm:ss");

			return format.format(date);
		} else {
			return "null";
		}
	}
}
