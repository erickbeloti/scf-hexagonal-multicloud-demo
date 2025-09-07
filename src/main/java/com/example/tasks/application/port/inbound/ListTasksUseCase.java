package com.example.tasks.application.port.inbound;

import com.example.tasks.domain.*;
import java.util.List;

public interface ListTasksUseCase {
    List<Task> listTasks(UserId userId, int page, int size);
}
