package streams.jmx.ws.rest;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

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

    public HttpServer startServer() {

        String[] packages = { "streams.jmx.ws.rest.resources",
                "streams.jmx.ws.rest.errorhandling",
                "streams.jmx.ws.rest.serializers" };

        final ResourceConfig rc = new ResourceConfig().packages(packages);
        // Enable JSON media conversions
        rc.register(JacksonFeature.class);

        return GrizzlyHttpServerFactory.createHttpServer(URI.create(baseUri),
                rc);
    }

    public void stopServer() {
        server.shutdownNow();
    }
}
