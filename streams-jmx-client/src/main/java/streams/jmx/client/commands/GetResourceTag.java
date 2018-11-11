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

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import java.util.HashMap;
import java.util.Map;

import com.ibm.streams.management.domain.DomainMXBean;
import com.ibm.streams.management.resource.ResourceTag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

@Parameters(commandDescription = Constants.DESC_GETRESOURCETAG)
public class GetResourceTag extends AbstractDomainCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger("root."
            + GetResourceTag.class.getName());

    @Parameter(description = "Specifies the name of the tag", required=true)
    private String tagName = null;


    public GetResourceTag() {
    } 

    @Override
    public String getName() {
        return (Constants.CMD_GETRESOURCETAG);
    }

    @Override
    public String getHelp() {
        return (Constants.DESC_GETRESOURCETAG);
    }

    @Override
    protected CommandResult doExecute() {
        try {

            ObjectMapper mapper = new ObjectMapper();
            ObjectNode jsonOut = mapper.createObjectNode();
            
            DomainMXBean domain = getDomainMXBean();
    
            ResourceTag theResourceTag = null;
            for (ResourceTag resourceTag : domain.getResourceTags()) {
                if (resourceTag.getName().equals(tagName)) {
                    theResourceTag = resourceTag;
                    break;
                }
            }

            if (theResourceTag != null) {
                jsonOut.put("domain", domain.getName());
                jsonOut.put("tag", tagName);
                jsonOut.put("description", theResourceTag.getDescription());
                if (theResourceTag.isDefinitionFormatProperties()) {
                  ArrayNode propertyArray = mapper.createArrayNode();
                  for (Map.Entry<String,String> entry : theResourceTag.getDefinitionAsProperties().entrySet()) {
                    ObjectNode propertyObject = mapper.createObjectNode();
                    propertyObject.put("property",entry.getKey().toString());
                    propertyObject.put("value",entry.getValue());
                    propertyArray.add(propertyObject);
                  }
                  jsonOut.set("properties",propertyArray);
                } else {
                  jsonOut.put("properties", theResourceTag.getDefinitionAsCustomFormat());
                }
            } else {
                throw new Exception("The following tag is not defined in the " + domain.getName() + "domain: " + tagName);
            }
            return new CommandResult(jsonOut.toString());
    
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("GetResourceTag caught Exception: " + e.toString());
                e.printStackTrace();
            }
            return new CommandResult(ExitStatus.FAILED_COMMAND, null, e.getLocalizedMessage());
        }
    }
}
