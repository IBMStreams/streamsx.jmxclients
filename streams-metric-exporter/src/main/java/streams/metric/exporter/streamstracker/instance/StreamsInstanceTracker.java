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

import java.math.BigInteger;
import java.net.MalformedURLException;
import java.text.Format;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.AttributeChangeNotification;
import javax.management.NotificationFilterSupport;
import javax.management.ObjectName;
import javax.management.NotificationListener;
import javax.management.InstanceNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.apache.commons.lang.time.StopWatch;
import com.ibm.streams.management.ObjectNameBuilder;
import com.ibm.streams.management.OperationListenerMXBean;
import com.ibm.streams.management.OperationStatusMessage;
import com.ibm.streams.management.instance.InstanceMXBean;
import com.ibm.streams.management.job.JobMXBean;

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
import streams.metric.exporter.streamstracker.job.JobInfo;
import streams.metric.exporter.streamstracker.job.JobMap;
import streams.metric.exporter.streamstracker.metrics.AllJobMetrics;
import streams.metric.exporter.streamstracker.snapshots.AllJobSnapshots;
import com.ibm.streams.management.Metric;
import com.ibm.streams.management.Notifications;

/*
 * StreamsInstanceTracker
 *  Initialization
 *  		* Get InstanceMXBean
 *  		* Register for JMX Notifications
 *  		* Create InstanceInfo object
 *  Refresh
 *  		* Get Metrics Snapshot
 *  		* Get Jobs Snapshot
 *  		* Update JobMap using snapshot lists
 *  Notification of Instance change
 *  		* Update Instance Info
 */
public class StreamsInstanceTracker implements NotificationListener, MXBeanSourceProviderListener {
    private static final Logger LOGGER = LoggerFactory.getLogger("root."
            + StreamsInstanceTracker.class.getName());

    private ServiceConfig config = null;
    private JmxServiceContext jmxContext;
    private String protocol;
    private boolean autoRefresh;

    /* Domain info */
    private String domainName = null;

    /* Instance info */
    private InstanceInfo instanceInfo = new InstanceInfo();

    /* Job Metrics Info */
    private AllJobMetrics allJobMetrics = null;
    private boolean metricsAvailable = false;
    
    /* Job Snapshots Info */
    private AllJobSnapshots allJobSnapshots = null;
    private boolean snapshotsAvailable = false;

    private boolean jobsAvailable = false;

    private final Map<String, Map<String, Long>> instanceResourceMetrics = new HashMap<String, Map<String, Long>>();
    private Long instanceResourceMetricsLastUpdated = null;
    
    /*****************************************
     * Metrics Exporter for non REST JSON 
     **************************************/
    // Future change to plugin
	private MetricsExporter metricsExporter = PrometheusMetricsExporter.getInstance();

    /*****************************************
     * JOB MAP and INDEXES
     **************************************/
    /* Job Map Info */
    //private JobMap jobMap = new JobMap(this.instanceInfo.getInstanceName());
    private JobMap jobMap = null;
    //private ConcurrentSkipListMap<BigInteger, JobDetails> jobMap = new ConcurrentSkipListMap<BigInteger, JobDetails>();
    //private ConcurrentSkipListMap<String, BigInteger> jobNameIndex = new ConcurrentSkipListMap<String, BigInteger>();


