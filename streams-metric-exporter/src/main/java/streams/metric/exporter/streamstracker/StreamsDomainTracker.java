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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;

import javax.management.Notification;
import javax.management.AttributeChangeNotification;
import javax.management.NotificationFilterSupport;
import javax.management.ObjectName;
import javax.management.NotificationListener;
import javax.management.InstanceNotFoundException;

import org.apache.commons.lang.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ibm.streams.management.ObjectNameBuilder;
import com.ibm.streams.management.domain.DomainMXBean;
import streams.metric.exporter.Constants;
import streams.metric.exporter.ServiceConfig;
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
    private ServiceConfig config = null;
    private int refreshRateSeconds; // How often to retrieve bulk metrics
    private boolean autoRefresh=false;
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
            Set<String> requestedInstances, int refreshRateSeconds, String protocol, ServiceConfig config)
            throws StreamsTrackerException {
        if (domainTrackerSingleton != null) {
            LOGGER.warn("Re-Initializing StreamsDomainTracker");
        } else {
            LOGGER.debug("Initializing StreamsDomainTracker");
        }

        try {
            domainTrackerSingleton = new StreamsDomainTracker(jmxContext,
                    domainName, requestedInstances, refreshRateSeconds, protocol, config);
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
        		LOGGER.debug("DOMAIN: On-demand refresh of metrics and snapshots...");
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
    			LOGGER.debug("DOMAIN: Automatic refresh of metrics and snapshots...");
            refresh();
        }
    };

    /*******************************************************************
     * CONSTRUCTOR
     *******************************************************************/
    private StreamsDomainTracker(JmxServiceContext jmxContext,
            String domainName, Set<String> requestedInstances, int refreshRateSeconds,
            String protocol, ServiceConfig config) throws StreamsTrackerException {
    	
        LOGGER.trace("Constructing StreamsDomainTracker");
        this.config=config;
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

        if (this.refreshRateSeconds == Constants.NO_REFRESH) {
            this.autoRefresh = true;
        }

        // Get Domain
        domainInfo = new DomainInfo(this.domainName);
        initStreamsDomain(true);

        if (this.autoRefresh != true) {
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
        LOGGER.debug("*** DOMAIN Refresh: {}",this.domainName);
		LOGGER.trace("    current state: isDomainAvailable: {}",this.isDomainAvailable());
		
		StopWatch outerwatch = null;
        StopWatch stopwatch = null;
        LinkedHashMap<String, Long> timers = null;
        if (LOGGER.isDebugEnabled()) {
        		outerwatch = new StopWatch();
            stopwatch = new StopWatch();
            timers = new LinkedHashMap<String, Long>();
        }
		
        try {
        		//*** REFRESH LOGIC ***
            
            if (LOGGER.isDebugEnabled()) {
            		outerwatch.reset();
                stopwatch.reset();
                outerwatch.start();
                stopwatch.start();
            }
            
        		// If previously marked unavailable, re-initialize it
            if (!this.isDomainAvailable()) {
            	LOGGER.trace("*** Calling initStreamsDomain()");
                initStreamsDomain(false);
            } 
            
            
            if (LOGGER.isDebugEnabled()) {
                stopwatch.stop();
                timers.put("initStreamsDomain", stopwatch.getTime());
                stopwatch.reset();
                stopwatch.start();
            }
            
            // Any refresh we need to do to Domain?  
            // Not at the moment, notifications 
            
            
            // Refresh Instances
            Iterator<Map.Entry<String, StreamsInstanceTracker>> iit = this.getInstanceTrackerMap().entrySet().iterator();
            while (iit.hasNext()) {
            		Map.Entry<String, StreamsInstanceTracker> InstanceEntry = iit.next();
            		StreamsInstanceTracker sit = InstanceEntry.getValue();    
            		
            		sit.refresh();
            		
            		if (LOGGER.isDebugEnabled()) {
            			stopwatch.stop();
            			timers.put("refreshInstance(" + sit.getInstanceInfo().getInstanceName() + ")",stopwatch.getTime());
            			stopwatch.reset();
            			stopwatch.start();
            		}
            }
            
            if (LOGGER.isDebugEnabled()) {
            		outerwatch.stop();
                stopwatch.stop();
                LOGGER.debug("*** DOMAIN Refresh timing (ms):");
                for (Map.Entry<String, Long> entry : timers.entrySet()) {
                    LOGGER.debug("      " + entry.getKey() + " time: "
                            + entry.getValue());
                }
                LOGGER.debug("*** DOMAIN Refresh total time: " + outerwatch.getTime());
            }
            
        } catch (StreamsTrackerException e) {
            LOGGER.debug(
                    "DOMAIN Refresh StreamsMonitorException: {}.",
                    e);
            resetDomainTracker();
        } catch (UndeclaredThrowableException e) {

            LOGGER.debug("DOMAIN Refresh UndeclaredThrowableException and unwrapping it");
            Throwable t = e.getUndeclaredThrowable();
            if (t instanceof IOException) {
                LOGGER.debug("DOMAIN Refresh unwrapped IOException, we will ignore and let JMC Connecton Pool reconnect");
                resetDomainTracker();
            } else {
                LOGGER.debug("DOMAIN Refresh unwrapped "
                        + t.getClass()
                        + " which was unexpected, throw original undeclarable...");
                throw e;
            }
        } catch (Exception e) {
            LOGGER.warn(
                    "DOMAIN Refresh Unexpected Exception: {}.  Report so it can be caught appropriately.",
                    e);
            resetDomainTracker();
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
            if (domainBean.getStatus() != DomainMXBean.Status.RUNNING) {
            		resetDomainTracker();
            		LOGGER.info("Waiting for domain status to be RUNNING");
            		return;
            }
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
			throw new StreamsTrackerException(StreamsTrackerErrorCode.DOMAIN_NOT_FOUND,
					"Domain name " + this.domainName + " does not match the domain of the JMX Server.", infe);

        } catch (MalformedURLException me) {
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
            
            updateExportedDomainMetrics();
            
    	} catch (Exception e) {
    		LOGGER.error("Streams Domain Notification Handler caught exception: {}",e.toString());
    		e.printStackTrace();
    	}
    }
    
    

    public ServiceConfig getConfig() {
		return config;
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

    public boolean isAutoRefresh() {
        return autoRefresh;
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
		
		LOGGER.trace("createUpdateStreamsInstanceTrackers...");
		if (this.trackAllInstances) {
			instancesToTrack = domainInfo.getInstances();
		} else {
			instancesToTrack = this.requestedInstances;
		}
		
		LOGGER.debug("*** Setting streams instances to track");
		if (this.trackAllInstances) {
			LOGGER.debug("    Configuration set to track ALL instances");
			LOGGER.debug("    Current Instances in the domain: {}",Arrays.toString(instancesToTrack.toArray()));
		} else {
			LOGGER.debug("    Configuration set to track specified instances");
			LOGGER.debug("    Specified instances: {}",Arrays.toString(instancesToTrack.toArray()));
		}

		for (String instanceName : instancesToTrack) {
			
			if (this.instanceTrackerMap.getInstanceTracker(instanceName) == null) {
				LOGGER.debug("    Instance ({}) not currently being tracked, initializing instance tracking...", instanceName);
				try {
					StreamsInstanceTracker newInstanceTracker = new StreamsInstanceTracker(this.jmxContext,
							this.domainName,
                            instanceName,
                            this.isAutoRefresh(),
							this.config);
					
					this.instanceTrackerMap.addInstanceTrackerToMap(instanceName, newInstanceTracker);
				} catch (StreamsTrackerException e) {
					LOGGER.warn("Received a StreamsTrackerException while creating StreamsInstanceTracker for instance ({})",instanceName);
				}
			} else {
				LOGGER.debug("    Instance ({}) is already being tracked.",instanceName);
			}
		}
		
		// Remove any instances that we no longer need to track
		Set<String> currentTrackers = (Set<String>)instanceTrackerMap.getInstanceNames();
		for (String curInstance : currentTrackers) {
			if (!instancesToTrack.contains(curInstance) ) {
				LOGGER.debug("    Instance ({}) no longer needs to be tracked.  Removing from Instance Tracker Map.",curInstance);
				instanceTrackerMap.getInstanceTracker(curInstance).close();
				instanceTrackerMap.removeInstanceTrackerFromMap(curInstance);
			}
		}
		LOGGER.debug("*** Setting streams instances to track complete.");

	}

    public synchronized StreamsInstanceTracker getInstanceTracker(String instanceName) throws StreamsTrackerException {
        StreamsInstanceTracker sit = null;

        sit = instanceTrackerMap.getInstanceTracker(instanceName);
        if (sit == null) {
            throw new StreamsTrackerException(
                    StreamsTrackerErrorCode.INSTANCE_NOT_FOUND, "Instance Name " + instanceName
                            + " does not exist, or is not being tracked");
        }
        
        return sit;
    }




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

    private void updateExportedDomainMetrics() {
        metricsExporter.getStreamsMetric("status", StreamsObjectType.DOMAIN, this.domainName).set(getDomainStatusAsMetric());
        metricsExporter.getStreamsMetric("instanceCount", StreamsObjectType.DOMAIN, this.domainName).set(this.domainInfo.getInstances().size());
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
