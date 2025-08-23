package com.example.tasks.adapters.inbound.functions;

import com.example.tasks.application.port.inbound.UpdateTaskUseCase;
import com.example.tasks.domain.Task;
import org.springframework.cloud.function.function.Function;
import org.springframework.stereotype.Component;

@Component("updateTask")
public class UpdateTaskFunction implements Function<UpdateTaskRequest, TaskResponse> {
    
    private final UpdateTaskUseCase updateTaskUseCase;
    
    public UpdateTaskFunction(UpdateTaskUseCase updateTaskUseCase) {
        this.updateTaskUseCase = updateTaskUseCase;
    }
    
    @Override
    public TaskResponse apply(UpdateTaskRequest request) {
        Task task = updateTaskUseCase.updateTask(
            request.taskId(),
            request.userId(), 
            request.description(), 
            request.priority(),
            request.status()
        );
        
        return TaskResponse.from(task);
    }
}