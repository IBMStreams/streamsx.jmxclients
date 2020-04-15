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

package streams.metric.exporter.streamstracker.instance;

import java.net.MalformedURLException;
import java.text.Format;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;

import javax.management.InstanceNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.apache.commons.lang.time.StopWatch;
import com.ibm.streams.management.instance.InstanceMXBean;
import com.ibm.streams.management.resource.ResourceMXBean;

import streams.metric.exporter.ServiceConfig;
import streams.metric.exporter.error.StreamsTrackerErrorCode;
import streams.metric.exporter.error.StreamsTrackerException;
import streams.metric.exporter.jmx.JmxServiceContext;
import streams.metric.exporter.jmx.MXBeanSource;
import streams.metric.exporter.jmx.MXBeanSourceProviderListener;
import streams.metric.exporter.metrics.MetricsExporter;
import streams.metric.exporter.metrics.MetricsExporter.StreamsObjectType;
import streams.metric.exporter.prometheus.PrometheusMetricsExporter;
import streams.metric.exporter.streamstracker.instance.InstanceInfo;
import streams.metric.exporter.streamstracker.job.JobDetails;
import streams.metric.exporter.streamstracker.job.JobMap;
import streams.metric.exporter.streamstracker.metrics.AllJobMetrics;
import streams.metric.exporter.streamstracker.snapshots.AllJobSnapshots;
import com.ibm.streams.management.Metric;

/*
 * StreamsInstanceTracker
 *  Initialization
 *  		* Get InstanceMXBean
 *  		* Register for JMX Notifications for the Instance
 *  		* Create InstanceInfo object
 *  Refresh
 *  		* Get Metrics Snapshot
 *  		* Get Jobs Snapshot
 *  		* Update JobMap using snapshot lists
 *  Notification of Instance change
 *  		* Update Instance Info
 */
//public class StreamsInstanceTracker implements NotificationListener, MXBeanSourceProviderListener {
public class StreamsInstanceTracker implements MXBeanSourceProviderListener {
        private static final Logger LOGGER = LoggerFactory.getLogger("root."
            + StreamsInstanceTracker.class.getName());

    private ServiceConfig config = null;
    private JmxServiceContext jmxContext;
    private boolean autoRefresh;

    /* Domain name */
    private String domainName = null;

    /* Instance info */
    private InstanceInfo instanceInfo = new InstanceInfo();

    /* Job Metrics Info */
    private AllJobMetrics allJobMetrics = null;
    
    /* Job Snapshots Info */
    private AllJobSnapshots allJobSnapshots = null;

    /* Resource Map */
    private final Map<String, Map<String, Long>> instanceResourceMetrics = new HashMap<String, Map<String, Long>>();
    private Long instanceResourceMetricsLastUpdated = null;
    
    /* Metrics Exporter */
	private MetricsExporter metricsExporter = PrometheusMetricsExporter.getInstance();

    /* Job Map */
    private JobMap jobMap = null;


    /**************************************************************************
     * Constructor
     * 
     * Note: InstanceNotFoundException is a jmx exception we use not to be
     * confused with streams instance
     ***************************************************************************/
    public StreamsInstanceTracker(JmxServiceContext jmxContext,
            String domainName, String instanceName, boolean autoRefresh,
            ServiceConfig config) throws StreamsTrackerException {
        LOGGER.debug("** Initializing StreamsInstanceTracker for: " + instanceName);
        this.config = config;
        this.jmxContext = jmxContext;
        this.domainName = domainName;
        this.instanceInfo.setInstanceName(instanceName);
        this.autoRefresh = autoRefresh;
        this.jmxContext.getBeanSourceProvider().addBeanSourceProviderListener(this);

        initStreamsInstanceTracker();

        if (this.instanceInfo.isInstanceAvailable()) {
            updateInstanceResourceMetrics();
        }

    }
    

