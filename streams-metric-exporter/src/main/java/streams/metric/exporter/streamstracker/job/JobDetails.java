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

package streams.metric.exporter.streamstracker.job;

import java.io.IOException;

import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigInteger;
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

import streams.metric.exporter.error.StreamsTrackerErrorCode;
import streams.metric.exporter.error.StreamsTrackerException;
import streams.metric.exporter.jmx.MXBeanSource;
import streams.metric.exporter.metrics.MetricsExporter;
import streams.metric.exporter.metrics.MetricsExporter.StreamsObjectType;
import streams.metric.exporter.prometheus.PrometheusMetricsExporter;
import streams.metric.exporter.streamstracker.StreamsInstanceTracker;

/* Job Details including map of port names so metrics can have names for ports rather than just ids */
public class JobDetails implements NotificationListener {
	private static final Logger LOGGER = LoggerFactory.getLogger("root." + StreamsInstanceTracker.class.getName());

	private StreamsInstanceTracker monitor;
	private String streamsInstanceName;
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

	private final Map<String, Map<Integer, String>> operatorInputPortNames = new HashMap<String, Map<Integer, String>>();
	private final Map<String, Map<Integer, String>> operatorOutputPortNames = new HashMap<String, Map<Integer, String>>();

	/* Metrics Exporter*/
	/* Temporary solution: always use Prometheus exporter */
	/* Future: Make this pluggable, add Elasticsearch exporter */
	private MetricsExporter metricsExporter = PrometheusMetricsExporter.getInstance();
	
