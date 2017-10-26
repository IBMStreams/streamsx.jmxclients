package streams.metric.exporter.httpclient;

/**
 * Abstracts HTTP operations to make unit testing easier.
 */
public interface WebClient {
    
    /**
     * Gets data from via HTTP GET.
     * @param fromUri the target URI
     * @return the full response body
     * @throws WebClientException if the HTTP operation fails
     */
    String get(String fromUri) throws WebClientException;
}
