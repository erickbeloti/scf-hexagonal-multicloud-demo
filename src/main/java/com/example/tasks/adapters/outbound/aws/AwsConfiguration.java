package com.example.tasks.adapters.outbound.aws;

import io.awspring.cloud.dynamodb.DynamoDbTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Configuration
@Profile("aws")
public class AwsConfiguration {

    @Value("${AWS_DYNAMODB_TABLE_NAME:tasks-dev}")
    private String tableName;

    @Bean
    public DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }

    @Bean
    public TableSchema<TaskEntity> taskEntityTableSchema() {
        return TableSchema.fromBean(TaskEntity.class);
    }

    @Bean
    public DynamoDbTable<TaskEntity> taskTable(DynamoDbEnhancedClient enhancedClient, TableSchema<TaskEntity> tableSchema) {
        return enhancedClient.table(tableName, tableSchema);
    }

    @Bean
    public DynamoDbTemplate dynamoDbTemplate(DynamoDbEnhancedClient enhancedClient, DynamoDbTable<TaskEntity> taskTable) {
        return new DynamoDbTemplate(enhancedClient);
    }
}
