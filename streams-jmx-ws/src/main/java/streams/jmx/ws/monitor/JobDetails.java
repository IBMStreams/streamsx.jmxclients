package streams.jmx.ws.monitor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigInteger;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.management.AttributeChangeNotification;
import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationFilterSupport;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.apache.commons.lang.time.StopWatch;

import com.ibm.streams.management.Notifications;
import com.ibm.streams.management.ObjectNameBuilder;
import com.ibm.streams.management.job.JobMXBean;
import com.ibm.streams.management.job.OperatorMXBean;
import com.ibm.streams.management.job.OperatorInputPortMXBean;
import com.ibm.streams.management.job.OperatorOutputPortMXBean;

class JobDetails implements NotificationListener {
    private static final Logger LOGGER = LoggerFactory.getLogger("root." + StreamsInstanceJobMonitor.class.getName());

    private StreamsInstanceJobMonitor monitor;
    private BigInteger jobid;
    private JobMXBean jobBean;
    private JobMXBean.Status status;
    private String jobMetrics = null;
    // private String jobResolvedMetrics = null;
    private Date lastMetricsRefresh = null;
    private Date lastMetricsFailure = null;
    private boolean lastMetricsRefreshFailed = false;

    private String adlFile = null;
    private String applicationName = null;
    private String applicationPath = null;
    private String applicationScope = null;
    private String applicationVersion = null;
    private String dataPath = null;
    private String domain = null;
    private JobMXBean.Health health = JobMXBean.Health.UNKNOWN;
    private String instance = null;
    private String jobGroup = null;
    private String name = null;
    private String outputPath = null;
    private String startedByUser = null;
    private long submitTime = 0;

    private final Map<String, Map<Integer, String>> operatorInputPortNames  = new HashMap<String, Map<Integer, String>>();
    private final Map<String, Map<Integer, String>> operatorOutputPortNames = new HashMap<String, Map<Integer, String>>();
    
    public JobDetails(StreamsInstanceJobMonitor monitor, BigInteger jobid, JobMXBean jobBean) {
        this.monitor = monitor;
        
        setJobid(jobid);
        setJobBean(jobBean);
        setStatus(JobMXBean.Status.UNKNOWN);
        setJobMetrics(null);

        MXBeanSource beanSource = null;
        MBeanServerConnection mbsc = null;
        try {
            beanSource = monitor.getContext().getBeanSourceProvider().getBeanSource();
            mbsc = beanSource.getMBeanServerConnection();
        } catch (IOException e) {
            String message = "jobInfo Constructor: Exception getting MBeanServerConnection from JMX Connection Pool"; 
            LOGGER.error(message, e);
            
            throw new IllegalStateException(e);
        }

        this.setStatus(jobBean.getStatus());
        this.setAdlFile(jobBean.getAdlFile());
        this.setApplicationName(jobBean.getApplicationName());
        this.setApplicationPath(jobBean.getApplicationPath());
        this.setApplicationScope(jobBean.getApplicationScope());
        this.setApplicationVersion(jobBean.getApplicationVersion());
        this.setDataPath(jobBean.getDataPath());
        this.setDomain(jobBean.getDomain());
        this.setHealth(jobBean.getHealth());
        this.setInstance(jobBean.getInstance());
        this.setJobGroup(jobBean.getJobGroup());
        this.setName(jobBean.getName());
        this.setOutputPath(jobBean.getOutputPath());
        this.setStartedByUser(jobBean.getStartedByUser());
        this.setSubmitTime(jobBean.getSubmitTime());

        // Setup notifications (should handle exceptions)
        try {
            ObjectName jobObjName = ObjectNameBuilder.job(domain,
                    instance, jobid);
            NotificationFilterSupport filter = new NotificationFilterSupport();
            // Only worry about changes that may be status, instance level
            // handles removal of jobs
            filter.enableType(Notifications.JOB_CHANGED);
            filter.enableType(AttributeChangeNotification.ATTRIBUTE_CHANGE);
            mbsc.addNotificationListener(jobObjName, this, filter, null);
        } catch (Exception e) {
            String message = "Error setting up job notification for JMX";
            LOGGER.error(message, e);
            
            throw new IllegalStateException(e);
        }

        try {
            mapPortNames(beanSource);
        } catch (Exception e) {
            String message = "Unable to create port name map";
            LOGGER.error(message, e);
            
            throw new IllegalStateException(e);
        }
    }

