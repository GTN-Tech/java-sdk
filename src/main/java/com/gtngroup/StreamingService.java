package com.gtngroup;

import com.gtngroup.util.Params;

/**
 * (C) Copyright 2010-2021 Global Market Technologies. All Rights Reserved.
 * Created by Uditha Nagahawatta on 2025-04-07.
 */
public interface StreamingService {

    void connect(String endpoint, String events);

    void connect(String endpoint);

    void disconnect();

    void sendMessage(Params params);
}
