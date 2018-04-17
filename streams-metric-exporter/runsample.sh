#!/bin/bash
# Sample execution
# If running inside of Streams Quickstart VM, use as is
# If running on host (mac,pc) with Quickstart VM running, run the following command in the vmware image to get the 
#    <quickstart vm ip>
#    ifconfig eth0 
#    Replace the localhost with the ip address returned
JMXHOST=localhost
java -jar target/executable-streams-metric-exporter.jar -j service:jmx:jmxmp://${JMXHOST}:9975 -d StreamsDomain -i StreamsInstance -u streamsadmin -r 10 -p 25500 -h 0.0.0.0
