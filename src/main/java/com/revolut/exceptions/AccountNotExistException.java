package com.revolut.exceptions;

public class AccountNotExistException extends TransferException {
    public AccountNotExistException(String message) {
        super(message);
    }
}
