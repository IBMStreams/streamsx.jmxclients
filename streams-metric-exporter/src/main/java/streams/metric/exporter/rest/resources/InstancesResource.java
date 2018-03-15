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

package streams.metric.exporter.rest.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.WebApplicationException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import streams.metric.exporter.error.StreamsTrackerException;
import streams.metric.exporter.rest.serializers.StreamsInstanceListSerializer;
import streams.metric.exporter.streamstracker.StreamsDomainTracker;
import streams.metric.exporter.streamstracker.instance.StreamsInstanceTracker;

public class InstancesResource {
	
    private SimpleModule instancesModule = new SimpleModule("InstancesModule");

    public InstancesResource() {
        instancesModule.addSerializer(StreamsDomainTracker.class, new StreamsInstanceListSerializer());
    }
    // If instance does not exist, then returns 404
    // If it exists and is not working, then return empty list with count 0
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllInstances() throws StreamsTrackerException,
            WebApplicationException, JsonProcessingException {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(instancesModule);
        StreamsDomainTracker jobTracker = StreamsDomainTracker
                .getDomainTracker();

        return Response.status(Response.Status.OK)
                .entity(om.writeValueAsString(jobTracker)).build();
    }

    @Path("{instanceName}")
    public InstanceResource getInstance(@PathParam("instanceName") String instanceName)
            throws StreamsTrackerException, WebApplicationException {
        StreamsInstanceTracker sit = null;

        StreamsDomainTracker domainTracker = StreamsDomainTracker
                .getDomainTracker();
        sit = domainTracker.getInstanceTracker(instanceName);

        return new InstanceResource(sit);

    }
}
