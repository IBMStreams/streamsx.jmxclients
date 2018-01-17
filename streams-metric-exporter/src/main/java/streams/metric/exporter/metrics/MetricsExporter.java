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

package streams.metric.exporter.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import streams.metric.exporter.streamstracker.StreamsInstanceTracker;

public abstract class MetricsExporter {
	private static final Logger LOGGER = LoggerFactory.getLogger("root." + StreamsInstanceTracker.class.getName());
	
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
	
	public MetricLabelIndex getMetricIndex() {
		return metricIndex;
	}
	
	protected Set<Metric> removeAllChildMetricsFromIndex(String... labelValues) {
		return this.metricIndex.removeWithChildren(labelValues);
	}
	
	public enum StreamsObjectType {
		INSTANCE("streams_instance_", new String[] { "domainname","instancename" }),
		RESOURCE("streams_resource_", new String[] { "domainname","instancename", "resource"}),
		JOB("streams_job_",new String[] { "domainname","instancename", "jobname" }),
		PE("streams_pe_",new String[] { "domainname", "instancename", "jobname", "peid"}),
		PE_INPUTPORT("streams_pe_ip_",new String[] {"domainname","instancename", "jobname","peid","index"}),
		PE_OUTPUTPORT("streams_pe_op_",new String[] {"domainname","instancename","jobname","peid","index"}),
		PE_OUTPUTPORT_CONNECTION("streams_pe_op_connection_",new String[] {"domainname","instancename","jobname","peid","index","connectionid"}),
		OPERATOR("streams_operator_",new String[] { "domainname","instancename", "jobname", "peid", "operatorname" }), 
		OPERATOR_INPUTPORT("streams_operator_ip_",new String[] { "domainname","instancename", "jobname", "peid", "operatorname", "inputportname" }), 
		OPERATOR_OUTPUTPORT("streams_operator_op_",new String[] { "domainname","instancename", "jobname", "peid", "operatorname","outputportname" });

		private final String metric_prefix;
		private final String[] labels;

		StreamsObjectType(String metric_prefix, String[] labels) {
			this.metric_prefix = metric_prefix;
			this.labels = labels;
		}

		String metricPrefix() {
			return metric_prefix;
		}

		public String[] metricLabelNames() {
			return labels;
		}

		public String metricDescriptionPrefix() {
			String description;
			switch (this) {
			case INSTANCE:
				description = "Streams instance metric";
				break;
			case RESOURCE:
				description = "Streams resource metric";
			case JOB:
				description = "Streams job metric";
				break;
			case PE:
				description = "Streams pe metric";
				break;
			case PE_INPUTPORT:
				description = "Streams pe input port metric";
				break;
			case PE_OUTPUTPORT:
				description = "Streams pe output port metric";
				break;
			case PE_OUTPUTPORT_CONNECTION:
				description = "Streams pe output port connection metric";
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
		
		public String getName() {
			return name;
		}

		public List<String> getLabelValues() {
			return labelValues;
		}

		abstract public void set(double val);

		/* Determine if this metrics labels are a child of the set passed in */
		/* "I1","J1","Op1" is a labelChildOf("I1","J1") */
		public boolean labelChildOf(String... compareLabelValues) {
			List<String> compareList = Arrays.asList(compareLabelValues);
			if (compareList.size() > labelValues.size()) {
				return false;
			}
			if (compareList.size() > 0)  {
				LOGGER.trace("compareList size: {}: {}",compareList.size(),Arrays.toString(compareList.toArray()));
				LOGGER.trace("this.labelValues size: {}: {}",labelValues.size(),Arrays.toString(labelValues.toArray()));
				List<String> subList = this.labelValues.subList(0, compareList.size());
				return (subList.equals(compareList));
			}
			return true;
		}

		// Hashcode builder for use in HashSet
		@Override
		public int hashCode() {
			return new HashCodeBuilder().append(this.name).append(this.labelValues).toHashCode();
		}

		// Equals method for use in HashSet
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Metric == false) {
				return false;
			}
			if (this == obj) {
				return true;
			}
			final Metric otherObject = (Metric)obj;

			return new EqualsBuilder().append(this.name,otherObject.name)
				.append(this.labelValues, otherObject.labelValues)
				.isEquals();
		}

	}
	
	public class MetricLabelIndex {
		private Set<Metric> metrics;
		
		public MetricLabelIndex() {
			this.metrics = new HashSet<Metric>();
		}
		
		public void add(Metric... newMetrics) {
			synchronized (this.metrics){
				for (Metric m : newMetrics) {
					this.metrics.add(m);
				}
			}
		}
		
		public int size() {
			return metrics.size();
		}
		
		public Set<Metric> removeWithChildren(String... labelValues) {	
			Set<Metric> removedMetrics = new HashSet<Metric>();
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
