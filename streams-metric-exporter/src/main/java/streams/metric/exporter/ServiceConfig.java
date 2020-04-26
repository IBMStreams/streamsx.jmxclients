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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.internal.Console;
import com.beust.jcommander.internal.DefaultConsole;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import streams.metric.exporter.cli.ServerProtocolValidator;
import streams.metric.exporter.cli.LoglevelValidator;
import streams.metric.exporter.rest.Protocol;
import streams.metric.exporter.cli.FileExistsValidator;
import streams.metric.exporter.cli.DirectoryExistsValidator;
import streams.metric.exporter.cli.RefreshRateValidator;
import streams.metric.exporter.cli.ServerProtocolConverter;

public class ServiceConfig {
	
    private static final Logger LOGGER = LoggerFactory.getLogger("root");

	// Command line arguments with defaults from environment variables

    @Parameter(names = "--help", description = Constants.DESC_HELP, help = true)
    private boolean help;
    
    @Parameter(names = {"-v", "--version" }, description = Constants.DESC_VERSION, required = false)
    private boolean version;

    @Parameter(names = { "-h", "--host" }, description = Constants.DESC_HOST, required = false)
    private String host = getEnvDefault(Constants.ENV_HOST,"localhost");
    	    
    @Parameter(names = { "-p", "--port" }, description = Constants.DESC_PORT, required = false)
    private String port = getEnvDefault(Constants.ENV_PORT,"25500");
    
    @Parameter(names = { "--webPath","" }, description = Constants.DESC_WEBPATH, required = false)
    private String webPath = getEnvDefault(Constants.ENV_WEBPATH,Constants.DEFAULT_WEBPATH);

    @Parameter(names = { "-j", "--jmxurl" }, description = Constants.DESC_JMXCONNECT, required = false)
    private String jmxUrl = getEnvDefault(Constants.ENV_JMXCONNECT,Constants.DEFAULT_JMXCONNECT);

    @Parameter(names = { "-i", "--instance" }, description = Constants.DESC_INSTANCE_ID, required = false)
    private String instanceName = getEnvDefault(Constants.ENV_INSTANCE_ID,Constants.DEFAULT_INSTANCE_ID);
    
    @Parameter(names = { "-u", "--user" }, description = Constants.DESC_USERNAME, required = false)
    private String user = getEnvDefault(Constants.ENV_USERNAME,Constants.DEFAULT_USERNAME);
    
    @Parameter(names = {"--password"}, description = Constants.DESC_PASSWORD, required = false)
    @JsonIgnore
    private String password = getEnvDefault(Constants.ENV_PASSWORD,Constants.DEFAULT_PASSWORD);
    @JsonGetter("password")
    public String jsonPassword() { 
    		if (getPassword() != null && !getPassword().isEmpty()) {
    			return "(hidden)";
    		}
    		return "";
    }
    
    @Parameter(names = { "-x", "--x509cert" }, description = Constants.DESC_X509CERT, required = false)
    private String x509Cert = getEnvDefault(Constants.ENV_X509CERT,Constants.DEFAULT_X509CERT);

    @Parameter(names = "--noconsole", description = Constants.DESC_NOCONSOLE)
    private boolean hasNoConsole = false;

    @Parameter(names = { "-r", "--refresh" }, description = Constants.DESC_REFRESHRATE, required = false)
    private int refreshRateSeconds = Integer.parseInt(getEnvDefault(Constants.ENV_REFRESHRATE,Constants.DEFAULT_REFRESHRATE));

    @Parameter(names = "--jmxtruststore", description = Constants.DESC_JMX_TRUSTSTORE, required = false, validateWith = FileExistsValidator.class)
    private String truststore = getEnvDefault(Constants.ENV_JMX_TRUSTSTORE,Constants.DEFAULT_JMX_TRUSTSTORE);

    @Parameter(names = "--jmxssloption", description = Constants.DESC_JMX_SSLOPTION, required = false)
    private String sslOption = getEnvDefault(Constants.ENV_JMX_SSLOPTION,Constants.DEFAULT_JMX_SSLOPTION);
    
    @Parameter(names = "--jmxhttphost", description = Constants.DESC_JMX_HTTP_HOST, required = false)
    private String jmxHttpHost = getEnvDefault(Constants.ENV_JMX_HTTP_HOST,Constants.DEFAULT_JMX_HTTP_HOST);
    
    @Parameter(names = "--jmxhttpport", description = Constants.DESC_JMX_HTTP_PORT, required = false)
    private String jmxHttpPort = getEnvDefault(Constants.ENV_JMX_HTTP_PORT,Constants.DEFAULT_JMX_HTTP_PORT);
    
