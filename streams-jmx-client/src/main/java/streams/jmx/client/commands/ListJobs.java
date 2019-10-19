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

import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.management.ObjectName;
import com.ibm.streams.management.instance.InstanceMXBean;
import com.ibm.streams.management.job.JobMXBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Parameters(commandDescription = Constants.DESC_LISTJOBS)
public class ListJobs extends AbstractJobListCommand {
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

    @Override
    protected CommandResult doExecute() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode jsonOut = mapper.createObjectNode();

            InstanceMXBean instance = getInstanceMXBean();


            // Process Job Lists

            // Mutual Exclusivity Test
            LOGGER.debug("mutual Exlusive total (should be < 2:" + ( 
                ((getJobIdOptionList() != null && getJobIdOptionList().size() >0)?1:0) + 
                ((getJobNameOptionList() != null && getJobNameOptionList().size() > 0)?1:0)));
            if ((((getJobIdOptionList() != null && getJobIdOptionList().size() >0)?1:0) + 
                 ((getJobNameOptionList() != null && getJobNameOptionList().size() > 0)?1:0))>1) {
                throw new ParameterException("The following options are mutually exclusive: {[-j,--jobs <jobId>] | [--jobnames <job-names>,...]}");
            }

            List<String> jobsToList = new ArrayList<String>();

            if (getJobIdOptionList() != null && getJobIdOptionList().size() > 0) {
                LOGGER.debug("Size of jobIds: " + getJobIdOptionList().size());
                LOGGER.debug("jobIds: " + Arrays.toString(getJobIdOptionList().toArray())); 

                // reference copy
                jobsToList = getJobIdOptionList();
            }


            if (getJobNameOptionList() != null && getJobNameOptionList().size() > 0) {
                LOGGER.debug("Size of jobNames: " + getJobNameOptionList().size());
                LOGGER.debug("jobNames: " + Arrays.toString(getJobNameOptionList().toArray()));

                // reference copy
                jobsToList = getResolvedJobNameOptionList();
            }

            // Populate the result object
            jsonOut.put("instance",instance.getName());
    
            ArrayNode jobArray = mapper.createArrayNode();
            int listCount = 0;

            for (String jobId : instance.getJobs()) {
                // Check if we want this one
                if (jobsToList.size() > 0) {
                    if (!jobsToList.contains(jobId))
                        continue;  // skip it
                }
                LOGGER.trace("Lookup up job bean for jobid: {} of instance: {}", jobId, instance.getName());

                @SuppressWarnings("unused")
                ObjectName jobObjectName = instance.registerJobById(jobId);
                JobMXBean job = getBeanSource().getJobBean(getInstanceName(),jobId);
                
                listCount++;
                ObjectNode jobObject = mapper.createObjectNode();
                jobObject.put("id",jobId);
                jobObject.put("status",job.getStatus().toString());
                jobObject.put("health",job.getHealth().toString());
                jobObject.put("startedbyuser",job.getStartedByUser());
                jobObject.put("submittime",job.getSubmitTime());
                jobObject.put("name",job.getName());
                jobObject.put("group",job.getJobGroup());

                jobArray.add(jobObject);
            }
            jsonOut.put("count",listCount);
            jsonOut.set("jobs",jobArray);
            //System.out.println(sb.toString());
            return new CommandResult(jsonOut.toString());
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                System.out.println("ListJobs caught Exception: " + e.toString());
                e.printStackTrace();
            }
            return new CommandResult(ExitStatus.FAILED_COMMAND, null, e.getLocalizedMessage());
        }
    }
}
