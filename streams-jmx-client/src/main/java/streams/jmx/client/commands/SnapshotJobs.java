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
import streams.jmx.client.cli.FileExistsValidator;
import streams.jmx.client.Constants;
import streams.jmx.client.ExitStatus;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.FileConverter;

import java.io.File;
import java.math.BigInteger;
import java.util.Arrays;
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

@Parameters(commandDescription = Constants.DESC_SUBMITJOB)
public class SnapshotJobs extends AbstractInstanceCommand {

    @Parameter(names = {"-j","--jobs"}, description = "A list of job ids delimited by commas", required = true)
    private String jobIdString;

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

    @SuppressWarnings("unchecked")
    @Override
    protected CommandResult doExecute() {
        try {
            //JSONObject jsonOut = new JSONObject();
            //StringBuilder sb = new StringBuilder();
            InstanceMXBean instance = getInstanceMXBean();
    
            String uri = null;

            BigInteger jobId = new BigInteger(jobIdString);

            uri = instance.snapshotJobs(new HashSet<BigInteger>(Arrays.asList(jobId)),maxDepth,includeStatic);

            String snapshotOutput = getJmxServiceContext().getWebClient().get(uri, getConfig().getJmxHttpHost(), getConfig().getJmxHttpPort());

            return new CommandResult(snapshotOutput);
        } catch (Exception e) {
            //System.out.println("GetInstanceState caught Exception: " + e.toString());
            //e.printStackTrace();
            return new CommandResult(ExitStatus.FAILED_COMMAND, null, e.getLocalizedMessage());
        }
    }
}
