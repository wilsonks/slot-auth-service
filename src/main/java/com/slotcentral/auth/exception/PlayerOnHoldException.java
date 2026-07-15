package com.slotcentral.auth.exception;

public class PlayerOnHoldException extends RuntimeException {
    public PlayerOnHoldException(String uid) {
        super("Player card is on hold: " + uid);
    }
}
