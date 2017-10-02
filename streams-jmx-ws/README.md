# streamsx-jmx-ws
This application caches IBM Streams metrics and status and presents a REST endpoint.  This application improves performance by periodically pulling all job metrics and caching them.  Users can use the REST endpoints to get metrics and status of specific jobs.  This layer reduces simplifies clients that need to monitor IBM Streams.
