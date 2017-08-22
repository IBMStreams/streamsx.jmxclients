package streams.jmx.ws.monitor;

/**
 * Provides execution context to JMX-based service implementations.
 */
public interface JmxServiceContext {

    /**
     * Gets an MXBeanSourceProvider.
     * 
     * @return the MXBeanSourceProvider to use to obtain MXBeanSource instances.
     */
    MXBeanSourceProvider getBeanSourceProvider();

    /**
     *
     * Gets a web client.
     *
     * @return the web client
     */
    WebClient getWebClient();
}
