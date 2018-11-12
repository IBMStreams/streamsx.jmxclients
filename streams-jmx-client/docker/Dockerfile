FROM ibmjava:sfj-alpine
ARG APP_VERSION=1.0.0
LABEL VERSION=${APP_VERSION} \
AUTHOR="Brian M. Williams" \
MAINTAINER="bmwilli@us.ibm.com" \
DESCRIPTION="IBM Streams JMX Command Line Client" \
RUN="docker run [-it] --env-file <my-env-file> bmwilli1/streams-jmx-client [command]" \
source="https://github.com/IBMStreams/streamsx.jmxclients" 
COPY target/executable-streams-jmx-client.jar .
ENTRYPOINT ["java","-jar","./executable-streams-jmx-client.jar"]
