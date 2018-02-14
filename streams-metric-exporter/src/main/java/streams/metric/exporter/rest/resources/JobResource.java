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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.WebApplicationException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.ObjectMapper;

import streams.metric.exporter.error.StreamsTrackerException;
import streams.metric.exporter.streamstracker.StreamsInstanceTracker;
import streams.metric.exporter.streamstracker.job.JobInfo;

public class JobResource {

    private JobInfo ji;

    public JobResource(JobInfo ji) {
        this.ji = ji;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJob() {
        // We do not use 304 status because that actually sends last body
        // and we want to be able to see details in the job info that tracks
        // dates of last updates

        return Response.status(Response.Status.OK).entity(ji).build();
    }

    @Path("status")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJobStatus() throws StreamsTrackerException,
            WebApplicationException, JsonProcessingException {
        ObjectMapper om = new ObjectMapper();

        Map<String, String> m = new HashMap<String, String>();
        m.put("status", ji.getStatus().toString());

        return Response.status(200).entity(om.writeValueAsString(m)).build();
    }

    @Path("health")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJobHealth() throws StreamsTrackerException,
            WebApplicationException, JsonProcessingException {
        ObjectMapper om = new ObjectMapper();

        Map<String, String> m = new HashMap<String, String>();
        m.put("health", ji.getHealth().toString());

        return Response.status(200).entity(om.writeValueAsString(m)).build();

    }

    @Path("snapshotnow")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJobSnapshotNow(
            @DefaultValue("1") @QueryParam("depth") int maximumDepth,
            @DefaultValue("true") @QueryParam("static") boolean includeStaticAttributes)
            throws StreamsTrackerException, WebApplicationException {

        StreamsInstanceTracker jobTracker = StreamsInstanceTracker
                .getInstance();

        String snapshot = null;
        snapshot = jobTracker.getJobSnapshot(ji.getId().intValue(),
                maximumDepth, includeStaticAttributes);

        if (snapshot == null) {
            throw new WebApplicationException("Job " + ji.getId()
                    + " returned an empty snapshot.",
                    Response.Status.NO_CONTENT); // 204
        }

        return Response.status(200).entity(snapshot).build();
    }

    @Path("metrics")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJobMetrics() throws StreamsTrackerException,
            WebApplicationException, JsonProcessingException {
        ObjectMapper om = new ObjectMapper();

        Response r = null;

        // Create return format
        JobMetricsBody body = new JobMetricsBody(ji.getLastMetricsRefresh(),
                ji.getLastMetricsFailure(), ji.isLastMetricsRefreshFailed(),
                ji.getJobMetrics());

        // If the metrics refresh failed, use NOT_MODIFIED so client can
        // understand we are sending cached info
        // More cached than it usually is :)
        if (ji.isLastMetricsRefreshFailed()) {
            r = Response.status(Response.Status.NOT_MODIFIED)
                    .entity(om.writeValueAsString(body)).build();
        } else {
            r = Response.status(Response.Status.OK)
                    .entity(om.writeValueAsString(body)).build();
        }
        return r;

    }
    
    @Path("snapshot")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJobSnapshot() throws StreamsTrackerException,
            WebApplicationException, JsonProcessingException {
        ObjectMapper om = new ObjectMapper();

        Response r = null;

        // Create return format
        JobSnapshotBody body = new JobSnapshotBody(ji.getLastSnapshotRefresh(),
                ji.getLastSnapshotFailure(), ji.isLastSnapshotRefreshFailed(),
                ji.getJobSnapshot());

        // If the metrics refresh failed, use NOT_MODIFIED so client can
        // understand we are sending cached info
        // More cached than it usually is :)
        if (ji.isLastSnapshotRefreshFailed()) {
            r = Response.status(Response.Status.NOT_MODIFIED)
                    .entity(om.writeValueAsString(body)).build();
        } else {
            r = Response.status(Response.Status.OK)
                    .entity(om.writeValueAsString(body)).build();
        }
        return r;

    }    

    /******** SUPPORTING CLASSES FOR OUTPUT FORMATTING ********/
    private class JobMetricsBody {
        public Date lastMetricsRefresh = null;
        public Date lastMetricsFailure = null;
        public boolean lastMetricsRefreshFailed = false;
        @JsonRawValue
        public String jobMetrics;

        public JobMetricsBody(Date lastMetricsRefresh, Date lastMetricsFailure,
                boolean lastMetricsRefreshFailed, String jobMetrics) {
            this.lastMetricsRefresh = lastMetricsRefresh;
            this.lastMetricsFailure = lastMetricsFailure;
            this.lastMetricsRefreshFailed = lastMetricsRefreshFailed;
            this.jobMetrics = jobMetrics;
        }
    }
    
    /******** SUPPORTING CLASSES FOR OUTPUT FORMATTING ********/
    private class JobSnapshotBody {
        public Date lastSnapshotRefresh = null;
        public Date lastSnapshotFailure = null;
        public boolean lastSnapshotRefreshFailed = false;
        @JsonRawValue
        public String jobSnapshot;

        public JobSnapshotBody(Date lastSnapshotRefresh, Date lastSnapshotFailure,
                boolean lastSnapshotRefreshFailed, String jobSnapshot) {
            this.lastSnapshotRefresh = lastSnapshotRefresh;
            this.lastSnapshotFailure = lastSnapshotFailure;
            this.lastSnapshotRefreshFailed = lastSnapshotRefreshFailed;
            this.jobSnapshot = jobSnapshot;
        }
    }
}
