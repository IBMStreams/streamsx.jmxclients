package streams.jmx.ws.monitor;

import com.ibm.streams.management.instance.InstanceMXBean;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

/* 
 * InstanceInfo
 * Interface class used to retrieve information from the Monitor
 * Rest server will convert these to proper output format
 * including JSON.
 */
public class InstanceInfo {

    private String instanceName = null;
    private InstanceMXBean.Status instanceStatus = InstanceMXBean.Status.UNKNOWN;
    private Long instanceStartTime = null;

    @JsonIgnore
    private boolean instanceAvailable = false;

    @JsonIgnore
    private boolean instanceExists = false;

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public InstanceMXBean.Status getInstanceStatus() {
        return instanceStatus;
    }

    @JsonGetter("instanceStatus")
    public String getInstanceStatusString() {
        return instanceStatus.toString();
    }

    public void setInstanceStatus(InstanceMXBean.Status instanceStatus) {
        this.instanceStatus = instanceStatus;
    }

    public Long getInstanceStartTime() {
        return instanceStartTime;
    }

    public void setInstanceStartTime(Long instanceStartTime) {
        this.instanceStartTime = instanceStartTime;
    }

    public boolean isInstanceAvailable() {
        return instanceAvailable;
    }

    public void setInstanceAvailable(boolean instanceAvailable) {
        this.instanceAvailable = instanceAvailable;
    }

    public boolean isInstanceExists() {
        return instanceExists;
    }

    public void setInstanceExists(boolean instanceExists) {
        this.instanceExists = instanceExists;
    }
}
