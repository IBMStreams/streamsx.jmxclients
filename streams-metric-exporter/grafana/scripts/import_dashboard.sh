#!/bin/bash
curl 'http://admin:admin@localhost:3000/api/dashboards/db' -X POST -H 'Content-Type: application/json;charset=UTF-8' --header 'ContentType: application/json' -d @${1}
