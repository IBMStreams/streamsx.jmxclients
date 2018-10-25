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

public class Constants {
	public static final String
		PROGRAM_NAME = "streams-jmx-client";
	
	public static final String
		INTERACTIVE_PREFIX = "streamsclient",
		INTERACTIVE_SUFFIX = "> "
	;

	/* Commands */
	public static final String
		CMD_HELP = "help",
		CMD_VERSION = "version",
		CMD_GETDOMAINSTATE = "getdomainstate",
		CMD_GETINSTANCESTATE = "getinstancestate",
		CMD_LISTJOBS = "lsjobs",
		CMD_SUBMITJOB = "submitjob",
		CMD_CANCELJOB = "canceljob",
		CMD_SNAPSHOTJOBS = "snapshotjobs",
		CMD_QUIT = "quit"
	;

	/* Command Descriptions */
	public static final String
		DESC_GETDOMAINSTATE = "Get the state of the Streams domain",
		DESC_GETINSTANCESTATE = "Get the state of the Streams instance",
		DESC_LISTJOBS = "Get a list of the jobs running in an instance",
		DESC_SUBMITJOB = "Submit Streams application to run in a Streams instance",
		DESC_CANCELJOB = "Cancel Streams application",
		DESC_SNAPSHOTJOBS = "Capture a snapshot of all jobs in the Streams instance",
		DESC_QUIT = "Exit interactive client application"
	;

	/* Environment Variables */
	public static final String
		ENV_JMXCONNECT = "STREAMS_CLIENT_JMXCONNECT",
		ENV_DOMAIN_ID = "STREAMS_DOMAIN_ID",
		ENV_INSTANCE_ID = "STREAMS_INSTANCE_ID",
		ENV_INSTANCE_LIST = "STREAMS_CLIENT_INSTANCE_LIST",
		ENV_HOST = "STREAMS_CLIENT_HOST",
		ENV_PORT = "STREAMS_CLIENT_PORT",
		ENV_WEBPATH = "STREAMS_CLIENT_WEBPATH",
		ENV_USERNAME = "STREAMS_CLIENT_USERNAME",
		ENV_PASSWORD = "STREAMS_CLIENT_PASSWORD",
		ENV_X509CERT = "STREAMS_X509CERT",
		ENV_REFRESHRATE = "STREAMS_CLIENT_REFRESHRATE",
		ENV_JMX_TRUSTSTORE = "STREAMS_CLIENT_JMX_TRUSTSTORE",
		ENV_JMX_SSLOPTION = "STREAMS_CLIENT_JMX_SSLOPTION",
		ENV_JMX_HTTP_HOST = "STREAMS_CLIENT_JMX_HTTP_HOST",
		ENV_JMX_HTTP_PORT = "STREAMS_CLIENT_JMX_HTTP_PORT",
		ENV_SERVER_PROTOCOL = "STREAMS_CLIENT_SERVER_PROTOCOL",
		ENV_SERVER_KEYSTORE = "STREAMS_CLIENT_SERVER_KEYSTORE",
		ENV_SERVER_KEYSTORE_PWD = "STREAMS_CLIENT_SERVER_KEYSTORE_PWD",
		ENV_LOGLEVEL = "STREAMS_CLIENT_LOGLEVEL",
		ENV_LOGDIR = "STREAMS_CLIENT_LOGDIR"
	;
	

	/* Program Argument Defaults */
	public static final String
		DEFAULT_JMXCONNECT = null,
		DEFAULT_DOMAIN_ID = null,
		DEFAULT_INSTANCE_ID = null,
		DEFAULT_INSTANCE_LIST = "UNSPECIFIED",
		DEFAULT_HOST = "0.0.0.0",
		DEFAULT_PORT = "25500",
		DEFAULT_WEBPATH = "/",
		DEFAULT_USERNAME = null,
		DEFAULT_PASSWORD = null,
		DEFAULT_X509CERT = null,
		DEFAULT_REFRESHRATE = "10",
		DEFAULT_JMX_TRUSTSTORE = null,
		DEFAULT_JMX_SSLOPTION = "TLSv1",
		DEFAULT_JMX_HTTP_HOST = null,
		DEFAULT_JMX_HTTP_PORT = null,
		DEFAULT_SERVER_PROTOCOL = "http",
		DEFAULT_SERVER_KEYSTORE = null,
		DEFAULT_SERVER_KEYSTORE_PWD = null,
		DEFAULT_LOGLEVEL = "fatal", // Supress logging unless user really wants it.
		DEFAULT_LOGDIR = ""
	;
	
	public static final String indent = "       ";
	
