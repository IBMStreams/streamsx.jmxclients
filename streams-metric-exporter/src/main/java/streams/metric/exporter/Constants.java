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

public class Constants {
	public static final String
		PROGRAM_NAME = "streams-metric-exporter";
	
	/* Environment Variables */
	public static final String
		ENV_JMXCONNECT = "STREAMS_EXPORTER_JMXCONNECT",
		ENV_DOMAIN_ID = "STREAMS_DOMAIN_ID",
		ENV_INSTANCE_ID = "STREAMS_INSTANCE_ID",
		ENV_HOST = "STREAMS_EXPORTER_HOST",
		ENV_PORT = "STREAMS_EXPORTER_PORT",
		ENV_WEBPATH = "STREAMS_EXPORTER_WEBPATH",
		ENV_USERNAME = "STREAMS_EXPORTER_USERNAME",
		ENV_PASSWORD = "STREAMS_EXPORTER_PASSWORD",
		ENV_X509CERT = "STREAMS_X509CERT",
		ENV_REFRESHRATE = "STREAMS_EXPORTER_REFRESHRATE",
		ENV_TRUSTSTORE = "STREAMS_EXPORTER_TRUSTSTORE",
		ENV_PROTOCOL = "STREAMS_EXPORTER_SSLOPTION"
	;
	

	public static final String
		DEFAULT_JMXCONNECT = null,
		DEFAULT_DOMAIN_ID = null,
		DEFAULT_INSTANCE_ID = null,
		DEFAULT_HOST = "0.0.0.0",
		DEFAULT_PORT = "25500",
		DEFAULT_WEBPATH = "./",
		DEFAULT_USERNAME = null,
		DEFAULT_PASSWORD = null,
		DEFAULT_X509CERT = null,
		DEFAULT_REFRESHRATE = "10",
		DEFAULT_TRUSTSTORE = null,
		DEFAULT_PROTOCOL = "SSL_TLSv2"
	;
	public static final String indent = "       ";
	
	public static final String
		DESC_HELP = "Display command line arguments",
		DESC_JMXCONNECT = "JMX Connection URL (e.g. service:jmx:jmxmp://localhost:9975)\n      Environment Variable: " + ENV_JMXCONNECT,
		DESC_DOMAIN_ID = "Streams domain name\n      Environment Variable: " + ENV_DOMAIN_ID,
		DESC_INSTANCE_ID = "Streams instance name\n      Environment Variable: " + ENV_INSTANCE_ID,
		DESC_HOST = "Listen Host or IP address for this service (e.g. localhost)\n      Environment Variable: " + ENV_HOST,
		DESC_PORT = "Listen Port for this service\n      Environment Variable: " + ENV_PORT,
		DESC_WEBPATH = "Base URI prefix (e.g. /someprefix)\n      Environment Variable: " + ENV_WEBPATH,
		DESC_USERNAME = "Streams login username. Use this or X509CERT\n      Environment Variable: " + ENV_USERNAME,
		DESC_PASSWORD = "Streams login password. Recommend using environment variable\n      Environment Variable: " + ENV_PASSWORD,
		DESC_X509CERT = "X509 Certificate file to use instead of username/password\n      Environment Variable: " + ENV_X509CERT,
		DESC_REFRESHRATE = "Refresh rate of metrics in seconds\n      Environment Variable: " + ENV_REFRESHRATE,
		DESC_TRUSTSTORE = "Java keystore of certificates/signers to trust from JMX Server\n      Environment Variable: " + ENV_TRUSTSTORE,
		DESC_PROTOCOL = "SSL Protocol for connection to Streams JMX Server (e.g. SSL_TLSv2, TSLv1.1, TLSv1.2)\n      Environment Variable: " + ENV_PROTOCOL,
		DESC_NOCONSOLE = "Flag to indicate not to prompt for password (can still redirect from stdin or use environment variable for password."
	;
}
