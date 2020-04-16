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

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.ibm.streams.management.job.JobMXBean;
import com.ibm.streams.management.job.PeMXBean;

import streams.metric.exporter.metrics.MetricsExporter;
import streams.metric.exporter.metrics.MetricsExporter.StreamsObjectType;
import streams.metric.exporter.prometheus.PrometheusMetricsExporter;
import streams.metric.exporter.streamstracker.StreamsInstanceTracker;

/* Job Details including map of port names so metrics can have names for ports rather than just ids */
public class JobDetails {
	private static final Logger LOGGER = LoggerFactory.getLogger("root." + StreamsInstanceTracker.class.getName());

	//private StreamsInstanceTracker monitor;
	//private ServiceConfig config = null;
	private String streamsInstanceName;
	private String instance = null;
	private String jobid = null;
	private String status = null;
	private String health = null;
	private String jobname = null;
	//private long submitTime = 0;

	private String jobSnapshot = null;
	private String jobMetrics = null;

	private final Map<String, Map<String, String>> peInfoMap = new HashMap<String, Map<String, String>>();
	private final Map<String, String> operatorKindMap = new HashMap<String, String>();
	// port maps <operatorname, map<indexWithinOperator,portname>>
	private final Map<String, Map<Long, String>> operatorInputPortNames = new HashMap<String, Map<Long, String>>();
	private final Map<String, Map<Long, String>> operatorOutputPortNames = new HashMap<String, Map<Long, String>>();

		/* Metrics Exporter*/
	/* Temporary solution: always use Prometheus exporter */
	/* Future: Make this pluggable, add Elasticsearch exporter */
	private MetricsExporter metricsExporter = PrometheusMetricsExporter.getInstance();

	
	public JobDetails(StreamsInstanceTracker monitor, String jobid, String jobname) {
		LOGGER.trace("jobDetails constructor: jobid {}, jobname {}",jobid,jobname);
		//this.monitor = monitor;
		//this.config = monitor.getConfig();

		this.streamsInstanceName = monitor.getInstanceInfo().getInstanceName();

		setJobid(jobid);
		setJobname(jobname);
		//setStatus(JobMXBean.Status.UNKNOWN);
		setJobMetrics(null);
		setJobSnapshot(null);

		createExportedMetrics();
	}
	
	// Called by Instance to pass in snapshot and metrics to update exported metrics
	public void refresh(String jobSnapshot, String jobMetrics) {
		LOGGER.trace("refresh job: {}", this.jobname);

		// Remove old metrics in case things moved around and new labels for things like resource are required
		this.removeExportedMetrics();
		this.createExportedMetrics();

		setJobSnapshot(jobSnapshot);
		setJobMetrics(jobMetrics);

		this.processSnapshot(jobSnapshot);
		this.processMetrics(jobMetrics);
	}

	// Create Mappings for Metric Lookup and Snapshot based metrics
	private void processSnapshot(String jobSnapshot) {
		LOGGER.trace("processSnapshot job: {}", this.jobname);

		// clear maps
		peInfoMap.clear();
		operatorKindMap.clear();
		operatorInputPortNames.clear();
		operatorOutputPortNames.clear();

		if (jobSnapshot != null) {

			JSONParser parser = new JSONParser();
			try {
				JSONObject snapshotObject = (JSONObject) parser.parse(jobSnapshot);

				String instance = (String)snapshotObject.get("instance");
				String status = (String)snapshotObject.get("status");
				String health = (String)snapshotObject.get("health");
				String jobname = (String)snapshotObject.get("name");
				Long submitTime = (Long)snapshotObject.get("submitTime");

				this.instance = instance;
				this.status = status;
				this.health = health;
				this.jobname = jobname;

				metricsExporter.getStreamsMetric("submitTime", StreamsObjectType.JOB, instance, jobname).set(submitTime);
				metricsExporter.getStreamsMetric("healthy", StreamsObjectType.JOB, instance, jobname).set(getHealthAsMetric(health));
				metricsExporter.getStreamsMetric("health", StreamsObjectType.JOB, instance, jobname).set(getHealthAsMetric(health));
				metricsExporter.getStreamsMetric("status", StreamsObjectType.JOB, instance, jobname).set(getStatusAsMetric(status));

				JSONArray peArray = (JSONArray) snapshotObject.get("pes");
				
				/* PE Loop */
				for (int i = 0; i < peArray.size(); i++) {
					JSONObject pe = (JSONObject) peArray.get(i);

					String peid = (String)pe.get("id");
					String resource = (String)pe.get("resource");
					String pestatus = (String)pe.get("status");
					String pehealth = (String)pe.get("health");
					long launchCount = (long)pe.get("launchCount");


					// Capture peInfo for metrics
					HashMap<String, String> peInfo = new HashMap<String, String>();
					peInfo.put("status",(String)pe.get("status"));
					peInfo.put("health",(String)pe.get("health"));
					peInfo.put("resource",(String)pe.get("resource"));
					peInfoMap.put(peid, peInfo);

					mapOperatorKindAndPortNames(pe);

					metricsExporter.getStreamsMetric("status",
							StreamsObjectType.PE,
							instance,
							jobname,
							resource,
							peid).set(getPEStatusAsMetric(pestatus));	

					metricsExporter.getStreamsMetric("health",
							StreamsObjectType.PE,
							instance,
							jobname,
							resource,
							peid).set(getPEHealthAsMetric(pehealth));	
							
					metricsExporter.getStreamsMetric("launchCount",
							StreamsObjectType.PE,
							instance,
							jobname,
							resource,
							peid).set(launchCount);	
				} // End pe loop
			} catch (ParseException e) {
				throw new IllegalStateException(e);
			}
		} // end if snapshot != null
	}

