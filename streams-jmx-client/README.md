# streams-jmx-client

This application provides a command-line interface to the IBM Streams - Stream Processing System.  This program functions similar to the **streamtool** command that is included with IBM Streams, however, this application uses a pure JMX interface and can be run on any java platform and does not require the IBM Streams installation on the same host.

Reasons for using this application include

* Interacting with IBM Streams from a lightweight container (e.g. Docker container)
* Interacting with IBM Streams running inside of a continer hosting environment (e.g. Kubernetes)

The application supports an interactive mode (similar to streamtool interactive mode)

The initial set of commands that are supported are limited while the plumbing and infrastructure was being created.  Requests for additional commands are welcome and contributions are encouraged.

Current commands include:
* submitjob
* canceljob
* getdomainstate
* getinstancestate
* lsjobs
* snapshotjobs

## Questions
For questions and issues, please contact:

Brian M Williams, IBM<br>
bmwilli@us.ibm.com

# Contents
1. [Building streams-jmx-client](#building-the-application)
2. [Command line options](#command-line-options)
3. [Running streams-jmx-client](#running-the-application)
4. [Logging](#logging)
5. [Redirecting JMX HTTP URLs](#redirecting-jmx-http-urls)
6. [Running with Docker](#running-with-docker)

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
Location will be: target/executable-streams-jmx-client.jar

## Create packaged binary and supporting files in .tar.gz
```
make tar
```
Location will be: target/streams-jmx-client-x.x.x-release.tar.gz


# Command line options
`java -jar target/executable-streams-jmx-client.jar --help`

<pre>
Usage: streams-jmx-client [options] [command] [command options]
  Options:
    -h, --help
      Display commands or specific commands arguments
    --jmxhttphost
      Host or IP used to replace jmx http large data set URL host fields.  Not usually needed. Use with caution.      Environment 
      Variable: STREAMS_CLIENT_JMX_HTTP_HOST
      Default: localhost
    --jmxhttpport
      Port used to replace jmx http large data set URL port fields.  Not usually needed. Use with caution.      Environment 
      Variable: STREAMS_CLIENT_JMX_HTTP_PORT
      Default: 30976
    --jmxssloption
      SSL Option for connection to Streams JMX Server (e.g. SSL_TLSv2, TSLv1.1, TLSv1.2)
      Environment Variable: 
      STREAMS_CLIENT_JMX_SSLOPTION 
      Default: TLSv1.1
    --jmxtruststore
      Java keystore of certificates/signers to trust from JMX Server
      Environment Variable: STREAMS_CLIENT_JMX_TRUSTSTORE
    -j, --jmxurl
      JMX Connection URL (e.g. service:jmx:jmxmp://localhost:9975)
      Environment Variable: STREAMS_CLIENT_JMXCONNECT
      Default: service:jmx:jmxmp://localhost:30975
    --logdir
      Logging direcotry.  If not set or empty log to stdout.
     Environment Variable: STREAMS_CLIENT_LOGDIR
      Default: <empty string>
    -l, --loglevel
      Logging level [ fatal | error | warn | info | debug | trace ]
      Environment Variable: STREAMS_CLIENT_LOGLEVEL
      Default: fatal
    --noconsole
      Flag to indicate not to prompt for password (can still redirect from stdin or use environment variable for password.
      Default: false
    --password
      Streams login password. Recommend using environment variable
      Environment Variable: STREAMS_CLIENT_PASSWORD
    -u, --user
      Streams login username. Use this or X509CERT
      Environment Variable: STREAMS_CLIENT_USERNAME
      Default: streamsadmin
    -v, --version
      Display version information
      Default: false
    -x, --x509cert
      X509 Certificate file to use instead of username/password
      Environment Variable: STREAMS_X509CERT
  Commands:
    help      Display commands or specific commands arguments
      Usage: help Command to get help information for

    version      Display version information
      Usage: version

    getinstancestate      Get the state of the Streams domain
      Usage: getinstancestate [options]
        Options:
          -d, --domain-id
            Streams domain name
            Environment Variable: STREAMS_DOMAIN_ID
            Default: awskube43
          -i, --instance-id
            Streams instance name.  Only used if Instance List not provided.
            Environment Variable: STREAMS_INSTANCE_ID
            Default: StreamsInstance

    getdomainstate      Get the state of the Streams domain
      Usage: getdomainstate [options]
        Options:
          -d, --domain-id
            Streams domain name
            Environment Variable: STREAMS_DOMAIN_ID
            Default: awskube43

    canceljob      Cancel Streams application
      Usage: canceljob [options] Job IDs to cancel
        Options:
          -d, --domain-id
            Streams domain name
            Environment Variable: STREAMS_DOMAIN_ID
            Default: awskube43
          --force
            Forces quick cancellation of job
            Default: false
          -i, --instance-id
            Streams instance name.  Only used if Instance List not provided.
            Environment Variable: STREAMS_INSTANCE_ID
            Default: StreamsInstance
          -j, --jobs
            A list of job ids delimited by commas

    lsjobs      Get a list of the jobs running in an instance
      Usage: lsjobs [options]
        Options:
          -d, --domain-id
            Streams domain name
            Environment Variable: STREAMS_DOMAIN_ID
            Default: awskube43
          -i, --instance-id
            Streams instance name.  Only used if Instance List not provided.
            Environment Variable: STREAMS_INSTANCE_ID
            Default: StreamsInstance

    submitjob      Submit Streams application to run in a Streams instance
      Usage: submitjob [options] Path to the .sab file
        Options:
          -P, --P
            Submission time parameter (may be used multiple times)
            Syntax: -Pkey=value
            Default: {}
          -C, --config
            Application configuration setting (may be used multiple times)
            Syntax: -Ckey=value
            Default: {}
          -d, --domain-id
            Streams domain name
            Environment Variable: STREAMS_DOMAIN_ID
            Default: awskube43
          -f, --file
            Path to the .sab file
          -i, --instance-id
            Streams instance name.  Only used if Instance List not provided.
            Environment Variable: STREAMS_INSTANCE_ID
            Default: StreamsInstance
          -g, --jobConfig
            Job configuration overlay file
          -J, --jobgroup
            Specifies the job group
          --jobname
            Specifies the name of the job
          --override
            Do not use resource load protection
            Default: false

    snapshotjobs      Submit Streams application to run in a Streams instance
      Usage: snapshotjobs [options]
        Options:
          -d, --domain-id
            Streams domain name
            Environment Variable: STREAMS_DOMAIN_ID
            Default: awskube43
          --includestatic
            Flag to include static attributes (default false)
            Default: false
          -i, --instance-id
            Streams instance name.  Only used if Instance List not provided.
            Environment Variable: STREAMS_INSTANCE_ID
            Default: StreamsInstance
        * -j, --jobs
            A list of job ids delimited by commas
          --maxdepth
            Maximum depth of operators to traverse (default 1)
            Default: 1

</pre>

# Running the application

There is a simple shell script to simplify running the application (streamsclient.sh)

The format of the command line tool is:
```./streamsclient.sh <jmx connection options> <command> <command specific options and arguments>```

Interactive mode is initated by running without a command:
```./streamsclient.sh <jmx connection options>```

Example:
```
java -jar target/executable-streams-jmx-client.jar \ 
-j service:jmx:jmxmp://localhost:9975  -u streamsadmin \
getdomainstate -d StreamsDomain 

password: <enter streamsadmin password>
```

## Using environment variables for the JMX connection options is the easiest way to run repeated commands

Every connection parameter has a corresponding envrionment variable.  There is a sample setenv.sh script that can be edited for your environment and sourced before running the commands.  This greatly simplifies the command line interace.


# Logging
Logging is performed through the log4j 1.2 facility. There are two arguments to control logging.

| argument | env | default | description |
|:---------|:----|:--------|:------------|
|--logdir|STREAM_EXPORTER_LOGDIR|undefined<br>(stdout)|If this argument is undefined log messages are sent to the **console** (stdout).<br>If this argument is present then a rolling logfile is created in the directory specified with the name of the logfile: **StreamsJmxClient.log**|
|--loglevel|STREAMS_EXPORTER_LOGLEVEL|info|fatal,error,warn,info,debug,trace<br>**note:** debug level contains timing messages|


## Adding to the default logging
If you wish to configure your own logging (in addition to that which the application already does), create a log4j.properties file and point to it using the log4j.configuration java property.  For example:
```
java -Dlog4j.configuration=file:${PWD}/log4j.properties -jar target/executable-streams-jmx-client.jar -j \
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
IBM Streams JMX API has several calls that return urls that are to be used by an HTTP Get request to pull back large data items.  Examples used in this application include: `submitjob command which calls deployApplication()` and `snapshotJobs()`.

The default configuration of IBM Streams uses a random port for the JMX Http Server.  To override this and set a specific port set the following IBM Streams properties:

| Streams Version | Property Type | Property |
|:----------------|:--------------|:---------|
| 4.2.1.3 & earlier | Instance | sam.jmxHttpPort |
| 4.2.4 & later | Domain | jmx.httpPort |

In cases where the Streams JMX Server is running inside of a container or environment with private IP addresses and a gateway, the URLs returned from these calls will use the host and port of the internal address of the JMX Http Port.  In addition, the default configuration of Streams will use a random port at startup for the JMX Http Server.

Using the `--jmxhttphost` and `--jmxhttpport` arguments or environment variables an be used to override the URL before the HTTP Get request is performed.

Logging at the `debug` level will provide messages showing the URLs before and after the override.

When the Streams domain is running inside of Kubernetes, and the Streams jmx client is running outside of the Kubernetes cluster, Kubernetes Service Objects are used to map external facing hostnames and ports to internal pod hosts (ip address) and ports.  The JMX HTTP Host and Port override arguments can be used to get around this issue.  Provide the Kubernetes external cluster hostname and the NodePort of the service as the arguments for Streams JMX Client.


# Running with Docker

Coming Soon.



# Commands
The set of commands available will increasing.  use the help command to list the available commands.

