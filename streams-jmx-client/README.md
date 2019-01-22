# streams-jmx-client

This application provides a command-line interface to the IBM Streams - Stream Processing System.  This program functions similar to the **streamtool** command that is included with IBM Streams, however, this application uses a pure JMX interface and can be run on any java platform and does **not** require the IBM Streams installation on the same host.

Reasons for using this application include

* Interacting with IBM Streams from a lightweight container (e.g. Docker container)
* Interacting with IBM Streams running inside of a continer hosting environment (e.g. Kubernetes)

The application supports an interactive mode (similar to streamtool interactive mode)

The initial set of commands focus on interacting with Streams in a container hosting environment.

Requests for additional commands are welcome and contributions are encouraged.

## Commands

| Command | Description |
|:--------|:------------|
|canceljob|Cancel Streams application
|getdomainproperty|Get values of domain properties
|getdomainstate|Get the state of the Streams domain
|getinstancestate|Get the state of the Streams instance
|getproperty|Get values of instance properties
|gettag|Display resource definition for a tag
|help|Display commands or specific commands arguments
|lsjobs|Get a list of the jobs running in an instance
|lspes|Report health summary for each PE
|lstag|List tags defined for a domain
|mktag|Create a resource tag in a domain with description and resource definition
|rmproperty|Remove an instance property or application variable
|rmtag|Remove a resource tag from a domain
|setdomainproperty|Set one or more property values for a domain
|setproperty|Set one or more property values for an instance
|snapshotjobs|Capture a snapshot of jobs running in an instance
|submitjob|Submit Streams application to run in a Streams instance
|version|Display version information

## Questions

For questions and issues, please contact:

Brian M Williams, IBM<br>
bmwilli@us.ibm.com

## Contents