	/* Stop/unregister anything you need to */
	public void close() {
		removeExportedMetrics();
	}

	public String getJobMetrics() {
		return this.jobMetrics;
	}

	public void setJobMetrics(String jobMetrics) {
		this.jobMetrics = jobMetrics;
	}

	public String getJobid() {
		return this.jobid;
	}

	public void setJobid(String jobid) {
		this.jobid = jobid;
	}


	public String getJobSnapshot() {
		return jobSnapshot;
	}

	public void setJobSnapshot(String jobSnapshot) {
		this.jobSnapshot = jobSnapshot;
	}

	public String getInstance() {
		return instance;
	}

	public void setInstance(String instance) {
		this.instance = instance;
	}


	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getHealth() {
		return health;
	}

	public void setHealth(String health) {
		this.health = health;
	}

	public String getJobname() {
		return this.jobname;
	}

	public void setJobname(String jobname) {
		this.jobname = jobname;
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		// String newline = System.getProperty("line.separator");
		result.append("Job id: " + this.getJobid());
		result.append("Job name: " + this.getJobname());
		result.append(" Metrics: " + this.getJobMetrics());
		result.append(" Snapshot: " + this.getJobSnapshot());
		return result.toString();
	}

/*****************************************************************
 * Create Maps for easy reference when processing metrics
 * Some information is only found in the snapshot json
 *****************************************************************/

	private void mapOperatorKindAndPortNames(JSONObject peObject) {

		JSONArray operatorArray = (JSONArray) peObject.get("operators");
		
		/* Operator Loop */
		for (int i = 0; i < operatorArray.size(); i++) {
			JSONObject operator = (JSONObject) operatorArray.get(i);
			String operatorName = (String)operator.get("name");
			String operatorKind = (String)operator.get("operatorKind");
			operatorKindMap.put(operatorName,operatorKind);
			mapOperatorInputPortNames(operator, operatorName);
			mapOperatorOutputPortNames(operator, operatorName);
		}
	}

	// port maps <operatorname, map<indexWithinOperator,portname>>
	private void mapOperatorInputPortNames(JSONObject operator, String operatorName) {
		JSONArray inputPortsArray = (JSONArray) operator.get("inputPorts");
		Map<Long,String> inputPortNames = new HashMap<Long,String>();

		for (int i = 0; i < inputPortsArray.size(); i++) {
			JSONObject ip = (JSONObject) inputPortsArray.get(i);
			Long indexWithinOperator = (Long)ip.get("indexWithinOperator");
			String inputPortName = (String)ip.get("name");
			inputPortNames.put(indexWithinOperator,inputPortName);
		}
		operatorInputPortNames.put(operatorName,inputPortNames);
	}

