FROM ibmjava:8-sfj
ARG APP_VERSION=5.0.0
ENV STREAMS_EXPORTER_PORT=25500
LABEL VERSION=${APP_VERSION} \
AUTHOR="Brian M. Williams" \
MAINTAINER="bmwilli@us.ibm.com" \
DESCRIPTION="IBM Streams Prometheus Metric Exporter for Streams v5.x" \
RUN="docker run [-it] --env-file <my-env-file> bmwilli1/streams-metric-exporter" \
source="https://github.com/IBMStreams/streamsx.jmxclients"
EXPOSE ${STREAMS_EXPORTER_PORT}
COPY target/executable-streams-metric-exporter.jar .
RUN groupadd -g 1000 metricsuser && \
  useradd -u 1000 -g 1000 metricsuser && \
  chown metricsuser:metricsuser ./executable-streams-metric-exporter.jar
USER metricsuser
HEALTHCHECK --interval=10s --timeout=5s --retries=2 \
  CMD wget -q -O /dev/null -t 1 -T 5 http://localhost:${STREAMS_EXPORTER_PORT}/version || exit 1
ENTRYPOINT ["java","-jar","./executable-streams-metric-exporter.jar"]
