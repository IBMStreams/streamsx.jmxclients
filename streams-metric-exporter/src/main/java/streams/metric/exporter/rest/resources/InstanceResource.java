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
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.WebApplicationException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.ObjectMapper;

import streams.metric.exporter.error.StreamsTrackerException;
import streams.metric.exporter.streamstracker.StreamsInstanceTracker;
import streams.metric.exporter.streamstracker.metrics.AllJobMetrics;
import streams.metric.exporter.streamstracker.snapshots.AllJobSnapshots;

public class InstanceResource {

	private StreamsInstanceTracker sit;

	public InstanceResource(StreamsInstanceTracker sit) {
		this.sit = sit;
	}

	@Path("instance")
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
		InstanceMetricsBody body = new InstanceMetricsBody(sit.getInstanceInfo().getInstanceName(),ajm.getLastMetricsRefresh(), ajm.getLastMetricsFailure(),
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

	@Path("snapshots")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getJobSnapshots() throws StreamsTrackerException, WebApplicationException, JsonProcessingException {
		ObjectMapper om = new ObjectMapper();

		Response r = null;

		AllJobSnapshots ajs = sit.getAllJobSnapshots();

		// Create return format
		InstanceSnapshotsBody body = new InstanceSnapshotsBody(sit.getInstanceInfo().getInstanceName(),ajs.getLastSnaphostRefresh(), ajs.getLastSnapshotFailure(),
				ajs.isLastSnapshotRefreshFailed(), ajs.getAllSnapshots());

		// If the snapshots refresh failed, use NOT_MODIFIED so client can
		// understand we are sending cached info
		// More cached than it usually is :)
		if (ajs.isLastSnapshotRefreshFailed()) {
			r = Response.status(Response.Status.NOT_MODIFIED).entity(om.writeValueAsString(body)).build();
		} else {
			r = Response.status(Response.Status.OK).entity(om.writeValueAsString(body)).build();
		}
		return r;

	}

	@Path("/resourceMetrics")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getInstanceResourceMetrics()
			throws StreamsTrackerException, WebApplicationException, JsonProcessingException {
		ObjectMapper om = new ObjectMapper();

		return Response.status(Response.Status.OK).entity(om.writeValueAsString(sit.getInstanceResourceMetrics()))
				.build();
	}



	/******** SUPPORTING CLASSES FOR OUTPUT FORMATTING ********/
	private class InstanceMetricsBody {
		@SuppressWarnings("unused")
		public String instanceName = null;
		@SuppressWarnings("unused")
		public Date lastMetricsRefresh = null;
		@SuppressWarnings("unused")
		public Date lastMetricsFailure = null;
		@SuppressWarnings("unused")
		public boolean lastMetricsRefreshFailed = false;
		@JsonRawValue
		public String instanceMetrics;

		public InstanceMetricsBody(String instanceName, Date lastMetricsRefresh, Date lastMetricsFailure, boolean lastMetricsRefreshFailed,
				String instanceMetrics) {
			this.instanceName = instanceName;

			this.lastMetricsRefresh = lastMetricsRefresh;
			this.lastMetricsFailure = lastMetricsFailure;
			this.lastMetricsRefreshFailed = lastMetricsRefreshFailed;
			this.instanceMetrics = instanceMetrics;
		}
	}
	/******** SUPPORTING CLASSES FOR OUTPUT FORMATTING ********/
	private class InstanceSnapshotsBody {
		@SuppressWarnings("unused")
		public String instanceName = null;
		@SuppressWarnings("unused")
		public Date lastSnapshotsRefresh = null;
		@SuppressWarnings("unused")
		public Date lastSnapshotsFailure = null;
		@SuppressWarnings("unused")
		public boolean lastSnapshotsRefreshFailed = false;
		@JsonRawValue
		public String instanceSnapshots;

		public InstanceSnapshotsBody(String instanceName, Date lastSnapshotsRefresh, Date lastSnapshotsFailure, boolean lastSnapshotsRefreshFailed,
				String instanceSnapshots) {
			this.instanceName = instanceName;
			this.lastSnapshotsRefresh = lastSnapshotsRefresh;
			this.lastSnapshotsFailure = lastSnapshotsFailure;
			this.lastSnapshotsRefreshFailed = lastSnapshotsRefreshFailed;
			this.instanceSnapshots = instanceSnapshots;
		}
	}
}