	private void mapOperatorOutputPortNames(JSONObject operator, String operatorName) {
		JSONArray outputPortsArray = (JSONArray) operator.get("outputPorts");
		Map<Long,String> outputPortNames = new HashMap<Long,String>();

		for (int i = 0; i < outputPortsArray.size(); i++) {
			JSONObject ip = (JSONObject) outputPortsArray.get(i);
			Long indexWithinOperator = (Long)ip.get("indexWithinOperator");
			String outputPortName = (String)ip.get("name");
			outputPortNames.put(indexWithinOperator,outputPortName);
		}
		operatorOutputPortNames.put(operatorName,outputPortNames);
	}

	private void createExportedMetrics() {
		LOGGER.trace("createExportedMetrics job: {}", this.jobname);
		// Create our own metrics that will be aggregates of Streams metrics
	    // PE, PE InputPort, PE OutputPort, PE Output Port Connection,
		// Operator, Operator InputPort, and Operator OutputPort metrics
		// are automatically created based on metrics discovered in json
		
		// job snapshot based metrics
		metricsExporter.createStreamsMetric("submitTime", StreamsObjectType.JOB, "Epoch time in milliseconds when job was submitted");
		metricsExporter.createStreamsMetric("healthy", StreamsObjectType.JOB, "DEPRECTED: Use helath: Job health, set to 1 of job is healthy else 0");
		metricsExporter.createStreamsMetric("health", StreamsObjectType.JOB, "Job health, 1: healthy, .5: partially healthy, 0: unhealthy, unknown");
    	metricsExporter.createStreamsMetric("status", StreamsObjectType.INSTANCE, "Job status, 1: running, .5: cancelling, constructed, 0: canceled, failed, unknown");

		// job calculated metrics
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
		LOGGER.trace("removeExportedMetrics job: {}", this.jobname);
		metricsExporter.removeAllChildStreamsMetrics(this.streamsInstanceName,this.jobname);
	}


