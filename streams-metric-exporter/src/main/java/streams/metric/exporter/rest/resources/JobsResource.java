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

import streams.metric.exporter.error.StreamsMonitorException;
import streams.metric.exporter.streamstracker.StreamsInstanceTracker;
import streams.metric.exporter.streamstracker.job.JobInfo;

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
            throws StreamsMonitorException, WebApplicationException {
        Response r = null;
        JobInfo ji = null;

        StreamsInstanceTracker jobTracker = StreamsInstanceTracker
                .getInstance();

        ji = jobTracker.getJobInfo(id);

        return new JobResource(ji);

    }
}
