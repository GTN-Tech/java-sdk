package com.gtn;

import org.json.JSONObject;

/**
 * <p>
 * (C) Copyright 2010-2025 Global Trading Network. All Rights Reserved.
 * <p/>
 * Created by uditha on 2025-03-20.
 */
public interface MessageListener {

    void onOpen();

    void onMessage(JSONObject message);

    void onError(JSONObject message);

    void onClose(String closeMessage);
}