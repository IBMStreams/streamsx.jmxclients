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

package streams.jmx.client;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jline.TerminalFactory;
import jline.console.ConsoleReader;
import streams.jmx.client.commands.AbstractJmxCommand;
import streams.jmx.client.commands.CancelJob;
import streams.jmx.client.commands.Command;
import streams.jmx.client.commands.CommandResult;
import streams.jmx.client.commands.GetDomainState;
import streams.jmx.client.commands.GetInstanceState;
import streams.jmx.client.commands.SubmitJob;
import streams.jmx.client.commands.Help;
import streams.jmx.client.commands.ListJobs;
import streams.jmx.client.commands.Quit;
import streams.jmx.client.commands.SnapshotJobs;
import streams.jmx.client.commands.Version;
import streams.jmx.client.httpclient.WebClient;
import streams.jmx.client.httpclient.WebClientImpl;
import streams.jmx.client.jmx.JmxConnectionPool;
import streams.jmx.client.jmx.JmxServiceContext;
import streams.jmx.client.jmx.JmxTrustManager;
import streams.jmx.client.jmx.MXBeanSource;
import streams.jmx.client.jmx.MXBeanSourceProvider;

public class Main {



	private static final Logger LOGGER = LoggerFactory.getLogger("root");
	private static boolean consoleLogging = true;

	private static final Pattern clPattern = Pattern.compile("[^\\s]*\"(\\\\+\"|[^\"])*?\"|[^\\s]*'(\\\\+'|[^'])*?'|(\\\\\\s|[^\\s])+", Pattern.MULTILINE);

	private final boolean retryInitialConnection = false;

	private Map<String, Command> commandMap = null;

	private ServiceConfig config = null;

	private JmxConnectionPool connectionPool;
	private WebClient webClient;
	private JmxServiceContext jmxContext = null;


	//@SuppressWarnings("unused")
	//static private StreamsDomainTracker domainTracker = null;
	//static private RestServer restServer = null;

	/* Constructor */
	public Main() {
		initCommandMap();
	}

	private void initCommandMap() {
		Map<String, Command> cm = new HashMap<String, Command>();
		// cm.put(Constants.CMD_HELP, new Help());
		// cm.put(Constants.CMD_VERSION, new Version());
		cm.put(Constants.CMD_GETDOMAINSTATE, new GetDomainState());
		cm.put(Constants.CMD_GETINSTANCESTATE, new GetInstanceState());
		cm.put(Constants.CMD_LISTJOBS, new ListJobs());
		cm.put(Constants.CMD_SUBMITJOB, new SubmitJob());
		cm.put(Constants.CMD_CANCELJOB, new CancelJob());
		cm.put(Constants.CMD_SNAPSHOTJOBS, new SnapshotJobs());

		commandMap = cm;
	}

