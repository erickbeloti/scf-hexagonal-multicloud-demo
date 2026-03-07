package com.example.tasks.adapters.outbound.azure;

import com.azure.cosmos.CosmosClientBuilder;
import com.azure.spring.data.cosmos.config.AbstractCosmosConfiguration;
import com.azure.spring.data.cosmos.config.CosmosConfig;
import com.azure.spring.data.cosmos.core.ResponseDiagnostics;
import com.azure.spring.data.cosmos.core.ResponseDiagnosticsProcessor;
import com.azure.spring.data.cosmos.repository.config.EnableCosmosRepositories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.Nullable;

/**
 * Azure Cosmos DB configuration for the azure profile.
 * Configures connection and repository scanning.
 */
@Configuration
@Profile("azure")
@EnableCosmosRepositories(basePackages = "com.example.tasks.adapters.outbound.azure")
public class AzureConfiguration extends AbstractCosmosConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(AzureConfiguration.class);

    @Value("${azure.cosmos.uri}")
    private String uri;

    @Value("${azure.cosmos.key}")
    private String key;

    @Value("${azure.cosmos.database:tasks-db}")
    private String database;

    @Value("${azure.cosmos.queryMetricsEnabled:false}")
    private boolean queryMetricsEnabled;

    @Bean
    public CosmosClientBuilder cosmosClientBuilder() {
        return new CosmosClientBuilder()
            .endpoint(uri)
            .key(key);
    }

    @Override
    protected String getDatabaseName() {
        return database;
    }

    @Override
    public CosmosConfig cosmosConfig() {
        return CosmosConfig.builder()
            .enableQueryMetrics(queryMetricsEnabled)
            .responseDiagnosticsProcessor(new ResponseDiagnosticsProcessor() {
                @Override
                public void processResponseDiagnostics(@Nullable ResponseDiagnostics responseDiagnostics) {
                    if (responseDiagnostics != null && logger.isDebugEnabled()) {
                        logger.debug("Cosmos DB diagnostics: {}", responseDiagnostics);
                    }
                }
            })
            .build();
    }
}

