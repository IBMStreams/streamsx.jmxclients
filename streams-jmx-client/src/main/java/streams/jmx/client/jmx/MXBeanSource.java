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

package streams.jmx.client.jmx;

import java.math.BigInteger;

import javax.management.MBeanServerConnection;

import com.ibm.streams.management.domain.DomainMXBean;
import com.ibm.streams.management.domain.DomainServiceMXBean;
import com.ibm.streams.management.instance.InstanceMXBean;
import com.ibm.streams.management.instance.InstanceServiceMXBean;
import com.ibm.streams.management.job.JobMXBean;
import com.ibm.streams.management.job.OperatorMXBean;
import com.ibm.streams.management.job.OperatorInputPortMXBean;
import com.ibm.streams.management.job.OperatorOutputPortMXBean;
import com.ibm.streams.management.job.PeInputPortMXBean;
import com.ibm.streams.management.job.PeMXBean;
import com.ibm.streams.management.job.PeOutputPortMXBean;
import com.ibm.streams.management.resource.ResourceMXBean;

/**
 * Represents a source of MXBean instances.
 */
public interface MXBeanSource {

    /**
     * Gets the MBeanServerConnection
     */

    MBeanServerConnection getMBeanServerConnection();

    /**
     * Gets a job management bean.
     * 
     * @param domainId
     *            the domain to which the job was submitted.
     * @param instanceId
     *            the instance to which the job was submitted.
     * @param jobId
     *            the ID that was assigned to the Job after submission.
     * @return the JobMXBean
     */
    JobMXBean getJobBean(String domainId, String instanceId, BigInteger jobId);

    /**
     * Gets a Processing Element management bean.
     * 
     * @param domainId
     *            the domain in which the processing element exists.
     * @param instanceId
     *            the instance in which the processing element exists.
     * @param peId
     *            the ID that was assigned to the PE during job submission.
     * @return the PeMXBean
     */
    PeMXBean getPeBean(String domainId, String instanceId, BigInteger peId);

    /**
     * Gets a Domain management bean.
     * 
     * @param domainId
     *            the name of the domain.
     * @return the DomainMXBean
     */
    DomainMXBean getDomainBean(String domainId);

    /**
     * Gets an Instance management bean.
     * 
     * @param domainId
     *            the name of the domain in which the instance exists.
     * @param instanceId
     *            the name of the instance.
     * @return the InstanceMXBean
     */
    InstanceMXBean getInstanceBean(String domainId, String instanceId);

    /**
     * Gets a Resource management bean.
     * 
     * @param domainId
     *            the name of the domain to which the resource belongs.
     * @param resourceId
     *            the name of the resource.
     * @return the ResourceMXBean
     */
    ResourceMXBean getResourceBean(String domainId, String resourceId);

    /**
     * Gets a Domain Service management bean.
     * 
     * @param domainId
     *            the name of the domain containing the service.
     * @param serviceType
     *            the type of domain service
     * @return the DomainServiceMXBean
     */
    DomainServiceMXBean getDomainServiceBean(String domainId,
            DomainServiceMXBean.Type serviceType);

    /**
     * Gets an Instance Service management bean.
     * 
     * @param domainId
     *            the name of the domain containing the service.
     * @param instanceId
     *            the name of the instance containing the service.
     * @param serviceType
     *            the type of instance service
     * @return the InstanceServiceMXBean
     */
    InstanceServiceMXBean getInstanceServiceMXBean(String domainId,
            String instanceId, InstanceServiceMXBean.Type serviceType);


    /**
     * Gets an Operator management bean.
     * 
     * @param domainId the name of the domain containing the service.
     * @param instanceId the name of the instance
     * @param jobId the id of the job that includes the operator
     * @param operator the name of the operator
     * @return the OperatorMXBean
     */
    OperatorMXBean getOperatorMXBean(String domainId, String instanceId,
            BigInteger jobId, String operator);
    
    /**
     * Gets an Operator Input Port management bean.
     * 
     * @param domainId
     *            the name of the domain containing the job that includes the
     *            operator and input port.
     * @param instanceId
     *            the name of the instance containing the job that includes the
     *            operator and input port.
     * @param jobId
     *            the id of the job that includes the operator and input port
     * @param operator
     *            the name of the operator
     * @param indexWithinOperator
     *            the index of the input port within the operator
     * @return the OperatorInputPortMXBean
     */
    OperatorInputPortMXBean getOperatorInputPortMXBean(String domainId,
            String instanceId, BigInteger jobId, String operator,
            int indexWithinOperator);

    /**
     * Gets an Operator Output Port management bean.
     * 
     * @param domainId
     *            the name of the domain containing the job that includes the
     *            operator and output port.
     * @param instanceId
     *            the name of the instance containing the job that includes the
     *            operator and output port.
     * @param jobId
     *            the id of the job that includes the operator and output port
     * @param operator
     *            the name of the operator
     * @param indexWithinOperator
     *            the index of the output port within the operator
     * @return the OperatorOutputPortMXBean
     */
    OperatorOutputPortMXBean getOperatorOutputPortMXBean(String domainId,
            String instanceId, BigInteger jobId, String operator,
            int indexWithinOperator);

    /**
     * Gets a PE Input Port management bean.
     * 
     * @param domainId
     *            the name of the domain containing the pe that includes the
     *            input port
     * @param instanceId
     *            the name of the instance containing the pe that includes the
     *            input port
     * @param peId
     *            the id of the PE that includes the input port
     * @param indexWithinPe
     *            the index of the input port within the PE
     */
    PeInputPortMXBean getPeInputPortMXBean(String domainId, String instanceId,
            BigInteger peId, int indexWithinPe);

    /**
     * Gets a PE Output Port management bean.
     * 
     * @param domainId
     *            the name of the domain containing the pe that includes the
     *            output port
     * @param instanceId
     *            the name of the instance containing the pe that includes the
     *            output port
     * @param peId
     *            the id of the PE that includes the output port
     * @param indexWithinPe
     *            the index of the output port within the PE
     */
    PeOutputPortMXBean getPeOutputPortMXBean(String domainId,
            String instanceId, BigInteger peId, int indexWithinPe);
}
