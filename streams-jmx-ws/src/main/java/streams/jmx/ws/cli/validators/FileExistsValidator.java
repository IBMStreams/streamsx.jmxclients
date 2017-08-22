package streams.jmx.ws.cli.validators;

import java.io.File;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

/**
 * IParameterValidator that verifies that a parameter value is a filesystem path
 * that points to an existing file.
 */
public class FileExistsValidator implements IParameterValidator {

    @Override
    public void validate(String name, String value) throws ParameterException {
        if (value == null) {
            throw new ParameterException(String.format("Parameter %s must not be null.", name));
        }
        
        File f = new File(value);
        
        if (!f.exists() || f.isDirectory()) {
            throw new ParameterException(String.format("%s does not exist or is not a valid file. Parameter %s must contain the path of an existing file.", value, name));
        }
    }
}
