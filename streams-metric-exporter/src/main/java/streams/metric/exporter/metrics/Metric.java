package streams.metric.exporter.metrics;

import java.util.List;

import io.prometheus.client.Gauge;

public class Metric {
	private final String name;
	private final List<String> labelNames;
	
	protected Metric(String name, List<String> labelNames) {
		this.name = name;
		this.labelNames = labelNames;
	}
	
	public void set(double val) {
		Gauge g = MetricsExporter.getGauge(name);
		if (g != null)
			g.labels((String[])labelNames.toArray()).set(val);
	}
}
