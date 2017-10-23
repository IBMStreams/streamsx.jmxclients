package streams.jmx.ws.metrics;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import io.prometheus.client.Gauge;

/* Approach to dynamic set of Prometheus metrics */
/* You interact with this using the labels as you would a prometheus metric */
public class MetricsExporter {
	static final Map<String, Gauge> gaugeMap = new HashMap<String, Gauge>();
	
	static private String getStreamsInstanceMetricFullName(String metricName) {
		return "streams_instance_" + metricName;
	}
	
	static private String getJobMetricFullName(String metricName) {
		return "streams_job_" + metricName;
	}
	
	static private String getOperatorMetricFullName(String metricName) {
		return "streams_operator_" + metricName;
	}
	
	static private String getOperatorInputPortMetricFullName(String metricName) {
		return "streams_operator_ip_" + metricName;
	}
	
	static public void createStreamsInstanceMetric(String metricName, String description) {
		String metricFullName = getStreamsInstanceMetricFullName(metricName);
		if (!gaugeMap.containsKey(metricFullName)) {
			gaugeMap.put(metricFullName, Gauge.build()
							.name(metricFullName)
							.help(description)
							.labelNames("instancename")
							.register());
		}
	}
	
	static public void createJobMetric(String metricName, String description) {
		String metricFullName = getJobMetricFullName(metricName);
		if (!gaugeMap.containsKey(metricFullName)) {
			gaugeMap.put(metricFullName, Gauge.build()
							.name(metricFullName)
							.help(description)
							.labelNames("instancename","jobname")
							.register());
		}
	}
	
	static public void createOperatorMetric(String metricName, String description) {
		String metricFullName = getOperatorMetricFullName(metricName);
		if (!gaugeMap.containsKey(metricFullName)) {
			gaugeMap.put(metricFullName, Gauge.build()
							.name(metricFullName)
							.help(description)
							.labelNames("instancename","jobname","operatorname")
							.register());
		}
	}
	
	static public void createOperatorInputPortMetric(String metricName, String description) {
		String metricFullName = getOperatorInputPortMetricFullName(metricName);
		if (!gaugeMap.containsKey(metricFullName)) {
			gaugeMap.put(metricFullName, Gauge.build()
							.name(metricFullName)
							.help(description)
							.labelNames("instancename","jobname","operatorname","inputportname")
							.register());
		}
	}
	
	
	
	

	static public Metric getStreamsInstanceMetric(String metricName, String instanceName) {
		String metricFullName = getStreamsInstanceMetricFullName(metricName);
		if (!gaugeMap.containsKey(metricFullName)) {
			// Create with default help text
			createStreamsInstanceMetric(metricName, "Streams instance metric: " + metricName);
		}
		return new Metric(metricFullName, Arrays.asList(instanceName));		
	}	
	
	
	static public Metric getJobMetric(String metricName, String streamsInstanceName, String jobName) {
		String metricFullName = getJobMetricFullName(metricName);
		if (!gaugeMap.containsKey(metricFullName)) {
			// Create with default help text
			createJobMetric(metricName, "Streams job metric: " + metricName);
		}
		return new Metric(metricFullName, Arrays.asList(streamsInstanceName, jobName));		
	}
	
	static public Metric getOperatorMetric(String metricName, String streamsInstanceName, String jobName, String operatorname) {
		String metricFullName = getOperatorMetricFullName(metricName);
		if (!gaugeMap.containsKey(metricFullName)) {
			// Create with default help text
			createOperatorMetric(metricName, "Streams operator metric: " + metricName);
		}
		return new Metric(metricFullName, Arrays.asList(streamsInstanceName, jobName, operatorname));		
	}
	
	static public Metric getOperatorInputPortMetric(String metricName, String streamsInstanceName, String jobName, String operatorname, String inputportname) {
		String metricFullName = getOperatorInputPortMetricFullName(metricName);
		if (!gaugeMap.containsKey(metricFullName)) {
			// Create with default help text
			createOperatorInputPortMetric(metricName, "Streams operator input port metric: " + metricName);
		}
		return new Metric(metricFullName, Arrays.asList(streamsInstanceName, jobName, operatorname, inputportname));		
	}
	
	
	
	
	
	
	/* Methods that set metrics should call clear when they are done */
	static public void removeStreamsInstanceMetric(String metricName, String instanceName) {
		String metricFullName = getStreamsInstanceMetricFullName(metricName);
		if (gaugeMap.containsKey(metricFullName)) {
			Gauge g = gaugeMap.get(metricFullName);
			g.remove(new String[]{instanceName});
		}
	}
	
	static public void removeJobMetric(String metricName, String streamsInstanceName, String jobName) {
		String metricFullName = getJobMetricFullName(metricName);
		if (gaugeMap.containsKey(metricFullName)) {
			Gauge g = gaugeMap.get(metricFullName);
			g.remove(new String[]{streamsInstanceName,jobName});

		}
	}
	
	static public void removeOperatorMetric(String metricName, String streamsInstanceName, String jobName, String operatorname) {
		String metricFullName = getOperatorMetricFullName(metricName);
		if (gaugeMap.containsKey(metricFullName)) {
			Gauge g = gaugeMap.get(metricFullName);
			g.remove(new String[]{streamsInstanceName,jobName,operatorname});

		}
	}	
	
	static public void removeOperatorInputPortMetric(String metricName, String streamsInstanceName, String jobName, String operatorname, String inputportname) {
		String metricFullName = getOperatorInputPortMetricFullName(metricName);
		if (gaugeMap.containsKey(metricFullName)) {
			Gauge g = gaugeMap.get(metricFullName);
			g.remove(new String[]{streamsInstanceName,jobName,operatorname,inputportname});

		}
	}	

	
	static protected Gauge getGauge(String metricFullName) {
		Gauge g = null;
		if (gaugeMap.containsKey(metricFullName))
			g = gaugeMap.get(metricFullName);
		return g;
	}

}
