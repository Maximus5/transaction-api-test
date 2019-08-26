package com.sample.api.handlers;

public class Sessions implements Base {
    private long next_session_id = 1;

    private Sessions() {}

    private static Sessions handlerInstance = null;

    public static synchronized Sessions getInstance() {
        if (handlerInstance == null)
            handlerInstance = new Sessions();
        return handlerInstance;
    }

    @Override
    public String handleRequest(Request request) throws ApiException {
        switch (request.getAction()) {
            case "create":
                return createSession();
            default:
                throw new ApiException(404, "URL not found: " + request.getURI());
        }
    }

    synchronized private String createSession() {
        return "S" + String.format("%05d", next_session_id++);
    }
}
