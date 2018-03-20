# streams-metric-exporter

This application provides an interface to the IBM Streams - Stream Processing System for the purposes of retrieving status and metrics of an IBM Streams domain, instances, jobs, and resources.

This version provides 2 HTTP/HTTPS interfaces:
* Prometheus: HTTP/Text endpoint in Prometheus metrics format.
* REST: Multiple endpoints returning json.

IBM Streams provides a JMX Service (with HTTP GET interface for batch metric pulls) that is capable of providing status of the Streams instance, deployed streaming application (jobs), and cluster resources.  In addition, metrics are available via the Streams JMX Server.

The primary use-case for this application is as a Prometheus metrics exporter to provide time series displays using Grafana.
It is meant to be used as a Streams Application Metrics exporter.  It is not meant to monitor the internal system services of IBM Streams.
This application improves performance over per-job metric scraping by periodically (optionally on-demand) pulling all job metrics (via the JMX Server HTTP callbacks) and caching them.  Users can use the REST endpoints (including Prometheus endpoint) to get metrics and status of specific jobs.

The service can be configured with periodic refresh (refresh rate > 0) or on-demand refresh (refresh rate == 0) when the HTTP/HTTPS endpoints are accessed.

The REST service supports HTTP and HTTPS with One-way SSL Authentication.

## Questions
For questions and issues, please contact:

Brian M Williams, IBM<br>
bmwilli@us.ibm.com

