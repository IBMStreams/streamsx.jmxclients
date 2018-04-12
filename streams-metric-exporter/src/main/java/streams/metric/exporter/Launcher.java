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
import java.io.IOException;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.lang.time.StopWatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

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
import streams.metric.exporter.rest.RestServer;
import streams.metric.exporter.streamstracker.StreamsDomainTracker;

import java.io.FileInputStream;

import java.security.KeyStore;
import java.security.cert.CertificateException;

public class Launcher {

	private JmxConnectionPool connectionPool;
	private WebClient webClient;

	private static final Logger LOGGER = LoggerFactory.getLogger("root");

	private final boolean retryInitialConnection = true;
	private JmxServiceContext jmxContext = null;

	private ServiceConfig config = null;
	@SuppressWarnings("unused")
	static private StreamsDomainTracker domainTracker = null;
	static private RestServer restServer = null;

	public Launcher(ServiceConfig config) {
		
		this.config = config;
		connectionPool = new JmxConnectionPool(config.getJmxUrl(), config.getX509Cert(), config.getUser(),
				config.getPassword(), config.getSslOption(), retryInitialConnection);

		TrustManager[] trustManagers = null;

		if (config.getTruststore() == null) {
			trustManagers = new TrustManager[] { new JmxTrustManager() };
		} else {
			try {
				KeyStore ks = KeyStore.getInstance("JKS");

				try {
					FileInputStream fis = new FileInputStream(config.getTruststore());

					try {
						ks.load(fis, null);

						try {
							TrustManagerFactory tmf = TrustManagerFactory
									.getInstance(TrustManagerFactory.getDefaultAlgorithm());
							tmf.init(ks);

							trustManagers = tmf.getTrustManagers();
						} catch (NoSuchAlgorithmException e) {
							throw new IllegalStateException("Unable to initialize TrustManagerFactory", e);
						} catch (KeyStoreException e) {
							throw new IllegalStateException("Unable to initialize TrustManagerFactory", e);
						}
					} catch (NoSuchAlgorithmException e) {
						throw new IllegalStateException("Keystore verification algorithm not found", e);
					} catch (CertificateException e) {
						throw new IllegalStateException(
								String.format("Unable to load certificates from %s", config.getTruststore()), e);
					} finally {
						fis.close();
					}
				} catch (IOException e) {
					throw new IllegalStateException(
							String.format("Unable to load keystore file %s", config.getTruststore()), e);
				}
			} catch (KeyStoreException e) {
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
		LOGGER.debug("Creating and starting HTTP Server...");
		try {
			restServer = new RestServer(config.getHost(), config.getPort(), config.getWebPath(),
					config.getServerProtocol(), config.getServerKeystore(), config.getServerKeystorePwd());
		} catch (Exception e) {
			LOGGER.error("Error starting REST Server: HTTP Server failed to start", e);
			return false;
		}
		LOGGER.debug("... HTTP Server Started");
		return true;
	}

	// If we cannot connect to the JMX Server at least once shutdown
	// Once started, we allow for reconnection attempts, but if this fails
	// it usually means the credentials or url are incorrect and should get
	// fixed.
	public boolean checkValidJMXConnection() {
		boolean success = true;
		LOGGER.debug("Connecting to JMX Server {}...", new Object[] { config.getJmxUrl() });
		try {
			@SuppressWarnings("unused")
			MXBeanSource streamsBeanSource = connectionPool.getBeanSource();
			LOGGER.debug("...Connected");
		} catch (IOException e) {
			LOGGER.error("Inital JMX Connection Failed: ", e);
			success = false;
		}
		return success;
	}

	private boolean startStreamsDomainTracker() {
		LOGGER.debug("Creating and starting Streams Domain Tracker...");
		StopWatch sw = null;
		if (LOGGER.isDebugEnabled()) {
			sw = new StopWatch();
			sw.reset();
			sw.start();
		}

		try {
			domainTracker = StreamsDomainTracker.initDomainTracker(jmxContext, config.getDomainName(),
					config.getInstanceNameSet(), config.getRefreshRateSeconds(), config.getSslOption());
		} catch (StreamsTrackerException e) {
			LOGGER.error(
					"Error starting StreamsDomainTracker: Could not construct the StreamsDomainTracker, Exit!", e.getLocalizedMessage());
			return false;
		}

		if (LOGGER.isDebugEnabled()) {
			sw.stop();
			LOGGER.debug("Timing for initial startup of StreamsDomainTracker (milliseconds): " + sw.getTime());
		}

		LOGGER.debug("...Streams Domain Tracker started.");
		return true;
	}
	
	private static void setupLogging(String loglevel, String logdir) {
		// Try setting log level
		org.apache.log4j.Logger logger = org.apache.log4j.Logger.getRootLogger();
		logger.setLevel(org.apache.log4j.Level.toLevel(loglevel));
		
		org.apache.log4j.Appender console_appender = logger.getAppender("CONSOLE");
		org.apache.log4j.Appender rollingfile_appender = logger.getAppender("ROLLINGFILE");
		
		((org.apache.log4j.AppenderSkeleton)console_appender).setThreshold(org.apache.log4j.Level.toLevel(loglevel));
		((org.apache.log4j.AppenderSkeleton)rollingfile_appender).setThreshold(org.apache.log4j.Level.toLevel(loglevel));;		
		
		
		// Turn off built in grizzly logging that uses JUL, and route to our SLF4J via
		// log4j implementation
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();

		// java.util.logging.Logger julLogger =
		// java.util.logging.Logger.getLogger("org.glassfish.grizzly");

		// julLogger.setLevel(Level.FINER);
		// julLogger.setUseParentHandlers(false);
		// java.util.logging.ConsoleHandler ch = new java.util.logging.ConsoleHandler();
		// ch.setLevel(Level.FINEST);
		// julLogger.addHandler(ch);

	}

	private static void printVersion() {
		System.out.println(Version.getTitleAndVersionString());
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

		if (config.isVersion()) {
			printVersion();
			System.exit(0);
		}
		

		// Add validate config because we now accept environment variables, and
		// jcommander does not handle that
		// FUTURE: replace with a more comprehensive approach
		try {
			config.validateConfig();
		} catch (ParameterException e) {
			System.out.println(e.getLocalizedMessage());
			System.out.println("Use -h to get command line usage");
			System.exit(1);
		}
		
		setupLogging(config.getLoglevel(), config.getLogdir());

		System.out.println("Streams Metric Exporter STARTING...");

		LOGGER.debug("*** Configuration ***\n" + config);

		Launcher launcher = new Launcher(config);
		if (launcher.checkValidJMXConnection()) {
			if (launcher.startRestServer()) {
				if (launcher.startStreamsDomainTracker()) {
					LOGGER.info("Streams Metric Exporter RUNNING.");
				} else {
					LOGGER.error("Startup of Streams Metric Exporter FAILED, Exiting Program.");
					System.out.println("Startup of Streams Metric Exporter FAILED, Exiting Program.");
					restServer.stopServer();
					System.exit(1);
				}
			} else {
				LOGGER.error("Startup of HTTP Server FAILED, Exiting Program.");
				System.out.println("Startup of HTTP Server FAILED, Exiting Program.");
				System.exit(1);
			}
		} else {
			LOGGER.error("Initial JMX Connection failed.  Exiting Program.");
			System.out.println("Initial JMX Connection failed.  See log for details.");
			System.out.println("  Check status of Streams Domain and JMX Service");
			System.out.println("  Check JMX url and connection credentials");
		}
		
		System.out.println("Streams Metric Exporter STARTED");
	}
}
