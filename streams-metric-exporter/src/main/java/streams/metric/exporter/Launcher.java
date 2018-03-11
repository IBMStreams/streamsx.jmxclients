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

package streams.metric.exporter;

import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.io.IOException;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.lang.time.StopWatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import streams.metric.exporter.error.StreamsTrackerException;
import streams.metric.exporter.httpclient.WebClient;
import streams.metric.exporter.httpclient.WebClientImpl;
import streams.metric.exporter.jmx.JmxConnectionPool;
import streams.metric.exporter.jmx.JmxServiceContext;
import streams.metric.exporter.jmx.JmxTrustManager;
import streams.metric.exporter.jmx.MXBeanSource;
import streams.metric.exporter.jmx.MXBeanSourceProvider;
import streams.metric.exporter.rest.Protocol;
import streams.metric.exporter.rest.RestServer;
import streams.metric.exporter.streamstracker.StreamsDomainTracker;

import java.io.FileInputStream;

import java.security.KeyStore;
import java.security.cert.CertificateException;

public class Launcher {

    private JmxConnectionPool connectionPool;
    private WebClient         webClient;

    private static final Logger LOGGER = LoggerFactory.getLogger("root");

    private final boolean retryInitialConnection = true;
    private JmxServiceContext jmxContext = null;

    private ServiceConfig config = null;
    static private StreamsDomainTracker domainTracker = null;
    static private RestServer restServer = null;

    public Launcher(ServiceConfig config) {
        this.config = config;
        connectionPool = new JmxConnectionPool(config.getJmxUrl(),
                config.getX509Cert(), config.getUser(),
                config.getPassword(), config.getSslOption(), retryInitialConnection);

        TrustManager[] trustManagers = null;

        if (config.getTruststore() == null) {
            trustManagers = new TrustManager[] { new JmxTrustManager() };
        }
        else {
            try {
                KeyStore ks = KeyStore.getInstance("JKS");

                try {
                    FileInputStream fis = new FileInputStream(config.getTruststore());

                    try {
                        ks.load(fis, null);

                        try {
                            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                            tmf.init(ks);
  
                            trustManagers = tmf.getTrustManagers();
                        }
                        catch (NoSuchAlgorithmException e) {
                            throw new IllegalStateException("Unable to initialize TrustManagerFactory", e);
                        }
                        catch (KeyStoreException e) {
                            throw new IllegalStateException("Unable to initialize TrustManagerFactory", e);
                        }
                    }
                    catch (NoSuchAlgorithmException e) {
                        throw new IllegalStateException("Keystore verification algorithm not found", e);
                    }
                    catch (CertificateException e) {
                        throw new IllegalStateException(String.format("Unable to load certificates from %s", config.getTruststore()), e);
                    }
                    finally {
                        fis.close();
                    }
                }
                catch (IOException e) {
                    throw new IllegalStateException(String.format("Unable to load keystore file %s", config.getTruststore()), e);
                }
            }
            catch (KeyStoreException e) {
                throw new IllegalStateException("JKS is not a supported keystore type?!", e);
            } 
        }

        webClient = new WebClientImpl(config.getSslOption(), trustManagers);

        this.jmxContext = new JmxServiceContext() {
            public MXBeanSourceProvider getBeanSourceProvider() {
                return connectionPool;
            }

            public WebClient getWebClient() {
                return webClient;
            }
        };
    }

    private boolean startRestServer() {
    	LOGGER.info("Creating and starting HTTP Server...");
        try {
            restServer = new RestServer(config.getHost(), config.getPort(), config.getWebPath(), config.getServerProtocol(), config.getServerKeystore(), config.getServerKeystorePwd());
        } catch (Exception e) {
            LOGGER.error("HTTP Server failed to start !!", e);
            return false;
        }
        LOGGER.info("... HTTP Server Started");
        return true;
    }
    
    // If we cannot connect to the JMX Server at least once shutdown
    // Once started, we allow for reconnection attempts, but if this fails
    // it usually means the credentials or url are incorrect and should get
    // fixed.
    public boolean checkValidJMXConnection() {
    	boolean success = true;
        LOGGER.info("Connecting to JMX Server {}...", new Object[] { config.getJmxUrl() });
        try {
            MXBeanSource streamsBeanSource = connectionPool.getBeanSource();
            LOGGER.info("...Connected");
        } catch (IOException e) {
            LOGGER.error("Inital JMX Connection Failed: ", e);
            success = false;
        }   
        return success;
    }

    private boolean startStreamsMonitor() {
    	LOGGER.info("Creating and starting Streams Tracker...");
        StopWatch sw = null;
        if (LOGGER.isDebugEnabled()) {
            sw = new StopWatch();
            sw.reset();
            sw.start();
        }

        try {
            domainTracker = StreamsDomainTracker.initDomainTracker(
                    jmxContext, config.getDomainName(), config.getInstanceNameSet(),
                    config.getRefreshRateSeconds(), config.getSslOption());
        } catch (StreamsTrackerException e) {
            LOGGER.error("Could not construct the StreamsInstanceJobMonitor, Exit!", e);
            return false;
        }

        if (LOGGER.isDebugEnabled()) {
            sw.stop();
            LOGGER.debug("Timing for initial startup of StreamsDomainTracker (milliseconds): " + sw.getTime()) ;
        }
        
        LOGGER.info("...Streams Domain Tracker started.");
        return true;
    }


    public static void main(String[] args) {
        // Parse command line arguments
        ServiceConfig config = new ServiceConfig();
        JCommander jc = null;
        try {
            jc = new JCommander(config);
            jc.setProgramName(Constants.PROGRAM_NAME);
            jc.setColumnSize(132);
            jc.parse(args);
        } catch (ParameterException e) {
            System.out.println(e.getLocalizedMessage());
            jc.usage();
            System.exit(1);
        }

        if (config.isHelp()) {
            jc.usage();
            System.exit(0); 
        }
        
        // Add validate config because we now accept environment variables, and jcommander does not handle that
        // FUTURE: replace with a more comprehensive approach
        try {
        	config.validateConfig();
        } catch (ParameterException e) {
        	System.out.println(e.getLocalizedMessage());
        	jc.usage();
        	System.exit(1);
        }

        LOGGER.trace("*** Settings ***\n" + config);
        
        // REMOVE!!!
        System.exit(1);
        
        Launcher launcher = new Launcher(config);
        if (launcher.checkValidJMXConnection()) {
        	if (launcher.startRestServer()) {
        		if (launcher.startStreamsMonitor()) {
        			LOGGER.info("Streams Metric Exporter running.");
        		} else {
        			LOGGER.error("Startup of Streams Tracker failed, Exiting Program.");
        			restServer.stopServer();
        			System.exit(1);
        		}
        	} else {
        		LOGGER.error("Startup of HTTP Server failed, Exiting Program.");
        		System.exit(1);
        	}
        } else {
        	LOGGER.error("Initial JMX Connection failed.  Exiting Program.");
        	System.out.println("Initial JMX Connection failed.  See log for details.");
        	System.out.println("  Check status of Streams Domain and JMX Service");
        	System.out.println("  Check JMX url and connection credentials");
        }
    }
}
