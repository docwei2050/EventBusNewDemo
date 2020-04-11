package com.docwei.eventbusnewdemo.app;

import android.app.Application;

import com.docwei.eventbusnewdemo.EventBus;
import com.docwei.eventbusnewdemo.MainEventBusIndex;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.builder().addIndex(new MainEventBusIndex());
    }
}