    /*******************************************************************************
     * 
     *   I N I T I A L I Z A T I O N
     * 
     *   Ensure JMX InstanceMXBean is available
     *   Create:
     *     * Job Metrics Handler
     *     * Job Snapshot Handler
     *     * Job Map
     * 
     *******************************************************************************/
    private synchronized void initStreamsInstanceTracker() throws StreamsTrackerException {
        LOGGER.debug("initStreamsInstanceTracker()");

        // Initialize the Instance Info and get current info from the InstanceMXBean if available
        setInstanceInfo();

        // Initialize Snapshots Handler
        LOGGER.debug("  Initialize or clear Snapshots Handler (allJobSnapshots)");

        try {
            if (allJobSnapshots == null) {
                allJobSnapshots = new AllJobSnapshots(this.jmxContext, 
                this.domainName,
                this.instanceInfo.getInstanceName(),
                this.config.getJmxHttpHost(), 
                this.config.getJmxHttpPort());
            } else {
                allJobSnapshots.clear();
            }
        } catch (IOException e) {
            LOGGER.warn("JMX IO Exception when initializing all job snapshots, resetting the tracker: " + e.getLocalizedMessage());
            resetTracker();
        }

        // Initialize Metrics Handler
        LOGGER.debug("  Initialize or clear Metrics Handler (allJobMetrics)");
        try {
            if (allJobMetrics == null) {
                allJobMetrics = new AllJobMetrics(this.jmxContext, 
                this.domainName,
                this.instanceInfo.getInstanceName(),
                this.config.getJmxHttpHost(), 
                this.config.getJmxHttpPort());
            } else {
                allJobMetrics.clear();
            }
        } catch (IOException e) {
            LOGGER.warn("JMX IO Exception when initializing all job metrics, resetting the tracker: " + e.getLocalizedMessage());
            resetTracker();
        }

        // Create Job Map
        LOGGER.debug("  Create or clear Job Map");
        if (jobMap == null) {
            jobMap = new JobMap(this.instanceInfo.getInstanceName());
        } else {
            jobMap.clear();
        }
        
        // Create the metrics we will set for the instance
        LOGGER.debug("  Create Exported Instance Metrics");

        createExportedInstanceMetrics();

        LOGGER.debug("DONE...initStreamsInstanceTracker()");
    }


    /***************************************************************************************
     *   S E T   I N S T A N C E   I N F O
     * 
     *   This is the primary location of ensuring that the JMX bean for the instance
     *   Is available.  It will detect JMX issues, and Update the status of the instance
     * 
     **************************************************************************************/

    private void setInstanceInfo() throws StreamsTrackerException {
        LOGGER.trace("setInstanceInfo()");

        // Refresh the InstanceInfo from the bean also ensures we still have JMX connection
        MXBeanSource beanSource = null;
        String instanceName = this.instanceInfo.getInstanceName();

        // Initialize connection to Instance via JMX Instance Bean
        try {

            beanSource = jmxContext.getBeanSourceProvider().getBeanSource();

            // Get the InstanceMXBean
            InstanceMXBean instanceBean = beanSource.getInstanceBean(domainName,instanceName);

            // If Instance does not exist , assume it is coming and try again on the next scheduled refresh
            if (instanceBean == null) {
                LOGGER.warn(
                    "Instance '{}' not found.  Continuing assuming it will be created in the future", instanceName);
                resetTracker();
                return;
            }

            this.instanceInfo.setInstanceExists(true);
            this.instanceInfo.setInstanceStatus(instanceBean.getStatus());
            this.instanceInfo.setInstanceHealth(instanceBean.retrieveJobHealthSummary());
            this.instanceInfo.setInstanceStartTime(instanceBean.getStartTime());
            this.instanceInfo.setInstanceCreationTime(instanceBean.getCreationTime());

            // If instanceStartTime is null, then instance not ready
            if (this.instanceInfo.getInstanceStartTime() == null) {
                LOGGER.warn("Instance '{}' found, but is not started.  Current Status: {}",
                    new Object[] {
                        this.instanceInfo.getInstanceName(),
                        this.instanceInfo.getInstanceStatus() });
                resetTracker();
                return;
            } else {
                if (!this.instanceInfo.isInstanceAvailable()) {
                    // Since wasn't available before log now available
                    LOGGER.info("Streams Instance '{}' found, Status: {}",
                    new Object[] {
                        this.instanceInfo.getInstanceName(),
                        this.instanceInfo.getInstanceStatus() });
                    }
                this.instanceInfo.setInstanceAvailable(true);
            }
        } catch (UndeclaredThrowableException e) {
            // Sometimes instance issues are burried in UndeclaredThrowableExceptions from the JMX API
            Throwable t = e.getUndeclaredThrowable();
            if (t instanceof InstanceNotFoundException) {
                LOGGER.warn(
                    "Instance '{}' not found when initializing.  Continuing assuming it will be created in the future",
                    this.instanceInfo.getInstanceName());
                this.instanceInfo.setInstanceExists(false);
                resetTracker();
            } else {
                LOGGER.trace("Unexpected exception ("
                        + t.getClass()
                        + ") when initializing instance, throwing original undeclarable...");
                throw e;
            }
        } catch (MalformedURLException me) {
            resetTracker();
            throw new StreamsTrackerException(
                    "Invalid JMX URL when initializing instance", me);
        } catch (IOException ioe) {
            // JMX Error, cannot initialize streams instance so ensure state
            LOGGER.warn("JMX IO Exception when initializing instance, Continuing to wait for reconnect");
            resetTracker();
        }
    }


