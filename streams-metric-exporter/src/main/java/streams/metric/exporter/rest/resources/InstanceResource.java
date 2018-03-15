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

import java.util.List;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.WebApplicationException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.ObjectMapper;

import streams.metric.exporter.error.StreamsTrackerException;
import streams.metric.exporter.streamstracker.instance.StreamsInstanceTracker;
import streams.metric.exporter.streamstracker.job.JobInfo;
import streams.metric.exporter.streamstracker.metrics.AllJobMetrics;

public class InstanceResource {

	private StreamsInstanceTracker sit;

	public InstanceResource(StreamsInstanceTracker sit) {
		this.sit = sit;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getInstanceTracker() {
		// We do not use 304 status because that actually sends last body
		// and we want to be able to see details in the job info that tracks
		// dates of last updates

		return Response.status(Response.Status.OK).entity(sit.getInstanceInfo()).build();
	}

	@Path("status")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getJobStatus() throws StreamsTrackerException, WebApplicationException, JsonProcessingException {
		ObjectMapper om = new ObjectMapper();

		Map<String, String> m = new HashMap<String, String>();
		m.put("status", sit.getInstanceInfo().getInstanceStatusString());

		return Response.status(200).entity(om.writeValueAsString(m)).build();
	}

	@Path("metrics")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getJobMetrics() throws StreamsTrackerException, WebApplicationException, JsonProcessingException {
		ObjectMapper om = new ObjectMapper();

		Response r = null;

		AllJobMetrics ajm = sit.getAllJobMetrics();

		// Create return format
		InstanceMetricsBody body = new InstanceMetricsBody(ajm.getLastMetricsRefresh(), ajm.getLastMetricsFailure(),
				ajm.isLastMetricsRefreshFailed(), ajm.getAllMetrics());

		// If the metrics refresh failed, use NOT_MODIFIED so client can
		// understand we are sending cached info
		// More cached than it usually is :)
		if (ajm.isLastMetricsRefreshFailed()) {
			r = Response.status(Response.Status.NOT_MODIFIED).entity(om.writeValueAsString(body)).build();
		} else {
			r = Response.status(Response.Status.OK).entity(om.writeValueAsString(body)).build();
		}
		return r;

	}

	@Path("jobs/")
	public JobsResource getJobs() throws StreamsTrackerException, WebApplicationException, JsonProcessingException {

		return new JobsResource(sit);
	}

	/******** SUPPORTING CLASSES FOR OUTPUT FORMATTING ********/
	private class InstanceMetricsBody {
		@SuppressWarnings("unused")
		public Date lastMetricsRefresh = null;
		@SuppressWarnings("unused")
		public Date lastMetricsFailure = null;
		@SuppressWarnings("unused")
		public boolean lastMetricsRefreshFailed = false;
		@JsonRawValue
		public String instanceMetrics;

		public InstanceMetricsBody(Date lastMetricsRefresh, Date lastMetricsFailure, boolean lastMetricsRefreshFailed,
				String instanceMetrics) {
			this.lastMetricsRefresh = lastMetricsRefresh;
			this.lastMetricsFailure = lastMetricsFailure;
			this.lastMetricsRefreshFailed = lastMetricsRefreshFailed;
			this.instanceMetrics = instanceMetrics;
		}
	}
	
  @Path("joblist/")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getJobList(@Context UriInfo uriInfo) throws StreamsTrackerException,
          WebApplicationException, JsonProcessingException {
      Response r = null;
      ArrayList<JobInfo> jia = null;
      ObjectMapper om = new ObjectMapper();

      jia = sit.getAllJobInfo();

      Map<String, Object> m = new HashMap<String, Object>();

      m.put("total", new Integer(jia.size()));

      ArrayList<Object> jlist = new ArrayList<Object>();

      for (JobInfo curJob : jia) {
          Map<String, Object> j = new HashMap<String, Object>();
          j.put("id", curJob.getId());
          j.put("name", curJob.getName());

          //UriBuilder jub = uriInfo.getBaseUriBuilder();
          UriBuilder jub = uriInfo.getAbsolutePathBuilder();
          
          List<PathSegment> ps = uriInfo.getPathSegments();
          System.out.println("PathSegment: " + ps.toString());
          StringBuilder newPath = new StringBuilder();
          if (ps.size() > 0) {
	          for (int i = 0;i<ps.size() - 1;i++) {
	        	  	newPath.append(ps.get(i).getPath());
	        	  	newPath.append("/");
	          }
          }
          
          jub.replacePath(newPath.toString());
          
          URI jobUri = jub.path("jobs").path(String.valueOf(curJob.getId()))
                  .build();
          j.put("jobInfo", jobUri.toASCIIString());

          UriBuilder mub = jub.clone();
          URI metricsUri = mub.path("metrics").build();
          j.put("metrics", metricsUri.toASCIIString());
          
          UriBuilder sub = jub.clone();
          URI snapshotUri = sub.path("snapshot").build();
          j.put("snapshot", snapshotUri.toASCIIString());

          UriBuilder snub = jub.clone();
          URI snapshotNowUri = snub.path("snapshotnow").build();
          j.put("snapshotnow", snapshotNowUri.toASCIIString());

          jlist.add(j);
      }

      m.put("jobs", jlist);

      r = Response.status(Response.Status.OK)
              .entity(om.writeValueAsString(m)).build();

      return r;

  }


}
