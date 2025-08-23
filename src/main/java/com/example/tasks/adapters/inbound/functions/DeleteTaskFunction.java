package com.example.tasks.adapters.inbound.functions;

import com.example.tasks.application.port.inbound.DeleteTaskUseCase;
import org.springframework.cloud.function.function.Function;
import org.springframework.stereotype.Component;

@Component("deleteTask")
public class DeleteTaskFunction implements Function<DeleteTaskRequest, DeleteTaskResponse> {
    
    private final DeleteTaskUseCase deleteTaskUseCase;
    
    public DeleteTaskFunction(DeleteTaskUseCase deleteTaskUseCase) {
        this.deleteTaskUseCase = deleteTaskUseCase;
    }
    
    @Override
    public DeleteTaskResponse apply(DeleteTaskRequest request) {
        deleteTaskUseCase.deleteTask(
            request.taskId(),
            request.userId()
        );
        
        return new DeleteTaskResponse("Task deleted successfully");
    }
}