    /****************************************************************************
     * resetTracker In the case of a JMX error or anything else that could have
     * invalidated our state reset the state so that the instance, jobs, and
     * metrics are re-initialized and brought back into consistency with
     * Streams.
     ****************************************************************************/
    public synchronized void resetTracker() {
        LOGGER.debug("*** resetTracker ***");
        this.instanceInfo.setInstanceAvailable(false);

        // Set Metrics Failure on metrics Object
        if (this.allJobMetrics != null) {
        	this.allJobMetrics.setLastMetricsFailure(new Date());
            this.allJobMetrics.setLastMetricsRefreshFailed(true);
            this.allJobMetrics.clear();
        }
        // Set Snapshot Failure on metrics Object
        if (this.allJobSnapshots != null) {
        	this.allJobSnapshots.setLastSnapshotFailure(new Date());
            this.allJobSnapshots.setLastSnapshotRefreshFailed(true);
            this.allJobSnapshots.clear();
        }
        // Clear Job Map
        if (this.jobMap != null) {
            this.jobMap.clear();
        }
        LOGGER.debug("  removeExportedInstanceMetrics()...");
        removeExportedInstanceMetrics();
        LOGGER.debug("  createExportedInstanceMetrics()...");
        createExportedInstanceMetrics();
        LOGGER.debug("*** DONE...resetTracker()");
    }

    
    /******************************************************************
     * REFRESH
     * 
     * Primary mechanism for updating internal state of instance
     * from Streams JMX Server
     * 
     * Triggered by call from StreamsDomainTracker 
     * Exceptions at this level should just be logged so that we
     * continue to refresh.
     * Some are expected in recoverable situations so only log at low
     * level.
     * Unexpected exceptions should be thrown
     *****************************************************************/
    public synchronized void refresh() throws StreamsTrackerException {
        LOGGER.debug("** INSTANCE Refresh: {}",this.getInstanceInfo().getInstanceName());
        LOGGER.trace("** INSTANCE INFO: " + this.getInstanceInfo().toString());
        StopWatch totaltimer = null;
        StopWatch stopwatch = null;
        LinkedHashMap<String, Long> timers = null;
        if (LOGGER.isDebugEnabled()) {
            totaltimer = new StopWatch();
            stopwatch = new StopWatch();
            timers = new LinkedHashMap<String, Long>();
            totaltimer.reset();
            totaltimer.start();
            stopwatch.reset();
            stopwatch.start();
        }
        
        // If something made the instance unavailable initialize it
		if (!this.instanceInfo.isInstanceAvailable()) {
        	LOGGER.debug("Streams Instance Refresh: Instance not available, try and initialize it.");
            initStreamsInstanceTracker();

            if (LOGGER.isDebugEnabled()) {
                stopwatch.stop();
                timers.put("Intialize Streams Instance and set Info",stopwatch.getTime());
                stopwatch.reset();
                stopwatch.start();
            }
        } else {
            // Update Instance Info
            setInstanceInfo();
            if (LOGGER.isDebugEnabled()) {
                stopwatch.stop();
                timers.put("Set Streams Instance Info",stopwatch.getTime());
                stopwatch.reset();
                stopwatch.start();
            }
        }

        if (instanceInfo.isInstanceAvailable()) {
            LOGGER.trace("Instance available, updating exported instance metrics");
            metricsExporter.getStreamsMetric("status", StreamsObjectType.INSTANCE,
                this.domainName, this.instanceInfo.getInstanceName()).set(getInstanceStatusAsMetric());
            metricsExporter.getStreamsMetric("health", StreamsObjectType.INSTANCE,
                this.domainName, this.instanceInfo.getInstanceName()).set(getInstanceHealthAsMetric());

            metricsExporter.getStreamsMetric("startTime", StreamsObjectType.INSTANCE, 
                this.domainName, this.instanceInfo.getInstanceName()).set(this.instanceInfo.getInstanceStartTime());
            metricsExporter.getStreamsMetric("creationTime", StreamsObjectType.INSTANCE, 
                this.domainName, this.instanceInfo.getInstanceName()).set(this.instanceInfo.getInstanceCreationTime());


                    
            LOGGER.trace("** Calling updateInstanceResourceMetrics()");
            updateInstanceResourceMetrics();
        
            if (LOGGER.isDebugEnabled()) {
                stopwatch.stop();
                timers.put("Update Instance Resource Metrics",stopwatch.getTime());
                stopwatch.reset();
                stopwatch.start();
            }

        	LOGGER.trace("** Calling updateAllJobSnapshots(true)");
            updateAllJobSnapshots(true);
            if (LOGGER.isDebugEnabled()) {
                stopwatch.stop();
                timers.put("Update All Job Snapshots",stopwatch.getTime());
                stopwatch.reset();
                stopwatch.start();
            }

        	LOGGER.trace("** Calling updateAllJobMetrics(true)");
            updateAllJobMetrics(true);
            if (LOGGER.isDebugEnabled()) {
                stopwatch.stop();
                timers.put("Update All Job Metrics",stopwatch.getTime());
                stopwatch.reset();
                stopwatch.start();
            }
        

            LOGGER.trace("** Calling refreshAllJobs()");
            refreshAllJobs();
            if (LOGGER.isDebugEnabled()) {
                stopwatch.stop();
                timers.put("Refresh All Jobs",stopwatch.getTime());
                stopwatch.reset();
                stopwatch.start();
            }

            // Refresh job count metric
        	metricsExporter.getStreamsMetric("jobCount", StreamsObjectType.INSTANCE,
        			this.domainName, this.instanceInfo.getInstanceName()).set(jobMap.size());            

        } else {
            LOGGER.debug("Instance refresh: Instance was not available for this refresh");
        }


        if (LOGGER.isDebugEnabled()) {
            totaltimer.stop();
            stopwatch.stop();
            LOGGER.debug("** INSTANCE refresh timing (ms):");
            for (Map.Entry<String,Long> entry : timers.entrySet()) {
                LOGGER.debug("   " + entry.getKey() + " timing: " + entry.getValue());
            }
            LOGGER.debug("Total instance (" + this.instanceInfo.getInstanceName() + ") Refresh Time (ms) :" + totaltimer.getTime());              
        }
        
    }



