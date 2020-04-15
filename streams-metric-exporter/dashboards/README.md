# Streams Metric Exporter Sample Dashboards for Grafana

These were created using Grafana version 6+ Docker Image

These Dashboards are meant to be imported via the Grafana GUI

The dashboards included are just samples.  The Streams Metric Exporter solution captures all of the metrics in Prometheus, thus allowing dashboards to be created that focus on different aspects of IBM Streams and different users.  Possibilities include:

* Domain summarization
* Instance summarization
* Specific application health
* Specific application data flow
* Resource utilization focus
* Domain Administrator view
* Application Developer view


## IBM Streams Domain Dashboard

![IBMStreamsDomainDashboardExample](../images/IBMStreamsDomainDashboard.png)

This dashboard provides a simple example of using IBM Streams prometheus metrics in a general case, meaning, the dashboard is not tailored to any specific analytic solution or IBM Streams job. 

The dashboard provides an overview of the IBM Streams Domain which has an instance of the steams-metric-exporter running.

There is a multi-select template at the top to allow a subset of the instances to be selected If you configure streams-metric-exporter to export multiple instances or have multiple streams-metric-exporters feeding the same prometheus database.

Metrics used include:
* streams_domain_status
* streams_domain_startTime
* streams_domain_creationTime
* streams_domain_instanceCount
* streams_instance_status
* streams_instance_jobCount
* streams_resource_status
* streams_resource_memoryTotal
* streams_resource_memoryFree
* streams_job_nCpuMilliseconds
* streams_job_nResidentMemoryConsumption
* streams_job_submitTime
* streams_job_status
* streams_job_health
* streams_job_max_congestionFactor
* streams_pe_health

## IBM Streams Instance Dashboard

![IBMStreamsInstanceDashboard](../images/IBMStreamsInstanceDashboard.png)

This dashboard focuses more on the details of an IBM Streams Instance and the currently running jobs.

The dashboard uses templates allowing the selection of a single IBM Streams instance and a multi-select for Job Name.

## IBM Streams Job Dashboard

![IBMStreamsJobDashboard](../images/IBMStreamsJobDashboard.png)

## IBM Streams Resource Dashboard

![IBMStreamsResourceDashboard](../images/IBMStreamsResourceDashboard.png)

