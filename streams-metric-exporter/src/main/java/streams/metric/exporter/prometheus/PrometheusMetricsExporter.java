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

package streams.metric.exporter.prometheus;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.prometheus.client.Gauge;
import streams.metric.exporter.metrics.MetricsExporter;


public class PrometheusMetricsExporter extends MetricsExporter {
	static final Map<String, Gauge> gaugeMap = new HashMap<String, Gauge>();	
	
	public void createStreamsMetric(String metricName, StreamsObjectType type, String description) {
		String metricFullName = getStreamsMetricFullName(metricName, type);
		if (!gaugeMap.containsKey(metricFullName)) {
			gaugeMap.put(metricFullName,
					Gauge.build().name(metricFullName).help(description).labelNames(type.metricLabelNames()).register());
		}
	}

	public Metric getStreamsMetric(String metricName, StreamsObjectType type, String... labelValues) {
		String metricFullName = getStreamsMetricFullName(metricName, type);
		if (!gaugeMap.containsKey(metricFullName)) {
			// Create with default help text
			createStreamsMetric(metricName, type, type.metricDescriptionPrefix() + ": " + metricName);
		}
		PrometheusMetric pm = new PrometheusMetric(metricFullName, Arrays.asList(labelValues));
		super.addStreamsMetricToIndex(pm);	
		return pm;
	}
	
	public void removeAllChildStreamsMetrics(String... labelValues) {
		List<Metric> metricsToRemove;
		metricsToRemove = super.removeAllChildMetricsFromIndex(labelValues);
		Iterator<Metric> it = metricsToRemove.iterator();
		while (it.hasNext()) {
			Metric metric = it.next();
			if (gaugeMap.containsKey(metric.getName())) {
				Gauge g = gaugeMap.get(metric.getName());
				g.remove((String[])metric.getLabelValues().toArray());
			}
		}		
	}

	public void removeStreamsMetric(String metricName, StreamsObjectType type, String... labelValues) {
		String metricFullName = getStreamsMetricFullName(metricName, type);
		if (gaugeMap.containsKey(metricFullName)) {
			Gauge g = gaugeMap.get(metricFullName);
			g.remove(labelValues);
		}
	}

	static protected Gauge getGauge(String metricFullName) {
		Gauge g = null;
		if (gaugeMap.containsKey(metricFullName))
			g = gaugeMap.get(metricFullName);
		return g;
	}

	class PrometheusMetric extends MetricsExporter.Metric {
		protected PrometheusMetric(String name, List<String> labelValues) {
			super(name, labelValues);
		}

		public void set(double val) {
			Gauge g = PrometheusMetricsExporter.getGauge(name);
			if (g != null)
				g.labels((String[]) labelValues.toArray()).set(val);
		}
	}
	
//	 public static void main(String[] args) {
//		 	MetricsExporter metricsExporter = new PrometheusMetricsExporter();
//		 	
//	        System.out.println("Hello World!"); // Display the string.
//	        Metric m1 = metricsExporter.getStreamsMetric("nTuplesSubmitted", StreamsObjectType.JOB, "StreamsInstance","Job 1");
//	        Metric m2 = metricsExporter.getStreamsMetric("nTuplesSubmitted", StreamsObjectType.JOB, "StreamsInstance","Job 1","Operator 1");
//	        System.out.println(m1.labelChildOf("StreamsInstance","Job 1"));
//	        System.out.println(m1.labelChildOf("SomthingElse","Another"));
//	    }
}
