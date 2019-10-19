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

import java.lang.reflect.UndeclaredThrowableException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.ibm.streams.management.instance.InstanceMXBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Parameters(commandDescription = Constants.DESC_GETPROPERTY)
public class GetProperty extends AbstractInstanceCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger("root."
    + GetProperty.class.getName());

    @Parameter(description = "property-name ...", required=false)
    private List<String> instanceProperties = null;

    @Parameter(names = {"-a","--all"}, description = "List values of all instance properties", required = false)
    private boolean showAll = false; 

    @Parameter(names = {"--application-ev"}, description = "Specifies to retrieve values for application environment variables.", required = false)
    private boolean applicationEv = false;

    public GetProperty() {
    }

    @Override
    public String getName() {
        return (Constants.CMD_GETPROPERTY);
    }

    @Override
    public String getHelp() {
        return (Constants.DESC_GETPROPERTY);
    }

    @Override
    protected CommandResult doExecute() {
        try {

           // Mutual Exclusivity Test
           LOGGER.debug("instanceProperties.size(): {}",(instanceProperties == null)?"null": instanceProperties.size());
           LOGGER.debug("showAll: {}",(showAll == true)?1:0);
           LOGGER.debug("mutual Exlusive total (should be < 2):" + ( 
            ((instanceProperties != null && instanceProperties.size() >0)?1:0) + 
            ((showAll == true)?1:0)));
            if ((((instanceProperties != null && instanceProperties.size() >0)?1:0) + 
                ((showAll == true)?1:0))>1) {
                throw new ParameterException("The following options are mutually exclusive: {[-a,--all] | [property-name ...]}");
            }

            // instance-property or all is required
            if ((((instanceProperties != null && instanceProperties.size() >0)?1:0) + 
                ((showAll == true)?1:0))<1) {
                throw new ParameterException("A required option or argument was not specified. Specify one of the following options or arguments: {[-a,--all] | [property-name ...]}");
            }

            ObjectMapper mapper = new ObjectMapper();
            ObjectNode jsonOut = mapper.createObjectNode();

            InstanceMXBean instance = getInstanceMXBean();


            // Populate the result object
            ArrayNode propertyArray = mapper.createArrayNode();
            jsonOut.put("instance",instance.getName());

            if (applicationEv) {
                TreeMap<String,String> applicationEvMap = new TreeMap<String,String>();
                applicationEvMap.putAll(instance.getApplicationEnvironmentVariables());

                if (instanceProperties != null && instanceProperties.size() > 0) {
                    for (String propertyName : instanceProperties) {
                        if (applicationEvMap.containsKey(propertyName)) {
                            ObjectNode propertyObject = mapper.createObjectNode();
                            propertyObject.put("property",propertyName);
                            propertyObject.put("value",applicationEvMap.get(propertyName));
                            propertyArray.add(propertyObject);
                        } else {
                            LOGGER.warn("The following properrty is not an application property: {}", propertyName);
                        }
                    }
                } else {
                    for (Map.Entry<String, String> entry : applicationEvMap.entrySet()) {
                        ObjectNode propertyObject = mapper.createObjectNode();
                        propertyObject.put("property",entry.getKey().toString());
                        propertyObject.put("value",entry.getValue());
                        propertyArray.add(propertyObject);
                    }   
                }
            } else {
                if (instanceProperties != null && instanceProperties.size() > 0) {
                    for (String propertyName : instanceProperties) {
                        LOGGER.debug("Looking up property: {}", propertyName);
                        ObjectNode propertyObject = mapper.createObjectNode();
                        InstanceMXBean.PropertyId propertyId = null;
                        try {
                            propertyId = InstanceMXBean.PropertyId.fromString(propertyName);
                        } catch (IllegalArgumentException e) {
                            LOGGER.warn("The following property is not valid: {}",propertyName);
                        }
                        if (propertyId != null) {
                            String propertyValue = instance.getProperty(propertyId);
                            propertyObject.put("property",propertyName);
                            propertyObject.put("value",propertyValue);
                            propertyArray.add(propertyObject);
                        }
                    }
                } else {
                    // all were specified
                    try {
                        // Sort the properties
                        TreeMap<InstanceMXBean.PropertyId,String> propertyMap = new TreeMap<InstanceMXBean.PropertyId,String>(new PropertyComparator());
                        propertyMap.putAll(instance.getProperties(true));

                        for (Map.Entry<InstanceMXBean.PropertyId, String> entry : propertyMap.entrySet()) {
                            ObjectNode propertyObject = mapper.createObjectNode();
                            propertyObject.put("property",entry.getKey().toString());
                            propertyObject.put("value",entry.getValue());
                            propertyArray.add(propertyObject);
                        }   
                    } catch ( UndeclaredThrowableException e) {
                        LOGGER.debug("Caught UndeclaredThrowableException (Usually because of wrong version of streams mx .jar files");
                        Throwable t = e.getUndeclaredThrowable();
                        LOGGER.debug("Unwrapped: {}: {}", t.getClass(),t.getLocalizedMessage());
                    }            
                }
            }

            jsonOut.set("properties",propertyArray);
            jsonOut.put("count",propertyArray.size());


            return new CommandResult(jsonOut.toString());
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("GetProperty caught Exception: {}", e.toString());
                e.printStackTrace();
            }
            return new CommandResult(ExitStatus.FAILED_COMMAND, null, e.getLocalizedMessage());
        }
    }

    class PropertyComparator implements Comparator<InstanceMXBean.PropertyId> {
        public int compare(InstanceMXBean.PropertyId e1, InstanceMXBean.PropertyId e2) {
            return (e1.toString()).compareTo(e2.toString());
        }
    }

}
