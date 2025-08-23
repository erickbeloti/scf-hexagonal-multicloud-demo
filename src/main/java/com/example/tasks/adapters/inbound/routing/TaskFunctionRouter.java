package com.example.tasks.adapters.inbound.functions;

import org.springframework.cloud.function.function.Function;
import org.springframework.cloud.function.function.FunctionCatalog;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Function;

@Component("taskRouter")
public class TaskFunctionRouter implements Function<Map<String, Object>, Object> {
    
    private final FunctionCatalog functionCatalog;
    
    public TaskFunctionRouter(FunctionCatalog functionCatalog) {
        this.functionCatalog = functionCatalog;
    }
    
    @Override
    public Object apply(Map<String, Object> request) {
        String operation = (String) request.get("operation");
        
        if (operation == null) {
            throw new IllegalArgumentException("Operation field is required");
        }
        
        Function<Object, Object> function = functionCatalog.lookup(operation);
        if (function == null) {
            throw new IllegalArgumentException("Unknown operation: " + operation);
        }
        
        return function.apply(request);
    }
}