package streams.jmx.ws.monitor;

import java.math.BigInteger;
import java.net.URL;
import java.net.MalformedURLException;
import java.text.Format;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.Timer;
import java.util.Iterator;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.UndeclaredThrowableException;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.AttributeChangeNotification;
import javax.management.NotificationFilterSupport;
import javax.management.ObjectName;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.management.NotificationListener;
import javax.management.InstanceNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.apache.commons.lang.time.StopWatch;

import com.ibm.streams.management.ObjectNameBuilder;
import com.ibm.streams.management.OperationListenerMXBean;
import com.ibm.streams.management.OperationStatusMessage;
import com.ibm.streams.management.domain.DomainMXBean;
import com.ibm.streams.management.instance.InstanceMXBean;
import com.ibm.streams.management.job.JobMXBean;
import com.ibm.streams.management.Metric;
import com.ibm.streams.management.Notifications;

import streams.jmx.ws.monitor.AllJobMetrics;

/*
 * StreamsInstanceJobMonitor
 * 	Listens for Instance notifications to help update its status
 *  Has a periodic refresh() to also update status and retrieve periodic items (e.g. allJobMetrics)
 *  
 *  Pattern: Modified Singleton Pattern
 *  Driver: The Singleton pattern is used to allow JERSEY rest classes to get the instance
 *          easily without introducing Java Dependency Injection (could do that in the future)
 *          The modification is required because the instance needs some parameters and the 
 *          traditional singleton pattern does not support that
 *  Options: Could have used property file but due to time constraints and evolution stuck with
 *           parameters
 *  Usage: StraemsInstanceJobMonitor.initInstance(param1, param2, param3, ...)
 *         StreamsInstanceJobMonitor.getInstance() 
 *           throws exception if instance was not initalized yet
 *  Future: Move to a Factory method so that multiple of these could exist for
 *          different domains / instances in a single run of the application
 */
public class StreamsInstanceJobMonitor implements NotificationListener, MXBeanSourceProviderListener {
    private static final Logger LOGGER = LoggerFactory.getLogger("root."
            + StreamsInstanceJobMonitor.class.getName());

    private static StreamsInstanceJobMonitor singletonInstance = null;

    private static boolean isInitialized = false;

    private JmxServiceContext jmxContext;
    private int refreshRateSeconds; // How often to retrieve bulk metrics
    private String protocol;

    /* Domain info */
    private String domainName = null;

    /* Instance info */
    private InstanceInfo instanceInfo = new InstanceInfo();

    /* Job Metrics Info */
    private AllJobMetrics allJobMetrics = null;
    private boolean metricsAvailable = false;
    private boolean jobsAvailable = false;

    private final Map<String, Map<String, Long>> instanceResourceMetrics = new HashMap<String, Map<String, Long>>();
    private Long instanceResourceMetricsLastUpdated = null;

    /*****************************************
     * JOB MAP and INDEXES
     **************************************/
    /* Job Map Info */
    private ConcurrentSkipListMap<BigInteger, JobDetails> jobMap = new ConcurrentSkipListMap<BigInteger, JobDetails>();
    private ConcurrentSkipListMap<String, BigInteger> jobNameIndex = new ConcurrentSkipListMap<String, BigInteger>();

    // refresher is a TimerTask that refreshes the Status and Metrics
    // automatically
    private TimerTask refresher = new TimerTask() {
        @Override
        public void run() {
            // Exceptions at this level should just be logged so that we
            // continue to refresh
            // Some are expected in recoverable situations so only log at low
            // level
            try {
                refresh();
            } catch (StreamsMonitorException e) {
                LOGGER.trace(
                        "StreamsMonitor Periodic Refresh StreamsMonitorException: {}.",
                        e);
            } catch (UndeclaredThrowableException e) {

                LOGGER.trace("StreamsMonitor Periodic Refresh UndeclaredThrowableException and unwrapping it");
                Throwable t = e.getUndeclaredThrowable();
                if (t instanceof IOException) {
                    LOGGER.trace("StreamsMonitor Periodic Refresh unwrapped IOException, we will ignore and let JMC Connecton Pool reconnect");
                } else {
                    LOGGER.debug("StreamsMonitor Period Refresh unwrapped "
                            + t.getClass()
                            + " which was unexpected, throw original undeclarable...");
                    throw e;
                }
            } catch (Exception e) {
                LOGGER.warn(
                        "StreamsMonitor Periodic Refresh Unexpected Exception: {}.  Report so it can be caught appropriately.",
                        e);
            }

        }
    };

