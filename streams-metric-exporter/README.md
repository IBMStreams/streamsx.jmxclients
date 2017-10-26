# streams-metric-exporter
(formerly the poorly named streams-metric-exporter)
This application provides an interface to the IBM Streams - Stream Processing System for the purposes of retrieving status and metrics of streams instances, jobs, and resources.

This version provides 2 interfaces:
* Prometheus: HTTP/Text endpoint in Prometheus metrics format.
* REST: Multiple endpoints returning json.

IBM Streams provides a JMX Service (with HTTP GET interface for batch metric pulls) that is capable of providing status of the Streams instance, deployed streaming application (jobs), and cluster resources.  In addition, metrics are available via the Streams JMX Server.

The primary use-case for this application is as a Prometheus metrics exporter to provide time series displays using Grafana. 
It is meant to be used as a Streams Application Metrics exporter.  It is not meant to monitor the internal system services of IBM Streams.
This application improves performance over per-job metric scraping by periodically pulling all job metrics (via the JMX Server HTTP callbacks) and caching them.  Users can use the REST endpoints (including Prometheus endpoint) to get metrics and status of specific jobs.

## Questions
For questions and issues, please contact:

Brian M Williams, IBM<br>
bmwilli@us.ibm.com

# Contents
1. [Building streams-metric-exporter](#building-the-application)
2. [Command line options](#command-line-options)
3. [Running streams-metric-exporter](#running-the-application)
4. [Prometheus Integration](#prometheus-integration)
5. [Grafana Dashboard Example](#grafana-example)
5. [Cached REST endpoints](#cached-rest-endpoints)
6. [Passthrough REST endpoints](#passthrough-rest-endpoints)

# Building the application

## Dependencies
The build location must be a linux node with IBM Streams installed.  The environment variable (STREAMS_INSTALL) must be set.  The pom.xml file references the IBM Streams JMX API classes in the product directory.
## Compiling the application
`mvn compile`

## Create executable .jar with dependencies included
`mvn package`

Location will be: target/executable-streams-metric-exporter.jar

# Command line options
`java -jar target/executable-streams-metric-exporter.jar --help`

<pre>
Usage: <main class> [options]
  Options:
  * -d, --domain
       Streams domain name
    --help
       Default: false
    -h, --host
       REST Servers host interface to listen (e.g. localhost)
       Default: localhost
  * -i, --instance
       Streams instance name
  * -j, --jmxurl
       JMX Connection URL (e.g. service:jmx:jmxmp://server:9975)
    --noconsole
       Indicates that the console should not be used for prompts
       Default: false
    -p, --port
       REST Server port to listen on (e.g. 25500
       Default: 25500
    -r, --refresh
       Refresh rate of metrics in seconds
       Default: 10
    --truststore
       Path to a Java keystore containing the trusted jmx server's certificate
    -u, --user
       Streams login user name
    --webPath
       REST Base URI Web Path (e.g. /thispath)
       Default: /
    -x, --x509cert
       X509 Certification file to use instead of username/password, defaults to
       $STREAMS_X509CERT
</pre>

# Running the application
`java -jar target/executable-streams-metric-exporter.jar -j service:jmx:jmxmp://localhost:9975 -d StreamsDomain -i StreamsInstance -u streamsadmin`

`password: <enter streamsadmin password>`

# Prometheus Integration
## Endpoint
```/prometheus```
## prometheus.yml
  - job_name: "ibmstreams"
    scrape_interval: "15s"
    metrics_path: "/prometheus"
    static_configs:
    - targets: ['localhost:25500']
## metric names
The set of metrics exposed in continuously increasing as this is active development
The metric names chosen for this endpoint are a hybrid of prometheus naming conventions and the pre-defined metrics of IBMStreams.
The pattern for metric names is<br>
```streams_<objecttype>[_<subobjecttype>]_[_<aggregationtype>]_<streams metric>```
Examples
```
streams_operator_ip_nTuplesProcessed
streams_job_max_congestionFactor
```
## label names
label names:
```
instancename (instance is reserved by prometheus)
jobname (job is reserved by prometheus)
operatorname
inputportname
outputportname
```
# Grafana Examples
See the dashboards directory

# Cached REST endpoints
## /instance
Retrieve status of instance being monitored

`curl http://localhost:25500/instance`

```json
{

    "instanceName": "StreamsInstance",
    "instanceStatus": "running",
    "instanceStartTime": 1506945075968

}
```

## /instance/resourceMetrics
Retrieves resources and specific metrics about them

`curl http://localhost:25500/instance/resourceMetrics`

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

## /joblist
List of job names and ids along with links to details about the job

`curl http://localhost:25500/joblist`

```json
{

    "total": 1,
    "jobs": [
        {
            "name": "SimpleJob_0",
            "id": 0,
            "metrics": "http://localhost:25500/jobs/0/metrics",
            "jobInfo": "http://localhost:25500/jobs/0",
            "snapshot": "http://localhost:25500/jobs/0/snapshot"
        }
    ]

}
```

## /jobs/{jobid}/status
Status of a single job

`curl http://localhost:25500/jobs/{jobid}/status`

```json
{
    "status": "running"
}
```

## /jobs/{jobid}/health
Health of a single job

`curl http://localhost:25500/jobs/{jobid}/health`

```json
{
    "health": "healthy"
}
```

## /jobs/{jobid}/metrics
Metrics of a single job

`curl http://localhost:25500/jobs/{jobid}/metrics`

```json
{

    "lastMetricsRefresh": 1506958998911,
    "lastMetricsFailure": null,
    "lastMetricsRefreshFailed": false,
    "jobMetrics": {
        "id": "0",
        "pes": [
            {
                "indexWithinJob": 0,
                "operators": [
                    {
                        "outputPorts": [
                            {
                                "indexWithinOperator": 0,
                                "name": "SinkStream",
                                "metrics": [
                                    {
                                        "name": "nTuplesSubmitted",
                                        "value": 676291
                                    },
                                    {
                                        "name": "nFinalPunctsSubmitted",
                                        "value": 0
                                    },
                                    {
                                        "name": "nWindowPunctsSubmitted",
                                        "value": 0
                                    }
                                ]
                            }
                        ],
                        "name": "SinkStream",
                        "inputPorts": [
                            {
                                "indexWithinOperator": 0,
                                "name": "FunctorStream",
                                "metrics": [
                                    {
                                        "name": "queueSize",
                                        "value": 0
                                    },
                                    {
                                        "name": "nWindowPunctsQueued",
                                        "value": 0
                                    },
                                    {
                                        "name": "nTuplesQueued",
                                        "value": 0
                                    },
                                    {
                                        "name": "nTuplesDropped",
                                        "value": 0
                                    },
                                    {
                                        "name": "nTuplesProcessed",
                                        "value": 676291
                                    },
                                    {
                                        "name": "nWindowPunctsProcessed",
                                        "value": 0
                                    },
                                    {
                                        "name": "nFinalPunctsQueued",
                                        "value": 0
                                    },
                                    {
                                        "name": "nFinalPunctsProcessed",
                                        "value": 0
                                    }
                                ]
                            }
                        ],
                        "metrics": [ ]
                    },
                    {
                        "outputPorts": [ ],
                        "name": "Sink",
                        "inputPorts": [
                            {
                                "indexWithinOperator": 0,
                                "name": "In",
                                "metrics": [
                                    {
                                        "name": "nWindowPunctsProcessed",
                                        "value": 0
                                    },
                                    {
                                        "name": "nTuplesProcessed",
                                        "value": 676291
                                    },
                                    {
                                        "name": "nWindowPunctsQueued",
                                        "value": 0
                                    },
                                    {
                                        "name": "queueSize",
                                        "value": 0
                                    },
                                    {
                                        "name": "nFinalPunctsQueued",
                                        "value": 0
                                    },
                                    {
                                        "name": "nTuplesDropped",
                                        "value": 0
                                    },
                                    {
                                        "name": "nTuplesQueued",
                                        "value": 0
                                    },
                                    {
                                        "name": "nFinalPunctsProcessed",
                                        "value": 0
                                    }
                                ]
                            }
                        ],
                        "metrics": [ ]
                    },
                    {
                        "outputPorts": [
                            {
                                "indexWithinOperator": 0,
                                "name": "FunctorStream",
                                "metrics": [
                                    {
                                        "name": "nWindowPunctsSubmitted",
                                        "value": 0
                                    },
                                    {
                                        "name": "nTuplesSubmitted",
                                        "value": 676291
                                    },
                                    {
                                        "name": "nFinalPunctsSubmitted",
                                        "value": 0
                                    }
                                ]
                            }
                        ],
                        "name": "FunctorStream",
                        "inputPorts": [
                            {
                                "indexWithinOperator": 0,
                                "name": "FilterStream",
                                "metrics": [
                                    {
                                        "name": "nTuplesProcessed",
                                        "value": 676291
                                    },
                                    {
                                        "name": "nWindowPunctsQueued",
                                        "value": 0
                                    },
                                    {
                                        "name": "queueSize",
                                        "value": 0
                                    },
                                    {
                                        "name": "nFinalPunctsQueued",
                                        "value": 0
                                    },
                                    {
                                        "name": "nWindowPunctsProcessed",
                                        "value": 0
                                    },
                                    {
                                        "name": "nTuplesQueued",
                                        "value": 0
                                    },
                                    {
                                        "name": "nTuplesDropped",
                                        "value": 0
                                    },
                                    {
                                        "name": "nFinalPunctsProcessed",
                                        "value": 0
                                    }
                                ]
                            }
                        ],
                        "metrics": [ ]
                    },
                    {
                        "outputPorts": [
                            {
                                "indexWithinOperator": 0,
                                "name": "BeaconStream",
                                "metrics": [
                                    {
                                        "name": "nTuplesSubmitted",
                                        "value": 1352582
                                    },
                                    {
                                        "name": "nWindowPunctsSubmitted",
                                        "value": 0
                                    },
                                    {
                                        "name": "nFinalPunctsSubmitted",
                                        "value": 0
                                    }
                                ]
                            }
                        ],
                        "name": "BeaconStream",
                        "inputPorts": [ ],
                        "metrics": [ ]
                    },
                    {
                        "outputPorts": [
                            {
                                "indexWithinOperator": 0,
                                "name": "FilterStream",
                                "metrics": [
                                    {
                                        "name": "nTuplesSubmitted",
                                        "value": 676291
                                    },
                                    {
                                        "name": "nWindowPunctsSubmitted",
                                        "value": 0
                                    },
                                    {
                                        "name": "nFinalPunctsSubmitted",
                                        "value": 0
                                    }
                                ]
                            }
                        ],
                        "name": "FilterStream",
                        "inputPorts": [
                            {
                                "indexWithinOperator": 0,
                                "name": "BeaconStream",
                                "metrics": [
                                    {
                                        "name": "nFinalPunctsProcessed",
                                        "value": 0
                                    },
                                    {
                                        "name": "nTuplesProcessed",
                                        "value": 1352582
                                    },
                                    {
                                        "name": "nFinalPunctsQueued",
                                        "value": 0
                                    },
                                    {
                                        "name": "nWindowPunctsQueued",
                                        "value": 0
                                    },
                                    {
                                        "name": "queueSize",
                                        "value": 0
                                    },
                                    {
                                        "name": "nTuplesQueued",
                                        "value": 0
                                    },
                                    {
                                        "name": "nTuplesDropped",
                                        "value": 0
                                    },
                                    {
                                        "name": "nWindowPunctsProcessed",
                                        "value": 0
                                    }
                                ]
                            }
                        ],
                        "metrics": [ ]
                    }
                ],
                "outputPorts": [ ],
                "inputPorts": [ ],
                "id": "0",
                "metrics": [
                    {
                        "name": "nResidentMemoryConsumption",
                        "value": 33892
                    },
                    {
                        "name": "nMemoryConsumption",
                        "value": 810016
                    },
                    {
                        "name": "nCpuMilliseconds",
                        "value": 61950
                    }
                ],
                "lastTimeRetrieved": 1506958996000
            }
        ]
    }

}
```

## /jobs
Array of all jobs in the instance along with job information and the metrics for each job

`curl http://localhost:25500/jobs`

```json
{

    "total": 1,
    "jobs": [
        {
            "id": 0,
            "status": "running",
            "lastMetricsRefresh": 1506958518905,
            "lastMetricsFailure": null,
            "lastMetricsRefreshFailed": false,
            "adlFile": "output/SimpleJob.adl",
            "applicationName": "SimpleJob",
            "applicationPath": "toolkits/SimpleJob",
            "applicationScope": "Default",
            "applicationVersion": "4211",
            "dataPath": "/home/streamsadmin/git/streams/spl/SimpleJob/-a",
            "domain": "StreamsDomain",
            "health": "HEALTHY",
            "instance": "StreamsInstance",
            "jobGroup": "default",
            "name": "SimpleJob_0",
            "outputPath": "output",
            "startedByUser": "streamsadmin",
            "submitTime": 1506945170000,
            "jobMetrics": {
                "id": "0",
                "pes": [
                    {
                        "indexWithinJob": 0,
                        "operators": [
                            {
                                "outputPorts": [
                                    {
                                        "indexWithinOperator": 0,
                                        "name": "SinkStream",
                                        "metrics": [
                                            {
                                                "name": "nTuplesSubmitted",
                                                "value": 652797
                                            },
                                            {
                                                "name": "nFinalPunctsSubmitted",
                                                "value": 0
                                            },
                                            {
                                                "name": "nWindowPunctsSubmitted",
                                                "value": 0
                                            }
                                        ]
                                    }
                                ],
                                "name": "SinkStream",
                                "inputPorts": [
                                    {
                                        "indexWithinOperator": 0,
                                        "name": "FunctorStream",
                                        "metrics": [
                                            {
                                                "name": "nWindowPunctsQueued",
                                                "value": 0
                                            },
                                            {
                                                "name": "nFinalPunctsQueued",
                                                "value": 0
                                            },
                                            {
                                                "name": "nTuplesQueued",
                                                "value": 0
                                            },
                                            {
                                                "name": "nTuplesDropped",
                                                "value": 0
                                            },
                                            {
                                                "name": "nTuplesProcessed",
                                                "value": 652797
                                            },
                                            {
                                                "name": "nWindowPunctsProcessed",
                                                "value": 0
                                            },
                                            {
                                                "name": "nFinalPunctsProcessed",
                                                "value": 0
                                            },
                                            {
                                                "name": "queueSize",
                                                "value": 0
                                            }
                                        ]
                                    }
                                ],
                                "metrics": [ ]
                            },
                            {
                                "outputPorts": [ ],
                                "name": "Sink",
                                "inputPorts": [
                                    {
                                        "indexWithinOperator": 0,
                                        "name": "In",
                                        "metrics": [
                                            {
                                                "name": "nFinalPunctsQueued",
                                                "value": 0
                                            },
                                            {
                                                "name": "nTuplesQueued",
                                                "value": 0
                                            },
                                            {
                                                "name": "nFinalPunctsProcessed",
                                                "value": 0
                                            },
                                            {
                                                "name": "nWindowPunctsProcessed",
                                                "value": 0
                                            },
                                            {
                                                "name": "queueSize",
                                                "value": 0
                                            },
                                            {
                                                "name": "nTuplesDropped",
                                                "value": 0
                                            },
                                            {
                                                "name": "nTuplesProcessed",
                                                "value": 652797
                                            },
                                            {
                                                "name": "nWindowPunctsQueued",
                                                "value": 0
                                            }
                                        ]
                                    }
                                ],
                                "metrics": [ ]
                            },
                            {
                                "outputPorts": [
                                    {
                                        "indexWithinOperator": 0,
                                        "name": "FunctorStream",
                                        "metrics": [
                                            {
                                                "name": "nFinalPunctsSubmitted",
                                                "value": 0
                                            },
                                            {
                                                "name": "nTuplesSubmitted",
                                                "value": 652797
                                            },
                                            {
                                                "name": "nWindowPunctsSubmitted",
                                                "value": 0
                                            }
                                        ]
                                    }
                                ],
                                "name": "FunctorStream",
                                "inputPorts": [
                                    {
                                        "indexWithinOperator": 0,
                                        "name": "FilterStream",
                                        "metrics": [
                                            {
                                                "name": "queueSize",
                                                "value": 0
                                            },
                                            {
                                                "name": "nFinalPunctsProcessed",
                                                "value": 0
                                            },
                                            {
                                                "name": "nWindowPunctsQueued",
                                                "value": 0
                                            },
                                            {
                                                "name": "nTuplesProcessed",
                                                "value": 652797
                                            },
                                            {
                                                "name": "nTuplesDropped",
                                                "value": 0
                                            },
                                            {
                                                "name": "nFinalPunctsQueued",
                                                "value": 0
                                            },
                                            {
                                                "name": "nWindowPunctsProcessed",
                                                "value": 0
                                            },
                                            {
                                                "name": "nTuplesQueued",
                                                "value": 0
                                            }
                                        ]
                                    }
                                ],
                                "metrics": [ ]
                            },
                            {
                                "outputPorts": [
                                    {
                                        "indexWithinOperator": 0,
                                        "name": "BeaconStream",
                                        "metrics": [
                                            {
                                                "name": "nFinalPunctsSubmitted",
                                                "value": 0
                                            },
                                            {
                                                "name": "nWindowPunctsSubmitted",
                                                "value": 0
                                            },
                                            {
                                                "name": "nTuplesSubmitted",
                                                "value": 1305594
                                            }
                                        ]
                                    }
                                ],
                                "name": "BeaconStream",
                                "inputPorts": [ ],
                                "metrics": [ ]
                            },
                            {
                                "outputPorts": [
                                    {
                                        "indexWithinOperator": 0,
                                        "name": "FilterStream",
                                        "metrics": [
                                            {
                                                "name": "nWindowPunctsSubmitted",
                                                "value": 0
                                            },
                                            {
                                                "name": "nTuplesSubmitted",
                                                "value": 652797
                                            },
                                            {
                                                "name": "nFinalPunctsSubmitted",
                                                "value": 0
                                            }
                                        ]
                                    }
                                ],
                                "name": "FilterStream",
                                "inputPorts": [
                                    {
                                        "indexWithinOperator": 0,
                                        "name": "BeaconStream",
                                        "metrics": [
                                            {
                                                "name": "nTuplesQueued",
                                                "value": 0
                                            },
                                            {
                                                "name": "nTuplesDropped",
                                                "value": 0
                                            },
                                            {
                                                "name": "nWindowPunctsQueued",
                                                "value": 0
                                            },
                                            {
                                                "name": "queueSize",
                                                "value": 0
                                            },
                                            {
                                                "name": "nFinalPunctsQueued",
                                                "value": 0
                                            },
                                            {
                                                "name": "nWindowPunctsProcessed",
                                                "value": 0
                                            },
                                            {
                                                "name": "nTuplesProcessed",
                                                "value": 1305594
                                            },
                                            {
                                                "name": "nFinalPunctsProcessed",
                                                "value": 0
                                            }
                                        ]
                                    }
                                ],
                                "metrics": [ ]
                            }
                        ],
                        "outputPorts": [ ],
                        "inputPorts": [ ],
                        "id": "0",
                        "metrics": [
                            {
                                "name": "nMemoryConsumption",
                                "value": 810016
                            },
                            {
                                "name": "nResidentMemoryConsumption",
                                "value": 33892
                            },
                            {
                                "name": "nCpuMilliseconds",
                                "value": 60000
                            }
                        ],
                        "lastTimeRetrieved": 1506958516000
                    }
                ]
            }
        }
    ]

}
```

## /jobs/{jobid}
Job information and metrics for a single job

`curl http://localhost:25500/jobs/{jobid}`

```json
{

    "id": 0,
    "status": "running",
    "lastMetricsRefresh": 1506958648907,
    "lastMetricsFailure": null,
    "lastMetricsRefreshFailed": false,
    "adlFile": "output/SimpleJob.adl",
    "applicationName": "SimpleJob",
    "applicationPath": "toolkits/SimpleJob",
    "applicationScope": "Default",
    "applicationVersion": "4211",
    "dataPath": "/home/streamsadmin/git/streams/spl/SimpleJob/-a",
    "domain": "StreamsDomain",
    "health": "HEALTHY",
    "instance": "StreamsInstance",
    "jobGroup": "default",
    "name": "SimpleJob_0",
    "outputPath": "output",
    "startedByUser": "streamsadmin",
    "submitTime": 1506945170000,
    "jobMetrics": {
        "id": "0",
        "pes": [
            {
                "indexWithinJob": 0,
                "operators": [
                    {
                        "outputPorts": [
                            {
                                "indexWithinOperator": 0,
                                "name": "SinkStream",
                                "metrics": [
                                    {
                                        "name": "nWindowPunctsSubmitted",
                                        "value": 0
                                    },
                                    {
                                        "name": "nFinalPunctsSubmitted",
                                        "value": 0
                                    },
                                    {
                                        "name": "nTuplesSubmitted",
                                        "value": 659115
                                    }
                                ]
                            }
                        ],
                        "name": "SinkStream",
                        "inputPorts": [
                            {
                                "indexWithinOperator": 0,
                                "name": "FunctorStream",
                                "metrics": [
                                    {
                                        "name": "queueSize",
                                        "value": 0
                                    },
                                    {
                                        "name": "nTuplesQueued",
                                        "value": 0
                                    },
                                    {
                                        "name": "nTuplesDropped",
                                        "value": 0
                                    },
                                    {
                                        "name": "nWindowPunctsQueued",
                                        "value": 0
                                    },
                                    {
                                        "name": "nFinalPunctsQueued",
                                        "value": 0
                                    },
                                    {
                                        "name": "nWindowPunctsProcessed",
                                        "value": 0
                                    },
                                    {
                                        "name": "nFinalPunctsProcessed",
                                        "value": 0
                                    },
                                    {
                                        "name": "nTuplesProcessed",
                                        "value": 659115
                                    }
                                ]
                            }
                        ],
                        "metrics": [ ]
                    },
                    {
                        "outputPorts": [ ],
                        "name": "Sink",
                        "inputPorts": [
                            {
                                "indexWithinOperator": 0,
                                "name": "In",
                                "metrics": [
                                    {
                                        "name": "nTuplesProcessed",
                                        "value": 659115
                                    },
                                    {
                                        "name": "nTuplesDropped",
                                        "value": 0
                                    },
                                    {
                                        "name": "nWindowPunctsQueued",
                                        "value": 0
                                    },
                                    {
                                        "name": "nFinalPunctsQueued",
                                        "value": 0
                                    },
                                    {
                                        "name": "nTuplesQueued",
                                        "value": 0
                                    },
                                    {
                                        "name": "queueSize",
                                        "value": 0
                                    },
                                    {
                                        "name": "nFinalPunctsProcessed",
                                        "value": 0
                                    },
                                    {
                                        "name": "nWindowPunctsProcessed",
                                        "value": 0
                                    }
                                ]
                            }
                        ],
                        "metrics": [ ]
                    },
                    {
                        "outputPorts": [
                            {
                                "indexWithinOperator": 0,
                                "name": "FunctorStream",
                                "metrics": [
                                    {
                                        "name": "nWindowPunctsSubmitted",
                                        "value": 0
                                    },
                                    {
                                        "name": "nFinalPunctsSubmitted",
                                        "value": 0
                                    },
                                    {
                                        "name": "nTuplesSubmitted",
                                        "value": 659115
                                    }
                                ]
                            }
                        ],
                        "name": "FunctorStream",
                        "inputPorts": [
                            {
                                "indexWithinOperator": 0,
                                "name": "FilterStream",
                                "metrics": [
                                    {
                                        "name": "nTuplesProcessed",
                                        "value": 659115
                                    },
                                    {
                                        "name": "nTuplesDropped",
                                        "value": 0
                                    },
                                    {
                                        "name": "nWindowPunctsQueued",
                                        "value": 0
                                    },
                                    {
                                        "name": "queueSize",
                                        "value": 0
                                    },
                                    {
                                        "name": "nWindowPunctsProcessed",
                                        "value": 0
                                    },
                                    {
                                        "name": "nTuplesQueued",
                                        "value": 0
                                    },
                                    {
                                        "name": "nFinalPunctsProcessed",
                                        "value": 0
                                    },
                                    {
                                        "name": "nFinalPunctsQueued",
                                        "value": 0
                                    }
                                ]
                            }
                        ],
                        "metrics": [ ]
                    },
                    {
                        "outputPorts": [
                            {
                                "indexWithinOperator": 0,
                                "name": "BeaconStream",
                                "metrics": [
                                    {
                                        "name": "nTuplesSubmitted",
                                        "value": 1318230
                                    },
                                    {
                                        "name": "nFinalPunctsSubmitted",
                                        "value": 0
                                    },
                                    {
                                        "name": "nWindowPunctsSubmitted",
                                        "value": 0
                                    }
                                ]
                            }
                        ],
                        "name": "BeaconStream",
                        "inputPorts": [ ],
                        "metrics": [ ]
                    },
                    {
                        "outputPorts": [
                            {
                                "indexWithinOperator": 0,
                                "name": "FilterStream",
                                "metrics": [
                                    {
                                        "name": "nFinalPunctsSubmitted",
                                        "value": 0
                                    },
                                    {
                                        "name": "nWindowPunctsSubmitted",
                                        "value": 0
                                    },
                                    {
                                        "name": "nTuplesSubmitted",
                                        "value": 659115
                                    }
                                ]
                            }
                        ],
                        "name": "FilterStream",
                        "inputPorts": [
                            {
                                "indexWithinOperator": 0,
                                "name": "BeaconStream",
                                "metrics": [
                                    {
                                        "name": "nFinalPunctsProcessed",
                                        "value": 0
                                    },
                                    {
                                        "name": "nWindowPunctsQueued",
                                        "value": 0
                                    },
                                    {
                                        "name": "nWindowPunctsProcessed",
                                        "value": 0
                                    },
                                    {
                                        "name": "nTuplesProcessed",
                                        "value": 1318230
                                    },
                                    {
                                        "name": "queueSize",
                                        "value": 0
                                    },
                                    {
                                        "name": "nTuplesQueued",
                                        "value": 0
                                    },
                                    {
                                        "name": "nTuplesDropped",
                                        "value": 0
                                    },
                                    {
                                        "name": "nFinalPunctsQueued",
                                        "value": 0
                                    }
                                ]
                            }
                        ],
                        "metrics": [ ]
                    }
                ],
                "outputPorts": [ ],
                "inputPorts": [ ],
                "id": "0",
                "metrics": [
                    {
                        "name": "nCpuMilliseconds",
                        "value": 60520
                    },
                    {
                        "name": "nMemoryConsumption",
                        "value": 810016
                    },
                    {
                        "name": "nResidentMemoryConsumption",
                        "value": 33892
                    }
                ],
                "lastTimeRetrieved": 1506958645000
            }
        ]
    }

}
```


## /jobtracker
The Provides a complete overview of the streams-metric-exporter server.  Not recommended for programatic interface, however, a good interface for status of this server

`curl http://localhost:25500/jobtracker`

```json
{

    "domain": "StreamsDomain",
    "instance": {
        "available": true,
        "name": "StreamsInstance",
        "status": "running",
        "instanceStartTime": "2017 10 02 07:51:15"
    },
    "jobMapAvailable": true,
    "jobMetricsAvailable": true,
    "instanceResourceMetricsLastUpdateTime": "2017 10 02 09:31:41",
    "jobCount": 1,
    "jobMap": [
        {
            "jobInfo": {
                "id": "0",
                "status": "running",
                "applicationName": "SimpleJob",
                "metrics": "{\"id\":\"0\",\"pes\":[{\"indexWithinJob\":0,\"operators\":[{\"outputPorts\":[{\"indexWithinOperator\":0,\"name\":\"SinkStream\",\"metrics\":[{\"name\":\"nFinalPunctsSubmitted\",\"value\":0},{\"name\":\"nTuplesSubmitted\",\"value\":289387},{\"name\":\"nWindowPunctsSubmitted\",\"value\":0}]}],\"name\":\"SinkStream\",\"inputPorts\":[{\"indexWithinOperator\":0,\"name\":\"FunctorStream\",\"metrics\":[{\"name\":\"queueSize\",\"value\":0},{\"name\":\"nFinalPunctsQueued\",\"value\":0},{\"name\":\"nFinalPunctsProcessed\",\"value\":0},{\"name\":\"nTuplesProcessed\",\"value\":289387},{\"name\":\"nWindowPunctsQueued\",\"value\":0},{\"name\":\"nTuplesQueued\",\"value\":0},{\"name\":\"nWindowPunctsProcessed\",\"value\":0},{\"name\":\"nTuplesDropped\",\"value\":0}]}],\"metrics\":[]},{\"outputPorts\":[],\"name\":\"Sink\",\"inputPorts\":[{\"indexWithinOperator\":0,\"name\":\"In\",\"metrics\":[{\"name\":\"nWindowPunctsQueued\",\"value\":0},{\"name\":\"nTuplesProcessed\",\"value\":289387},{\"name\":\"nWindowPunctsProcessed\",\"value\":0},{\"name\":\"nFinalPunctsProcessed\",\"value\":0},{\"name\":\"nTuplesQueued\",\"value\":0},{\"name\":\"nFinalPunctsQueued\",\"value\":0},{\"name\":\"queueSize\",\"value\":0},{\"name\":\"nTuplesDropped\",\"value\":0}]}],\"metrics\":[]},{\"outputPorts\":[{\"indexWithinOperator\":0,\"name\":\"FunctorStream\",\"metrics\":[{\"name\":\"nFinalPunctsSubmitted\",\"value\":0},{\"name\":\"nWindowPunctsSubmitted\",\"value\":0},{\"name\":\"nTuplesSubmitted\",\"value\":289387}]}],\"name\":\"FunctorStream\",\"inputPorts\":[{\"indexWithinOperator\":0,\"name\":\"FilterStream\",\"metrics\":[{\"name\":\"queueSize\",\"value\":0},{\"name\":\"nTuplesDropped\",\"value\":0},{\"name\":\"nTuplesQueued\",\"value\":0},{\"name\":\"nTuplesProcessed\",\"value\":289387},{\"name\":\"nFinalPunctsProcessed\",\"value\":0},{\"name\":\"nWindowPunctsProcessed\",\"value\":0},{\"name\":\"nWindowPunctsQueued\",\"value\":0},{\"name\":\"nFinalPunctsQueued\",\"value\":0}]}],\"metrics\":[]},{\"outputPorts\":[{\"indexWithinOperator\":0,\"name\":\"BeaconStream\",\"metrics\":[{\"name\":\"nWindowPunctsSubmitted\",\"value\":0},{\"name\":\"nTuplesSubmitted\",\"value\":578773},{\"name\":\"nFinalPunctsSubmitted\",\"value\":0}]}],\"name\":\"BeaconStream\",\"inputPorts\":[],\"metrics\":[]},{\"outputPorts\":[{\"indexWithinOperator\":0,\"name\":\"FilterStream\",\"metrics\":[{\"name\":\"nTuplesSubmitted\",\"value\":289387},{\"name\":\"nFinalPunctsSubmitted\",\"value\":0},{\"name\":\"nWindowPunctsSubmitted\",\"value\":0}]}],\"name\":\"FilterStream\",\"inputPorts\":[{\"indexWithinOperator\":0,\"name\":\"BeaconStream\",\"metrics\":[{\"name\":\"nTuplesDropped\",\"value\":0},{\"name\":\"nFinalPunctsProcessed\",\"value\":0},{\"name\":\"nWindowPunctsQueued\",\"value\":0},{\"name\":\"nTuplesQueued\",\"value\":0},{\"name\":\"nFinalPunctsQueued\",\"value\":0},{\"name\":\"nTuplesProcessed\",\"value\":578773},{\"name\":\"queueSize\",\"value\":0},{\"name\":\"nWindowPunctsProcessed\",\"value\":0}]}],\"metrics\":[]}],\"outputPorts\":[],\"inputPorts\":[],\"id\":\"0\",\"metrics\":[{\"name\":\"nCpuMilliseconds\",\"value\":26050},{\"name\":\"nResidentMemoryConsumption\",\"value\":33892},{\"name\":\"nMemoryConsumption\",\"value\":810016}],\"lastTimeRetrieved\":1506951100000}]}"
            }
        }
    ],
    "jobNameIndex": [
        {
            "key": "SimpleJob_0",
            "value": "0"
        }
    ]

}
```

## /metris
Retrieves all metrics for the monitored instance

`curl http://localhost:25500/metrics`

```json
{

    "lastMetricsRefresh": 1506958218912,
    "lastMetricsFailure": null,
    "lastMetricsRefreshFailed": false,
    "allMetrics": {
        "jobs": [
            {
                "id": "0",
                "pes": [
                    {
                        "indexWithinJob": 0,
                        "operators": [
                            {
                                "outputPorts": [
                                    {
                                        "indexWithinOperator": 0,
                                        "metrics": [
                                            {
                                                "name": "nFinalPunctsSubmitted",
                                                "value": 0
                                            },
                                            {
                                                "name": "nTuplesSubmitted",
                                                "value": 638117
                                            },
                                            {
                                                "name": "nWindowPunctsSubmitted",
                                                "value": 0
                                            }
                                        ]
                                    }
                                ],
                                "name": "SinkStream",
                                "inputPorts": [
                                    {
                                        "indexWithinOperator": 0,
                                        "metrics": [
                                            {
                                                "name": "nTuplesDropped",
                                                "value": 0
                                            },
                                            {
                                                "name": "nFinalPunctsQueued",
                                                "value": 0
                                            },
                                            {
                                                "name": "queueSize",
                                                "value": 0
                                            },
                                            {
                                                "name": "nFinalPunctsProcessed",
                                                "value": 0
                                            },
                                            {
                                                "name": "nWindowPunctsQueued",
                                                "value": 0
                                            },
                                            {
                                                "name": "nTuplesQueued",
                                                "value": 0
                                            },
                                            {
                                                "name": "nWindowPunctsProcessed",
                                                "value": 0
                                            },
                                            {
                                                "name": "nTuplesProcessed",
                                                "value": 638117
                                            }
                                        ]
                                    }
                                ],
                                "metrics": [ ]
                            },
                            {
                                "outputPorts": [ ],
                                "name": "Sink",
                                "inputPorts": [
                                    {
                                        "indexWithinOperator": 0,
                                        "metrics": [
                                            {
                                                "name": "nWindowPunctsQueued",
                                                "value": 0
                                            },
                                            {
                                                "name": "nFinalPunctsQueued",
                                                "value": 0
                                            },
                                            {
                                                "name": "queueSize",
                                                "value": 0
                                            },
                                            {
                                                "name": "nWindowPunctsProcessed",
                                                "value": 0
                                            },
                                            {
                                                "name": "nTuplesDropped",
                                                "value": 0
                                            },
                                            {
                                                "name": "nFinalPunctsProcessed",
                                                "value": 0
                                            },
                                            {
                                                "name": "nTuplesProcessed",
                                                "value": 638117
                                            },
                                            {
                                                "name": "nTuplesQueued",
                                                "value": 0
                                            }
                                        ]
                                    }
                                ],
                                "metrics": [ ]
                            },
                            {
                                "outputPorts": [
                                    {
                                        "indexWithinOperator": 0,
                                        "metrics": [
                                            {
                                                "name": "nTuplesSubmitted",
                                                "value": 638117
                                            },
                                            {
                                                "name": "nWindowPunctsSubmitted",
                                                "value": 0
                                            },
                                            {
                                                "name": "nFinalPunctsSubmitted",
                                                "value": 0
                                            }
                                        ]
                                    }
                                ],
                                "name": "FunctorStream",
                                "inputPorts": [
                                    {
                                        "indexWithinOperator": 0,
                                        "metrics": [
                                            {
                                                "name": "nFinalPunctsQueued",
                                                "value": 0
                                            },
                                            {
                                                "name": "queueSize",
                                                "value": 0
                                            },
                                            {
                                                "name": "nTuplesProcessed",
                                                "value": 638117
                                            },
                                            {
                                                "name": "nTuplesQueued",
                                                "value": 0
                                            },
                                            {
                                                "name": "nWindowPunctsProcessed",
                                                "value": 0
                                            },
                                            {
                                                "name": "nTuplesDropped",
                                                "value": 0
                                            },
                                            {
                                                "name": "nWindowPunctsQueued",
                                                "value": 0
                                            },
                                            {
                                                "name": "nFinalPunctsProcessed",
                                                "value": 0
                                            }
                                        ]
                                    }
                                ],
                                "metrics": [ ]
                            },
                            {
                                "outputPorts": [
                                    {
                                        "indexWithinOperator": 0,
                                        "metrics": [
                                            {
                                                "name": "nFinalPunctsSubmitted",
                                                "value": 0
                                            },
                                            {
                                                "name": "nTuplesSubmitted",
                                                "value": 1276233
                                            },
                                            {
                                                "name": "nWindowPunctsSubmitted",
                                                "value": 0
                                            }
                                        ]
                                    }
                                ],
                                "name": "BeaconStream",
                                "inputPorts": [ ],
                                "metrics": [ ]
                            },
                            {
                                "outputPorts": [
                                    {
                                        "indexWithinOperator": 0,
                                        "metrics": [
                                            {
                                                "name": "nWindowPunctsSubmitted",
                                                "value": 0
                                            },
                                            {
                                                "name": "nTuplesSubmitted",
                                                "value": 638117
                                            },
                                            {
                                                "name": "nFinalPunctsSubmitted",
                                                "value": 0
                                            }
                                        ]
                                    }
                                ],
                                "name": "FilterStream",
                                "inputPorts": [
                                    {
                                        "indexWithinOperator": 0,
                                        "metrics": [
                                            {
                                                "name": "nFinalPunctsQueued",
                                                "value": 0
                                            },
                                            {
                                                "name": "nWindowPunctsQueued",
                                                "value": 0
                                            },
                                            {
                                                "name": "nTuplesQueued",
                                                "value": 0
                                            },
                                            {
                                                "name": "nWindowPunctsProcessed",
                                                "value": 0
                                            },
                                            {
                                                "name": "nTuplesProcessed",
                                                "value": 1276233
                                            },
                                            {
                                                "name": "nFinalPunctsProcessed",
                                                "value": 0
                                            },
                                            {
                                                "name": "nTuplesDropped",
                                                "value": 0
                                            },
                                            {
                                                "name": "queueSize",
                                                "value": 0
                                            }
                                        ]
                                    }
                                ],
                                "metrics": [ ]
                            }
                        ],
                        "outputPorts": [ ],
                        "inputPorts": [ ],
                        "id": "0",
                        "metrics": [
                            {
                                "name": "nResidentMemoryConsumption",
                                "value": 33892
                            },
                            {
                                "name": "nCpuMilliseconds",
                                "value": 58740
                            },
                            {
                                "name": "nMemoryConsumption",
                                "value": 810016
                            }
                        ],
                        "lastTimeRetrieved": 1506958216000
                    }
                ]
            }
        ],
        "id": "StreamsInstance"
    }

}
```

# Passthrough REST Endpoints
These endpoints are not cached, rather, they are passed through directly to the Streams JMX Server API

## /jobs/{jobid}/snapshot

Calls the snapshot() jmx method on the specified job

### Parameters:

depth : depth of job topology to return (default: 1)

static: include static job attributes in addition to dynamic attributes (default: true)

`curl http://localhost:25500/jobs/{jobid}/snapshot`

`curl http://localhost:25500/jobs/{jobid}/snapshot?depth=2&static=false`

`curl http://localhost:25500/jobs/{jobid}/snapshot?depth=2&statuc=true`

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