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

package streams.jmx.client.jmx;

import java.security.cert.CertificateException;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;

public class JmxTrustManager implements X509TrustManager {
    private static final String ISSUER = "CN=www.ibm.com,OU=SWG,O=IBM,L=Rochester,ST=MN,C=US";

    public void checkServerTrusted(java.security.cert.X509Certificate[] chain,
            String authType) throws CertificateException {
        for (java.security.cert.X509Certificate cert : chain) {
            X500Principal issuer = cert.getIssuerX500Principal();
            if (!issuer.getName().equals(ISSUER)) {
                throw new CertificateException("invalid issuer="
                        + issuer.getName());
            }
        }
    }

    public void checkClientTrusted(java.security.cert.X509Certificate[] chain,
            String authType) throws CertificateException {
    }

    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
        return null;
    }
}