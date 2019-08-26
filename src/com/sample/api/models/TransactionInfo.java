package com.sample.api.models;

import java.math.BigDecimal;

public class TransactionInfo {
    public String id;
    public String src_account_id;
    public String dst_account_id;
    public BigDecimal amount;
    public boolean completed = false;
    public String error_message;
}
