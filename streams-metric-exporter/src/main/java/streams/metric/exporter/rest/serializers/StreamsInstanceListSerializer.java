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
import java.util.Iterator;
import java.util.Map;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import streams.metric.exporter.streamstracker.StreamsDomainTracker;
import streams.metric.exporter.streamstracker.instance.InstanceInfo;
import streams.metric.exporter.streamstracker.instance.StreamsInstanceTracker;

/**
 * Serializes a StreamsInstanceJobMonitor instance to JSON.
 */
public class StreamsInstanceListSerializer extends JsonSerializer<StreamsDomainTracker> {
	@Override
	public void serialize(StreamsDomainTracker monitor, JsonGenerator jgen, SerializerProvider provider)
			throws IOException, JsonProcessingException {
		jgen.writeStartObject();
		jgen.writeNumberField("total", monitor.getInstanceTrackerMap().size());

		jgen.writeArrayFieldStart("instances");
		Iterator<Map.Entry<String, StreamsInstanceTracker>> iit = monitor.getInstanceTrackerMap().entrySet().iterator();
		while (iit.hasNext()) {
			Map.Entry<String, StreamsInstanceTracker> InstanceEntry = iit.next();
			StreamsInstanceTracker sit = InstanceEntry.getValue();
			// try {
			InstanceInfo instanceInfo = sit.getInstanceInfo();
			jgen.writeObject(instanceInfo);

		} // end loop over instance trackers
		jgen.writeEndArray();
		jgen.writeEndObject();
	}
}