    /**************************************************************************
     * Constructor
     * 
     * Note: InstanceNotFoundException is a jmx exception we use not to be
     * confused with streams instance
     ***************************************************************************/
    public StreamsInstanceTracker(JmxServiceContext jmxContext,
            String domainName, String instanceName, boolean autoRefresh,
            String protocol, ServiceConfig config) throws StreamsTrackerException {
        LOGGER.debug("** Initializing StreamsInstanceTracker for: " + instanceName);
        this.config = config;
        this.jmxContext = jmxContext;
        this.domainName = domainName;
        this.instanceInfo.setInstanceName(instanceName);
        this.autoRefresh = autoRefresh;
        this.protocol = protocol;
        this.jmxContext.getBeanSourceProvider().addBeanSourceProviderListener(this);
        jobMap = new JobMap(instanceName);

        initStreamsInstance();

        if (this.instanceInfo.isInstanceAvailable()) {
            updateInstanceResourceMetrics();
            initAllJobs();
        }

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
		LOGGER.trace("    current state: isInstanceAvailable: {}, jobsAvailable: {}, metricsAvailable {}, snapshotsAvailable {}",this.instanceInfo.isInstanceAvailable(), jobsAvailable, metricsAvailable, snapshotsAvailable);

        StopWatch stopwatch = null;
        if (LOGGER.isDebugEnabled()) {
            stopwatch = new StopWatch();
            stopwatch.reset();
            stopwatch.start();
        }
		
		if (!this.instanceInfo.isInstanceAvailable()) {
        	LOGGER.trace("** Calling initStreamsInstance()");
            initStreamsInstance();
        }

        if (instanceInfo.isInstanceAvailable()) {
        	LOGGER.trace("** Calling updateInstanceResourceMetrics()");
        	metricsExporter.getStreamsMetric("jobCount", StreamsObjectType.INSTANCE,
        			this.domainName,
        			this.instanceInfo.getInstanceName()).set(jobMap.size());
            updateInstanceResourceMetrics();
        }

        // Need to know if instance was unavailable on previous run so we
        // re-initialize jobs
        if (!jobsAvailable) {
        	LOGGER.trace("** Calling initAllJobs()");
            initAllJobs();
        }

        if (snapshotsAvailable) {
        	LOGGER.trace("** Calling updateAllJobSnapshots(true)");
            updateAllJobSnapshots(true);
        }
        

        if (metricsAvailable) {
        	LOGGER.trace("** Calling updateAllJobMetrics(true)");
            updateAllJobMetrics(true);
        }
        
        if (LOGGER.isDebugEnabled()) {
            stopwatch.stop();
            LOGGER.debug("StreamsInstanceTracker(" + this.instanceInfo.getInstanceName() + ") Refresh Time (ms) :" + stopwatch.getTime());              
        }
        
    }

    /*******************************************************************************
     * INIT STREAMS INSTANCE 
     * 
     * Get The Instance Bean
     * 
     * Setup Notifications
     * 
     *******************************************************************************/
    private synchronized void initStreamsInstance() throws StreamsTrackerException {
        MXBeanSource beanSource = null;
        try {

            beanSource = jmxContext.getBeanSourceProvider().getBeanSource();

            // Determines if the instance exists
            // If not, assume it is coming and try again on the
            // next scheduled refresh
            InstanceMXBean instance = beanSource.getInstanceBean(domainName,
                    this.instanceInfo.getInstanceName());

            if (instance == null) {
                LOGGER.warn(
                        "Instance '{}' not found.  Continuing assuming it will be created in the future",
                        this.instanceInfo.getInstanceName());
                resetTracker();
                return;
            }

            this.instanceInfo.setInstanceExists(true);
            this.instanceInfo.setInstanceStatus(instance.getStatus());
            this.instanceInfo.setInstanceStartTime(instance.getStartTime());

            // If instanceStartTime is null, then instance not ready, do not
            // need to
            // deal with individual statuses at this time
            if (this.instanceInfo.getInstanceStartTime() == null) {
                LOGGER.warn(
                        "Instance '{}' found, but is not started.  Current Status: {}",
                        new Object[] { this.instanceInfo.getInstanceName(),
                                this.instanceInfo.getInstanceStatus() });
                resetTracker();
                return;
            } else {
                // Force jobs and metrics to initialize by setting instance as
                // available
                LOGGER.info("Streams Instance '{}' found, Status: {}", new Object[] {
                        instance.getName(), instance.getStatus() });
                this.instanceInfo.setInstanceAvailable(true);
                jobsAvailable = false;
                metricsAvailable = false;
            }

            // Setup notifications (should handle exceptions)
            ObjectName instanceObjName = ObjectNameBuilder.instance(domainName,
                    this.instanceInfo.getInstanceName());
            NotificationFilterSupport filter = new NotificationFilterSupport();
            filter.disableAllTypes();
            filter.enableType(AttributeChangeNotification.ATTRIBUTE_CHANGE);
            filter.enableType(Notifications.JOB_ADDED);
            filter.enableType(Notifications.JOB_REMOVED);
            filter.enableType(Notifications.INSTANCE_DELETED); // Will tell us
                                                               // if the
                                                               // instance we
                                                               // are
                                                               // monitoring is
                                                               // deleted

            // Create notification listener for new jobs, if it fails, we need
            // to reset state
            // so that instance is initialized again assuming this is a
            // temporary JMX issue.
            // Remove just incase it is already set
            try {
                beanSource.getMBeanServerConnection()
                        .removeNotificationListener(instanceObjName, this);
            } catch (Exception e) {
                // Ignore because we do not care if this fails
            }
            beanSource.getMBeanServerConnection().addNotificationListener(
                    instanceObjName, this, filter, null);

        } catch (UndeclaredThrowableException e) {
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
            // Some InstanceNotFoundExceptions are wrapped in
            // UndeclaredThrowableExceptions sadly

        } catch (InstanceNotFoundException infe) {
            LOGGER.warn(
                    "Instance '{}' not found when initializing.  Continuing assuming it will be created in the future",
                    this.instanceInfo.getInstanceName());
            this.instanceInfo.setInstanceExists(false);
            resetTracker();
            // throw new StreamsMonitorException("Instance MXBean not found when
            // initializing instance");
        } catch (MalformedURLException me) {
            resetTracker();
            throw new StreamsTrackerException(
                    "Invalid JMX URL when initializing instance", me);
        } catch (IOException ioe) {
            // JMX Error, cannot initialize streams instance so ensure state
            // variables reflect and return
            LOGGER.warn("JMX IO Exception when initializing instance, Continuing to wait for reconnect");
            resetTracker();
        }
        createExportedInstanceMetrics();
    }

