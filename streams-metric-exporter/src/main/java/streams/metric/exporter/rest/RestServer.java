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
        ServletRegistration prometheus = context.addServlet("PrometheusContainer",new MetricsServlet());
        prometheus.addMapping("/prometheus");
        
        context.deploy(theServer);

        
        return theServer;
    }
    public void stopServer() {
        server.shutdownNow();
    }
}
