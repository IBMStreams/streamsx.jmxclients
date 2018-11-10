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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.JMX;
import javax.management.ObjectName;

import com.ibm.streams.management.OperationListenerMXBean;
import com.ibm.streams.management.OperationStatusMessage;
import com.ibm.streams.management.domain.DomainMXBean;
import com.ibm.streams.management.resource.ResourceTag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Parameters(commandDescription = Constants.DESC_REMOVERESOURCETAG)
public class RemoveResourceTag extends AbstractDomainCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger("root."
            + MakeResourceTag.class.getName());

    @Parameter(description = "Specifies the name of the tag to remove", required=true)
    private List<String> tagNames = null;

    @Parameter(names = {"--noprompt"}, description = "Supress confirmation prompts", required = false)
    private boolean noPrompt=false;

    public RemoveResourceTag() {
    } 

    @Override
    public String getName() {
        return (Constants.CMD_REMOVERESOURCETAG);
    }

    @Override
    public String getHelp() {
        return (Constants.DESC_REMOVERESOURCETAG);
    }

    @Override
    protected CommandResult doExecute() {
        try {

            ObjectMapper mapper = new ObjectMapper();
            ObjectNode jsonOut = mapper.createObjectNode();
            ArrayNode tagArray = mapper.createArrayNode();

            DomainMXBean domain = getDomainMXBean();
    
            int removeCount = 0;
            jsonOut.put("domain",domain.getName());

            Set<String> allTagNames = new HashSet<String>();
            for (ResourceTag resourceTag : domain.getResourceTags()) {
                allTagNames.add(resourceTag.getName());
            }

            for (String tagName : tagNames) {
                if (allTagNames.contains(tagName)) {
                    try {
                        domain.removeResourceTag(tagName, null);
                        removeCount++;
                        ObjectNode tagObject = mapper.createObjectNode();
                        tagObject.put("tag", tagName);
                        tagArray.add(tagObject);
                    } catch (IllegalStateException e) {
                        LOGGER.warn(e.getLocalizedMessage());
                    }
                } else {
                    LOGGER.warn("The following tag is not defined in the {} domain: {}", domain.getName(), tagName);
                }
            }
            jsonOut.put("count",removeCount);
            jsonOut.set("tags",tagArray);

            return new CommandResult(jsonOut.toString());
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("RemoveResourceTag caught Exception: " + e.toString());
                e.printStackTrace();
            }
            return new CommandResult(ExitStatus.FAILED_COMMAND, null, e.getLocalizedMessage());
        }
    }
}
