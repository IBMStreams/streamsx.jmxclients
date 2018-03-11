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

package streams.metric.exporter.streamstracker;

import java.net.MalformedURLException;
import java.util.TimerTask;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigInteger;

import javax.management.Notification;
import javax.management.AttributeChangeNotification;
import javax.management.NotificationFilterSupport;
import javax.management.ObjectName;
import javax.management.NotificationListener;
import javax.management.InstanceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ibm.streams.management.ObjectNameBuilder;
import com.ibm.streams.management.domain.DomainMXBean;
import streams.metric.exporter.Constants;
import streams.metric.exporter.error.StreamsTrackerErrorCode;
import streams.metric.exporter.error.StreamsTrackerException;
import streams.metric.exporter.jmx.JmxServiceContext;
import streams.metric.exporter.jmx.MXBeanSource;
import streams.metric.exporter.jmx.MXBeanSourceProviderListener;
import streams.metric.exporter.metrics.MetricsExporter;
import streams.metric.exporter.metrics.MetricsExporter.StreamsObjectType;
import streams.metric.exporter.prometheus.PrometheusMetricsExporter;
import streams.metric.exporter.streamstracker.instance.InstanceTrackerMap;
import streams.metric.exporter.streamstracker.instance.StreamsInstanceTracker;
import streams.metric.exporter.streamstracker.job.JobInfo;

import com.ibm.streams.management.Notifications;

/*
 * StreamsDomainTracker
 *  Listens for Domain notifications to help update its status
 *  Has a periodic refresh() to also update status and retrieve periodic items 
 *  
 *  Pattern: Modified Singleton Pattern
 *  Driver: The Singleton pattern is used to allow JERSEY rest classes to get the instance
 *          easily without introducing Java Dependency Injection (could do that in the future)
 *          The modification is required because the instance needs some parameters and the 
 *          traditional singleton pattern does not support that
 *  Options: Could have used property file but due to time constraints and evolution stuck with
 *           parameters
 *  Usage: StreamsDomainTracker.initDomain(param1, param2, param3, ...)
 *         StreamsDomainTracker.getDomain() 
 *           throws exception if domain was not initalized yet
 */
public class StreamsDomainTracker implements NotificationListener, MXBeanSourceProviderListener {
    private static final Logger LOGGER = LoggerFactory.getLogger("root."
            + StreamsDomainTracker.class.getName());

    /*****************************************
     * SINGLETON PATTERN
     *****************************************/
    private static StreamsDomainTracker domainTrackerSingleton = null;
    private static boolean isInitialized = false;

    /*****************************************
     * JMX
     *****************************************/
    private JmxServiceContext jmxContext;
    
    /*****************************************
     * CONFIGURATION
     *****************************************/
    private int refreshRateSeconds; // How often to retrieve bulk metrics
    private boolean trackAllInstances = false;
    private Set<String> requestedInstances = new HashSet<String>(); // Instances user is interested in
    private String protocol;

    /*****************************************
     * DOMAIN INFORMATION
     *****************************************/
    private DomainMXBean domainBean = null;
    private DomainInfo domainInfo = null;
    private String domainName = null;  
    private boolean domainAvailable = false;
    //private boolean instancesAvailable = false;

    
    /*****************************************
     * Metrics Exporter for non REST JSON 
     **************************************/
    // Future change to plugin
	private MetricsExporter metricsExporter = PrometheusMetricsExporter.getInstance();

    /*****************************************
     * INSTANCE MAP
     **************************************/
    private InstanceTrackerMap instanceTrackerMap = null;
  

    /*************************************************************
     * SINGLETON METHODS
     *************************************************************/
    public static StreamsDomainTracker initDomainTracker(
            JmxServiceContext jmxContext, String domainName,
            Set<String> requestedInstances, int refreshRateSeconds, String protocol)
            throws StreamsTrackerException {
        if (domainTrackerSingleton != null) {
            LOGGER.warn("Re-Initializing StreamsDomainTracker");
        } else {
            LOGGER.debug("Initializing StreamsDomainTracker");
        }

        try {
            domainTrackerSingleton = new StreamsDomainTracker(jmxContext,
                    domainName, requestedInstances, refreshRateSeconds, protocol);
            StreamsDomainTracker.isInitialized = true;
        } catch (StreamsTrackerException e) {
            LOGGER.error("Initalization of StreamsDomainTracker instance FAILED!!");
            throw e;
        }

        return domainTrackerSingleton;
    }    
    
    // If refresh rate is 0 (NO_REFRESH) then perform a refresh.
    public static StreamsDomainTracker getDomainTracker()
            throws StreamsTrackerException {
        if (!StreamsDomainTracker.isInitialized) {
            LOGGER.warn("An attempt to retrieve the singleton instance of StreamsDomainTracker was made before it was initialized");
            throw new StreamsTrackerException(
                    StreamsTrackerErrorCode.STREAMS_MONITOR_UNAVAILABLE,
                    "StreamsDomainTracker is not initialized");
        }
        
        if (domainTrackerSingleton.refreshRateSeconds == Constants.NO_REFRESH) {
        		LOGGER.debug("On-demand refresh of metrics and snapshots...");
        		domainTrackerSingleton.refresh();
        }   
        return domainTrackerSingleton;
    }   
    
    
    /***************************************************************
     * AUTOMATIC REFRESH TIMER TASK
     ***************************************************************/
    
