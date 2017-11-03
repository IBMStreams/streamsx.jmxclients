package streams.metric.exporter;

import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import java.util.HashMap;
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

import streams.metric.exporter.error.StreamsMonitorException;
import streams.metric.exporter.httpclient.WebClient;
import streams.metric.exporter.httpclient.WebClientImpl;
import streams.metric.exporter.jmx.JmxConnectionPool;
import streams.metric.exporter.jmx.JmxServiceContext;
import streams.metric.exporter.jmx.JmxTrustManager;
import streams.metric.exporter.jmx.MXBeanSource;
import streams.metric.exporter.jmx.MXBeanSourceProvider;
import streams.metric.exporter.rest.RestServer;
import streams.metric.exporter.streamstracker.StreamsInstanceTracker;

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
    private StreamsInstanceTracker jobTracker = null;
    private RestServer restServer = null;

    public Launcher(ServiceConfig config) {
        this.config = config;

        connectionPool = new JmxConnectionPool(config.getJmxUrl(),
                config.getX509Cert(), config.getUser(),
                config.getPassword(), config.getProtocol(), retryInitialConnection);

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

        webClient = new WebClientImpl(config.getProtocol(), trustManagers);

        this.jmxContext = new JmxServiceContext() {
            public MXBeanSourceProvider getBeanSourceProvider() {
                return connectionPool;
            }

            public WebClient getWebClient() {
                return webClient;
            }
        };
    }

    private void startRestServer() {
        try {
            restServer = new RestServer(config.getHost(), config.getPort(), config.getWebPath());
        } catch (Exception e) {
            LOGGER.error("REST Server failed to start !! NEED BETTER ERROR HANDLING !!", e);

            System.exit(1);
        }
    }
    
    // If we cannot connect to the JMX Server at least once shutdown
    // Once started, we allow for reconnection attempts, but if this fails
    // it usually means the credentials or url are incorrect and should get
    // fixed.
    public boolean checkValidJMXConnection() {
    	boolean success = true;
        try {
            MXBeanSource streamsBeanSource = connectionPool.getBeanSource();
        } catch (IOException e) {
            LOGGER.error("Inital JMX Connection Failed: ", e);
            success = false;
        }   
        return success;
    }

    private void startStreamsMonitor() {
        StopWatch sw = null;
        LinkedHashMap<String, Long> timers = null;
        if (LOGGER.isDebugEnabled()) {
            sw = new StopWatch();
            // Create hashMap for timing Stuff
            timers = new LinkedHashMap<String, Long>();
        }

        LOGGER.info("Connecting to JMX Server {}...", new Object[] { config.getJmxUrl() });

        if (LOGGER.isDebugEnabled()) {
            sw.reset();
            sw.start();
        }

        LOGGER.info("...Connected");

        if (LOGGER.isDebugEnabled()) {
            sw.stop();
            timers.put("Connect", sw.getTime());

            LOGGER.debug("Debug Profiling of StreamsJMXServer");
            for (Map.Entry<String, Long> entry : timers.entrySet()) {
                LOGGER.debug("Timing for " + entry.getKey() + ": "
                        + entry.getValue());
            }
        }

        if (LOGGER.isDebugEnabled()) {
            timers.clear();

            sw.reset();
            sw.start();
        }

        try {
            jobTracker = StreamsInstanceTracker.initInstance(
                    jmxContext, config.getDomainName(), config.getInstanceName(),
                    config.getRefreshRateSeconds(), config.getProtocol());
        } catch (StreamsMonitorException e) {
            LOGGER.error("Could not construct the StreamsInstanceJobMonitor", e);

            System.exit(1);
        }

        if (LOGGER.isDebugEnabled()) {
            sw.stop();
            timers.put("startStreamsMonitor", sw.getTime());

            LOGGER.debug("Debug Profiling of StreamsJMXServer");

            for (Map.Entry<String, Long> entry : timers.entrySet()) {
                LOGGER.debug("Timing for " + entry.getKey() + ": "
                        + entry.getValue());
            }
        }

    }


    public static void main(String[] args) {
        // Parse command line arguments
        ServiceConfig config = new ServiceConfig();
        JCommander jc = null;
        try {
            jc = new JCommander(config);
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

        LOGGER.trace("*** Settings ***\n" + config);
        Launcher launcher = new Launcher(config);
        if (launcher.checkValidJMXConnection()) {
        	launcher.startRestServer();
        	launcher.startStreamsMonitor();
        } else {
        	LOGGER.error("Initial JMX Connection failed.  Exiting Program.");
        	System.out.println("Initial JMX Connection failed.  See log for details.");
        	System.out.println("  Check status of Streams Domain and JMX Service");
        	System.out.println("  Check JMX url and connection credentials");
        }
    }
}
