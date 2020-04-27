FROM ibmjava:8-sfj
ARG APP_VERSION=5.0.0
LABEL VERSION=${APP_VERSION} \
AUTHOR="Brian M. Williams" \
MAINTAINER="bmwilli@us.ibm.com" \
DESCRIPTION="IBM Streams Prometheus Metric Exporter for Streams v5.x" \
RUN="docker run [-it] --env-file <my-env-file> bmwilli1/streams-metric-exporter" \
source="https://github.com/IBMStreams/streamsx.jmxclients"
EXPOSE 25500
COPY target/executable-streams-metric-exporter.jar .
RUN groupadd -g 1000 metricsuser && \
  useradd -u 1000 -g 1000 metricsuser && \
  chown metricsuser:metricsuser ./executable-streams-metric-exporter.jar
USER metricsuser
ENTRYPOINT ["java","-jar","./executable-streams-metric-exporter.jar"]