	public void processCommandLine(String[] args) {

		// Parse command line arguments
		config = new ServiceConfig();
		JCommander jc = null;
		String parsedCommand = null;
		
		try {
			// jc = new JCommander(config);
			// jc.setProgramName(Constants.PROGRAM_NAME);
			// jc.setColumnSize(132);
			// jc.addCommand(Constants.CMD_HELP, new Help());
			// jc.addCommand(Constants.CMD_VERSION, new Version());
			// jc.addCommand(Constants.CMD_GETDOMAINSTATE, new GetDomainState());
		
			jc = JCommander.newBuilder()
				.programName(Constants.PROGRAM_NAME)
				.columnSize(132)
				.addObject(config)
				.addCommand(Constants.CMD_HELP, new Help())
				.addCommand(Constants.CMD_VERSION, new Version())
				.build();
			
			for (Map.Entry<String, Command> entry : commandMap.entrySet()) {
				jc.addCommand(entry.getKey(),entry.getValue());
			}

			jc.parse(args);
			parsedCommand = jc.getParsedCommand();
			if (parsedCommand == null) {
				parsedCommand = "";
			}

		} catch (ParameterException e) {
			System.out.println("Invalid command line arguments:");
			System.out.println(e.getLocalizedMessage());
			System.out.println("Use --help or the help command to get command line usage");
			//jc.usage();
			System.exit(1);
		}

		// Order so that main options (-v -h take precedence over commands: version and help)
		if (config.isHelp()) {
			jc.usage();
			System.exit(0);
		}
		if (config.isVersion()) {
			printVersion();
			System.exit(0);
		}
		if (parsedCommand.equals(Constants.CMD_HELP)) {
			jc.usage();
			System.exit(0);
		}
		if (parsedCommand.equals(Constants.CMD_VERSION)) {
			printVersion();
			System.exit(0);
		}

		// Add validate config because we now accept environment variables, and
		// jcommander does not handle that
		// FUTURE: replace with a more comprehensive approach
		try {
			config.validateConfig();
		} catch (ParameterException e) {
			System.out.println("Invalid command line arguments:");
			System.out.println(e.getLocalizedMessage());
			System.out.println("Use --help or the help command to get command line usage");
			System.exit(1);
		}


		if (setupLogging(config.getLoglevel(), config.getLogdir()) == false) {
			System.out.println("Failed to initialize logging system, exiting.");
			System.exit(1);
		};
		
		if (!consoleLogging) {
			LOGGER.info("Streams JMX Client STARTING...");
		}


		LOGGER.debug("*** Configuration ***\n" + config);

		initJmxContext();

		if (checkValidJMXConnection()) {
			LOGGER.debug("Initial JMX Connection Succeeded, commands can be processed.");
			// if (main.startRestServer()) {
			// 	if (! main.startStreamsDomainTracker()) {
			// 		LOGGER.error("Startup of Streams Metric Exporter FAILED, Exiting Program.");
			// 		System.out.println("Startup of Streams Metric Exporter FAILED, Exiting Program.");
			// 		restServer.stopServer();
			// 		System.exit(1);
			// 	}
			// } else {
			// 	LOGGER.error("Startup of HTTP Server FAILED, Exiting Program.");
			// 	System.out.println("Startup of HTTP Server FAILED, Exiting Program.");
			// 	System.exit(1);
			// }
		} else {
			LOGGER.error("Initial JMX Connection failed.  Exiting Program.");
			System.out.println("Initial JMX Connection failed.  See log for details.");
			System.out.println("  Check status of Streams Domain and JMX Service");
			System.out.println("  Check JMX url and connection credentials");
			System.exit(1);
		}

		// JMX Connection made, initialize JMX Commands
		LOGGER.trace("Initializing commands ...");
		for (Map.Entry<String, Command> entry : commandMap.entrySet()) {
			Command c = entry.getValue();
			if (c instanceof AbstractJmxCommand) {
				((AbstractJmxCommand) c).initialize(config, jmxContext);
			}
		}

		// Commands are initialized so we can execute single command or go into interactive mode
		if (commandMap.containsKey(parsedCommand)) {
			LOGGER.debug("Executing single command: " + parsedCommand);
			Command matchedCommand = commandMap.get(parsedCommand);
			CommandResult result = matchedCommand.execute();
			if (result != null) {
				if (result.getOutput() != null) {
					System.out.println(result.getOutput());
				}
				if (!result.getExitStatus().isSuccess()) {
					System.err.println(result.getErrorMessage());
				}

				System.exit(result.getExitStatus().getStatusCode());
			}
		} else {
			// Interactive Mode
			processInteractiveCommands();
		}
		
		
	
	}


