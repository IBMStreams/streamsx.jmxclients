package streams.metric.exporter.jmx;

import streams.metric.exporter.httpclient.WebClient;

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
