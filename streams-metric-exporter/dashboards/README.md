# Streams Metric Exporter Sample Dashboards for Grafana

These were created using Grafana version 4.6.2 Docker Image

These dashboard files can also be pointed to by a Grafana 5 dashboard provisioning file.

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

The dashboard provides an overview of the IBM Streams Domain you point it at.  It will show the number of instances in the domain and their status. 

There is a multi-select template at the top to allow a subset of the instances to be selected.

Metrics used include:
* streams_domain_status
* streams_domain_instanceCount
* streams_instance_status
* streams_instance_jobCount
* streams_resource_memoryTotal
* streams_job_healthy
* streams_job_nCpuMilliseconds
* streams_job_nMemoryResidentConsumption
* streams_job_max_congestionFactor
* streams_operator_ip_nTuplesDropped

## IBM Streams Instance Dashboard

![IBMStreamsInstanceDashboardExample](../images/IBMStreamsInstanceDashboard.png)

This dashboard focuses more on the details of IBM Streams jobs, including Processing Element and Operator metrics.  In addition, PE Launch Count is included.  This is a good candidate for alerting when/if PE's are restarting.

The dashboard uses templates allowing the selection of a single IBM Streams instance and a multi-select for Job Name.

Metrics used include:
* streams_instance_status
* streams_instance_jobCount
* streams_job_pecount
* streams_pe_nCpuMilliseconds
* streams_pe_op_connection_congestionFactor
* streams_pe_nResidentMemoryConsumption
* streams_pe_ip_nTuplesProcessed
* streams_pe_ip_nTupleBytesProcessed
* streams_pe_op_nTuplesSubmitted
* streams_pe_op_nTupleBytesSubmitted
* streams_operator_ip_nTuplesProcessed
* streams_pe_launchCount


| Filename | Description     |
| :------------- | :------------- |
| IBMStreamsDomainDashboard.json | Use GUI Dashboard Import dialogue box.<br>Allows you to select the datasource name in the import dialogue
| IBMStreamsInstanceDashboard.json | Use GUI Dashboard Import dialogue box.<br>Allows you to select the datasource name in the import dialogue
