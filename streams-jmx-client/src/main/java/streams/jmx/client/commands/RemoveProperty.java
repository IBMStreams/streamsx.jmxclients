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

package streams.jmx.client.commands;

import streams.jmx.client.Constants;
import streams.jmx.client.ExitStatus;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;

import java.util.List;
import java.util.Map;

import com.ibm.streams.management.domain.DomainMXBean;
import com.ibm.streams.management.instance.InstanceMXBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Parameters(commandDescription = Constants.DESC_REMOVEPROPERTY)
public class RemoveProperty extends AbstractInstanceCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger("root."
    + RemoveProperty.class.getName());

    @Parameter(description = "property-name ...", required=false)
    private List<String> instanceProperties = null;

    @Parameter(names = {"--application-ev"}, description = "Specifies that the property is an application environment variable.", required = false)
    private boolean applicationEv = false;

    public RemoveProperty() {
    }

    @Override
    public String getName() {
        return (Constants.CMD_REMOVEPROPERTY);
    }

    @Override
    public String getHelp() {
        return (Constants.DESC_REMOVEPROPERTY);
    }

    @Override
    protected CommandResult doExecute() {
        try {

           LOGGER.debug("instanceProperties.size(): {}",(instanceProperties == null)?"null":instanceProperties.size());
        

            // domain-property 
            if ((instanceProperties == null) || (instanceProperties.size() == 0)) {
                throw new ParameterException("The following argument is required: property-name=property-value ...");
            }

            ObjectMapper mapper = new ObjectMapper();
            ObjectNode jsonOut = mapper.createObjectNode();

            DomainMXBean domain = getDomainMXBean();
            InstanceMXBean instance = getInstanceMXBean();

            // Populate the result object
            ArrayNode propertyArray = mapper.createArrayNode();
            jsonOut.put("domain",domain.getName());
            jsonOut.put("instance",instance.getName());


            if (applicationEv) {
                // Get existing application environment variables
                Map<String,String> applicationEvMap = instance.getApplicationEnvironmentVariables();

                for (String propertyName : instanceProperties) {
                    ObjectNode propertyObject = mapper.createObjectNode();
                    propertyObject.put("property",propertyName);
                    // Check to see if it already exists
                    if (applicationEvMap.containsKey(propertyName)) {
                        LOGGER.debug("About to remove application environment variable {}",propertyName);
                        instance.removeApplicationEnvironmentVariable(propertyName);
                    } else {
                        LOGGER.debug("Application environment variable {} does not exist.",propertyName);
                    }
                    propertyArray.add(propertyObject);
                }
            } else {
                for (String propertyName : instanceProperties) {
                    LOGGER.debug("About to remove property {}",propertyName);
                    ObjectNode propertyObject = mapper.createObjectNode();
                    InstanceMXBean.PropertyId propertyId = null;
                    try {
                        propertyId = InstanceMXBean.PropertyId.fromString(propertyName);
                    } catch (IllegalArgumentException e) {
                        LOGGER.warn("The following property is not valid: {}",propertyName);
                    }
                    try {
                        if (propertyId != null) {
                            instance.removeProperty(propertyId);
                            propertyObject.put("property",propertyId.toString());
                            propertyArray.add(propertyObject);
                        }
                    } catch (IllegalStateException e) {
                        LOGGER.warn(e.getLocalizedMessage());
                    }
                }
            }
            jsonOut.set("properties",propertyArray);
            jsonOut.put("count",propertyArray.size());

            return new CommandResult(jsonOut.toString());
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("SetProperty caught Exception: {}", e.toString());
                e.printStackTrace();
            }
            return new CommandResult(ExitStatus.FAILED_COMMAND, null, e.getLocalizedMessage());
        }
    }
}
