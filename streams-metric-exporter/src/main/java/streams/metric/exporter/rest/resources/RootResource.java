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
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.WebApplicationException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.module.SimpleModule;

import streams.metric.exporter.error.StreamsTrackerException;
import streams.metric.exporter.rest.serializers.StreamsInstanceDomainTrackerSerializer;
import streams.metric.exporter.streamstracker.StreamsDomainTracker;

import com.fasterxml.jackson.databind.ObjectMapper;

@Path("/")
public class RootResource {

    private SimpleModule domainTrackerModule = new SimpleModule("JobTrackerModule");

    @Context
    UriInfo uriInfo;

    public RootResource() {
        domainTrackerModule.addSerializer(StreamsDomainTracker.class, new StreamsInstanceDomainTrackerSerializer());
    }
    
    // Default page
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getRoot() throws WebApplicationException {
    	return Response.status(Response.Status.OK).entity("try using the /jobtracker or /prometheus url").build();
    }

    // if Domain does not exist, returns 404
    @Path("domain")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDomainInfo() throws StreamsTrackerException,
            WebApplicationException {
        StreamsDomainTracker domainTracker = StreamsDomainTracker
                .getDomainTracker();

        return Response.status(200).entity(domainTracker.getDomainInfo())
                .build();
    }    
    
    // if Instance does not exist, returns 404
    @Path("instances/")
    public InstancesResource getInstances() throws StreamsTrackerException, JsonProcessingException,
            WebApplicationException {   
    	
    		return new InstancesResource();
    		
    }


    // Internal debugging resource
    @Path("/{parameter: jobtracker|streamsexporter}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response jobtracker() throws StreamsTrackerException,
            JsonProcessingException {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(domainTrackerModule);
        StreamsDomainTracker domainTracker = StreamsDomainTracker
                .getDomainTracker();

        String domainTrackerJson = om.writeValueAsString(domainTracker);
        return Response.status(Response.Status.OK)
                .entity(domainTrackerJson).build();
    }
}
