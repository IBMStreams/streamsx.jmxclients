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
import streams.jmx.client.cli.BigIntegerConverter;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.management.ObjectName;
import com.ibm.streams.management.instance.InstanceMXBean;
import com.ibm.streams.management.job.JobMXBean;
import com.ibm.streams.management.job.PeMXBean;
import com.ibm.streams.management.resource.ResourceMXBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Parameters(commandDescription = Constants.DESC_LISTPES)
public class ListPes extends AbstractJobListCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger("root."
            + ListPes.class.getName());

    @Parameter(names = {"--pes"}, description = "A list of processing elements (PDs)", required = false,
    converter = BigIntegerConverter.class)
    private List<BigInteger> peIds;

    public ListPes() {
    }

    @Override
    public String getName() {
        return (Constants.CMD_LISTPES);
    }

    @Override
    public String getHelp() {
        return (Constants.DESC_LISTPES);
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

            List<BigInteger> jobsToList = new ArrayList<BigInteger>();

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
    
            ArrayNode peArray = mapper.createArrayNode();
            int listCount = 0;

            for (BigInteger jobId : instance.getJobs()) {
                // Check if we want this one
                if (jobsToList.size() > 0) {
                    if (!jobsToList.contains(jobId))
                        continue;  // skip it
                }
                LOGGER.trace("Lookup up job bean for jobid: {} of instance: {}", jobId, instance.getName());

                @SuppressWarnings("unused")
                ObjectName jobObjectName = instance.registerJob(jobId);
                JobMXBean job = getBeanSource().getJobBean(getDomainName(),getInstanceName(),jobId);
                
                for (BigInteger peId : job.getPes()) {
                    // Check if we want this one
                    if ((peIds != null) && (peIds.size() > 0)) {
                        if (!peIds.contains(peId))
                            continue;  // skip it
                    }


                    PeMXBean pe = getBeanSource().getPeBean(getDomainMXBean().getName(), instance.getName(), peId);
                    ResourceMXBean resource = getBeanSource().getResourceBean(instance.getDomain(), pe.getResource());

                    listCount++;

                    ObjectNode peObject = mapper.createObjectNode();
                    peObject.put("id",peId.longValue());
                    peObject.put("status",pe.getStatus().toString());
                    peObject.put("statusreason",pe.getStatusReason().toString());
                    peObject.put("health",pe.getHealth().toString());
                    peObject.put("resource",pe.getResource());
                    peObject.put("ip",resource.getIpAddress());
                    peObject.put("pid",pe.getPid());
                    peObject.put("launchcount",pe.getLaunchCount());
                    peObject.put("jobid",jobId.longValue());
                    peObject.put("jobname",job.getName());

                    ArrayNode operatorArray = mapper.valueToTree(pe.getOperators());
                    peObject.set("operators",operatorArray);

                    peArray.add(peObject);
                }


            }
            jsonOut.put("count",listCount);
            jsonOut.set("pes",peArray);
            //System.out.println(sb.toString());
            return new CommandResult(jsonOut.toString());
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                System.out.println("ListPes caught Exception: " + e.toString());
                e.printStackTrace();
            }
            return new CommandResult(ExitStatus.FAILED_COMMAND, null, e.getLocalizedMessage());
        }
    }
}
