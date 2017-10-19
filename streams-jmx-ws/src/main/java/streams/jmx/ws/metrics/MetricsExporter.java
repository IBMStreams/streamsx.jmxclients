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

	static public Metric getStreamsInstanceMetric(String metricName, String instanceName) {
		String metricFullName = getStreamsInstanceMetricFullName(metricName);
		if (!gaugeMap.containsKey(metricFullName)) {
			// Create with default help text
			createStreamsInstanceMetric(metricName, "Streams instance metric: " + metricName);
//			gaugeMap.put(metricFullName, Gauge.build()
//							.name(metricFullName)
//							.help("Streams instance metric: " + metricName)
//							.labelNames("streamsInstance")
//							.register());
		}

		return new Metric(metricFullName, Arrays.asList(instanceName));		
	}	
	
	
	static public Metric getJobMetric(String metricName, String streamsInstanceName, String jobName) {
		String metricFullName = getJobMetricFullName(metricName);
		if (!gaugeMap.containsKey(metricFullName)) {
			// Create with default help text
			createJobMetric(metricName, "Streams job metric: " + metricName);
//			gaugeMap.put(metricFullName, Gauge.build()
//							.name(metricFullName)
//							.help("Streams job metric: " + metricName)
//							.labelNames("streamsInstance","jobname")
//							.register());
		}

		return new Metric(metricFullName, Arrays.asList(streamsInstanceName, jobName));		
	}
	
	/* Methods that set metrics should call clear when they are done */
	static public void removeJobMetric(String metricName, String streamsInstanceName, String jobName) {
		String metricFullName = getJobMetricFullName(metricName);
		if (gaugeMap.containsKey(metricFullName)) {
			Gauge g = gaugeMap.get(metricFullName);
			//g.remove((String[])Arrays.asList(streamsInstanceName, jobName).toArray());
			g.remove(new String[]{streamsInstanceName,jobName});

		}
	}
	
	static public void removeStreamsInstanceMetric(String metricName, String instanceName) {
		String metricFullName = getStreamsInstanceMetricFullName(metricName);
		if (gaugeMap.containsKey(metricFullName)) {
			Gauge g = gaugeMap.get(metricFullName);
			g.remove(new String[]{instanceName});
		}
	}
	
	static protected Gauge getGauge(String metricFullName) {
		Gauge g = null;
		if (gaugeMap.containsKey(metricFullName))
			g = gaugeMap.get(metricFullName);
		return g;
	}

//	final static class Metric {
//		protected final String name;
//		protected final List<String> labelNames;
//		
//		public Metric(String name, List<String> labelNames) {
//			this.name = name;
//			this.labelNames = labelNames;
//		}
//		
//		public void set(double val) {
//			Gauge g = gaugeMap.get(name);
//			g.labels((String[])labelNames.toArray()).set(val);
//		}
//	}
}
