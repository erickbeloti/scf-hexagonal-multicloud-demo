package com.example.tasks.adapters.in.function;

import com.example.tasks.application.port.in.UpdateTaskUseCase;
import com.example.tasks.domain.model.Task;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component("updateTask")
public class UpdateTaskFn implements Function<Task, Task> {

    private final UpdateTaskUseCase updateTaskUseCase;

    public UpdateTaskFn(UpdateTaskUseCase updateTaskUseCase) {
        this.updateTaskUseCase = updateTaskUseCase;
    }

    @Override
    public Task apply(Task task) {
        return updateTaskUseCase.updateTask(task);
    }
}
