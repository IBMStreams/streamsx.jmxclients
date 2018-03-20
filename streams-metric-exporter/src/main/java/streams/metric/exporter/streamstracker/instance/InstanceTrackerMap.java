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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import streams.metric.exporter.streamstracker.StreamsDomainTracker;

/*
 * InstanceTrackerMap
 * 
 * Map of StreamsInstanceTrackers.
 * 
 */
public class InstanceTrackerMap {
	private static final Logger LOGGER = LoggerFactory.getLogger("root." + StreamsDomainTracker.class.getName());

    /*****************************************
     * INSTANCE MAP
     **************************************/
    /* Instance Map Info */
    private ConcurrentSkipListMap<String, StreamsInstanceTracker> instanceTrackerMap = new ConcurrentSkipListMap<String, StreamsInstanceTracker>();

    
    /****************************************
     * CONSTRUCTOR
     ****************************************/
    public InstanceTrackerMap() {
    		LOGGER.trace("InstanceTrackerMap Initialization");
    }
 
    /****************************************
     * ADD and REMOVE
     ****************************************/

    public synchronized void addInstanceTrackerToMap(String instanceName, StreamsInstanceTracker instanceTracker) {

		instanceTrackerMap.put(instanceName, instanceTracker);
	}

    public synchronized void removeInstanceTrackerFromMap(String instanceName) {
    	if (instanceTrackerMap.containsKey(instanceName)) {
            instanceTrackerMap.get(instanceName).close();
        }
        instanceTrackerMap.remove(instanceName);
    }
	
    /****************************************
     * CLEAR
     ****************************************/
    public synchronized void clear() {
    	LOGGER.trace("InstanceTrackerMap.clear()");
        Iterator<Map.Entry<String, StreamsInstanceTracker>> it = instanceTrackerMap
                .entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, StreamsInstanceTracker> pair = it.next();
            StreamsInstanceTracker curTracker = (StreamsInstanceTracker) pair.getValue();
            curTracker.close(); 
        }
    	instanceTrackerMap.clear();
    }
    
    /*****************************************
     * SIZE
     *****************************************/
    public synchronized int size() {
    		return instanceTrackerMap.size();
    }
    
    /*****************************************
     * GET MAP
     *****************************************/
    public synchronized Map<String, StreamsInstanceTracker> getMap() {
        HashMap<String, StreamsInstanceTracker> m = new HashMap<String, StreamsInstanceTracker>();

        Iterator<Map.Entry<String, StreamsInstanceTracker>> it = instanceTrackerMap.entrySet()
                .iterator();

        while (it.hasNext()) {
            Map.Entry<String, StreamsInstanceTracker> entry = it.next();

            m.put(entry.getKey(), entry.getValue());
        }

        return m;
    }
   
    /*******************************************************
     * GET METHODS
     *******************************************************/
    public synchronized StreamsInstanceTracker getInstanceTracker(String instanceName) {

        return instanceTrackerMap.get(instanceName);
    }
    
    public synchronized Set<String> getInstanceNames() {
    		return instanceTrackerMap.keySet();
    }

    /* Return instance info of each StreamsInstanceTracker in the map */
	public synchronized ArrayList<InstanceInfo> getInstanceInfo() {
		ArrayList<InstanceInfo> instanceInfoArray = new ArrayList<InstanceInfo>();

		Iterator<Map.Entry<String, StreamsInstanceTracker>> it = instanceTrackerMap.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, StreamsInstanceTracker> pair = it.next();

			StreamsInstanceTracker curTracker = (StreamsInstanceTracker) pair.getValue();
			instanceInfoArray.add(curTracker.getInstanceInfo());
		}

		return instanceInfoArray;
	}
	
	/* Return Instance info of a specific instance in the map */
    public synchronized InstanceInfo getInstanceInfo(String instanceName)  {
    		StreamsInstanceTracker sit = null;
        InstanceInfo ii = null;

        sit = instanceTrackerMap.get(instanceName);
        if (sit != null) {
        	ii = sit.getInstanceInfo();
        }

        return ii;
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
    
}
