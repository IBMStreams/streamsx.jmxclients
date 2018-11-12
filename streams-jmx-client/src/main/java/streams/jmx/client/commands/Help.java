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
import streams.jmx.client.ServiceConfig;

import com.beust.jcommander.Parameters;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

@Parameters(commandDescription = Constants.DESC_HELP)
public class Help implements Command {

    @Parameter(description = "Command to get help information for", required=false)
    private String helpCommand = "";

    private JCommander allCommands = null;

    public String getName() {
        return Constants.CMD_HELP;
    }

    public String getHelp() {
        return Constants.DESC_HELP;
    }

    public CommandResult execute() {

        StringBuilder sb = new StringBuilder();
        
        if (allCommands == null) {
            sb.append("Help command has not been initialized with all commands");
        } else if (helpCommand != null && !helpCommand.equals("")) {
            allCommands.usage(helpCommand,sb);
        } else {
            List<Object> objectList = this.allCommands.getObjects();
            // If this was on the primary command line
            if (objectList.size() > 0) {
                JCommander baseArguments = JCommander.newBuilder()
                    //.addObject(objectList.get(0))
                    .addObject(new ServiceConfig())

                    .addObject(new usageHelp())
                    .programName(Constants.PROGRAM_NAME)
                    .columnSize(132)
                    .build();
                baseArguments.usage(sb);
            }
            sb.append("\nClient Commands:\n\n");

            // Sort the commands
            TreeMap<String,JCommander> sortedCommandList = new TreeMap<String,JCommander>(allCommands.getCommands());


            sb.append(String.format("%-20s  %s\n","Command","Description"));
            sb.append(String.format("%-20s  %s\n","--------------------","-----------"));
            //for (Map.Entry<String, JCommander> entry :  jc.getCommands().entrySet()) {
            for (Map.Entry<String, JCommander> entry :  sortedCommandList.entrySet()) {
                Command commandObject = (Command)entry.getValue().getObjects().get(0);
                sb.append(String.format("%-20s  %s\n",entry.getKey(),commandObject.getHelp()));
            }
        }
        return new CommandResult(sb.toString());
    }

    public String getHelpCommand() {
        return this.helpCommand;
    }

    public void clearHelpCommand() {
        this.helpCommand = null;
    }

    public void setAllCommands(JCommander allCommands) {
        this.allCommands = allCommands;
    }

    private class usageHelp {
        @Parameter(description = "Command", required=false)
        private String command = null;
    }

}