	private void processInteractiveCommands() {
		System.out.println("Streams JMX Client INTERACTIVE MODE STARTED");
		if (!consoleLogging) {
			LOGGER.info("Streams JMX Client INTERACTIVE MODE STARTED");
		}

		JCommander jc = null;
		String parsedCommand = null;
		
		try {

			Help helpCommand = new Help();

			// jc = JCommander.newBuilder()
			// 	.programName(Constants.PROGRAM_NAME)
			// 	.columnSize(132)
			// 	.addCommand(Constants.CMD_HELP, helpCommand)
			// 	.addCommand(Constants.CMD_VERSION, new Version())
			// 	.addCommand(Constants.CMD_QUIT, new Quit())
			// 	.allowParameterOverwriting(true)
			// 	.build();
			
			// for (Map.Entry<String, Command> entry : commandMap.entrySet()) {
			// 	jc.addCommand(entry.getKey(),entry.getValue());
			// }

			String command_prompt = Constants.INTERACTIVE_PREFIX + Constants.INTERACTIVE_SUFFIX;

			// Jline stuff
			ConsoleReader console = new ConsoleReader();
			console.setPrompt(command_prompt);
			String line = null;
			boolean timeToQuit = false;

			while (!timeToQuit) {
				// Create new jcommander each time.  Without this we received
				// odd issues.
				jc = JCommander.newBuilder()
					.programName(Constants.PROGRAM_NAME)
					.columnSize(132)
					.addCommand(Constants.CMD_HELP, helpCommand)
					.addCommand(Constants.CMD_VERSION, new Version())
					.addCommand(Constants.CMD_QUIT, new Quit())
					.allowParameterOverwriting(true)
					.build();
				
				for (Map.Entry<String, Command> entry : commandMap.entrySet()) {
					jc.addCommand(entry.getKey(),entry.getValue());
				}
				
				parsedCommand = null;
				line = null;

				line = console.readLine();
				if (line == null) {
					// EOF
					LOGGER.debug("readLine returned null, interpreting as EOF and exit");
					timeToQuit = true;
					continue;
				}
				
				if (line.isEmpty()){
					LOGGER.trace("Empty command line, get next input");
					continue;
				} 

				String[] clArgs = parseCommndLine(line);
	
				LOGGER.debug("interactive clArgs: " + Arrays.toString(clArgs));

				try {
					LOGGER.debug("Interactive: About to parse args...");
					jc.parse(clArgs);
					LOGGER.debug("Interactive: About to getParsedCommand, currently = " + parsedCommand);
					parsedCommand = jc.getParsedCommand();
					if (parsedCommand == null) {
						parsedCommand = "";
					}
					LOGGER.debug("parsedCommand: " + parsedCommand);


					if (parsedCommand.equals(Constants.CMD_HELP)) {
						String commandHelp = helpCommand.getHelpCommand();
						helpCommand.clearHelpCommand();
						if (commandHelp != null && !commandHelp.equals("")) {
							jc.usage(commandHelp);
						} else {
							System.out.println(String.format("%-20s  %s","Command","Description"));
							System.out.println(String.format("%-20s  %s\n","--------------------","-----------"));
							for (Map.Entry<String, JCommander> entry :  jc.getCommands().entrySet()) {
								Command commandObject = (Command)entry.getValue().getObjects().get(0);
								System.out.println(String.format("%-20s  %s\n",entry.getKey(),commandObject.getHelp()));
							}
						}
					} else if (parsedCommand.equals(Constants.CMD_VERSION)) {
						printVersion();
					} else if (parsedCommand.equals(Constants.CMD_QUIT)) {
						timeToQuit = true;
					} else if (commandMap.containsKey(parsedCommand)) {
						Command matchedCommand = commandMap.get(parsedCommand);
						CommandResult result = matchedCommand.execute();
						if (result != null) {
							if (result.getOutput() != null) {
								ObjectMapper mapper = new ObjectMapper();
								Object json = mapper.readValue(result.getOutput(), Object.class);
								
								String indented = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
								System.out.println(indented);
							}
							if (!result.getExitStatus().isSuccess()) {
								System.out.println("ERROR: " + result.getErrorMessage());
							}
						}
					} else {
						// No command specified
					}

				} catch (ParameterException e) {
					System.out.println("Invalid command line arguments:");
					System.out.println(e.getLocalizedMessage());
					System.out.println("Use --help or the help command to get command line usage");
					//jc.usage();
					//System.exit(1);
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println("Caught unexpected exception in command loop: " + e.getLocalizedMessage());
				}


			}
			console.close();

		} catch(IOException e) {
			System.out.println("Interactive Mode Exception: " + e.getLocalizedMessage());
            e.printStackTrace();
        } finally {
            try {
				TerminalFactory.get().restore();
            } catch(Exception e) {
				System.out.println("TerminalFactory restore failed");
                e.printStackTrace();
            }
        }


	}



	private void initJmxContext() {
		connectionPool = new JmxConnectionPool(config.getJmxUrl(), config.getX509Cert(), config.getUser(),
		config.getPassword(), config.getSslOption(), retryInitialConnection);

		TrustManager[] trustManagers = null;
		KeyStore ks = null;

		if (config.getTruststore() == null) {
			trustManagers = new TrustManager[] { new JmxTrustManager() };
		} else {
			try {
				ks = KeyStore.getInstance("JKS");

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

		// In the process of moving between approaches to https connections
		// Sending trustManagers for old one and ks for new apache httpclient apporach
		webClient = new WebClientImpl(config.getSslOption(), trustManagers, ks);

		this.jmxContext = new JmxServiceContext() {
			public MXBeanSourceProvider getBeanSourceProvider() {
				return connectionPool;
			}

			public WebClient getWebClient() {
				return webClient;
			}
		};
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



	private String[] parseCommndLine(String line) {
		if (line != null) {
			line = line.trim();
		}
		if (line == null || line.length() == 0) {
			return new String[] {};
		}
		Matcher m = clPattern.matcher(line);
		List<String> args = new ArrayList<String>();

		while (m.find()) {
			args.add(m.group());
		}
		return args.toArray(new String[] {});
	}

	private void close() {
		try {
			connectionPool.close();
		} catch (IOException e) {
		}
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
			//System.out.println("Logging to console...");
			ConsoleAppender consoleAppender = new ConsoleAppender(layout);
			consoleAppender.setName(Constants.LOG_APPENDER_NAME);
			logger.addAppender(consoleAppender);
		}		
		
		// Turning down apache httpclient logging
		// Default is DEBUG logging for wire is too verbose
		if (!logger.isTraceEnabled()) {
			logger.getLogger("org.apache.http.wire").setLevel(org.apache.log4j.Level.toLevel("INFO"));
		}
		//logger.getLogger("org.apache.http.http").setLevel(org.apache.log4j.Level.toLevel(loglevel));

		// Turn off built in grizzly logging that uses JUL, and route to our SLF4J via
		// log4j implementation
		// SLF4JBridgeHandler.removeHandlersForRootLogger();
		// SLF4JBridgeHandler.install();
	
		return true;
	}

	private static void printVersion() {
		System.out.println(Version.getTitleAndVersionString());
	}

	public static void main(String[] args) {

		//List<String> cmdLine = parseCommndLine(args);

		Main main = new Main();
		main.processCommandLine(args);

	}
}
