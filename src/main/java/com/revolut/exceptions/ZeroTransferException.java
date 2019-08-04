package com.revolut.exceptions;

public class ZeroTransferException extends TransferException {
    public ZeroTransferException(String message) {
        super(message);
    }
}
