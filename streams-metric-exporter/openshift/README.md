# Sample Run

oc new-project streams-test-dashboard

oc new-app -f streams-metric-dashboard.yaml \
   -p STREAMS_EXPORTER_JMXCONNECT=service:jmx:jmxmp://stream1-jmx.zen:9975 \
   -p STREAMS_INSTANCE_ID=stream1 \
   -p STREAMS_EXPORTER_USERNAME=streamsmetricuser \
   -p STREAMS_EXPORTER_PASSWORD=passw0rd
