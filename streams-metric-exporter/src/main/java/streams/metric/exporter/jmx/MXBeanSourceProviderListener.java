package streams.metric.exporter.jmx;

/**
 * Subscriber to BeanSourceProvider events.
 */
public interface MXBeanSourceProviderListener {

    void beanSourceInterrupted(MXBeanSource bs);
}
