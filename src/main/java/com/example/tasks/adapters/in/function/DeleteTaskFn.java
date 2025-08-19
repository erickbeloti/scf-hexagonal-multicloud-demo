package com.example.tasks.adapters.in.function;

import com.example.tasks.application.port.in.DeleteTaskUseCase;
import com.example.tasks.adapters.in.function.dto.IdRequest;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component("deleteTask")
public class DeleteTaskFn implements Consumer<IdRequest> {

    private final DeleteTaskUseCase deleteTaskUseCase;

    public DeleteTaskFn(DeleteTaskUseCase deleteTaskUseCase) {
        this.deleteTaskUseCase = deleteTaskUseCase;
    }

    @Override
    public void accept(IdRequest request) {
        deleteTaskUseCase.deleteTask(request.id());
    }
}
