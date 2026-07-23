package ai.unlearn.dlq.core.autoconfigure;

import ai.unlearn.dlq.core.client.ArtemisManagementClient;
import ai.unlearn.dlq.core.config.ArtemisConnectionProperties;
import ai.unlearn.dlq.core.service.DlqBrowserService;
import ai.unlearn.dlq.core.service.impl.DlqBrowserServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Wires up the DLQ core beans from {@code dlq.artemis.*} properties. Any application that adds
 * dlq-core as a dependency gets an {@link ArtemisManagementClient} and {@link DlqBrowserService}
 * for free, connected on startup and closed on shutdown.
 */
@AutoConfiguration
@EnableConfigurationProperties(ArtemisConnectionProperties.class)
public class DlqCoreAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper dlqObjectMapper() {
        return new ObjectMapper();
    }

    @Bean(initMethod = "start", destroyMethod = "close")
    @ConditionalOnMissingBean
    public ArtemisManagementClient artemisManagementClient(ArtemisConnectionProperties properties) {
        return new ArtemisManagementClient(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public DlqBrowserService dlqBrowserService(ArtemisManagementClient artemisManagementClient,
                                                ObjectMapper dlqObjectMapper) {
        return new DlqBrowserServiceImpl(artemisManagementClient, dlqObjectMapper);
    }
}
