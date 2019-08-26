package com.sample.api.handlers;

import com.sample.api.models.ModelException;
import com.sample.api.models.TransactionInfo;
import com.sample.api.models.TransactionsTable;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class Transactions implements Base {
    private Map<String, TransactionInfo> idempotence_tokens;
    private TransactionsTable transactions;

    private Transactions() {
        idempotence_tokens = new HashMap<String, TransactionInfo>();
        transactions = new TransactionsTable();
    }

    private static Transactions handlerInstance = null;

    public static synchronized Transactions getInstance() {
        if (handlerInstance == null)
            handlerInstance = new Transactions();
        return handlerInstance;
    }

    @Override
    public String handleRequest(Request request) throws ApiException {
        switch (request.getAction()) {
            case "create":
                String token = request.getStringArg("session_id") + ":" + request.getStringArg("token");
                return createTransaction(
                        token,
                        request.getStringArg("src_account_id"),
                        request.getStringArg("dst_account_id"),
                        request.getDecArg("amount"));
            default:
                throw new ApiException(404, "URL not found: " + request.getURI());
        }
    }

    synchronized private String createTransaction(String idempotence_token, String src_account_id,
                                                  String dst_account_id, BigDecimal amount) throws ApiException {
        TransactionInfo transaction = idempotence_tokens.get(idempotence_token);
        if (transaction != null) {
            // Sequential calls are allowed only for the same data
            if (!transaction.src_account_id.equals(src_account_id))
                throw new ApiException(400, "src_account_id does not match for token " + idempotence_token);
            if (!transaction.dst_account_id.equals(dst_account_id))
                throw new ApiException(400, "dst_account_id does not match for token " + idempotence_token);
            if (!transaction.amount.equals(amount))
                throw new ApiException(400, "amount does not match for token " + idempotence_token);
            if (!transaction.completed)
                throw new ApiException(400, transaction.error_message);
            return transaction.id;
        }
        // Create an empty transaction object
        transaction = new TransactionInfo();
        transaction.src_account_id = src_account_id;
        transaction.dst_account_id = dst_account_id;
        transaction.amount = amount;
        idempotence_tokens.put(idempotence_token, transaction);
        // Do actual transfer
        try {
            transaction.id = transactions.createTransaction(
                    Accounts.getInstance().getAccounts(),
                    src_account_id,
                    dst_account_id,
                    amount
            );
            transaction.completed = true;
        } catch (ModelException ex) {
            transaction.error_message = ex.getMessage();
            throw new ApiException(400, ex.getMessage());
        }
        return transaction.id;
    }
}