1. [Building streams-jmx-client](#building-the-application)
2. [Command line options](#command-line-options)
3. [Running streams-jmx-client](#running-the-application)
4. [Logging](#logging)
5. [Redirecting JMX HTTP URLs](#redirecting-jmx-http-urls)
6. [Running with Docker](#running-with-docker)

## Building the application

### Install Dependencies

This application does NOT need to be compiled on a system with IBM Streams installed.  The lib folder contains two redistributable .jar files from IBM Streams that must be installed into your local maven repository.

There is a Makefile included that will do this installation.

```bash
make setup
```

### Compiling the application

```bash
mvn compile
```

### Create executable .jar with dependencies included

```bash
mvn package
```

or

```bash
make package
```

Location will be: target/executable-streams-jmx-client.jar

### Create packaged binary and supporting files in .tar.gz

```bash
make tar
```

Location will be: target/streams-jmx-client-x.x.x-release.tar.gz

## Command line options

`java -jar target/executable-streams-jmx-client.jar --help`

<pre>
Usage: streams-jmx-client [options] Command
  Options:
    -h, --help
      Display commands or specific commands arguments
    -v, --version
      Display version information
      Default: false
    -j, --jmxurl
      JMX Connection URL (e.g. service:jmx:jmxmp://localhost:9975)
      Environment Variable: STREAMS_CLIENT_JMXCONNECT
    --jmxssloption
      SSL Option for connection to Streams JMX Server (e.g. SSL_TLSv2, TSLv1.1, TLSv1.2)
      Environment Variable:
      STREAMS_CLIENT_JMX_SSLOPTION
      Default: TLSv1
    -u, --user
      Streams login username. Use this or X509CERT
      Environment Variable: STREAMS_CLIENT_USERNAME
    --password
      Streams login password. Recommend using environment variable
      Environment Variable: STREAMS_CLIENT_PASSWORD
    -x, --x509cert
      X509 Certificate file to use instead of username/password
      Environment Variable: STREAMS_X509CERT
    --jmxtruststore
      Java keystore of certificates/signers to trust from JMX Server
      Environment Variable: STREAMS_CLIENT_JMX_TRUSTSTORE
    --jmxhttphost
      Host or IP used to replace jmx http large data set URL host fields.  Not usually needed. Use with caution.
      Environment
      Variable: STREAMS_CLIENT_JMX_HTTP_HOST
    --jmxhttpport
      Port used to replace jmx http large data set URL port fields.  Not usually needed. Use with caution.
      Environment
      Variable: STREAMS_CLIENT_JMX_HTTP_PORT
    -l, --loglevel
      Logging level [ fatal | error | warn | info | debug | trace ]
      Environment Variable: STREAMS_CLIENT_LOGLEVEL
      Default: warn
    --logdir
      Logging direcotry.  If not set or empty log to stdout.
      Environment Variable: STREAMS_CLIENT_LOGDIR
      Default: empty
    --noconsole
      Flag to indicate not to prompt for password (can still redirect from stdin or use environment variable for password.
      Default: false

Client Commands:

Command               Description
--------------------  -----------
canceljob             Cancel Streams application
getdomainproperty     Get values of domain properties
getdomainstate        Get the state of the Streams domain
getinstancestate      Get the state of the Streams instance
getproperty           Get values of instance properties
gettag                Display resource definition for a tag
help                  Display commands or specific commands arguments
lsjobs                Get a list of the jobs running in an instance
lspes                 Report health summary for each PE
lstag                 List tags defined for a domain
mktag                 Create a resource tag in a domain with description and resource definition
rmproperty            Remove an instance property or application ev
rmtag                 Remove a resource tag from a domain
setdomainproperty     Set one or more property values for a domain
setproperty           Set one or more property values for an instance
snapshotjobs          Capture a snapshot of jobs running in an instance
submitjob             Submit Streams application to run in a Streams instance
version               Display version information
</pre>

## Running the application

There is a simple shell script to simplify running the application (streamsclient.sh)

The format of the command line tool is:

```./streamsclient.sh <jmx connection options> <command> <command specific options and arguments>```

Interactive mode is initated by running without a command:

```./streamsclient.sh <jmx connection options>```

Example:

```bash
java -jar target/executable-streams-jmx-client.jar \ 
-j service:jmx:jmxmp://localhost:9975  -u streamsadmin \
getdomainstate -d StreamsDomain 

password: <enter streamsadmin password>
```

### Using environment variables for the JMX connection options is the easiest way to run repeated commands

Every connection parameter has a corresponding envrionment variable.  There is a sample setenv.sh script that can be edited for your environment and sourced before running the commands.  This greatly simplifies the command line interace.

## Logging

Logging is performed through the log4j 1.2 facility. There are two arguments to control logging.

| argument | env | default | description |
|:---------|:----|:--------|:------------|
|--logdir|STREAM_EXPORTER_LOGDIR|undefined<br>(stdout)|If this argument is undefined log messages are sent to the **console** (stdout).<br>If this argument is present then a rolling logfile is created in the directory specified with the name of the logfile: **StreamsJmxClient.log**|
|--loglevel|STREAMS_EXPORTER_LOGLEVEL|info|fatal,error,warn,info,debug,trace<br>**note:** debug level contains timing messages|

### Adding to the default logging

If you wish to configure your own logging (in addition to that which the application already does), create a log4j.properties file and point to it using the log4j.configuration java property.  For example:

```bash
java -Dlog4j.configuration=file:${PWD}/log4j.properties -jar target/executable-streams-jmx-client.jar -j \
service:jmx:jmxmp://localhost:9975 -d StreamsDomain -i \
StreamsInstance -u streamsadmin
```

This would be useful in situations when you want to log to both the console and a file.<br>
**Note:** The log level will still be set by the command line argument or environment variable, NOT the rootlogger value in your log4j.properties.

## Redirecting JMX HTTP URLs

There are some configurations where you will need to override the URLs returned for large data sets to be pulled from IBM Streams over Http.
<br>
There are two arguments that can be used to overwrite the JMX Http URL that is used for large data objects.

| argument | env | default | description |
|:---------|:----|:--------|:------------|
|--jmxhttphost|STREAM_EXPORTER_JMX_HTTP_HOST|undefined|Host name or IP Address<br>If this argument is specified the host field of the JMX Http URL will be replaced with this value|
|--jmxhttpport|STREAMS_EXPORTER_JMX_HTTP_PORT|undefined|Port number<br>If this argument is specified the port field of the JMX Http URL will be replaced with this value|

### JMX HTTP Host and Port redirection examples and Kubernetes

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

## Running with Docker

The latest docker container is published to Docker Hub: https://hub.docker.com/r/bmwilli1/streams-jmx-client/

1. Create .env file:

The env file simplifies connectivity. IT is a file of key=value pairs.  See the command usage options for the environment variables used to set each option.

The following example was used to connect to Streams 4.3 QuickStart for Docker running in Docker for Mac.  The domain property jmx.httpPort was set to 8006 (A port exposed by default in the Streams 4.3 Docker instructions).
<pre>
STREAMS_CLIENT_JMXCONNECT=service:jmx:jmxmp://172.17.0.2:9975
STREAMS_CLIENT_USERNAME=streamsadmin
STREAMS_CLIENT_PASSWORD=passw0rd
STREAMS_CLIENT_JMX_SSLOPTION=TLSv1.2
STREAMS_DOMAIN_ID=StreamsDomain
STREAMS_INSTANCE_ID=StreamsInstance
</pre>

A complete documented sample one can be found here: https://github.com/IBMStreams/streamsx.jmxclients/blob/master/streams-jmx-client/docker/sample.env

2. Get the domain state

```bash
docker run --rm -it --env-file mydocker.env bmwilli1/streams-jmx-client:1.0.0 getdomainstate
```

3. Submit a job (requires mounting the .sab file into the container)

```bash
docker run --rm -it --env-file mydocker.env -v ${PWD}/HelloWorld.sab:/opt/HelloWorld.sab bmwilli1/streams-jmx-client submitjob /opt/HelloWorld.sab
```

4. Run interactive shell

```bash
docker run --rm -it --env-file mydocker.env bmwilli1/streams-jmx-client:1.0.0
Streams JMX Client INTERACTIVE MODE STARTED
streamsclient>
```