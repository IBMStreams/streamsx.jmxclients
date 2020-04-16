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

package streams.metric.exporter.streamstracker;

import com.ibm.streams.management.domain.DomainMXBean;

import streams.metric.exporter.error.StreamsTrackerException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

/* 
 * DomainInfo
 * Interface class used to retrieve information about the
 * Streams domain and convert these to proper output format
 * including JSON.
 */
public class DomainInfo {
    @JsonIgnore
    private static final Logger LOGGER = LoggerFactory.getLogger("root."
            + StreamsDomainTracker.class.getName());
    
    private String name = null;
    private String status = DomainMXBean.Status.UNKNOWN.toString();
    private String fullProductVersion = null;
	private Long creationTime = null;
	private Long startTime=null;
    private String creationUser = null;
    private String externalResourceManager = null;
    private int highAvailabilityCount = 0;
    private java.util.Set<String> instances = null;
    private java.util.Set<String> resources = null;
    
    
    /*
     * Constructor
     */
    public DomainInfo(String domainName) throws StreamsTrackerException {
        LOGGER.trace("Constructing DomainInfo");
        this.name = domainName;
    }
    
    public void updateInfo(DomainMXBean bean) throws StreamsTrackerException {
    	try {
	    	setStatus(bean.getStatus().toString());
			setFullProductVersion(bean.getActiveVersion().getFullProductVersion());
			setStartTime(bean.getStartTime());
			setCreationTime(bean.getCreationTime());
	    	setCreationUser(bean.getCreationUser());
	    	setExternalResourceManager(bean.getExternalResourceManager());
	    	setHighAvailabilityCount(bean.getHighAvailabilityCount());
	    	setInstances(bean.getInstances());
	    	setResources(bean.getResources());
    	} catch (Exception e) {
			LOGGER.error("Exception setting the Domain Info using the Domain JMX Bean: {}",e.getLocalizedMessage());
    		throw new StreamsTrackerException("Exception setting the Domain Info using Domain JMX Bean: " + e.getLocalizedMessage(),e);
    	}
    }

    public String getName() {
        return this.name;
    }

    public void setName(String domainName) {
        this.name = domainName;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    

    
    public String getFullProductVersion() {
		return fullProductVersion;
	}

	public void setFullProductVersion(String fullProductVersion) {
		this.fullProductVersion = fullProductVersion;
	}

	public Long getCreationTime() {
		return creationTime;
	}

	public void setCreationTime(long creationTime) {
		this.creationTime = creationTime;
	}

	public Long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public String getCreationUser() {
		return creationUser;
	}

	public void setCreationUser(String creationUser) {
		this.creationUser = creationUser;
	}

	public String getExternalResourceManager() {
		return externalResourceManager;
	}

	public void setExternalResourceManager(String externalResourceManager) {
		this.externalResourceManager = externalResourceManager;
	}

	public int getHighAvailabilityCount() {
		return highAvailabilityCount;
	}

	public void setHighAvailabilityCount(int highAvailabilityCount) {
		this.highAvailabilityCount = highAvailabilityCount;
	}

	public java.util.Set<String> getInstances() {
		return instances;
	}

	public void setInstances(java.util.Set<String> instances) {
		this.instances = instances;
	}

	public java.util.Set<String> getResources() {
		return resources;
	}

	public void setResources(java.util.Set<String> resources) {
		this.resources = resources;
	}

	public void close(){
		setStatus(DomainMXBean.Status.UNKNOWN.toString());
	}
}
