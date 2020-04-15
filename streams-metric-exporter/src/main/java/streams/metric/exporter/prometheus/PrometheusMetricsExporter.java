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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.regex.Pattern;

import io.prometheus.client.Gauge;
import streams.metric.exporter.metrics.MetricsExporter;
//import streams.metric.exporter.streamstracker.StreamsDomainTracker;

// For local Main debugging
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;


public class PrometheusMetricsExporter extends MetricsExporter {
	//private static final Logger LOGGER = LoggerFactory.getLogger("root." + PrometheusMetricsExporter.class.getName());
	private static final Logger LOGGER = LoggerFactory.getLogger("root");
	// Singleton Pattern
	static MetricsExporter singletonExporter = null;

	protected PrometheusMetricsExporter(){}

	static public MetricsExporter getInstance() {
		if (singletonExporter == null) {
			singletonExporter = new PrometheusMetricsExporter();
		}
		return singletonExporter;
	}

	final Map<String, Gauge> gaugeMap = new HashMap<String, Gauge>();	

  // Prometheus has some rules for valid metric names
  // We will pre-sanitize metric names before using Promethus version
  // so that we can do the following:
  //  - Replace all series of 1 or more white-space with underscore
  //  - Remove all special characters except for underscore
  // Streams 4.3 had a metric name "nItemsQueued (Port 2)" that failed.
  private static final Pattern SPACES_PATTERN = Pattern.compile("\\s+");
  private static final Pattern NON_SPECIAL_OR_UNDERSCORE = Pattern.compile("(\\W|^_)*");

  private String sanitizeMetricName(String metricName) {
    return Gauge.sanitizeMetricName(
      NON_SPECIAL_OR_UNDERSCORE.matcher(
        SPACES_PATTERN.matcher(metricName).replaceAll("_")
      ).replaceAll(""));
  }
	
	public void createStreamsMetric(String metricName, StreamsObjectType type, String description) {
    String sanitizedName = sanitizeMetricName(metricName);
		String metricFullName = getStreamsMetricFullName(sanitizedName, type);
		if (!gaugeMap.containsKey(metricFullName)) {
			gaugeMap.put(metricFullName,
					Gauge.build().name(metricFullName).help(description).labelNames(type.metricLabelNames()).register());
		}
	}

	public Metric getStreamsMetric(String metricName, StreamsObjectType type, String... labelValues) {
    String sanitizedName = sanitizeMetricName(metricName);
		String metricFullName = getStreamsMetricFullName(sanitizedName, type);
		if (!gaugeMap.containsKey(metricFullName)) {
			// Create with default help text
			createStreamsMetric(metricName, type, type.metricDescriptionPrefix() + ": " + metricName);
		}
		PrometheusMetric pm = new PrometheusMetric(metricFullName, Arrays.asList(labelValues));
		super.addStreamsMetricToIndex(pm);	
		return pm;
	}
	
	public void removeAllChildStreamsMetrics(String... labelValues) {
		LOGGER.trace("PROMETHEUS metrics: removeAllChildStreamsMetrics({})",Arrays.asList(labelValues));
		Set<Metric> metricsToRemove;
		metricsToRemove = super.removeAllChildMetricsFromIndex(labelValues);
		LOGGER.trace("metricsToRemove.size: {}",metricsToRemove.size());
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
    String sanitizedName = sanitizeMetricName(metricName);
		String metricFullName = getStreamsMetricFullName(sanitizedName, type);
		if (gaugeMap.containsKey(metricFullName)) {
			Gauge g = gaugeMap.get(metricFullName);
			g.remove(labelValues);
		}
	}

	protected Gauge getGauge(String metricFullName) {
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
			Gauge g = getGauge(name);
			try {
				if (g != null)
					g.labels((String[]) labelValues.toArray()).set(val);
				else {
					LOGGER.debug("Tried to do a set on a gauge that did not exist name={}",name);
				}
			} catch (IllegalArgumentException e) {
				LOGGER.error("Attempting to set Prometheus Metric value returned IllegalArgumentException");
				LOGGER.error("Metric: name={}, labelValues={}",name,String.join(",",labelValues));
				LOGGER.error("This should not occur.  Usually caused by invalid labels for metric.  Get this fixed!!");
			}
		}
	}
	
	 public static void main(String[] args) {
			 MetricsExporter metricsExporter = PrometheusMetricsExporter.getInstance();
			 		org.apache.log4j.Logger logger = org.apache.log4j.Logger.getRootLogger();
					PatternLayout layout = new PatternLayout("%d{ISO8601} - %-5p [%t:%C{1}@%L] - %m%n");

					ConsoleAppender consoleAppender = new ConsoleAppender(layout);
					consoleAppender.setName("PMETEST");
					logger.addAppender(consoleAppender);
					logger.setLevel(org.apache.log4j.Level.toLevel("trace"));

	        System.out.println("Hello World!"); // Display the string.
	        Metric m1 = metricsExporter.getStreamsMetric("nTuplesSubmitted", StreamsObjectType.JOB, "StreamsInstance","Job 1");
	        Metric m2 = metricsExporter.getStreamsMetric("nTuplesSubmitted", StreamsObjectType.JOB, "StreamsInstance","Job 1","Operator 1");
	        Metric m3 = metricsExporter.getStreamsMetric("nTuplesSubmitted", StreamsObjectType.JOB, "StreamsInstance","Job 1","Operator 1");
	        Metric m4 = metricsExporter.getStreamsMetric("nTuplesSubmitted", StreamsObjectType.JOB, "");
					m4.set(1.0);

	        System.out.println("m1.equals(m2): " + m1.equals(m2));
	        System.out.println("m1.equals(m1): " + m1.equals(m1));
	        System.out.println("m2.equals(m3): " + m2.equals(m3));
	        
	        System.out.println("size: " + metricsExporter.getMetricIndex().size());
	        metricsExporter.removeAllChildStreamsMetrics("StreamsInstance","Job 1");
	        System.out.println("size after remove: " + metricsExporter.getMetricIndex().size());

	        
	        //System.out.println(m1.labelChildOf("StreamsInstance","Job 1"));
	        //System.out.println(m1.labelChildOf("SomthingElse","Another"));
	    }
}