    /*
     * Constructor
     * 
     * Note: InstanceNotFoundException is a jmx exception we use not to be
     * confused with streams instance
     */
    private StreamsInstanceJobMonitor(JmxServiceContext jmxContext,
            String domainName, String instanceName, int refreshRateSeconds,
            String protocol) throws StreamsMonitorException {
        LOGGER.trace("Constructing StreamsInstanceJobMonitor");
        this.jmxContext = jmxContext;
        this.domainName = domainName;
        this.instanceInfo.setInstanceName(instanceName);
        this.refreshRateSeconds = refreshRateSeconds;
        this.protocol = protocol;
        this.jmxContext.getBeanSourceProvider().addBeanSourceProviderListener(this);

        // ** Domain Info **
        MXBeanSource beanSource = null;
        try {
            beanSource = jmxContext.getBeanSourceProvider().getBeanSource();
            DomainMXBean domain = beanSource.getDomainBean(this.domainName);
            LOGGER.info("Domain '{}' found, Status: {}",
                    new Object[] { domain.getName(), domain.getStatus() });
        } catch (UndeclaredThrowableException e) {
            // Some InstanceNotFoundExceptions are wrapped in
            // UndeclaredThrowableExceptions sadly

            Throwable t = e.getUndeclaredThrowable();
            if (t instanceof InstanceNotFoundException) {
                LOGGER.error(
                        "Domain '{}' not found when initializing.  Ensure the JMX URL specified is for the domain you are attempting to connect to.",
                        this.domainName);
                throw new StreamsMonitorException(
                        StreamsMonitorErrorCode.DOMAIN_NOT_FOUND,
                        "Domain name "
                                + this.domainName
                                + " does not match the domain of the JMX Server.",
                        e);
            } else {
                LOGGER.trace("Unexpected exception ("
                        + t.getClass()
                        + ") when retrieving Streams domain information from JMX Server, throwing original undeclarable...");
                throw e;
            }
        } catch (MalformedURLException e) {
            throw new StreamsMonitorException(
                    StreamsMonitorErrorCode.JMX_MALFORMED_URL,
                    "Malformed URL error while retrieving domain information, domain: "
                            + this.domainName, e);
        } catch (IOException e) {
            LOGGER.error("JMX IO Exception when retrieving domain information.  Not sure why JMX Connection Pool did not retry connection");
            throw new StreamsMonitorException(
                    StreamsMonitorErrorCode.JMX_IOERROR,
                    "JMX IO error while retrieving domain information, domain: "
                            + this.domainName, e);
        }

        initStreamsInstance();

        if (this.instanceInfo.isInstanceAvailable()) {
            updateInstanceResourceMetrics();
            initAllJobs();
        }

        // Create timer to automatically refresh the status and metrics
        Timer timer = new Timer("Refresher");
        timer.scheduleAtFixedRate(refresher, refreshRateSeconds * 1000,
                refreshRateSeconds * 1000);

    }

    public static StreamsInstanceJobMonitor initInstance(
            JmxServiceContext jmxContext, String domainName,
            String instanceName, int refreshRateSeconds, String protocol)
            throws StreamsMonitorException {
        if (singletonInstance != null) {
            LOGGER.warn("Re-Initializing StreamsInstanceJobMonitor");
        } else {
            LOGGER.info("Initializing StreamsInstanceJobMonitor");
        }

        try {
            singletonInstance = new StreamsInstanceJobMonitor(jmxContext,
                    domainName, instanceName, refreshRateSeconds, protocol);
            StreamsInstanceJobMonitor.isInitialized = true;
        } catch (StreamsMonitorException e) {
            LOGGER.error("Initalization of StreamsInstanceJobMonitor instance FAILED!!");
            throw e;
        }

        return singletonInstance;
    }

