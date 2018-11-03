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

import com.ibm.streams.management.domain.DomainMXBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Parameters(commandDescription = Constants.DESC_GETDOMAINSTATE)
public class GetDomainProperty extends AbstractDomainCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger("root."
    + GetDomainProperty.class.getName());

    @Parameter(description = "property-name ...", required=false)
    private List<String> domainProperties = null;

    @Parameter(names = {"-a","--all"}, description = "List values of all domain properties", required = false)
    private boolean showAll = false; 


    public GetDomainProperty() {
    }

    @Override
    public String getName() {
        return (Constants.CMD_GETDOMAINPROPERTY);
    }

    @Override
    public String getHelp() {
        return (Constants.DESC_GETDOMAINPROPERTY);
    }

    @Override
    protected CommandResult doExecute() {
        try {

           // Mutual Exclusivity Test
           LOGGER.debug("domainProperties.size(): {}",(domainProperties == null)?"null":domainProperties.size());
           LOGGER.debug("showAll: {}",(showAll == true)?1:0);
           LOGGER.debug("mutual Exlusive total (should be < 2):" + ( 
            ((domainProperties != null && domainProperties.size() >0)?1:0) + 
            ((showAll == true)?1:0)));
            if ((((domainProperties != null && domainProperties.size() >0)?1:0) + 
                ((showAll == true)?1:0))>1) {
                throw new ParameterException("The following options are mutually exclusive: {[-a,--all] | [property-name ...]}");
            }

            // domain-property or all is required
            if ((((domainProperties != null && domainProperties.size() >0)?1:0) + 
                ((showAll == true)?1:0))<1) {
                throw new ParameterException("A required option or argument was not specified. Specify one of the following options or arguments: {[-a,--all] | [domain-property ...]}");
            }

            ObjectMapper mapper = new ObjectMapper();
            ObjectNode jsonOut = mapper.createObjectNode();

            DomainMXBean domain = getDomainMXBean();



            // Populate the result object
            ArrayNode propertyArray = mapper.createArrayNode();
            jsonOut.put("domain",domain.getName());

            if (domainProperties != null && domainProperties.size() > 0) {
                for (String propertyName : domainProperties) {
                    LOGGER.debug("Looking up property: {}", propertyName);
                    ObjectNode propertyObject = mapper.createObjectNode();
                    DomainMXBean.PropertyId propertyId = null;
                    try {
                        propertyId = DomainMXBean.PropertyId.fromString(propertyName);
                    } catch (IllegalArgumentException e) {
                        LOGGER.warn("The following property is not valid: {}",propertyName);
                    }
                    if (propertyId != null) {
                        String propertyValue = domain.getProperty(propertyId);
                        propertyObject.put("property",propertyName);
                        propertyObject.put("value",propertyValue);
                        propertyArray.add(propertyObject);
                    }
                }
            } else {
                // all were specified
                try {
                    // Sort the properties
                    TreeMap<DomainMXBean.PropertyId,String> propertyMap = new TreeMap<DomainMXBean.PropertyId,String>(new PropertyComparator());
                    propertyMap.putAll(domain.getProperties(true));

                    for (Map.Entry<DomainMXBean.PropertyId, String> entry : propertyMap.entrySet()) {
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


            jsonOut.set("properties",propertyArray);
            jsonOut.put("count",propertyArray.size());

            return new CommandResult(jsonOut.toString());
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("GetDomainProperty caught Exception: {}", e.toString());
                e.printStackTrace();
            }
            return new CommandResult(ExitStatus.FAILED_COMMAND, null, e.getLocalizedMessage());
        }
    }

    class PropertyComparator implements Comparator<DomainMXBean.PropertyId> {
        public int compare(DomainMXBean.PropertyId e1, DomainMXBean.PropertyId e2) {
            return (e1.toString()).compareTo(e2.toString());
        }
    }

}
