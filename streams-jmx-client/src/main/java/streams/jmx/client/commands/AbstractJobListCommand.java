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

import streams.jmx.client.cli.BigIntegerConverter;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


import com.beust.jcommander.Parameter;
import com.ibm.streams.management.instance.InstanceMXBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class AbstractJobListCommand extends AbstractInstanceCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger("root."
            + AbstractJobListCommand.class.getName());


    @Parameter(names = {"-j","--jobs"}, description = "A list of job ids delimited by commas", required = false,
    converter = BigIntegerConverter.class)
    private List<BigInteger> jobIds;
    //private String jobIdString;

    @Parameter(names = {"--jobnames"}, description = "Specifies a list of job names, which are delimited by commas.", required = false)
    private List<String> jobNames=null;            






    public AbstractJobListCommand() {

    }

    @Override
    protected final void prepareJmxInstanceExecution() throws Exception {
        LOGGER.trace("In prepareJmxInstanceExecution...");
     
        prepareJmxJobListExecution();
    }

    // Override in decendent class
    protected void prepareJmxJobListExecution() throws Exception {}

    protected final List<BigInteger> getJobIdOptionList() {
        return jobIds;
    }

    protected final List<String> getJobNameOptionList() {
        return jobNames;
    }

    public List<BigInteger> getResolvedJobNameOptionList() throws Exception {

        InstanceMXBean instance = getInstanceMXBean();

        ArrayList<BigInteger> jobNameIds = null;

        if (jobNames != null && jobNames.size() > 0) {
            LOGGER.debug("Size of jobNames: " + jobNames.size());
            LOGGER.debug("jobNames: " + Arrays.toString(jobNames.toArray()));

            jobNameIds = new ArrayList<BigInteger>();
            for (String jobname : jobNames) {
                LOGGER.debug("Lookup up jobId of jobName({})",jobname);
                try {
                    BigInteger curJobId = instance.getJobId(jobname);
                    jobNameIds.add(curJobId);
                } catch (IllegalStateException e) {
                    LOGGER.warn(e.getLocalizedMessage());
                }
            }
        }
        return jobNameIds;
    }
}