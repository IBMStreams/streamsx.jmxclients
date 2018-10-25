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
import streams.jmx.client.jmx.JmxServiceContext;
import streams.jmx.client.ServiceConfig;
import streams.jmx.client.error.StreamsClientErrorCode;
import streams.jmx.client.error.StreamsClientException;

import java.lang.reflect.UndeclaredThrowableException;

import javax.management.InstanceNotFoundException;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.ibm.streams.management.domain.DomainMXBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class AbstractDomainCommand extends AbstractJmxCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger("root."
            + AbstractDomainCommand.class.getName());

    @Parameter(names = { "-d", "--domain-id" }, description = Constants.DESC_DOMAIN_ID, required = false)
    private String domainName = ServiceConfig.getEnvDefault(Constants.ENV_DOMAIN_ID,Constants.DEFAULT_DOMAIN_ID);

    private DomainMXBean domainMXBean = null;

    public AbstractDomainCommand() {

    }

    @Override
    protected final void prepareJmxExecution() throws Exception {
        LOGGER.trace("In prepareJmxExecution...");
        if (getDomainName() == null) {
		    throw new ParameterException(
		        "Streams domain name must be specified.  Please use parameter (-d or --domain-id) or environment variable: " + Constants.ENV_DOMAIN_ID);
        }
        try {
            LOGGER.trace("About to getBeanSource().getDomainBean(\"{}\")",getDomainName());
            domainMXBean = getBeanSource().getDomainBean(getDomainName());
            // Set domainname by accessing bean.  Only way to get exceptions raised if it was not found
            setDomainName(domainMXBean.getName());
        } catch (UndeclaredThrowableException e) {
            LOGGER.trace("Caught UndeclaredThrowableException");
            // Some InstanceNotFoundExceptions are wrapped in
            // UndeclaredThrowableExceptions sadly
			Throwable t = e.getUndeclaredThrowable();
			if (t instanceof InstanceNotFoundException) {
				LOGGER.error(
						"Domain '{}' not found.  Ensure the JMX URL specified is for the domain you are attempting to connect to.",
						this.getDomainName());
				throw new StreamsClientException(StreamsClientErrorCode.DOMAIN_NOT_FOUND,
						"Domain name " + this.domainName + " does not match the domain of the JMX Server.", e);
			} else {
				LOGGER.trace("Unexpected exception (" + t.getClass()
						+ ") when retrieving Streams domain information from JMX Server, throwing original undeclarable...");
				throw e;
			}

        } catch (InstanceNotFoundException infe) {
            LOGGER.error(
                    "Domain '{}' not found.  Ensure the JMX URL specified is for the domain you are attempting to connect to.",
                    this.getDomainName());
			throw new StreamsClientException(StreamsClientErrorCode.DOMAIN_NOT_FOUND,
					"Domain name " + this.domainName + " does not match the domain of the JMX Server.", infe);
        }
        LOGGER.trace("About to call prepareJmxDomainExecution()");
        prepareJmxDomainExecution();
            
    }

    // Override in decendent class
    protected void prepareJmxDomainExecution() throws Exception {}

    protected final DomainMXBean getDomainMXBean() throws Exception {
        //return getBeanSource().getDomainBean(getDomainName());
        return domainMXBean;
    }

    public String getDomainName() {
        return domainName;
    }

    private void setDomainName(String domainName) {
        this.domainName = domainName;
    }
}