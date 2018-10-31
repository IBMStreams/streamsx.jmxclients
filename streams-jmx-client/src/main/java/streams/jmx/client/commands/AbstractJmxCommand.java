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

import streams.jmx.client.ExitStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import streams.jmx.client.jmx.JmxServiceContext;
import streams.jmx.client.ServiceConfig;

import streams.jmx.client.jmx.MXBeanSource;

public abstract class AbstractJmxCommand implements Command {
	private static final Logger LOGGER = LoggerFactory.getLogger("root." + AbstractJmxCommand.class.getName());

    private JmxServiceContext jmxServiceContext = null;
    private ServiceConfig config = null;

    public final void initialize(ServiceConfig config, JmxServiceContext jmxServiceContext) {
        this.config = config;
        this.jmxServiceContext = jmxServiceContext;
    }

    @Override
    public final CommandResult execute() {
        LOGGER.trace("calling prepareJmxExecution()");
        try {
            prepareJmxExecution();
            return doExecute();
        } catch (Throwable t) {
            //return throwableMapper.mapThrowable(t);
            return new CommandResult(ExitStatus.FAILED_COMMAND, null, t.getLocalizedMessage());
        }
    }

    // To support advanced argument processing (Environment variables) we set parameters to optional
    // on the abstract classes (AbstractDomainCommand, AbstractInstanceCommand, etc.), however,
    // in order to ensure that a value for something like domainname was actually set via arg or env var
    // we need to make a call down through the inheritence stack for the command to verify
    // parameters before executing the command.
    //
    // To verify commands each level overrides the prepare function from the level it inherits,
    // validates its parameters, and then calls the prepare for itself which can be overridden
    // if the abstract class is used as the basis for a follow-on abstractions.
    //
    // This could include the actual command to be implemented if it has its own parameters.
    //
    // Override in decendent class
    protected void prepareJmxExecution() throws Exception {
        System.out.println("****** WRONG prepareJmxExecution");
    }
    

    protected JmxServiceContext getJmxServiceContext() {
        return jmxServiceContext;
    }

    protected MXBeanSource getBeanSource() throws Exception {
        return jmxServiceContext.getBeanSourceProvider().getBeanSource();
    }

    protected ServiceConfig getConfig() {
        return config;
    }

    public String getHelp() {
        return "AbstractJmxCommand: No detailed help available.";
    }

    /**
     * Subclasses must implement this method to carry out the command's
     * specific functions.
     */
    protected abstract CommandResult doExecute() throws Exception;

    // private static class DefaultThrowableResultMapper implements ThrowableResultMapper {
    //     @Override
    //     public CommandResult mapThrowable(Throwable t) {
    //         return new CommandResult(ExitStatus.FAILED_COMMAND, null, t.getLocalizedMessage());
    //     }
    // }
}