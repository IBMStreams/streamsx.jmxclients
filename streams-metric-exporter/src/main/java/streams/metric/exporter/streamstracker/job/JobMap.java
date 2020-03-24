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

package streams.metric.exporter.streamstracker.job;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import streams.metric.exporter.streamstracker.StreamsDomainTracker;

/*
 * JobMap
 * 
 * Map and Index to keep track of current jobs in IBM Streams Instance.
 * 
 */
public class JobMap {
	private static final Logger LOGGER = LoggerFactory.getLogger("root." + StreamsDomainTracker.class.getName());

	@SuppressWarnings("unused")
	private String streamsInstanceName = null;
	/*****************************************
	 * JOB MAP and INDEXES
	 **************************************/
	/* Job Map Info */
	// <jobid,jobdetails>
	private ConcurrentSkipListMap<String, JobDetails> jobDetailsMap = new ConcurrentSkipListMap<String, JobDetails>();
	// <jobname, jobid>
	private ConcurrentSkipListMap<String, String> jobNameIndex = new ConcurrentSkipListMap<String, String>();


	public JobMap(String streamsInstanceName) {
		LOGGER.trace("JobMap Initialization");
		this.streamsInstanceName = streamsInstanceName;
	}

	// Clear out the job map
	public synchronized void clear() {
		LOGGER.trace("JobMap.clear()");
		Iterator<Map.Entry<String, JobDetails>> it = jobDetailsMap.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, JobDetails> pair = it.next();
			JobDetails curInfo = (JobDetails) pair.getValue();
			curInfo.close();
		}
		jobDetailsMap.clear();
		jobNameIndex.clear();
	}

	public synchronized int size() {
		return jobDetailsMap.size();
	}

	/* Return a map using jobInfo objects rather than all details */
	public synchronized Map<String, JobInfo> getJobMap() {
		HashMap<String, JobInfo> m = new HashMap<String, JobInfo>();

		Iterator<Map.Entry<String, JobDetails>> it = jobDetailsMap.entrySet().iterator();

		while (it.hasNext()) {
			Map.Entry<String, JobDetails> entry = it.next();

			m.put(entry.getKey(), entry.getValue().getJobInfo());
		}

		return m;
	}

	/* Get list of job ids as a set */
	public synchronized Set<String> getJobIds() {
		return jobDetailsMap.keySet();
	}

	/* Return a copy of the job name index */
	public synchronized Map<String, String> getCurrentJobNameIndex() {
		return new HashMap<String, String>(jobNameIndex);
	}

	/* Return job details of a job */
	public synchronized JobDetails getJob(String jobid) {
		return jobDetailsMap.get(jobid);
	}

	/* Return job info of each job in the map */
	public synchronized ArrayList<JobInfo> getJobInfo() {
		ArrayList<JobInfo> jia = new ArrayList<JobInfo>();

		Iterator<Map.Entry<String, JobDetails>> it = jobDetailsMap.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, JobDetails> pair = it.next();

			JobDetails curInfo = (JobDetails) pair.getValue();
			jia.add(curInfo.getJobInfo());
		}

		return jia;
	}

	/* Return job info of a specific job in the map */
	public synchronized JobInfo getJobInfo(String jobid) {
		JobDetails jd = null;
		JobInfo ji = null;

		jd = jobDetailsMap.get(jobid);
		if (jd != null) {
			ji = jd.getJobInfo();
		}

		return ji;
	}

	// ** Add Job to job map
	public synchronized void addJobToMap(String jobid, JobDetails details) {
		LOGGER.debug("jobMap.addJobToMap: jobDetails: " + details.toString());
		jobDetailsMap.put(jobid, details);
		jobNameIndex.put(details.getJobname(), jobid);
	}

	public synchronized void removeJobFromMap(String jobid) {
		// Tell jobDetails handle going away, whatever it needs to do
		if (jobDetailsMap.containsKey(jobid)) {
			jobDetailsMap.get(jobid).close();
		}
		jobDetailsMap.remove(jobid);
		// Need to remove it from the jobNameIndex
		jobNameIndex.values().removeAll(Collections.singleton(jobid));
	}


	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		String newline = System.getProperty("line.separator");

		result.append("All " + jobDetailsMap.size() + " Jobs:");
		result.append(newline);
		Iterator<Map.Entry<String, JobDetails>> it = jobDetailsMap.entrySet().iterator();

		while (it.hasNext()) {
			Map.Entry<String, JobDetails> pair = it.next();
			JobDetails curInfo = (JobDetails) pair.getValue();
			result.append(curInfo.toString());
			result.append(newline);
		}
		result.append(newline);
		result.append("jobNameIndex:");
		result.append(newline);
		Iterator<Map.Entry<String, String>> jnit = jobNameIndex.entrySet().iterator();
		while (jnit.hasNext()) {
			Map.Entry<String, String> pair = jnit.next();
			result.append(pair.getKey() + " : " + pair.getValue());
			result.append(newline);
		}
		return result.toString();
	}
}