	public JobDetails(StreamsInstanceTracker monitor, BigInteger jobid, JobMXBean jobBean) {
		this.monitor = monitor;

		try {
			this.streamsInstanceName = monitor.getInstanceInfo().getInstanceName();
		} catch (StreamsTrackerException sme) {
			String message = "jobDetails Constructor: Error getting streams instance name from monitor, setting to UNKNOWN.";
			LOGGER.warn(message, sme);
			this.streamsInstanceName = "UNKNOWN";
		}
		setJobid(jobid);
		setJobBean(jobBean);
		setStatus(JobMXBean.Status.UNKNOWN);
		setJobMetrics(null);
		createExportedMetrics();

		MXBeanSource beanSource = null;
		MBeanServerConnection mbsc = null;
		try {
			beanSource = monitor.getContext().getBeanSourceProvider().getBeanSource();
			mbsc = beanSource.getMBeanServerConnection();
		} catch (IOException e) {
			String message = "jobDetails Constructor: Exception getting MBeanServerConnection from JMX Connection Pool";
			LOGGER.error(message, e);

			throw new IllegalStateException(e);
		}
		
		this.setName(jobBean.getName());
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
		this.setOutputPath(jobBean.getOutputPath());
		this.setStartedByUser(jobBean.getStartedByUser());
		this.setSubmitTime(jobBean.getSubmitTime());

		// Setup notifications (should handle exceptions)
		try {
			ObjectName jobObjName = ObjectNameBuilder.job(domain, instance, jobid);
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
	
	/* Stop/unregister anything you need to */
	public void close() {
		removeExportedMetrics();
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
		// Resolve portnames and store locally
		this.jobMetrics = resolvePortNames(jobMetrics);
		updateExportedMetrics();
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
		metricsExporter.getStreamsMetric("healthy", StreamsObjectType.JOB, this.streamsInstanceName, this.name).set((this.getHealth() == JobMXBean.Health.HEALTHY?1:0));

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
		//ji.setJobMetrics(resolvePortNames(jobMetrics));
		// Already resolved
		ji.setJobMetrics(jobMetrics);

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
				LOGGER.debug(
						"*    It was an " + t.getClass() + " which was unexpected, throw original undeclarable...");
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

	public String getSnapshot(int maximumDepth, boolean includeStaticAttributes) throws StreamsTrackerException {

		StringBuilder newSnapshot = new StringBuilder();

		// Create hashMap for timing Stuff
		LinkedHashMap<String, Long> timers = new LinkedHashMap<String, Long>();
		StopWatch stopwatch = new StopWatch();
		String uri = null;

		/**** JMX Interaction *****/
		try {
			stopwatch.start();
			/***
			 * ISSUE: snapshotJobMetrics does not declare it throws IOException
			 * but it does and comes back to us as UndeclaredThrowableException,
			 * handle that here
			 */
			try {
				uri = this.getJobBean().snapshot(maximumDepth, includeStaticAttributes);
			} catch (UndeclaredThrowableException e) {
				LOGGER.trace("* Handling snapshotJobMetrics UndeclaredThrowableException and unwrapping it");
				Throwable t = e.getUndeclaredThrowable();
				if (t instanceof IOException) {
					LOGGER.trace("*    It was an IOException we can handle, throwing the IOException");
					throw (IOException) t;
				} else {
					LOGGER.trace(
							"*    It was an " + t.getClass() + " which was unexpected, throw original undeclarable...");
					throw e;
				}
			}
			stopwatch.stop();
			timers.put("job.snapshot", stopwatch.getTime());

		} catch (IOException e) {
			// IOException from JMX usually means server restarted or domain
			LOGGER.warn("** job.snapshot JMX Interaction IOException **");
			LOGGER.info("details", e);

			throw new StreamsTrackerException(StreamsTrackerErrorCode.JMX_IOERROR,
					"Unable to retrieve snapshots at this time.", e);

		} catch (Exception e) {
			throw new StreamsTrackerException(StreamsTrackerErrorCode.UNSPECIFIED_ERROR,
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
			throw new StreamsTrackerException(StreamsTrackerErrorCode.UNSPECIFIED_ERROR,
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
	 * kind of notification and then went and pulled the new status Moving to a
	 * specific processing of the notification.
	 */
	public void handleNotification(Notification notification, Object handback) {
		try {
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
					monitor.resetTracker();
				}
	
				break;
			}
    	} catch (Exception e) {
    		LOGGER.error("Job ({}) Notification Handler caught exception: {}",this.name,e.toString());
    		e.printStackTrace();
    	}
	}

	private void mapPortNames(MXBeanSource beanSource) {
		Set<String> operators = getJobBean().getOperators();

		for (String operatorName : operators) {
			OperatorMXBean operatorBean = beanSource.getOperatorMXBean(getDomain(), getInstance(), getJobid(),
					operatorName);
			mapOperatorInputPortNames(beanSource, operatorName, operatorBean.getInputPorts());
			mapOperatorOutputPortNames(beanSource, operatorName, operatorBean.getOutputPorts());
		}
	}

	//@SuppressWarnings("unchecked")
	private void mapOperatorInputPortNames(MXBeanSource beanSource, String operatorName, Set<Integer> inputPorts) {
		for (Integer portIndex : inputPorts) {
			OperatorInputPortMXBean bean = beanSource.getOperatorInputPortMXBean(getDomain(), getInstance(), getJobid(),
					operatorName, portIndex);

			Map<Integer, String> inputPortNames = operatorInputPortNames.get(operatorName);

			if (inputPortNames == null) {
				inputPortNames = new HashMap<Integer, String>();
				operatorInputPortNames.put(operatorName, inputPortNames);
			}

			inputPortNames.put(portIndex, bean.getName());
		}
	}

	//@SuppressWarnings("unchecked")
	private void mapOperatorOutputPortNames(MXBeanSource beanSource, String operatorName, Set<Integer> outputPorts) {
		for (Integer portIndex : outputPorts) {
			OperatorOutputPortMXBean bean = beanSource.getOperatorOutputPortMXBean(getDomain(), getInstance(),
					getJobid(), operatorName, portIndex);

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

					// resolvePeInputPortNames(pe);
					// resolvePeOutputPortNames(pe);

					JSONArray operatorArray = (JSONArray) pe.get("operators");

					for (int j = 0; j < operatorArray.size(); j++) {
						JSONObject operator = (JSONObject) operatorArray.get(j);

						resolveOperatorInputPortNames(operator);
						resolveOperatorOutputPortNames(operator);
					}
				}

				metricsSnapshot = metricsObject.toJSONString();
			} catch (ParseException e) {
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
				outputPort.put("name", portName);
			}
		}
	}

	private String getOperatorName(JSONObject operator) {
		return operator.get("name").toString();
	}

	private int getOperatorPortIndex(JSONObject port) {
		return ((Number) port.get("indexWithinOperator")).intValue();
	}
	
	private void createExportedMetrics() {
		// Create our own metrics that will be aggregates of Streams metrics
	    // Operator, Operator InputPort, and Operator OUtputPort metrics
		// are automatically created based on metrics discovered in json
		// job health
		metricsExporter.createStreamsMetric("healthy", StreamsObjectType.JOB, "Job health, set to 1 of job is healthy else 0");
		// job metrics
		metricsExporter.createStreamsMetric("nCpuMilliseconds", StreamsObjectType.JOB, "Sum of each pe metric: nCpuMilliseconds");
		metricsExporter.createStreamsMetric("nResidentMemoryConsumption", StreamsObjectType.JOB, "Sum of each pe metric: nResidentMemoryConsumption");
		metricsExporter.createStreamsMetric("nMemoryConsumption", StreamsObjectType.JOB, "Sum of each pe metric: nMemoryConsumption");
		metricsExporter.createStreamsMetric("avg_congestionFactor", StreamsObjectType.JOB, "Average of all pe connection metric: congestionFactor");
		metricsExporter.createStreamsMetric("max_congestionFactor", StreamsObjectType.JOB, "Maximum of all pe connection metric: congestionFactor");
		metricsExporter.createStreamsMetric("min_congestionFactor", StreamsObjectType.JOB, "Minimum of all pe connection metric: congestionFactor");
		metricsExporter.createStreamsMetric("sum_congestionFactor", StreamsObjectType.JOB, "Sum of each pe metric: congestionFactor (no value used by itself");
		metricsExporter.createStreamsMetric("pecount", StreamsObjectType.JOB, "Number of pes deployed for this job");
	}

	private void removeExportedMetrics() {
		// When this job is removed, remove all metrics for this job
		// (really its the specific instance of the metric for the streams objects of this job)
		LOGGER.debug("removeExportedMetrics()");
		metricsExporter.removeAllChildStreamsMetrics(this.streamsInstanceName,name);
	}
	private void updateExportedMetrics() {
		/* Use this.jobMetrics to update the exported metrics */
		/* Some will be auto created, others we will control and aggregate */
		/* Specifically, we aggregate PE metrics to the job level */
		/* As the purpose of this is application metrics, PE level are not */
		/* understandable by operators, and future versions of streams */
		/* may create/remove pes automatically, and that would drive */
		/* a metric graphing tool crazy */
		
		/* Use SimpleJSON, it tests out pretty fast and easy to use */
		if (this.jobMetrics != null) {

			JSONParser parser = new JSONParser();
			try {
				JSONObject metricsObject = (JSONObject) parser.parse(this.jobMetrics);

				JSONArray peArray = (JSONArray) metricsObject.get("pes");
				
				/* Job Metrics */
				long ncpu = 0, nrmc = 0, nmc = 0;
				long numconnections = 0, totalcongestion = 0, curcongestion = 0;
				long maxcongestion = 0 , avgcongestion = 0, mincongestion = 999;
				/* PE Loop */
				for (int i = 0; i < peArray.size(); i++) {
					JSONObject pe = (JSONObject) peArray.get(i);


					JSONArray peMetricsArray = (JSONArray) pe.get("metrics");
					/* PE Metrics Loop */
					for (int j = 0; j < peMetricsArray.size(); j++) {
						JSONObject metric = (JSONObject) peMetricsArray.get(j);
						switch ((String) metric.get("name")) {
						case "nCpuMilliseconds":
							ncpu += (long)metric.get("value");
							break;
						case "nResidentMemoryConsumption":
							nrmc += (long)metric.get("value");
							break;
						case "nMemoryConsumption":
							nmc += (long)metric.get("value");
							break;
						default:
						}
					}
					
					/* PE outputPorts Loop */
					JSONArray outputPorts = (JSONArray) pe.get("outputPorts");
					for (int oportnum = 0; oportnum < outputPorts.size(); oportnum++) {
						JSONObject oport = (JSONObject)outputPorts.get(oportnum);
						JSONArray connections = (JSONArray) oport.get("connections");
						for (int con = 0; con < connections.size(); con++) {
							numconnections++;
							JSONObject connection = (JSONObject)connections.get(con);
							JSONArray metricsArray = (JSONArray) connection.get("metrics");
							for (int m = 0; m < metricsArray.size(); m++) {
								JSONObject metric = (JSONObject) metricsArray.get(m);
								switch ((String)metric.get("name")) {
								case "congestionFactor":
									curcongestion = (long)metric.get("value");
									totalcongestion += curcongestion;
									if (curcongestion > maxcongestion) maxcongestion = curcongestion;
									if (curcongestion < mincongestion) mincongestion = curcongestion;
								}
							}
						}
					}

					/* PE operator Loop */
					JSONArray operatorArray = (JSONArray)pe.get("operators");
					for (int op = 0; op < operatorArray.size(); op++) {
						JSONObject operator = (JSONObject) operatorArray.get(op);
						//System.out.println(operator.toString());
						String operatorName = (String)operator.get("name");
//						System.out.println("OPERATOR NAME: " + operatorName);
						JSONArray opMetricsArray = (JSONArray) operator.get("metrics");
						/* Operator Metrics Loop, these are non-standard metrics */
						for (int om = 0; om < opMetricsArray.size(); om++) {
							JSONObject metric = (JSONObject) opMetricsArray.get(om);
							String operatorMetricName = (String)metric.get("name");
//							System.out.println("OPERATOR METRIC: " + operatorMetricName);
							switch (operatorMetricName) {
							default:
//								System.out.println("About to set " + operatorMetricName +
//										" using " + this.streamsInstanceName +
//										", " + name +
//										", " + operatorName +
//										" to: " + metric.get("value"));
								metricsExporter.getStreamsMetric(operatorMetricName,
										StreamsObjectType.OPERATOR,
										this.streamsInstanceName,
										name,
										operatorName).set((long)metric.get("value"));
								break;
							}
						}	// End Operator Metrics Loop		
						
						// Loop over Operator Input Ports
						JSONArray opipArray = (JSONArray) operator.get("inputPorts");
						for (int opip = 0; opip < opipArray.size(); opip++) {
							JSONObject inputPort = (JSONObject)opipArray.get(opip);
							//System.out.println("INPUTPORT: " + inputPort.toString());
							String inputPortName = (String)inputPort.get("name");
							//System.out.println("INPUTPORTNAME: " + inputPortName);
							JSONArray ipMetrics = (JSONArray)inputPort.get("metrics");
							for (int opipm = 0; opipm < ipMetrics.size(); opipm++) {
								JSONObject metric = (JSONObject) ipMetrics.get(opipm);
								String metricName = (String)metric.get("name");
								switch (metricName) {
								default:
									metricsExporter.getStreamsMetric(metricName,
											StreamsObjectType.OPERATOR_INPUTPORT,
											this.streamsInstanceName,
											name,
											operatorName,
											inputPortName).set((long)metric.get("value"));
									break;
								}
							} // End Input Port Metrics Loop
						} // End Operator Input Port Loop

						// Loop over Operator Output Ports
						JSONArray opopArray = (JSONArray) operator.get("outputPorts");
						for (int opop = 0; opop < opopArray.size(); opop++) {
							JSONObject outputPort = (JSONObject)opopArray.get(opop);
							//System.out.println("OUTPUTPORT: " + outputPort.toString());
							String outputPortName = (String)outputPort.get("name");
							//System.out.println("OUTPUTPORTNAME: " + outputPortName);
							JSONArray opMetrics = (JSONArray)outputPort.get("metrics");
							for (int opopm = 0; opopm < opMetrics.size(); opopm++) {
								JSONObject metric = (JSONObject) opMetrics.get(opopm);
								String metricName = (String)metric.get("name");
								switch (metricName) {
								default:
									metricsExporter.getStreamsMetric(metricName,
											StreamsObjectType.OPERATOR_OUTPUTPORT,
											this.streamsInstanceName,
											name,
											operatorName,
											outputPortName).set((long)metric.get("value"));
									break;
								}
							} // End Output Port Metrics Loop
						} // End Operator Output Port Loop						
						
					} // End Operator Loop
				} // End PE Loop
				metricsExporter.getStreamsMetric("pecount", StreamsObjectType.JOB,this.streamsInstanceName, name).set(peArray.size());
				metricsExporter.getStreamsMetric("nCpuMilliseconds", StreamsObjectType.JOB, this.streamsInstanceName,name).set(ncpu);
				metricsExporter.getStreamsMetric("nResidentMemoryConsumption", StreamsObjectType.JOB, this.streamsInstanceName,name).set(nrmc);
				metricsExporter.getStreamsMetric("nMemoryConsumption", StreamsObjectType.JOB,this.streamsInstanceName,name).set(nmc);
				if (numconnections > 0)
					avgcongestion = totalcongestion / numconnections;
				// else it was initialized to 0;
				metricsExporter.getStreamsMetric("sum_congestionFactor", StreamsObjectType.JOB,this.streamsInstanceName, name).set(totalcongestion);
				metricsExporter.getStreamsMetric("avg_congestionFactor", StreamsObjectType.JOB,this.streamsInstanceName,name).set(avgcongestion);
				metricsExporter.getStreamsMetric("max_congestionFactor", StreamsObjectType.JOB,this.streamsInstanceName,name).set(maxcongestion);
				if (mincongestion == 999) mincongestion = 0;
				metricsExporter.getStreamsMetric("min_congestionFactor", StreamsObjectType.JOB, this.streamsInstanceName,name).set(mincongestion);
			} catch (ParseException e) {
				throw new IllegalStateException(e);
			}
		}
	}
}