    @Parameter(names = "--serverprotocol", description = Constants.DESC_SERVER_PROTOCOL, required = false, validateWith = ServerProtocolValidator.class)
    private String serverProtocol = getEnvDefault(Constants.ENV_SERVER_PROTOCOL,Constants.DEFAULT_SERVER_PROTOCOL);
    
    @Parameter(names = "--serverkeystore", description = Constants.DESC_SERVER_KEYSTORE, required = false, validateWith = FileExistsValidator.class)
    private String serverKeystore = getEnvDefault(Constants.ENV_SERVER_KEYSTORE,Constants.DEFAULT_SERVER_KEYSTORE);
    
    @Parameter(names = "--serverkeystorepwd", description = Constants.DESC_SERVER_KEYSTORE_PWD, required = false)
    @JsonIgnore
    private String serverKeystorePwd = getEnvDefault(Constants.ENV_SERVER_KEYSTORE_PWD,Constants.DEFAULT_SERVER_KEYSTORE_PWD);
    @JsonGetter("serverKeystorePwd")
    public String jsonserverKeystorePwd() { 
    		if (getServerKeystorePwd() != null && !getServerKeystorePwd().isEmpty()) {
    			return "(hidden)";
    		}
    		return "";
    }
    
    @Parameter(names = { "-l", "--loglevel" }, description = Constants.DESC_LOGLEVEL, required = false, validateWith = LoglevelValidator.class)
    private String loglevel = getEnvDefault(Constants.ENV_LOGLEVEL, Constants.DEFAULT_LOGLEVEL);
    
    @Parameter(names = "--logdir", description = Constants.DESC_LOGDIR, required = false, validateWith = DirectoryExistsValidator.class)
    private String logdir = getEnvDefault(Constants.ENV_LOGDIR, Constants.DEFAULT_LOGDIR);
    
    
    public String getPassword(boolean hasConsole) {
        // Choose the appropriate JCommander console implementation to use
        Console console = null;

        if (!hasConsole) {
            console = new DefaultConsole();
        } else {
            System.out.print("User password: ");

            console = JCommander.getConsole();
        }

        return new String(console.readPassword(false));
    }
    


    private String readPassword() {
        if (password == null) {
            password = getPassword(!hasNoConsole);
        }

        return password;
    }

    public String getPassword() {
        if (user != null && !user.isEmpty()) {
            return readPassword();
        } else {
            return null;
        }

    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }
    
    public String getWebPath() {
        return webPath;
    }
    
    public void setWebPath(String webPath) {
        this.webPath = webPath;
    }

    public String getJmxUrl() {
        return jmxUrl;
    }

    public void setJmxUrl(String jmxUrl) {
        this.jmxUrl = jmxUrl;
    }

        public String getInstanceName() {
    		return instanceName;
    }
    public void setInstanceName(String instanceName) {
    		this.instanceName = instanceName;
    }
    
    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getX509Cert() {
        return x509Cert;
    }

    public void setX509Cert(String x509Cert) {
        this.x509Cert = x509Cert;
    }

    public boolean isHasNoConsole() {
        return hasNoConsole;
    }

    public void setHasNoConsole(boolean hasNoConsole) {
        this.hasNoConsole = hasNoConsole;
    }

    public int getRefreshRateSeconds() {
        return refreshRateSeconds;
    }

    public void setRefreshRateSeconds(int refreshRateSeconds) {
        this.refreshRateSeconds = refreshRateSeconds;
    }

    public void setTruststore(String path) {
        truststore = path;
    }

    public String getTruststore() {
        return truststore;
    }

    public boolean isHelp() {
        return help;
    }

    public void setHelp(boolean help) {
        this.help = help;
    }
    
    public boolean isVersion() {
		return version;
	}

	public void setVersion(boolean version) {
		this.version = version;
	}

	public String getSslOption() {
		return sslOption;
	}

	public void setSslOption(String sslOption) {
		this.sslOption = sslOption;
	}
	

	public String getJmxHttpHost() {
		return jmxHttpHost;
	}

	public void setJmxHttpHost(String jmxHttpHost) {
		this.jmxHttpHost = jmxHttpHost;
	}

	public String getJmxHttpPort() {
		return jmxHttpPort;
	}

	public void setJmxHttpPort(String jmxHttpPort) {
		this.jmxHttpPort = jmxHttpPort;
	}



	public void setServerProtocol(String serverProtocol) {
		this.serverProtocol = serverProtocol;
	}

	public String getServerKeystore() {
		return serverKeystore;
	}

	public void setServerKeystore(String serverKeystore) {
		this.serverKeystore = serverKeystore;
	}

