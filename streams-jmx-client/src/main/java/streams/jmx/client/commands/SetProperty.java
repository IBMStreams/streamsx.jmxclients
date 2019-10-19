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
import streams.jmx.client.cli.KeyValueConverter;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.ibm.streams.management.instance.InstanceMXBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Parameters(commandDescription = Constants.DESC_SETPROPERTY)
public class SetProperty extends AbstractInstanceCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger("root."
    + SetProperty.class.getName());

    // Bug in version 1.72 prevents convert working here
   
    @Parameter(description = "property-name=property-value ...", required=false)
    private List<String> instancePropertiesString;

    @Parameter(names = {"--application-ev"}, description = "Specifies that the property is an application environment variable.", required = false)
    private boolean applicationEv = false;

    //@Parameter(description = "property-name=property-value ...", required=false,
    //   converter = KeyValueConverter.class)
    private List<AbstractMap.SimpleImmutableEntry<String,String>> instanceProperties = null;

    public SetProperty() {
    }

    @Override
    public String getName() {
        return (Constants.CMD_SETPROPERTY);
    }

    @Override
    public String getHelp() {
        return (Constants.DESC_SETPROPERTY);
    }

    @Override
    protected CommandResult doExecute() {
        try {

           LOGGER.debug("instancePropertiesString.size(): {}",(instancePropertiesString == null)?"null":instancePropertiesString.size());
        

            // domain-property or all is required
            if ((instancePropertiesString == null) || (instancePropertiesString.size() == 0)) {
                throw new ParameterException("The following argument is required: property-name=property-value ...");
            }

            KeyValueConverter keyValueConverter = new KeyValueConverter();

            instanceProperties = new ArrayList<AbstractMap.SimpleImmutableEntry<String,String>>();
            for (String instancePropertyString : instancePropertiesString) {
                instanceProperties.add(keyValueConverter.convert(instancePropertyString));
            }

            ObjectMapper mapper = new ObjectMapper();
            ObjectNode jsonOut = mapper.createObjectNode();

            InstanceMXBean instance = getInstanceMXBean();

            // Populate the result object
            ArrayNode propertyArray = mapper.createArrayNode();
            jsonOut.put("instance",instance.getName());


            if (applicationEv) {
                // Get existing application environment variables
                Map<String,String> applicationEvMap = instance.getApplicationEnvironmentVariables();

                for (AbstractMap.SimpleImmutableEntry<String, String> keyValuePair : instanceProperties) {
                    ObjectNode propertyObject = mapper.createObjectNode();
                    propertyObject.put("property",keyValuePair.getKey());
                    propertyObject.put("newvalue",keyValuePair.getValue());
                    // Check to see if it already exists
                    if (applicationEvMap.containsKey(keyValuePair.getKey().toString())) {
                        propertyObject.put("previousvalue",applicationEvMap.get(keyValuePair.getKey()));
                        LOGGER.debug("About to change application environment variable {} = {}",keyValuePair.getKey(), keyValuePair.getValue());
                        instance.changeApplicationEnvironmentVariable(keyValuePair.getKey(), keyValuePair.getValue());
                    } else {
                        propertyObject.put("previousvalue","");
                        LOGGER.debug("About to add application environment variable {} = {}",keyValuePair.getKey(), keyValuePair.getValue());
                        instance.addApplicationEnvironmentVariable(keyValuePair.getKey(), keyValuePair.getValue());
                    }
                    propertyArray.add(propertyObject);
                }
            } else {
                for (AbstractMap.SimpleImmutableEntry<String, String> keyValuePair : instanceProperties) {
                    LOGGER.debug("About to set property {} = {}",keyValuePair.getKey(), keyValuePair.getValue());
                    ObjectNode propertyObject = mapper.createObjectNode();
                    InstanceMXBean.PropertyId propertyId = null;
                    try {
                        propertyId = InstanceMXBean.PropertyId.fromString(keyValuePair.getKey());
                    } catch (IllegalArgumentException e) {
                        LOGGER.warn("The following property is not valid: {}",keyValuePair.getKey());
                    }
                    try {
                        if (propertyId != null) {
                            String oldValue = instance.getProperty(propertyId);
                            instance.setProperty(InstanceMXBean.PropertyId.fromString(keyValuePair.getKey()),keyValuePair.getValue());
                            propertyObject.put("property",propertyId.toString());
                            propertyObject.put("newvalue",keyValuePair.getValue());
                            propertyObject.put("previousvalue",oldValue);
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
