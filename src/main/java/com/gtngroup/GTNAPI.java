package com.gtngroup;

import com.gtngroup.util.Params;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

/**
 * (C) Copyright 2025-2025 Global Market Technologies. All Rights Reserved.
 * Created by Uditha Nagahawatta on 2025-02-20.
 */
public class GTNAPI {

    private static boolean initialised;

    private static final Logger LOGGER = LogManager.getLogger(Auth.class);

    /**
     * initialise the session
     *
     * @param params to start the session
     */
    public GTNAPI(Params params) {

        Shared.getInstance().init( // institution mode
                params.getString("api_url"),
                params.getString("app_key"),
                params.getString("app_secret"),
                params.getString("private_key"),
                params.getString("institution"),
                params.getString("user_id"),
                params.getString("channel", "TRADE"),
                params.getString("institution_id", "-1"));
    }

    /**
     * Initialise the session
     *
     * @return the authentication status
     */
    public JSONObject init() {
        if (isInitialised()) {
            throw new RuntimeException("Already initialised. init() can be called only once per session");
        }
        setInitialised();
        return Auth.init();
    }

    /**
     * Login a customer
     *
     * @param customerNumber to login
     * @return the token
     */
    public JSONObject initCustomer(String customerNumber) {
        return Auth.initCustomer(customerNumber);
    }


    /**
     * Initialise status.
     * init() method can be called only once per session
     *
     * @return true if already initialised
     */
    public static boolean isInitialised() {
        return initialised;
    }

    /**
     * internal call to set initialise status
     */
    private static void setInitialised() {
        GTNAPI.initialised = true;
    }


    /**
     * logout the session
     */
    public static void stop() {
        Auth.logout();
        TradeStreaming.getInstance().disconnect();
        MarketDataStreaming.getInstance().disconnect();
    }

    /**
     * HTTP GET method
     *
     * @param endpoint to call
     * @return JSON response
     * @throws IOException on error
     */
    public JSONObject get(String endpoint) throws Exception {
        return get(endpoint, new Params());
    }

    /**
     * HTTP GET method
     *
     * @param endpoint       to call
     * @param customerNumber requesting
     * @return JSON response
     * @throws IOException on error
     */
    public JSONObject get(String endpoint, String customerNumber) throws Exception {
        return get(endpoint, new Params(), customerNumber);
    }

    /**
     * HTTP GET method
     *
     * @param endpoint to call
     * @param payload  to send to the endpoint
     * @return JSON response
     * @throws IOException on error
     */
    public JSONObject get(String endpoint, Params payload) throws Exception {
        return Requests.get(endpoint, payload, null);
    }

    /**
     * HTTP GET method
     *
     * @param endpoint       to call
     * @param payload        to send to the endpoint
     * @param customerNumber requesting
     * @return JSON response
     * @throws IOException on error
     */
    public JSONObject get(String endpoint, Params payload, String customerNumber) throws Exception {
        return Requests.get(endpoint, payload, customerNumber);
    }

    /**
     * HTTP POST method
     *
     * @param endpoint to call
     * @param payload  to send to the endpoint
     * @return JSON response
     * @throws IOException on error
     */
    public JSONObject post(String endpoint, Params payload) throws Exception {
        return Requests.post(endpoint, payload, null);
    }

    /**
     * HTTP POST method
     *
     * @param endpoint       to call
     * @param payload        to send to the endpoint
     * @param customerNumber requesting
     * @return JSON response
     * @throws IOException on error
     */
    public JSONObject post(String endpoint, Params payload, String customerNumber) throws Exception {
        return Requests.post(endpoint, payload, customerNumber);
    }

    /**
     * HTTP PATCH method
     *
     * @param endpoint to call
     * @param payload  to send to the endpoint
     * @return JSON response
     * @throws IOException on error
     */
    public JSONObject patch(String endpoint, Params payload) throws Exception {
        return Requests.patch(endpoint, payload, null, null);
    }

    /**
     * HTTP PATCH method
     *
     * @param endpoint       to call
     * @param payload        to send to the endpoint
     * @param customerNumber requesting
     * @return JSON response
     * @throws IOException on error
     */
    public JSONObject patch(String endpoint, Params payload, String customerNumber) throws Exception {
        return Requests.patch(endpoint, payload, null, customerNumber);
    }

    /**
     * HTTP DELETE method
     *
     * @param endpoint to call
     * @param payload  to send to the endpoint
     * @return JSON response
     * @throws IOException on error
     */
    public JSONObject delete(String endpoint, Params payload) throws Exception {
        return Requests.delete(endpoint, payload, null);

    }

    /**
     * HTTP DELETE method
     *
     * @param endpoint       to call
     * @param payload        to send to the endpoint
     * @param customerNumber requesting
     * @return JSON response
     * @throws IOException on error
     */
    public JSONObject delete(String endpoint, Params payload, String customerNumber) throws Exception {
        return Requests.delete(endpoint, payload, customerNumber);

    }

    /**
     * Get the trade streaming service
     *
     * @param listener to receive events
     * @return TradeStreaming
     */
    public StreamingService getTradeStreamingService(MessageListener listener) {
        TradeStreaming.getInstance().addListener(listener);
        return TradeStreaming.getInstance();
    }

    /**
     * Get the market data streaming service
     *
     * @param listener to receive events
     * @return the MarketDataStreaming
     */
    public StreamingService getMarketDataStreamingService(MessageListener listener) {
        MarketDataStreaming.getInstance().addListener(listener);
        return MarketDataStreaming.getInstance();
    }

    /**
     * Check whether the customer is already logged in and valid
     *
     * @param customerNumber of the customer
     * @return the validity status
     */
    public boolean isCustomerValid(String customerNumber) {
        try {
            JSONObject token = Shared.getInstance().getCustomerToken(customerNumber);

            if (token == null) { // no such account
                return false;
            }

            long expMillis;
            try {
                expMillis = token.getLong("refreshTokenExpiresAt");
            } catch (JSONException e) {
                expMillis = token.getLong("refreshTokenExpiry"); //DWM
            }
            long delta = expMillis - System.currentTimeMillis();

            if (delta <= 0) { // expired account
                Shared.getInstance().removeCustomer(customerNumber);
                return false;
            } else {
                return true;
            }
        } catch (JSONException e) {
            LOGGER.error("Error checking customer token validity", e);
            Shared.getInstance().removeCustomer(customerNumber);
            return false;
        }
    }


    /**
     * get the list of logged in customers
     * may contain expired customers also
     *
     * @return the list of customer numbers
     */
    public synchronized List<String> getActiveCustomers() {
        return Shared.getInstance().getActiveCustomers();
    }

    /**
     * Get the current access token for a given customer.
     *
     * @param customerNumber the customer identifier
     * @return the access token for the customer, or null if not available
     */
    public String getCustomerAccessToken(String customerNumber) {
        return Shared.getInstance().getCustomerAccessToken(customerNumber);
    }
}

