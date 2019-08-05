package com.revolut.exceptions;

public class SameAccountsException extends TransferException {
    public SameAccountsException(String message) {
        super(message);
    }
}
