package com.example.tasks.config;

import com.example.tasks.application.port.outbound.ClockPort;
import com.example.tasks.application.port.outbound.IdGeneratorPort;
import com.example.tasks.application.port.outbound.TaskRepositoryPort;
import com.example.tasks.application.service.TaskService;
import com.example.tasks.domain.policy.TaskCreationPolicy;
import com.example.tasks.domain.policy.TaskUpdatePolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.UUID;

@Configuration
public class AppConfig {

    @Bean
    public TaskCreationPolicy taskCreationPolicy() { return new TaskCreationPolicy(); }

    @Bean
    public TaskUpdatePolicy taskUpdatePolicy() { return new TaskUpdatePolicy(); }

    @Bean
    public ClockPort clockPort() {
        return Clock::systemUTC;
    }

    @Bean
    public IdGeneratorPort idGeneratorPort() {
        return () -> UUID.randomUUID().toString();
    }

    @Bean
    public TaskService taskService(TaskRepositoryPort repository,
                                   ClockPort clockPort,
                                   IdGeneratorPort idGeneratorPort,
                                   TaskCreationPolicy creationPolicy,
                                   TaskUpdatePolicy updatePolicy) {
        return new TaskService(repository, clockPort, idGeneratorPort, creationPolicy, updatePolicy);
    }
}

