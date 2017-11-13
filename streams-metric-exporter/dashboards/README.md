# Streams Metric Exporter Sample Dashboards for Grafana

These were created using Grafana version 4.6.1 Docker Image

This version of Grafana had two different formats for loading dashboards:

1. `GUI Import`: This is the mechanism where you choose a file or copy/paste the json for the dashboard into the GUI dialog box

2. `API Create`: This mechanism is the REST API, and uses a more explicit version of the format, which includes the data source pointer.

## StreamsSampleDashboard

This dashboard provides a simple example of using streams prometheus metrics in a general case, meaning, the dashboard is not tailored to any specific analytic solution or IBM Streams job.

Metrics used include:
* streams_instance_status
* streams_instance_jobCount
* streams_resource_memoryTotal
* streams_job_healthy
* streams_job_nCpuMilliseconds
* streams_job_nMemoryResidentConsumption
* streams_job_max_congestionFactor
* streams_operator_ip_nTuplesDropped

| Filename | Description     |
| :------------- | :------------- |
| StreamsSampleDashboard_apiCreate.json       | Use the REST API to load this dashboard into Grafana.<br>Requires datasource named: Prometheus       |
| StreamsSampleDashboard_guiImport.json | Use GUI Dashboard Import dialogue box.<br>Allows you to select the datasource name in the import dialogue