    private void refreshAllJobs() {
        // Get currently tracked jobs
        if (jobMap != null) {
            Set<String> currentJobIds = new HashSet<String>(jobMap.getJobIds());

            LOGGER.debug("Refresh All Jobs, number of jobs: {}", currentJobIds.size());
            for (String jobId : currentJobIds) {
                JobDetails jd = jobMap.getJob(jobId);
                jd.refresh(jd.getJobSnapshot(),jd.getJobMetrics());
            }
        }
    }

    
    /***********************************************************
     * Add Job to job map
     ***********************************************************/
    private synchronized void addJobToMap(String jobid, String jobname, String jobSnapshot) {
        LOGGER.trace("AddJobToMap({})...", jobid);

        JobDetails jd = new JobDetails(this, jobid, jobname);
        jd.setJobSnapshot(jobSnapshot);
        jobMap.addJobToMap(jobid, jd);
        
		metricsExporter.getStreamsMetric("jobCount", StreamsObjectType.INSTANCE, this.domainName, this.instanceInfo.getInstanceName()).set(jobMap.size());

    }

    /***********************************************************
     * Remove Job from job map
     ***********************************************************/

    private synchronized void removeJobFromMap(String jobid) {
        LOGGER.trace("removeJobFromMap({})...", jobid);

        jobMap.removeJobFromMap(jobid);

		metricsExporter.getStreamsMetric("jobCount", StreamsObjectType.INSTANCE, this.domainName, this.instanceInfo.getInstanceName()).set(jobMap.size());
    }       
    
