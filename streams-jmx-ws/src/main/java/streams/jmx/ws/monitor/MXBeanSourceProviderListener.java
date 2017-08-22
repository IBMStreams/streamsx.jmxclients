package streams.jmx.ws.monitor;

/**
 * Subscriber to BeanSourceProvider events.
 */
public interface MXBeanSourceProviderListener {

    void beanSourceInterrupted(MXBeanSource bs);
}
