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

import com.beust.jcommander.Parameters;

import java.util.Set;

import com.ibm.streams.management.instance.InstanceMXBean;
import com.ibm.streams.management.resource.ResourceMXBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Parameters(commandDescription = Constants.DESC_GETDOMAINSTATE)
public class GetInstanceState extends AbstractInstanceCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger("root."
            + GetInstanceState.class.getName());

    public GetInstanceState() {
    }

    @Override
    public String getName() {
        return (Constants.CMD_GETINSTANCESTATE);
    }

    @Override
    public String getHelp() {
        return (Constants.DESC_GETINSTANCESTATE);
    }

    @Override
    protected CommandResult doExecute() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode jsonOut = mapper.createObjectNode();

            InstanceMXBean instance = getInstanceMXBean();

            // Populate the result object
            jsonOut.put("instance",instance.getName());
            jsonOut.put("state",instance.getStatus().toString());    
    
            ArrayNode resourceArray = mapper.createArrayNode();

            for (String resourceId : instance.getResources()) {
                LOGGER.trace("Lookup up resource bean for resource: {} of instance: {}", resourceId, instance.getName());

                //get resource
                //ObjectName objName = ObjectNameBuilder.resource(domain.getName(),s);
                //ResourceMXBean resourceBean = JMX.newMXBeanProxy(getMBeanServerConnection(), objName, ResourceMXBean.class, true);
                    ResourceMXBean resourceBean = getBeanSource().getResourceBean(instance.getName(), resourceId);
                //json
                ObjectNode resourceObject = mapper.createObjectNode();
                resourceObject.put("resource",resourceId);
                resourceObject.put("status",resourceBean.getStatus().toString());
                resourceObject.put("schedulable",resourceBean.isApplicationResource());

                // Instance Services
                //List<InstanceServiceMXBean.Type> instanceServices = resourceBean.retrieveInstanceServices(getInstanceName());
                //ArrayNode serviceArray = mapper.valueToTree(instanceServices);


                ArrayNode serviceArray = mapper.createArrayNode();
                for (ResourceMXBean.ServiceType serviceType : resourceBean.getServices()) {
                    ResourceMXBean.ServiceStatus serviceStatus = resourceBean.getServiceStatus(serviceType);
                    ObjectNode serviceObject = mapper.createObjectNode();
                    serviceObject.put("service", serviceType.toString());
                    serviceObject.put("status", serviceStatus.toString());
                    serviceArray.add(serviceObject);
                }
            
                resourceObject.set("services",serviceArray);


                // Resource Tags
                Set<String> resourceTags = resourceBean.getTags();
                ArrayNode tagArray = mapper.valueToTree(resourceTags);
                resourceObject.set("tags",tagArray);

                resourceArray.add(resourceObject);
            }
            jsonOut.set("resources",resourceArray);
            //System.out.println(sb.toString());
            return new CommandResult(jsonOut.toString());
        } catch (Exception e) {
            System.out.println("GetInstanceState caught Exception: " + e.toString());
            e.printStackTrace();
            return new CommandResult(ExitStatus.FAILED_COMMAND, null, e.getLocalizedMessage());
        }
    }
}
