package com.docwei.eventbusnewdemo;

//因为有多个module,都可能有这个SubscriberMethodIndex，需要提供一个接口
public interface ISubscriberMethodIndex {

    public SubscribeMethodApt[] getSubScriberMethod(Class<?> clazz);
}