	public String getServerKeystorePwd() {
		return serverKeystorePwd;
	}

	public void setServerKeystorePwd(String serverKeystorePwd) {
		this.serverKeystorePwd = serverKeystorePwd;
	}
	 
	public String getLoglevel() {
		return loglevel;
	}

	public void setLoglevel(String loglevel) {
		this.loglevel = loglevel;
	}

	public String getLogdir() {
		return logdir;
	}

	public void setLogdir(String logdir) {
		this.logdir = logdir;
	}

	// Validated values.  Cannot just use jcommander because we now accept environment variables
	public Protocol getServerProtocol() throws ParameterException {
		return ServerProtocolConverter.convertProtocol(serverProtocol);
	}
	
	public void validateConfig() throws ParameterException {
		if (getJmxUrl() == null) {
			throw new ParameterException(
					"JMX URL must be specified.  Please use parameter (-j or --jmxUrl) or environment variable: " + Constants.ENV_JMXCONNECT);
		}
		if (getInstanceName() == null) {
			throw new ParameterException(
					"Streams instance name must be specified.  Please use parameter (-i or --instance) or environment variable: " + Constants.ENV_INSTANCE_ID);
		}
		if (!ServerProtocolValidator.isValid(serverProtocol)) {
            throw new ParameterException(String.format(Constants.INVALID_SERVER_PROTOCOL, serverProtocol));
		}
		if (!RefreshRateValidator.isValid(refreshRateSeconds)) {
			throw new ParameterException(String.format(Constants.INVALID_REFRESHRATE, refreshRateSeconds));
		}
		if (!LoglevelValidator.isValid(loglevel)) {
			throw new ParameterException(String.format(Constants.INVALID_LOGLEVEL, loglevel));
		}		
        if ((user == null || getPassword() == null) && this.getX509Cert() == null) {
            throw new ParameterException(
                    "Missing or incomplete credentials. Please select an authentication parameter (-u or -X509cert) or set environment variables: " +
                    		Constants.ENV_USERNAME + " or " + Constants.ENV_X509CERT);
        }
        if ((port != null) && !(port.isEmpty())) {
	        try {
	        		Integer.parseInt(port);
	        } catch (NumberFormatException e) {
	        		throw new ParameterException(
	        				"Invalid Port number(" + port + "). Must be an integer.");
	        }
        }
        if ((jmxHttpPort != null) && !(jmxHttpPort.isEmpty())) {
	        try {
	        		Integer.parseInt(jmxHttpPort); 
	        } catch (NumberFormatException e) {
	        		throw new ParameterException(
	        			"Invalid jmxHttpPort(" + jmxHttpPort + "). Must be an integer.");
	        }
        }
    }
	
	

	@Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        String newline = System.getProperty("line.separator");

        result.append("host: " + this.getHost());
        result.append(newline);
        result.append("port: " + this.getPort());
        result.append(newline);
        result.append("webPath: " + this.getWebPath());
        result.append(newline);
        result.append("jmxUrl: " + this.getJmxUrl());
        result.append(newline);
        result.append("instance: " + this.getInstanceName());
        result.append(newline);
        result.append("user: " + this.getUser());
        result.append(newline);
        result.append("hasNoConsole: " + this.isHasNoConsole());
        result.append(newline);
        if (LOGGER.isTraceEnabled()) {
        		if (user != null && !user.isEmpty()) {
        			result.append("password: " + this.readPassword());
        			result.append(newline);
        		}
        } else {
        		result.append("password: (hidden)");
        		result.append(newline);
        }
        result.append("x509cert: " + this.getX509Cert());
        result.append(newline);
        result.append("refreshRateSeconds: " + this.getRefreshRateSeconds());
        result.append(newline);
        result.append("jmxtruststore: " + getTruststore());
        result.append(newline);
        result.append("jmxssloption: " + getSslOption());
        result.append(newline);
        result.append("jmxhttphost: " + getJmxHttpHost());
        result.append(newline);
        result.append("jmxhttpport: " + getJmxHttpPort());
        result.append(newline);
        result.append("serverprotocol: " + getServerProtocol().toString());
        result.append(newline);
        result.append("serverkeystore: " + getServerKeystore());
        result.append(newline);
        result.append("serverkeystorepwd: " + getServerKeystorePwd());
        result.append(newline);
        result.append("loglevel: " + getLoglevel());
        result.append(newline);
        result.append("logdir: " + getLogdir());
        return result.toString();
    }
     
    private String getEnvDefault(String env, String defaultValue) {
    	String value = System.getenv(env);
    	return value == null ? defaultValue : value;
    }
}
