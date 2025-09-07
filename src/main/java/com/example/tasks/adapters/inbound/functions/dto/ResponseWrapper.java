package com.example.tasks.adapters.inbound.functions.dto;

import java.util.List;

public class ResponseWrapper<T> {
    private int status;
    private T data;
    private List<Error> errors;
    private String message;

    public ResponseWrapper() {}

    public ResponseWrapper(int status, T data, List<Error> errors, String message) {
        this.status = status;
        this.data = data;
        this.errors = errors;
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public List<Error> getErrors() {
        return errors;
    }

    public void setErrors(List<Error> errors) {
        this.errors = errors;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static class Builder<T> {
        private int status;
        private T data;
        private List<Error> errors;
        private String message;

        public Builder<T> status(int status) {
            this.status = status;
            return this;
        }

        public Builder<T> data(T data) {
            this.data = data;
            return this;
        }

        public Builder<T> errors(List<Error> errors) {
            this.errors = errors;
            return this;
        }

        public Builder<T> message(String message) {
            this.message = message;
            return this;
        }

        public ResponseWrapper<T> build() {
            return new ResponseWrapper<>(status, data, errors, message);
        }
    }
}
