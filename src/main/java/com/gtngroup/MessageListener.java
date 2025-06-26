package com.gtngroup;

import org.json.JSONObject;

/**
 * (C) Copyright 2010-2025 Global Trading Network. All Rights Reserved.
 * Created by Uditha Nagahawatta on 2025-03-20.
 */
public interface MessageListener {

    void onOpen();

    void onMessage(JSONObject message);

    void onError(JSONObject message);

    void onClose(String closeMessage);
}