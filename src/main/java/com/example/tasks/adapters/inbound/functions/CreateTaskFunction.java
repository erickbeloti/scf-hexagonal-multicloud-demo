package com.example.tasks.adapters.inbound.functions;

import com.example.tasks.application.port.inbound.CreateTaskUseCase;
import com.example.tasks.domain.Task;
import org.springframework.cloud.function.function.Function;
import org.springframework.stereotype.Component;

@Component("createTask")
public class CreateTaskFunction implements Function<CreateTaskRequest, TaskResponse> {
    
    private final CreateTaskUseCase createTaskUseCase;
    
    public CreateTaskFunction(CreateTaskUseCase createTaskUseCase) {
        this.createTaskUseCase = createTaskUseCase;
    }
    
    @Override
    public TaskResponse apply(CreateTaskRequest request) {
        Task task = createTaskUseCase.createTask(
            request.userId(), 
            request.description(), 
            request.priority()
        );
        
        return TaskResponse.from(task);
    }
}