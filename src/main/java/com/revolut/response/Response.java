package com.revolut.response;

public class Response {
    private Status status;
    private String message;

    public Response(Status status) {
        this.status = status;
    }
    public Response(Status status, String message) {
        this.status = status;
        this.message = message;
    }
}