# Contents
1. [Building streams-metric-exporter](#building-the-application)
2. [Command line options](#command-line-options)
3. [Running streams-metric-exporter](#running-the-application)
4. [Prometheus Integration](#prometheus-integration)
5. [Grafana Dashboard Example](#grafana-examples)
6. [Running with Docker](#running-with-docker)
7. [Cached REST endpoints](#cached-rest-endpoints)
8. [Passthrough REST endpoints](#passthrough-rest-endpoints)

# Building the application

## Dependencies
The build location must be a linux node with IBM Streams installed.

The environment variable (STREAMS_INSTALL) must be set.

The pom.xml file references the IBM Streams JMX API classes in the product directory.

Another option is to copy the necessary files to the local machine and modify the pom.xml files

## Compiling the application
```
mvn compile
```

## Create executable .jar with dependencies included
```
mvn package
```

Location will be: target/executable-streams-metric-exporter.jar

# Command line options
`java -jar target/executable-streams-metric-exporter.jar --help`

<pre>
Usage: streams-metric-exporter [options]
  Options:
    -d, --domain
      Streams domain name
      Environment Variable: STREAMS_DOMAIN_ID
    --help
      Display command line arguments
    -h, --host
      Listen Host or IP address for this service (e.g. localhost)
      Environment Variable: STREAMS_EXPORTER_HOST
      Default: localhost
    -i, --instance
      Streams instance name.  Only used if Instance List not provided.
      Environment Variable: STREAMS_INSTANCE_ID
    --instancelist
      Comma separated list of 1 or more Streams Instances to monitor ('ALL' for all instances). Default ALL if STREAMS_INSTANCE_ID 
      is not set.
      Envrionment Variable: STREAMS_EXPORTER_INSTANCE_LIST
      Default: [UNSPECIFIED]
    --jmxssloption
      SSL Option for connection to Streams JMX Server (e.g. SSL_TLSv2, TSLv1.1, TLSv1.2)
      Environment Variable: 
      STREAMS_EXPORTER_JMX_SSLOPTION 
      Default: TLSv1
    --jmxtruststore
      Java keystore of certificates/signers to trust from JMX Server
      Environment Variable: STREAMS_EXPORTER_JMX_TRUSTSTORE
    -j, --jmxurl
      JMX Connection URL (e.g. service:jmx:jmxmp://localhost:9975)
      Environment Variable: STREAMS_EXPORTER_JMXCONNECT
    --noconsole
      Flag to indicate not to prompt for password (can still redirect from stdin or use environment variable for password.
      Default: false
    --password
      Streams login password. Recommend using environment variable
      Environment Variable: STREAMS_EXPORTER_PASSWORD
    -p, --port
      Listen Port for this service
      Environment Variable: STREAMS_EXPORTER_PORT
      Default: 25500
    -r, --refresh
      Refresh rate of metrics in seconds or 0 for no automatic refresh
      Environment Variable: STREAMS_EXPORTER_REFRESHRATE
      Default: 10
    --serverkeystore
      Java keystore containing server certificate and key to identify server side of this application
      Environment Variable: 
      STREAMS_EXPORTER_SERVER_KEYSTORE 
    --serverkeystorepwd
      Passphrase to java keystore.  Passphrase of keystore and key (if it has one) must match
      Environment Variable: 
      STREAMS_EXPORTER_SERVER_KEYSTORE_PWD 
    --serverprotocol
      http or https.  https will use one-way ssl authentication and java default for tls level (TLSv1.2)
      Environment 
      Variable: STREAMS_EXPORTER_SERVER_PROTOCOL
      Default: http
    -u, --user
      Streams login username. Use this or X509CERT
      Environment Variable: STREAMS_EXPORTER_USERNAME
    --webPath, 
      Base URI prefix (e.g. /someprefix)
      Environment Variable: STREAMS_EXPORTER_WEBPATH
      Default: /
    -x, --x509cert
      X509 Certificate file to use instead of username/password
      Environment Variable: STREAMS_X509CERT
</pre>

# Running the application
```
java -jar target/executable-streams-metric-exporter.jar -j \
service:jmx:jmxmp://localhost:9975 -d StreamsDomain  \
--instancelist ALL -u streamsadmin

password: <enter streamsadmin password>
```
The ``--instanelist`` option allows the specification of 0 or more instances.  If none or chosen or it is set to the value ``ALL`` then all instances metrics will be exported.  In addition, instances are created and removed, the cooresponding metrics will be added and removed.  If, however, you specify a specific list of instances, then any addtional instances that exist in the domain or are created after the application is started WILL NOT be included in the exported set.

## Changing the default logging
The default logging configuration (log4j.properties) is stored in the executable jar.  To override the defaults, create a log4j.properties file and point to it using the log4j.configuration java property.  For example:
```
java -Dlog4j.configuration=file:${PWD}/log4j.properties -jar target/executable-streams-metric-exporter.jar -j \
service:jmx:jmxmp://localhost:9975 -d StreamsDomain -i \
StreamsInstance -u streamsadmin
```
# Prometheus Integration

## Endpoint

```bash
/prometheus
```
## prometheus.yml
This file configures how prometheus will scrape the streams-metric-exporter.  By default, the metrics are cached and retrieved every 10 seconds.  If you desire for the exporter to work as a "proper" prometheus exporter, set the refresh rate (--refresh) to 0, which causes refreshes to be manual and performed whenever an end point is accessed (the /prometheus endpoint for example).

```yml
  - job_name: "ibmstreams"
    scrape_interval: "15s"
    metrics_path: "/prometheus"
    static_configs:
    - targets: ['localhost:25500']
```
## Metric Names
The exact set of metric names exposed is a mix of static and dynamically named metrics.
The metric names chosen for this endpoint are a hybrid of prometheus naming conventions and the pre-defined metrics of IBMStreams.
Metric prefixes are static and based on which Streams objects the metrics are produced for.

The pattern for metric names is<br>

```streams_<objecttype>[_<subobjecttype>]_[_<aggregationtype>]_<streams metric>```

Examples
```
streams_operator_ip_nTuplesProcessed
streams_job_max_congestionFactor
```

| Metric Name Prefix | Description |
| :----------------- | :---------- |
| **streams_domain_**|domain level metrics and status|
| **streams_instance_**|instance level metrics and status|
| **streams_resource_**|streams resource metrics|
| **streams_job_**|streams job metrics|
| **streams_pe_**|streams pe metrics|
| **streams_pe_ip_**|streams pe input port metrics|
| **streams_pe_op_**|streams pe output port metrics|
| **streams_pe_op_connection_**|streams pe output port connection metrics|
| **streams_operator_**|streams operator metrics|
| **streams_operator_ip_**|streams operator input port metrics|
| **streams_operator_op_**|streams operatore output port metrics|

## Metric Labels
The prometheus metric names are not specific to streams objects (e.g. a specific job), rather, they are for an object type (e.g. operator input port).  The labels are used to identify the individual instances (e.g. job: job_1, operator: myBeacon, input port: StockTickersIn).
Note: the streams-metric-exporter resolves operator input and output ports to names rather than indexes.  This is easier for use in queries.

| Label Name | Description     |
| :------------- | :------------- |
|**domainname**|name of the streams domain
|**instancename**|name of streams instance (instance is reserved by prometheus)|
|**jobname**| name of streams job (job is reserved by prometheus)|
|**operatorname**| name of operator|
|**inputportname**| name of input port|
|**outputportname**| name of output port|
|**resource**| name of streams resource|
|**peid**|id of streams processing element|
|**index**|index of pe input or output port|
|**connectionid**|connection id of streams pe output port connection|

## Prometheus endpoint example metrics

```
# HELP streams_domain_instanceCount Number of instances currently created in the streams domain
# TYPE streams_domain_instanceCount gauge
streams_domain_instanceCount{domainname="StreamsDomain",} 2.0

# HELP streams_instance_jobCount Number of jobs currently deployed into the streams instance
# TYPE streams_instance_jobCount gauge
streams_instance_jobCount{domainname="StreamsDomain",instancename="StreamsInstance",} 1.0

# HELP streams_job_pecount Number of pes deployed for this job
# TYPE streams_job_pecount gauge
streams_job_pecount{domainname="StreamsDomain",instancename="StreamsInstance",jobname="MultiPEJob",} 2.0

# HELP streams_operator_ip_nTuplesProcessed Streams operator input port metric: nTuplesProcessed
# TYPE streams_operator_ip_nTuplesProcessed gauge
streams_operator_ip_nTuplesProcessed{domainname="StreamsDomain",instancename="StreamsInstance",jobname="MultiPEJob",peid="1",operatorname="FilterStream",inputportname="BeaconStream",} 675632.0

# HELP streams_pe_op_connection_congestionFactor Streams pe output port connection metric: congestionFactor
# TYPE streams_pe_op_connection_congestionFactor gauge
streams_pe_op_connection_congestionFactor{domainname="StreamsDomain",instancename="StreamsInstance",jobname="MultiPEJob",peid="1",index="0",connectionid="o0p1i0p0",} 0.0
```

# Grafana Examples
See [dashboards directory](dashboards/README.md) for more information.

![Grafana Example](images/IBMStreamsDomainDashboard.png)

# Running with Docker

The easiest way to try out the Streams Metric Exporter is to run it using Docker.  Included in this release is a Dockerfile for building the image and a docker-compose.yml file for starting it up with Prometheus and Grafana instances.

The versions of Prometheus and Grafana specified in the docker-compose.yml file are those that were used for testing.

## Prerequisites

* Compiled version of Streams Metric Exporter (executable-streams-metric-exporter.jar)
* JMX Access to a running IBM Streams 4.2.1 Domain (JMX Port 9975 is the default)
* Docker Engine (version 17.05.0.ce used in development)
* Docker Compose (version 1.9.0-5 with .yml file format 2 used in development)
* Access to Dockerhub or local repository with Images:
  * ibmjava:sfj-alpine (or any 1.8 version)
  * prom/prometheus (2.0.0 used in development)
  * grafana/grafana (4.6.1 used in development)

## Setup environment

1. Change to the docker directory in this project<br>
```bash
cd docker
```
2. Create .env file from sample.env
```
cp sample.env .env
```
3. Edit .env to set authentication for streams jmx Server
```
STREAMS_EXPORTER_USERNAME=<username with domain/instance read access>
STREAMS_EXPORTER_PASSWORD=<user password for pam authentication>
```
4. Export additional environment variables via .env file or from the command line
```
export STREAMS_EXPORTER_JMXCONNECT=service:jmx:jmxmp://<jmxhost>:<jmxport>
export STREAMS_DOMAIN_ID=<domain id>
```
5. Build / Run Docker Images
```
docker-compose build
docker-compose up
```
6. Create Prometheus data source in grafana
```bash
../scripts/create_datasource.sh
```
7. Import sample dashboard into Grafana
```
../scripts/import_dashboard.sh ../dashboards/StreamsSampleDashboard_apiCreate.json
```
8. Open Grafana in browswer
```
http://localhost:3000
```
9. Login with default username/password
```
admin/admin
```
10. Navigate to Home Dashboard
```
IBM Streams Sample Dashboard
```
11. View raw metrics output from /prometheus endpoint
```
http://localhost:25500
```
12. Query the prometheus ui
```
http://localhost:9090
```
13. Attach to the streamsexporter instance and tail the more detailed logfile
```
docker exec -it docker_streamsexporter_1 /bin/ash
# tail -f StreamsMetricExporter.log
```

# Cached REST endpoints
The original version of streams-metric-exporter provided REST endpoints that returned json information.  The REST endpoints are still available in this version.  The reason to use these REST endpoints over those available directly from IBM Streams is that by default the auto-refresh is used, and these endpoints only retrieve from the metrics cached in the streams-metric-exporter.  Beware, however, if you turn auto-refresh off (--refresh 0) then each of these access points will cause the metrics to be pulled from the Streams JMX Server.

## /domain
Retrieve information about the domain being monitored

`curl localhost:25500/domain`

```json
{
    "creationTime": "12/11/2017 21:55:55",
    "creationUser": "streamsadmin",
    "externalResourceManager": null,
    "fullProductVersion": "4.2.1.3",
    "highAvailabilityCount": 1,
    "instances": [
        "StreamsInstance",
        "test1"
    ],
    "name": "StreamsDomain",
    "resources": [
        "streamshost.localdomain"
    ],
    "status": "running"
}
```
## /instances
Retrieve status of all instances being monitored
`curl localhost:25500/instances`

```json
   "instances": [
        {
            "instanceName": "StreamsInstance",
            "instanceStartTime": 1520813342320,
            "instanceStatus": "running"
        },
        {
            "instanceName": "test1",
            "instanceStartTime": 1520813432867,
            "instanceStatus": "running"
        }
    ],
    "total": 2
}
```
## /instances/{instancename}
Retrieve status of a specific instance being monitored

`curl http://localhost:25500/instances/StreamsInstance`

```json
{

    "instanceName": "StreamsInstance",
    "instanceStatus": "running",
    "instanceStartTime": 1506945075968

}
```

## /instances/{instancename}/resourceMetrics
Retrieves resources and specific metrics about them

`curl http://localhost:25500/instances/StreamsInstance/resourceMetrics`

```json
{

    "streamsqse.localdomain": {
        "cpuSpeed": 8677,
        "memoryFree": 385960,
        "memoryTotal": 3908520,
        "loadAverage": 0,
        "nProcessors": 2
    }

}
```

## /instances/{instancename}/joblist
List of job names and ids along with links to details about the job

`curl http://localhost:25500/joblist`

```json
{

    "total": 1,
    "jobs": [
        {
            "name": "SimpleJob_0",
            "id": 0,
            "metrics": "http://localhost:25500/instances/StreamsInstance/jobs/0/metrics",
            "jobInfo": "http://localhost:25500/instances/StreamsInstance/jobs/0",
            "snapshot": "http://localhost:25500/instances/StreamsInstance/jobs/0/snapshot"
            "snapshotnow": "http://localhost:25500/instances/StreamsInstance/jobs/0/snapshotnow"
        }
    ]

}
```

## /instances/{instancename}/jobs/{jobid}/status
Status of a single job

`curl http://localhost:25500/instances/StreamsInstance/jobs/0/status`

```json
{
    "status": "running"
}
```

## /instances/{instancename}/jobs/{jobid}/health
Health of a single job


## /instances/{instancename}/jobs/{jobid}/metrics
Metrics of a single job



## /instances/{instancename}/jobs
Array of all jobs in the instance along with job information and the metrics for each job



## /instances/{instancename}/jobs/{jobid}
Job information and metrics for a single job


## /streamsexporter || /jobtracker
The Provides a complete overview of the streams-metric-exporter server.  Not recommended for programatic interface, however, a good interface for status of this server


## /instances/{instancename}/metrics
Retrieves all metrics for the selected instance

## /instances/{instancename}/snapshots
Retrieves all snapshots for the selected instance

# Passthrough REST Endpoints
These endpoints are not cached, rather, they are passed through directly to the Streams JMX Server API

## instances/{instancename}/jobs/{jobid}/snapshot

Calls the snapshot() jmx method on the specified job

### Parameters:

depth : depth of job topology to return (default: 1)

static: include static job attributes in addition to dynamic attributes (default: true)

`curl http://localhost:25500/instances/StreamsInstance/jobs/0/snapshot`

`curl http://localhost:25500/instances/StreamsInstance/jobs/0/snapshot?depth=2&static=false`

`curl http://localhost:25500/instances/StreamsInstande/jobs/0/snapshot?depth=2&statuc=true`

```json
{

    "applicationVersion": "4211",
    "instance": "StreamsInstance",
    "submitParameters": [ ],
    "startedBy": "streamsadmin",
    "health": "healthy",
    "resources": [
        {
            "id": "streamsqse.localdomain"
        }
    ],
    "jobGroup": "default",
    "dataPath": "/home/streamsadmin/git/streams/spl/SimpleJob/-a",
    "adlFile": "output/SimpleJob.adl",
    "submitTime": 1506945170000,
    "applicationPath": "toolkits/SimpleJob",
    "checkpointPath": "/home/streamsadmin/git/streams/spl/SimpleJob/-a/ckpt",
    "outputPath": "output",
    "domain": "StreamsDomain",
    "name": "SimpleJob_0",
    "id": "0",
    "applicationName": "SimpleJob",
    "status": "running",
    "applicationScope": "Default",
    "pes": [
        {
            "launchCount": 1,
            "restartable": true,
            "resource": "streamsqse.localdomain",
            "health": "healthy",
            "relocatable": true,
            "tracingLevel": "error",
            "optionalConnections": "connected",
            "resourceTags": [ ],
            "pendingTracingLevel": null,
            "indexWithinJob": 0,
            "statusReason": "none",
            "processId": "5951",
            "operators": [
                {
                    "indexWithinJob": 2,
                    "name": "FunctorStream",
                    "operatorKind": "spl.relational::Functor"
                },
                {
                    "indexWithinJob": 0,
                    "name": "BeaconStream",
                    "operatorKind": "spl.utility::Beacon"
                },
                {
                    "indexWithinJob": 1,
                    "name": "FilterStream",
                    "operatorKind": "spl.relational::Filter"
                },
                {
                    "indexWithinJob": 3,
                    "name": "SinkStream",
                    "operatorKind": "spl.relational::Functor"
                },
                {
                    "indexWithinJob": 4,
                    "name": "Sink",
                    "operatorKind": "spl.utility::Custom"
                }
            ],
            "requiredConnections": "connected",
            "outputPorts": [ ],
            "inputPorts": [ ],
            "id": "0",
            "osCapabilities": [ ],
            "status": "running"
        }
    ]

}
```
