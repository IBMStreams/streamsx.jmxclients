#!/bin/bash
curl 'http://admin:admin@localhost:3000/api/datasources' -X POST -H 'Content-Type: application/json;charset=UTF-8' --data-binary '{"Name":"Prometheus","Type":"prometheus","URL":"http://prometheus:9090","Access":"proxy","isDefault":true}' --header "ContentType: application/json"
