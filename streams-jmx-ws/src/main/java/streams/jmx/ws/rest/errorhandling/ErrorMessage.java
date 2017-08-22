package streams.jmx.ws.rest.errorhandling;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import streams.jmx.ws.monitor.StreamsMonitorException;

/*
 * ErrorMessage
 * Allows Jersey to convert error message into output type (e.g. JSON)
 */
public class ErrorMessage {
    int status;
    int code;
    String message;
    String developerMessage;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDeveloperMessage() {
        return developerMessage;
    }

    public void setDeveloperMessage(String developerMessage) {
        this.developerMessage = developerMessage;
    }

    public ErrorMessage(StreamsMonitorException ex) {
        setStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        setCode(ex.getErrorCode().getCode());
        setMessage(ex.getErrorCode().getDescription());
        setDeveloperMessage(ex.getMessage());
    }

    public ErrorMessage(NotFoundException ex) {
        setStatus(Response.Status.NOT_FOUND.getStatusCode());
        setMessage(ex.getMessage());
    }

    public ErrorMessage() {
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        String newline = System.getProperty("line.separator");

        result.append("status: " + this.getStatus());
        result.append(newline);
        result.append("code: " + this.getCode());
        result.append(newline);
        result.append("mesage: " + this.getMessage());
        result.append(newline);
        result.append("developerMessage: " + this.getDeveloperMessage());
        return result.toString();
    }

}
