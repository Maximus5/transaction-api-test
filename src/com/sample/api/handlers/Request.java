package com.sample.api.handlers;

import com.sun.net.httpserver.HttpExchange;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

public class Request {
    String uri_;
    String[] parts_;
    Map<String, String> parameters_;

    // e.g. "v1/transactions/create?token=123&source_account_id=567&amount=777.12"
    public Request(HttpExchange exchange) throws ApiException {
        uri_ = exchange.getRequestURI().getPath();
        if (uri_.isEmpty() || uri_ == "/")
            throw new ApiException(400, "empty URL");
        System.out.println("uri: " + uri_);
        parts_ = uri_.substring(1).split("/");
        if (parts_.length != 3)
            throw new ApiException(400, "URI should have form /v1/<object>/<action>");
        parameters_ = new HashMap<String, String>();
        parseQuery(exchange.getRequestURI().getQuery());
    }

    void parseQuery(String query) throws ApiException {
        System.out.println("query: " + query);
        if (query == null || query.isEmpty())
            return;
        final String[] parms = query.split("&");
        for (String param : parms) {
            final String[] key_value = param.split("=", 2);
            if (key_value.length != 2)
                throw new ApiException(400, "URL should contain key=value pairs");
            parameters_.put(key_value[0], key_value[1]);
        }
    }

    public String getURI() {
        return uri_;
    }

    public String getVersion() {
        return parts_[0];
    }

    // e.g. transaction, account
    public String getObject() {
        return parts_[1];
    }

    // e.g. create, info
    public String getAction() {
        return parts_[2];
    }

    public String getStringArg(String key) throws ApiException {
        String value = parameters_.get(key);
        if (value == null || value.isEmpty())
            throw new ApiException(400, key + " should be specified");
        return value;
    }

    public BigDecimal getDecArg(String key) throws ApiException {
        try {
            return new BigDecimal(getStringArg(key)).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            throw new ApiException(400, key + " should be decimal");
        }
    }
}
