package streams.metric.exporter.metrics;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public abstract class MetricsExporter {
	
	// Metric Labels Index, allows us to remove child metrics by label
	private MetricLabelIndex metricIndex = new MetricLabelIndex();
	
	public abstract void createStreamsMetric(String metricName, StreamsObjectType type, String description);

	public abstract Metric getStreamsMetric(String metricName, StreamsObjectType type, String... labelValues);

	public abstract void removeStreamsMetric(String metricName, StreamsObjectType type, String... labelValues);

	public abstract void removeAllChildStreamsMetrics(String... labelValues);

	static protected String getStreamsMetricFullName(String metricName, StreamsObjectType type, String... labelValues) {
		return type.metricPrefix() + metricName;
	}
	
	protected void addStreamsMetricToIndex(Metric m) {
		this.metricIndex.add(m);
	}
	
	protected List<Metric> removeAllChildMetricsFromIndex(String... labelValues) {
		return this.metricIndex.removeWithChildren(labelValues);
	}
	
	public enum StreamsObjectType {
		INSTANCE("streams_instance_", new String[] { "instancename" }),
		JOB("streams_job_",new String[] { "instancename", "jobname" }),
		OPERATOR("streams_operator_",new String[] { "instancename", "jobname", "operatorname" }), 
		OPERATOR_INPUTPORT("streams_operator_ip_",new String[] { "instancename", "jobname", "operatorname", "inputportname" }), 
		OPERATOR_OUTPUTPORT("streams_operator_op_",new String[] { "instancename", "jobname", "operatorname","outputportname" });

		private final String metric_prefix;
		private final String[] labels;

		StreamsObjectType(String metric_prefix, String[] labels) {
			this.metric_prefix = metric_prefix;
			this.labels = labels;
		}

		String metricPrefix() {
			return metric_prefix;
		}

		String[] metricLabelNames() {
			return labels;
		}

		String metricDescriptionPrefix() {
			String description;
			switch (this) {
			case INSTANCE:
				description = "Streams instance metric";
				break;
			case JOB:
				description = "Streams job metric";
				break;
			case OPERATOR:
				description = "Streams operator metric";
				break;
			case OPERATOR_INPUTPORT:
				description = "Streams operator input port metric";
				break;
			case OPERATOR_OUTPUTPORT:
				description = "Streams operator output port metric";
			default:
				description = "Streams metric";
			}
			return description;
		}

	}



	public abstract class Metric {
		protected final String name;
		protected final List<String> labelValues;

		protected Metric(String name, List<String> labelValues) {
			this.name = name;
			this.labelValues = labelValues;
		}

		abstract public void set(double val);

		/* Determine if this metrics labels are a child of the set passed in */
		/* "I1","J1","Op1" is a labelChildOf("I1","J1") */
		public boolean labelChildOf(String... compareLabelValues) {
			List<String> compareList = Arrays.asList(compareLabelValues);
			if (compareList.size() > 0) {
				List<String> subList = this.labelValues.subList(0, compareList.size());
				return (subList.equals(compareList));
			}
			return true;
		}

	}
	
	private class MetricLabelIndex {
		private List<Metric> metrics;
		
		public MetricLabelIndex() {
			this.metrics = new LinkedList<Metric>();
		}
		
		public void add(Metric... newMetrics) {
			synchronized (this.metrics){
				for (Metric m : newMetrics) {
					this.metrics.add(m);
				}
			}
		}
		
		public List<Metric> removeWithChildren(String... labelValues) {			
			List<Metric> removedMetrics = new LinkedList<Metric>();
			
			synchronized (this.metrics){
				Iterator<Metric> it = this.metrics.iterator();
				while (it.hasNext()) {
					Metric metric = it.next();
					
					if (metric.labelChildOf(labelValues)) {
						removedMetrics.add(metric);
						it.remove();
					}
				}
			}
			
			return removedMetrics;
			
		}
	}

}
