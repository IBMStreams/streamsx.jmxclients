FROM ibmjava:sfj-alpine
EXPOSE 25500
COPY target/executable-streams-jmx-client.jar .
ENTRYPOINT ["java","-jar","./executable-streams-jmx-client.jar"]