    /*******************************************************************************
     * INIT ALL JOBS 
     * 
     * Get The Job Beans (initJobMap)
     * Initialize Metrics
     * Initialize Snapshots
     * 
     *******************************************************************************/
    private synchronized void initAllJobs() throws StreamsTrackerException {

        initJobMap();

        if (this.instanceInfo.isInstanceAvailable() && jobsAvailable) {

            StopWatch stopwatch = null;
            LinkedHashMap<String, Long> timers = null;
            if (LOGGER.isDebugEnabled()) {
                stopwatch = new StopWatch();
                timers = new LinkedHashMap<String, Long>();
                stopwatch.reset();
                stopwatch.start();
            }
            
            
            // Initialize Snapshots
            try {
                // Try to only create it if it does not exist and rely on the
                // clearing of the metrics so we preserve our timing attributes
                if (allJobSnapshots == null) {
                    allJobSnapshots = new AllJobSnapshots(this.jmxContext,
                            this.domainName,
                            this.instanceInfo.getInstanceName(),
                            this.config.getJmxHttpHost(),
                            this.config.getJmxHttpPort());
                } else {
                    allJobSnapshots.clear();
                }

                if (LOGGER.isDebugEnabled()) {
                    stopwatch.stop();
                    timers.put("Initialize AllJobSnapshots", stopwatch.getTime());
                    stopwatch.reset();
                    stopwatch.start();
                }

                // Assume available, may need a more detailed check in the
                // future
                snapshotsAvailable = true;

                // Do not refresh from server because the AllJobSnapshots constructor pulled
                // them
                if (snapshotsAvailable) {
                    updateAllJobSnapshots(false);
                }
            } catch (IOException e) {
                LOGGER.warn("JMX IO Exception when initializing all job snapshots, resetting monitor. Exception message: "
                        + e.getLocalizedMessage());
                resetTracker();
            }           

            if (LOGGER.isDebugEnabled()) {
                stopwatch.stop();
                timers.put("updateAllJobSnapshots(false)", stopwatch.getTime());
                stopwatch.reset();
                stopwatch.start();
            }            
            
            // Initialize Metrics
            try {
                // Try to only create it if it does not exist and rely on the
                // clearing of the metrics so we preserve our timing attributes
                if (allJobMetrics == null) {
                    allJobMetrics = new AllJobMetrics(this.jmxContext,
                            this.domainName,
                            this.instanceInfo.getInstanceName(),
                            this.config.getJmxHttpHost(),
                            this.config.getJmxHttpPort());
                } else {
                    allJobMetrics.clear();
                }

                if (LOGGER.isDebugEnabled()) {
                    stopwatch.stop();
                    timers.put("Initialize AllJobMetrics", stopwatch.getTime());
                    stopwatch.reset();
                    stopwatch.start();
                }

                // Assume available, may need a more detailed check in the
                // future
                metricsAvailable = true;

                // Do not refresh from server because the AllJobMetrics constructor pulled
                // them
                if (metricsAvailable) {
                    updateAllJobMetrics(false);
                }
            } catch (IOException e) {
                LOGGER.warn("JMX IO Exception when initializing all job metrics, resetting monitor. Exception message: "
                        + e.getLocalizedMessage());
                resetTracker();
            }
            
            
            if (LOGGER.isDebugEnabled()) {
                stopwatch.stop();
                timers.put("updateAllJobMetrics(false)", stopwatch.getTime());
                LOGGER.debug("** INSTANCE Initialize Job snapshots and metrics timing (ms):");
                for (Map.Entry<String, Long> entry : timers.entrySet()) {
                    LOGGER.debug("   " + entry.getKey() + " timing: "
                            + entry.getValue());
                }
            }

        }

    }