	/* Program Argment Descriptions */
	public static final String
		DESC_HELP = "Display commands or specific commands arguments",
		DESC_VERSION = "Display version information",
		DESC_JMXCONNECT = "JMX Connection URL (e.g. service:jmx:jmxmp://localhost:9975)\n      Environment Variable: " + ENV_JMXCONNECT,
		DESC_DOMAIN_ID = "Streams domain name\n      Environment Variable: " + ENV_DOMAIN_ID,
		DESC_INSTANCE_ID = "Streams instance name.  Only used if Instance List not provided.\n      Environment Variable: " + ENV_INSTANCE_ID,
		DESC_INSTANCE_LIST = "Comma separated list of 1 or more Streams Instances to monitor ('ALL' for all instances). Default ALL if STREAMS_INSTANCE_ID is not set.\n      Envrionment Variable: " + ENV_INSTANCE_LIST,
		DESC_HOST = "Listen Host or IP address for this service (e.g. localhost)\n      Environment Variable: " + ENV_HOST,
		DESC_PORT = "Listen Port for this service\n      Environment Variable: " + ENV_PORT,
		DESC_WEBPATH = "Base URI prefix (e.g. /someprefix)\n      Environment Variable: " + ENV_WEBPATH,
		DESC_USERNAME = "Streams login username. Use this or X509CERT\n      Environment Variable: " + ENV_USERNAME,
		DESC_PASSWORD = "Streams login password. Recommend using environment variable\n      Environment Variable: " + ENV_PASSWORD,
		DESC_X509CERT = "X509 Certificate file to use instead of username/password\n      Environment Variable: " + ENV_X509CERT,
		DESC_REFRESHRATE = "Refresh rate of metrics in seconds or 0 for no automatic refresh\n      Environment Variable: " + ENV_REFRESHRATE,
		DESC_JMX_TRUSTSTORE = "Java keystore of certificates/signers to trust from JMX Server\n      Environment Variable: " + ENV_JMX_TRUSTSTORE,
		DESC_JMX_SSLOPTION = "SSL Option for connection to Streams JMX Server (e.g. SSL_TLSv2, TSLv1.1, TLSv1.2)\n      Environment Variable: " + ENV_JMX_SSLOPTION,
		DESC_JMX_HTTP_HOST = "Host or IP used to replace jmx http large data set URL host fields.  Not usually needed. Use with caution.      Environment Variable: " + ENV_JMX_HTTP_HOST,
		DESC_JMX_HTTP_PORT = "Port used to replace jmx http large data set URL port fields.  Not usually needed. Use with caution.      Environment Variable: " + ENV_JMX_HTTP_PORT,
		DESC_NOCONSOLE = "Flag to indicate not to prompt for password (can still redirect from stdin or use environment variable for password.",
		DESC_SERVER_PROTOCOL = "http or https.  https will use one-way ssl authentication and java default for tls level (TLSv1.2)\n      Environment Variable: " + ENV_SERVER_PROTOCOL,
		DESC_SERVER_KEYSTORE = "Java keystore containing server certificate and key to identify server side of this application\n      Environment Variable: " + ENV_SERVER_KEYSTORE,
		DESC_SERVER_KEYSTORE_PWD = "Passphrase to java keystore.  Passphrase of keystore and key (if it has one) must match\n      Environment Variable: " + ENV_SERVER_KEYSTORE_PWD,
		DESC_LOGLEVEL = "Logging level [ fatal | error | warn | info | debug | trace ]\n      Environment Variable: " + ENV_LOGLEVEL,
		DESC_LOGDIR = "Logging direcotry.  If not set or empty log to stdout.\n     Environment Variable: " + ENV_LOGDIR
	;
	
	public static final String
	    INVALID_SERVER_PROTOCOL = "%s is not a valid protocol.  Valid values include [http|https]",
	    INVALID_REFRESHRATE = "%s is not a valid refresh rate.  Must be positive integer or 0 for no automatic refresh",
	    INVALID_LOGLEVEL = "%s is not a valid loglevel.  Valid values include [ fatal | error | warn | info | debug | trace ]"
	;
	
	public static final int NO_REFRESH = 0;
	public static final String LOG_APPENDER_NAME = "STREAMSJMXCLIENT";
	public static final String LOG_FILENAME = "StreamsJmxClient.log";
	public static final String LOG_PATTERN_LAYOUT = "%d{ISO8601} - %-5p [%t:%C{1}@%L] - %m%n";
	public static final int LOG_MAX_BACKUP_INDEX = 5;
	public static final String LOG_MAX_FILE_SIZE = "10MB";

	

}
