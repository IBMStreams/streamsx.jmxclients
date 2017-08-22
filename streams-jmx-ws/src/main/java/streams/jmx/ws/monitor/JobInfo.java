package streams.jmx.ws.monitor;

import java.math.BigInteger;
import java.util.Date;

import com.ibm.streams.management.job.JobMXBean;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonGetter;

/* 
 * JobInfo
 * Interface class used to retrieve information from the Monitor
 * Rest server will convert these to proper output format
 * including JSON.
 */
public class JobInfo {
    private BigInteger id;
    @JsonIgnore
    private JobMXBean jobBean;
    private JobMXBean.Status status = JobMXBean.Status.UNKNOWN;
    private Date lastMetricsRefresh = null;
    private Date lastMetricsFailure = null;
    private boolean lastMetricsRefreshFailed = false;

    private String adlFile = null;
    private String applicationName = null;
    private String applicationPath = null;
    private String applicationScope = null;
    private String applicationVersion = null;
    private String dataPath = null;
    private String domain = null;
    private JobMXBean.Health health = JobMXBean.Health.UNKNOWN;
    private String instance = null;
    private String jobGroup = null;
    private String name = null;
    private String outputPath = null;
    private String startedByUser = null;
    private long submitTime = 0;

    @JsonRawValue
    private String jobMetrics = null;

    public BigInteger getId() {
        return id;
    }

    public void setId(BigInteger id) {
        this.id = id;
    }

    public JobMXBean getJobBean() {
        return jobBean;
    }

    public void setJobBean(JobMXBean jobBean) {
        this.jobBean = jobBean;
    }

    public String getJobMetrics() {
        return jobMetrics;
    }

    public void setJobMetrics(String jobMetrics) {
        this.jobMetrics = jobMetrics;
    }

    public JobMXBean.Status getStatus() {
        return status;
    }

    @JsonGetter("status")
    public String getStatusString() {
        return status.toString();
    }

    public void setStatus(JobMXBean.Status status) {
        this.status = status;
    }

    public Date getLastMetricsRefresh() {
        return lastMetricsRefresh;
    }

    public void setLastMetricsRefresh(Date lastMetricsRefresh) {
        this.lastMetricsRefresh = lastMetricsRefresh;
    }

    public Date getLastMetricsFailure() {
        return lastMetricsFailure;
    }

    public void setLastMetricsFailure(Date lastMetricsFailure) {
        this.lastMetricsFailure = lastMetricsFailure;
    }

    public boolean isLastMetricsRefreshFailed() {
        return lastMetricsRefreshFailed;
    }

    public void setLastMetricsRefreshFailed(boolean lastMetricsRefreshFailed) {
        this.lastMetricsRefreshFailed = lastMetricsRefreshFailed;
    }

    public String getAdlFile() {
        return adlFile;
    }

    public void setAdlFile(String adlFile) {
        this.adlFile = adlFile;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getApplicationPath() {
        return applicationPath;
    }

    public void setApplicationPath(String applicationPath) {
        this.applicationPath = applicationPath;
    }

    public String getApplicationScope() {
        return applicationScope;
    }

    public void setApplicationScope(String applicationScope) {
        this.applicationScope = applicationScope;
    }

    public String getApplicationVersion() {
        return applicationVersion;
    }

    public void setApplicationVersion(String applicationVersion) {
        this.applicationVersion = applicationVersion;
    }

    public String getDataPath() {
        return dataPath;
    }

    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public JobMXBean.Health getHealth() {
        return health;
    }

    public void setHealth(JobMXBean.Health health) {
        this.health = health;
    }

    public String getInstance() {
        return instance;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }

    public String getJobGroup() {
        return jobGroup;
    }

    public void setJobGroup(String jobGroup) {
        this.jobGroup = jobGroup;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    public String getStartedByUser() {
        return startedByUser;
    }

    public void setStartedByUser(String startedByUser) {
        this.startedByUser = startedByUser;
    }

    public long getSubmitTime() {
        return submitTime;
    }

    public void setSubmitTime(long submitTime) {
        this.submitTime = submitTime;
    }

    public JobInfo(BigInteger id, JobMXBean jobBean, JobMXBean.Status status,
            String jobMetrics, Date lastMetricsRefresh,
            Date lastMetricsFailure, boolean lastMetricsRefreshFailed) {
        setId(id);
        setJobBean(jobBean);
        setStatus(status);
        if (jobMetrics == null) {
            setJobMetrics("");
        } else {
            setJobMetrics(jobMetrics);
        }
        setLastMetricsRefresh(lastMetricsRefresh);
        setLastMetricsFailure(lastMetricsFailure);
        setLastMetricsRefreshFailed(lastMetricsRefreshFailed);
    }

    public JobInfo() {
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        String newline = System.getProperty("line.separator");

        result.append("id: " + this.getId());
        result.append(newline);
        result.append("status: " + this.getStatus());

        return result.toString();
    }
}
