package ai.unlearn.dlq.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dlq.artemis")
public class ArtemisConnectionProperties {

    /** Core protocol URL, e.g. tcp://localhost:61616 (or the SSL acceptor's port, e.g. tcp://localhost:61617) */
    private String brokerUrl = "tcp://localhost:61616";

    private String username;

    private String password;

    /** Address the broker listens on for management operations. */
    private String managementAddress = "activemq.management";

    private long connectTimeoutMs = 5000;

    private long requestTimeoutMs = 5000;

    private Ssl ssl = new Ssl();

    public String getBrokerUrl() {
        return brokerUrl;
    }

    public void setBrokerUrl(String brokerUrl) {
        this.brokerUrl = brokerUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getManagementAddress() {
        return managementAddress;
    }

    public void setManagementAddress(String managementAddress) {
        this.managementAddress = managementAddress;
    }

    public long getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(long connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public long getRequestTimeoutMs() {
        return requestTimeoutMs;
    }

    public void setRequestTimeoutMs(long requestTimeoutMs) {
        this.requestTimeoutMs = requestTimeoutMs;
    }

    public Ssl getSsl() {
        return ssl;
    }

    public void setSsl(Ssl ssl) {
        this.ssl = ssl;
    }

    /**
     * Client-side TLS configuration. {@code trustStore*} is what the client uses to verify the
     * broker's certificate; {@code keyStore*} is the client's own identity and is only required
     * if the broker's acceptor is configured with {@code needClientAuth=true} (mutual TLS).
     */
    public static class Ssl {

        private boolean enabled = false;

        private String keyStorePath;

        private String keyStorePassword;

        private String keyStoreType = "JKS";

        private String trustStorePath;

        private String trustStorePassword;

        private String trustStoreType = "JKS";

        /** Whether to verify the broker certificate's hostname against the connection host. */
        private boolean verifyHost = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getKeyStorePath() {
            return keyStorePath;
        }

        public void setKeyStorePath(String keyStorePath) {
            this.keyStorePath = keyStorePath;
        }

        public String getKeyStorePassword() {
            return keyStorePassword;
        }

        public void setKeyStorePassword(String keyStorePassword) {
            this.keyStorePassword = keyStorePassword;
        }

        public String getKeyStoreType() {
            return keyStoreType;
        }

        public void setKeyStoreType(String keyStoreType) {
            this.keyStoreType = keyStoreType;
        }

        public String getTrustStorePath() {
            return trustStorePath;
        }

        public void setTrustStorePath(String trustStorePath) {
            this.trustStorePath = trustStorePath;
        }

        public String getTrustStorePassword() {
            return trustStorePassword;
        }

        public void setTrustStorePassword(String trustStorePassword) {
            this.trustStorePassword = trustStorePassword;
        }

        public String getTrustStoreType() {
            return trustStoreType;
        }

        public void setTrustStoreType(String trustStoreType) {
            this.trustStoreType = trustStoreType;
        }

        public boolean isVerifyHost() {
            return verifyHost;
        }

        public void setVerifyHost(boolean verifyHost) {
            this.verifyHost = verifyHost;
        }
    }
}
