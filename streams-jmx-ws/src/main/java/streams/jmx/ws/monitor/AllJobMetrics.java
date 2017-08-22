package streams.jmx.ws.monitor;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigInteger;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Date;
import java.security.GeneralSecurityException;

import org.apache.commons.lang.time.StopWatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonRawValue;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.ibm.streams.management.job.OperatorInputPortMXBean;
import com.ibm.streams.management.job.OperatorOutputPortMXBean;

import com.ibm.streams.management.instance.InstanceMXBean;

public class AllJobMetrics {
    private static final Logger LOGGER = LoggerFactory.getLogger("root."
            + AllJobMetrics.class.getName());

    private String domainName;
    private String instanceName;
    private JmxServiceContext jmxContext;

    private InstanceMXBean instance;
    private String protocol;

    // Primary attributes for consumer
    private Date lastMetricsRefresh = null;
    private Date lastMetricsFailure = null;
    private boolean lastMetricsRefreshFailed = false;
    private String allMetrics = null;

    // @JsonValue
    @JsonRawValue
    public String getAllMetrics() {
        return allMetrics;
    }

    private void setAllMetrics(String allMetrics) {
        this.allMetrics = allMetrics;
    }

    public Date getLastMetricsRefresh() {
        return lastMetricsRefresh;
    }

    public void setLastMetricsRefresh(Date lastMetricsRefresh) {
        this.lastMetricsRefresh = lastMetricsRefresh;
    }

    public Date getLastMetricsFailure() {
        return lastMetricsFailure;
    }

    public void setLastMetricsFailure(Date lastMetricsFailure) {
        this.lastMetricsFailure = lastMetricsFailure;
    }

    public boolean isLastMetricsRefreshFailed() {
        return lastMetricsRefreshFailed;
    }

    public void setLastMetricsRefreshFailed(boolean lastMetricsRefreshFailed) {
        this.lastMetricsRefreshFailed = lastMetricsRefreshFailed;
    }

    public AllJobMetrics(JmxServiceContext jmxContext, String domainName,
            String instanceName, String protocol) throws IOException,
            StreamsMonitorException {

        this.domainName = domainName;
        this.instanceName = instanceName;
        this.jmxContext = jmxContext;
        this.protocol = protocol;

        this.refresh();

    }

    public void clear() {
        this.allMetrics = null;

    }

    public void refresh() throws IOException, StreamsMonitorException {
        LOGGER.trace("Entered");
        LOGGER.trace("** Refreshing all jobs metrics");

        StringBuilder newMetrics = new StringBuilder();
        String uri = null;
        Date previousRefresh = null;
        if (this.lastMetricsRefresh != null) {
            previousRefresh = new Date(this.lastMetricsRefresh.getTime());
        } else {
            previousRefresh = new Date();
        }

        StopWatch stopwatch = null;
        LinkedHashMap<String, Long> timers = null;
        if (LOGGER.isDebugEnabled()) {
            stopwatch = new StopWatch();
            timers = new LinkedHashMap<String, Long>();
            stopwatch.reset();
            stopwatch.start();
        }

        // JMX Interaction
        try {
            LOGGER.trace("* AllJobMetrcs * Get instance object from jmxContext");

            InstanceMXBean instance = jmxContext.getBeanSourceProvider()
                    .getBeanSource()
                    .getInstanceBean(this.domainName, this.instanceName);

            LOGGER.trace("* AllJobMetrics * SnapshotJobMetrics...");
            //
            // ISSUE: snapshotJobMetrics does not declare it throws IOException
            // but it does and comes back to us as UndeclaredThrowableException,
            // handle that here
            //
            try {
                uri = instance.snapshotJobMetrics();
            } catch (UndeclaredThrowableException e) {
                LOGGER.trace("* Handling snapshotJobMetrics UndeclaredThrowableException and unwrapping it");
                Throwable t = e.getUndeclaredThrowable();
                if (t instanceof IOException) {
                    LOGGER.trace("*    It was an IOException we can handle, throwing the IOException");
                    throw (IOException) t;
                } else {
                    LOGGER.trace("*    It was an "
                            + t.getClass()
                            + " which was unexpected, throw original undeclarable...");
                    throw e;
                }
            }
            if (LOGGER.isDebugEnabled()) {
                stopwatch.stop();
                timers.put("snapshotJobMetrics", stopwatch.getTime());
            }

        } catch (IOException e) {
            // IOException from JMX usually means server restarted or domain
            LOGGER.warn("Metrics snapshot received JMX IO Error.  Recording that metrics failed.");
            this.setLastMetricsFailure(new Date());
            this.setLastMetricsRefreshFailed(true);

            throw e;

        }

        if (LOGGER.isDebugEnabled()) {
            stopwatch.reset();
            stopwatch.start();
        }

        /******* HTTPS Interaction ********/
        try {
            LOGGER.trace("* AllJobMetrics * Connect to Metrics URI and retrieve...");

            this.setAllMetrics(jmxContext.getWebClient().get(uri));
            this.setLastMetricsRefresh(new Date());
            this.setLastMetricsRefreshFailed(false);

            if (LOGGER.isDebugEnabled()) {
                stopwatch.stop();
                timers.put("connect and retrieve metrics", stopwatch.getTime());
            }

        } catch (WebClientException e) {
            // IOException from JMX usually means server restarted or domain
            LOGGER.warn("Metrics snapshot received HTTP Error.  Recording that metrics failed.");
            this.setLastMetricsFailure(new Date());
            this.setLastMetricsRefreshFailed(true);
            throw new StreamsMonitorException(e);
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Profiling for AllMetrics Refresh");
            LOGGER.debug("Time (seconds) since last refresh: "
                    + ((this.lastMetricsRefresh.getTime() - previousRefresh
                            .getTime()) / 1000));
            for (Map.Entry<String, Long> entry : timers.entrySet()) {
                LOGGER.debug("Timing for " + entry.getKey() + ": "
                        + entry.getValue());
            }
        }

        LOGGER.trace("Exited");

    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        String newline = System.getProperty("line.separator");

        result.append("LastMetricsRefresh: "
                + convertDate(this.getLastMetricsRefresh()));
        result.append(newline);
        result.append("LastMetricsFailure: "
                + convertDate(this.getLastMetricsFailure()));
        result.append(newline);
        result.append("LastMetricsRefreshFailed: "
                + this.isLastMetricsRefreshFailed());
        return result.toString();
    }

    private String convertDate(Date date) {
        if (date != null) {
            Format format = new SimpleDateFormat("yyyy MM dd HH:mm:ss");
            return format.format(date);
        } else {
            return "null";
        }
    }
}
