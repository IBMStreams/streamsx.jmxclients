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
import streams.jmx.client.error.StreamsClientErrorCode;
import streams.jmx.client.error.StreamsClientException;

import java.lang.reflect.UndeclaredThrowableException;

import javax.management.InstanceNotFoundException;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.ibm.streams.management.instance.InstanceMXBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class AbstractInstanceCommand extends AbstractDomainCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger("root."
            + AbstractInstanceCommand.class.getName());

    @Parameter(names = { "-i", "--instance-id" }, description = Constants.DESC_INSTANCE_ID, required = false)
    private String instanceName = ServiceConfig.getEnvDefault(Constants.ENV_INSTANCE_ID,Constants.DEFAULT_INSTANCE_ID);

    private InstanceMXBean instanceMXBean;


    public AbstractInstanceCommand() {

    }

    @Override
    protected final void prepareJmxDomainExecution() throws Exception {
        LOGGER.trace("In prepareJmxDomainExecution...");
        if (getInstanceName() == null) {
		    throw new ParameterException(
		        "Streams instance name must be specified.  Please use parameter (-i or --instance-id) or environment variable: " + Constants.ENV_INSTANCE_ID);
        }

        try {
            instanceMXBean = getBeanSource().getInstanceBean(getDomainName(),getInstanceName());
            // Set name by accessing bean.  Only way to get exceptions raised if it was not found
            setInstanceName(instanceMXBean.getName());
        } catch (UndeclaredThrowableException e) {
            Throwable t = e.getUndeclaredThrowable();
            if (t instanceof InstanceNotFoundException) {
                LOGGER.warn(
                        "Instance '{}' not found in the following domain: {}",
                        getInstanceName(), getDomainName());
                throw new StreamsClientException(StreamsClientErrorCode.INSTANCE_NOT_FOUND,
                "The " + this.instanceName + " instance does not exist for the following domain: " + getDomainName(), e);
            } else {
                LOGGER.trace("Unexpected exception ("
                        + t.getClass()
                        + ") when retrieving instance bean from the JMX server, throwing original undeclarable...");
                throw e;
            }
            // Some InstanceNotFoundExceptions are wrapped in
            // UndeclaredThrowableExceptions sadly

        } catch (InstanceNotFoundException infe) {
            LOGGER.warn(
                "Instance '{}' not found in the following domain: {}",
                getInstanceName(), getDomainName());
            throw new StreamsClientException(StreamsClientErrorCode.INSTANCE_NOT_FOUND,
            "The " + this.instanceName + " instance does not exist for the following domain: " + getDomainName(), infe);
        }

        prepareJmxInstanceExecution();
    }

    // Override in decendent class
    protected void prepareJmxInstanceExecution() throws Exception {}

    protected final InstanceMXBean getInstanceMXBean() throws Exception {
        //return getBeanSource().getInstanceBean(getDomainName(),getInstanceName());
        return instanceMXBean;
    }

    public String getInstanceName() {
        return instanceName;
    }

    private void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }
}