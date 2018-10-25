// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.


// NOTE: This is in the process of being redone from java.net.URL to use Apache Commons

package streams.jmx.client.httpclient;

import java.io.InputStreamReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;

import java.net.URL;
import java.security.KeyStore;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebClientImpl implements WebClient {
    private static final Logger LOG = LoggerFactory.getLogger(WebClientImpl.class);

    private String sslProtocol;
    private TrustManager[] trustManagers;
    private KeyStore ks;

    public WebClientImpl(String sslProtocol, TrustManager[] tms, KeyStore ks) {
        this.sslProtocol = sslProtocol;
        trustManagers = tms;
        ks = ks;
    }

    public String get(String fromUri) throws WebClientException {

        try {

            SSLContextBuilder sslContextBuilder = SSLContextBuilder.create();
            sslContextBuilder.loadTrustMaterial(ks, new TrustSelfSignedStrategy());
            sslContextBuilder.setProtocol(this.sslProtocol);
            SSLContext sslContext = sslContextBuilder.build();
            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());

            HttpClientBuilder httpClientBuilder = HttpClients.custom().setSSLSocketFactory(sslSocketFactory);
            CloseableHttpClient httpClient = httpClientBuilder.build();
            HttpGet httpget = new HttpGet(fromUri);  // throws IllegalArgumentException

            try {

                ResponseHandler<String> rh = new ResponseHandler<String>() {
                    @Override
                    public String handleResponse(final HttpResponse response) throws IOException {
                        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                            throw new IOException(String.format("Unable to get information from Streams JMX HTTP Server URI: %s",
                                fromUri));
                        }
                        HttpEntity entity = response.getEntity();
                        //System.out.println(EntityUtils.toString(entity));
                        InputStreamReader reader = new InputStreamReader(entity.getContent());

                        try {
                            return readFully(reader);
                        }
                        finally {
                            reader.close();
                        }
                    }
                };

                return httpClient.execute(httpget, rh);

            } catch (Exception e) {
                if (LOG.isDebugEnabled()) {
                    e.printStackTrace();
                }
                throw new WebClientException(e);
            } finally {
                httpget.releaseConnection();
            }

        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                e.printStackTrace();
            }
            throw new WebClientException(e);
        }
    }


    public String get(String fromUri, String host, String port) throws WebClientException{
    		String newUri = fromUri;
    		// If either host or port need to be overridden
    		boolean hostOverride = false;
    		boolean portOverride = false;
    		if ((host != null) && (!host.isEmpty())) {
    			hostOverride = true;
    		}
    		if ((port != null) && (!port.isEmpty())) {
    			portOverride = true;
    		}
    		if ((hostOverride) || (portOverride)) {
    			LOG.debug("httpClient.get called to replace host and/or port of uri({}) with host: {}, port: {}",fromUri,host,port);
	    		try {
		        URL url = new URL(fromUri);
		        String theHost = url.getHost();
		        int thePort = url.getPort();
		        
		        if (hostOverride) {
		        		theHost = host;
		        }
		        if (portOverride) {
		        		thePort = Integer.parseInt(port);
		        }
		        
		        URL newUrl = new URL(url.getProtocol(),theHost,thePort,url.getFile());
		        newUri = newUrl.toString();
		        LOG.debug("httpClient.get new uri: {}",newUri);
	    		} catch (IOException e) {
	            throw new WebClientException(String.format("Failed GET request to uri %s", fromUri), e);
	    		}
    		}
    		return get(newUri);
    }


    private static final String readFully(Reader r) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buffer = new char[1000];

        int len;
        while ((len = r.read(buffer)) > -1) {
            sb.append(buffer, 0, len);
        }

        return sb.toString(); 
    }

    @Override
    public void putFile(String toUri, String contentType, File file) throws WebClientException {

        try {

            SSLContextBuilder sslContextBuilder = SSLContextBuilder.create();
            sslContextBuilder.loadTrustMaterial(ks, new TrustSelfSignedStrategy());
            sslContextBuilder.setProtocol(this.sslProtocol);
            SSLContext sslContext = sslContextBuilder.build();
            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());

            HttpClientBuilder httpClientBuilder = HttpClients.custom().setSSLSocketFactory(sslSocketFactory);
            CloseableHttpClient httpClient = httpClientBuilder.build();
            //CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpPut putFile = new HttpPut(toUri);  // throws IllegalArgumentException

            try {

                putFile.setEntity(new FileEntity(file,ContentType.create(contentType)));

                ResponseHandler<HttpResponse> rh = new ResponseHandler<HttpResponse>() {
                    @Override
                    public HttpResponse handleResponse(final HttpResponse response) throws IOException {
                        return response;
                    }
                };

                HttpResponse response = httpClient.execute(putFile, rh);

                if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                    throw new WebClientException(String.format("Unable to send file (%s) to Streams JMX HTTP Server URI: %s",
                        file.getName(), toUri));
                }

            } catch (Exception e) {
                throw new WebClientException(e);
            } finally {
                putFile.releaseConnection();
            }

        } catch (Exception e) {
            throw new WebClientException(e);
        }
    }


    public void putFile(String toUri, String contentType, File file, String host, String port) throws WebClientException{
        String newUri = toUri;
        // If either host or port need to be overridden
        boolean hostOverride = false;
        boolean portOverride = false;
        if ((host != null) && (!host.isEmpty())) {
            hostOverride = true;
        }
        if ((port != null) && (!port.isEmpty())) {
            portOverride = true;
        }
        if ((hostOverride) || (portOverride)) {
            LOG.debug("httpClient.putFile called to replace host and/or port of uri({}) with host: {}, port: {}",toUri,host,port);
            try {
                URL url = new URL(toUri);
                String theHost = url.getHost();
                int thePort = url.getPort();
                
                if (hostOverride) {
                        theHost = host;
                }
                if (portOverride) {
                        thePort = Integer.parseInt(port);
                }
                
                URL newUrl = new URL(url.getProtocol(),theHost,thePort,url.getFile());
                newUri = newUrl.toString();
                LOG.debug("httpClient.putFile new uri: {}",newUri);
            } catch (IOException e) {
                throw new WebClientException(String.format("Failed URI host(%s)/port(%s) replacement to uri %s", host, port, toUri), e);
            }
        }
        putFile(newUri, contentType, file);
}

}
