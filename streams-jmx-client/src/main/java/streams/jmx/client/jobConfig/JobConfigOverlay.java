// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package streams.jmx.client.jobConfig;

//import java.util.HashSet;
//import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ibm.streams.management.job.JobPropertiesMXBean.FusionScheme;
import com.ibm.streams.management.job.JobPropertiesMXBean.ThreadingModel;
import com.ibm.streams.management.instance.TracingPropertiesMXBean.Level;

// JobConfigOverlay
// Supports creating a default / simple JobConfigOverlay json file using the Config Parameters (-C) that 
// can be passed into submitjob
public class JobConfigOverlay {
	
    private static final Logger LOGGER = LoggerFactory.getLogger("root");

    private String dataDirectory;
    private FusionScheme fusionScheme;
    private String fusionTargetPeCount;
    private ParallelRegionConfig parallelRegionConfig;
    private String placementScheme;
    private Boolean preloadApplicationBundles;
    private Boolean dynamicThreadingElastic;
    private int dynamicThreadingThreadCount;
    private ThreadingModel threadingModel;
    private Level tracing;

    // JobConfig Jackson Tree
    private JsonNode jobConfigNode = null;

    private ObjectMapper mapper = new ObjectMapper();

    public JobConfigOverlay() {

    }

    // Create with configurationParameters from submitjob
    public JobConfigOverlay(Map<String,String> configSettings) throws IllegalArgumentException {
        // Loop through the map, setting any fields that we support
        // Additional fields in map will be ignored and a warning provided
        LOGGER.debug("Creating JobConfigOverlay from configSettings...");

        // Create empty json for our jobconfig
        jobConfigNode = mapper.createObjectNode();

        // Create the new jobconfig
        updateWithConfigSettings(configSettings);
    }

    // Create with a given configuration file and the configuration parameters
    public JobConfigOverlay(File jobConfigFile, Map<String,String> configSettings) throws IllegalArgumentException {
        LOGGER.debug("Creating JobConfigOverlay from jobConfigFile and configSettings...");
        
        try {
            // Read File into JSON
            LOGGER.debug("Reading job config overlay file: {}",jobConfigFile.toString());
            jobConfigNode = new ObjectMapper().readTree(jobConfigFile);


        } catch (JsonProcessingException e) {
            LOGGER.debug("Caught JsonProcessingException: {}", e.getLocalizedMessage());
            throw new IllegalArgumentException("Error Parsing job config overlay file (" + jobConfigFile.toString() + ")");
        } catch (IOException e) {
            LOGGER.debug("Caught IOException: {}", e.getLocalizedMessage());
            throw new IllegalArgumentException("Error Reading job config overlay file (" + jobConfigFile.toString() + ")");
        }

        // We need to update based on configSettings
        updateWithConfigSettings(configSettings);

    }


