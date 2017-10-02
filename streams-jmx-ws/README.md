# streamsx-jmx-ws
This application caches IBM Streams metrics and status and presents a REST endpoint.  This application improves performance by periodically pulling all job metrics and caching them.  Users can use the REST endpoints to get metrics and status of specific jobs.  This layer reduces simplifies clients that need to monitor IBM Streams.

## Questions
For questions and issues, please contact:

Brian M Williams, IBM<br>
bmwilli@us.ibm.com

# Contents
1. [Buil streams-jmx-ws](#building-the-application)
2. [Command line options](#command-line-options)
3. [Running streams-jmx-ws](#running-the-application)
4. [REST endpoints](#rest-endpoints)

# Building the application

## Dependencies
The build location must be a linux node with IBM Streams installed.  The environment variable (STREAMS_INSTALL) must be set.  The pom.xml file references the IBM Streams JMX API classes in the product directory.
## Compiling the application
`mvn compile`

## Create executable .jar with dependencies included
`mvn package`
Location will be: target/executable-streams-jmx-ws.jar

# Command line options
`java -jar target/executable-streams-jmx-ws.jar --help`
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
`java -jar target/executable-streams-jmx-ws.jar -j service:jmx:jmxmp://localhost:9975 -d StreamsDomain -i StreamsInstance -u streamsadmin`
`password: <enter streamsadmin password>`

# REST endpoints
## /jobtracker
The Provides a complete overview of the streams-jmx-ws server.  Not recommended for programatic interface, however, a good interface for status of this server
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
