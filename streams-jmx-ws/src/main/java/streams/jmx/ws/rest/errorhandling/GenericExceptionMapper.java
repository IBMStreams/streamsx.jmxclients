package streams.jmx.ws.rest.errorhandling;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import streams.jmx.ws.rest.RestServer;

import javax.ws.rs.WebApplicationException;

@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {
    private static final int GENERIC_APP_ERROR_CODE = 5001;
    private static final Logger LOGGER = LoggerFactory.getLogger("root."
            + GenericExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable ex) {

        ErrorMessage errorMessage = new ErrorMessage();
        setHttpStatus(ex, errorMessage);
        errorMessage.setCode(GENERIC_APP_ERROR_CODE);
        errorMessage.setMessage(ex.getMessage());
        StringWriter errorStackTrace = new StringWriter();
        ex.printStackTrace(new PrintWriter(errorStackTrace));
        errorMessage.setDeveloperMessage(errorStackTrace.toString());

        LOGGER.debug("RestServer GenericExceptionManager: " + errorMessage);

        return Response.status(errorMessage.getStatus()).entity(errorMessage)
                .type(MediaType.APPLICATION_JSON).build();
    }

    private void setHttpStatus(Throwable ex, ErrorMessage errorMessage) {
        if (ex instanceof WebApplicationException) { // NICE way to combine both
                                                     // methods
            errorMessage.setStatus(((WebApplicationException) ex).getResponse()
                    .getStatus());
        } else {
            errorMessage.setStatus(Response.Status.INTERNAL_SERVER_ERROR
                    .getStatusCode());
            ; // default to internal server error
        }
    }

}
