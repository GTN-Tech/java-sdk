package com.gtn;

/**
 * <p>
 * (C) Copyright 2010-2025 Global Trading Network. All Rights Reserved.
 * <p/>
 * Created by uditha on 2025-02-20.
 */

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.gtn.util.Params;
import com.gtn.util.Utils;
import org.json.JSONObject;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Auth {

    private static boolean active;
    private static Thread thread;


    /**
     * Initialise the session
     * @return the authentication status
     */
    protected static JSONObject init() {
        if (Shared.getInstance().isUserMode()) {
            return initUser();
        } else {
            return initInstitution();
        }
    }

    /**
     * Initialise in user/pass mode
     * @return the authentication status
     */
    private static JSONObject initUser() {
        AuthStatus authStatus = AuthStatus.AUTH_SUCCESS;


        JSONObject customerToken = getCustomerTokenForUser(
                Shared.getInstance().getUser(),
                Shared.getInstance().getPass(),
                Shared.getInstance().getInstitution());


        int status = customerToken.getInt("http_status");

        if (status == 200 &&
                Utils.getMapData("response/status", customerToken).equals("SUCCESS")) {
            System.out.println("Customer authentication success");

            Shared.getInstance().setCustomerToken(customerToken.getJSONObject("response"));
            System.out.println("GTN API initiated in Customer mode.");
        } else {
            if (customerToken.getJSONObject("response").isEmpty()) {
                System.out.println("Customer authentication failed. http status {status} and token is None");
            } else {
                System.err.println("Customer authentication failed. status {status} and token status {customer_token['status']}");
            }
            authStatus = AuthStatus.CUSTOMER_AUTH_FAILED;
        }

        if (authStatus == AuthStatus.AUTH_SUCCESS) {
            startThread();
        } else {
            if (status == 200) {
                status = -1;
            }
        }

        return new JSONObject().put("http_status", status).put("auth_status", authStatus.getValue());

    }

    /**
     * Initialise in institution  mode
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
        System.out.println("Assertion created successfully");


        Params params = new Params("assertion", Shared.getInstance().getAssertion());

        JSONObject serverToken = getServerToken(params, basicAuth);
        int httpStatus = serverToken.getInt("http_status");
        if (httpStatus == 200 &&
                !serverToken.getJSONObject("response").isEmpty() &&
                Utils.getMapData("response/status", serverToken).equals("SUCCESS")) {
            System.out.println("Institution authentication success");
            Shared.getInstance().setServerToken(serverToken.getJSONObject("response"));

//            Shared.getInstance().getServerToken().put("accessTokenExpiresAt", System.currentTimeMillis() + 60_000L);
//            System.out.println("exp set " + (System.currentTimeMillis() + 60_000L));

            if (Shared.getInstance().isCustomerMode()) {
                params = new Params("customerNumber", Shared.getInstance().getCustomerNumber())
                        .add("accessToken", Shared.getInstance().getServerToken().getString("accessToken"));
                JSONObject customerToken = getCustomerToken(params, basicAuth);

                httpStatus = customerToken.getInt("http_status");
                if (httpStatus == 200 &&
                        !customerToken.getJSONObject("response").isEmpty() &&
                        Utils.getMapData("response/status", customerToken).equals("SUCCESS")) {
                    System.out.println("Customer authentication success");

                    Shared.getInstance().setCustomerToken(customerToken.getJSONObject("response"));
//                    Shared.getInstance().getCustomerToken().put("accessTokenExpiresAt", System.currentTimeMillis() + 60_000L);
//                    System.out.println("exp set " + (System.currentTimeMillis() + 60_000L));

                    System.out.println("GTN API initiated in Customer mode.");
                } else {
                    if (customerToken.getJSONObject("response").isEmpty()) {
                        System.out.println("Customer authentication failed. http status " + httpStatus + " and token is empty");
                    } else {
                        System.out.println("Customer authentication failed. status " +
                                httpStatus +
                                " and token status " + Utils.getMapData("response/status", customerToken));
                    }
                    authStatus = AuthStatus.CUSTOMER_AUTH_FAILED;
                }
            }

        } else {
            if (serverToken.getJSONObject("response").isEmpty()) {
                System.out.println("Server authentication failed. http status " + httpStatus + " and token is empty");
            } else {
                System.out.println("Server authentication failed. status " +
                        httpStatus +
                        " and token status " + Utils.getMapData("response/status", serverToken));
            }
            authStatus = AuthStatus.SEVER_AUTH_FAILED;
        }

        if (authStatus.equals(AuthStatus.AUTH_SUCCESS)) {
            startThread();
        } else {
            if (httpStatus == 200) {
                httpStatus = -1;
            }
        }

        return new JSONObject().put("http_status", httpStatus).put("auth_status", authStatus.getValue());
    }

    /**
     * Create the assertion
     * @param privateKey string
     * @param appKey string
     * @param institution string
     * @param userId string
     * @return the assertion
     */
    private static String createToken(String privateKey, String appKey,
                                     String institution, String userId) {
        try {

            Map<String, String> payload = new HashMap<>();
            payload.put("instCode", institution);
            payload.put("userId", userId);

            Algorithm algorithm = getAlgorithm(privateKey);
            Date accessTokenExpiry = (new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000));

            String assertion = JWT.create()
                    .withIssuer(appKey)
                    .withPayload(payload)
                    .withExpiresAt(accessTokenExpiry)
                    .sign(algorithm);
            return assertion;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Basic auth string creation
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
            byte[] pvtKeyBytes = base16Decoder(privateKeyString);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pvtKeyBytes);
            return (RSAPrivateKey) kf.generatePrivate(keySpec);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Base 16 encode the given string
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
     * Get the server token from the server
     * @param params map
     * @param token assertion
     * @return
     */
    private static JSONObject getServerToken(Params params, String token) {
        try {

            return Requests.post("/trade/auth/token", params.toString(), token);
        } catch (Exception e) {
            e.printStackTrace();
            return new JSONObject().put("http_status", -1).put("response", new JSONObject());
        }
    }

    /**
     * Fetch the refreshed server token
     * @return new token
     */
    private static JSONObject getServerTokenRefresh() {
        try {
            String refreshToken = Shared.getInstance().getServerToken().getString("refreshToken");
            Params params = new Params("refreshToken", refreshToken);
            return Requests.post("/trade/auth/token/refresh", params);
        } catch (Exception e) {
            e.printStackTrace();
            return new JSONObject().put("http_status", -1).put("response", new JSONObject());
        }
    }

    /**
     * Get the customer token from the server
     * @param params map
     * @param token assertion
     * @return
     */
    private static JSONObject getCustomerToken(Params params, String token) {
        try {

            return Requests.post("/trade/auth/customer/token", params.toString(), token);
        } catch (Exception e) {
            e.printStackTrace();
            return new JSONObject().put("http_status", -1).put("response", new JSONObject());
        }
    }

    /**
     * Fetch the refreshed server token
     * @return new token
     */
    private static JSONObject getCustomerTokenRefresh() {
        try {
            String refreshToken = Shared.getInstance().getCustomerToken().getString("refreshToken");
            Params params = new Params("refreshToken", refreshToken);
            return Requests.post("/trade/auth/customer/token/refresh", params);
        } catch (Exception e) {
            e.printStackTrace();
            return new JSONObject().put("http_status", -1).put("response", new JSONObject());
        }
    }

    /**
     * Get the customer token for the user/pass login
     * @param user string
     * @param passw string
     * @param institution string
     * @return the customer token
     */
    private static JSONObject getCustomerTokenForUser(String user, String passw, String institution) {
        try {
            String encoded_pw = hashPassword(passw);
            Params params = new Params();
            params.add("loginName", user)
                    .add("password", encoded_pw)
                    .add("institutionCode", institution)
                    .add("encryptionType", 2);
            return Requests.post("/trade/auth/user-login", params);
        } catch (Exception e) {
            e.printStackTrace();
            return new JSONObject().put("http_status", -1).put("response", "{}");
        }
    }

    /**
     * Logout the current session
     */
    protected static void logout() {
        active = false;
        Shared.getInstance().setCustomerToken(null);
        try {
            thread.interrupt();
        } catch (Exception e) {
            // ignore
        }
    }

    /**
     * Base 64 encode username and password
     * @param username string
     * @param password string
     * @return encoded string
     */
    private static String base64Encode(String username, String password) {
        String credentials = username + ":" + password;
        return Base64.getEncoder().encodeToString(credentials.getBytes());
    }

    /**
     * Hash the password
     * @param password string
     * @return the hashed password
     */
    private static String hashPassword(String password) {
        String salt = "MUBASHER";
        try {
            // Derive the key using PBKDF2 with HMAC-SHA-512
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), 10000, 512);
            byte[] hash = factory.generateSecret(spec).getEncoded();
            // Convert the resulting byte array into a hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
//            logger.error(" Error obtaining hashPassword,{}",e.toString());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Start the key refresh thread
     */
    private static void startThread() {
        thread = new Thread() {
            @Override
            public void run() {
                active = true;
                keyRefresh();
            }
        };
        thread.start();
    }

    /**
     * Refresh tokens when called by the thread
     */
    private static void keyRefresh() {
        while (active) {
            try {
                String mode = "client";
                if (Shared.getInstance().isServerMode()) {
                    mode = "server";
                }

                JSONObject token = Shared.getInstance().getToken();
                long expMillis = token.getLong("refreshTokenExpiresAt");
                long delta = expMillis - System.currentTimeMillis();

                if (delta < 50_000) {
                    System.out.println("refresh token expired. logging out");
                    logout();
                } else {
                    expMillis = token.getLong("accessTokenExpiresAt");
                    delta = expMillis - System.currentTimeMillis();
                    if (delta > 0 && delta < 100_000) {
                        System.out.printf("--> refreshing %s access token\n", mode);
                        JSONObject refreshedToken;
                        if (Shared.getInstance().isServerMode()) {
                            refreshedToken = getServerTokenRefresh();
                        } else {
                            refreshedToken = getCustomerTokenRefresh();
                        }
                        if (refreshedToken.getInt("http_status") == 200) {
                            if (Shared.getInstance().isServerMode()) {
                                Shared.getInstance().setServerToken(refreshedToken.getJSONObject("response"));
                            } else {
                                Shared.getInstance().setCustomerToken(refreshedToken.getJSONObject("response"));
                            }
                        } else {
                            System.out.printf("--> Error refreshing the %s token: %d\n", mode, (delta / 1000L));
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }
}
