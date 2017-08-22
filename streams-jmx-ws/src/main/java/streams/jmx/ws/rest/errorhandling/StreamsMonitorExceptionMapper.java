package streams.jmx.ws.rest.errorhandling;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import streams.jmx.ws.monitor.StreamsMonitorException;

@Provider
public class StreamsMonitorExceptionMapper implements
        ExceptionMapper<StreamsMonitorException> {

    private static final Logger LOGGER = LoggerFactory.getLogger("root."
            + StreamsMonitorExceptionMapper.class.getName());

    @Override
    public Response toResponse(StreamsMonitorException ex) {
        ErrorMessage errorMessage = new ErrorMessage(ex);
        setHttpStatus(ex, errorMessage);

        LOGGER.debug("RestServer StreamsMonitorExceptionManager: "
                + errorMessage);

        return Response.status(errorMessage.getStatus()).entity(errorMessage)
                .type(MediaType.APPLICATION_JSON).build();
    }

    private void setHttpStatus(StreamsMonitorException ex,
            ErrorMessage errorMessage) {

        // Interpret Internal Application Codes to HTTP Response Codes
        switch (ex.getErrorCode()) {
        case INSTANCE_NOT_FOUND: {
            errorMessage.setStatus(Response.Status.NOT_FOUND.getStatusCode());
            break;
        }
        case JOB_NOT_FOUND: {
            errorMessage.setStatus(Response.Status.NOT_FOUND.getStatusCode());
            break;
        }
        case ALL_METRICS_NOT_AVAILABLE: {
            errorMessage.setStatus(Response.Status.NOT_FOUND.getStatusCode());
            break;
        }
        case ALL_JOBS_NOT_AVAILABLE: {
            errorMessage.setStatus(Response.Status.NOT_FOUND.getStatusCode());
            break;
        }
        default: {
            errorMessage.setStatus(Response.Status.INTERNAL_SERVER_ERROR
                    .getStatusCode());

        }
        }

    }

}