    private TimerTask refresher = new TimerTask() {
        @Override
        public void run() {


            refresh();

        }
    };

    /*******************************************************************
     * CONSTRUCTOR
     *******************************************************************/
    private StreamsDomainTracker(JmxServiceContext jmxContext,
            String domainName, Set<String> requestedInstances, int refreshRateSeconds,
            String protocol) throws StreamsTrackerException {
    	
        LOGGER.trace("Constructing StreamsDomainTracker");
        this.jmxContext = jmxContext;
        this.domainName = domainName;
        this.refreshRateSeconds = refreshRateSeconds;
        this.protocol = protocol;
        this.jmxContext.getBeanSourceProvider().addBeanSourceProviderListener(this);
        this.requestedInstances = requestedInstances;
        
        instanceTrackerMap = new InstanceTrackerMap();
        // Are we tracking all instances
        if (requestedInstances.size() == 0) {
        		this.trackAllInstances = true;
        } else {
        		this.trackAllInstances = false;
        }
        
		LOGGER.debug("requested instances ({})...",Arrays.toString(this.requestedInstances.toArray()));
		LOGGER.debug("trackAllInstances: " + this.trackAllInstances);

        // Get Domain
        domainInfo = new DomainInfo(this.domainName);
        initStreamsDomain(true);
        

        
//
//        if (this.instanceInfo.isInstanceAvailable()) {
//            updateInstanceResourceMetrics();
//            initAllJobs();
//        }

        if (this.refreshRateSeconds != Constants.NO_REFRESH) {
        	LOGGER.debug("Refresh rate set to {}, setting up timer process",this.refreshRateSeconds);
        	// Create timer to automatically refresh the status and metrics
        	Timer timer = new Timer("Refresher");
        	timer.scheduleAtFixedRate(refresher, this.refreshRateSeconds * 1000,
                refreshRateSeconds * 1000);
        } else {
        	LOGGER.debug("Refresh rate set to NO_REFRESH, Refreshes will be on-demand, not automatic");
        }

    }


    /******************************************************************
     * REFRESH
     * 
     * Primary mechanism for updating internal state
     * from Streams JMX Server
     * 
     * Triggered by Auto-Refresh Timer or Manually 
     * Exceptions at this level should just be logged so that we
     * continue to refresh.
     * Some are expected in recoverable situations so only log at low
     * level.
     * Unexpected exceptions should be thrown
     *****************************************************************/
    public synchronized void refresh() {
        LOGGER.debug("*** Streams Domain Tracker Refresh");
		LOGGER.trace("    current state: isDomainAvailable: {}",this.isDomainAvailable());
		
        try {
        		//*** REFRESH LOGIC ***
            
            if (!this.isDomainAvailable()) {
            	LOGGER.trace("*** Calling initStreamsDomain()");
                initStreamsDomain(false);
            }            
                       
        } catch (StreamsTrackerException e) {
            LOGGER.debug(
                    "Streams Tracker Refresh StreamsMonitorException: {}.",
                    e);
        } catch (UndeclaredThrowableException e) {

            LOGGER.debug("StreamsMonitor Refresh UndeclaredThrowableException and unwrapping it");
            Throwable t = e.getUndeclaredThrowable();
            if (t instanceof IOException) {
                LOGGER.debug("StreamsMonitor Refresh unwrapped IOException, we will ignore and let JMC Connecton Pool reconnect");
            } else {
                LOGGER.debug("StreamsMonitor Refresh unwrapped "
                        + t.getClass()
                        + " which was unexpected, throw original undeclarable...");
                throw e;
            }
        } catch (Exception e) {
            LOGGER.warn(
                    "StreamsMonitor Refresh Unexpected Exception: {}.  Report so it can be caught appropriately.",
                    e);
        }		
    }