	private void processMetrics(String jobMetrics) {
		LOGGER.trace("processMetrics job: {}", this.jobname);
		
		if (jobMetrics != null) {
			JSONParser parser = new JSONParser();
			try {
				JSONObject metricsObject = (JSONObject) parser.parse(this.jobMetrics);
				JSONArray peArray = (JSONArray) metricsObject.get("pes");
				
				// Job Metrics 
				long ncpu = 0, nrmc = 0, nmc = 0;
				long numconnections = 0, totalcongestion = 0, curcongestion = 0;
				long maxcongestion = 0 , avgcongestion = 0, mincongestion = 999;

				// PE Loop 
				for (int i = 0; i < peArray.size(); i++) {
					JSONObject pe = (JSONObject) peArray.get(i);
					String peid = (String)pe.get("id");

					// Get info from peInfoMap
					Map<String,String> peInfo = peInfoMap.get(peid);

					String health = peInfo.get("health");
					String resource = peInfo.get("resource");

					// If the PE is not healthy, then its resource may not be correct while it is being
					// relocated, and we cannot create / update those metrics
					if (!health.equalsIgnoreCase("healthy")) {
						LOGGER.info("Job ({}: {}) Metrics, pe: " + peid + " is NOT healthy, NOT setting metrics",this.jobid,this.jobname);
						continue; // skip to next pe in loop
					}

					JSONArray peMetricsArray = (JSONArray) pe.get("metrics");
					/* PE Metrics Loop */
					for (int j = 0; j < peMetricsArray.size(); j++) {
						JSONObject metric = (JSONObject) peMetricsArray.get(j);
						String metricName = (String)metric.get("name");
						switch (metricName) {
						case "nCpuMilliseconds":
							ncpu += (long)metric.get("value");
							break;
						case "nResidentMemoryConsumption":
							nrmc += (long)metric.get("value");
							break;
						case "nMemoryConsumption":
							nmc += (long)metric.get("value");
							break;
						}
						metricsExporter.getStreamsMetric(metricName,
								StreamsObjectType.PE,
								this.streamsInstanceName,
								this.jobname,
								resource,
								peid).set((long)metric.get("value"));
					}
					
					/* PE inputPorts Loop */
					JSONArray inputPorts = (JSONArray) pe.get("inputPorts");
					for (int portnum = 0; portnum < inputPorts.size(); portnum++) {
						JSONObject port = (JSONObject)inputPorts.get(portnum);
						String indexWithinPE = Long.toString((long)port.get("indexWithinPE"));
						JSONArray metricsArray = (JSONArray) port.get("metrics");
						for (int m = 0; m < metricsArray.size(); m++) {
							JSONObject metric = (JSONObject) metricsArray.get(m);
							String metricName = (String)metric.get("name");
							metricsExporter.getStreamsMetric(metricName,
									StreamsObjectType.PE_INPUTPORT,
									this.streamsInstanceName,
									this.jobname,
									resource,
									peid,
									indexWithinPE).set((long)metric.get("value"));
						}	// End PE Input Ports Metrics Loop		
					} // End PE inputPorts loop			
									
					/* PE outputPorts Loop */
					JSONArray outputPorts = (JSONArray) pe.get("outputPorts");
					for (int portnum = 0; portnum < outputPorts.size(); portnum++) {
						JSONObject port = (JSONObject)outputPorts.get(portnum);
						
						String indexWithinPE = Long.toString((long)port.get("indexWithinPE"));
						JSONArray metricsArray = (JSONArray) port.get("metrics");
						for (int m = 0; m < metricsArray.size(); m++) {
							JSONObject metric = (JSONObject) metricsArray.get(m);
							String metricName = (String)metric.get("name");
							metricsExporter.getStreamsMetric(metricName,
									StreamsObjectType.PE_OUTPUTPORT,
									this.streamsInstanceName,
									this.jobname,
									resource,
									peid,
									indexWithinPE).set((long)metric.get("value"));
						}	// End PE Output Ports Metrics Loop		
						
						
						/* PE outputPorts Connections Loop */
						JSONArray connections = (JSONArray) port.get("connections");
						for (int con = 0; con < connections.size(); con++) {
							numconnections++;
							JSONObject connection = (JSONObject)connections.get(con);
							String connectionId = (String)connection.get("id");
							JSONArray cMetricsArray = (JSONArray) connection.get("metrics");
							for (int m = 0; m < cMetricsArray.size(); m++) {
								JSONObject metric = (JSONObject) cMetricsArray.get(m);
								String metricName = (String)metric.get("name");
								switch (metricName) {
								case "congestionFactor":
									curcongestion = (long)metric.get("value");
									totalcongestion += curcongestion;
									if (curcongestion > maxcongestion) maxcongestion = curcongestion;
									if (curcongestion < mincongestion) mincongestion = curcongestion;
								}
								metricsExporter.getStreamsMetric(metricName,
										StreamsObjectType.PE_OUTPUTPORT_CONNECTION,
										this.streamsInstanceName,
										this.jobname,
										resource,
										peid,
										indexWithinPE,
										connectionId).set((long)metric.get("value"));								
							}
						} // End PE outputPort Connectdions Loop
					} // End PE outputPort loop

					/* PE operator Loop */
					JSONArray operatorArray = (JSONArray)pe.get("operators");
					for (int op = 0; op < operatorArray.size(); op++) {
						JSONObject operator = (JSONObject) operatorArray.get(op);
						String operatorName = (String)operator.get("name");
						String operatorKind = this.operatorKindMap.get(operatorName);
						JSONArray opMetricsArray = (JSONArray) operator.get("metrics");

						/* Operator Metrics Loop, these are non-standard metrics */
						for (int om = 0; om < opMetricsArray.size(); om++) {
							JSONObject metric = (JSONObject) opMetricsArray.get(om);
							String operatorMetricName = (String)metric.get("name");
							switch (operatorMetricName) {
							default:
								metricsExporter.getStreamsMetric(operatorMetricName,
										StreamsObjectType.OPERATOR,
										this.streamsInstanceName,
										this.jobname,
										resource,
										peid,
										operatorName,
										operatorKind).set((long)metric.get("value"));
								break;
							}
						}	// End Operator Metrics Loop		
						
						// Loop over Operator Input Ports
						JSONArray opipArray = (JSONArray) operator.get("inputPorts");
						for (int opip = 0; opip < opipArray.size(); opip++) {
							JSONObject inputPort = (JSONObject)opipArray.get(opip);
							Long indexWithinOperator = (Long)inputPort.get("indexWithinOperator");
							String inputPortName = this.operatorInputPortNames.get(operatorName).get(indexWithinOperator);
							JSONArray ipMetrics = (JSONArray)inputPort.get("metrics");
							for (int opipm = 0; opipm < ipMetrics.size(); opipm++) {
								JSONObject metric = (JSONObject) ipMetrics.get(opipm);
								String metricName = (String)metric.get("name");
								switch (metricName) {
								default:
									metricsExporter.getStreamsMetric(metricName,
											StreamsObjectType.OPERATOR_INPUTPORT,
											this.streamsInstanceName,
											this.jobname,
											resource,
											peid,
											operatorName,
											operatorKind,
											inputPortName).set((long)metric.get("value"));
									break;
								}
							} // End Input Port Metrics Loop
						} // End Operator Input Port Loop

						// Loop over Operator Output Ports
						JSONArray opopArray = (JSONArray) operator.get("outputPorts");
						for (int opop = 0; opop < opopArray.size(); opop++) {
							JSONObject outputPort = (JSONObject)opopArray.get(opop);
							Long indexWithinOperator = (Long)outputPort.get("indexWithinOperator");
							String outputPortName = this.operatorOutputPortNames.get(operatorName).get(indexWithinOperator);
							JSONArray opMetrics = (JSONArray)outputPort.get("metrics");
							for (int opopm = 0; opopm < opMetrics.size(); opopm++) {
								JSONObject metric = (JSONObject) opMetrics.get(opopm);
								String metricName = (String)metric.get("name");
								switch (metricName) {
								default:
									metricsExporter.getStreamsMetric(metricName,
											StreamsObjectType.OPERATOR_OUTPUTPORT,
											this.streamsInstanceName,
											this.jobname,
											resource,
											peid,
											operatorName,
											operatorKind,
											outputPortName).set((long)metric.get("value"));
									break;
								}
							} // End Output Port Metrics Loop
						} // End Operator Output Port Loop						
						
					} // End Operator Loop
				} // End PE Loop
				metricsExporter.getStreamsMetric("pecount", StreamsObjectType.JOB,this.streamsInstanceName, this.jobname).set(peArray.size());
				metricsExporter.getStreamsMetric("nCpuMilliseconds", StreamsObjectType.JOB,this.streamsInstanceName,this.jobname).set(ncpu);
				metricsExporter.getStreamsMetric("nResidentMemoryConsumption", StreamsObjectType.JOB, this.streamsInstanceName,this.jobname).set(nrmc);
				metricsExporter.getStreamsMetric("nMemoryConsumption", StreamsObjectType.JOB,this.streamsInstanceName,this.jobname).set(nmc);
				if (numconnections > 0)
					avgcongestion = totalcongestion / numconnections;
				// else it was initialized to 0;
				metricsExporter.getStreamsMetric("sum_congestionFactor", StreamsObjectType.JOB,this.streamsInstanceName, this.jobname).set(totalcongestion);
				metricsExporter.getStreamsMetric("avg_congestionFactor", StreamsObjectType.JOB,this.streamsInstanceName,this.jobname).set(avgcongestion);
				metricsExporter.getStreamsMetric("max_congestionFactor", StreamsObjectType.JOB,this.streamsInstanceName,this.jobname).set(maxcongestion);
				if (mincongestion == 999) mincongestion = 0;
				metricsExporter.getStreamsMetric("min_congestionFactor", StreamsObjectType.JOB, this.streamsInstanceName,this.jobname).set(mincongestion);
			} catch (ParseException e) {
				throw new IllegalStateException(e);
			}
		} // end if metrics != null
	}
	
    private double getStatusAsMetric(String status) {
    	double value = 0;
    	switch (JobMXBean.Status.fromString(status)) {
    	case RUNNING :
    		value = 1;
    		break;
    	case CANCELING:
    	case CONSTRUCTED:
            value = 0.5;
            break;
    	default:
    		value = 0;
    	}
    	return value;
	}    
	
    private double getHealthAsMetric(String health) {
    	double value = 0;
    	switch (JobMXBean.Health.fromString(health)) {
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

    private double getPEStatusAsMetric(String status) {
    	double value = 0;
    	switch (PeMXBean.Status.fromString(status)) {
    	case RUNNING :
    		value = 1;
    		break;
    	case STARTING:
		case CONSTRUCTED:
		case RESTARTING:
		case STOPPING:
		case SUBMITTED:
            value = 0.5;
            break;
    	default:
    		value = 0;
    	}
    	return value;
	}    
	
    private double getPEHealthAsMetric(String health) {
    	double value = 0;
    	switch (PeMXBean.Health.fromString(health)) {
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


}
