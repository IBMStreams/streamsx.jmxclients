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

package streams.metric.exporter.httpclient;

import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;

import java.net.URL;
import java.security.GeneralSecurityException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebClientImpl implements WebClient {
    private static final Logger LOG = LoggerFactory.getLogger(WebClientImpl.class);

    private String sslProtocol;
    private TrustManager[] trustManagers;

    public WebClientImpl(String sslProtocol, TrustManager[] tms) {
        this.sslProtocol = sslProtocol;
        trustManagers = tms;
    }

    public String get(String fromUri) throws WebClientException {
        /******* HTTPS Interaction ********/
        try {
            // set up trust manager
            SSLContext ctxt = null;
            try {
                ctxt = SSLContext.getInstance(sslProtocol);
                ctxt.init(null, trustManagers, null);
            } catch (GeneralSecurityException e) {
                LOG.error("HTTP retrieval initialization received Security Exception: "
                        + e);
                throw new WebClientException(
                        "HTTP Security Exception", e);
            }
            // set up hostname verifier
            HostnameVerifier hv = new HostnameVerifier() {
                public boolean verify(String urlHostName, SSLSession session) {
                    // return false to reject
                    return true;
                }
            };

            URL url = new URL(fromUri);
            if (LOG.isTraceEnabled()) {
                LOG.trace(String.format("Connecting to URL %s", fromUri));
            }

            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

            try {
                conn.setSSLSocketFactory(ctxt.getSocketFactory());
                conn.setHostnameVerifier(hv);
                conn.setRequestMethod("GET");
                conn.connect();
                InputStreamReader reader = new InputStreamReader(conn.getInputStream());

                try {
                    return readFully(reader);
                }
                finally {
                    reader.close();
                }
            }
            finally {
                conn.disconnect();
            }
        }
        catch (IOException e) {
            throw new WebClientException(String.format("Failed GET request to uri %s", fromUri), e);
        }
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
}
