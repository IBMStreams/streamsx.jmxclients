# streams-metric-exporter

This application provides an interface to the IBM Streams - Stream Processing System for the purposes of retrieving status and metrics of an IBM Streams domain, instances, jobs, and resources.

This version provides 2 HTTP/HTTPS interfaces:
* Prometheus: HTTP/Text endpoint in Prometheus metrics format.
* REST: Multiple endpoints returning json.

IBM Streams provides a JMX Service (with HTTP GET interface for batch metric pulls) that is capable of providing status of the Streams instance, deployed streaming application (jobs), and cluster resources.  In addition, metrics are available via the Streams JMX Server.

The service supports user-defined custom metrics that are found with Streams jobs.  These custom metrics do not have to be predefined within the Streams Metric Exporter.  They are automatically discovered and made available as Prometheus metrics and in the REST JSON.

The primary use-case for this application is as a Prometheus metrics exporter to provide time series displays using Grafana.<br>
The original use case for this application was as a Streams Application Metrics exporter.  It is not meant to monitor the internal system services of IBM Streams.  It does, however, provide metrics and status of domains, instances, and resources which do give a operations view of the health of your system.<br>
This application is **optimized** for better performance over per-job metric scraping approaches.  The metrics and status are pulled from IBM Streams for all job metrics and status (via the JMX Server HTTP callbacks). The pulls from IBM STreams can be performed whenever a rest endpoint is hit (on-demand) or scheduled to be pulled and cached with a configurable interval.  Users can use the REST endpoints (including Prometheus endpoint) to get metrics and status of specific jobs, thus providing granular access to the information without sacrificing performance and impacting the services of IBM Streams.

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
4. [Logging](#logging)
5. [Redirecting JMX HTTP URLs](#redirecting-jmx-http-urls)
6. [Prometheus Integration](#prometheus-integration)
7. [Running with Docker](#running-with-docker)
8. [Cached REST endpoints](#cached-rest-endpoints)
9. [Passthrough REST endpoints](#passthrough-rest-endpoints)

# Building the application

## Install Dependencies
This application does NOT need to be compiled on a system with IBM Streams installed.  The lib folder contains two redistributable .jar files from IBM Streams that must be installed into your local maven repository.

There is a Makefile included that will do this installation.

```
make setup
```

## Compiling the application
```
mvn compile
```

## Create executable .jar with dependencies included
```
mvn package
```
or
```
make package
```
Location will be: target/executable-streams-metric-exporter.jar

## Create packaged binary and supporting files in .tar.gz
```
make tar
```
Location will be: target/streams-metric-exporter-x.x.x-release.tar.gz


# Command line options
`java -jar target/executable-streams-metric-exporter.jar --help`

<pre>
Usage: streams-metric-exporter [options]
  Options:
    -d, --domain
      Streams domain name
      Environment Variable: STREAMS_DOMAIN_ID
      Default: StreamsDomain
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
    --jmxhttphost
      Host or IP used to replace jmx http large data set URL host fields.  Not usually needed. Use with caution.      Environment
      Variable: STREAMS_EXPORTER_JMX_HTTP_HOST
    --jmxhttpport
      Port used to replace jmx http large data set URL port fields.  Not usually needed. Use with caution.      Environment
      Variable: STREAMS_EXPORTER_JMX_HTTP_PORT
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
      Default: service:jmx:jmxmp://192.168.242.141:9975
    --logdir
      Logging direcotry.  If not set or empty log to stdout.
     Environment Variable: STREAMS_EXPORTER_LOGDIR
      Default: <empty string>
    -l, --loglevel
      Logging level [ fatal | error | warn | info | debug | trace ]
      Environment Variable: STREAMS_EXPORTER_LOGLEVEL
      Default: trace
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
    -v, --version
      Display version information
      Default: false
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
The ``--instancelist`` option allows the specification of 0 or more instances.  If none or chosen or it is set to the value ``ALL`` then all instances metrics will be exported.  In addition, instances are created and removed, the cooresponding metrics will be added and removed.  If, however, you specify a specific list of instances, then any addtional instances that exist in the domain or are created after the application is started WILL NOT be included in the exported set.

# Logging
Logging is performed through the log4j 1.2 facility. There are two arguments to control logging.

| argument | env | default | description |
|:---------|:----|:--------|:------------|
|--logdir|STREAM_EXPORTER_LOGDIR|undefined<br>(stdout)|If this argument is undefined log messages are sent to the **console** (stdout).<br>If this argument is present then a rolling logfile is created in the directory specified with the name of the logfile: **StreamsMetricExporter.log**|
|--loglevel|STREAMS_EXPORTER_LOGLEVEL|info|fatal,error,warn,info,debug,trace<br>**note:** debug level contains timing messages|


## Adding to the default logging
If you wish to configure your own logging (in addition to that which the application already does), create a log4j.properties file and point to it using the log4j.configuration java property.  For example:
```
java -Dlog4j.configuration=file:${PWD}/log4j.properties -jar target/executable-streams-metric-exporter.jar -j \
service:jmx:jmxmp://localhost:9975 -d StreamsDomain -i \
StreamsInstance -u streamsadmin
```
This would be useful in situations when you want to log to both the console and a file.<br>
**Note:** The log level will still be set by the command line argument or environment variable, NOT the rootlogger value in your log4j.properties.

# Redirecting JMX HTTP URLs
There are some configurations where you will need to override the URLs returned for large data sets to be pulled from IBM Streams over Http.
<br>
There are two arguments that can be used to overwrite the JMX Http URL that is used for large data objects.

| argument | env | default | description |
|:---------|:----|:--------|:------------|
|--jmxhttphost|STREAM_EXPORTER_JMX_HTTP_HOST|undefined|Host name or IP Address<br>If this argument is specified the host field of the JMX Http URL will be replaced with this value|
|--jmxhttpport|STREAMS_EXPORTER_JMX_HTTP_PORT|undefined|Port number<br>If this argument is specified the port field of the JMX Http URL will be replaced with this value|


## JMX HTTP Host and Port redirection examples and Kubernetes
IBM Streams JMX API has several calls that return urls that are to be used by an HTTP Get request to pull back large data items.  Examples used in this application include: `snapshotJobMetrics()` and `snapshotJobs()`.

The default configuration of IBM Streams uses a random port for the JMX Http Server.  To override this and set a specific port set the following IBM Streams properties:

| Streams Version | Property Type | Property |
|:----------------|:--------------|:---------|
| 4.2.1.3 & earlier | Instance | sam.jmxHttpPort |
| 4.2.4 & later | Domain | jmx.httpPort |

In cases where the Streams JMX Server is running inside of a container or environment with private IP addresses and a gateway, the URLs returned from these calls will use the host and port of the internal address of the JMX Http Port.  In addition, the default configuration of Streams will use a random port at startup for the JMX Http Server.

Using the `--jmxhttphost` and `--jmxhttpport` arguments or environment variables an be used to override the URL before the HTTP Get request is performed.

Logging at the `debug` level will provide messages showing thge URLs before and after the override.

When the Streams domain is running inside of Kubernetes, and the Streams metric export is running outside of the Kubernetes cluster, Kubernetes Service Objects are used to map external facing hostnames and ports to internal pod hosts (ip address) and ports.  The JMX HTTP Host and Port override arguments can be used to get around this issue.  Provide the Kubernetes external cluster hostname and the NodePort of the service as the arguments for Streams Metric Exporter.

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
streams_operator_myCustomMetric
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
| **streams_operator_**|streams operator metrics<br>**Includes custom metrics**|
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
streams_operator_ip_nTuplesProcessed{domainname="StreamsDomain",instancename="StreamsInstance",jobname="MultiPEJob",resource="streamsqse",peid="1",operatorname="FilterStream",inputportname="BeaconStream",} 675632.0

# HELP streams_pe_op_connection_congestionFactor Streams pe output port connection metric: congestionFactor
# TYPE streams_pe_op_connection_congestionFactor gauge
streams_pe_op_connection_congestionFactor{domainname="StreamsDomain",instancename="StreamsInstance",jobname="MultiPEJob",resource="streamsqse",peid="1",index="0",connectionid="o0p1i0p0",} 0.0
```

# Grafana Examples
See [dashboards directory](dashboards/README.md) for more information.

![Grafana Example](images/IBMStreamsDomainDashboard.png)

# Running with Docker

The easiest way to try out the Streams Metric Exporter is to run it using Docker.  Included in this release is a Dockerfile for building the image and a docker-compose.yml file for starting it up with Prometheus and Grafana instances.

The versions of Prometheus and Grafana specified in the docker-compose.yml file are those that were used for testing.

The current version uses Docker 5.0 and its new provisioning capabilities to create the initial datasource pointing to prometheus and initial general purpose dashboards for Streams domains and instances.

## Prerequisites

* Compiled version of Streams Metric Exporter (executable-streams-metric-exporter.jar)
* JMX Access to a running IBM Streams 4.2.1 Domain (JMX Port 9975 is the default)
* Docker Engine (version 17.05.0.ce used in development)
* Docker Compose (version 1.9.0-5 with .yml file format 2 used in development)
* Access to Dockerhub or local repository with Images:
  * ibmjava:sfj-alpine (or any 1.8 version)
  * prom/prometheus (2.1.0 used in development)
  * grafana/grafana (5.0.4 used in development)

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
6. Open Grafana in browswer
```
http://localhost:3000
```
7. Login with default username/password
```
admin/admin
```
8. Navigate to Home Dashboard
```
IBM Streams Sample Dashboard
```
9. View raw metrics output from /prometheus endpoint
```
http://localhost:25500/prometheus
```
10. Query the prometheus ui
```
http://localhost:9090
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
      "snapshotnow": "http://localhost:25500/instances/StreamsInstance/jobs/0/snapshotnow",
      "name": "MultiPEJob",
      "id": 0,
      "metrics": "http://localhost:25500/instances/StreamsInstance/jobs/0/metrics",
      "jobInfo": "http://localhost:25500/instances/StreamsInstance/jobs/0",
      "snapshot": "http://localhost:25500/instances/StreamsInstance/jobs/0/snapshot"
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

## /instances/{instancename}/jobs/{jobid}/snapshots
Snapshot of a single job

## /instances/{instancename}/jobs
Array of all jobs in the instance along with job information and the metrics for each job



## /instances/{instancename}/jobs/{jobid}
Job information and metrics for a single job

## /instances/{instancename}/metrics
Retrieves all metrics for the selected instance

## /instances/{instancename}/snapshots
Retrieves all snapshots for the selected instance

## /streamsexporter || /streamsexporter
The Provides a complete overview of the streams-metric-exporter server.  Not recommended for programatic interface, however, a good interface for status of this server

## /config
Displays JSON view of the configuration values the program is using.<br>
**Note:** passwords are displayed as "(hidden)" if they were specified


# Passthrough REST Endpoints
These endpoints are not cached, rather, they are passed through directly to the Streams JMX Server API

## instances/{instancename}/jobs/{jobid}/snapshotnow

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
