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
import streams.jmx.client.cli.BigIntegerConverter;
import streams.jmx.client.cli.FileExistsValidator;
import streams.jmx.client.Constants;
import streams.jmx.client.ExitStatus;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.FileConverter;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.management.ObjectName;
import com.ibm.streams.management.ObjectNameBuilder;
import com.ibm.streams.management.domain.DomainMXBean;
import com.ibm.streams.management.instance.InstanceMXBean;
import com.ibm.streams.management.instance.InstanceServiceMXBean;
import com.ibm.streams.management.job.DeployInformation;
import com.ibm.streams.management.resource.ResourceMXBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Parameters(commandDescription = Constants.DESC_CANCELJOB)
public class CancelJob extends AbstractInstanceCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger("root."
            + CancelJob.class.getName());

    // Need to fix when converter is finally supported for main parameter
    @Parameter(description = "Job IDs to cancel", required=false,
    converter=BigIntegerConverter.class)
    private List<String> jobIdArgumentStrings = null;

    private List<BigInteger> jobIdArguments = null;

    @Parameter(names = {"-j","--jobs"}, description = "A list of job ids delimited by commas", required = false,
    converter = BigIntegerConverter.class)
    private List<BigInteger> jobIds;
    //private String jobIdString;

    @Parameter(names = {"--jobnames"}, description = "Specifies a list of job names, which are delimited by commas.", required = false)
    private List<String> jobNames=null;

    @Parameter(names = "--force", description = "Forces quick cancellation of job", required = false)
    private boolean forceCancel = false;

    public CancelJob() {
    }

    @Override
    public String getName() {
        return (Constants.CMD_CANCELJOB);
    }

    @Override
    public String getHelp() {
        return (Constants.DESC_CANCELJOB);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected CommandResult doExecute() {
        try {

            // Mutual Exclusivity Test
            //if (((jobIdArgumentString != null?1:0) + (jobId != null?1:0) + (jobNames.size() > 1?1:0))>1) {
            LOGGER.debug("mutual Exlusive total (should be < 2:" + (((jobIdArgumentStrings != null && jobIdArgumentStrings.size() > 1)?1:0) + 
            ((jobIds != null && jobIds.size() >1)?1:0) + 
            ((jobNames != null && jobNames.size() > 1)?1:0)));
            if ((((jobIdArgumentStrings != null && jobIdArgumentStrings.size() > 1)?1:0) + 
                 ((jobIds != null && jobIds.size() >1)?1:0) + 
                 ((jobNames != null && jobNames.size() > 1)?1:0))>1) {
                throw new ParameterException("The following options are mutually exclusive: {[-j,--jobs <jobId>] | [<jobIdArgument>] | [--jobnames <job-names>,...]}");
            }

            //if ((jobIdArgument == null) && (jobId == null) && (jobNames.size() == 0)) {
            if ((jobIdArgumentStrings != null && jobIdArgumentStrings.size() == 0) && 
                (jobIds != null && jobIds.size() == 0) && 
                (jobNames != null && jobNames.size() == 0)) {
                throw new ParameterException("A required option or argument was not specified. Specify one of the following options or arguments: {[-j,--jobs] | [<jobIdArgument>] | [--jobnames]}");
            }

            InstanceMXBean instance = getInstanceMXBean();

            List<BigInteger> jobsToCancel = new ArrayList<BigInteger>();

            // Work around until jcommander 1.74 available in maven
            //if ((jobIdArgumentString != null) && (!jobIdArgumentString.isEmpty())) {
            if (jobIdArgumentStrings != null && jobIdArgumentStrings.size() > 0) {
                LOGGER.debug("Attempting to convert jobIdArgumentStrings ({}) to BigIntegers...", Arrays.toString(jobIdArgumentStrings.toArray()));

                BigIntegerConverter bigIntegerConverter = new BigIntegerConverter("jobId");
                for (String bigIntString : jobIdArgumentStrings) {
                    jobIdArguments.add(bigIntegerConverter.convert(bigIntString));
                }

                LOGGER.debug("Size of jobIdArguments: " + jobIdArguments.size());
                LOGGER.debug("jobIdArguments: " + Arrays.toString(jobIdArguments.toArray()));

                // reference copy
                jobsToCancel = jobIdArguments;
            }

            if (jobIds != null && jobIds.size() > 0) {
                LOGGER.debug("Size of jobIds: " + jobIds.size());
                LOGGER.debug("jobIds: " + Arrays.toString(jobIds.toArray())); 

                // reference copy
                jobsToCancel = jobIds;
            }


            if (jobNames != null && jobNames.size() > 0) {
                LOGGER.debug("Size of jobNames: " + jobNames.size());
                LOGGER.debug("jobNames: " + Arrays.toString(jobNames.toArray()));

                ArrayList<BigInteger> jobNameIds = new ArrayList<BigInteger>();
                for (String jobname : jobNames) {
                    LOGGER.debug("Lookup up jobId of jobName({})",jobname);
                    try {
                        BigInteger curJobId = instance.getJobId(jobname);
                        jobNameIds.add(curJobId);
                    } catch (IllegalStateException e) {
                        LOGGER.warn(e.getLocalizedMessage());
                    }
                }

                // reference copy
                jobsToCancel = jobNameIds;
            }

            LOGGER.debug("About to cancel following jobids: " + Arrays.toString(jobsToCancel.toArray()));

            //BigInteger theJobId = (jobId != null ? jobId : jobIdArgument);

            ObjectMapper mapper = new ObjectMapper();
            ObjectNode jsonOut = mapper.createObjectNode();
            ArrayNode jobArray = mapper.createArrayNode();
            int cancelCount = 0;
            
            //StringBuilder sb = new StringBuilder();
            for (BigInteger jobid : jobsToCancel) {
                instance.cancelJob(jobid, forceCancel);
                cancelCount++;
                ObjectNode jobObject = mapper.createObjectNode();
                jobObject.put("jobId", jobid.longValue());
                jobArray.add(jobObject);
            }
            jsonOut.put("count",cancelCount);
            jsonOut.set("jobs",jobArray);

            return new CommandResult(jsonOut.toString());
        } catch (Exception e) {
            LOGGER.debug("CancelJob caught Exception: " + e.toString());
            e.printStackTrace();
            return new CommandResult(ExitStatus.FAILED_COMMAND, null, e.getLocalizedMessage());
        }
    }
}
