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



import java.io.IOException;
import java.io.StringWriter;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;

import io.prometheus.client.CollectorRegistry;
import streams.metric.exporter.error.StreamsTrackerException;
import streams.metric.exporter.streamstracker.StreamsDomainTracker;

@Path("/prometheus")
public class PrometheusResource {
    private static final Logger LOGGER = LoggerFactory.getLogger("root."
            + PrometheusResource.class.getName());
    
    public PrometheusResource() {
    }

    // if Instance does not exist, returns 404
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getDomainInfo() throws IOException,
            WebApplicationException,
    		StreamsTrackerException{
    	
    	// At this time, if the auto-refresh is turned off, the call to getInstance() will cause the refresh() to occur.
        StreamsDomainTracker jobTracker = StreamsDomainTracker
                .getDomainTracker();   
        
        LOGGER.trace("/prometheus endpoint handler: domainAvailable={}",jobTracker.isDomainAvailable());
        
        // Create streams_exporter_metrics_available and streams_exporter_instance_available
        
    	StringWriter writer = new StringWriter();
    	
    	io.prometheus.client.exporter.common.TextFormat.write004(writer, CollectorRegistry.defaultRegistry.metricFamilySamples());

        return Response.status(200).entity(writer.toString())
                .build();
    }
}
