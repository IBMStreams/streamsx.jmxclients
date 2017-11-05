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

package streams.metric.exporter.rest;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.prometheus.client.exporter.MetricsServlet;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.servlet.ServletRegistration;
import org.glassfish.grizzly.servlet.WebappContext;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.jackson.JacksonFeature;

public class RestServer {
    private static final Logger LOGGER = LoggerFactory.getLogger("root."
            + RestServer.class.getName());

    private String baseUri = null;
    private HttpServer server = null;

    public RestServer(String host, String webPort, String webPath) {
        this.baseUri = "http://" + host + ":" + webPort + ((webPath.length() > 0) && !webPath.startsWith("/")?"/":"") + webPath + (webPath.endsWith("/")?"":"/");

        server = startServer();

        LOGGER.debug("Jersey app started listening on {}.",
                new Object[] { this.baseUri });
    }

//    public HttpServer startServer() {
//
//        String[] packages = { "streams.prometheus.exporter.rest.resources",
//                "streams.prometheus.exporter.rest.errorhandling",
//                "streams.prometheus.exporter.rest.serializers" };
//
//        final ResourceConfig rc = new ResourceConfig().packages(packages);
//        // Enable JSON media conversions
//        rc.register(JacksonFeature.class);
//
//        return GrizzlyHttpServerFactory.createHttpServer(URI.create(baseUri),
//                rc);
//    }

    public HttpServer startServer() {
    	HttpServer theServer;
    	
//        String[] packages = { "streams.prometheus.exporter.rest.resources",
//                "streams.prometheus.exporter.rest.errorhandling",
//                "streams.prometheus.exporter.rest.serializers" };

        //final ResourceConfig rc = new ResourceConfig().packages(packages);
        // Enable JSON media conversions
        //rc.register(JacksonFeature.class);
        
        WebappContext context = new WebappContext("WebappContext","");
        ServletRegistration registration = context.addServlet("ServletContainer", ServletContainer.class);
        registration.setInitParameter("jersey.config.server.provider.packages",
        		"streams.metric.exporter.rest.resources;streams.metric.exporter.rest.errorhandling;streams.metric.exporter.rest.serializers");
        registration.addMapping("/*");
        
        theServer = GrizzlyHttpServerFactory.createHttpServer(URI.create(baseUri));
        
        // Prometheus servlet
        // FUTURE: if we go with plugin this needs to be variant if it is created or not
        ServletRegistration prometheus = context.addServlet("PrometheusContainer",new MetricsServlet());
        prometheus.addMapping("/prometheus");
        
        context.deploy(theServer);

        
        return theServer;
    }
    public void stopServer() {
        server.shutdownNow();
    }
}
