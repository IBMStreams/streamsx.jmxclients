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

import streams.jmx.client.cli.FileExistsValidator;
import streams.jmx.client.Constants;
import streams.jmx.client.ExitStatus;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.FileConverter;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import javax.management.JMX;
import javax.management.ObjectName;

import com.ibm.streams.management.OperationListenerMXBean;
import com.ibm.streams.management.OperationStatusMessage;
import com.ibm.streams.management.instance.InstanceMXBean;
import com.ibm.streams.management.ContentTransfer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.commons.lang.StringUtils;

@Parameters(commandDescription = Constants.DESC_SUBMITJOB)
public class SubmitJob extends AbstractInstanceCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger("root."
            + SubmitJob.class.getName());

    // Allow .sab file to be put at end of command as streamtool does
    // Need jcommander 1.74 to fix no converter for main parameter
    @Parameter(description = "Path to the .sab file", required=false,
        converter=com.beust.jcommander.converters.FileConverter.class,
        validateWith = FileExistsValidator.class)
    private String sabFileArgumentString = null;
    private File sabFileArgument=null;

    @Parameter(names = {"-f","--file"}, description = "Path to the .sab file", required = false,
        converter=com.beust.jcommander.converters.FileConverter.class,
        validateWith = FileExistsValidator.class)
    private File sabFile=null;

    @Parameter(names = {"-g","--jobConfig"}, description = "Job configuration overlay file", required = false,
        converter=com.beust.jcommander.converters.FileConverter.class,
        validateWith = FileExistsValidator.class)
    private File jobConfigFile=null;

    @DynamicParameter(names = {"-P","--P"}, description = "Submission time parameter (may be used multiple times)", required = false)
    private Map<String, String> params = new HashMap<String, String>();

    @DynamicParameter(names = {"-C","--config"}, description = "Application configuration setting (may be used multiple times)", required = false)
    private Map<String, String> configSettings = new HashMap<String, String>();

    @Parameter(names = {"--jobname"}, description = "Specifies the name of the job", required = false)
    private String jobName=null;

    @Parameter(names = {"-J","--jobgroup"}, description = "Specifies the job group", required = false)
    private String jobGroup=null;

    @Parameter(names = {"-w","--preview"}, description = "Indicates to preview the job submission and produce results as a job configuration overlay json file", required=false)
    private boolean preview = false;

    @Parameter(names = {"-q","--out_jobConfig"}, description = "Specifies the name of the output file in which the command writes the operator configuration information.", required=false,
        converter=com.beust.jcommander.converters.FileConverter.class)
    private File jobConfigOutFile=null;

    public SubmitJob() {
    } 

    @Override
    public String getName() {
        return (Constants.CMD_SUBMITJOB);
    }

    @Override
    public String getHelp() {
        return (Constants.DESC_SUBMITJOB);
    }

    @Override
    protected CommandResult doExecute() {
        try {

            // Work around until jcommander 1.74 available in maven
            if ((sabFileArgumentString != null) && (!sabFileArgumentString.isEmpty())) {
                LOGGER.debug("Attempting to convert sabFileArgumentString ({}) to file...", sabFileArgumentString);

                FileConverter fileConverter = new FileConverter();
                sabFileArgument = fileConverter.convert(sabFileArgumentString);
            }

            LOGGER.debug("Checking that a sab file was specified and only by one argument...", sabFileArgumentString);

            if ((sabFileArgument != null) && (sabFile != null)) {
                throw new ParameterException("The following options are mutually exclusive: {[-f,--file <file-name>] | [<sabFileArgument>]}");
            }

            if ((sabFileArgument == null) && (sabFile == null)) {
                throw new ParameterException("A required option or argument was not specified. Specify one of the following options or arguments: {[-f,--file <file-name>] | [<sabFileArgument>]}");
            }

            // Check preview options and what is allowed
            if (preview) {
                LOGGER.debug("About to set preview file");

                // Future check that --outfile is not used (and --file when we move to be exactly like streamtool)
                if (jobConfigOutFile == null) {
                    String[] fileParts = StringUtils.split(sabFile.getName(),".");
                    Path currentRelativePath = Paths.get("");
                    Path curPath = currentRelativePath.toAbsolutePath();
                    jobConfigOutFile = new File(curPath.toFile(),fileParts[0] + "_config.json");
                }
                LOGGER.debug("preview outputfile: {}", jobConfigOutFile.toString());
            } else {
                // Cannot specify --out)_jobConfig

            }

            LOGGER.debug("...Finished argument checking.", sabFileArgumentString);

            File theSabFile = (sabFile != null ? sabFile : sabFileArgument);


            ObjectMapper mapper = new ObjectMapper();
            ObjectNode jsonOut = mapper.createObjectNode();
            
            InstanceMXBean instance = getInstanceMXBean();
    
            // ContentTransfer
            // URI 0: http for app bundle
            // URI 1: http for job config overlay
            // URI 2: http put signal complete
            ContentTransfer deployInformation = instance.deployApplication(theSabFile.getName());

            //Put Job Configuration Overlay if provided
            if (jobConfigFile != null) {
                LOGGER.debug("Sending job config overlay file to HTTP Server");
                getJmxServiceContext().getWebClient().putFile(deployInformation.getUris().get(1),
                    "application/json", jobConfigFile, getConfig().getJmxHttpHost(), getConfig().getJmxHttpPort());
            }

            //Put sab file to http endpoint
            LOGGER.debug("Sending .sab file to HTTP Server");
            getJmxServiceContext().getWebClient().putFile(deployInformation.getUris().get(0),
                "application/x-jar", (sabFile != null ? sabFile : sabFileArgument), getConfig().getJmxHttpHost(), getConfig().getJmxHttpPort());

            //Get operational listener
            ObjectName opListenerName = instance.createOperationListener();
            //OperationListenerMXBean opListener = 
            OperationListenerMXBean opListener = JMX.newMXBeanProxy(
                getBeanSource().getMBeanServerConnection(), opListenerName,
                OperationListenerMXBean.class, true);

            /**** TESTING GETTING BACK THE JCO */
            if (preview) {
                // Preview the job
                String jcoUri = instance.submitJob(
                    deployInformation.getId(),
                    params,
                    true, // Just a preview
                    jobGroup,
                    jobName,
                    opListener.getId() // listenerId
                    );
                LOGGER.debug("Retrieving preview from uri: {}, override host: {}, override port: {}",jcoUri, getConfig().getJmxHttpHost(), getConfig().getJmxHttpPort());
                // Replace this with writing out file
                //jsonOut.put("preview", getJmxServiceContext().getWebClient().get(jcoUri, getConfig().getJmxHttpHost(), getConfig().getJmxHttpPort()));
                String previewJsonString = getJmxServiceContext().getWebClient().get(jcoUri, getConfig().getJmxHttpHost(), getConfig().getJmxHttpPort());

                // Test parsing the json
                ObjectMapper previewMapper = new ObjectMapper();
                ObjectNode previewJson = (ObjectNode)previewMapper.readTree(previewJsonString);
                ObjectWriter previewWriter = previewMapper.writer(new DefaultPrettyPrinter());
                previewWriter.writeValue(jobConfigOutFile, previewJson);
                jsonOut.put("previewFile",jobConfigOutFile.getAbsolutePath());

                // Test removing results for later
                //previewJson.remove("results");
                //jsonOut.put("preview",previewJsonString);
                //jsonOut.put("modified",previewJson.toString());

            } else {
                // Submit the job    
                jsonOut.put("jobId", instance.submitJob(
                    deployInformation.getId(),
                    params,
                    false, // Not a preview
                    //configSettings, -- NOT SURE WHAT HAPPENED TO THEM in v5
                    //override, -- No more override resource load protection in v5
                    jobGroup,
                    jobName,
                    opListener.getId() // listenerId
                    ));
            }

            for (OperationStatusMessage osm : opListener.getMessages()) {
                System.out.println(osm.getMessage());
            }

            return new CommandResult(jsonOut.toString());
        } catch (Exception e) {
            LOGGER.debug("SubmitJob caught Exception: " + e.toString());
            e.printStackTrace();
            return new CommandResult(ExitStatus.FAILED_COMMAND, null, e.getLocalizedMessage());
        }
    }
}
