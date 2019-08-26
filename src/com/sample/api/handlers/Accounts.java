package com.sample.api.handlers;

import com.sample.api.models.AccountInfo;
import com.sample.api.models.AccountsTable;
import com.sample.api.models.ModelException;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class Accounts implements Base {
    private Map<String, AccountInfo> idempotence_tokens;
    private AccountsTable accounts;

    private Accounts() {
        idempotence_tokens = new HashMap<String, AccountInfo>();
        accounts = new AccountsTable();
    }

    private static Accounts handlerInstance = null;

    public static synchronized Accounts getInstance() {
        if (handlerInstance == null)
            handlerInstance = new Accounts();
        return handlerInstance;
    }

    public AccountsTable getAccounts() {
        return accounts;
    }

    @Override
    public String handleRequest(Request request) throws ApiException {
        switch (request.getAction()) {
            case "create":
                String token = request.getStringArg("session_id") + ":" + request.getStringArg("token");
                return createAccount(
                        token,
                        request.getDecArg("amount"));
            default:
                throw new ApiException(404, "URL not found: " + request.getURI());
        }
    }

    synchronized private String createAccount(String idempotence_token,
                                              BigDecimal amount) throws ApiException {
        AccountInfo account = idempotence_tokens.get(idempotence_token);
        if (account != null) {
            // Sequential calls are allowed only for the same data
            if (!account.amount.equals(amount))
                throw new ApiException(400, "amount does not match for token " + idempotence_token);
            if (account.id.isEmpty())
                throw new ApiException(500, "account was not created properly, use new token");
            return account.id;
        }
        // Create empty account object
        account = new AccountInfo();
        account.amount = amount;
        idempotence_tokens.put(idempotence_token, account);
        try {
            account.id = accounts.createAccount(amount);
        } catch (ModelException ex) {
            throw new ApiException(400, ex.getMessage());
        }
        return account.id;
    }

    synchronized public BigDecimal getAccountAmount(String id) throws ApiException {
        try {
            return accounts.getAccountAmount(id);
        } catch (ModelException ex) {
            throw new ApiException(400, ex.getMessage());
        }
    }
}
