package streams.jmx.ws.monitor;

import streams.jmx.ws.monitor.StreamsMonitorErrorCode;

public class StreamsMonitorException extends Exception {

    // required/suggested for all serializable classes
    private static final long serialVersionUID = 814863L;

    StreamsMonitorErrorCode errorCode = StreamsMonitorErrorCode.UNSPECIFIED_ERROR;

    public StreamsMonitorException() {
        super();
        this.errorCode = StreamsMonitorErrorCode.UNSPECIFIED_ERROR;
    }

    public StreamsMonitorException(StreamsMonitorErrorCode errorCode) {
        super();
        this.errorCode = errorCode;
    }

    public StreamsMonitorException(String message, Throwable cause,
            boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.errorCode = StreamsMonitorErrorCode.UNSPECIFIED_ERROR;

    }

    public StreamsMonitorException(StreamsMonitorErrorCode errorCode,
            String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.errorCode = errorCode;
    }

    public StreamsMonitorException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = StreamsMonitorErrorCode.UNSPECIFIED_ERROR;

    }

    public StreamsMonitorException(StreamsMonitorErrorCode errorCode,
            String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;

    }

    public StreamsMonitorException(String message) {
        super(message);
        this.errorCode = StreamsMonitorErrorCode.UNSPECIFIED_ERROR;

    }

    public StreamsMonitorException(StreamsMonitorErrorCode errorCode,
            String message) {
        super(message);
        this.errorCode = errorCode;

    }

    public StreamsMonitorException(Throwable cause) {
        super(StreamsMonitorErrorCode.UNSPECIFIED_ERROR.getDescription(), cause);
        this.errorCode = StreamsMonitorErrorCode.UNSPECIFIED_ERROR;

    }

    public StreamsMonitorException(StreamsMonitorErrorCode errorCode,
            Throwable cause) {
        super(errorCode.getDescription(), cause);
        this.errorCode = errorCode;

    }

    public StreamsMonitorErrorCode getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(StreamsMonitorErrorCode errorCode) {
        this.errorCode = errorCode;
    }

}
