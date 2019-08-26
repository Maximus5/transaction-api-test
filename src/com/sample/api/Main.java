package com.sample.api;

import com.sample.api.handlers.*;
import com.sample.api.utils.Arguments;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;

public class Main {

    public static void main(String[] args) {
        Arguments arguments = Arguments.parseArguments(args);
        if (arguments == null)
            return;
        HttpServer server = null;
        try {
            server = HttpServer.create(new InetSocketAddress(arguments.getPort()), 0);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.printf("ERROR: bind to port %d%n", arguments.getPort());
            return;
        }
        HttpContext context = server.createContext("/v1");
        context.setHandler(Main::handleRequest);
        System.out.printf("INFO: server started at port %d%n", arguments.getPort());
        server.start();
    }

    private static void handleRequest(HttpExchange exchange) throws IOException {
        try {
            if (!exchange.getRequestMethod().equals("POST"))
                throw new ApiException(400, "method not supported: '" + exchange.getRequestMethod() + "'");
            Request request = new Request(exchange);
            if (!request.getVersion().equals("v1"))
                throw new ApiException(404, "URL not found: " + request.getURI());
            String response = handleRequestV1(request);
            System.out.println("response: code=200, message=" + response);
            exchange.sendResponseHeaders(200, response.getBytes().length); //response code and length
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        } catch (ApiException ex) {
            String message = ex.getMessage();
            System.out.println("response: code=" + ex.getCode() + ", message=" + message);
            exchange.sendResponseHeaders(ex.getCode(), message.getBytes().length); //response code and length
            OutputStream os = exchange.getResponseBody();
            os.write(message.getBytes());
            os.close();
        } catch (Exception ex) {
            String message = ex.toString();
            System.out.println("response: code=500, message=" + message);
            exchange.sendResponseHeaders(500, message.getBytes().length); //response code and length
            OutputStream os = exchange.getResponseBody();
            os.write(message.getBytes());
            os.close();
        }
    }

    private static String handleRequestV1(Request request) throws ApiException {
        Base handler = null;
        String query = "";
        switch (request.getObject()) {
            case "sessions":
                handler = Sessions.getInstance();
                break;
            case "account":
                handler = Account.getInstance();
                break;
            case "accounts":
                handler = Accounts.getInstance();
                break;
            case "transactions":
                handler = Transactions.getInstance();
                break;
            default:
                throw new ApiException(404, "URL not found: " + request.getURI());
        }
        return handler.handleRequest(request);
    }
}
