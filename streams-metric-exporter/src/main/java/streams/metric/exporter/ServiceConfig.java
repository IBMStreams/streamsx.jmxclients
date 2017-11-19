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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.internal.Console;
import com.beust.jcommander.internal.DefaultConsole;

import streams.metric.exporter.cli.validators.FileExistsValidator;

public class ServiceConfig {

    @Parameter(names = "--help", description = Constants.DESC_HELP, help = true)
    private boolean help;

    @Parameter(names = { "-h", "--host" }, description = Constants.DESC_HOST, required = false)
    private String host = getEnvDefault(Constants.ENV_HOST,"localhost");
    	    
    @Parameter(names = { "-p", "--port" }, description = Constants.DESC_PORT, required = false)
    private String port = getEnvDefault(Constants.ENV_PORT,"25500");
    
    @Parameter(names = { "--webPath","" }, description = Constants.DESC_WEBPATH, required = false)
    private String webPath = getEnvDefault(Constants.ENV_WEBPATH,Constants.DEFAULT_WEBPATH);

    @Parameter(names = { "-j", "--jmxurl" }, description = Constants.DESC_JMXCONNECT, required = false)
    private String jmxUrl = getEnvDefault(Constants.ENV_JMXCONNECT,Constants.DEFAULT_JMXCONNECT);

    @Parameter(names = { "-d", "--domain" }, description = Constants.DESC_DOMAIN_ID, required = false)
    private String domainName = getEnvDefault(Constants.ENV_DOMAIN_ID,Constants.DEFAULT_DOMAIN_ID);

    @Parameter(names = { "-i", "--instance" }, description = Constants.DESC_INSTANCE_ID, required = false)
    private String instanceName = getEnvDefault(Constants.ENV_INSTANCE_ID,Constants.DEFAULT_INSTANCE_ID);

    @Parameter(names = { "-u", "--user" }, description = Constants.DESC_USERNAME, required = false)
    private String user = getEnvDefault(Constants.ENV_USERNAME,Constants.DEFAULT_USERNAME);
    
    @Parameter(names = {"--password"}, description = Constants.DESC_PASSWORD, required = false)
    private String password = getEnvDefault(Constants.ENV_PASSWORD,Constants.DEFAULT_PASSWORD);
    
    @Parameter(names = { "-x", "--x509cert" }, description = Constants.DESC_X509CERT, required = false)
    private String x509Cert = getEnvDefault(Constants.ENV_X509CERT,Constants.DEFAULT_X509CERT);

    @Parameter(names = "--noconsole", description = Constants.DESC_NOCONSOLE)
    private boolean hasNoConsole = false;

    @Parameter(names = { "-r", "--refresh" }, description = Constants.DESC_REFRESHRATE, required = false)
    private int refreshRateSeconds = Integer.parseInt(getEnvDefault(Constants.ENV_REFRESHRATE,Constants.DEFAULT_REFRESHRATE));

    @Parameter(names = "--truststore", description = Constants.DESC_TRUSTSTORE, required = false, validateWith = FileExistsValidator.class)
    private String truststore = getEnvDefault(Constants.ENV_TRUSTSTORE,Constants.DEFAULT_TRUSTSTORE);

    @Parameter(names = "--sslOption", description = Constants.DESC_PROTOCOL, required = false)
    private String sslOption = getEnvDefault(Constants.ENV_PROTOCOL,Constants.DEFAULT_PROTOCOL);;
    

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

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
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

    public void setProtocol(String p) {
        sslOption = p;
    }

    public String getProtocol() {
        return sslOption;
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
        result.append("domain: " + this.getDomainName());
        result.append(newline);
        result.append("instance: " + this.getInstanceName());
        result.append(newline);
        result.append("user: " + this.getUser());
        result.append(newline);
        result.append("hasNoConsole: " + this.isHasNoConsole());
        result.append(newline);
        if (user != null && !user.isEmpty()) {
            result.append("password: " + this.readPassword());
            result.append(newline);
        }
        result.append("x509cert: " + this.getX509Cert());
        result.append(newline);
        result.append("refreshRateSeconds: " + this.getRefreshRateSeconds());
        result.append(newline);
        result.append("truststore: " + getTruststore());
        result.append(newline);
        result.append("protocol: " + getProtocol());

        return result.toString();
    }
     
    private String getEnvDefault(String env, String defaultValue) {
    	String value = System.getenv(env);
    	return value == null ? defaultValue : value;
    }
}
