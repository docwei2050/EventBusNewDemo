package com.docwei.eventbusnewdemo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EventBusBuilder {

    private final static ExecutorService DEFAULT_EXECUTOR_SERVICE = Executors.newCachedThreadPool();
     ExecutorService executorService;
     final List<ISubscriberMethodIndex> mISubscriberMethodIndexList;


    public EventBusBuilder() {
        executorService = DEFAULT_EXECUTOR_SERVICE ;
        mISubscriberMethodIndexList = new ArrayList<>();

    }
    public EventBusBuilder setExecutorService(ExecutorService service){
        executorService=service;
        return this;
    }
    //就因为不让用户在除eventBus初始化的地方使用这个方法，而用Builder设计模式
    public EventBusBuilder addIndex(ISubscriberMethodIndex index) {
        mISubscriberMethodIndexList.add(index);
        return this;
    }
    public EventBus build() {
        return new EventBus(this);
    }

}
