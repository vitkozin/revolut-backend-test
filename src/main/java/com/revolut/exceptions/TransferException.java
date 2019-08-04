package com.revolut.exceptions;

public abstract class TransferException extends Exception {
    TransferException(String message) {
        super(message);
    }
}
