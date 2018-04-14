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

package streams.metric.exporter.streamstracker.snapshots;


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

public class AllJobSnapshots {
    private static final Logger LOGGER = LoggerFactory.getLogger("root."
            + AllJobSnapshots.class.getName());

    private String domainName;
    private String instanceName;
    private JmxServiceContext jmxContext;
    private String jmxHttpHost;
    private String jmxHttpPort;

    //private InstanceMXBean instance;
    //private String protocol;

    // Primary attributes for consumer
    private Date lastSnapshotRefresh = null;
    private Date lastSnapshotFailure = null;
    private boolean lastSnapshotRefreshFailed = false;
    private String allSnapshots = null;

    // @JsonValue
    @JsonRawValue
    public String getAllSnapshots() {
        return allSnapshots;
    }

    private void setAllSnapshots(String allSnapshots) {
        this.allSnapshots = allSnapshots;
    }

    public Date getLastSnaphostRefresh() {
        return lastSnapshotRefresh;
    }

    public void setLastSnapshotRefresh(Date lastSnapshotRefresh) {
        this.lastSnapshotRefresh = lastSnapshotRefresh;
    }

    public Date getLastSnapshotFailure() {
        return lastSnapshotFailure;
    }

    public void setLastSnapshotFailure(Date lastSnapshotFailure) {
        this.lastSnapshotFailure = lastSnapshotFailure;
    }

    public boolean isLastSnapshotRefreshFailed() {
        return lastSnapshotRefreshFailed;
    }

    public void setLastSnapshotRefreshFailed(boolean lastSnapshotRefreshFailed) {
        this.lastSnapshotRefreshFailed = lastSnapshotRefreshFailed;
    }

    public AllJobSnapshots(JmxServiceContext jmxContext, String domainName,
            String instanceName, String jmxHttpHost, String jmxHttpPort) throws IOException,
            StreamsTrackerException {

        this.domainName = domainName;
        this.instanceName = instanceName;
        this.jmxContext = jmxContext;
        this.jmxHttpHost = jmxHttpHost;
        this.jmxHttpPort = jmxHttpPort;
        //this.protocol = protocol;

        this.refresh();

    }

    public void clear() {
        this.allSnapshots = null;

    }

    public void refresh() throws IOException, StreamsTrackerException {
        LOGGER.trace("Entered");
        LOGGER.trace("** Refreshing all job snapshots");

        String uri = null;
        Date previousRefresh = null;
        if (this.lastSnapshotRefresh != null) {
            previousRefresh = new Date(this.lastSnapshotRefresh.getTime());
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
            LOGGER.trace("* AllJobSnapshots * Get instance object from jmxContext");

            InstanceMXBean instance = jmxContext.getBeanSourceProvider()
                    .getBeanSource()
                    .getInstanceBean(this.domainName, this.instanceName);

            LOGGER.trace("* AllJobSnapshots * SnapshotJobs...");
            //
            // ISSUE: snapshotJobMetrics does not declare it throws IOException
            // but it does and comes back to us as UndeclaredThrowableException,
            // handle that here, not suyre about snapshotJobs.
            //
            try {
                uri = instance.snapshotJobs(null,1,false);
            } catch (UndeclaredThrowableException e) {
                LOGGER.trace("* Handling snapshotJobs UndeclaredThrowableException and unwrapping it");
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
                timers.put("jmx call to snapshotJobs(null,1,false)", stopwatch.getTime());
            }

        } catch (IOException e) {
            // IOException from JMX usually means server restarted or domain
            LOGGER.warn("snapshotJobs received JMX IO Error.  Recording that snapshots failed.");
            this.setLastSnapshotFailure(new Date());
            this.setLastSnapshotRefreshFailed(true);

            throw e;

        }

        if (LOGGER.isTraceEnabled()) {
            stopwatch.reset();
            stopwatch.start();
        }

        /******* HTTPS Interaction ********/
        try {
            LOGGER.debug("Instance ({}) Snapshots HTTP Retrieve from URI ({}) ...",this.instanceName,uri);

            this.setAllSnapshots(jmxContext.getWebClient().get(uri,this.jmxHttpHost,this.jmxHttpPort));
            this.setLastSnapshotRefresh(new Date());
            this.setLastSnapshotRefreshFailed(false);

            if (LOGGER.isTraceEnabled()) {
                stopwatch.stop();
                timers.put("connect jmx(http server) and retrieve snapshots", stopwatch.getTime());
            }

        } catch (WebClientException e) {
            // IOException from JMX usually means server restarted or domain
            LOGGER.warn("Job snapshots received HTTP Error.  Recording that snapshots failed.");
            this.setLastSnapshotFailure(new Date());
            this.setLastSnapshotRefreshFailed(true);
            throw new StreamsTrackerException(e);
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Profiling for AllSnapshots Refresh");
            LOGGER.trace("Time (seconds) since last refresh: "
                    + ((this.lastSnapshotRefresh.getTime() - previousRefresh
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

        result.append("LastSnapshotRefresh: "
                + convertDate(this.getLastSnaphostRefresh()));
        result.append(newline);
        result.append("LastSnapshotFailure: "
                + convertDate(this.getLastSnapshotFailure()));
        result.append(newline);
        result.append("LastSnapshotRefreshFailed: "
                + this.isLastSnapshotRefreshFailed());
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
