package com.example.tasks.adapters.in.function;

import com.example.tasks.application.port.in.CreateTaskUseCase;
import com.example.tasks.domain.model.Task;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component("createTask")
public class CreateTaskFn implements Function<Task, Task> {

    private final CreateTaskUseCase createTaskUseCase;

    public CreateTaskFn(CreateTaskUseCase createTaskUseCase) {
        this.createTaskUseCase = createTaskUseCase;
    }

    @Override
    public Task apply(Task task) {
        return createTaskUseCase.createTask(task);
    }
}