    /********************************************************************************
     * updateAllJobSnapshots
     * 
     * Triggered by: Refresh
     * 
     ********************************************************************************/
    private synchronized void updateAllJobSnapshots(boolean refreshFromServer)
            throws StreamsTrackerException {
        LOGGER.trace("***** Entered updateAllJobSnapshots, refreshFromServer {}",
                refreshFromServer);
        
        // Current Job IDs for use in determine missing jobs or jobs that need to be removed
        Set<String> currentJobIds = null;
        
        if (this.allJobSnapshots != null) {
            // Refresh Snapshots if requested
            if (refreshFromServer) {
                try {
                    this.allJobSnapshots.refresh();
                } catch (IOException e) {
                    LOGGER.error("Updating all snapshots received IO Exception from JMX Connection Pool.  Resetting monitor.  Exception Message: "
                            + e.getLocalizedMessage());
                    resetTracker();
                }
            }

            if (allJobSnapshots.isLastSnapshotRefreshFailed()) {
                LOGGER.debug("updateAllJobSnapshots, isLastSnapshotRefreshFailed is true");
            } else {
                // We retrieved them successfully
                
                    // Get currently tracked jobs
                    currentJobIds = new HashSet<String>(jobMap.getJobIds());
                    
                    // Get the snapshot json
                String allSnapshots = this.allJobSnapshots.getAllSnapshots();

                // Parse and update each jobInfo
                if (allSnapshots != null) {

                    try {
                        JSONParser parser = new JSONParser();
                        JSONObject snapshotsObject = (JSONObject) parser
                                .parse(allSnapshots);
                        JSONArray jobArray = (JSONArray) snapshotsObject
                                .get("jobs");

                        for (int j = 0; j < jobArray.size(); j++) {
                            JSONObject jobObject = (JSONObject) jobArray.get(j);
                            String jobId = (String) jobObject.get("id");
                            String jobname = (String) jobObject.get("name");
                            JobDetails jd = jobMap.getJob(jobId);
                            if (jd != null) {
                                jd.setJobSnapshot(jobObject.toString());
                                // Remove it from our set we are using to check for jobs no longer existing
                                LOGGER.trace("Updated snapshot for jobId({}), removing from set used to track leftovers",jobId);
                                currentJobIds.remove(jobId);

                            } else {
                                LOGGER.info("Adding new job({}): {}", jobId, jobname);
                                addJobToMap(jobId,jobname,jobObject.toString());
                            }
                        }
                        
                        // Are there any jobs in the map that we did not get snapshots for?  Remove them
                        if (!currentJobIds.isEmpty()) {
                                LOGGER.trace("There are jobs in the job map that we did not receive a snapshot for, removing them...");
                                for (String jobId : currentJobIds) {
                                    LOGGER.warn("Removing JobId({})",jobId);
                                    removeJobFromMap(jobId);
                                }
                        }
                        
                    } catch (Exception e) {
                        LOGGER.error("Exception Parsing Snapsnots JSON...exiting");
                        LOGGER.error(e.toString());
                        e.printStackTrace();
                        System.exit(1);
                    }
                }
            }
        } else {
            LOGGER.error("Attempted to update snapshots but did not have an allJobSnapshots object available");
        }

        LOGGER.trace("Exit updateAllJobSnapshots");
    }    
    
