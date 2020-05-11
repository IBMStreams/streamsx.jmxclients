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

import java.util.Collections;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;

import com.ibm.streams.management.instance.InstanceMXBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Parameters(commandDescription = Constants.DESC_LISTJOBGROUP)
public class ListJobGroup extends AbstractInstanceCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger("root."
    + RemoveProperty.class.getName());

    @Parameter(description = "<jobgroup-name-pattern>", required=false)
    private String jobGroupPattern = null;

    public ListJobGroup() {
    }

    @Override
    public String getName() {
        return (Constants.CMD_LISTJOBGROUP);
    }

    @Override
    public String getHelp() {
        return (Constants.DESC_LISTJOBGROUP);
    }

    @Override
    protected CommandResult doExecute() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode jsonOut = mapper.createObjectNode();

            InstanceMXBean instance = getInstanceMXBean();

            // Populate the result object
            jsonOut.put("instance",instance.getName());

            ArrayNode jobGroupArray = mapper.createArrayNode();

            // Get job groups
            TreeSet<String> jobGroups = new TreeSet<String>();
            jobGroups.addAll(instance.getJobGroups());

            String jobGroupRegex = null;

            if (jobGroupPattern == null) {
                LOGGER.debug("No job group pattern supplied, returning all job groups");
            } else {
                jobGroupRegex = "^" + jobGroupPattern.replace("?",".?").replace("*",".*?") + "$";
                LOGGER.debug("Checking jobgroups matching pattern: {}, regex: {}",jobGroupPattern,jobGroupRegex);
            }

            for (String jobgroup : jobGroups) {
                if ((jobGroupRegex == null) || (jobgroup.matches(jobGroupRegex))) {
                    jobGroupArray.add(jobgroup);
                }
            }

            jsonOut.set("jobgroups",jobGroupArray);

            return new CommandResult(jsonOut.toString());
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("{} caught Exception: {}", getName(), e.toString());
                e.printStackTrace();
            }
            return new CommandResult(ExitStatus.FAILED_COMMAND, null, e.getLocalizedMessage());
        }
    }
}
