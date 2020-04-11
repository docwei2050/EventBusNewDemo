package com.docwei.processor;

import com.docwei.annotation.ThreadMode;

public class SubscriberMethodInfo {

    public String eventType;
    public String threadmode;
    public String method;

    public SubscriberMethodInfo(String eventType, String threadmode, String method) {
        this.eventType = eventType;
        this.threadmode = threadmode;
        this.method = method;
    }
}
