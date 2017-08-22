package streams.jmx.ws.rest.resources;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import streams.jmx.ws.monitor.JobInfo;
import streams.jmx.ws.monitor.StreamsInstanceJobMonitor;
import streams.jmx.ws.monitor.StreamsMonitorException;

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
    public Response getJobStatus() throws StreamsMonitorException,
            WebApplicationException, JsonProcessingException {
        ObjectMapper om = new ObjectMapper();

        Map<String, String> m = new HashMap<String, String>();
        m.put("status", ji.getStatus().toString());

        return Response.status(200).entity(om.writeValueAsString(m)).build();
    }

    @Path("health")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJobHealth() throws StreamsMonitorException,
            WebApplicationException, JsonProcessingException {
        ObjectMapper om = new ObjectMapper();

        Map<String, String> m = new HashMap<String, String>();
        m.put("health", ji.getHealth().toString());

        return Response.status(200).entity(om.writeValueAsString(m)).build();

    }

    @Path("snapshot")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJobSnapshot(
            @DefaultValue("1") @QueryParam("depth") int maximumDepth,
            @DefaultValue("true") @QueryParam("static") boolean includeStaticAttributes)
            throws StreamsMonitorException, WebApplicationException {

        StreamsInstanceJobMonitor jobTracker = StreamsInstanceJobMonitor
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
    public Response getJobMetrics() throws StreamsMonitorException,
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
}
