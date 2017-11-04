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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.WebApplicationException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import streams.metric.exporter.error.StreamsTrackerException;
import streams.metric.exporter.streamstracker.StreamsInstanceTracker;
import streams.metric.exporter.streamstracker.job.JobInfo;

public class JobsResource {
    // If instance does not exist, then returns 404
    // If it exists and is not working, then return empty list with count 0
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllJobs() throws StreamsTrackerException,
            WebApplicationException, JsonProcessingException {
        Response r = null;
        ArrayList<JobInfo> jia = null;
        ObjectMapper om = new ObjectMapper();

        StreamsInstanceTracker jobTracker = StreamsInstanceTracker
                .getInstance();

        jia = jobTracker.getAllJobInfo();

        Map<String, Object> m = new HashMap<String, Object>();
        m.put("total", new Integer(jia.size()));
        m.put("jobs", jia);

        r = Response.status(Response.Status.OK)
                .entity(om.writeValueAsString(m)).build();

        return r;
    }

    @Path("{id}")
    public JobResource getJob(@PathParam("id") int id)
            throws StreamsTrackerException, WebApplicationException {
        Response r = null;
        JobInfo ji = null;

        StreamsInstanceTracker jobTracker = StreamsInstanceTracker
                .getInstance();

        ji = jobTracker.getJobInfo(id);

        return new JobResource(ji);

    }
}