    private void updateWithConfigSettings(Map<String,String> configSettings) throws IllegalArgumentException {
        // Loop through the map, setting any fields that we support
        // Additional fields in map will be ignored and a warning provided
        LOGGER.debug("parsing configSettings to update JobConfigJsonNode ...");

        for (Map.Entry<String,String> entry : configSettings.entrySet()) {
            String key = entry.getKey();
            String valueString = entry.getValue();
            switch (entry.getKey()) {
                case "dataDirectory" :
                    this.setDataDirectory(valueString);
                    if (this.getDataDirectory() != null) {
                        JsonNode jobConfig = getCreateJobConfig();
                        //((ObjectNode) jobConfigNode.get("jobConfigOverlays").get(0).get("jobConfig")).put("dataDirectory",this.getDataDirectory());
                        ((ObjectNode) jobConfig).put("dataDirectory",this.getDataDirectory());
                    }
                    break;
                case "fusionScheme":
                    try {
                        this.setFusionScheme(FusionScheme.valueOf(valueString.toUpperCase()));
                        if (getFusionScheme() != null) {
                            JsonNode deploymentConfig = getCreateDeploymentConfig();
                            ((ObjectNode) deploymentConfig).put("fusionScheme",this.getFusionScheme().toString().toLowerCase());
                        }
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Invalid value for fusionScheme (" + valueString + ")");
                    } catch (NullPointerException e) {
                        throw new IllegalArgumentException("Invalid null value for fusionScheme (" + valueString + ")");
                    }
                    break;
                case "parallelRegionConfig":
                    try {
                        this.setParallelRegionConfig(ParallelRegionConfig.valueOf(valueString.toUpperCase()));
                        if (getParallelRegionConfig() != null) {
                            JsonNode parallelRegionConfig = getCreateParallelRegionConfig();
                            ((ObjectNode) parallelRegionConfig).put("fusionType",this.getParallelRegionConfig().toString());
                        }
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Invalid value for parallelRegionConfig (" + valueString + ")");
                    } catch (NullPointerException e) {
                        throw new IllegalArgumentException("Invalid null value for parallelRegionConfig (" + valueString + ")");
                    }                
                    break;
                case "preloadApplicationBundles": {
                        this.setPreloadApplicationBundles(Boolean.parseBoolean(valueString));
                        JsonNode jobConfig = getCreateJobConfig();
                        ((ObjectNode) jobConfig).put("preloadApplicationBundles",this.isPreloadApplicationBundles());
                    }
                    break;  
                case "dynamicThreadingElastic": {
                        this.setDynamicThreadingElastic(Boolean.parseBoolean(valueString));
                        JsonNode  deploymentConfig = getCreateDeploymentConfig();
                        ((ObjectNode) deploymentConfig).put("dynamicThreadingElastic",this.isDynamicThreadingElastic());
                    }
                    break;                                      
                case "dynamicThreadingThreadCount" :
                    try {
                        this.setDynamicThreadingThreadCount(Integer.parseInt(valueString));
                        JsonNode deploymentConfig = getCreateDeploymentConfig();
                        ((ObjectNode) deploymentConfig).put("dynamicThreadingThreadCount",this.getDynamicThreadingThreadCount());
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException("Invalid integer value for dynamicThreadingCount (" + valueString + ")");
                        }
                    break;
                case "threadingModel":
                    try {
                        this.setThreadingModel(ThreadingModel.valueOf(valueString.toUpperCase()));
                        if (getThreadingModel() != null) {
                            JsonNode deploymentConfig = getCreateDeploymentConfig();
                            ((ObjectNode) deploymentConfig).put("threadingModel",this.getThreadingModel().toString().toLowerCase());
                        }
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Invalid value for threadingModel (" + valueString + ")");
                    } catch (NullPointerException e) {
                        throw new IllegalArgumentException("Invalid null value for threadingModel (" + valueString + ")");
                    }
                    break;
                case "tracing":
                    try {
                        this.setTracing(Level.valueOf(valueString.toUpperCase()));
                        if (getTracing() != null) {
                            JsonNode jobConfig = getCreateJobConfig();
                            ((ObjectNode) jobConfig).put("tracing",this.getTracing().toString().toLowerCase());
                        }
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Invalid value for tracing (" + valueString + ")");
                    } catch (NullPointerException e) {
                        throw new IllegalArgumentException("Invalid null value for tracing (" + valueString + ")");
                    }
                    break;
                default:
                    LOGGER.warn("Unrecognized configuration parameter specified ({}), ignoring it",key);
            }
        }
    }

    public String getDataDirectory() {
        return this.dataDirectory;
    }

