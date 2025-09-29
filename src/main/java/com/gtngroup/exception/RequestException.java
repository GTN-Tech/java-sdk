package com.gtngroup.exception;

/**
 * <p>
 * (C) Copyright 2025-2025 Global Trading Network. All Rights Reserved.
 * </p>
 * Created by uditha on 2025-08-19.
 */
public class RequestException  extends Exception{

    private final int statusCode;
    private final String description;

    public RequestException(String message, int statusCode, String description) {
        super(message);
        this.statusCode = statusCode;
        this.description = description;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getDescription() {
        return description;
    }
}
