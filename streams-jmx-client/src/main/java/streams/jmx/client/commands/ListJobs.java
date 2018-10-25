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

import streams.jmx.client.jmx.JmxServiceContext;
import streams.jmx.client.ServiceConfig;
import streams.jmx.client.Constants;
import streams.jmx.client.ExitStatus;

import com.beust.jcommander.Parameters;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;

import javax.management.ObjectName;
import com.ibm.streams.management.ObjectNameBuilder;
import com.ibm.streams.management.domain.DomainMXBean;
import com.ibm.streams.management.instance.InstanceMXBean;
import com.ibm.streams.management.instance.InstanceServiceMXBean;
import com.ibm.streams.management.job.JobMXBean;
import com.ibm.streams.management.resource.ResourceMXBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Parameters(commandDescription = Constants.DESC_LISTJOBS)
public class ListJobs extends AbstractInstanceCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger("root."
            + ListJobs.class.getName());

    public ListJobs() {
    }

    @Override
    public String getName() {
        return (Constants.CMD_LISTJOBS);
    }

    @Override
    public String getHelp() {
        return (Constants.DESC_LISTJOBS);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected CommandResult doExecute() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode jsonOut = mapper.createObjectNode();

            InstanceMXBean instance = getInstanceMXBean();

            // Populate the result object
            jsonOut.put("instance",instance.getName());
    
            ArrayNode jobArray = mapper.createArrayNode();

            for (BigInteger jobId : instance.getJobs()) {
                LOGGER.trace("Lookup up job bean for jobid: {} of instance: {}", jobId, instance.getName());

                ObjectName jobObjectName = instance.registerJob(jobId);
                JobMXBean job = getBeanSource().getJobBean(getDomainName(),getInstanceName(),jobId);
                
                ObjectNode jobObject = mapper.createObjectNode();
                jobObject.put("id",jobId.longValue());
                jobObject.put("status",job.getStatus().toString());
                jobObject.put("health",job.getHealth().toString());
                jobObject.put("startedbyuser",job.getStartedByUser());
                jobObject.put("submittime",job.getSubmitTime());
                jobObject.put("name",job.getName());
                jobObject.put("group",job.getJobGroup());

                jobArray.add(jobObject);
            }
            jsonOut.set("jobs",jobArray);
            //System.out.println(sb.toString());
            return new CommandResult(jsonOut.toString());
        } catch (Exception e) {
            System.out.println("ListJobs caught Exception: " + e.toString());
            e.printStackTrace();
            return new CommandResult(ExitStatus.FAILED_COMMAND, null, e.getLocalizedMessage());
        }
    }
}
