package com.gtn;

/**
 * <p>
 * (C) Copyright 2010-2025 Global Trading Network. All Rights Reserved.
 * <p/>
 * Created by uditha on 2025-03-06.
 */
public enum AuthStatus {
    AUTH_SUCCESS("AUTH_SUCCESS"),
    AUTH_FAILED ("AUTH_FAILED"),
    ASSERTION_ERROR ("ASSERTION_ERROR"),
    SEVER_AUTH_FAILED ("SEVER_AUTH_FAILED"),
    CUSTOMER_AUTH_FAILED ("CUSTOMER_AUTH_FAILED"),
    SERVER_TOKEN_RENEWED ("SERVER_TOKEN_RENEWED"),
    SERVER_TOKEN_RENEW_FAILED ("SERVER_TOKEN_RENEW_FAILED"),
    CUSTOMER_TOKEN_RENEWED ("CUSTOMER_TOKEN_RENEWED"),
    CUSTOMER_TOKEN_RENEW_FAILED ("CUSTOMER_TOKEN_RENEWED_FAILED"),
    AUTH_EXPIRED ("AUTH_EXPIRED");

    private final String value;

    AuthStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
