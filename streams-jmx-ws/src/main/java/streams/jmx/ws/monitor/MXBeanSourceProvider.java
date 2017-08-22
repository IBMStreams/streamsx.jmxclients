package streams.jmx.ws.monitor;

import java.io.IOException;
import java.net.MalformedURLException;

/**
 * Represents a source of <code>MXBeanSource</code> instances.
 */
public interface MXBeanSourceProvider {

    // Add a newer getBeanSource that uses internal variables in implementing
    // class
    MXBeanSource getBeanSource() throws MalformedURLException, IOException;

    void addBeanSourceProviderListener(MXBeanSourceProviderListener listener);
}
