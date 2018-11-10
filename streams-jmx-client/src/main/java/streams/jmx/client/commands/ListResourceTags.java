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

import java.util.Comparator;
import java.util.TreeSet;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import com.ibm.streams.management.domain.DomainMXBean;
import com.ibm.streams.management.resource.ResourceTag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Parameters(commandDescription = Constants.DESC_LISTRESOURCETAGS)
public class ListResourceTags extends AbstractDomainCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger("root."
            + ListResourceTags.class.getName());

    @Parameter(description = "optional tag pattern * = zero or more chars, ? = any single character", required=false)
    private String tagPattern = null;

    public ListResourceTags() {
    }

    @Override
    public String getName() {
        return (Constants.CMD_LISTRESOURCETAGS);
    }

    @Override
    public String getHelp() {
        return (Constants.DESC_LISTRESOURCETAGS);
    }

    @Override
    protected CommandResult doExecute() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode jsonOut = mapper.createObjectNode();

            DomainMXBean domain = getDomainMXBean();

            // Populate the result object
            jsonOut.put("domain",domain.getName());
    
            ArrayNode tagArray = mapper.createArrayNode();

            // Sort the tags
            TreeSet<ResourceTag> sortedTagSet = new TreeSet<ResourceTag>(new ResourceTagComparator());
            sortedTagSet.addAll(domain.getResourceTags());

            //TreeSet<ResourceTag> sortedTagSet = new TreeSet<ResourceTag>(domain.getResourceTags());

            //for (ResourceTag resourceTag : domain.getResourceTags()) {
            String tagRegex = null;

            if (tagPattern == null) {
                LOGGER.debug("No tagPattern supplied, returning all tags");
            } else {
                tagRegex = "^" + tagPattern.replace("?",".?").replace("*", ".*?") + "$";
                LOGGER.debug("Checking tags matching pattern: {}, regex: {}",tagPattern,tagRegex);
            }
            for (ResourceTag resourceTag : sortedTagSet) {
                if ((tagRegex == null) || (resourceTag.getName().matches(tagRegex)))
                {
                    ObjectNode resourceTagObject = mapper.createObjectNode();
                    resourceTagObject.put("tag",resourceTag.getName());
                    resourceTagObject.put("description",resourceTag.getDescription());
                    tagArray.add(resourceTagObject);
                }
            }
            jsonOut.set("tags",tagArray);

            return new CommandResult(jsonOut.toString());
        } catch (Exception e) {
            System.out.println("ListResourceTags caught Exception: " + e.toString());
            e.printStackTrace();
            return new CommandResult(ExitStatus.FAILED_COMMAND, null, e.getLocalizedMessage());
        }
    }

    class ResourceTagComparator implements Comparator<ResourceTag> {
        public int compare(ResourceTag e1, ResourceTag e2) {
            return (e1.getName().compareToIgnoreCase(e2.getName()));
        }
    }
}
