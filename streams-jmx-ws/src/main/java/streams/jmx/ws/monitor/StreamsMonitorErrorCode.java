package streams.jmx.ws.monitor;

public enum StreamsMonitorErrorCode {
    UNSPECIFIED_ERROR(0, "Unspecified Error Code"), JMX_MALFORMED_URL(1,
            "A JMX Malformed URL error has occured."), JMX_IOERROR(2,
            "A JMX IO Error has occured.  Usually means that the connection has been lost."),

    DOMAIN_NOT_FOUND(10,
            "The specified domain name is not found on the Streams JMX Server specified"), INSTANCE_NOT_FOUND(
            11,
            "The specified streams instance was not found in the Streams domain"), JOB_NOT_FOUND(
            12, "Specified Streams job was not found in the Streams instance"),

    STREAMS_MONITOR_UNAVAILABLE(50,
            "The Streams Monitor has not been created and initialized."), ALL_JOBS_NOT_AVAILABLE(
            51,
            "The Streams jobs are not available at this time.  The Streams instance does not exist."), ALL_METRICS_NOT_AVAILABLE(
            52,
            "The Metrics for all jobs is not available at this time.  Either the JMX Connection or the Instance is not avaiable."),

    OTHER_ERROR(99, "Unspecified Error Code");

    private final int code;
    private final String description;

    private StreamsMonitorErrorCode(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return code + ":" + description;
    }
}
