package com.gtngroup.util;

import java.util.HashMap;
import java.util.Map;

import org.json.*;

/**
 * (C) Copyright 2010-2025 Global Trading Network. All Rights Reserved.
 * Created by Uditha Nagahawatta on 2025-03-05.
 */
public class Params extends HashMap<String, Object> {

    public Params() {
    }

    public Params(String key, String value) {
        this();
        add(key, value);
    }

    public Params add(String key, Object value) {
        super.put(key, value);
        return this;
    }

    @Override
    public Object put(String key, Object value) {
        throw new RuntimeException("put method not allowed");
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
        throw new RuntimeException("putAll method not allowed");
    }

    /**
     * Get the string value
     * @param key to find the value
     * @return the value
     */
    public String getString(Object key) {
        return super.get(key).toString();
    }

    /**
     * Get the string value
     * @param key to find the value
     * @param defaultValue is key not found
     * @return the value of the default
     */
    public String getString(Object key, String defaultValue) {
        if (super.containsKey(key)) {
            return super.get(key).toString();
        } else {
            return defaultValue;
        }
    }

    public String toString() {
        JSONObject jsonObject = new JSONObject(this);
        return jsonObject.toString();
    }

    public Params setURL(String url) {
        url = url.trim();
        if (url.trim().endsWith("/")) {
            this.add("api_url", url.substring(0, url.length() - 1));
        } else {
            this.add("api_url", url);
        }
        return this;
    }

    public Params setAppKey(String appKey) {
        this.add("app_key", appKey);
        return this;
    }

    public Params setAppSecret(String appSecret) {
        this.add("app_secret", appSecret);
        return this;
    }

    public Params setInstitution(String institution) {
        this.add("institution", institution);
        return this;
    }

    public Params setInstitutionId(int institution) {
        this.add("institution_id", Integer.toString(institution));
        return this;
    }

    public Params setUserId(String userId) {
        this.add("user_id", userId);
        return this;
    }

    /*public Params setCustomerNumber(String customerNumber) {
        this.add("customer_number", customerNumber);
        return this;
    }*/

    public Params setPrivateKey(String privateKey) {
        this.add("private_key", privateKey);
        return this;
    }

    public Params setLoginName(String loginName) {
        this.add("user", loginName);
        return this;
    }

    /*public Params setPassword(String password) {
        this.add("password", password);
        return this;
    }*/

    public Params setChannel(String channel) {
        this.add("channel", channel);
        return this;
    }

}
