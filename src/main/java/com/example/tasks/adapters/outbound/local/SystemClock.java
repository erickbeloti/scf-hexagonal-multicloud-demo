package com.example.tasks.adapters.outbound.local;

import com.example.tasks.application.port.outbound.ClockPort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Profile("local")
public class SystemClock implements ClockPort {
    
    @Override
    public LocalDateTime now() {
        return LocalDateTime.now();
    }
}