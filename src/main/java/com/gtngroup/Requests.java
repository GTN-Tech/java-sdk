package com.gtngroup;

import com.gtngroup.exception.RequestException;
import com.gtngroup.exception.UnknownCustomerException;
import com.gtngroup.util.Params;
import com.gtngroup.util.Utils;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * (C) Copyright 2010-2025 Global Trading Network. All Rights Reserved.
 * Created by Uditha Nagahawatta on 2025-02-20.
 */
public class Requests {

    private static final Logger LOGGER = LogManager.getLogger(Auth.class);

    /**
     * HTTP GET method
     *
     * @param endpoint to call
     * @return JSON response
     * @throws IOException on error
     */
    protected static JSONObject get(String endpoint, String customerNumber) throws Exception {
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
    protected static JSONObject get(String endpoint, Params payload, String customerNumber) throws Exception {

        int count = 0;
        StringBuilder paramString = new StringBuilder();
        for (String key : payload.keySet()) {
            if (count > 0) {
                paramString.append("&");
            } else {
                paramString.append("?");
            }
            paramString.append(key);
            paramString.append("=");
            paramString.append(payload.get(key));
            count++;
        }
        return sendRequest(endpoint + paramString, "GET", (String)null, customerNumber);
    }

    /**
     * HTTP POST method
     *
     * @param endpoint to call
     * @param payload  to send to the endpoint
     * @return JSON response
     * @throws IOException on error
     */
    protected static JSONObject post(String endpoint, Params payload, String customerNumber) throws Exception {
        return sendRequest(endpoint, "POST", payload.toString(), customerNumber);
    }

    /**
     * HTTP POST method
     *
     * @param endpoint to call
     * @param payload  to send to the endpoint
     * @param token    to send
     * @return JSON response
     * @throws IOException on error
     */
    protected static JSONObject post(String endpoint, Params payload, String token, String customerNumber) throws Exception {
        return sendRequest(endpoint, "POST", payload.toString(), token, customerNumber);
    }

    /**
     * HTTP PATCH method
     *
     * @param endpoint to call
     * @param payload  to send to the endpoint
     * @param token    to send
     * @return JSON response
     * @throws IOException on error
     */
    protected static JSONObject patch(String endpoint, Params payload, String token, String customerNumber) throws Exception {
        return sendRequest(endpoint, "PATCH", payload.toString(), token, customerNumber);
    }

    /**
     * HTTP DELETE method
     *
     * @param endpoint to call
     * @param payload  to send to the endpoint
     * @return JSON response
     * @throws IOException on error
     */
    public static JSONObject delete(String endpoint, Params payload, String customerNumber) throws Exception {
        int count = 0;
        StringBuilder paramString = new StringBuilder();
        for (String key : payload.keySet()) {
            if (count > 0) {
                paramString.append("&");
            } else {
                paramString.append("?");
            }
            paramString.append(key);
            paramString.append("=");
            paramString.append(payload.get(key));
            count++;
        }

        return sendRequest(endpoint + paramString, "DELETE", null, null, customerNumber);
    }

    /**
     * Send the request to the server
     *
     * @param endpoint to call
     * @param method   GET, POST, PATCH, DELETE
     * @param payload  to send with the endpoint
     * @return endpoint response as per the API documentation
     * @throws IOException on error
     */
    private static JSONObject sendRequest(String endpoint, String method, String payload, String customerNumber) throws Exception {
        return sendRequest(endpoint, method,payload, null, customerNumber);
    }

    /**
     * Send the request to the server
     *
     * @param endpoint to call
     * @param method   GET, POST, PATCH, DELETE
     * @param payload  to send with the endpoint
     * @param token    authorisation token
     * @return endpoint response as per the API documentation
     * @throws IOException on error
     */

    private static JSONObject sendRequest(String endpoint, String method, Params payload, String token) throws Exception {
        return sendRequest(endpoint, method, payload.toString(), token, null);
    }
    private static JSONObject sendRequest(String endpoint, String method, String payload, String token, String customerNumber) throws RequestException {

        URI url;
        String responseBody = null;
        int responseCode = -1;


        if (endpoint.charAt(0) != '/') {
            url = URI.create(Shared.getInstance().getAPIUrl() + "/" + endpoint);
        } else {
            url = URI.create(Shared.getInstance().getAPIUrl() + endpoint);
        }

        HttpClient client = HttpClient.newHttpClient();

        try {
            HttpRequest.Builder request = HttpRequest.newBuilder()
                    .uri(url)
                    .header("Content-Type", "application/json");

            if (token != null) {
                request.header("Authorization", token);
            } else {
                try {
                    if (customerNumber != null) {
                        token = Shared.getInstance().getCustomerAccessToken(customerNumber);
                    } else {
                        token = Utils.getMapData("accessToken", Shared.getInstance().getServerToken()).toString();
                    }
                    if (token != null) {
                        request.header("Authorization", "Bearer " + token);
                    } else {
                        throw new UnknownCustomerException(String.format("No valid token available for the customer %s. Try api.initCustomer() first", customerNumber));
                    }
                } catch (UnknownCustomerException e){
                    throw e;
                } catch (Exception e) {
                    //e.printStackTrace();
                }
            }

            request.header("Throttle-Key", Shared.getInstance().getAppKey());
            request.header("User-Agent", "GTN-SDK-Java/0.9.1");

            if (payload != null && !payload.isEmpty()) {
                request.method(method, HttpRequest.BodyPublishers.ofString(payload));
            }else {
                request.method(method, HttpRequest.BodyPublishers.ofString(""));
            }

            LOGGER.debug(String.format("requesting %s for %s%n", url, customerNumber == null ? "server token": "customer " + customerNumber));
            HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());

            responseCode = response.statusCode();

            JSONObject responseObject = new JSONObject();
            responseObject.put("http_status", responseCode);

            responseBody = response.body();
            LOGGER.debug("Response --> " + response.toString());
            responseObject.put("response", new JSONObject(responseBody));

            return responseObject;
        } catch (Exception e) {
            LOGGER.error("Error in request " + endpoint, e);
            throw new RequestException("Error in request " + endpoint, responseCode, responseBody);
        }
    }
}
