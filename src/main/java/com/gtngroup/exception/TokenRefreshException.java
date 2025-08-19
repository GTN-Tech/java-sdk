package com.gtngroup.exception;

/**
 * <p>
 * (C) Copyright 2025-2025 Global Trading Network. All Rights Reserved.
 * </p>
 * Created by uditha on 2025-08-11.
 */
public class TokenRefreshException extends Exception{

    private final int httpStatus;

    public TokenRefreshException(String message, int httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
