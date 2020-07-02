# Deploy Streams Metric Dashboard in Cloud Pak 4 Data 3.x

## Create a new OpenShift Project
oc new-project streams-test-dashboard

## Create new application with default admin login to IBM Streams (admin/password)
```
oc new-app -f streams-metric-dashboard.yaml \
   -p STREAMS_INSTANCE_ID=stream1 
```

## Create new application specifying the user account to login to IBM Streams
```
oc new-app -f streams-metric-dashboard.yaml \
   -p STREAMS_EXPORTER_USERNAME=admin \
   -p STREAMS_EXPORTER_PASSWORD=password
```

## Provision dashboards as configmap (These cannot be edited via grafana gui)
```
oc create configmap dashboards-grafana --from-file ../dashboards
```

## Login to grafana
http://grafan-streams-test-dashboard.apps.<cluster_domain_name><br>
username: admin<br>
password: password<br>
NOTE: These can be configured as parameters to the new-app command above<br>
  -p ADMIN_USERNAME_GRAFANA=<username><br>
  -p ADMIN_PASSWORD_GRAFANA=<password><br>


## Provision dashboards through import
See Grafana documentation to import dashboard yaml file from ../dashboards

Done through import allows these dashboards to be edited via Grafana

## Modify application
```
oc process -f streams-metric-dashboard.yaml [-p PARAMETER=value ...] | oc apply -f -
```


## Delete app (Not PVC's)
```
oc delete all -l app=streams-metric-dashboard
```

## Delete pvc's
```
oc delete pvc pvc-grafana
oc delete pvc pvc-prometheus
```

## Delete Project
```
oc delete project streams-test-dashboard
```
