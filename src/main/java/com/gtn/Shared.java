package com.gtn;

import org.json.JSONObject;

/**
 * <p>
 * (C) Copyright 2010-2025 Global Trading Network. All Rights Reserved.
 * <p/>
 * Created by uditha on 2025-02-20.
 */
public class Shared {
    private static Shared instance;
    private String apiUrl;
    private String appKey;
    private String appSecret;
    private String privateKey;
    private String institution;
    private String customerNumber;
    private String userId;
    private JSONObject customerToken;
    private JSONObject serverToken;
    private String assertion;
    private String user;
    private String pass;
    private boolean ready;

    private Shared() {
        // Private constructor to prevent instantiation
    }

    protected static Shared getInstance() {
        if (instance == null) {
            instance = new Shared();
        }
        return instance;
    }

    protected void init(String apiUrl, String appKey, String appSecret, String privateKey,
                     String institution, String customerNumber, String userId, String user, String pass) {
        this.apiUrl = apiUrl;
        this.appKey = appKey;
        this.appSecret = appSecret;
        this.privateKey = privateKey;
        this.institution = institution;
        this.customerNumber = customerNumber;
        this.userId = userId;
        this.user = user;
        this.pass = pass;
        this.ready = true;
    }

    protected void destroy() {
        this.ready = false;
        this.apiUrl = null;
        this.appKey = null;
        this.appSecret = null;
        this.privateKey = null;
        this.institution = null;
        this.customerNumber = null;
        this.customerToken = null;
        this.serverToken = null;
    }

    /**
     * Get the URL of the API hub
     * @return the url
     */
    protected String getAPIUrl() {
        return apiUrl;
    }

    /**
     * Get the App key assigned to the institution
     * @return the app key
     */
    protected String getAppKey() {
        return appKey;
    }

    /**
     * Get the App secret assigned to the institution
     * @return the app secret
     */
    protected String getAppSecret() {
        return appSecret;
    }

    /**
     * Get the private key assigned to the institution
     * @return private key key
     */
    protected String getPrivateKey() {
        return privateKey;
    }

    /**
     * Get the institution code
     * @return the institution code
     */
    protected String getInstitution() {
        return institution;
    }

    /**
     * Get the logged in customer number
     * @return the customer number
     */
    protected String getCustomerNumber() {
        if (isUserMode()){
            return getCustomerToken().getString("customerNumber");
        } else {
            return customerNumber;
        }
    }

    /**
     * Get the user id
     * @return the user id
     */
    protected String getUserId() {
        return userId;
    }

    /**
     * Get the customer token for the session
     * @return the customer token
     */
    protected JSONObject getCustomerToken() {
        return customerToken;
    }

    /**
     * Get the active token, could be institution or the customer
     * @return the token
     */
    protected JSONObject getToken() {
        if (isCustomerMode()) {
            return customerToken;
        } else {
            return serverToken;
        }
    }

    /**
     * Get the server token for the session
     * @return the server token
     */
    protected JSONObject getServerToken() {
        return serverToken;
    }

    /**
     * Get the assertion
     * @return the assertion
     */
    protected String getAssertion() {
        return assertion;
    }

    /**
     * Set the assertion
     * @param assertion string
     */
    protected void setAssertion(String assertion) {
        this.assertion = assertion;
    }

    /**
     * Set the customer token for the session
     * @param customerToken the customer token object
     */
    protected void setCustomerToken(JSONObject customerToken) {
        this.customerToken = customerToken;
    }

    /**
     * Set the server token for the session
     * @param serverToken object
     */
    protected void setServerToken(JSONObject serverToken) {
        this.serverToken = serverToken;
    }


    /**
     * @return the username
     */
    protected String getUser() {
        return user;
    }

    /**
     * @return the password
     */
    protected String getPass() {
        return pass;
    }

    /**
     * @return true if the session is in customer mode
     */
    protected boolean isCustomerMode(){
        return customerNumber != null || user != null;
    }

    /**
     * @return true if the session is in server mode
     */
    protected boolean isServerMode(){
        return customerNumber == null && user == null;
    }

    /**
     * @return true if the session is in user mode
     */
    protected boolean isUserMode(){
        return user != null;
    }
}

