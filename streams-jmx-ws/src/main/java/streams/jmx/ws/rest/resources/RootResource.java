package streams.jmx.ws.rest.resources;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import streams.jmx.ws.monitor.JobInfo;
import streams.jmx.ws.monitor.StreamsInstanceJobMonitor;
import streams.jmx.ws.monitor.StreamsMonitorException;

import streams.jmx.ws.rest.serializers.StreamsInstanceJobMonitorSerializer;

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
import com.fasterxml.jackson.databind.ObjectMapper;

@Path("/")
public class RootResource {

    private SimpleModule jobTrackerModule = new SimpleModule("JobTrackerModule");

    @Context
    UriInfo uriInfo;

    public RootResource() {
        jobTrackerModule.addSerializer(StreamsInstanceJobMonitor.class, new StreamsInstanceJobMonitorSerializer());
    }

    // if Instance does not exist, returns 404
    @Path("instance")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getInstanceInfo() throws StreamsMonitorException,
            WebApplicationException {
        StreamsInstanceJobMonitor jobTracker = StreamsInstanceJobMonitor
                .getInstance();

        return Response.status(200).entity(jobTracker.getInstanceInfo())
                .build();
    }

    @Path("instance/resourceMetrics")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getInstanceResourceMetrics() throws StreamsMonitorException, WebApplicationException, JsonProcessingException {
        StreamsInstanceJobMonitor jobTracker = StreamsInstanceJobMonitor.getInstance();
        ObjectMapper om = new ObjectMapper();

        return Response.status(Response.Status.OK).entity(
                   om.writeValueAsString(jobTracker.getInstanceResourceMetrics())).build();
    }

    // If instance is not started or exists, then returns 404
    @Path("metrics")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllJobMetrics() throws StreamsMonitorException,
            WebApplicationException {
        StreamsInstanceJobMonitor jobTracker = StreamsInstanceJobMonitor
                .getInstance();

        return Response.status(200).entity(jobTracker.getAllJobMetrics())
                .build();
    }

    @Path("joblist/")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJobList() throws StreamsMonitorException,
            WebApplicationException, JsonProcessingException {
        Response r = null;
        ArrayList<JobInfo> jia = null;
        ObjectMapper om = new ObjectMapper();

        StreamsInstanceJobMonitor jobTracker = StreamsInstanceJobMonitor
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
    public JobsResource getJobs() throws StreamsMonitorException,
            WebApplicationException, JsonProcessingException {

        return new JobsResource();
    }

    // Internal debugging resource
    @Path("jobtracker")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response jobtracker() throws StreamsMonitorException,
            JsonProcessingException {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(jobTrackerModule);
        StreamsInstanceJobMonitor jobTracker = StreamsInstanceJobMonitor
                .getInstance();

        return Response.status(Response.Status.OK)
                .entity(om.writeValueAsString(jobTracker)).build();
    }
}
