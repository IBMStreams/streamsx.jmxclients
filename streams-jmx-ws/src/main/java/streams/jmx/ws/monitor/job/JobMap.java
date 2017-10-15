package streams.jmx.ws.monitor.job;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

import io.prometheus.client.Gauge;

/*
 * JobMap
 * 
 * Map and Index to keep track of current jobs in IBM Streams Instance.
 * 
 */
public class JobMap {

    /*****************************************
     * JOB MAP and INDEXES
     **************************************/
    /* Job Map Info */
    private ConcurrentSkipListMap<BigInteger, JobDetails> jobMap = new ConcurrentSkipListMap<BigInteger, JobDetails>();
    private ConcurrentSkipListMap<String, BigInteger> jobNameIndex = new ConcurrentSkipListMap<String, BigInteger>();

    // Prometheus Metrics
    static final Gauge jobCount = Gauge.build()
    		.name("jobCount").help("Number of jobs in streams instance").register();

	
    public synchronized void clear() {
    	jobMap.clear();
    	jobNameIndex.clear();
    	jobCount.clear();
    }
    
    public synchronized int size() {
    	return jobMap.size();
    }
    
    /* Return a map using jobInfo objects rather than all details */
    public synchronized Map<BigInteger, JobInfo> getJobMap() {
        HashMap<BigInteger, JobInfo> m = new HashMap<BigInteger, JobInfo>();

        Iterator<Map.Entry<BigInteger, JobDetails>> it = jobMap.entrySet()
                .iterator();

        while (it.hasNext()) {
            Map.Entry<BigInteger, JobDetails> entry = it.next();

            m.put(entry.getKey(), entry.getValue().getJobInfo());
        }

        return m;
    }
    
    /* Return a copy of the job name index */
    public synchronized Map<String, BigInteger> getCurrentJobNameIndex() {
        return new HashMap<String, BigInteger>(jobNameIndex);
    }
    
    /* Return job details of a job */
    public synchronized JobDetails getJob(int jobid) {
        BigInteger jid = BigInteger.valueOf(jobid);

        return jobMap.get(jid);
    }

    /* Return job info of each job in the map */
	public synchronized ArrayList<JobInfo> getJobInfo() {
		ArrayList<JobInfo> jia = new ArrayList<JobInfo>();

		Iterator<Map.Entry<BigInteger, JobDetails>> it = jobMap.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<BigInteger, JobDetails> pair = it.next();

			JobDetails curInfo = (JobDetails) pair.getValue();
			jia.add(curInfo.getJobInfo());
		}

		return jia;
	}
	
	/* Return job info of a specific job in the map */
    public synchronized JobInfo getJobInfo(int jobid)  {
        BigInteger jid = BigInteger.valueOf(jobid);
        JobDetails jd = null;
        JobInfo ji = null;

        jd = jobMap.get(jid);
        if (jd != null) {
        	ji = jd.getJobInfo();
        }

        return ji;
    }
    
    // ** Add Job to job map
	public synchronized void addJobToMap(BigInteger jobid, JobDetails details) {

		jobMap.put(jobid, details);
		jobNameIndex.put(details.getName(), jobid);
		jobCount.set(size());

	}

    public synchronized void removeJobFromMap(BigInteger jobid) {

        jobMap.remove(jobid);
        // Need to remove it from the jobNameIndex
        jobNameIndex.values().removeAll(Collections.singleton(jobid));
        jobCount.set(size());
    }
    
    public synchronized void setJobMetricsFailed(Date failureDate) {
        Iterator<Map.Entry<BigInteger, JobDetails>> it = jobMap
                .entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BigInteger, JobDetails> pair = it.next();
            JobDetails curInfo = (JobDetails) pair.getValue();
            curInfo.setLastMetricsFailure(failureDate);
            curInfo.setLastMetricsRefreshFailed(true); 
        }
    }
    
    @Override
    public String toString() {  
        StringBuilder result = new StringBuilder();
        String newline = System.getProperty("line.separator");
        
        result.append("All " + jobMap.size() + " Jobs:");
        result.append(newline);
        Iterator<Map.Entry<BigInteger, JobDetails>> it = jobMap.entrySet()
                .iterator();

        while (it.hasNext()) {
            Map.Entry<BigInteger, JobDetails> pair = it.next();
            JobDetails curInfo = (JobDetails) pair.getValue();
            result.append(curInfo.toString());
            result.append(newline);
        }
        result.append(newline);
        result.append("jobNameIndex:");
        result.append(newline);
        Iterator<Map.Entry<String, BigInteger>> jnit = jobNameIndex
                .entrySet().iterator();
        while (jnit.hasNext()) {
            Map.Entry<String, BigInteger> pair = jnit.next();
            result.append(pair.getKey() + " : " + pair.getValue());
            result.append(newline);
        }
        return result.toString();
    }
}
