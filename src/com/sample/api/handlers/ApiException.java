package com.sample.api.handlers;

public class ApiException extends Exception {
    private int code_;

    public ApiException(int code, String message) {
        super(message);
        code_ = code;
    }

    public int getCode() { return code_; }
}
