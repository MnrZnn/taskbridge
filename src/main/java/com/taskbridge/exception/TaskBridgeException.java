package com.taskbridge.exception;

public abstract class TaskBridgeException extends RuntimeException {

    protected TaskBridgeException(String message) {
        super(message);
    }
}
