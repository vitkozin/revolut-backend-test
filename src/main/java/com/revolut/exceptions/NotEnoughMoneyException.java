package com.revolut.exceptions;

public class NotEnoughMoneyException extends TransferException {
    public NotEnoughMoneyException(String message) {
        super(message);
    }
}