    /*******************************************************************************
     * INIT STREAMS DOMAIN 
     * 
     * Get The Domain Bean
     * 
     * Setup any/all Instance Trackers we need
     * 
     * Exceptions thrown by this method will impact the program differently
     * depending on if it is the first time it is run (on construction) in which
     * case it will cause the program to exit vs it is part of the recurring
     * refresh, in which case we assume it is a recoverable exception (e.g. jmx
     * connection failure) and we will just try to re-initalize on the next
     * refresh.
     *******************************************************************************/
    private synchronized void initStreamsDomain(boolean firstRun) throws StreamsTrackerException {
        MXBeanSource beanSource = null;
        this.setDomainAvailable(false);
        try {

            beanSource = jmxContext.getBeanSourceProvider().getBeanSource();

            // Determines if the domain exists
            // This is really never going to be the case, as if the domain goes away
            // The jmx server would have gone away.  The key here is just to capture
            // any weird issue.
 
 			// This returns a bean, even if the domain does not exist.  Need to access it to find out names do not match
            this.domainBean = beanSource.getDomainBean(this.domainName);
            
            // Attempt to access the bean, only way to determine if it is a valid domain name
            LOGGER.info("Streams Domain '{}' found, Status: {}",
                    new Object[] { domainBean.getName(), domainBean.getStatus() });
            
            this.setDomainAvailable(true);
            domainInfo.updateInfo(this.domainBean);

            // Setup notifications (should handle exceptions)
            ObjectName domainObjName = ObjectNameBuilder.domain(this.domainName);
            NotificationFilterSupport filter = new NotificationFilterSupport();
            filter.disableAllTypes();
            filter.enableType(AttributeChangeNotification.ATTRIBUTE_CHANGE);
            filter.enableType(Notifications.INSTANCE_CREATED);
            filter.enableType(Notifications.INSTANCE_DELETED);

            // Remove any existing notification listener
            try {
                beanSource.getMBeanServerConnection()
                        .removeNotificationListener(domainObjName, this);
            } catch (Exception e) {
                // Ignore because we do not care if this fails
            }
            beanSource.getMBeanServerConnection().addNotificationListener(
                    domainObjName, this, filter, null);


		} catch (UndeclaredThrowableException e) {
            // Some InstanceNotFoundExceptions are wrapped in
            // UndeclaredThrowableExceptions sadly
			Throwable t = e.getUndeclaredThrowable();
			if (t instanceof InstanceNotFoundException) {
				LOGGER.error(
						"Domain '{}' not found when initializing domain tracker.  Ensure the JMX URL specified is for the domain you are attempting to connect to.",
						this.getDomainName());
				resetDomainTracker();
				throw new StreamsTrackerException(StreamsTrackerErrorCode.DOMAIN_NOT_FOUND,
						"Domain name " + this.domainName + " does not match the domain of the JMX Server.", e);
			} else {
				LOGGER.trace("Unexpected exception (" + t.getClass()
						+ ") when retrieving Streams domain information from JMX Server, throwing original undeclarable...");
				throw e;
			}

        } catch (InstanceNotFoundException infe) {
            LOGGER.error(
                    "Domain '{}' not found when initializing domain tracker.  Ensure the JMX URL specified is for the domain you are attempting to connect to.",
                    this.getDomainName());
            resetDomainTracker();
			throw new StreamsTrackerException(StreamsTrackerErrorCode.DOMAIN_NOT_FOUND,
					"Domain name " + this.domainName + " does not match the domain of the JMX Server.", infe);

        } catch (MalformedURLException me) {
            resetDomainTracker();
            throw new StreamsTrackerException(
                    StreamsTrackerErrorCode.JMX_MALFORMED_URL,
                    "Malformed URL error while retrieving domain information, domain: "
                            + this.domainName, me);
        } catch (IOException ioe) {
        		resetDomainTracker();
            // JMX Error, cannot initialize streams domain so ensure state
            // variables reflect and return
            LOGGER.warn("JMX IO Exception when initializing domain tracker.  Not sure why JMX Connection Pool did not retry connection");
            // If this is the first connect attempt, then we need to exit, throw exception
            if (firstRun) {
	            throw new StreamsTrackerException(
	                    StreamsTrackerErrorCode.JMX_IOERROR,
	                    "JMX IO error while retrieving domain information, domain: "
	                            + this.domainName, ioe);
            }
        }
        createExportedDomainMetrics();
        
        /* Create the Instance Trackers */
        createUpdateStreamsInstanceTrackers();

    }
    
    
    /**********************************************************************************
     * RESET DOMAIN TRACKER
     * 
     * resetDomainTracker In the case of a JMX error or anything else that could have
     * invalidated our state reset the state so that any JMX Beans we need can be 
     * re-acquired.
     **********************************************************************************/
    public synchronized void resetDomainTracker() {
        this.setDomainAvailable(false);
        domainInfo.close();
//        this.jobsAvailable = false;
//        this.metricsAvailable = false;
//        this.snapshotsAvailable = false;
        // Set Metrics Failure on metrics Object
//        if (this.allJobMetrics != null) {
//        	this.allJobMetrics.setLastMetricsFailure(new Date());
//        	this.allJobMetrics.setLastMetricsRefreshFailed(true);
//        }
//        // Set Snapshot Failure on metrics Object
//        if (this.allJobSnapshots != null) {
//        	this.allJobSnapshots.setLastSnapshotFailure(new Date());
//        	this.allJobSnapshots.setLastSnapshotRefreshFailed(true);
//        }
        removeExportedDomainMetrics();
        createExportedDomainMetrics();
    }
    
    
    /**********************************************************************************
     * Streams Domain handleNotification
     * 
     * Primary interface to listen for changes to the domain we are monitoring
     * Only interested in specific notifications so should implement filter soon
     **********************************************************************************/
    public void handleNotification(Notification notification, Object handback) {
    	try {
    		String notificationType = notification.getType();
    		LOGGER.trace("Streams Domain Notification: " + notification
    				+ "; User Data: " + notification.getUserData());

    		switch (notificationType) {
	    		case AttributeChangeNotification.ATTRIBUTE_CHANGE:
	    			AttributeChangeNotification acn = (AttributeChangeNotification) notification;
	    			String attributeName = acn.getAttributeName();
	    			LOGGER.debug("Domain attribute changed: {} from {} to {}, updating Domain Info...",
	    					attributeName, acn.getOldValue(), acn.getNewValue());
	    			domainInfo.updateInfo(this.domainBean);
    			
	    			break;
	        case Notifications.INSTANCE_CREATED:
	            LOGGER.debug("Streams Instance created in domain, If tracking it or all instances, we need to add it to the instanceMap");
    				domainInfo.updateInfo(this.domainBean);

    				createUpdateStreamsInstanceTrackers();
	            //this.instanceInfo.setInstanceExists(false);
	            //resetTracker();
	            //clearTracker();
	            break;

	        case Notifications.INSTANCE_DELETED:
	            LOGGER.debug("Instance deleted from domain, If tracking it or all instances, we need to update its status");
    				domainInfo.updateInfo(this.domainBean);
    				
    				createUpdateStreamsInstanceTrackers();

	            //this.instanceInfo.setInstanceExists(false);
	            //resetTracker();
	            //clearTracker();
	            break;
	        }
    	} catch (Exception e) {
    		LOGGER.error("Streams Domain Notification Handler caught exception: {}",e.toString());
    		e.printStackTrace();
    	}
    }

