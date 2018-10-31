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

import java.io.File;
import java.net.UnknownHostException;
import java.io.IOException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.util.*;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang.StringUtils;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectionNotification;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.NotificationListener;
import javax.management.ListenerNotFoundException;

import com.ibm.streams.management.ObjectNameBuilder;
import com.ibm.streams.management.domain.DomainMXBean;
import com.ibm.streams.management.domain.DomainServiceMXBean;
import com.ibm.streams.management.domain.DomainServiceMXBean.Type;
import com.ibm.streams.management.instance.InstanceMXBean;
import com.ibm.streams.management.instance.InstanceServiceMXBean;
import com.ibm.streams.management.job.JobMXBean;
import com.ibm.streams.management.job.OperatorMXBean;
import com.ibm.streams.management.job.OperatorInputPortMXBean;
import com.ibm.streams.management.job.OperatorOutputPortMXBean;
import com.ibm.streams.management.job.PeInputPortMXBean;
import com.ibm.streams.management.job.PeMXBean;
import com.ibm.streams.management.job.PeOutputPortMXBean;
import com.ibm.streams.management.resource.ResourceMXBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An <code>MXBeanSourceProvider</code> that pools its connections to the JMX
 * server, reusing MBeanServerConnection instances per distinct combination of
 * JMX uri, username, password, and provider.
 */
public class JmxConnectionPool implements MXBeanSourceProvider {
    private static final String STREAMS_X509CERT = "STREAMS_X509CERT";
    private static final Logger LOGGER = LoggerFactory.getLogger("root."
            + JmxConnectionPool.class.getName());

    // Delay for attempting jmx reconnection in seconds
    private static final int reconnectionDelay = 10;
    // Delay for attempting initial JMX connection if first attempt fails
    private static final int startRetryDelay = 30;

    // private final Map<ConnectorKey, JMXConnector> connectors = new
    // HashMap<ConnectorKey, JMXConnector>();
    private final Map<ConnectorKey, PooledJmxConnection> connectors = new HashMap<ConnectorKey, PooledJmxConnection>();

    private String jmxUri = null;
    private String user = null;
    private String password = null;
    private String sslOption = null;
    private String X509Cert = null;
    private boolean retryConnections = false;
    private List<MXBeanSourceProviderListener> providerListeners = new ArrayList<MXBeanSourceProviderListener>();
    final private String provider = "com.ibm.streams.management";

