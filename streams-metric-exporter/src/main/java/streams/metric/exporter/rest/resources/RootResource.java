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

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.WebApplicationException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.module.SimpleModule;

import streams.metric.exporter.error.StreamsTrackerException;
import streams.metric.exporter.rest.serializers.StreamsInstanceJobMonitorSerializer;
import streams.metric.exporter.streamstracker.StreamsInstanceTracker;
import streams.metric.exporter.streamstracker.job.JobInfo;

import com.fasterxml.jackson.databind.ObjectMapper;

@Path("/")
public class RootResource {

    private SimpleModule jobTrackerModule = new SimpleModule("JobTrackerModule");

    @Context
    UriInfo uriInfo;

    public RootResource() {
        jobTrackerModule.addSerializer(StreamsInstanceTracker.class, new StreamsInstanceJobMonitorSerializer());
    }

    // if Instance does not exist, returns 404
    @Path("instance")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getInstanceInfo() throws StreamsTrackerException,
            WebApplicationException {
        StreamsInstanceTracker jobTracker = StreamsInstanceTracker
                .getInstance();

        return Response.status(200).entity(jobTracker.getInstanceInfo())
                .build();
    }

    @Path("instance/resourceMetrics")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getInstanceResourceMetrics() throws StreamsTrackerException, WebApplicationException, JsonProcessingException {
        StreamsInstanceTracker jobTracker = StreamsInstanceTracker.getInstance();
        ObjectMapper om = new ObjectMapper();

        return Response.status(Response.Status.OK).entity(
                   om.writeValueAsString(jobTracker.getInstanceResourceMetrics())).build();
    }

    // If instance is not started or exists, then returns 404
    @Path("metrics")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllJobMetrics() throws StreamsTrackerException,
            WebApplicationException {
        StreamsInstanceTracker jobTracker = StreamsInstanceTracker
                .getInstance();

        return Response.status(200).entity(jobTracker.getAllJobMetrics())
                .build();
    }

    @Path("joblist/")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJobList() throws StreamsTrackerException,
            WebApplicationException, JsonProcessingException {
        Response r = null;
        ArrayList<JobInfo> jia = null;
        ObjectMapper om = new ObjectMapper();

        StreamsInstanceTracker jobTracker = StreamsInstanceTracker
                .getInstance();

        jia = jobTracker.getAllJobInfo();

        Map<String, Object> m = new HashMap<String, Object>();

        m.put("total", new Integer(jia.size()));

        ArrayList<Object> jlist = new ArrayList<Object>();

        for (JobInfo curJob : jia) {
            Map<String, Object> j = new HashMap<String, Object>();
            j.put("id", curJob.getId());
            j.put("name", curJob.getName());

            UriBuilder jub = uriInfo.getBaseUriBuilder();
            URI jobUri = jub.path("jobs").path(String.valueOf(curJob.getId()))
                    .build();
            j.put("jobInfo", jobUri.toASCIIString());

            UriBuilder mub = jub.clone();
            URI metricsUri = mub.path("metrics").build();
            j.put("metrics", metricsUri.toASCIIString());

            UriBuilder sub = jub.clone();
            URI snapshotUri = sub.path("snapshot").build();
            j.put("snapshot", snapshotUri.toASCIIString());

            jlist.add(j);
        }

        m.put("jobs", jlist);

        r = Response.status(Response.Status.OK)
                .entity(om.writeValueAsString(m)).build();

        return r;

    }

    // If instance does not exist, then returns 404
    // If it exists and is not working, then return empty list with count 0
    @Path("jobs/")
    public JobsResource getJobs() throws StreamsTrackerException,
            WebApplicationException, JsonProcessingException {

        return new JobsResource();
    }

    
    // Internal debugging resource
    @Path("jobtracker")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response jobtracker() throws StreamsTrackerException,
            JsonProcessingException {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(jobTrackerModule);
        StreamsInstanceTracker jobTracker = StreamsInstanceTracker
                .getInstance();

        return Response.status(Response.Status.OK)
                .entity(om.writeValueAsString(jobTracker)).build();
    }
}