    public boolean isDomainAvailable() {
		return domainAvailable;
	}

	public void setDomainAvailable(boolean domainAvailable) {
		this.domainAvailable = domainAvailable;
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
    
    public synchronized Map<String, StreamsInstanceTracker> getInstanceTrackerMap() {
    		return instanceTrackerMap.getMap();
    }

    public synchronized DomainInfo getDomainInfo() throws StreamsTrackerException {
    	
        //verifyInstanceExists();

        return domainInfo;
    }

	String getProtocol() {
        return protocol;
    }



    /*
     * clearMonitor In the case that the Streams instance is stopped/fails we
     * will not be able to recover the metrics or jobs so clear them out
     */
//    private synchronized void clearTracker() {
//        instanceResourceMetrics.clear();
//        removeExportedInstanceMetrics();
//        createExportedInstanceMetrics();
//        this.allJobMetrics.clear();
//        this.allJobSnapshots.clear();
//        this.instanceMap.clear();
//    }

	
	/******************************************************
	 * INSTANCES TRACKER MAP and MANAGEMENT
	 ******************************************************/
	
	/* Respond to initial creation and whenever instances added or removed */
	private synchronized void createUpdateStreamsInstanceTrackers() {
		Set<String> instancesToTrack = null;
		
		LOGGER.debug("createUpdateStreamsInstanceTrackers...");
		if (this.trackAllInstances) {
			instancesToTrack = domainInfo.getInstances();
		} else {
			instancesToTrack = this.requestedInstances;
		}
		
		LOGGER.debug("requested instances ({})...",Arrays.toString(this.requestedInstances.toArray()));
		LOGGER.debug("trackAllInstances: " + this.trackAllInstances);

		
		LOGGER.debug("Ensure we are tracking instances ({})...",Arrays.toString(instancesToTrack.toArray()));

		for (String instanceName : instancesToTrack) {
			
			if (this.instanceTrackerMap.getInstanceTracker(instanceName) == null) {
				LOGGER.debug("Instance ({}) not found in instanceTrackerMap, creating...", instanceName);
				try {
					StreamsInstanceTracker newInstanceTracker = new StreamsInstanceTracker(this.jmxContext,
							this.domainName,
							instanceName,
							this.refreshRateSeconds,
							this.protocol);
					
					this.instanceTrackerMap.addInstanceTrackerToMap(instanceName, newInstanceTracker);
				} catch (StreamsTrackerException e) {
					LOGGER.warn("Received a StreamsTrackerException while creating StreamsInstanceTracker for instance ({})",instanceName);
				}
			} else {
				LOGGER.debug("Instance ({}) is being tracked.",instanceName);
			}
		}
		
		// Remove any instances that we no longer need to track
		Set<String> currentTrackers = (Set<String>)instanceTrackerMap.getInstanceNames();
		for (String curInstance : currentTrackers) {
			if (!instancesToTrack.contains(curInstance) ) {
				LOGGER.debug("Instance ({}) no longer needs to be tracked.  Removing from Instance Tracker Map.",curInstance);
				instanceTrackerMap.getInstanceTracker(curInstance).close();
				instanceTrackerMap.removeInstanceTrackerFromMap(curInstance);
			}
		}
		
	}


    // Initialize our tracking of jobs and metrics
    // Used at startup and when we have lost contact to JMX or Instance
//    private synchronized void initAllJobs() throws StreamsTrackerException {
//
//        initJobMap();
//
//        if (this.instanceInfo.isInstanceAvailable() && jobsAvailable) {
//
//            StopWatch stopwatch = null;
//            LinkedHashMap<String, Long> timers = null;
//            if (LOGGER.isDebugEnabled()) {
//                stopwatch = new StopWatch();
//                timers = new LinkedHashMap<String, Long>();
//                stopwatch.reset();
//                stopwatch.start();
//            }
//
//            // Initialize Metrics
//            try {
//                // Try to only create it if it does not exist and rely on the
//                // clearing of the metrics so we preserve our timing attributes
//                if (allJobMetrics == null) {
//                    allJobMetrics = new AllJobMetrics(this.jmxContext,
//                            this.domainName,
//                            this.instanceInfo.getInstanceName(), this.protocol);
//                } else {
//                    allJobMetrics.clear();
//                }
//
//                if (LOGGER.isDebugEnabled()) {
//                    stopwatch.stop();
//                    timers.put("Initialize AllJobMetrics", stopwatch.getTime());
//                    stopwatch.reset();
//                    stopwatch.start();
//                }
//
//                // Assume available, may need a more detailed check in the
//                // future
//                metricsAvailable = true;
//
//                // Do not refresh from server because the constructor pulled
//                // them
//                if (metricsAvailable)
//                    updateAllJobMetrics(false);
//            } catch (IOException e) {
//                LOGGER.warn("JMX IO Exception when initializing all job metrics, resetting monitor. Exception message: "
//                        + e.getLocalizedMessage());
//                resetTracker();
//            }
//            
//            if (LOGGER.isDebugEnabled()) {
//                stopwatch.stop();
//                timers.put("updateAllJobMetrics(false)", stopwatch.getTime());
//                LOGGER.debug("Profiling for Inializing job map");
//                for (Map.Entry<String, Long> entry : timers.entrySet()) {
//                    LOGGER.debug("Timing for " + entry.getKey() + ": "
//                            + entry.getValue());
//                }
//                stopwatch.reset();
//                stopwatch.start();
//            }            
//
//            // Initialize Snapshots
//            try {
//                // Try to only create it if it does not exist and rely on the
//                // clearing of the metrics so we preserve our timing attributes
//                if (allJobSnapshots == null) {
//                    allJobSnapshots = new AllJobSnapshots(this.jmxContext,
//                            this.domainName,
//                            this.instanceInfo.getInstanceName(), this.protocol);
//                } else {
//                    allJobSnapshots.clear();
//                }
//
//                if (LOGGER.isDebugEnabled()) {
//                    stopwatch.stop();
//                    timers.put("Initialize AllJobSnapshots", stopwatch.getTime());
//                    stopwatch.reset();
//                    stopwatch.start();
//                }
//
//                // Assume available, may need a more detailed check in the
//                // future
//                snapshotsAvailable = true;
//
//                // Do not refresh from server because the constructor pulled
//                // them
//                if (snapshotsAvailable)
//                    updateAllJobSnapshots(false);
//            } catch (IOException e) {
//                LOGGER.warn("JMX IO Exception when initializing all job snapshots, resetting monitor. Exception message: "
//                        + e.getLocalizedMessage());
//                resetTracker();
//            }            
//            
//            if (LOGGER.isDebugEnabled()) {
//                stopwatch.stop();
//                timers.put("updateAllJobSnapshots(false)", stopwatch.getTime());
//                LOGGER.debug("Profiling for Initializing job map");
//                for (Map.Entry<String, Long> entry : timers.entrySet()) {
//                    LOGGER.debug("Timing for " + entry.getKey() + ": "
//                            + entry.getValue());
//                }
//            }
//
//        }
//
//    }

    // Initialize our JobMap
    // Used at startup and when we have lost contact to JMX or Instance
//    private synchronized void initJobMap() {
//        String domainName = this.domainName;
//        String instanceName = this.instanceInfo.getInstanceName();
//        InstanceMXBean instance = null;
//
//        if (this.instanceInfo.isInstanceAvailable()) {
//
//            StopWatch stopwatch = null;
//            LinkedHashMap<String, Long> timers = null;
//            if (LOGGER.isDebugEnabled()) {
//                stopwatch = new StopWatch();
//                timers = new LinkedHashMap<String, Long>();
//            }
//
//            try {
//
//                instance = jmxContext.getBeanSourceProvider().getBeanSource()
//                        .getInstanceBean(domainName, instanceName);
//
//                // Get list of Jobs
//                if (LOGGER.isDebugEnabled()) {
//                    stopwatch.reset();
//                    stopwatch.start();
//                }
//
//                LOGGER.debug("Get list of all jobs...");
//                Set<BigInteger> jobs = null;
//
//                jobs = jmxContext.getBeanSourceProvider().getBeanSource()
//                        .getInstanceBean(domainName, instanceName).getJobs();
//
//                if (LOGGER.isDebugEnabled()) {
//                    stopwatch.stop();
//                    timers.put("getJobs", stopwatch.getTime());
//                    stopwatch.reset();
//                    stopwatch.start();
//                }
//
//                ObjectName opListenerObj = instance.createOperationListener();
//                OperationListenerMXBean opListener = JMX.newMXBeanProxy(
//                        jmxContext.getBeanSourceProvider().getBeanSource()
//                                .getMBeanServerConnection(), opListenerObj,
//                        OperationListenerMXBean.class, true);
//                instance.registerAllJobs(opListener.getId());
//
//                // *** Wait for all jobs to be registered *** //
//                LOGGER.debug("Waiting for jobs to be registered...");
//                int numMessages = 0;
//                boolean registerCompleted = false;
//                boolean registerError = false;
//                List<OperationStatusMessage> messageList = new ArrayList<OperationStatusMessage>();
//                while (!registerCompleted && !registerError) {
//                    // System.out.println("opListener.getMessages()...");
//                    // Found issue in testing when messages were not ready, and
//                    // tried to
//                    // get them again
//                    // it raised a ConcurrentModificationException, but if we
//                    // ignore
//                    // and
//                    // try again, everything
//                    // works. So eat it for now.
//                    try {
//                        messageList = opListener.getMessages();
//                    } catch (java.util.ConcurrentModificationException e) {
//                    }
//
//                    numMessages = messageList.size();
//
//                    if (numMessages > 0) {
//                        for (OperationStatusMessage osm : messageList) {
//                            if (osm.getStatus() == OperationListenerMXBean.Status.COMPLETED)
//                                registerCompleted = true;
//                            if (osm.getStatus() == OperationListenerMXBean.Status.ERROR)
//                                registerError = true;
//                        }
//                    }
//                }
//
//                opListener.unregister();
//                
//                
//                // Verify that jobs are registered
//                // Found issue in Streams 4.2 where the message said COMPLETED
//                // but jobs were not truly registered
//                LOGGER.trace("Verifing all jobs show isRegistered() true...");
//                MBeanServerConnection mbsc = null;
//					      mbsc = jmxContext.getBeanSourceProvider().getBeanSource().getMBeanServerConnection();
//                for (BigInteger jobno : jobs) {
//                    LOGGER.trace("Verifying job #{}...",jobno);
//                    ObjectName tJobNameObj = ObjectNameBuilder.job(domainName,
//                            instanceName, jobno);   
//                    while (! mbsc.isRegistered(tJobNameObj)) {
//                        LOGGER.trace("...NOT registered, trying again...");
//                    }
//                    LOGGER.trace("...Registered!");             
//                }
//                
//                
//
//                if (LOGGER.isDebugEnabled()) {
//                    stopwatch.stop();
//                    timers.put("registerJobs", stopwatch.getTime());
//                    stopwatch.reset();
//                    stopwatch.start();
//                }
//
//                // Populate the map of jobs and create jobInfo objects with
//                // jobMXBeans
//                // Create the jobname index
//                jobMap.clear();
//
//                LOGGER.trace("Create hashmap of JobMXBeans...");
//                for (BigInteger jobno : jobs) {
//                    ObjectName tJobNameObj = ObjectNameBuilder.job(domainName,
//                            instanceName, jobno);
//                    JobMXBean jobBean = JMX.newMXBeanProxy(jmxContext
//                            .getBeanSourceProvider().getBeanSource()
//                            .getMBeanServerConnection(), tJobNameObj,
//                            JobMXBean.class, true);
//                    jobMap.addJobToMap(jobno, new JobDetails(this, jobno, jobBean));
//                }
//
//                // IMPORTANT: Set jobsAvailable to true
//                jobsAvailable = true;
//
//            } catch (IOException e) {
//                // An IOException at this point means the jmx connection is
//                // probably lost. Reset Monitor and continue to wait for it to
//                // reconnect
//                LOGGER.warn("Job Map Initialization received IO Exception from JMX Connection Pool.  Resetting monitor.  Exception Message: "
//                        + e.getLocalizedMessage());
//                resetTracker();
//            }
//
//            if (LOGGER.isDebugEnabled()) {
//                stopwatch.stop();
//                timers.put("create JobMXBeans", stopwatch.getTime());
//                LOGGER.debug("Profiling for Inializing job map");
//                for (Map.Entry<String, Long> entry : timers.entrySet()) {
//                    LOGGER.debug("Timing for " + entry.getKey() + ": "
//                            + entry.getValue());
//                }
//            }
//        }
//
//    }

    // ** Add Job to job map
//    private synchronized void addJobToMap(BigInteger jobid) {
//        InstanceMXBean instance = null;
//        LOGGER.debug("AddJobToMap({})...", jobid);
//        StopWatch sw = new StopWatch();
//        sw.start();
//
//        try {
//            instance = this.jmxContext
//                    .getBeanSourceProvider()
//                    .getBeanSource()
//                    .getInstanceBean(domainName,
//                            this.instanceInfo.getInstanceName());
//
//            ObjectName tJobNameObj = instance.registerJob(jobid);
//
//            JobMXBean jobBean = JMX.newMXBeanProxy(jmxContext
//                    .getBeanSourceProvider().getBeanSource()
//                    .getMBeanServerConnection(), tJobNameObj, JobMXBean.class,
//                    true);
//            jobMap.addJobToMap(jobid, new JobDetails(this, jobid, jobBean));
//
//        } catch (IOException e) {
//            LOGGER.warn("New Job Initialization received IO Exception from JMX Connection Pool.  Resetting monitor.  Exception Message: "
//                    + e.getLocalizedMessage());
//            resetTracker();
//        }
//
//        sw.stop();
//        LOGGER.debug("** addJobToMap (jobid: " + jobid + ") time: "
//                + sw.getTime());
//        
//		metricsExporter.getStreamsMetric("jobCount", StreamsObjectType.INSTANCE, this.domainName, this.instanceInfo.getInstanceName()).set(jobMap.size());
//
//    }
//
//    private synchronized void removeJobFromMap(BigInteger jobid) {
//        LOGGER.debug("removeJobFromMap({})...", jobid);
//        StopWatch sw = new StopWatch();
//        sw.start();
//
//        // Do we need to do anything to the MXBean we created for the job?
//        instanceMap.removeJobFromMap(jobid);
//
//        sw.stop();
//        LOGGER.debug("** removeJobFromMap (jobid: " + jobid + ") time: "
//                + sw.getTime());
//        
//		metricsExporter.getStreamsMetric("jobCount", StreamsObjectType.INSTANCE, this.domainName, this.instanceInfo.getInstanceName()).set(jobMap.size());
//
//    }   
//    
//    /*
//     * updateAllJobMetrics Need to use this to determine when JMX has been reset
//     * so we invalidate the job beans
//     */
//    private synchronized void updateAllJobMetrics(boolean refreshFromServer)
//            throws StreamsTrackerException {
//        LOGGER.trace("***** Entered updateAllJobMetrics, refreshFromServer {}, jobsAvailable {}",
//                refreshFromServer, jobsAvailable);
//        
//        if (jobsAvailable) {
//            LOGGER.debug("** updateAllJobMetrics Start timer...");
//            StopWatch sw = new StopWatch();
//            sw.reset();
//            sw.start();
//            
//            // Refresh Metrics if requested
//            if (refreshFromServer) {
//                try {
//                    this.allJobMetrics.refresh();
//                } catch (IOException e) {
//                    LOGGER.error("Updating all metrics received IO Exception from JMX Connection Pool.  Resetting monitor.  Exception Message: "
//                            + e.getLocalizedMessage());
//                    resetTracker();
//                }
//                sw.split();
//                LOGGER.debug("** updateAllJobMetrics refresh from server split time: " + sw.getSplitTime());
//                sw.unsplit();
//            }
//
//            if (allJobMetrics.isLastMetricsRefreshFailed()) {
//                // If retrieving metrics fails, we need to loop through the jobs
//                // and set the attributes to reflect that
//            	jobMap.setJobMetricsFailed(allJobMetrics.getLastMetricsFailure());
//
//            } else {
//                // We retrieved them successfully
//                String allMetrics = this.allJobMetrics.getAllMetrics();
//
//                // Parse and update each jobInfo
//                if (allMetrics != null) {
//
//                    try {
//                        JSONParser parser = new JSONParser();
//                        JSONObject metricsObject = (JSONObject) parser
//                                .parse(allMetrics);
//                        JSONArray jobArray = (JSONArray) metricsObject
//                                .get("jobs");
//
//                        for (int j = 0; j < jobArray.size(); j++) {
//                            JSONObject jobObject = (JSONObject) jobArray.get(j);
//                            BigInteger jobId = new BigInteger(
//                                    (String) jobObject.get("id"));
//                            JobDetails jd = jobMap.getJob(Integer.parseInt((String)jobObject.get("id")));
//                            if (jd != null) {
//                                jd.setJobMetrics(jobObject.toString());
//                                // Update the job details that we refreshed
//                                // metrics
//                                jd.setLastMetricsRefresh(allJobMetrics
//                                        .getLastMetricsRefresh());
//                                jd.setLastMetricsFailure(allJobMetrics
//                                        .getLastMetricsFailure());
//                                jd.setLastMetricsRefreshFailed(false);
//                            } else {
//                                LOGGER.warn(
//                                        "Received Metrics for jobId({}) that is not found in the current jobArray, missed notification of new job is likely cause",
//                                        jobId);
//                            }
//                        }
//                    } catch (Exception e) {
//                        LOGGER.error("Exception Parsing Metrics JSON...exiting");
//                        LOGGER.error(e.toString());
//                        e.printStackTrace();
//                        System.exit(1);
//                    }
//                }
//            }
//            sw.stop();
//            LOGGER.debug("** updateAllJobMetrics time total (includes parsing) (milliseconds): " + sw.getTime());
//        }
//
//
//        LOGGER.trace("Exited");
//
//    }
//    
//    
//    
//    /*
//     * updateAllJobSnapshots Need to use this to determine when JMX has been reset
//     * so we invalidate the job beans
//     */
//    private synchronized void updateAllJobSnapshots(boolean refreshFromServer)
//            throws StreamsTrackerException {
//        LOGGER.trace("***** Entered updateAllJobSnapshots, refreshFromServer {}, jobsAvailable {}",
//                refreshFromServer, jobsAvailable);
//        
//        if (jobsAvailable) {
//            LOGGER.debug("** updateAllJobSnapshots Start timer...");
//            StopWatch sw = new StopWatch();
//            sw.reset();
//            sw.start();
//            
//            // Refresh Snapshots if requested
//            if (refreshFromServer) {
//                try {
//                    this.allJobSnapshots.refresh();
//                } catch (IOException e) {
//                    LOGGER.error("Updating all snapshots received IO Exception from JMX Connection Pool.  Resetting monitor.  Exception Message: "
//                            + e.getLocalizedMessage());
//                    resetTracker();
//                }
//                sw.split();
//                LOGGER.debug("** updateAllJobSnapshots refresh from server split time: " + sw.getSplitTime());
//                sw.unsplit();
//            }
//
//            if (allJobSnapshots.isLastSnapshotRefreshFailed()) {
//                // If retrieving snapshots fails, we need to loop through the jobs
//                // and set the attributes to reflect that
//            	jobMap.setJobSnapshotFailed(allJobSnapshots.getLastSnapshotFailure());
//
//            } else {
//                // We retrieved them successfully
//                String allSnapshots = this.allJobSnapshots.getAllSnapshots();
//
//                // Parse and update each jobInfo
//                if (allSnapshots != null) {
//
//                    try {
//                        JSONParser parser = new JSONParser();
//                        JSONObject snapshotsObject = (JSONObject) parser
//                                .parse(allSnapshots);
//                        JSONArray jobArray = (JSONArray) snapshotsObject
//                                .get("jobs");
//
//                        for (int j = 0; j < jobArray.size(); j++) {
//                            JSONObject jobObject = (JSONObject) jobArray.get(j);
//                            BigInteger jobId = new BigInteger(
//                                    (String) jobObject.get("id"));
//                            JobDetails jd = jobMap.getJob(Integer.parseInt((String)jobObject.get("id")));
//                            if (jd != null) {
//                                jd.setJobSnapshot(jobObject.toString());
//                                // Update the job details that we refreshed
//                                // metrics
//                                jd.setLastSnapshotRefresh(allJobSnapshots
//                                        .getLastSnaphostRefresh());
//                                jd.setLastSnapshotFailure(allJobSnapshots
//                                        .getLastSnapshotFailure());
//                                jd.setLastSnapshotRefreshFailed(false);
//                            } else {
//                                LOGGER.warn(
//                                        "Received Snapsbhots for jobId({}) that is not found in the current jobArray, missed notification of new job is likely cause",
//                                        jobId);
//                            }
//                        }
//                    } catch (Exception e) {
//                        LOGGER.error("Exception Parsing Snapsnots JSON...exiting");
//                        LOGGER.error(e.toString());
//                        e.printStackTrace();
//                        System.exit(1);
//                    }
//                }
//            }
//            sw.stop();
//            LOGGER.debug("** updateAllJobSnapshots time total (includes parsing) (milliseconds): " + sw.getTime());
//        }
//
//
//        LOGGER.trace("Exited");
//
//    }    
//    
//    
//
//    @Override
//    public String toString() {
//        StringBuilder result = new StringBuilder();
//        String newline = System.getProperty("line.separator");
//
//        result.append("Domain: " + domainName);
//        result.append(newline);
//        result.append("Instance: " + this.instanceInfo.getInstanceName()
//                + ", status: " + this.instanceInfo.getInstanceStatus()
//                + ", instanceStartTime: "
//                + convertTime(this.instanceInfo.getInstanceStartTime()));
//        result.append(newline);
//        result.append("instanceAvailable:"
//                + this.instanceInfo.isInstanceAvailable());
//        result.append(newline);
//        result.append("jobMapAvailable:" + jobsAvailable);
//        result.append(newline);
//        result.append("jobMetricsAvailable:" + metricsAvailable);
//        result.append(newline);
//        result.append("jobSnapshotsAvailable:" + snapshotsAvailable);
//        result.append(newline);        
//        result.append("instanceResourceMetricsLastUpdated:" + convertTime(instanceResourceMetricsLastUpdated));
//        result.append(newline);
//        if (jobsAvailable) {
//        	result.append(jobMap.toString());
//        }
//        return result.toString();
//    }
//
//    private String convertTime(Long time) {
//        if (time != null) {
//            Date date = new Date(time);
//            Format format = new SimpleDateFormat("yyyy MM dd HH:mm:ss");
//            return format.format(date);
//        } else {
//            return "null";
//        }
//    }

//    public void printJob(BigInteger jobid) {
//    	
//        System.out.println("Job Status: " + jobMap.getJob(jobid).getStatus());
//        System.out.println("Job Metrics: " + jobMap.getJob(jobid).getJobMetrics());
//    }


    /*********************************
     * DOMAIN METRICS
     *********************************/
    private void createExportedDomainMetrics() {
	    	metricsExporter.createStreamsMetric("status", StreamsObjectType.DOMAIN, "Domain status, 1: running, .5: starting, stopping, 0: stopped, removing, unknown");
	    	metricsExporter.getStreamsMetric("status", StreamsObjectType.DOMAIN, this.domainName).set(getDomainStatusAsMetric());
	    	metricsExporter.createStreamsMetric("instanceCount", StreamsObjectType.DOMAIN, "Number of instances currently created in the streams domain");
	    	metricsExporter.getStreamsMetric("instanceCount", StreamsObjectType.DOMAIN, this.domainName).set(this.domainInfo.getInstances().size());
    }
    
    private void removeExportedDomainMetrics() {
		metricsExporter.removeAllChildStreamsMetrics(this.domainName);
    }
    
    private double getDomainStatusAsMetric() {
	    	double value = 0;
	    	switch (this.domainInfo.getStatus()) {
		    	case "running" :
		    		value = 1;
		    		break;
		    	case "starting":
		    	case "stopping":
		    		value = 0.5;
		    		break;
		    	default:
		    		value = 0;
	    	}
	    	return value;
    }
    
    /**************************************
     * JMX BEAN SOURCE PROVIDER LISTENER
     **************************************/

    @Override
    public void beanSourceInterrupted(MXBeanSource bs) {
        LOGGER.debug("***** Streams Domain BeanSource interrupted, resetting monitor...");
        resetDomainTracker();
    }
}
