package com.gtn;

import com.gtn.util.Params;

/**
 * <p>
 * (C) Copyright 2010-2021 Global Market Technologies. All Rights Reserved.
 * <p/>
 * Created by uditha on 2025-04-07.
 */
public interface StreamingService {

    void connect(String endpoint, String events);

    void connect(String endpoint);

    void disconnect();

    void sendMessage(Params params);
}
