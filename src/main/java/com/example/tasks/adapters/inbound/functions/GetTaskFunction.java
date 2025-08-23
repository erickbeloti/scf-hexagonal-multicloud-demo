package com.example.tasks.adapters.inbound.functions;

import com.example.tasks.application.port.inbound.GetTaskUseCase;
import com.example.tasks.domain.Task;
import org.springframework.cloud.function.function.Function;
import org.springframework.stereotype.Component;

@Component("getTaskById")
public class GetTaskFunction implements Function<GetTaskRequest, TaskResponse> {
    
    private final GetTaskUseCase getTaskUseCase;
    
    public GetTaskFunction(GetTaskUseCase getTaskUseCase) {
        this.getTaskUseCase = getTaskUseCase;
    }
    
    @Override
    public TaskResponse apply(GetTaskRequest request) {
        Task task = getTaskUseCase.getTask(
            request.taskId(),
            request.userId()
        );
        
        return TaskResponse.from(task);
    }
}