    /*******************************************************************************
     * INIT JOB MAP 
     * 
     * Get The Job Beans
     * 
     * Put them into the Job Map
     * 
     *******************************************************************************/

    private synchronized void initJobMap() {
        String domainName = this.domainName;
        String instanceName = this.instanceInfo.getInstanceName();
        InstanceMXBean instance = null;

        if (this.instanceInfo.isInstanceAvailable()) {

            StopWatch stopwatch = null;
            LinkedHashMap<String, Long> timers = null;
            if (LOGGER.isDebugEnabled()) {
                stopwatch = new StopWatch();
                timers = new LinkedHashMap<String, Long>();
            }

            try {

                instance = jmxContext.getBeanSourceProvider().getBeanSource()
                        .getInstanceBean(domainName, instanceName);

                // Get list of Jobs
                if (LOGGER.isDebugEnabled()) {
                    stopwatch.reset();
                    stopwatch.start();
                }

                LOGGER.debug("Instance ({}): Get list of all jobs...", this.getInstanceInfo().getInstanceName());
                Set<BigInteger> jobs = null;

                jobs = jmxContext.getBeanSourceProvider().getBeanSource()
                        .getInstanceBean(domainName, instanceName).getJobs();

                if (LOGGER.isDebugEnabled()) {
                    stopwatch.stop();
                    timers.put("JMX.getJobs", stopwatch.getTime());
                    stopwatch.reset();
                    stopwatch.start();
                }

                ObjectName opListenerObj = instance.createOperationListener();
                OperationListenerMXBean opListener = JMX.newMXBeanProxy(
                        jmxContext.getBeanSourceProvider().getBeanSource()
                                .getMBeanServerConnection(), opListenerObj,
                        OperationListenerMXBean.class, true);
                instance.registerAllJobs(opListener.getId());

                // *** Wait for all jobs to be registered *** //
                LOGGER.debug("Instance ({}): Waiting for jobs to be registered...", this.getInstanceInfo().getInstanceName());
                int numMessages = 0;
                boolean registerCompleted = false;
                boolean registerError = false;
                List<OperationStatusMessage> messageList = new ArrayList<OperationStatusMessage>();
                while (!registerCompleted && !registerError) {
                    // System.out.println("opListener.getMessages()...");
                    // Found issue in testing when messages were not ready, and
                    // tried to
                    // get them again
                    // it raised a ConcurrentModificationException, but if we
                    // ignore
                    // and
                    // try again, everything
                    // works. So eat it for now.
                    try {
                        messageList = opListener.getMessages();
                    } catch (java.util.ConcurrentModificationException e) {
                    }

                    numMessages = messageList.size();

                    if (numMessages > 0) {
                        for (OperationStatusMessage osm : messageList) {
                            if (osm.getStatus() == OperationListenerMXBean.Status.COMPLETED)
                                registerCompleted = true;
                            if (osm.getStatus() == OperationListenerMXBean.Status.ERROR)
                                registerError = true;
                        }
                    }
                }

                opListener.unregister();
                
                
                // Verify that jobs are registered
                // Found issue in Streams 4.2 where the message said COMPLETED
                // but jobs were not truly registered
                LOGGER.trace("Verifing all jobs show isRegistered() true...");
                MBeanServerConnection mbsc = null;
					      mbsc = jmxContext.getBeanSourceProvider().getBeanSource().getMBeanServerConnection();
                for (BigInteger jobno : jobs) {
                    LOGGER.trace("Verifying job #{}...",jobno);
                    ObjectName tJobNameObj = ObjectNameBuilder.job(domainName,
                            instanceName, jobno);   
                    while (! mbsc.isRegistered(tJobNameObj)) {
                        LOGGER.trace("...NOT registered, trying again...");
                    }
                    LOGGER.trace("...Registered!");             
                }
                
                

                if (LOGGER.isDebugEnabled()) {
                    stopwatch.stop();
                    timers.put("JMX.registerJobs", stopwatch.getTime());
                    stopwatch.reset();
                    stopwatch.start();
                }

                // Populate the map of jobs and create jobInfo objects with
                // jobMXBeans
                // Create the jobname index
                jobMap.clear();

                LOGGER.trace("Create hashmap of JobMXBeans...");
                for (BigInteger jobno : jobs) {
                    ObjectName tJobNameObj = ObjectNameBuilder.job(domainName,
                            instanceName, jobno);
                    JobMXBean jobBean = JMX.newMXBeanProxy(jmxContext
                            .getBeanSourceProvider().getBeanSource()
                            .getMBeanServerConnection(), tJobNameObj,
                            JobMXBean.class, true);
                    jobMap.addJobToMap(jobno, new JobDetails(this, jobno, jobBean));
                }

                // IMPORTANT: Set jobsAvailable to true
                jobsAvailable = true;

            } catch (IOException e) {
                // An IOException at this point means the jmx connection is
                // probably lost. Reset Monitor and continue to wait for it to
                // reconnect
                LOGGER.warn("Job Map Initialization received IO Exception from JMX Connection Pool.  Resetting monitor.  Exception Message: "
                        + e.getLocalizedMessage());
                resetTracker();
            }

            if (LOGGER.isDebugEnabled()) {
                stopwatch.stop();
                timers.put("create JobMXBeans Hashmap", stopwatch.getTime());
                LOGGER.debug("** INSTANCE Initializing jobs and job map timing (ms)");
                for (Map.Entry<String, Long> entry : timers.entrySet()) {
                    LOGGER.debug("   " + entry.getKey() + " timing: "
                            + entry.getValue());
                }
            }
        }

    }
    
