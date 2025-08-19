package com.example.tasks.adapters.in.function;

import com.example.tasks.application.port.in.GetTaskUseCase;
import com.example.tasks.adapters.in.function.dto.IdRequest;
import com.example.tasks.domain.model.Task;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Function;

@Component("getTask")
public class GetTaskFn implements Function<IdRequest, Optional<Task>> {

    private final GetTaskUseCase getTaskUseCase;

    public GetTaskFn(GetTaskUseCase getTaskUseCase) {
        this.getTaskUseCase = getTaskUseCase;
    }

    @Override
    public Optional<Task> apply(IdRequest request) {
        return getTaskUseCase.getTask(request.id());
    }
}
