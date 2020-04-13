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
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.lang.time.StopWatch;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
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
	private static boolean consoleLogging = true;

	private final boolean retryInitialConnection = true;
	private JmxServiceContext jmxContext = null;

	private ServiceConfig config = null;
	@SuppressWarnings("unused")
	static private StreamsDomainTracker domainTracker = null;
	static private RestServer restServer = null;

	public Launcher(ServiceConfig config) {
		
		this.config = config;
		LOGGER.debug("************************************");
		LOGGER.debug("****** Create JMX Connection Pool...");
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
		LOGGER.debug("*******************************************");
		LOGGER.debug("****** Creating and starting HTTP Server...");
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
		LOGGER.debug("*********************************");
		LOGGER.debug("****** checkValidJMXConnection..."); 
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
		LOGGER.debug("***********************************************");
		LOGGER.debug("****** Creating and starting Streams Tracker...");
		StopWatch sw = null;
		if (LOGGER.isDebugEnabled()) {
			sw = new StopWatch();
			sw.reset();
			sw.start();
		}

		try {
			domainTracker = StreamsDomainTracker.initDomainTracker(jmxContext, config.getDomainName(),
					config.getInstanceNameSet(), config.getRefreshRateSeconds(), config.getSslOption(), config);
		} catch (StreamsTrackerException e) {
			LOGGER.error(
					"Error starting StreamsDomainTracker: Could not construct the StreamsDomainTracker: {}", e.getLocalizedMessage());
			return false;
		}

		if (LOGGER.isDebugEnabled()) {
			sw.stop();
			LOGGER.debug("Timing for initial startup of StreamsDomainTracker (milliseconds): " + sw.getTime());
		}

		LOGGER.debug("...Streams Domain Tracker started.");
		return true;
	}
	
	private static boolean setupLogging(String loglevel, String logdir) {
		// Set the log level
		org.apache.log4j.Logger logger = org.apache.log4j.Logger.getRootLogger();
		logger.setLevel(org.apache.log4j.Level.toLevel(loglevel));
		
		// Create our appender
		PatternLayout layout = new PatternLayout(Constants.LOG_PATTERN_LAYOUT);
		
		if (logdir != null && !logdir.isEmpty() ) {
			// Rolling Log file
			consoleLogging = false;
			Path logfilePath = Paths.get(logdir,Constants.LOG_FILENAME);
			Path finalPath = logfilePath.toAbsolutePath();
			
			System.out.println("Logging to rolling logfile: " + finalPath);
			try {
				RollingFileAppender rollingAppender = new RollingFileAppender(layout,finalPath.toString(),true);
				rollingAppender.setName(Constants.LOG_APPENDER_NAME);
				rollingAppender.setMaxFileSize(Constants.LOG_MAX_FILE_SIZE);
				rollingAppender.setMaxBackupIndex(Constants.LOG_MAX_BACKUP_INDEX);
				logger.addAppender(rollingAppender);
				
			} catch (IOException e) {
				System.out.println("Error creating logfile: " + e.getLocalizedMessage());
				return false;
			}
		} else {
			// Console Logging
			System.out.println("Logging to console...");
			ConsoleAppender consoleAppender = new ConsoleAppender(layout);
			consoleAppender.setName(Constants.LOG_APPENDER_NAME);
			logger.addAppender(consoleAppender);
		}		
		
		// Turn off built in grizzly logging that uses JUL, and route to our SLF4J via
		// log4j implementation
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
	
		return true;
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
			System.out.println("Use --help to get command line usage");
			System.exit(1);
		}
		
		System.out.println("Streams Metric Exporter STARTING...");

		if (setupLogging(config.getLoglevel(), config.getLogdir()) == false) {
			System.exit(1);
		};
		
		if (!consoleLogging) {
			LOGGER.info("Streams Metric Exporter STARTING...");
		}


		LOGGER.debug("*** Configuration ***\n" + config);

		Launcher launcher = new Launcher(config);
		if (launcher.checkValidJMXConnection()) {
			if (launcher.startRestServer()) {
				if (! launcher.startStreamsDomainTracker()) {
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
		
		System.out.println("*******************************");
		System.out.println("Streams Metric Exporter STARTED");
		System.out.println("*******************************");

		if (!consoleLogging) {
			LOGGER.info("*******************************");
			LOGGER.info("Streams Metric Exporter STARTED");
			LOGGER.info("*******************************");
		}
	}
}
