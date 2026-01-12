package com.gtngroup;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.gtngroup.exception.RequestException;
import com.gtngroup.util.Params;
import com.gtngroup.util.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * (C) Copyright 2010-2025 Global Trading Network. All Rights Reserved.
 * Created by Uditha Nagahawatta on 2025-02-20.
 */
public class Auth {

    private static boolean active;
    private static Thread thread;

    private static final Logger LOGGER = LogManager.getLogger(Auth.class);

    /**
     * Initialise the session
     *
     * @return the authentication status
     */
    protected static JSONObject init() {
        return initInstitution();
    }

    /**
     * Initialise in institution  mode
     *
     * @return the authentication status
     */
    private static JSONObject initInstitution() {
        AuthStatus authStatus = AuthStatus.AUTH_SUCCESS;

        String basicAuth = getBasicAuth(
                Shared.getInstance().getAppKey(),
                Shared.getInstance().getAppSecret()
        );

        String assertion = createToken(
                Shared.getInstance().getPrivateKey(),
                Shared.getInstance().getAppKey(),
                Shared.getInstance().getInstitution(),
                Shared.getInstance().getUserId()
        );

        if (assertion == null) {
            System.out.println("Assertion created failed");
            return Utils.returnStatus(-1, AuthStatus.ASSERTION_ERROR.toString());
        }

        Shared.getInstance().setAssertion(assertion);
        LOGGER.debug("Assertion created successfully");

        Params params = new Params("assertion", Shared.getInstance().getAssertion())
                .add("authorization", basicAuth);

        JSONObject serverToken = getServerToken(params, basicAuth);
        int httpStatus = serverToken.getInt("http_status");
        if (httpStatus == 200 &&
                !serverToken.getJSONObject("response").isEmpty()
                && (!Utils.hasMapKey("response/status", serverToken) ||
                Utils.getMapData("response/status", serverToken).equals("SUCCESS"))

        ) {
            LOGGER.debug("Institution authentication success");
            // test serverToken.getJSONObject("response").put("accessTokenExpiresAt", System.currentTimeMillis() + 35_000L);
            Shared.getInstance().setServerToken(serverToken.getJSONObject("response"));
        } else {
            if (serverToken.getJSONObject("response").isEmpty()) {
                LOGGER.debug("Server authentication failed. http status " + httpStatus + " and token is empty");
            } else {
                LOGGER.debug("Server authentication failed. status " +
                        httpStatus +
                        " and token status " + Utils.getMapData("response/status", serverToken));
            }
            authStatus = AuthStatus.SEVER_AUTH_FAILED;
        }

        if (authStatus.equals(AuthStatus.AUTH_SUCCESS)) {
            active = true;
            startThread();
        } else {
            if (httpStatus == 200) {
                httpStatus = -1;
            }
        }

        return new JSONObject().put("http_status", httpStatus).put("auth_status", authStatus.getValue());
    }

    /**
     * Login a customer
     * @param customerNumber to login
     * @return the token
     */
    protected static JSONObject initCustomer(String customerNumber) {

        AuthStatus authStatus = AuthStatus.AUTH_SUCCESS;

        String basicAuth = getBasicAuth(
                Shared.getInstance().getAppKey(),
                Shared.getInstance().getAppSecret()
        );

        Params params = new Params("customerNumber", customerNumber)
                .add("accessToken", Shared.getInstance().getServerToken().getString("accessToken"));
        JSONObject customerToken = getCustomerToken(params, basicAuth);

        LOGGER.debug(customerToken.toString());

        int httpStatus = customerToken.getInt("http_status");
        if (httpStatus == 200 &&
                !customerToken.getJSONObject("response").isEmpty() &&
                Utils.getMapData("response/status", customerToken).equals("SUCCESS")) {
            LOGGER.debug("Customer authentication success");

            // test customerToken.getJSONObject("response").put("accessTokenExpiresAt", System.currentTimeMillis() + 30_000L);
            Shared.getInstance().setCustomerToken(customerNumber, customerToken.getJSONObject("response"));

            LOGGER.debug("GTN API initiated in Customer mode.");
        } else {
            if (customerToken.getJSONObject("response").isEmpty()) {
                LOGGER.debug("Customer authentication failed. http status " + httpStatus + " and token is empty");
            } else {
                LOGGER.debug("Customer authentication failed. status " +
                        httpStatus +
                        " and token status " + Utils.getMapData("response/status", customerToken));
            }
            authStatus = AuthStatus.CUSTOMER_AUTH_FAILED;
        }

        return new JSONObject().put("http_status", httpStatus).put("auth_status", authStatus.getValue());
    }

