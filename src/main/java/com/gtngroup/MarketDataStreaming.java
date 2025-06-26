package com.gtngroup;

import com.gtngroup.util.Params;
import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.CloseReason;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;

/**
 * (C) Copyright 2010-2021 Global Market Technologies. All Rights Reserved.
 * Created by Uditha Nagahawatta on 2025-02-20.
 */
@ClientEndpoint
public class MarketDataStreaming implements StreamingService {

    private Session session;
    private WebSocketContainer container;
    private static MarketDataStreaming self;
    private MessageListener webSocketListener;
    private boolean waiting;

    public MarketDataStreaming() {

    }

    protected static synchronized MarketDataStreaming getInstance() {
        if (self == null) {
            self = new MarketDataStreaming();
        }

        return self;
    }

    /**
     * Get called when websocket connection open
     *
     * @param session of websocket
     */
    @OnOpen
    public void onOpen(Session session) {
        self.session = session;
        try {
            session.getBasicRemote().sendText(String.format("{\"token\":\"%s\"}", Shared.getInstance().getToken().get("accessToken")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        self.webSocketListener.onOpen();
        self.waiting = false;
    }

    /**
     * Get called when websocket receives a message
     *
     * @param message from the socket
     */
    @OnMessage
    public void onMessage(String message) {
        JSONObject sseMessage = new JSONObject(message);
        self.webSocketListener.onMessage(sseMessage);
    }

    /**
     * Get called when websocket receives a message in binary format
     *
     * @param message from the socket
     */
    @OnMessage
    public void onBinaryMessage(byte[] message) {
    }

    /**
     * Get called when websocket connection closes
     *
     * @param session     of websocket
     * @param closeReason of the close event
     */
    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        self.waiting = false;
        self.webSocketListener.onClose(closeReason.getReasonPhrase());
    }

    /**
     * Get called when websocket connection gets an error
     *
     * @param session   of websocket
     * @param throwable of the error
     */
    @OnError
    public void onError(Session session, Throwable throwable) {
        self.waiting = false;
        self.webSocketListener.onError(new JSONObject().put("error", throwable.toString()));
    }


    /**
     * Send a message to the web socket
     *
     * @param message to send as per the API documentation
     */
    public void sendMessage(Params message) {
        try {
            self.session.getBasicRemote().sendText(message.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * register the event listener class.
     *
     * @param webSocketListener class
     */
    protected void addListener(MessageListener webSocketListener) {
        self = new MarketDataStreaming();
        self.webSocketListener = webSocketListener;
    }

    public void connect(String endpoint) {
        connect(endpoint, null);
    }

    /**
     * Connect the websocket
     *
     */
    public void connect(String endpoint, String events) {
        if (self == null || self.webSocketListener == null) {
            throw new RuntimeException("Websocket client class not registered. call register() method before connect()");
        } else if (self.session != null) {
            throw new RuntimeException("Websocket Already initialised");
        }

        if (endpoint.charAt(0) != '/') {
            endpoint = "/" + endpoint;
        }

        self.waiting = true;

        self.container = ContainerProvider.getWebSocketContainer();
        String uri = "wss" + Shared.getInstance().getAPIUrl().substring(5) + endpoint + "?throttle-key=" + Shared.getInstance().getAppKey();
        System.out.println(uri);
        try {
            self.container.connectToServer(MarketDataStreaming.class, URI.create(uri));
            int count = 300;
            while(self.waiting && count > 0){
                Thread.sleep(100);
                count--;
            }
        } catch (Exception e) {
            e.printStackTrace();
            self.waiting = false;
            self.webSocketListener.onClose("Error:" + e);
        }
    }

    /**
     * Disconnect the active websocket session
     */
    public void disconnect() {
        try {
            self.session.close();
        } catch (Exception e) {
            //ignore
        }
        if (self != null) {
            self.container = null;
            self.session = null;
            self.webSocketListener = null;
        }
    }
}

