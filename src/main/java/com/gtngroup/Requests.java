package com.gtngroup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import com.gtngroup.util.Params;
import com.gtngroup.util.Utils;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * (C) Copyright 2010-2025 Global Trading Network. All Rights Reserved.
 * Created by Uditha Nagahawatta on 2025-02-20.
 */
public class Requests {

    /**
     * HTTP GET method
     * @param endpoint to call
     * @return JSON response
     * @throws IOException on error
     */
    protected static JSONObject get(String endpoint) throws IOException {
        return get(endpoint, new Params());
    }

    /**
     * HTTP GET method
     * @param endpoint to call
     * @param payload to send to the endpoint
     * @return JSON response
     * @throws IOException on error
     */
    protected static JSONObject get(String endpoint, Params payload) throws IOException {

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
        return sendRequest(endpoint + paramString, "GET", null);
    }

    /**
     * HTTP POST method
     * @param endpoint to call
     * @param payload to send to the endpoint
     * @return JSON response
     * @throws IOException on error
     */
    protected static JSONObject post(String endpoint, Params payload) throws IOException {
        return sendRequest(endpoint, "POST", payload.toString());
    }

    /**
     * HTTP POST method
     * @param endpoint to call
     * @param payload to send to the endpoint
     * @param token to send
     * @return JSON response
     * @throws IOException on error
     */
    protected static JSONObject post(String endpoint, String payload, String token) throws IOException {
        return sendRequest(endpoint, "POST", payload, token);
    }

    /**
     * HTTP PATCH method
     * @param endpoint to call
     * @param payload to send to the endpoint
     * @param token to send
     * @return JSON response
     * @throws IOException on error
     */
    protected static JSONObject patch(String endpoint, String payload, String token) throws IOException {
        return sendRequest(endpoint, "PATCH", payload, token);
    }

    /**
     * HTTP DELETE method
     * @param endpoint to call
     * @param payload to send to the endpoint
     * @return JSON response
     * @throws IOException on error
     */
    public static JSONObject delete(String endpoint, Params payload) throws IOException {
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

        return sendRequest(endpoint + paramString, "DELETE", null, null);
    }

    /**
     * Send the request to the server
     * @param endpoint to call
     * @param method GET, POST, PATCH, DELETE
     * @param payload to send with the endpoint
     * @return endpoint response as per the API documentation
     * @throws IOException on error
     */
    private static JSONObject sendRequest(String endpoint, String method, String payload) throws IOException {
        return sendRequest(endpoint, method,payload, null);
    }

    /**
     * Send the request to the server
     * @param endpoint to call
     * @param method GET, POST, PATCH, DELETE
     * @param payload to send with the endpoint
     * @param token authorisation token
     * @return endpoint response as per the API documentation
     * @throws IOException on error
     */
    private static JSONObject sendRequest(String endpoint, String method, String payload, String token) throws IOException {

        URL url;

        if (endpoint.charAt(0) != '/'){
            url = new URL(Shared.getInstance().getAPIUrl() + "/" + endpoint);
        } else {
            url = new URL(Shared.getInstance().getAPIUrl() + endpoint);
        }


        HttpURLConnection conn = null;
        BufferedReader reader = null;
        StringBuilder response;

        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setRequestProperty("Content-Type", "application/json");

            if (token != null) {
                conn.setRequestProperty("Authorization",  token);
            } else {
                try {
                    token = Utils.getMapData("accessToken", Shared.getInstance().getToken()).toString();
                    if (token != null) {
                        conn.setRequestProperty("Authorization", "Bearer " + token);
                    }
                } catch (Exception e) {
                    //e.printStackTrace();
                }
            }

            conn.setRequestProperty("Throttle-Key", Shared.getInstance().getAppKey());
            conn.setRequestProperty("User-Agent", "GTN-SDK-Java");

            if (payload != null && !payload.isEmpty()) {
                conn.setDoOutput(true);
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = payload.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            }

            int responseCode = conn.getResponseCode();

            JSONObject responseObject = new JSONObject();
            responseObject.put("http_status", responseCode);

            if (responseCode >= 200 && responseCode < 300) {
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            } else {
                reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
            }

            response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            //System.out.println(response);
            try {
                responseObject.put("response", new JSONObject(response.toString()));
            } catch (JSONException e) {
                e.printStackTrace();
                responseObject.put("response", "{}");
            }
            return responseObject;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            try {
                if (reader != null) reader.close();
            } catch (Exception e) {
                // do nothing
            }
            try {
                if (conn != null) conn.disconnect();
            } catch (Exception e) {
                // do nothing
            }
        }
    }
}

