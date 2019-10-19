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

import javax.management.MBeanServerConnection;

import com.ibm.streams.management.instance.InstanceMXBean;
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
     * @param instanceId
     *            the instance to which the job was submitted.
     * @param jobId
     *            the ID that was assigned to the Job after submission.
     * @return the JobMXBean
     */
    JobMXBean getJobBean(String instanceId, String jobId);

    /**
     * Gets a Processing Element management bean.
     * 
     * @param instanceId
     *            the instance in which the processing element exists.
     * @param peId
     *            the ID that was assigned to the PE during job submission.
     * @return the PeMXBean
     */
    PeMXBean getPeBean(String instanceId, String peId);


    /**
     * Gets an Instance management bean.
     * 
     * @param instanceId
     *            the name of the instance.
     * @return the InstanceMXBean
     */
    InstanceMXBean getInstanceBean(String instanceId);

    /**
     * Gets a Resource management bean.
     * 
     * @param instanceId
     *            the name of the instance to which the resource belongs.
     * @param resourceId
     *            the name of the resource.
     * @return the ResourceMXBean
     */
    ResourceMXBean getResourceBean(String instanceId, String resourceId);



    /**
     * Gets an Operator management bean.
     * 
     * @param instanceId the name of the instance
     * @param jobId the id of the job that includes the operator
     * @param operator the name of the operator
     * @return the OperatorMXBean
     */
    OperatorMXBean getOperatorMXBean(String instanceId,
            String jobId, String operator);
    
    /**
     * Gets an Operator Input Port management bean.
     * 
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
    OperatorInputPortMXBean getOperatorInputPortMXBean(
            String instanceId, String jobId, String operator,
            int indexWithinOperator);

    /**
     * Gets an Operator Output Port management bean.
     * 
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
    OperatorOutputPortMXBean getOperatorOutputPortMXBean(
            String instanceId, String jobId, String operator,
            int indexWithinOperator);

    /**
     * Gets a PE Input Port management bean.
     * 
     * @param instanceId
     *            the name of the instance containing the pe that includes the
     *            input port
     * @param peId
     *            the id of the PE that includes the input port
     * @param indexWithinPe
     *            the index of the input port within the PE
     */
    PeInputPortMXBean getPeInputPortMXBean(String instanceId,
            String peId, int indexWithinPe);

    /**
     * Gets a PE Output Port management bean.
     * 
     * @param instanceId
     *            the name of the instance containing the pe that includes the
     *            output port
     * @param peId
     *            the id of the PE that includes the output port
     * @param indexWithinPe
     *            the index of the output port within the PE
     */
    PeOutputPortMXBean getPeOutputPortMXBean(
            String instanceId, String peId, int indexWithinPe);
}
