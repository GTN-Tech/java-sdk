package com.gtngroup;

import com.gtngroup.util.Params;
import com.gtngroup.util.Utils;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * (C) Copyright 2010-2025 Global Trading Network. All Rights Reserved.
 * Created by Uditha Nagahawatta on 2025-02-20.
 */
public class Shared {
    private static Shared instance;
    private String apiUrl;
    private String appKey;
    private String appSecret;
    private String privateKey;
    private String institution;
    private int institutionId;
    private String userId;
    private JSONObject serverToken;
    private String assertion;
    private String channel;

    private static Params authMap;
    private final Map<String, JSONObject> customerMap;

    private Shared() {
        initAuthMap();
        customerMap = Collections.synchronizedMap(new HashMap<>());
        // Private constructor to prevent instantiation
    }

    protected static Shared getInstance() {
        if (instance == null) {
            instance = new Shared();
        }
        return instance;
    }

    private static void initAuthMap() {
        authMap = new Params()
                .add("DWM_SERVER_TOKEN", "/microinvest/v1.0/auth/server/token")
                .add("DWM_SERVER_TOKEN_REFRESH", "/microinvest/v1.0/auth/server/refresh-token")
                .add("DWM_CUSTOMER_TOKEN", "/microinvest/v1.0/auth/client/token")
                .add("DWM_CUSTOMER_TOKEN_REFRESH", "/microinvest/v1.0/auth/client/refresh-token")

                .add("TRADE_SERVER_TOKEN", "/trade/auth/token")
                .add("TRADE_SERVER_TOKEN_REFRESH", "/trade/auth/token/refresh")
                .add("TRADE_CUSTOMER_TOKEN", "/trade/auth/customer/token")
                .add("TRADE_CUSTOMER_TOKEN_REFRESH", "/trade/auth/customer/token/refresh");
    }

    protected static String getAuthURL(String code) {
        String urlID = Shared.getInstance().getChannel() + "_" + code;
        return authMap.getString(urlID);
    }

    protected void init(String apiUrl, String appKey, String appSecret, String privateKey,
                        String institution, String userId,
                        String channel, String institutionId) {
        this.apiUrl = apiUrl;
        this.appKey = appKey;
        this.appSecret = appSecret;
        this.privateKey = privateKey;
        this.institution = institution;
        this.institutionId = Integer.parseInt(institutionId);
        this.userId = userId;
        this.channel = channel;
    }

    /**
     * Get the URL of the API hub
     *
     * @return the url
     */
    protected String getAPIUrl() {
        return apiUrl;
    }

    /**
     * Get the App key assigned to the institution
     *
     * @return the app key
     */
    protected String getAppKey() {
        return appKey;
    }

    /**
     * Get the App secret assigned to the institution
     *
     * @return the app secret
     */
    protected String getAppSecret() {
        return appSecret;
    }

    /**
     * Get the private key assigned to the institution
     *
     * @return private key key
     */
    protected String getPrivateKey() {
        return privateKey;
    }

    /**
     * Get the institution code
     *
     * @return the institution code
     */
    protected String getInstitution() {
        return institution;
    }

    /**
     * @return the institution id
     */
    public int getInstitutionId() {
        return institutionId;
    }

    /**
     * Get the user id
     *
     * @return the user id
     */
    protected String getUserId() {
        return userId;
    }

    /**
     * Get the server token for the session
     *
     * @return the server token
     */
    protected JSONObject getServerToken() {
        return serverToken;
    }

    /**
     * Get the assertion
     *
     * @return the assertion
     */
    protected String getAssertion() {
        return assertion;
    }

    /**
     * Set the assertion
     *
     * @param assertion string
     */
    protected void setAssertion(String assertion) {
        this.assertion = assertion;
    }

    /**
     * Set the server token for the session
     *
     * @param serverToken object
     */
    protected void setServerToken(JSONObject serverToken) {
        this.serverToken = serverToken;
    }

    /**
     * @return the channel code
     */
    public String getChannel() {
        return channel;
    }

    /**
     * Get the token of a customer already logged in
     * @param customerNumber in request
     * @return the token
     */
    protected JSONObject getCustomerToken(String customerNumber) {
        return customerMap.get(customerNumber);
    }

    /**
     * Set the token of a customer already logged in
     * @param customerNumber in request
     * @param token of the customer
     */
    protected void setCustomerToken(String customerNumber, JSONObject token) {
        customerMap.put(customerNumber, token);
    }

    /**
     * Get the access token of a customer logged in
     * @param customerNumber in request
     * @return the access token
     */
    protected String getCustomerAccessToken(String customerNumber) {
        try {
            return Utils.getMapData("accessToken", customerMap.get(customerNumber)).toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get the refresh token of a customer logged in
     * @param customerNumber in request
     * @return the access token
     */
    protected String getCustomerRefreshToken(String customerNumber) {
        try {
            return Utils.getMapData("refreshToken", customerMap.get(customerNumber)).toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * remove a customer session
     * @param customerNumber to remove
     */
    protected void removeCustomer(String customerNumber) {
        customerMap.remove(customerNumber);
    }


    /**
     * list all active customer numbers
     * @return list of customer numbers
     */
    protected synchronized List<String> getActiveCustomers() {
        return new ArrayList<>(customerMap.keySet());
    }

    /**
     * remove all customers in the session
     */
    protected void removeAllCustomers() {
        customerMap.clear();
    }
}