    // Do not confuse with a Streams Instance, this refers to the instance of
    // this class
    public static StreamsInstanceJobMonitor getInstance()
            throws StreamsMonitorException {
        if (!StreamsInstanceJobMonitor.isInitialized) {
            LOGGER.warn("An attempt to retrieve the instance of StreamsInstanceJobMonitor was made before it was initialized");
            throw new StreamsMonitorException(
                    StreamsMonitorErrorCode.STREAMS_MONITOR_UNAVAILABLE,
                    "StreamsInstanceJobMonitor is not initialized");
        }
        return singletonInstance;

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

    public synchronized Long getInstanceResourceMetricsLastUpdated() {
        return instanceResourceMetricsLastUpdated;
    }

    public synchronized Map<BigInteger, JobInfo> getCurrentJobMap() {
        HashMap<BigInteger, JobInfo> m = new HashMap<BigInteger, JobInfo>();

        Iterator<Map.Entry<BigInteger, JobDetails>> it = jobMap.entrySet()
                .iterator();

        while (it.hasNext()) {
            Map.Entry<BigInteger, JobDetails> entry = it.next();

            m.put(entry.getKey(), entry.getValue().getJobInfo());
        }

        return m;
    }

    public synchronized Map<String, BigInteger> getCurrentJobNameIndex() {
        return new HashMap<String, BigInteger>(jobNameIndex);
    }

    public synchronized InstanceInfo getInstanceInfo() throws StreamsMonitorException {
        verifyInstanceExists();

        return instanceInfo;
    }

    private void verifyInstanceExists() throws StreamsMonitorException {
        if (instanceInfo == null) {
            throw new StreamsMonitorException(
                    StreamsMonitorErrorCode.INSTANCE_NOT_FOUND,
                    "The InstanceInfo object does not exist.  This error should not occur.");
        } else if (!this.instanceInfo.isInstanceExists()) {
            throw new StreamsMonitorException(
                    StreamsMonitorErrorCode.INSTANCE_NOT_FOUND,
                    "The Streams instance "
                            + this.instanceInfo.getInstanceName()
                            + " does not exist.");
        }
    }

    public synchronized Map<String, Map<String, Long>> getInstanceResourceMetrics() throws StreamsMonitorException {
        verifyInstanceExists();

        synchronized (instanceResourceMetrics) {
            return instanceResourceMetrics;
        }
    }

    private synchronized void updateInstanceResourceMetrics() throws StreamsMonitorException {
        verifyInstanceExists();

        MXBeanSource beanSource = null;
                
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
            throw new StreamsMonitorException("Invalid JMX URL when retrieving instance bean", me);
        }
        catch (IOException ioe) {
            throw new StreamsMonitorException("JMX IO Exception when retrieving instance bean", ioe);
        }
    }

    public synchronized AllJobMetrics getAllJobMetrics() throws StreamsMonitorException {

        if ((this.instanceInfo == null)
                || (!this.instanceInfo.isInstanceExists())) {
            throw new StreamsMonitorException(
                    StreamsMonitorErrorCode.ALL_METRICS_NOT_AVAILABLE,
                    "The Streams instance "
                            + this.instanceInfo.getInstanceName()
                            + " does not exist.");
        } else if (allJobMetrics == null) {
            throw new StreamsMonitorException(
                    StreamsMonitorErrorCode.ALL_METRICS_NOT_AVAILABLE,
                    "The allJobMetrics object does not exist. Metrics have never been able to be retrieved.");
        }

        return allJobMetrics;
    }

