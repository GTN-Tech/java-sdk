package com.gtn;

import com.gtn.util.Params;
import org.json.JSONObject;

import java.io.IOException;

/**
 * <p>
 * (C) Copyright 2025-2025 Global Market Technologies. All Rights Reserved.
 * <p/>
 * Created by Uditha Nagahawatta on 2025-02-20.
 */
public class GTNAPI {

    private static boolean initialised;

    public GTNAPI(Params params) {
        if (params.containsKey("user") && params.containsKey("password")) { // user mode
            Shared.getInstance().init(
                    params.getString("api_url"),
                    params.getString("app_key"),
                    null,
                    null,
                    params.getString("institution"),
                    null,
                    null,
                    params.getString("user"),
                    params.getString("password"));
        } else if (params.containsKey("customer_number")) { // customer mode
            Shared.getInstance().init(
                    params.getString("api_url"),
                    params.getString("app_key"),
                    params.getString("app_secret"),
                    params.getString("private_key"),
                    params.getString("institution"),
                    params.getString("customer_number"),
                    "1111",
                    null,
                    null);
        } else {
            Shared.getInstance().init( // institution mode
                    params.getString("api_url"),
                    params.getString("app_key"),
                    params.getString("app_secret"),
                    params.getString("private_key"),
                    params.getString("institution"),
                    null,
                    params.getString("user_id"),
                    null,
                    null);
        }
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
     * Initialise status.
     * init() method can be called only once per session
     *
     * @return
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
     * @return the customer token object
     */
    public JSONObject getCustomerToken() {
        return Shared.getInstance().getCustomerToken();
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
     * @throws IOException
     */
    public JSONObject get(String endpoint) throws IOException {
        return get(endpoint, new Params());
    }

    /**
     * HTTP GET method
     *
     * @param endpoint to call
     * @param payload  to send to the endpoint
     * @return JSON response
     * @throws IOException
     */
    public JSONObject get(String endpoint, Params payload) throws IOException {
        return Requests.get(endpoint, payload);
    }

    /**
     * HTTP POST method
     *
     * @param endpoint to call
     * @param payload  to send to the endpoint
     * @return JSON response
     * @throws IOException
     */
    public JSONObject post(String endpoint, Params payload) throws IOException {
        return Requests.post(endpoint, payload);
    }

    /**
     * HTTP PATCH method
     *
     * @param endpoint to call
     * @param payload  to send to the endpoint
     * @return JSON response
     * @throws IOException
     */
    public JSONObject patch(String endpoint, String payload) throws IOException {
        return Requests.patch(endpoint, payload, null);
    }

    /**
     * HTTP DELETE method
     *
     * @param endpoint to call
     * @param payload  to send to the endpoint
     * @return JSON response
     * @throws IOException
     */
    public JSONObject delete(String endpoint, Params payload) throws IOException {
        return Requests.delete(endpoint, payload);

    }

    public StreamingService getTradeStreamingService(MessageListener listener) {
        TradeStreaming.getInstance().addListener(listener);
        return TradeStreaming.getInstance();
    }

    public StreamingService getMarketDataStreamingService(MessageListener listener) {
        MarketDataStreaming.getInstance().addListener(listener);
        return MarketDataStreaming.getInstance();
    }
}