    /****************************************************************************
     * resetMonitor In the case of a JMX error or anything else that could have
     * invalidated our state reset the state so that the instance, jobs, and
     * metrics are re-initialized and brought back into consistency with
     * Streams.
     ****************************************************************************/
    public synchronized void resetTracker() {
        this.instanceInfo.setInstanceAvailable(false);
        this.jobsAvailable = false;
        this.metricsAvailable = false;
        this.snapshotsAvailable = false;
        // Set Metrics Failure on metrics Object
        if (this.allJobMetrics != null) {
        	this.allJobMetrics.setLastMetricsFailure(new Date());
        	this.allJobMetrics.setLastMetricsRefreshFailed(true);
        }
        // Set Snapshot Failure on metrics Object
        if (this.allJobSnapshots != null) {
        	this.allJobSnapshots.setLastSnapshotFailure(new Date());
        	this.allJobSnapshots.setLastSnapshotRefreshFailed(true);
        }
        removeExportedInstanceMetrics();
        createExportedInstanceMetrics();
    }
    
    /*****************************************************************************
     * clearTracker In the case that the Streams instance is stopped/fails we
     * will not be able to recover the metrics or jobs so clear them out
     *****************************************************************************/
    private synchronized void clearTracker() {
        instanceResourceMetrics.clear();
        removeExportedInstanceMetrics();
        createExportedInstanceMetrics();
        this.allJobMetrics.clear();
        this.allJobSnapshots.clear();
        this.jobMap.clear();
    }
    
    /*****************************************************************************
     * Instance handleNotification
     * 
     * Primary interface to listen for changes to the instance we are monitoring
     * Only interested in specific notifications so filter was used
     *****************************************************************************/
    public void handleNotification(Notification notification, Object handback) {
	    	try {
	    		String notificationType = notification.getType();
	    		LOGGER.trace("Streams Instance ({}) Notification: {}; User Data: {}", this.getInstanceInfo().getInstanceName(), notification, notification.getUserData());
	
	    		switch (notificationType) {
	    		case AttributeChangeNotification.ATTRIBUTE_CHANGE:
	    			AttributeChangeNotification acn = (AttributeChangeNotification) notification;
	    			String attributeName = acn.getAttributeName();
	    			if (attributeName.equals("Status")) {
	    				InstanceMXBean.Status newValue = (InstanceMXBean.Status) acn
	    						.getNewValue();
	    				InstanceMXBean.Status oldValue = (InstanceMXBean.Status) acn
	    						.getOldValue();
	    				LOGGER.info("Streams Instance ({}) Status Changed from: {} to: {}", this.getInstanceInfo().getInstanceName(), oldValue, newValue);
	    				this.instanceInfo.setInstanceStatus((InstanceMXBean.Status) acn
	    						.getNewValue());
	    				if (newValue.equals(InstanceMXBean.Status.STOPPED)
	    						|| newValue.equals(InstanceMXBean.Status.FAILED)
	    						|| newValue.equals(InstanceMXBean.Status.UNKNOWN)) {
	    					LOGGER.info("Instance ({}) Status reflects not availabe status ({}), instance tracker will reset and reinitialize when instance is available", this.getInstanceInfo().getInstanceName(), newValue);
	    					this.instanceInfo.setInstanceStartTime(null);
	    					resetTracker();
	    					clearTracker();
	    					metricsExporter.getStreamsMetric("status", StreamsObjectType.INSTANCE, this.domainName, this.instanceInfo.getInstanceName()).set(getInstanceStatusAsMetric());
	    				}
	    			}
	    			break;
		        case Notifications.INSTANCE_DELETED:
		            LOGGER.debug("Instance ({}) deleted from domain, resetting monitor and waiting for instance to be recreated", this.getInstanceInfo().getInstanceName());
		            this.instanceInfo.setInstanceExists(false);
		            resetTracker();
		            clearTracker();
		            break;
		        case Notifications.JOB_ADDED:
		            LOGGER.debug("** INSTANCE ({}) Job added notification, Jobid: {}", this.getInstanceInfo().getInstanceName(), notification.getUserData());
		            addJobToMap((BigInteger) notification.getUserData());
		            break;
		        case Notifications.JOB_REMOVED:
		            LOGGER.debug("** INSTANCE ({}) Job removed notification, Jobid: {}", this.getInstanceInfo().getInstanceName(), notification.getUserData());
		            // We are only listening on Straems Instance, so user data is a
		            // jobid
		            removeJobFromMap((BigInteger) notification.getUserData());
		            break;
		        }
	    	} catch (Exception e) {
	    		LOGGER.error("Instance ({}) Notification Handler caught exception: {}",this.getInstanceInfo().getInstanceName(),e.toString());
	    		e.printStackTrace();
	    	}
    }
    