    public synchronized ArrayList<JobInfo> getAllJobInfo() throws StreamsMonitorException {
        ArrayList<JobInfo> jia = new ArrayList<JobInfo>();

        if ((this.instanceInfo == null)
                || (!this.instanceInfo.isInstanceExists())) {
            throw new StreamsMonitorException(
                    StreamsMonitorErrorCode.ALL_JOBS_NOT_AVAILABLE,
                    "The Streams instance "
                            + this.instanceInfo.getInstanceName()
                            + " does not exist.");
        }

        if (jobsAvailable) {

            Iterator<Map.Entry<BigInteger, JobDetails>> it = jobMap.entrySet()
                    .iterator();
            while (it.hasNext()) {
                Map.Entry<BigInteger, JobDetails> pair = it.next();

                JobDetails curInfo = (JobDetails) pair.getValue();
                jia.add(curInfo.getJobInfo());
            }
        }
        return jia;
    }

    public synchronized JobInfo getJobInfo(int jobid) throws StreamsMonitorException {
        BigInteger jid = BigInteger.valueOf(jobid);
        JobDetails jd = null;
        JobInfo ji = null;

        if (jobMap == null) {
            throw new StreamsMonitorException(
                    StreamsMonitorErrorCode.JOB_NOT_FOUND, "Job id " + jobid
                            + " does not exist");
        }
        jd = jobMap.get(jid);
        if (jd == null) {
            throw new StreamsMonitorException(
                    StreamsMonitorErrorCode.JOB_NOT_FOUND, "Job id " + jobid
                            + " does not exist");
        } else {
            ji = jd.getJobInfo();
        }
        return ji;
    }

    public synchronized String getJobSnapshot(int jobid, int maximumDepth,
            boolean includeStaticAttributes) throws StreamsMonitorException {
        BigInteger jid = BigInteger.valueOf(jobid);
        JobDetails jd = jobMap.get(jid);

        if (jd == null) {
            throw new StreamsMonitorException(
                    StreamsMonitorErrorCode.JOB_NOT_FOUND, "Job id " + jobid
                            + " does not exist");
        }

        return jd.getSnapshot(maximumDepth, includeStaticAttributes);
    }

    /*
     * Primary server refresher. Not a polling system, but when things go down
     * we need a way to recover and reset everything.
     */
    public synchronized void refresh() throws StreamsMonitorException {
        // No longer need to update all job status because we should get
        // notified
        LOGGER.trace("*** entering refresh()");
				LOGGER.trace("    current state: isInstanceAvailable: {}, jobsAvailable: {}, metricsAvailable {}",this.instanceInfo.isInstanceAvailable(), jobsAvailable, metricsAvailable);
        
        if (!this.instanceInfo.isInstanceAvailable()) {
        		LOGGER.trace("*** Calling initStreamsInstance()");
            initStreamsInstance();
        }

        if (instanceInfo.isInstanceAvailable()) {
        		LOGGER.trace("*** Calling updateInstanceResourceMetrics()");
            updateInstanceResourceMetrics();
        }

        // Need to know if instance was unavailable on previous run so we
        // re-initialize jobs
        if (!jobsAvailable) {
        		LOGGER.trace("*** Calling initAllJobs()");
            initAllJobs();
        }

        if (metricsAvailable) {
        		LOGGER.trace("*** Calling updateAllJobMetrics(true)");
            updateAllJobMetrics(true);
        }

    }

    String getProtocol() {
        return protocol;
    }

    /*
     * resetMonitor In the case of a JMX error or anything else that could have
     * invalidated our state reset the state so that the instance, jobs, and
     * metrics are re-initialized and brought back into consistency with
     * Streams.
     */
    synchronized void resetMonitor() {
        this.instanceInfo.setInstanceAvailable(false);
        this.jobsAvailable = false;
        this.metricsAvailable = false;
        // Set Metrics Failure on metrics Object
        if (this.allJobMetrics != null) {
        	this.allJobMetrics.setLastMetricsFailure(new Date());
        	this.allJobMetrics.setLastMetricsRefreshFailed(true);
        }
    }

