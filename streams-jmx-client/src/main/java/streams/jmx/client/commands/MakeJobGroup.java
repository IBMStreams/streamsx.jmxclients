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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;

import com.ibm.streams.management.instance.InstanceMXBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Parameters(commandDescription = Constants.DESC_MAKEJOBGROUP)
public class MakeJobGroup extends AbstractInstanceCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger("root."
    + RemoveProperty.class.getName());

    @Parameter(description = "<jobgroup>", required=true)
    private String jobgroup = null;

    public MakeJobGroup() {
    }

    @Override
    public String getName() {
        return (Constants.CMD_MAKEJOBGROUP);
    }

    @Override
    public String getHelp() {
        return (Constants.DESC_MAKEJOBGROUP);
    }

    @Override
    protected CommandResult doExecute() {
        try {

            // Validate jobgroup name
            Pattern p = Pattern.compile("[^a-z0-9]", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(jobgroup);
            if (m.find()) {
                throw new ParameterException("The jobgroup (" + jobgroup + ") is invalid.  Only alphanumerics are allowed");
            }
            
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode jsonOut = mapper.createObjectNode();

            InstanceMXBean instance = getInstanceMXBean();

            instance.createJobGroup(jobgroup);

            jsonOut.put("jobgroup",jobgroup);
            jsonOut.put("result","created");

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
