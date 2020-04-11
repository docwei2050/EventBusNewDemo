package com.docwei.eventbusnewdemo;
import java.util.HashMap;
import java.util.Map;
public class SubscribeMethodIndex implements ISubscriberMethodIndex{
    @Override
    public SubscribeMethodApt[] getSubScriberMethod(Class<?> clazz) {
        return new SubscribeMethodApt[0];
    }

    /*private final static Map<Class<?>, SubscribeMethodApt[]> SUBSCRIBER_INDEX;
    static {
        SUBSCRIBER_INDEX=new HashMap<>();
        //要搜集整个模块里面含有的注解方法
        SUBSCRIBER_INDEX.put(xxx.class,new SubscribeMethodApt[]{
             new SubscribeMethodApt("xxx",xxx.class,com.docwei.annotation.ThreadMode.ASYN),
        });
    }

    @Override
    public SubscribeMethodApt[] getSubScriberMethod(Class<?> clazz){
        return SUBSCRIBER_INDEX.get(clazz);
    }*/
}