    /********************************************************************************
     * updateAllJobMetrics
     * 
     * Triggered by: Refresh
     ********************************************************************************/
    private synchronized void updateAllJobMetrics(boolean refreshFromServer)
            throws StreamsTrackerException {
        LOGGER.trace("***** Entered updateAllJobMetrics, refreshFromServer {}",
                refreshFromServer);
        
        // Refresh Metrics if requested
        if (this.allJobMetrics != null) {
            if (refreshFromServer) {
                try {
                    this.allJobMetrics.refresh();
                } catch (IOException e) {
                    LOGGER.error("Updating all metrics received IO Exception from JMX Connection Pool.  Resetting monitor.  Exception Message: "
                            + e.getLocalizedMessage());
                    resetTracker();
                }
            }

            if (allJobMetrics.isLastMetricsRefreshFailed()) {
                LOGGER.debug("updateAllJobMetrics, isLastMetricsRefreshFailed is true");

            } else {
                // We retrieved them successfully
                String allMetrics = this.allJobMetrics.getAllMetrics();

                // Parse and update each jobInfo
                if (allMetrics != null) {

                    try {
                        JSONParser parser = new JSONParser();
                        JSONObject metricsObject = (JSONObject) parser
                                .parse(allMetrics);
                        JSONArray jobArray = (JSONArray) metricsObject
                                .get("jobs");
                        for (int j = 0; j < jobArray.size(); j++) {
                            JSONObject jobObject = (JSONObject) jobArray.get(j);
                            String jobId = (String) jobObject.get("id");
                            JobDetails jd = jobMap.getJob(jobId);
                            if (jd != null) {
                                jd.setJobMetrics(jobObject.toString());
                            } else {
                                LOGGER.warn(
                                        "Received Metrics for jobId({}) that is not found in the current job map, should be rectified by updateAllSnapshots, if it persists, report an issue.",
                                        jobId);
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.error("Exception Parsing Metrics JSON...exiting");
                        LOGGER.error(e.toString());
                        e.printStackTrace();
                        System.exit(1);
                    }
                }
            }
        } else {
            LOGGER.error("Attempted to update metrics but did not have an allJobMetrics object available");
        }

        LOGGER.trace("Exit updateAllJobMetrics");

    }
    
    
    
     
    
    
    public ServiceConfig getConfig() {
		return config;
	}


	public void setConfig(ServiceConfig config) {
		this.config = config;
	}


	public MetricsExporter getMetricsExporter() {
    	return metricsExporter;
    }
    
    public JmxServiceContext getContext() {
        return jmxContext;
    }

    public String getDomainName() {
        return domainName;
    }

    public synchronized boolean isAutoRefresh() {
        return autoRefresh;
    }

    public synchronized Long getInstanceResourceMetricsLastUpdated() {
        return instanceResourceMetricsLastUpdated;
    }

    public synchronized Map<String, String> getCurrentJobNameIndex() {
        return jobMap.getCurrentJobNameIndex();
    }

    public synchronized InstanceInfo getInstanceInfo() {
        //verifyInstanceExists();

        return instanceInfo;
    }

    private void verifyInstanceExists() throws StreamsTrackerException {
        if (instanceInfo == null) {
            throw new StreamsTrackerException(
                    StreamsTrackerErrorCode.INSTANCE_NOT_FOUND,
                    "The InstanceInfo object does not exist.  This error should not occur.");
        } else if (!this.instanceInfo.isInstanceExists()) {
            throw new StreamsTrackerException(
                    StreamsTrackerErrorCode.INSTANCE_NOT_FOUND,
                    "The Streams instance "
                            + this.instanceInfo.getInstanceName()
                            + " does not exist.");
        }
    }

    public synchronized Map<String, Map<String, Long>> getInstanceResourceMetrics() throws StreamsTrackerException {
        verifyInstanceExists();

        synchronized (instanceResourceMetrics) {
            return instanceResourceMetrics;
        }
    }

    
    /* Get Resource Metrics */
    private synchronized void updateInstanceResourceMetrics() throws StreamsTrackerException {
        verifyInstanceExists();

        MXBeanSource beanSource = null;
        
        Map<String, Map<String, Long>> prevInstanceResourceMetrics = new HashMap<String, Map<String, Long>>(instanceResourceMetrics);
                
        try {
            beanSource = jmxContext.getBeanSourceProvider().getBeanSource();

            InstanceMXBean instance = beanSource.getInstanceBean(domainName,
                this.instanceInfo.getInstanceName());

            Map<String, Set<Metric>> jmxResourceMetrics = instance.retrieveResourceMetrics(false);
            instanceResourceMetrics.clear();
            for (Map.Entry<String, Set<Metric>> jmxEntry : jmxResourceMetrics.entrySet()) {
                Map<String, Long> metrics = new HashMap<String, Long>();
                for (Metric m : jmxEntry.getValue()) {
                    metrics.put(m.getName(), m.getValue());
                }

                instanceResourceMetrics.put(jmxEntry.getKey(), metrics);
            }
            
            instanceResourceMetricsLastUpdated = System.currentTimeMillis();


            // Attempt to get resource status by retrieving each resourceMXBean
        
 
            Set<String> resourceIDs = instance.getResources();
            for (String resourceId : resourceIDs) {         
                ResourceMXBean resource = beanSource.getResourceBean(domainName, resourceId);
                ResourceMXBean.Status resourceStatus = resource.getStatus(this.instanceInfo.getInstanceName());
                metricsExporter.getStreamsMetric("status", StreamsObjectType.RESOURCE,
                this.domainName, this.instanceInfo.getInstanceName(), resourceId).set(getResourceStatusAsMetric(resourceStatus));
            }




        }
        catch (MalformedURLException me) {
            throw new StreamsTrackerException("Invalid JMX URL when retrieving instance bean", me);
        }
        catch (IOException ioe) {
            throw new StreamsTrackerException("JMX IO Exception when retrieving instance bean", ioe);
        }
        
        /* Process resource metrics for export */
        // Loop through old list and remove any not in the new list
        for (String key : prevInstanceResourceMetrics.keySet()) {
        	if (!instanceResourceMetrics.containsKey(key))
        		metricsExporter.removeAllChildStreamsMetrics(this.domainName,this.instanceInfo.getInstanceName(),key);
        }
        // Set exiting and new ones
        for (String resourceName : instanceResourceMetrics.keySet()) {
        	Map<String,Long> rmap = instanceResourceMetrics.get(resourceName);
        	for (String metricName : rmap.keySet()) {
				metricsExporter.getStreamsMetric(metricName,
						StreamsObjectType.RESOURCE,
						this.domainName,
						this.instanceInfo.getInstanceName(),
						resourceName).set((long)rmap.get(metricName));
        	}
        }
    }

    public synchronized AllJobMetrics getAllJobMetrics() throws StreamsTrackerException {

        if ((this.instanceInfo == null)
                || (!this.instanceInfo.isInstanceExists())) {
            throw new StreamsTrackerException(
                    StreamsTrackerErrorCode.ALL_METRICS_NOT_AVAILABLE,
                    "The Streams instance "
                            + this.instanceInfo.getInstanceName()
                            + " does not exist.");
        } else if (allJobMetrics == null) {
            throw new StreamsTrackerException(
                    StreamsTrackerErrorCode.ALL_METRICS_NOT_AVAILABLE,
                    "The allJobMetrics object does not exist. Metrics have never been able to be retrieved.");
        }

        return allJobMetrics;
    }
    
    public synchronized AllJobSnapshots getAllJobSnapshots() throws StreamsTrackerException {

        if ((this.instanceInfo == null)
                || (!this.instanceInfo.isInstanceExists())) {
            throw new StreamsTrackerException(
                    StreamsTrackerErrorCode.ALL_SNAPSHOTS_NOT_AVAILABLE,
                    "The Streams instance "
                            + this.instanceInfo.getInstanceName()
                            + " does not exist.");
        } else if (allJobMetrics == null) {
            throw new StreamsTrackerException(
                    StreamsTrackerErrorCode.ALL_SNAPSHOTS_NOT_AVAILABLE,
                    "The allJobSnapshots object does not exist. Snapshots have never been able to be retrieved.");
        }

        return allJobSnapshots;
    }    

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        String newline = System.getProperty("line.separator");

        result.append("Domain: " + domainName);
        result.append(newline);
        result.append("Instance: " + this.instanceInfo.getInstanceName()
                + ", status: " + this.instanceInfo.getInstanceStatus()
                + ", instanceStartTime: "
                + convertTime(this.instanceInfo.getInstanceStartTime()));
        result.append(newline);
        result.append("instanceAvailable:"
                + this.instanceInfo.isInstanceAvailable());
        result.append(newline);
        result.append("instanceResourceMetricsLastUpdated:" + convertTime(instanceResourceMetricsLastUpdated));
        result.append(newline);
        if (jobMap != null) {
        	result.append(jobMap.toString());
        }
        return result.toString();
    }

    private String convertTime(Long time) {
        if (time != null) {
            Date date = new Date(time);
            Format format = new SimpleDateFormat("yyyy MM dd HH:mm:ss");
            return format.format(date);
        } else {
            return "null";
        }
    }
    
    private void createExportedInstanceMetrics() {
        LOGGER.trace("createExportedInstanceMetrics...");
        LOGGER.trace("  INSTANCE INFO: " + this.getInstanceInfo().toString());
    	metricsExporter.createStreamsMetric("status", StreamsObjectType.INSTANCE, "Instance status, 1: running, .5: partially up, 0: stopped, failed, unknown");
    	metricsExporter.getStreamsMetric("status", StreamsObjectType.INSTANCE, this.domainName, this.instanceInfo.getInstanceName()).set(getInstanceStatusAsMetric());
    	metricsExporter.createStreamsMetric("health", StreamsObjectType.INSTANCE, "Instance health, 1: healthy, .5: partially healthy, 0: unhealthy, unknown");
		metricsExporter.createStreamsMetric("creationTime", StreamsObjectType.INSTANCE, "Epoch time in milliseconds when the instance was created");
    	metricsExporter.getStreamsMetric("creationTime", StreamsObjectType.INSTANCE, this.domainName, this.instanceInfo.getInstanceName()).set(instanceInfo.getInstanceCreationTime());
		metricsExporter.createStreamsMetric("startTime", StreamsObjectType.INSTANCE, "Epoch time in milliseconds when the instance was started");
    	metricsExporter.createStreamsMetric("jobCount", StreamsObjectType.INSTANCE, "Number of jobs currently deployed into the streams instance");
    }
    
    private void removeExportedInstanceMetrics() {
		metricsExporter.removeAllChildStreamsMetrics(this.domainName, this.instanceInfo.getInstanceName());
    }
    

    private double getInstanceStatusAsMetric() {
    	double value = 0;
    	switch (this.instanceInfo.getInstanceStatus()) {
    	case RUNNING :
    		value = 1;
    		break;
    	case STARTING:
    	case PARTIALLY_RUNNING:
    	case PARTIALLY_FAILED:
    	case STOPPING:
            value = 0.5;
            break;
    	default:
    		value = 0;
    	}
    	return value;
    }    

    private double getInstanceHealthAsMetric() {
    	double value = 0;
    	switch (this.instanceInfo.getInstanceHealth()) {
    	case HEALTHY :
    		value = 1;
    		break;
    	case PARTIALLY_HEALTHY:
    	case PARTIALLY_UNHEALTHY:
            value = 0.5;
            break;
    	default:
    		value = 0;
    	}
    	return value;
    }

    private double getResourceStatusAsMetric(ResourceMXBean.Status status) {
    	double value = 0;
    	switch (status) {
    	case RUNNING :
    		value = 1;
    		break;
    	case PARTIALLY_FAILED:
    	case PARTIALLY_RUNNING:
    	case QUIESCED:
        case QUIESCING:
        case RESTARTING:
        case RESUMING:
        case STARTING:
            value = 0.5;
            break;
    	default:
    		value = 0;
    	}
    	return value;
    }        
    
    // Should do whatever necessary to shutdown and close this object
    public void close() {
        this.removeExportedInstanceMetrics();
    }

    @Override
    public void beanSourceInterrupted(MXBeanSource bs) {
        LOGGER.warn("*** Streams Instance Tracker BeanSource interrupted, resetting streamsInstanceTracker ...");
        resetTracker();
    }
}
