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

package streams.metric.exporter.streamstracker.instance;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import streams.metric.exporter.streamstracker.StreamsDomainTracker;

/*
 * InstanceMap
 * 
 * Map and Index to keep track of Instances we are monitoring.
 * 
 */
public class InstanceMap {
	private static final Logger LOGGER = LoggerFactory.getLogger("root." + StreamsDomainTracker.class.getName());

	private String streamsDomainName = null;
    /*****************************************
     * INSTANCE MAP
     **************************************/
    /* Job Map Info */
    private ConcurrentSkipListMap<String, InstanceInfo> instanceInfoMap = new ConcurrentSkipListMap<String, InstanceInfo>();

//    static final Gauge streams_instance_jobcount = Gauge.build()
//    		.name("streams_instance_jobcount").help("Number of jobs in streams instance").register();

    public InstanceMap(String streamsDomainName) {
    	LOGGER.trace("JobMap Initialization");
    	this.streamsDomainName = streamsDomainName;
    }
	
    public synchronized void clear() {
    	LOGGER.trace("InstanceMap.clear()");
        Iterator<Map.Entry<String, InstanceInfo>> it = instanceInfoMap
                .entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, InstanceInfo> pair = it.next();
            InstanceInfo curInfo = (InstanceInfo) pair.getValue();
            curInfo.close(); 
        }
    	instanceInfoMap.clear();
    }
    
    public synchronized int size() {
    	return instanceInfoMap.size();
    }
    
//    public synchronized Map<String, InstanceInfo> getInstanceMap() {
//        HashMap<String, InstanceInfo> m = new HashMap<String, InstanceInfo>();
//
//        Iterator<Map.Entry<BigInteger, InstanceInfo>> it = instanceInfoMap.entrySet()
//                .iterator();
//
//        while (it.hasNext()) {
//            Map.Entry<String, InstanceInfo> entry = it.next();
//
//            m.put(entry.getKey(), entry.getValue().getInstanceInfo());
//        }
//
//        return m;
//    }
    
 
    
    public synchronized InstanceInfo getInstance(int jobid) {
        BigInteger jid = BigInteger.valueOf(jobid);

        return instanceInfoMap.get(jid);
    }

//    /* Return job info of each job in the map */
//	public synchronized ArrayList<JobInfo> getJobInfo() {
//		ArrayList<JobInfo> jia = new ArrayList<JobInfo>();
//
//		Iterator<Map.Entry<BigInteger, JobDetails>> it = instanceInfoMap.entrySet().iterator();
//		while (it.hasNext()) {
//			Map.Entry<BigInteger, JobDetails> pair = it.next();
//
//			JobDetails curInfo = (JobDetails) pair.getValue();
//			jia.add(curInfo.getJobInfo());
//		}
//
//		return jia;
//	}
	
//	/* Return job info of a specific job in the map */
//    public synchronized JobInfo getJobInfo(int jobid)  {
//        BigInteger jid = BigInteger.valueOf(jobid);
//        JobDetails jd = null;
//        JobInfo ji = null;
//
//        jd = instanceInfoMap.get(jid);
//        if (jd != null) {
//        	ji = jd.getJobInfo();
//        }
//
//        return ji;
//    }
    


    public synchronized void addInstanceToMap(String instanceName, InstanceInfo instanceInfo) {

		instanceInfoMap.put(instanceName, instanceInfo);
	}

    public synchronized void removeInstanceFromMap(String instanceName) {
    	if (instanceInfoMap.containsKey(instanceName)) {
            instanceInfoMap.get(instanceName).close();
        }
        instanceInfoMap.remove(instanceName);
    }
    
//    public synchronized void setJobMetricsFailed(Date failureDate) {
//        Iterator<Map.Entry<BigInteger, JobDetails>> it = instanceInfoMap
//                .entrySet().iterator();
//        while (it.hasNext()) {
//            Map.Entry<BigInteger, JobDetails> pair = it.next();
//            JobDetails curInfo = (JobDetails) pair.getValue();
//            curInfo.setLastMetricsFailure(failureDate);
//            curInfo.setLastMetricsRefreshFailed(true); 
//        }
//    }
//    
//    public synchronized void setJobSnapshotFailed(Date failureDate) {
//        Iterator<Map.Entry<BigInteger, JobDetails>> it = instanceInfoMap
//                .entrySet().iterator();
//        while (it.hasNext()) {
//            Map.Entry<BigInteger, JobDetails> pair = it.next();
//            JobDetails curInfo = (JobDetails) pair.getValue();
//            curInfo.setLastSnapshotFailure(failureDate);
//            curInfo.setLastSnapshotRefreshFailed(true); 
//        }
//    }    
    
//    @Override
//    public String toString() {  
//        StringBuilder result = new StringBuilder();
//        String newline = System.getProperty("line.separator");
//        
//        result.append("All " + instanceInfoMap.size() + " Jobs:");
//        result.append(newline);
//        Iterator<Map.Entry<BigInteger, JobDetails>> it = instanceInfoMap.entrySet()
//                .iterator();
//
//        while (it.hasNext()) {
//            Map.Entry<BigInteger, JobDetails> pair = it.next();
//            JobDetails curInfo = (JobDetails) pair.getValue();
//            result.append(curInfo.toString());
//            result.append(newline);
//        }
//        result.append(newline);
//        result.append("jobNameIndex:");
//        result.append(newline);
//        Iterator<Map.Entry<String, BigInteger>> jnit = jobNameIndex
//                .entrySet().iterator();
//        while (jnit.hasNext()) {
//            Map.Entry<String, BigInteger> pair = jnit.next();
//            result.append(pair.getKey() + " : " + pair.getValue());
//            result.append(newline);
//        }
//        return result.toString();
//    }
}