    public BigInteger getJobid() {
        return jobid;
    }

    public void setJobid(BigInteger jobid) {
        this.jobid = jobid;
    }

    public JobMXBean getJobBean() {
        return jobBean;
    }

    public void setJobBean(JobMXBean jobBean) {
        this.jobBean = jobBean;
    }

    public String getJobMetrics() {
        return jobMetrics;
    }

    public void setJobMetrics(String jobMetrics) {
        this.jobMetrics = jobMetrics;
    }

    public JobMXBean.Status getStatus() {
        return status;
    }

    public void setStatus(JobMXBean.Status status) {
        this.status = status;
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

    public String getAdlFile() {
        return adlFile;
    }

    public void setAdlFile(String adlFile) {
        this.adlFile = adlFile;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getApplicationPath() {
        return applicationPath;
    }

    public void setApplicationPath(String applicationPath) {
        this.applicationPath = applicationPath;
    }

    public String getApplicationScope() {
        return applicationScope;
    }

    public void setApplicationScope(String applicationScope) {
        this.applicationScope = applicationScope;
    }

    public String getApplicationVersion() {
        return applicationVersion;
    }

    public void setApplicationVersion(String applicationVersion) {
        this.applicationVersion = applicationVersion;
    }

    public String getDataPath() {
        return dataPath;
    }

    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public JobMXBean.Health getHealth() {
        return health;
    }

    public void setHealth(JobMXBean.Health health) {
        this.health = health;
    }

    public String getInstance() {
        return instance;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }

    public String getJobGroup() {
        return jobGroup;
    }

    public void setJobGroup(String jobGroup) {
        this.jobGroup = jobGroup;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    public String getStartedByUser() {
        return startedByUser;
    }

    public void setStartedByUser(String startedByUser) {
        this.startedByUser = startedByUser;
    }

    public long getSubmitTime() {
        return submitTime;
    }

    public void setSubmitTime(long submitTime) {
        this.submitTime = submitTime;
    }

    /*
     * getJobInfo Creates a JobInfo representation of this class with less
     * information
     */
    public JobInfo getJobInfo() {

        JobInfo ji = new JobInfo();
        ji.setAdlFile(adlFile);
        ji.setApplicationName(applicationName);
        ji.setApplicationPath(applicationPath);
        ji.setApplicationScope(applicationScope);
        ji.setApplicationVersion(applicationVersion);
        ji.setDataPath(dataPath);
        ji.setDomain(domain);
        ji.setHealth(health);
        ji.setId(getJobid());
        ji.setInstance(instance);
        ji.setJobGroup(jobGroup);
        ji.setJobMetrics(jobMetrics);
        ji.setLastMetricsFailure(lastMetricsFailure);
        ji.setLastMetricsRefresh(lastMetricsRefresh);
        ji.setLastMetricsRefreshFailed(lastMetricsRefreshFailed);
        ji.setName(name);
        ji.setOutputPath(outputPath);
        ji.setStartedByUser(startedByUser);
        ji.setStatus(status);
        ji.setSubmitTime(submitTime);
        ji.setJobMetrics(resolvePortNames(jobMetrics));

        return ji;

    }

    public void updateStatus() throws IOException {
        LOGGER.debug("** In updateStatus for job " + this.getJobid());
        // Be careful with timing and just in case the notification of job
        // removal is delayed, catch exception if job is gone before we
        // process notification
        // Found issue with Streams JMX, does not declare that
        // instanceNotFOundException thrown so comes out as
        // UndeclaredThrowableException
        try {
            JobMXBean jobBean = this.getJobBean();
            this.setStatus(jobBean.getStatus());

            // While we are here, update everything from the bean
            this.setAdlFile(jobBean.getAdlFile());
            this.setApplicationName(jobBean.getApplicationName());
            this.setApplicationPath(jobBean.getApplicationPath());
            this.setApplicationScope(jobBean.getApplicationScope());
            this.setApplicationVersion(jobBean.getApplicationVersion());
            this.setDataPath(jobBean.getDataPath());
            this.setDomain(jobBean.getDomain());
            this.setHealth(jobBean.getHealth());
            this.setInstance(jobBean.getInstance());
            this.setJobGroup(jobBean.getJobGroup());
            this.setName(jobBean.getName());
            this.setOutputPath(jobBean.getOutputPath());
            this.setStartedByUser(jobBean.getStartedByUser());
            this.setSubmitTime(jobBean.getSubmitTime());

        } catch (UndeclaredThrowableException e) {
            LOGGER.debug("* Handling jobBean.getStatus() UndeclaredThrowableException and unwrapping it");
            Throwable t = e.getUndeclaredThrowable();
            if (t instanceof IOException) {
                LOGGER.debug("*    It was an IOException we can handle, throwing the IOException");
                throw (IOException) t;
            } else {
                LOGGER.debug("*    It was an "
                        + t.getClass()
                        + " which was unexpected, throw original undeclarable...");
                throw e;
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        // String newline = System.getProperty("line.separator");
        result.append("Job: " + this.getJobid() + ": " + this.getStatus());
        result.append(", applicationName: " + this.getApplicationName());
        result.append(" Metrics: " + this.getJobMetrics());
        return result.toString();
    }

    /*
     * getJobSnapshot: method to grab snapshot of job from JMX Server
     */

    public String getSnapshot(int maximumDepth,
            boolean includeStaticAttributes) throws StreamsMonitorException {

        StringBuilder newSnapshot = new StringBuilder();

        // Create hashMap for timing Stuff
        LinkedHashMap<String, Long> timers = new LinkedHashMap<String, Long>();
        StopWatch stopwatch = new StopWatch();
        String uri = null;

        /**** JMX Interaction *****/
        try {
            stopwatch.start();
            /***
             * ISSUE: snapshotJobMetrics does not declare it throws
             * IOException but it does and comes back to us as
             * UndeclaredThrowableException, handle that here
             */
            try {
                uri = this.getJobBean().snapshot(maximumDepth,
                        includeStaticAttributes);
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
            stopwatch.stop();
            timers.put("job.snapshot", stopwatch.getTime());

        } catch (IOException e) {
            // IOException from JMX usually means server restarted or domain
            LOGGER.warn("** job.snapshot JMX Interaction IOException **");
            LOGGER.info("details", e);

            throw new StreamsMonitorException(
                    StreamsMonitorErrorCode.JMX_IOERROR,
                    "Unable to retrieve snapshots at this time.", e);

        } catch (Exception e) {
            throw new StreamsMonitorException(
                    StreamsMonitorErrorCode.UNSPECIFIED_ERROR,
                    "Unable to retrieve snapshots at this time.", e);
        }

        /******* HTTPS Interaction ********/
        try {
            LOGGER.trace("* job.snapshot * Connect to snapshot URI and retrieve...");
            stopwatch.reset();
            stopwatch.start();
            // set up trust manager
            newSnapshot.append(monitor.getContext().getWebClient().get(uri));

            stopwatch.stop();
            timers.put("connect and retrieve snapshot", stopwatch.getTime());

        } catch (Exception e) {
            throw new StreamsMonitorException(
                    StreamsMonitorErrorCode.UNSPECIFIED_ERROR,
                    "Unable to retrieve snapshots at this time.", e);
        }

        LOGGER.debug("job.shapshot timings:");
        for (Map.Entry<String, Long> entry : timers.entrySet()) {
            LOGGER.debug(entry.getKey() + ": " + entry.getValue());
        }

        LOGGER.trace("Exited");

        return String.valueOf(newSnapshot);
    }

    /*
     * JobDetails: handleNotification Original version just listened for any
     * kind of notification and then went and pulled the new status Moving
     * to a specific processing of the notification.
     */
    public void handleNotification(Notification notification,
            Object handback) {
        String notificationType = notification.getType();
        LOGGER.trace("** Job Notification: " + notification);

        switch (notificationType) {

        case AttributeChangeNotification.ATTRIBUTE_CHANGE:
            AttributeChangeNotification acn = (AttributeChangeNotification) notification;
            LOGGER.debug("** Job Notification: attribute changed: " + acn);
            // Need to be specific, but for now, if any attribute changes,
            // updateStatus() will update them all
            try {
                this.updateStatus();
            } catch (IOException e) {
                // Assuming this means that JMX connection was lost, mark
                // everything as unavailable
                monitor.resetMonitor();
            }

            break;
        }
    }
    
    private void mapPortNames(MXBeanSource beanSource) {
        Set<String> operators = getJobBean().getOperators();

        for (String operatorName : operators) {
            OperatorMXBean operatorBean = beanSource.getOperatorMXBean(getDomain(), getInstance(), getJobid(), operatorName);
            mapOperatorInputPortNames(beanSource, operatorName, operatorBean.getInputPorts());
            mapOperatorOutputPortNames(beanSource, operatorName, operatorBean.getOutputPorts());
        }
    }
        
    @SuppressWarnings("unchecked")
    private void mapOperatorInputPortNames(MXBeanSource beanSource, String operatorName, Set<Integer> inputPorts) {
        for (Integer portIndex : inputPorts) {
            OperatorInputPortMXBean bean = beanSource.getOperatorInputPortMXBean(
                    getDomain(), getInstance(), getJobid(), operatorName, portIndex);
            
            Map<Integer, String> inputPortNames = operatorInputPortNames.get(operatorName);
            
            if (inputPortNames == null) {
                inputPortNames = new HashMap<Integer, String>();
                operatorInputPortNames.put(operatorName, inputPortNames);
            }
            
            inputPortNames.put(portIndex, bean.getName());
        }   
    }
    
    @SuppressWarnings("unchecked")
    private void mapOperatorOutputPortNames(MXBeanSource beanSource, String operatorName, Set<Integer> outputPorts) {
        for (Integer portIndex : outputPorts) {
            OperatorOutputPortMXBean bean = beanSource.getOperatorOutputPortMXBean(
                    getDomain(), getInstance(), getJobid(), operatorName, portIndex);
            
            Map<Integer, String> outputPortNames = operatorOutputPortNames.get(operatorName);
            
            if (outputPortNames == null) {
                outputPortNames = new HashMap<Integer, String>();
                operatorOutputPortNames.put(operatorName, outputPortNames);
            }
            
            outputPortNames.put(portIndex, bean.getName());
        }
    }
    
    private String resolvePortNames(String metricsSnapshot) {
        if (metricsSnapshot != null) {

            JSONParser parser = new JSONParser();
            try {
                JSONObject metricsObject = (JSONObject) parser.parse(metricsSnapshot);
    
                JSONArray peArray = (JSONArray) metricsObject.get("pes");
        
                for (int i = 0; i < peArray.size(); i++) {
                    JSONObject pe = (JSONObject) peArray.get(i);
            
//            resolvePeInputPortNames(pe);
//            resolvePeOutputPortNames(pe);
            
                    JSONArray operatorArray = (JSONArray) pe.get("operators");

                    for (int j = 0; j < operatorArray.size(); j++) {
                        JSONObject operator = (JSONObject) operatorArray.get(j);
                
                        resolveOperatorInputPortNames(operator);
                        resolveOperatorOutputPortNames(operator);
                    }
                }

                metricsSnapshot = metricsObject.toJSONString();
            }
            catch (ParseException e) {
                throw new IllegalStateException(e);
            }
        }

        return metricsSnapshot;
    }
    
    @SuppressWarnings("unchecked")
    private void resolveOperatorInputPortNames(JSONObject operator) {
        JSONArray inputPortArray = (JSONArray) operator.get("inputPorts");
        
        if (inputPortArray == null) {
            return;
        }
        
        String operatorName = getOperatorName(operator);
        Map<Integer, String> inputPortNames = operatorInputPortNames.get(operatorName);

        if (inputPortNames == null) {
            return;
        }
        
        for (int i = 0; i < inputPortArray.size(); i++) {
            JSONObject inputPort = (JSONObject) inputPortArray.get(i);
            int portIndex = getOperatorPortIndex(inputPort);            
            String portName = inputPortNames.get(portIndex);
            
            if (portName != null) {
                inputPort.put("name", portName);                         
            }
        }   
    }
    
    @SuppressWarnings("unchecked")
    private void resolveOperatorOutputPortNames(JSONObject operator) {
        JSONArray outputPortArray = (JSONArray) operator.get("outputPorts");
        
        if (outputPortArray == null) {
            return;
        }
        
        String operatorName = getOperatorName(operator);
        Map<Integer, String> outputPortNames = operatorOutputPortNames.get(operatorName);

        if (outputPortNames == null) {
            return;
        }
        
        for (int i = 0; i < outputPortArray.size(); i++) {
            JSONObject outputPort = (JSONObject) outputPortArray.get(i);
            int portIndex = getOperatorPortIndex(outputPort);
            String portName = outputPortNames.get(portIndex);
            
            if (portName != null) {
                outputPort.put("name",  portName);
            }
        }
    }
    
    private String getOperatorName(JSONObject operator) {
        return operator.get("name").toString();
    }
        
    private int getOperatorPortIndex(JSONObject port) {
        return ((Number) port.get("indexWithinOperator")).intValue();
    }    
}
