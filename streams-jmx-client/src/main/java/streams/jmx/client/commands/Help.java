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
import com.beust.jcommander.Parameters;
import com.beust.jcommander.Parameter;

@Parameters(commandDescription = Constants.DESC_HELP)
public class Help implements Command {
    @Parameter(description = "Command to get help information for", required=false)
    private String helpCommand = "";

    public String getName() {
        return Constants.CMD_HELP;
    }

    public String getHelp() {
            return Constants.DESC_HELP;
    }

    public CommandResult execute() {
            return new CommandResult("This should have been handled before execution");
    }

    public String getHelpCommand() {
        return this.helpCommand;
    }

    public void clearHelpCommand() {
        this.helpCommand = null;
    }

}