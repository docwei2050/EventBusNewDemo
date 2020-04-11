package com.docwei.eventbusnewdemo;

public class Subscription {
    public Object subscriber;
    public SubscribeMethod subscribeMethod;

    public Subscription(Object subscriber, SubscribeMethod method) {
        this.subscriber = subscriber;
        this.subscribeMethod = method;
    }
}