    /*
     * clearMonitor In the case that the Streams instance is stopped/fails we
     * will not be able to recover the metrics or jobs so clear them out
     */
    private synchronized void clearMonitor() {
        instanceResourceMetrics.clear();
        this.allJobMetrics.clear();
        this.jobMap.clear();
    }

    /*
     * InitInstance If you initialize the instance, you must make jobs and
     * metrics initialize as well
     * 
     * Exceptions thrown by this method will impact the program differently
     * depending on if it is the first time it is run (on construction) in which
     * case it will cauase the program to exit vs it is part of the recurring
     * refresh, in which case we assume it is a recoverable exception (e.g. jmx
     * connection failure)and we will just try to re-initalize on the next
     * refresh.
     */
    private synchronized void initStreamsInstance() throws StreamsMonitorException {
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
                resetMonitor();
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
                resetMonitor();
                return;
            } else {
                // Force jobs and metrics to initialize by setting instance as
                // available
                LOGGER.info("Instance '{}' found, Status: {}", new Object[] {
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
                resetMonitor();
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
            resetMonitor();
            // throw new StreamsMonitorException("Instance MXBean not found when
            // initializing instance");
        } catch (MalformedURLException me) {
            resetMonitor();
            throw new StreamsMonitorException(
                    "Invalid JMX URL when initializing instance", me);
        } catch (IOException ioe) {
            // JMX Error, cannot initialize streams instance so ensure state
            // variables reflect and return
            LOGGER.warn("JMX IO Exception when initializing instance, Continuing to wait for reconnect");
            resetMonitor();
        }
    }

    // Initialize our tracking of jobs and metrics
    // Used at startup and when we have lost contact to JMX or Instance
    private synchronized void initAllJobs() throws StreamsMonitorException {

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

            try {
                // Try to only create it if it does not exist and rely on the
                // clearing of the metrics so we preserve our timing attributes
                if (allJobMetrics == null) {
                    allJobMetrics = new AllJobMetrics(this.jmxContext,
                            this.domainName,
                            this.instanceInfo.getInstanceName(), this.protocol);
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

                // Do not refresh from server because the constructor pulled
                // them
                if (metricsAvailable)
                    updateAllJobMetrics(false);
            } catch (IOException e) {
                LOGGER.warn("JMX IO Exception when initializing all job metrics, resetting monitor. Exception message: "
                        + e.getLocalizedMessage());
                resetMonitor();
            }

            if (LOGGER.isDebugEnabled()) {
                stopwatch.stop();
                timers.put("updateAllJobMetrics(false)", stopwatch.getTime());
                LOGGER.debug("Profiling for Inializing job map");
                for (Map.Entry<String, Long> entry : timers.entrySet()) {
                    LOGGER.debug("Timing for " + entry.getKey() + ": "
                            + entry.getValue());
                }
            }

        }

    }

    // Initialize our JobMap
    // Used at startup and when we have lost contact to JMX or Instance
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

                LOGGER.debug("Get list of all jobs...");
                Set<BigInteger> jobs = null;

                jobs = jmxContext.getBeanSourceProvider().getBeanSource()
                        .getInstanceBean(domainName, instanceName).getJobs();

                if (LOGGER.isDebugEnabled()) {
                    stopwatch.stop();
                    timers.put("getJobs", stopwatch.getTime());
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
                LOGGER.debug("Waiting for jobs to be registered...");
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
                    timers.put("registerJobs", stopwatch.getTime());
                    stopwatch.reset();
                    stopwatch.start();
                }

                // Populate the map of jobs and create jobInfo objects with
                // jobMXBeans
                // Create the jobname index
                jobMap.clear();
                jobNameIndex.clear();

                LOGGER.trace("Create hashmap of JobMXBeans...");
                for (BigInteger jobno : jobs) {
                    ObjectName tJobNameObj = ObjectNameBuilder.job(domainName,
                            instanceName, jobno);
                    JobMXBean jobBean = JMX.newMXBeanProxy(jmxContext
                            .getBeanSourceProvider().getBeanSource()
                            .getMBeanServerConnection(), tJobNameObj,
                            JobMXBean.class, true);
                    jobMap.put(jobno, new JobDetails(this, jobno, jobBean));
                    jobNameIndex.put(jobBean.getName(), jobno);
                }

                // IMPORTANT: Set jobsAvailable to true
                jobsAvailable = true;

            } catch (IOException e) {
                // An IOException at this point means the jmx connection is
                // probably lost. Reset Monitor and continue to wait for it to
                // reconnect
                LOGGER.warn("Job Map Initialization received IO Exception from JMX Connection Pool.  Resetting monitor.  Exception Message: "
                        + e.getLocalizedMessage());
                resetMonitor();
            }

