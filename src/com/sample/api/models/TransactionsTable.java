package com.sample.api.models;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class TransactionsTable {
    private Map<String, TransactionInfo> transactions;
    private long next_transaction_id = 1;

    public TransactionsTable() {
        transactions = new HashMap<String, TransactionInfo>();
    }

    synchronized public String createTransaction(AccountsTable accounts_table,
                                                 String src_account_id,
                                                 String dst_account_id,
                                                 BigDecimal amount) throws ModelException {
        // Create new transaction now
        String new_id = "T" + String.format("%05d", next_transaction_id++);
        TransactionInfo transaction = new TransactionInfo();
        transactions.put(new_id, transaction);
        transaction.id = new_id;
        transaction.src_account_id = src_account_id;
        transaction.dst_account_id = dst_account_id;
        transaction.amount = amount;
        try {
            accounts_table.transfer(src_account_id, dst_account_id, amount);
            transaction.completed = true;
        } catch (ModelException ex) {
            transaction.error_message = ex.getMessage();
            throw ex;
        }
        return new_id;
    }
}
