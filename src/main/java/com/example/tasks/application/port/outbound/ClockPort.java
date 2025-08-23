package com.example.tasks.application.port.outbound;

import java.time.LocalDateTime;

public interface ClockPort {
    LocalDateTime now();
}