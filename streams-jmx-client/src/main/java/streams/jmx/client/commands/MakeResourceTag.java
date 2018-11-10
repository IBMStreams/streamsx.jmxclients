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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Parameters(commandDescription = Constants.DESC_MAKERESOURCETAG)
public class MakeResourceTag extends AbstractDomainCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger("root."
            + MakeResourceTag.class.getName());

    @Parameter(description = "Specifies the name of the tag", required=true)
    private String tagName = null;

    @Parameter(names = {"--description"}, description = "Description of tag", required = false)
    private String tagDescription=null;


    @DynamicParameter(names = "--property", description = "Property name and value <property>=<value> (may be used multiple times)", required = false)
    private Map<String, String> tagProperties = new HashMap<String, String>();

    public MakeResourceTag() {
    } 

    @Override
    public String getName() {
        return (Constants.CMD_MAKERESOURCETAG);
    }

    @Override
    public String getHelp() {
        return (Constants.DESC_MAKERESOURCETAG);
    }

    @Override
    protected CommandResult doExecute() {
        try {

            ObjectMapper mapper = new ObjectMapper();
            ObjectNode jsonOut = mapper.createObjectNode();
            
            DomainMXBean domain = getDomainMXBean();
    
            domain.addResourceTag(tagName, tagDescription, tagProperties);
            jsonOut.put("tag", tagName);
            jsonOut.put("result", "created");

            return new CommandResult(jsonOut.toString());
        } catch (Exception e) {
            LOGGER.debug("MakeResourceTag caught Exception: " + e.toString());
            //e.printStackTrace();
            return new CommandResult(ExitStatus.FAILED_COMMAND, null, e.getLocalizedMessage());
        }
    }
}