    // Create a constructor that will allow the pool to be constructed for use
    // with a single connection string set
    public JmxConnectionPool(String jmxUri, String x509Cert, String user,
            String password, String ssloption, boolean retryConnections) {
        this.jmxUri = jmxUri; // use streamtool getjmxconnect to find
        this.user = StringUtils.trimToNull(user);
        this.password = StringUtils.trimToNull(password);
        this.X509Cert = StringUtils.trimToNull(x509Cert);
        this.sslOption = ssloption;
        this.retryConnections = retryConnections;

        // If the user did not pass in x509 or username password, try to get
        // x509 cert from Env variable or exit
        // This is the standard streams environment variable, but check here so
        // error can be handled gracefully

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String
                    .format("Credentials provided: user = %s, password = %s, x509 cert = %s",
                            user, password, X509Cert));
            LOGGER.debug("jmxUri: {}, sslOption: {}",jmxUri,ssloption);
        }

        if (X509Cert == null) {
            X509Cert = StringUtils.trimToNull(System.getenv(STREAMS_X509CERT));
        }

        if (X509Cert != null) {
            if (!new File(X509Cert).isFile()) {
                throw new IllegalArgumentException(
                        "The specified x509 certificate (" + this.X509Cert
                                + ") does not exist.");
            }
        }

        if ((user == null || password == null) && X509Cert == null) {
            throw new IllegalArgumentException(
                    "Missing or incomplete credentials. Please select an authentication parameter (-u or -X509cert) or set environment variable "
                            + STREAMS_X509CERT);
        }
    }

    @Override
    public MXBeanSource getBeanSource() throws IOException {
        return getConnection(this.jmxUri, this.X509Cert, this.user,
                this.password, this.provider).getStreamsBeanSource();
    }

    @Override
    public void addBeanSourceProviderListener(MXBeanSourceProviderListener listener) {
        synchronized(providerListeners) {
            providerListeners.add(listener);
        }
    }

    // ************* Internal Methods ********************

    // ** Get a connection from the pool or create it if it does not exist

    private PooledJmxConnection getConnection(String jmxUri, String x509Cert,
            String username, String password, String provider)
            throws IOException {

        ConnectorKey key = new ConnectorKey(jmxUri, username, password,
                provider);

        synchronized (connectors) {

            LOGGER.trace("Looking up connector key: " + key + "...");

            PooledJmxConnection connector = connectors.get(key);

            // Create the connection if it does not exist
            if (connector == null) {
                connector = new PooledJmxConnection(key, jmxUri, x509Cert,
                        username, password, provider, sslOption);
                connector.doStart();
                connectors.put(key, connector);
            } else {
                if (!connector.isActive()) {

                    // Attempt to restart the connection
                    connector.doStart();
                }
            }

            return connector;

        }
    }

    private void notifyBeanSourceInterrupted(MXBeanSource bs) {
        synchronized(providerListeners) {
            for (MXBeanSourceProviderListener listener : providerListeners) {
                listener.beanSourceInterrupted(bs);
            }
        }
    }

    /**
     * Closes all open connections in the pool.
     * 
     * @throws IOException
     *             if a particular connection can't be closed. This will abort
     *             the <code>close</code> operation for any remaining connection
     *             instances.
     */
    public void close() throws IOException {
        synchronized (connectors) {
            for (Iterator<Map.Entry<ConnectorKey, PooledJmxConnection>> entryIterator = connectors
                    .entrySet().iterator(); entryIterator.hasNext();) {
                Map.Entry<ConnectorKey, PooledJmxConnection> entry = entryIterator
                        .next();

                entry.getValue().doStop();

                entryIterator.remove();
            }
        }
    }

    /**
     * A Pooled connection should be self-restartable if the jmx server
     * connection is lost It is the responsibility of the user of the connection
     * to figure out what happened
     * 
     * 1) Just a temporary network failure 2) JMX Server failed and restarted
     * (Streams domain/instance/jobs still in tact) 3) Whole domain restarted
     * (thus causing jmx to restart)
     * 
     * NOTE: In Streams 4.x the JMX server is a domain service, thus the streams
     * instance can stop/start without affecting the JMX server Clients of this
     * connection pool need to monitor the instance (e.g. start date change)
     */
    private class PooledJmxConnection {
        private JMXConnector mConnector = null;
        private String mConnectionId = null;;
        @SuppressWarnings("unused")
		private ConnectorKey mConnectorKey = null;
        private MBeanServerConnection mBeanServerConnection = null;
        private MXBeanSource mStreamsBeanSource = null;
        private ConnectionNotificationListener mConnectionNotificationListener = null;

        private String jmxUri = null;
        private String user = null;
        private String password = null;
        private String protocol = null;
        private String x509Cert = null;
        private String provider = null;

        public PooledJmxConnection(ConnectorKey connectorKey, String jmxUri,
                String x509Cert, String user, String password, String provider, String protocol) {
            this.mConnectorKey = connectorKey;
            this.jmxUri = jmxUri; // use streamtool getjmxconnect to find
            this.x509Cert = x509Cert;
            this.user = user;
            this.password = password;
            this.provider = provider;
            this.protocol = protocol;
        }

        public void doStart() throws IOException, SecurityException {
            boolean canRetry = true;
            boolean needToRetry = true;
            // Loop until success or throws an error because cannot retry
            while (canRetry && needToRetry) {
                needToRetry = false;
                try {
                    initNetworkConnection();
                } catch (UnknownHostException e) {
                    LOGGER.error("Unknown Host in JMX URL: {}", this.jmxUri);
                    throw e;
                } catch (MalformedURLException e) {
                    LOGGER.error("Malformed JMX URL: {}", this.jmxUri);
                    throw e;
                } catch (IOException e) {
                    LOGGER.warn(
                            "Failed initial connection to JMX Server {}. Verify port number and ensure it is running.",
                            this.jmxUri);
                    System.out.println("*** e: " + e);
                    e.printStackTrace();

                    if (!retryConnections) {
                        throw e;
                    } else {
                        canRetry = true;
                        needToRetry = true;
                    }

                } catch (SecurityException e) {
                    LOGGER.error("Initial JMX connection ({}) failed for security reasons, check username, password, or x509certificates",this.jmxUri);
                    LOGGER.trace("  SecurityException e:" + e);

                    throw e;
                } catch (Exception e) {
                    LOGGER.error("Intial JMX connection ({}) failed for unknown reason.  Not trying again",this.jmxUri);
                    LOGGER.info("  Exception e: " + e);
                    e.printStackTrace();
                    throw e;
                }

                // If can retry, then wait a bit
                if (needToRetry && canRetry) {
                    LOGGER.info(
                            "Retrying initial JMX connection in " + startRetryDelay + " seconds...");
                    try {
                        Thread.sleep(startRetryDelay * 1000);
                    } catch (InterruptedException e) {
                        LOGGER.error("JMX Retry Interupted.");
                        throw new IOException(
                                "JMX Initial Connection Retry Interupted");
                    }
                }
            }
        }

        /*
         * initNetworkConnection Use the attributes of this class and attempt to
         * connect to the JMX server
         */
        private void initNetworkConnection() throws IOException {
            if (mConnector != null) {
                try {
                    LOGGER.trace("** initNetworkConnection: mConnector with connection id ("
                            + mConnectionId
                            + ") != null, so attempting to close it...");
                    mConnector.close();
                } catch (Exception e) {
                    // ignore, as this is best effort
                }
            }

            LOGGER.trace("...connection not found or null, creating new connection...");
            HashMap<String, Object> env = new HashMap<String, Object>();
            // Only set username/password credentials if available, othewise
            // assume PKI key used
            // via environment variable (STREAMS_X509CERT) or passed in (future)
            if (this.user != null && !this.user.isEmpty()) {
                String[] creds = { this.user, this.password };
                env.put("jmx.remote.credentials", creds);
            } else if (this.x509Cert != null && !this.x509Cert.isEmpty()) {
                Path certFile = Paths.get(this.x509Cert);
                Path fullCertFile = certFile.toAbsolutePath();
                env.put("jmx.remote.x.certificate", fullCertFile.toString());
            } else {
                // This should never happen!!!
                throw new IOException(
                        "JMX CLI Connection Credentials not available, contact the developer");
            }

            env.put("jmx.remote.protocol.provider.pkgs", provider);
            
            // sslOption in Streams, set for JMX Connections
            env.put("jmx.remote.tls.enabled.protocols", protocol);

            mConnector = JMXConnectorFactory.connect(new JMXServiceURL(jmxUri),
                    env);
            mConnectionId = mConnector.getConnectionId();
            mConnector.addConnectionNotificationListener(
                    getConnectionNotificationListener(), null, null);
            mBeanServerConnection = mConnector.getMBeanServerConnection();
            mStreamsBeanSource = new MXBeanSourceImpl(mBeanServerConnection);

            LOGGER.trace("***** initNetworkConnection, Created JMX ConnectonID: "
                    + mConnectionId);
            LOGGER.info("*** JMX Connection Success");
        }

        /**
         * Schedules an attempt to re-initialize a lost connection after the
         * reconnect delay
         */
        protected void scheduleReconnect() {
            TimerTask reconnector = new TimerTask() {
                @Override
                public void run() {
                    try {
                        initNetworkConnection();
                    } catch (Exception e) {
                        LOGGER.warn(
                                "** Failed to reconnect to JMX Server {}. => {}", jmxUri, e.getMessage());
                        scheduleReconnect();
                    }
                }
            };

            LOGGER.info(
                    "*** Delaying JMX reconnection. Trying again in " + reconnectionDelay + " seconds");

            // Create timer to automatically refresh the status and metrics
            Timer timer = new Timer("JMXReconnection");
            timer.schedule(reconnector, 1000 * reconnectionDelay);

        }

        // Stop/Destroy the connection
        public void doStop() {
            try {
                removeNotificationListener();
                mConnector.close();
            } catch (IOException e) {
                LOGGER.trace("doStop mConnector.close() raised IOException, but do not care since closing connection");
            }

        }

        // Validate the connection is still alive
        public boolean isActive() {
            return true;
        }

        @SuppressWarnings("unused")
		public MBeanServerConnection getMBeanServerConnection() {
            return mBeanServerConnection;
        }

        public MXBeanSource getStreamsBeanSource() {
            return mStreamsBeanSource;
        }

        // ******************** Connection Notification Helper Class *********

        protected ConnectionNotificationListener getConnectionNotificationListener() {
            if (mConnectionNotificationListener == null) {
                mConnectionNotificationListener = new ConnectionNotificationListener();
            }
            return mConnectionNotificationListener;
        }

        protected void removeNotificationListener() {
            if (mConnectionNotificationListener != null) {
                try {
                    mConnector
                            .removeConnectionNotificationListener(mConnectionNotificationListener);
                } catch (ListenerNotFoundException e) {
                }
                mConnectionNotificationListener = null;
            }
        }

        private class ConnectionNotificationListener implements
                NotificationListener {
            @Override
            public void handleNotification(Notification notification,
                    Object handback) {
                JMXConnectionNotification connectionNotification = (JMXConnectionNotification) notification;
                // Should handle connectionID in the future to be specific
                // Be aware!! JMX failures log SEVERE messages that are not
                // coming from us
                LOGGER.trace("*** JMX Connection Notification: "
                        + connectionNotification);
                LOGGER.trace("*** notification.getConnectionId(): "
                        + connectionNotification.getConnectionId());
                LOGGER.trace("*** this.notificationId: " + mConnectionId);

                // Only reset connection if the notification is for the
                // connection at this endpoint
                if (!connectionNotification.getConnectionId().equals(
                        mConnectionId)) {
                    return;
                }

                String notificationType = connectionNotification.getType();

                if (notificationType
                        .equals(JMXConnectionNotification.NOTIFS_LOST)
                        || notificationType
                                .equals(JMXConnectionNotification.CLOSED)
                        || notificationType
                                .equals(JMXConnectionNotification.FAILED)) {
                    LOGGER.warn("*** Lost JMX Connection, scheduling reconnect and removing connection listener ...");
                    // Remove connectionListener becuase often FAILED is follwed
                    // by CLOSED and was causing multiple reconnects which stomp
                    // on each other
                    removeNotificationListener();

                    notifyBeanSourceInterrupted(mStreamsBeanSource);
                    // Could test to ensure we want to try and reconnect
                    scheduleReconnect();
                }
            }
        }

    }

    private class ConnectorKey {
        private String jmxUri;
        private String username;
        private String password;
        private String provider;

        public ConnectorKey(String jmxUri, String username, String password,
                String provider) {
            this.jmxUri = jmxUri;
            this.username = username;
            this.password = password;
            this.provider = provider;
        }

        public int hashCode() {
            return Objects.hash(jmxUri, username, password, provider);
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            result.append("jmxUri: " + this.jmxUri + ", username: "
                    + this.username);
            result.append(", provider " + this.provider);
            return result.toString();
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof ConnectorKey)) {
                return false;
            }

            ConnectorKey ck = (ConnectorKey) obj;

            return Objects.equals(jmxUri, ck.jmxUri)
                    && Objects.equals(username, ck.username)
                    && Objects.equals(password, ck.password)
                    && Objects.equals(provider, ck.provider);
        }
    }

    private static class MXBeanSourceImpl implements MXBeanSource {
        private MBeanServerConnection connection;

        MXBeanSourceImpl(MBeanServerConnection connection) {
            this.connection = connection;
        }

        @Override
        public MBeanServerConnection getMBeanServerConnection() {
            return this.connection;
        }

        @Override
        public JobMXBean getJobBean(String domainId, String instanceId,
                BigInteger jobId) {
            ObjectName objName = ObjectNameBuilder.job(domainId, instanceId,
                    jobId);

            return JMX.newMXBeanProxy(connection, objName, JobMXBean.class,
                    true);
        }

        @Override
        public PeMXBean getPeBean(String domainId, String instanceId,
                BigInteger peId) {
            ObjectName objName = ObjectNameBuilder.pe(domainId, instanceId,
                    peId);
            return JMX
                    .newMXBeanProxy(connection, objName, PeMXBean.class, true);
        }

        @Override
        public DomainMXBean getDomainBean(String domainId) {
            ObjectName objName = ObjectNameBuilder.domain(domainId);

            return JMX.newMXBeanProxy(connection, objName, DomainMXBean.class,
                    true);
        }

        @Override
        public InstanceMXBean getInstanceBean(String domainId, String instanceId) {
            ObjectName objName = ObjectNameBuilder.instance(domainId,
                    instanceId);
            return JMX.newMXBeanProxy(connection, objName,
                    InstanceMXBean.class, true);
        }

        @Override
        public ResourceMXBean getResourceBean(String domainId, String resourceId) {
            ObjectName resourceObjectName = ObjectNameBuilder.resource(
                    domainId, resourceId);

            return JMX.newMXBeanProxy(connection, resourceObjectName,
                    ResourceMXBean.class, true);
        }

        @Override
        public DomainServiceMXBean getDomainServiceBean(String domainId,
                Type serviceType) {
            ObjectName serviceObjectName = ObjectNameBuilder.domainService(
                    domainId, serviceType);
            return JMX.newMXBeanProxy(connection, serviceObjectName,
                    DomainServiceMXBean.class, true);
        }

        @Override
        public InstanceServiceMXBean getInstanceServiceMXBean(
                String domainId,
                String instanceId,
                com.ibm.streams.management.instance.InstanceServiceMXBean.Type serviceType) {
            ObjectName serviceObjectName = ObjectNameBuilder.instanceService(
                    domainId, instanceId, serviceType);
            return JMX.newMXBeanProxy(connection, serviceObjectName,
                    InstanceServiceMXBean.class, true);
        }

        @Override
        public OperatorMXBean getOperatorMXBean(
                String domainId, String instanceId, BigInteger jobId, String operator) {
            ObjectName operatorName = ObjectNameBuilder.operator(domainId, instanceId, jobId, operator);

            return JMX.newMXBeanProxy(connection, operatorName, OperatorMXBean.class, true);
        }

        @Override
        public OperatorInputPortMXBean getOperatorInputPortMXBean(
                String domainId, String instanceId, BigInteger jobId,
                String operator, int indexWithinOperator) {
            ObjectName inputPortName = ObjectNameBuilder.operatorInputPort(
                    domainId, instanceId, jobId, operator, indexWithinOperator);
            return JMX.newMXBeanProxy(connection, inputPortName,
                    OperatorInputPortMXBean.class, true);
        }

        @Override
        public OperatorOutputPortMXBean getOperatorOutputPortMXBean(
                String domainId, String instanceId, BigInteger jobId,
                String operator, int indexWithinOperator) {
            ObjectName outputPortName = ObjectNameBuilder.operatorOutputPort(
                    domainId, instanceId, jobId, operator, indexWithinOperator);
            return JMX.newMXBeanProxy(connection, outputPortName,
                    OperatorOutputPortMXBean.class, true);
        }

        @Override
        public PeInputPortMXBean getPeInputPortMXBean(String domainId,
                String instanceId, BigInteger peId, int indexWithinPe) {
            ObjectName inputPortName = ObjectNameBuilder.peInputPort(domainId,
                    instanceId, peId, indexWithinPe);
            return JMX.newMXBeanProxy(connection, inputPortName,
                    PeInputPortMXBean.class, true);
        }

        @Override
        public PeOutputPortMXBean getPeOutputPortMXBean(String domainId,
                String instanceId, BigInteger peId, int indexWithinPe) {
            ObjectName outputPortName = ObjectNameBuilder.peOutputPort(
                    domainId, instanceId, peId, indexWithinPe);
            return JMX.newMXBeanProxy(connection, outputPortName,
                    PeOutputPortMXBean.class, true);
        }
    }
}
