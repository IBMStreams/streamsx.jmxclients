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

package streams.metric.exporter.streamstracker.metrics;


import java.lang.reflect.UndeclaredThrowableException;
import java.io.IOException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Date;
import org.apache.commons.lang.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonRawValue;
import streams.metric.exporter.error.StreamsTrackerException;
import streams.metric.exporter.httpclient.WebClientException;
import streams.metric.exporter.jmx.JmxServiceContext;

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
            StreamsTrackerException {

        this.domainName = domainName;
        this.instanceName = instanceName;
        this.jmxContext = jmxContext;
        this.protocol = protocol;

        this.refresh();

    }

    public void clear() {
        this.allMetrics = null;

    }

    public void refresh() throws IOException, StreamsTrackerException {
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
        if (LOGGER.isTraceEnabled()) {
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
            if (LOGGER.isTraceEnabled()) {
                stopwatch.stop();
                timers.put("jmx call to snapshotJobMetrics", stopwatch.getTime());
            }

        } catch (IOException e) {
            // IOException from JMX usually means server restarted or domain
            LOGGER.warn("Metrics snapshot received JMX IO Error.  Recording that metrics failed.");
            this.setLastMetricsFailure(new Date());
            this.setLastMetricsRefreshFailed(true);

            throw e;

        }

        if (LOGGER.isTraceEnabled()) {
            stopwatch.reset();
            stopwatch.start();
        }

        /******* HTTPS Interaction ********/
        try {
            LOGGER.trace("* AllJobMetrics * Connect to Metrics URI ({}) and retrieve...",uri);

            this.setAllMetrics(jmxContext.getWebClient().get(uri));
            this.setLastMetricsRefresh(new Date());
            this.setLastMetricsRefreshFailed(false);

            if (LOGGER.isTraceEnabled()) {
                stopwatch.stop();
                timers.put("connect jmx(http server) and retrieve metrics", stopwatch.getTime());
            }

        } catch (WebClientException e) {
            // IOException from JMX usually means server restarted or domain
            LOGGER.warn("Metrics snapshot received HTTP Error.  Recording that metrics failed.");
            this.setLastMetricsFailure(new Date());
            this.setLastMetricsRefreshFailed(true);
            throw new StreamsTrackerException(e);
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Profiling for AllMetrics Refresh");
            LOGGER.trace("Time (seconds) since last refresh: "
                    + ((this.lastMetricsRefresh.getTime() - previousRefresh
                            .getTime()) / 1000));
            for (Map.Entry<String, Long> entry : timers.entrySet()) {
                LOGGER.trace("Timing for " + entry.getKey() + ": "
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
