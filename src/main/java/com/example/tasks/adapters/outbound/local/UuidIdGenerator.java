package com.example.tasks.adapters.outbound.local;

import com.example.tasks.application.port.outbound.IdGeneratorPort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Profile("local")
public class UuidIdGenerator implements IdGeneratorPort {
    
    @Override
    public String generateId() {
        return UUID.randomUUID().toString();
    }
}