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

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.ibm.streams.management.instance.InstanceMXBean;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Parameters(commandDescription = Constants.DESC_SUBMITJOB)
public class SnapshotJobs extends AbstractJobListCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger("root."
            + SnapshotJobs.class.getName());

    @Parameter(names = "--maxdepth", description = "Maximum depth of operators to traverse (default 1)", required = false)
    private int maxDepth = 1;

    @Parameter(names = "--includestatic", description = "Flag to include static attributes (default false)", required = false)
    private boolean includeStatic = false;


    public SnapshotJobs() {
    }

    @Override
    public String getName() {
        return (Constants.CMD_SNAPSHOTJOBS);
    }

    @Override
    public String getHelp() {
        return (Constants.DESC_SNAPSHOTJOBS);
    }

    @Override
    protected CommandResult doExecute() {
        try {

            InstanceMXBean instance = getInstanceMXBean();

            // Mutual Exclusivity Test
            LOGGER.debug("mutual Exlusive total (should be < 2:" + ( 
                ((getJobIdOptionList() != null && getJobIdOptionList().size() >0)?1:0) + 
                ((getJobNameOptionList() != null && getJobNameOptionList().size() > 0)?1:0)));
            if ((((getJobIdOptionList() != null && getJobIdOptionList().size() >0)?1:0) + 
                 ((getJobNameOptionList() != null && getJobNameOptionList().size() > 0)?1:0))>1) {
                throw new ParameterException("The following options are mutually exclusive: {[-j,--jobs <jobId>] | [--jobnames <job-names>,...]}");
            }

            Set<BigInteger> jobsToSnapshot = null;

            if (getJobIdOptionList() != null && getJobIdOptionList().size() > 0) {
                LOGGER.debug("Size of jobIds: " + getJobIdOptionList().size());
                LOGGER.debug("jobIds: " + Arrays.toString(getJobIdOptionList().toArray())); 

                // reference copy
                jobsToSnapshot = new HashSet<BigInteger>(getJobIdOptionList());
            }


            if (getJobNameOptionList() != null && getJobNameOptionList().size() > 0) {
                LOGGER.debug("Size of jobNames: " + getJobNameOptionList().size());
                LOGGER.debug("jobNames: " + Arrays.toString(getJobNameOptionList().toArray()));

                // reference copy
                jobsToSnapshot = new HashSet<BigInteger>(getResolvedJobNameOptionList());
            }

            // Check if they wanted them all
            if (jobsToSnapshot == null) {
                jobsToSnapshot = instance.getJobs();
            }

            //JSONObject jsonOut = new JSONObject();
            //StringBuilder sb = new StringBuilder();
    
            String uri = null;

            uri = instance.snapshotJobs(jobsToSnapshot,maxDepth,includeStatic);

            String snapshotOutput = getJmxServiceContext().getWebClient().get(uri, getConfig().getJmxHttpHost(), getConfig().getJmxHttpPort());

            return new CommandResult(snapshotOutput);
        } catch (Exception e) {
            //System.out.println("GetInstanceState caught Exception: " + e.toString());
            //e.printStackTrace();
            return new CommandResult(ExitStatus.FAILED_COMMAND, null, e.getLocalizedMessage());
        }
    }
}
