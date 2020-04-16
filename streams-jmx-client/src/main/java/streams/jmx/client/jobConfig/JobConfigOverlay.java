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

import java.util.Map;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.internal.Console;
import com.beust.jcommander.internal.DefaultConsole;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
//import com.fasterxml.jackson.annotation.JsonGetter;
//import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ibm.streams.management.job.JobPropertiesMXBean.FusionScheme;

//import streams.jmx.client.cli.ServerProtocolValidator;
//import streams.jmx.client.cli.InstanceListConverter;
import streams.jmx.client.cli.LoglevelValidator;
//import streams.jmx.client.rest.Protocol;
import streams.jmx.client.cli.FileExistsValidator;
import streams.jmx.client.cli.DirectoryExistsValidator;
//import streams.jmx.client.cli.RefreshRateValidator;
//import streams.jmx.client.cli.ServerProtocolConverter;


// JobConfigOverlay
// Supports creating a default / simple JobConfigOverlay json file using the Config Parameters (-C) that 
// can be passed into submitjob
public class JobConfigOverlay {
	
    private static final Logger LOGGER = LoggerFactory.getLogger("root");

    private String dataDirectory;
    private FusionScheme fusionScheme;
    private String fusionTargetPeCount;
    private String parallelRegionConfig;
    private String placementScheme;
    private Boolean preloadApplicationBundles;
    private Boolean dynamicThreadingElastic;
    private int dynamicThreadingThreadCount;
    private String threadingModel;
    private String tracing;

    public JobConfigOverlay() {

    }

    // Create with configurationParameters from submitjob
    public JobConfigOverlay(Map<String,String> configSettings) throws IllegalArgumentException {
        // Loop through the map, setting any fields that we support
        // Additional fields in map will be ignored and a warning provided
        LOGGER.debug("Creating JobConfigOverlay from configSettings...");

        for (Map.Entry<String,String> entry : configSettings.entrySet()) {
            String key = entry.getKey();
            String valueString = entry.getValue();
            switch (entry.getKey()) {
                case "dataDirectory" :
                    this.setDataDirectory(valueString);
                    break;
                case "fusionScheme":
                    try {
                        this.setFusionScheme(FusionScheme.valueOf(valueString.toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Invalid value for fusionScheme (" + valueString + ")");
                    } catch (NullPointerException e) {}
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

    public String getParallelRegionConfig() {
        return this.parallelRegionConfig;
    }

    public void setParallelRegionConfig(String parallelRegionConfig) {
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

    public String getThreadingModel() {
        return this.threadingModel;
    }

    public void setThreadingModel(String threadingModel) {
        this.threadingModel = threadingModel;
    }

    public String getTracing() {
        return this.tracing;
    }

    public void setTracing(String tracing) {
        this.tracing = tracing;
    }


    private ObjectNode toObjectNode() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jobParameters = mapper.createObjectNode();

        // create IBM Streams JSON for config map overlay
        ArrayNode jobConfigOverlays = mapper.createArrayNode();
        ObjectNode jobConfigOverlay = mapper.createObjectNode();
        ObjectNode jobConfig = mapper.createObjectNode();
        ObjectNode deploymentConfig = mapper.createObjectNode();

        if (getDataDirectory() != null) jobConfig.put("dataDirectory",this.getDataDirectory());
        if (getFusionScheme() != null) deploymentConfig.put("fusionScheme",this.getFusionScheme().toString().toLowerCase());
        jobConfigOverlay.set("jobConfig",jobConfig);
        jobConfigOverlay.set("deploymentConfig",deploymentConfig);
        jobConfigOverlays.add(jobConfigOverlay);
        jobParameters.set("jobConfigOverlays",jobConfigOverlays);

        return(jobParameters);
    }

	@Override
    public String toString() {
        return(toObjectNode().toPrettyString());
    }
     
}
