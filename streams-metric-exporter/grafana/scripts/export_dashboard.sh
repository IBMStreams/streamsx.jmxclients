#!/bin/bash
# Export a dashboard from grafana using the REST api
# Names is lowercase with hyphens for spaces replacement
# e.g. This is my Test Dashboard = this-is-my-test-dashboard
#curl 'http://admin:admin@localhost:3000/api/dashboards/db/ibm-streams-sample-dashboard' -X GET -H 'Content-Type: application/json;charset=UTF-8'n
curl "http://admin:admin@localhost:3000/api/dashboards/db/${1}" -X GET -H 'Content-Type: application/json;charset=UTF-8'n