    public void setDataDirectory(String dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    public FusionScheme getFusionScheme() {
        return this.fusionScheme;
    }

    public void setFusionScheme(FusionScheme fusionScheme) {
        this.fusionScheme = fusionScheme;
    }

    public String getFusionTargetPeCount() {
        return this.fusionTargetPeCount;
    }

    public void setFusionTargetPeCount(String fusionTargetPeCount) {
        this.fusionTargetPeCount = fusionTargetPeCount;
    }

    public ParallelRegionConfig getParallelRegionConfig() {
        return this.parallelRegionConfig;
    }

    public void setParallelRegionConfig(ParallelRegionConfig parallelRegionConfig) {
        this.parallelRegionConfig = parallelRegionConfig;
    }

    public String getPlacementScheme() {
        return this.placementScheme;
    }

    public void setPlacementScheme(String placementScheme) {
        this.placementScheme = placementScheme;
    }

    public Boolean isPreloadApplicationBundles() {
        return this.preloadApplicationBundles;
    }

    public Boolean getPreloadApplicationBundles() {
        return this.preloadApplicationBundles;
    }

    public void setPreloadApplicationBundles(Boolean preloadApplicationBundles) {
        this.preloadApplicationBundles = preloadApplicationBundles;
    }

    public Boolean isDynamicThreadingElastic() {
        return this.dynamicThreadingElastic;
    }

    public Boolean getDynamicThreadingElastic() {
        return this.dynamicThreadingElastic;
    }

    public void setDynamicThreadingElastic(Boolean dynamicThreadingElastic) {
        this.dynamicThreadingElastic = dynamicThreadingElastic;
    }

    public int getDynamicThreadingThreadCount() {
        return this.dynamicThreadingThreadCount;
    }

    public void setDynamicThreadingThreadCount(int dynamicThreadingThreadCount) {
        this.dynamicThreadingThreadCount = dynamicThreadingThreadCount;
    }

    public ThreadingModel getThreadingModel() {
        return this.threadingModel;
    }

    public void setThreadingModel(ThreadingModel threadingModel) {
        this.threadingModel = threadingModel;
    }

    public Level getTracing() {
        return this.tracing;
    }

    public void setTracing(Level tracing) {
        this.tracing = tracing;
    }

    public JsonNode getJobConfigNode() {
        return this.jobConfigNode;
    }

    // Get the jobConfigOverlay (assuming only 1)
    private JsonNode getCreateJobConfigOverlay() {
        JsonNode jobConfigOverlays = this.jobConfigNode.get("jobConfigOverlays");
        if (jobConfigOverlays == null || jobConfigOverlays.isNull()) {
            ArrayNode jobConfigOverlaysArray = mapper.createArrayNode();
            ((ObjectNode)this.jobConfigNode).set("jobConfigOverlays",jobConfigOverlaysArray);
        }

        JsonNode jobConfigOverlay = this.jobConfigNode.get("jobConfigOverlays").get(0);
        if (jobConfigOverlay == null || jobConfigOverlay.isNull()) {
            ObjectNode jobConfigOverlayObject = mapper.createObjectNode();
            ((ArrayNode)this.jobConfigNode.get("jobConfigOverlays")).add(jobConfigOverlayObject);
        }        

        return this.jobConfigNode.get("jobConfigOverlays").get(0);
    }

    private JsonNode getCreateJobConfig() {
        JsonNode jco = getCreateJobConfigOverlay();
        JsonNode jobConfig = jco.get("jobConfig");
        if (jobConfig == null || jobConfig.isNull()) {
            ObjectNode jobConfigObject = mapper.createObjectNode();
            ((ObjectNode)jco).set("jobConfig",jobConfigObject);
        }
        return jco.get("jobConfig");
    }


    private JsonNode getCreateDeploymentConfig() {
        JsonNode jco = getCreateJobConfigOverlay();
        JsonNode deploymentConfig = jco.get("deploymentConfig");
        if (deploymentConfig == null || deploymentConfig.isNull()) {
            ObjectNode deploymentConfigObject = mapper.createObjectNode();
            ((ObjectNode)jco).set("deploymentConfig",deploymentConfigObject);
        }
        return jco.get("deploymentConfig");
    }

    private JsonNode getCreateParallelRegionConfig() {
        JsonNode dc = getCreateDeploymentConfig();
        JsonNode parallelRegionConfig = dc.get("parallelRegionConfig");
        if (parallelRegionConfig == null || parallelRegionConfig.isNull()) {
            ObjectNode parallelRegionConfigObject = mapper.createObjectNode();
            ((ObjectNode)dc).set("parallelRegionConfig",parallelRegionConfigObject);
        }
        return dc.get("parallelRegionConfig");
    }

	@Override
    public String toString() {
        return(getJobConfigNode().toPrettyString());
    }
     
}
