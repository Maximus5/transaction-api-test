package com.sample.api.models;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class AccountsTable {
    private Map<String, AccountInfo> accounts;
    private long next_account_id = 1;
    private static final BigDecimal min_transfer_amount = new BigDecimal(0.01);

    public AccountsTable() {
        accounts = new HashMap<String, AccountInfo>();
    }

    // returns new account id
    synchronized public String createAccount(BigDecimal amount) throws ModelException {
        // Create new account now
        String new_id = "A" + String.format("%05d", next_account_id++);
        AccountInfo account = new AccountInfo();
        accounts.put(new_id, account);
        account.id = new_id;
        account.amount = amount;
        return account.id;
    }

    synchronized public BigDecimal getAccountAmount(String id) throws ModelException {
        AccountInfo account = accounts.get(id);
        if (account == null)
            throw new ModelException("account " + id + " does not exist");
        return account.amount;
    }

    synchronized public void transfer(String src_account_id,
                                      String dst_account_id,
                                      BigDecimal amount) throws ModelException {
        if (src_account_id.equals(dst_account_id))
            throw new ModelException("same accounts aren't allowed");
        AccountInfo src_account = accounts.get(src_account_id);
        if (src_account == null)
            throw new ModelException("source account does not exist");
        AccountInfo dst_account = accounts.get(dst_account_id);
        if (dst_account == null)
            throw new ModelException("destination account does not exist");
        if (src_account == dst_account)
            throw new ModelException("same accounts aren't allowed");
        if (amount == null || amount.compareTo(min_transfer_amount) < 0)
            throw new ModelException("transfer amount is lesser than " + min_transfer_amount.toString());
        if (src_account.amount.compareTo(amount) < 0)
            throw new ModelException("source account does not have enough funds");

        // We are inside syncronized block, no one may modify amounts outside
        BigDecimal new_src_amount = src_account.amount.subtract(amount);
        BigDecimal new_dst_amount = dst_account.amount.add(amount);
        // Now we go to complex atomic operation
        src_account.amount = new_src_amount;
        dst_account.amount = new_dst_amount;
        // Transaction completed
    }
}
