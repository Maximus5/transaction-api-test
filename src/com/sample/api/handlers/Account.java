package com.sample.api.handlers;

import com.sample.api.models.ModelException;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Account implements Base {
    private Account() {}

    private static Account handlerInstance = null;

    public static synchronized Account getInstance() {
        if (handlerInstance == null)
            handlerInstance = new Account();
        return handlerInstance;
    }

    @Override
    public String handleRequest(Request request) throws ApiException {
        switch (request.getAction()) {
            case "amount":
                return getAmount(request.getStringArg("account_id"));
            default:
                throw new ApiException(404, "URL not found: " + request.getURI());
        }
    }

    private String getAmount(String account_id) throws ApiException {
        BigDecimal amount = Accounts.getInstance().getAccountAmount(account_id)
                .setScale(2, RoundingMode.HALF_UP);
        return amount.toString();
    }
}
