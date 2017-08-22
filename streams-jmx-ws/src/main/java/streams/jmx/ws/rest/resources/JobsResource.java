package streams.jmx.ws.rest.resources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import streams.jmx.ws.monitor.JobInfo;
import streams.jmx.ws.monitor.StreamsInstanceJobMonitor;
import streams.jmx.ws.monitor.StreamsMonitorException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.WebApplicationException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JobsResource {
    // If instance does not exist, then returns 404
    // If it exists and is not working, then return empty list with count 0
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllJobs() throws StreamsMonitorException,
            WebApplicationException, JsonProcessingException {
        Response r = null;
        ArrayList<JobInfo> jia = null;
        ObjectMapper om = new ObjectMapper();

        StreamsInstanceJobMonitor jobTracker = StreamsInstanceJobMonitor
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
            throws StreamsMonitorException, WebApplicationException {
        Response r = null;
        JobInfo ji = null;

        StreamsInstanceJobMonitor jobTracker = StreamsInstanceJobMonitor
                .getInstance();

        ji = jobTracker.getJobInfo(id);

        return new JobResource(ji);

    }
}