            if (LOGGER.isDebugEnabled()) {
                stopwatch.stop();
                timers.put("create JobMXBeans", stopwatch.getTime());
                LOGGER.debug("Profiling for Inializing job map");
                for (Map.Entry<String, Long> entry : timers.entrySet()) {
                    LOGGER.debug("Timing for " + entry.getKey() + ": "
                            + entry.getValue());
                }
            }
        }

    }

    // ** Add Job to job map
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
            jobMap.put(jobid, new JobDetails(this, jobid, jobBean));
            jobNameIndex.put(jobBean.getName(), jobid);

        } catch (IOException e) {
            LOGGER.warn("New Job Initialization received IO Exception from JMX Connection Pool.  Resetting monitor.  Exception Message: "
                    + e.getLocalizedMessage());
            resetMonitor();
        }

        sw.stop();
        LOGGER.debug("** addJobToMap (jobid: " + jobid + ") time: "
                + sw.getTime());

    }

    private synchronized void removeJobFromMap(BigInteger jobid) {
        LOGGER.debug("removeJobFromMap({})...", jobid);
        StopWatch sw = new StopWatch();
        sw.start();

        // Do we need to do anything to the MXBean we created for the job?
        jobMap.remove(jobid);
        // Need to remove it from the jobNameIndex
        jobNameIndex.values().removeAll(Collections.singleton(jobid));

        sw.stop();
        LOGGER.debug("** removeJobFromMap (jobid: " + jobid + ") time: "
                + sw.getTime());

    }

    /*
     * updateAllJobMetrics Need to use this to determine when JMX has been reset
     * so we invalidate the job beans
     */
    private synchronized void updateAllJobMetrics(boolean refreshFromServer)
            throws StreamsMonitorException {
        LOGGER.trace("***** Entered updateAllJobMetrics, refreshFromServer {}",
                refreshFromServer);
        StopWatch sw = new StopWatch();

        sw.reset();
        sw.start();
        if (jobsAvailable) {
            // Refresh Metrics if requested
            if (refreshFromServer) {
                try {
                    this.allJobMetrics.refresh();
                } catch (IOException e) {
                    LOGGER.error("Updating all metrics received IO Exception from JMX Connection Pool.  Resetting monitor.  Exception Message: "
                            + e.getLocalizedMessage());
                    resetMonitor();
                }
            }

            if (allJobMetrics.isLastMetricsRefreshFailed()) {
                // If retrieving metrics fails, we need to loop through the jobs
                // and set the attributes to reflect that

                Iterator<Map.Entry<BigInteger, JobDetails>> it = jobMap
                        .entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<BigInteger, JobDetails> pair = it.next();
                    JobDetails curInfo = (JobDetails) pair.getValue();
                    curInfo.setLastMetricsFailure(allJobMetrics
                            .getLastMetricsFailure());
                    curInfo.setLastMetricsRefreshFailed(true);
                }
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
                            // Have we seen this job before? If not thats a
                            // problem
                            // we
                            // missed the notification
                            if (jobMap.containsKey(jobId)) {
                                JobDetails jd = jobMap.get(jobId);
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
                                        "Received Metrics for jobId({}) that is not found in the current jobArray, missed notification of new job is likely cause",
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

        }
        sw.stop();
        LOGGER.trace("** updateAllJobMetrics time: " + sw.getTime());

        LOGGER.trace("Exited");

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
        result.append("instanceResourceMetricsLastUpdated:" + convertTime(instanceResourceMetricsLastUpdated));
        result.append(newline);
        if (jobsAvailable) {
            result.append("All " + jobMap.size() + " Jobs:");
            result.append(newline);
            Iterator<Map.Entry<BigInteger, JobDetails>> it = jobMap.entrySet()
                    .iterator();

            while (it.hasNext()) {
                Map.Entry<BigInteger, JobDetails> pair = it.next();
                JobDetails curInfo = (JobDetails) pair.getValue();
                result.append(curInfo.toString());
                result.append(newline);
            }
            result.append(newline);
            result.append("jobNameIndex:");
            result.append(newline);
            Iterator<Map.Entry<String, BigInteger>> jnit = jobNameIndex
                    .entrySet().iterator();
            while (jnit.hasNext()) {
                Map.Entry<String, BigInteger> pair = jnit.next();
                result.append(pair.getKey() + " : " + pair.getValue());
                result.append(newline);
            }
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

    public void printJob(BigInteger jobid) {
        System.out.println("Job Status: " + jobMap.get(jobid).getStatus());
        System.out.println("Job Metrics: " + jobMap.get(jobid).getJobMetrics());
    }

    /*
     * Instance handleNotification
     * 
     * Primary interface to listen for changes to the instance we are monitoring
     * Only interested in specific notifications so should implement filter soon
     */
    public void handleNotification(Notification notification, Object handback) {
        String notificationType = notification.getType();
        LOGGER.trace("Streams Instance Notification: " + notification
                + "; User Data: " + notification.getUserData());

        switch (notificationType) {
        case AttributeChangeNotification.ATTRIBUTE_CHANGE:
            AttributeChangeNotification acn = (AttributeChangeNotification) notification;
            String attributeName = acn.getAttributeName();
            if (attributeName.equals("Status")) {
                InstanceMXBean.Status newValue = (InstanceMXBean.Status) acn
                        .getNewValue();
                InstanceMXBean.Status oldValue = (InstanceMXBean.Status) acn
                        .getOldValue();
                LOGGER.debug("Instance Status Changed from: " + oldValue
                        + " to: " + newValue);
                this.instanceInfo.setInstanceStatus((InstanceMXBean.Status) acn
                        .getNewValue());
                if (newValue.equals(InstanceMXBean.Status.STOPPED)
                        || newValue.equals(InstanceMXBean.Status.FAILED)
                        || newValue.equals(InstanceMXBean.Status.UNKNOWN)) {
                    LOGGER.info("Instance Status ("
                            + newValue
                            + "), monitor will reset and reinitialize when instance is available");
                    this.instanceInfo.setInstanceStartTime(null);
                    resetMonitor();
                    clearMonitor();
                }
            }
            break;
        case Notifications.INSTANCE_DELETED:
            LOGGER.debug("Instance deleted from domain, resetting monitor and waiting for instance to be recreated");
            this.instanceInfo.setInstanceExists(false);
            resetMonitor();
            clearMonitor();
            break;
        case Notifications.JOB_ADDED:
            LOGGER.debug("****** Job add notification, Jobid : "
                    + notification.getUserData());
            addJobToMap((BigInteger) notification.getUserData());
            break;
        case Notifications.JOB_REMOVED:
            LOGGER.debug("******** Job removed notification, userData: "
                    + notification.getUserData());
            // We are only listening on Straems Instance, so user data is a
            // jobid
            removeJobFromMap((BigInteger) notification.getUserData());
            break;
        }

    }

    @Override
    public void beanSourceInterrupted(MXBeanSource bs) {
        LOGGER.debug("***** BeanSource interrupted, resetting monitor...");
        resetMonitor();
    }
}
