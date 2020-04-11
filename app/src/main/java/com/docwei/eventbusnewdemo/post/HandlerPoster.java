package com.docwei.eventbusnewdemo.post;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import com.docwei.eventbusnewdemo.EventBus;
import com.docwei.eventbusnewdemo.Subscription;

//源码这里使用sync，但是他的handleActive
public class HandlerPoster implements Handler.Callback {
    public EventBus eventBus;
    public Handler handler;

    public HandlerPoster(EventBus eventBus) {
        this.eventBus = eventBus;
        //主线程的handler
       handler=new Handler(Looper.getMainLooper(),this);

    }
    public void enqueue(Subscription subscription, Object eventType) {
        PostPending postPending=PostPending.obtainPostPending(eventType,subscription);
        Message message=Message.obtain();
        message.obj=postPending;
        handler.sendMessage(message);
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        if( msg.obj instanceof  PostPending){
            PostPending postPending= (PostPending) msg.obj;
            eventBus.invokeSubscriber(postPending);
        }
        return true;
    }
}
