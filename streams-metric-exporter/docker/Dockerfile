FROM ibmjava:sfj
EXPOSE 25500
COPY target/executable-streams-metric-exporter.jar .
ENTRYPOINT ["java","-jar","./executable-streams-metric-exporter.jar"]
