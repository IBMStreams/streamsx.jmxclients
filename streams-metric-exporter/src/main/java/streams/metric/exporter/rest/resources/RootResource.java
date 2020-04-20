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

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.module.SimpleModule;

import streams.metric.exporter.Version;
import streams.metric.exporter.error.StreamsTrackerException;
import streams.metric.exporter.rest.serializers.StreamsInstanceTrackerSerializer;
import streams.metric.exporter.streamstracker.StreamsInstanceTracker;

import com.fasterxml.jackson.databind.ObjectMapper;

@Path("/")
public class RootResource {

    private SimpleModule instanceTrackerModule = new SimpleModule("InstanceTrackerModule");

    @Context
    UriInfo uriInfo;

    public RootResource() {
        instanceTrackerModule.addSerializer(StreamsInstanceTracker.class, new StreamsInstanceTrackerSerializer());
    }
    
    // Default page
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getRoot() throws WebApplicationException {
    	return Response.status(Response.Status.OK).entity("try using the /streamsexporter or /prometheus url").build();
    }

    // if Instance does not exist, returns 404
    @Path("instance")
    public InstanceResource getInstance() throws StreamsTrackerException, JsonProcessingException,
            WebApplicationException {   
        StreamsInstanceTracker instanceTracker = StreamsInstanceTracker.getInstanceTracker();
    	return new InstanceResource(instanceTracker);
    		
    }


    @Path("config")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConfiguration() throws StreamsTrackerException,
            WebApplicationException {
        StreamsInstanceTracker instanceTracker = StreamsInstanceTracker
                .getInstanceTracker();

        return Response.status(200).entity(instanceTracker.getConfig())
                .build();
    }    

    @Path("version")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVersion() throws StreamsTrackerException,
            WebApplicationException, JsonProcessingException {

        ObjectMapper om = new ObjectMapper();
        Map<String,String> versionMap = new HashMap<String,String>();
        versionMap.put("title",Version.getImplementationTitle());
        versionMap.put("version",Version.getImplementationVersion());

        return Response.status(200).entity(om.writeValueAsString(versionMap))
                .build();
    }    


    // Internal debugging resource
    @Path("/{parameter: jobtracker|streamsexporter}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response jobtracker() throws StreamsTrackerException,
            JsonProcessingException {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(instanceTrackerModule);
        StreamsInstanceTracker instanceTracker = StreamsInstanceTracker
                .getInstanceTracker();

        String instanceTrackerJson = om.writeValueAsString(instanceTracker);
        return Response.status(Response.Status.OK)
                .entity(instanceTrackerJson).build();
    }
}
