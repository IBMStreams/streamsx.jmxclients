FROM ibmjava:sfj-alpine
ARG APP_VERSION=4.1.0
LABEL VERSION=${APP_VERSION} \
AUTHOR="Brian M. Williams" \
MAINTAINER="bmwilli@us.ibm.com" \
DESCRIPTION="IBM Streams Prometheus Metric Exporter for Streams v4.x with Domains" \
RUN="docker run [-it] --env-file <my-env-file> bmwilli1/streams-metric-exporter" \
source="https://github.com/IBMStreams/streamsx.jmxclients"
EXPOSE 25500
COPY target/executable-streams-metric-exporter.jar .
ENTRYPOINT ["java","-jar","./executable-streams-metric-exporter.jar"]