    /***********************************************************
     * Add Job to job map
     ***********************************************************/
    private synchronized void addJobToMap(BigInteger jobid) {
        InstanceMXBean instance = null;
        LOGGER.debug("AddJobToMap({})...", jobid);
        StopWatch sw = new StopWatch();
        sw.start();

        try {
            instance = this.jmxContext
                    .getBeanSourceProvider()
                    .getBeanSource()
                    .getInstanceBean(domainName,
                            this.instanceInfo.getInstanceName());

            ObjectName tJobNameObj = instance.registerJob(jobid);

            JobMXBean jobBean = JMX.newMXBeanProxy(jmxContext
                    .getBeanSourceProvider().getBeanSource()
                    .getMBeanServerConnection(), tJobNameObj, JobMXBean.class,
                    true);
            jobMap.addJobToMap(jobid, new JobDetails(this, jobid, jobBean));

        } catch (IOException e) {
            LOGGER.warn("New Job Initialization received IO Exception from JMX Connection Pool.  Resetting monitor.  Exception Message: "
                    + e.getLocalizedMessage());
            resetTracker();
        }

        sw.stop();
        LOGGER.debug("** addJobToMap (jobid: " + jobid + ") time: "
                + sw.getTime());
        
		metricsExporter.getStreamsMetric("jobCount", StreamsObjectType.INSTANCE, this.domainName, this.instanceInfo.getInstanceName()).set(jobMap.size());

    }

    /***********************************************************
     * Remove Job from job map
     ***********************************************************/

    private synchronized void removeJobFromMap(BigInteger jobid) {
        LOGGER.debug("removeJobFromMap({})...", jobid);
        StopWatch sw = new StopWatch();
        sw.start();

        // Do we need to do anything to the MXBean we created for the job?
        jobMap.removeJobFromMap(jobid);

        sw.stop();
        LOGGER.debug("** removeJobFromMap (jobid: " + jobid + ") time: "
                + sw.getTime());
        
		metricsExporter.getStreamsMetric("jobCount", StreamsObjectType.INSTANCE, this.domainName, this.instanceInfo.getInstanceName()).set(jobMap.size());

    }       
    
