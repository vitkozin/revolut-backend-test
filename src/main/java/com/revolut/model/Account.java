package com.revolut.model;

import java.math.BigDecimal;

public class Account {
    public long id;
    public BigDecimal balance;

    public Account(long id, BigDecimal balance) {
        this.id = id;
        this.balance = balance;
    }
}
