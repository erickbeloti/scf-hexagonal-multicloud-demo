package com.example.tasks.application.port.outbound;

import java.time.Clock;

public interface ClockPort {
    Clock systemClock();
}

