package com.example.tasks.adapters.inbound.functions;

import com.example.tasks.application.port.inbound.ListTasksUseCase;
import com.example.tasks.domain.Task;
import org.springframework.cloud.function.function.Function;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component("listTasksByUser")
public class ListTasksFunction implements Function<ListTasksRequest, PagedTasksResponse> {
    
    private final ListTasksUseCase listTasksUseCase;
    
    public ListTasksFunction(ListTasksUseCase listTasksUseCase) {
        this.listTasksUseCase = listTasksUseCase;
    }
    
    @Override
    public PagedTasksResponse apply(ListTasksRequest request) {
        List<Task> tasks = listTasksUseCase.listTasks(
            request.userId(),
            request.page(),
            request.size()
        );
        
        List<TaskResponse> taskResponses = tasks.stream()
            .map(TaskResponse::from)
            .collect(Collectors.toList());
        
        return new PagedTasksResponse(taskResponses, request.page(), request.size());
    }
}