    /********************************************************************************
     * updateAllJobSnapshots
     * 
     * Triggered by: Refresh
     * Need to use this to determine when JMX has been reset
     * so we invalidate the job beans
     * 
     * IMPORTANT: We use this as a backup for notifications
     * If a jmx notification of new job or deleted job is missed
     * we need to know about that and we do not trust JMX interfaces completely
     * based on production usage.  Therefore, we will allow discrepancies
     * in the snapshot list to drive adding/removing jobs as well.
     * 
     ********************************************************************************/
    private synchronized void updateAllJobSnapshots(boolean refreshFromServer)
            throws StreamsTrackerException {
        LOGGER.trace("***** Entered updateAllJobSnapshots, refreshFromServer {}, jobsAvailable {}",
                refreshFromServer, jobsAvailable);
        
        // Current Job IDs for use in determine missing jobs or jobs that need to be removed
        Set<BigInteger> currentJobIds = null;
        
        if (jobsAvailable) {
            LOGGER.trace("** updateAllJobSnapshots Start timer...");
            StopWatch sw = new StopWatch();
            sw.reset();
            sw.start();
            
            // Refresh Snapshots if requested
            if (refreshFromServer) {
                try {
                    this.allJobSnapshots.refresh();
                } catch (IOException e) {
                    LOGGER.error("Updating all snapshots received IO Exception from JMX Connection Pool.  Resetting monitor.  Exception Message: "
                            + e.getLocalizedMessage());
                    resetTracker();
                }
                sw.split();
                LOGGER.trace("** updateAllJobSnapshots refresh from server split time: " + sw.getSplitTime());
                sw.unsplit();
            }

            if (allJobSnapshots.isLastSnapshotRefreshFailed()) {
                // If retrieving snapshots fails, we need to loop through the jobs
                // and set the attributes to reflect that
            	jobMap.setJobSnapshotFailed(allJobSnapshots.getLastSnapshotFailure());

            } else {
                // We retrieved them successfully
            	
            	    // Get currently tracked jobs
            		currentJobIds = new HashSet<BigInteger>(jobMap.getJobIds());
            		
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
                            BigInteger jobId = new BigInteger(
                                    (String) jobObject.get("id"));
                            JobDetails jd = jobMap.getJob(Integer.parseInt((String)jobObject.get("id")));
                            if (jd != null) {
                                jd.setJobSnapshot(jobObject.toString());
                                // Update the job details that we refreshed
                                // metrics
                                jd.setLastSnapshotRefresh(allJobSnapshots
                                        .getLastSnaphostRefresh());
                                jd.setLastSnapshotFailure(allJobSnapshots
                                        .getLastSnapshotFailure());
                                jd.setLastSnapshotRefreshFailed(false);
                                // Remove it from our set we are using to check for jobs no longer existing
                                LOGGER.trace("Updated snapshot for jobId({}), removing from set used to track leftovers",jobId);
                                currentJobIds.remove(jobId);

                            } else {
                                LOGGER.warn(
                                        "Received Snapsbhots for jobId({}) that is not found in the current jobArray, missed notification of new job is likely cause, adding to job Map",
                                        jobId);
                                addJobToMap(jobId);
                            }
                        }
                        
                        // Are there any jobs in the map that we did not get snapshots for?  Remove them, we must have missed notification
                        if (!currentJobIds.isEmpty()) {
                        		LOGGER.warn("There are jobs in the job map that we did not receive a snapshot for, removing them...");
                        		for (BigInteger jobId : currentJobIds) {
                        			LOGGER.warn("JobId({}) was not in the list of snapshots, removing from job Map, possible cause, missed notification.",jobId);
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
            sw.stop();
            LOGGER.trace("** updateAllJobSnapshots time total (includes parsing) (milliseconds): " + sw.getTime());
        }


        LOGGER.trace("Exited");

    }    
    
    /********************************************************************************
     * updateAllJobMetrics
     * 
     * Triggered by: Refresh
     * Need to use this to determine when JMX has been reset
     * so we invalidate the job beans
     ********************************************************************************/
    private synchronized void updateAllJobMetrics(boolean refreshFromServer)
            throws StreamsTrackerException {
        LOGGER.trace("***** Entered updateAllJobMetrics, refreshFromServer {}, jobsAvailable {}",
                refreshFromServer, jobsAvailable);
        
        if (jobsAvailable) {
            LOGGER.trace("** updateAllJobMetrics Start timer...");
            StopWatch sw = new StopWatch();
            sw.reset();
            sw.start();
            
            // Refresh Metrics if requested
            if (refreshFromServer) {
                try {
                    this.allJobMetrics.refresh();
                } catch (IOException e) {
                    LOGGER.error("Updating all metrics received IO Exception from JMX Connection Pool.  Resetting monitor.  Exception Message: "
                            + e.getLocalizedMessage());
                    resetTracker();
                }
                sw.split();
                LOGGER.trace("** updateAllJobMetrics refresh from server split time: " + sw.getSplitTime());
                sw.unsplit();
            }

            if (allJobMetrics.isLastMetricsRefreshFailed()) {
                // If retrieving metrics fails, we need to loop through the jobs
                // and set the attributes to reflect that
            	jobMap.setJobMetricsFailed(allJobMetrics.getLastMetricsFailure());

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
                            BigInteger jobId = new BigInteger(
                                    (String) jobObject.get("id"));
                            JobDetails jd = jobMap.getJob(Integer.parseInt((String)jobObject.get("id")));
                            if (jd != null) {
                                jd.setJobMetrics(jobObject.toString());
                                // Update the job details that we refreshed
                                // metrics
                                jd.setLastMetricsRefresh(allJobMetrics
                                        .getLastMetricsRefresh());
                                jd.setLastMetricsFailure(allJobMetrics
                                        .getLastMetricsFailure());
                                jd.setLastMetricsRefreshFailed(false);
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
            sw.stop();
            LOGGER.trace("** updateAllJobMetrics time total (includes parsing) (milliseconds): " + sw.getTime());
        }


        LOGGER.trace("Exited");

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

    public synchronized boolean jobsAvailable() {
        return jobsAvailable;
    }

    public synchronized boolean metricsAvailable() {
        return metricsAvailable;
    }
    
    public synchronized boolean snapshotsAvailable() {
    	return snapshotsAvailable;
    }

    public synchronized boolean isAutoRefresh() {
        return autoRefresh;
    }

    public synchronized Long getInstanceResourceMetricsLastUpdated() {
        return instanceResourceMetricsLastUpdated;
    }

    public synchronized Map<BigInteger, JobInfo> getCurrentJobMap() {
    	return jobMap.getJobMap();
    }

    public synchronized Map<String, BigInteger> getCurrentJobNameIndex() {
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
    /* FUTURE: need to be notified of resources coming and going */
    /* For now, we will quickly just use a delta between this time and last time */
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

    public synchronized ArrayList<JobInfo> getAllJobInfo() throws StreamsTrackerException {
        ArrayList<JobInfo> jia = null;

        if ((this.instanceInfo == null)
                || (!this.instanceInfo.isInstanceExists())) {
            throw new StreamsTrackerException(
                    StreamsTrackerErrorCode.ALL_JOBS_NOT_AVAILABLE,
                    "The Streams instance "
                            + this.instanceInfo.getInstanceName()
                            + " does not exist.");
        }

        if (jobsAvailable) {
        	jia = jobMap.getJobInfo();
        } else {
        	// empty array
        	jia = new ArrayList<JobInfo>();
        }
        return jia;
    }

    public synchronized JobInfo getJobInfo(int jobid) throws StreamsTrackerException {
        JobInfo ji = null;

        ji = jobMap.getJobInfo(jobid);
        if (ji == null) {
            throw new StreamsTrackerException(
                    StreamsTrackerErrorCode.JOB_NOT_FOUND, "Job id " + jobid
                            + " does not exist");
        }
        
        return ji;
    }

    // Single job snapshot on demand
    // Was used before we started caching snapshots to get PE launchCounts
    public synchronized String getJobSnapshot(int jobid, int maximumDepth,
            boolean includeStaticAttributes) throws StreamsTrackerException {
        JobDetails jd = jobMap.getJob(jobid);

        if (jd == null) {
            throw new StreamsTrackerException(
                    StreamsTrackerErrorCode.JOB_NOT_FOUND, "Job id " + jobid
                            + " does not exist");
        }

        return jd.getSnapshot(maximumDepth, includeStaticAttributes);
    }


	String getProtocol() {
        return protocol;
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
        result.append("jobMapAvailable:" + jobsAvailable);
        result.append(newline);
        result.append("jobMetricsAvailable:" + metricsAvailable);
        result.append(newline);
        result.append("jobSnapshotsAvailable:" + snapshotsAvailable);
        result.append(newline);        
        result.append("instanceResourceMetricsLastUpdated:" + convertTime(instanceResourceMetricsLastUpdated));
        result.append(newline);
        if (jobsAvailable) {
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

//    public void printJob(BigInteger jobid) {
//    	
//        System.out.println("Job Status: " + jobMap.getJob(jobid).getStatus());
//        System.out.println("Job Metrics: " + jobMap.getJob(jobid).getJobMetrics());
//    }


    
    private void createExportedInstanceMetrics() {
    	metricsExporter.createStreamsMetric("status", StreamsObjectType.INSTANCE, "Instance status, 1: running, .5: partially up, 0: stopped, failed, unknown");
    	metricsExporter.getStreamsMetric("status", StreamsObjectType.INSTANCE, this.domainName, this.instanceInfo.getInstanceName()).set(getInstanceStatusAsMetric());
    	metricsExporter.createStreamsMetric("jobCount", StreamsObjectType.INSTANCE, "Number of jobs currently deployed into the streams instance");
    	metricsExporter.getStreamsMetric("jobCount", StreamsObjectType.INSTANCE, this.domainName, this.instanceInfo.getInstanceName()).set(0);
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
    	default:
    		value = 0;
    	}
    	return value;
    }
    
    // Should do whatever necessary to shutdown and close this object
    public void close() {}

    @Override
    public void beanSourceInterrupted(MXBeanSource bs) {
        LOGGER.debug("***** Streams Instance Tracker BeanSource interrupted, resetting monitor...");
        resetTracker();
    }
}
