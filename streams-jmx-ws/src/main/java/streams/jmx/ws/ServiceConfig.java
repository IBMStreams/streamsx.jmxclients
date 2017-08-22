package streams.jmx.ws;

import java.security.KeyStore;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.internal.Console;
import com.beust.jcommander.internal.DefaultConsole;

import streams.jmx.ws.cli.validators.FileExistsValidator;

public class ServiceConfig {

    private static final String STREAMS_X509CERT = "STREAMS_X509CERT";

    @Parameter(names = "--help", help = true)
    private boolean help;

    @Parameter(names = { "-h", "--host" }, description = "REST Servers host interface to listen (e.g. localhost)", required = false)
    private String host = "localhost";

    @Parameter(names = { "-p", "--port" }, description = "REST Server port to listen on (e.g. 25500", required = false)
    private String port = "25500";
    
    @Parameter(names = { "--webPath" }, description = "REST Base URI Web Path (e.g. /thispath)", required = false)
    private String webPath = "/";

    @Parameter(names = { "-j", "--jmxurl" }, description = "JMX Connection URL (e.g. service:jmx:jmxmp://server:9975)", required = true)
    private String jmxUrl;

    @Parameter(names = { "-d", "--domain" }, description = "Streams domain name", required = true)
    private String domainName;

    @Parameter(names = { "-i", "--instance" }, description = "Streams instance name", required = true)
    private String instanceName;

    @Parameter(names = { "-u", "--user" }, description = "Streams login user name", required = false)
    private String user;

    @Parameter(names = { "-x", "--x509cert" }, description = "X509 Certification file to use instead of username/password, defaults to $STREAMS_X509CERT", required = false)
    private String x509Cert = System.getenv(STREAMS_X509CERT);

    @Parameter(names = "--noconsole", description = "Indicates that the console should not be used for prompts")
    private boolean hasNoConsole = false;

    @Parameter(names = { "-r", "--refresh" }, description = "Refresh rate of metrics in seconds", required = false)
    private int refreshRateSeconds = 10;

    @Parameter(names = "--truststore", description = "Path to a Java keystore containing the trusted jmx server's certificate", required = false, validateWith = FileExistsValidator.class)
    private String truststore;

    private String protocol = "TLSv1";
    private String password;

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
        protocol = p;
    }

    public String getProtocol() {
        return protocol;
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

        return result.toString();
    }

}
