package com.gtngroup;

import com.gtngroup.util.Params;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.stream.Stream;

/**
 * (C) Copyright 2010-2025 Global Trading Network. All Rights Reserved.
 * Created by Uditha Nagahawatta on 2025-02-20.
 */
public class TradeStreaming implements StreamingService{

    private static TradeStreaming self;
    private MessageListener webSocketListener;
    private Stream<String> linesInResponse;


    private TradeStreaming() {
    }


    protected static synchronized TradeStreaming getInstance() {
        if (self == null) {
            self = new TradeStreaming();
        }

        return self;
    }

    private static void onMessage(String message) {
        if (message.startsWith("data:")) {
            JSONObject messageObj = new JSONObject(message.substring(5));
            if (messageObj.getString("event").equals("ERROR")) {
                self.webSocketListener.onError(messageObj);
            } else {
                self.webSocketListener.onMessage(messageObj);
            }
        }
    }


    /**
     * Send a message to the web socket
     *
     * @param message to send as per the API documentation
     */
    public void sendMessage(Params message) {
        try {
            // todo - pending dev on SSE server
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addListener(MessageListener webSocketListener) {
        self = new TradeStreaming();
        self.webSocketListener = webSocketListener;
    }

    public void connect(String endpoint) {
        connect(endpoint, null);
    }

    public void connect(String endpoint, String events) {

        try {
            if (endpoint.charAt(0) != '/'){
                endpoint =  "/" + endpoint;
            }

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .header("Authorization", "Bearer " + Shared.getInstance().getToken().get("accessToken").toString())
                    .header("Throttle-Key", Shared.getInstance().getAppKey())
                    .uri(URI.create(Shared.getInstance().getAPIUrl() + endpoint + "?events=" + events))
                    .GET()
                    .timeout(Duration.ofSeconds(120))
                    .build();

            self.linesInResponse = client.send(request, HttpResponse.BodyHandlers.ofLines()).body();
            new Thread("GTN Trade SSE Reader") {
                @Override
                public void run() {
                    self.webSocketListener.onOpen();
                    try {
                        self.linesInResponse.forEach(TradeStreaming::onMessage);
                        self.linesInResponse.close();
                        self.webSocketListener.onClose("Session closed");
                    } catch (Exception e) {
                        //ignore
                    }
                }
            }.start();

        } catch (Exception e) {
            try {
                if (self.linesInResponse != null) self.linesInResponse.close();
            } catch (Exception ex) {
                //ignore
            } finally {
                self.webSocketListener.onClose("Error:" + e.toString());
            }
        }
    }

    /**
     * Disconnect the active websocket session
     */
    public  void disconnect() {
        try {
            if (self.linesInResponse != null) self.linesInResponse.close();
        } catch (Exception e) {
            // ignore
        }
        if (self != null) {
            self.webSocketListener = null;
        }
    }
}

