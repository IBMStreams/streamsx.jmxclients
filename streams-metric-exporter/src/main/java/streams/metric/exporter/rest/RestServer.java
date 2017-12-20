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

import java.io.IOException;
import java.net.URI;

import javax.net.ssl.SSLContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.prometheus.client.exporter.MetricsServlet;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.servlet.ServletRegistration;
import org.glassfish.grizzly.servlet.WebappContext;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.servlet.ServletContainer;


public class RestServer {
    private static final Logger LOGGER = LoggerFactory.getLogger("root."
            + RestServer.class.getName());

    private String baseUri = null;
    private HttpServer server = null;
    
    private Protocol serverProtocol = Protocol.HTTP;
    private String serverKeystore = null;
    private String serverKeystorePwd = null;
    
    
    public RestServer(String host, String webPort, String webPath, Protocol serverProtocol, String serverKeystore, String serverKeystorePwd) throws IOException {
    	String webPathString = (webPath == null?"":webPath);
        //this.baseUri = serverProtocol.toString() + "://" + host + ":" + webPort + ((webPath.length() > 0) && !webPath.startsWith("/")?"/":"") + webPath + (webPath.endsWith("/")?"":"/");
        this.baseUri = serverProtocol.toString() + "://" + host + ":" + webPort + ((webPathString.length() > 0) && !webPathString.startsWith("/")?"/":"") + webPathString + (webPathString.endsWith("/")?"":"/");

        this.serverProtocol = serverProtocol;
        this.serverKeystore = serverKeystore;
        this.serverKeystorePwd = serverKeystorePwd;
        
        server = startServer();

        LOGGER.info("HTTP Server Listening on: {}",
                new Object[] { this.baseUri });
    }


    public HttpServer startServer() throws IOException {
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
        
       //if (this.serverProtocol.equalsIgnoreCase("https")) {
        if (this.serverProtocol == Protocol.HTTPS) {

        	LOGGER.debug("Using https protocol");
        	theServer = createHttpsServer();
        } else {
        	LOGGER.debug("Using http protcol");
        	theServer = createHttpServer();
        }
        // Prometheus servlet
        // FUTURE: if we go with plugin this needs to be variant if it is created or not
        //ServletRegistration prometheus = context.addServlet("PrometheusContainer",new MetricsServlet());
        //prometheus.addMapping("/prometheus");
        
        context.deploy(theServer);
        
        theServer.start();
 
        return theServer;
    }
    
    private HttpServer createHttpServer() {
    	// Create server but do not start it
    	return GrizzlyHttpServerFactory.createHttpServer(URI.create(this.baseUri),false);
    }
    
    private HttpServer createHttpsServer() {
        SSLContextConfigurator sslContextConfig = new SSLContextConfigurator();

        // set up security context
        sslContextConfig.setKeyStoreFile(this.serverKeystore); // contains server cert and key
        sslContextConfig.setKeyStorePass(this.serverKeystorePwd);

        // Create context and have exceptions raised if anything wrong with keystore or password
        SSLContext sslContext = sslContextConfig.createSSLContext(true);
                
        // Create server but do not start it
        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(URI.create(this.baseUri),false);

        
        //LOGGER.debug("About to loop through listeners");
        //for (NetworkListener listener : server.getListeners()) {
        //	LOGGER.debug("About to setSecure on listener name: " + listener.getName());
        //}
        
        // grizzly is the default listener name
        server.getListener("grizzly").setSecure(true);
        // One way authentication
        server.getListener("grizzly").setSSLEngineConfig(new SSLEngineConfigurator(sslContext).setClientMode(false).setNeedClientAuth(false));
        return server;
    }
    
    public void stopServer() {
        server.shutdownNow();
    }
}