    /**
     * Create the assertion
     *
     * @param privateKey  string
     * @param appKey      string
     * @param institution string
     * @param userId      string
     * @return the assertion
     */
    private static String createToken(String privateKey, String appKey,
                                      String institution, String userId) {
        try {

            Map<String, Object> payload = new HashMap<>();
            payload.put("instCode", institution);
            payload.put("userId", userId);
            payload.put("serverId", 1);
            int instId = Shared.getInstance().getInstitutionId();
            if (instId > 0) {
                payload.put("instId", instId);
            }

            Algorithm algorithm = getAlgorithm(privateKey);
            Date accessTokenExpiry = (new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000));

            Date now = new Date();

            return JWT.create()
                    .withIssuer(appKey)
                    .withPayload(payload)
                    .withExpiresAt(accessTokenExpiry)
                    .withIssuedAt(now)
                    .withNotBefore(now)
                    .sign(algorithm);

        } catch (Exception e) {
            LOGGER.error("Error creating the JWT token", e);
        }
        return null;
    }

    /**
     * Basic auth string creation
     *
     * @param username string
     * @param password string
     * @return the basic auth string
     */
    private static String getBasicAuth(String username, String password) {
        String token = base64Encode(username, password);
        return "Basic " + token;
    }

    /**
     * @param privateKey string
     * @return RSA string
     */
    private static Algorithm getAlgorithm(String privateKey) {
        return Algorithm.RSA256(null, getPrivateKey(privateKey));
    }

    /**
     * @param privateKeyString string
     * @return RSA private key
     */
    private static RSAPrivateKey getPrivateKey(String privateKeyString) {
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            byte[] pvtKeyBytes;
            if (isLikelyHex(privateKeyString)) {
                // base 16 encoded?
                pvtKeyBytes = base16Decoder(privateKeyString);
            } else {
                // maye be base 64
                pvtKeyBytes = Base64.getDecoder().decode(privateKeyString);
            }
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pvtKeyBytes);
            return (RSAPrivateKey) kf.generatePrivate(keySpec);
        } catch (Exception e) {
            LOGGER.error("Error creating private key", e);
        }
        return null;
    }

    /**
     * Base 16 encode the given string
     *
     * @param hex string
     * @return the encoded string
     */
    public static byte[] base16Decoder(final String hex) {
        final byte[] bts = new byte[hex.length() / 2];
        for (int i = 0; i < bts.length; ++i) {
            bts[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return bts;
    }

    /**
     * Check for hexa decimal patten in s string
     *
     * @param str to check
     * @return true if the sring is in HEX format
     */
    private static boolean isLikelyHex(String str) {
        // Base16 is typically only hexadecimal characters and even length
        return str.matches("(?i)^[0-9a-f]+$") && str.length() % 2 == 0;
    }

    /**
     * Get the server token from the server
     *
     * @param params map
     * @param token  assertion
     * @return the server token
     */
    private static JSONObject getServerToken(Params params, String token) {
        try {

            return Requests.post(Shared.getAuthURL("SERVER_TOKEN"), params, token, null);
        } catch (RequestException e){
            LOGGER.error(e.getMessage(), e);
            return new JSONObject().put("http_status", e.getStatusCode()).put("response", new JSONObject("message", e.getDescription()));
        } catch (Exception e) {
            LOGGER.error("Error getting the Server token", e);
            return new JSONObject().put("http_status", -1).put("response", new JSONObject());
        }
    }

    /**
     * Fetch the refreshed server token
     *
     * @return new token
     */
    private static JSONObject getServerTokenRefresh() {
        try {
            String refreshToken = Shared.getInstance().getServerToken().getString("refreshToken");
            Params params = new Params("refreshToken", refreshToken);
            return Requests.post(Shared.getAuthURL("SERVER_TOKEN_REFRESH"), params, null);
        } catch (Exception e) {
            LOGGER.error("Error getting the Server refresh token", e);
            return new JSONObject().put("http_status", -1).put("response", new JSONObject());
        }
    }

    /**
     * Get the customer token from the server
     *
     * @param params map
     * @param token  assertion
     * @return the customer token
     */
    private static JSONObject getCustomerToken(Params params, String token) {
        try {

            return Requests.post(Shared.getAuthURL("CUSTOMER_TOKEN"), params, "", null);
        } catch (Exception e) {
            LOGGER.error("Error getting the Customer token", e);
            return new JSONObject().put("http_status", -1).put("response", new JSONObject());
        }
    }

    /**
     * Fetch the refreshed server token
     *
     * @return new token
     */
    protected static JSONObject getCustomerTokenRefresh(String customerNumber) {
        try {
            String refreshToken = Shared.getInstance().getCustomerRefreshToken(customerNumber);
            Params params = new Params("refreshToken", refreshToken);
            return Requests.post(Shared.getAuthURL("CUSTOMER_TOKEN_REFRESH"), params, null);
        } catch (Exception e) {
            LOGGER.error("Error getting the Customer refresh token", e);
            return new JSONObject().put("http_status", -1).put("response", new JSONObject());
        }

    }

    /**
     * Logout the current session
     */
    protected static void logout() {
        active = false;
        try {
            thread.interrupt();
        } catch (Exception e) {
            // ignore
        }
    }

    /**
     * Base 64 encode username and password
     *
     * @param username string
     * @param password string
     * @return encoded string
     */
    private static String base64Encode(String username, String password) {
        String credentials = username + ":" + password;
        return Base64.getEncoder().encodeToString(credentials.getBytes());
    }

    /**
     * Start the key refresh thread
     */
    private static void startThread() {
        thread = new Thread(() -> {
            while(active) {
                boolean success = keyRefresh();
                if (!success) {
                    break;
                }
                sleep(100);
                List<String> activeCustomers = Shared.getInstance().getActiveCustomers();
                for (String customerNumber : activeCustomers) {
                    keyRefresh(customerNumber);
                    sleep(100);
                }
                sleep(10_000);
            }
            Shared.getInstance().removeAllCustomers(); // get rid of all customers
            //todo disconnect streaming clients
        });
        thread.start();
    }

    /**
     * refresh the server token
     * @return true if successful
     */
    private static boolean keyRefresh() {
        return keyRefresh(null);
    }

    /**
     * Refresh tokens when called by the thread
     */
    private static boolean keyRefresh(String customerNumber) {
        boolean serverToken = false;
        try {
            //LOGGER.debug("refreshing -> " + customerNumber);
            JSONObject token;

            if (customerNumber == null) {
                token = Shared.getInstance().getServerToken();
                serverToken = true;
            } else {
                token = Shared.getInstance().getCustomerToken(customerNumber);
            }
            long expMillis;

            // first check the refresh token
            try {
                expMillis = token.getLong("refreshTokenExpiresAt");
            } catch (JSONException e) {
                expMillis = token.getLong("refreshTokenExpiry"); //DWM
            }
            long delta = expMillis - System.currentTimeMillis();

            if (delta < 5_000) {
                LOGGER.debug("refresh token expired. logging out");
                if (serverToken) {
                    logout();
                } else {
                    Shared.getInstance().removeCustomer(customerNumber);
                }
                return false;
            } else {
                // now check the access token
                try {
                    expMillis = token.getLong("accessTokenExpiresAt");
                } catch (JSONException e) {
                    expMillis = token.getLong("tokenExpiry"); // DWM
                }
                delta = expMillis - System.currentTimeMillis();
                //LOGGER.debug("access delta " + delta);
                if (delta < 5_000) {
                    //LOGGER.debug("--> refreshing %s access token\n", serverToken ? "server" : "customer");
                    JSONObject refreshedToken;
                    if (serverToken) {
                        refreshedToken = getServerTokenRefresh();
                    } else {
                        refreshedToken = getCustomerTokenRefresh(customerNumber);
                    }
                    int http_status = refreshedToken.getInt("http_status");
                    if (http_status == 200) {
                        if (serverToken) {
                            if (refreshedToken.getJSONObject("response").optString("status").equalsIgnoreCase("FAILED")) {
                                initInstitution();
                            } else {
                                Shared.getInstance().setServerToken(refreshedToken.getJSONObject("response"));
                            }
                        } else {
                            if (refreshedToken.getJSONObject("response").optString("status").equalsIgnoreCase("FAILED")) {
                                Shared.getInstance().removeCustomer(customerNumber);
                                initCustomer(customerNumber);
                            } else {
                                Shared.getInstance().setCustomerToken(customerNumber, refreshedToken.getJSONObject("response"));
                            }
                        }
                        return true;
                    } else {
                        LOGGER.debug(String.format("Error refreshing the %s token: %d sec to expire. http status %d\n", serverToken ? "server" : "customer", (delta / 1000L), http_status));
                        return false;
                    }
                } else { // no need to refresh now
                    return true;
                }
            }
        } catch (Exception e) {
            if (serverToken) {
                LOGGER.error("Error refreshing the Server token", e);
            } else {
                LOGGER.error("Error refreshing the Customer token for customer: " + customerNumber, e);
            }
            return true;
        }

    }

    private static void sleep(long millies) {
        try {
            Thread.sleep(millies);
        } catch (InterruptedException e) {
            // ignore
        }
    }
}
