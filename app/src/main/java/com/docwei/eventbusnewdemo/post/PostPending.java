package com.docwei.eventbusnewdemo.post;

import com.docwei.eventbusnewdemo.Subscription;

import java.util.ArrayList;
import java.util.List;

public class PostPending {
    public static final List<PostPending> sPostPendings = new ArrayList<>();
    public Object eventType;
    public Subscription subscription;
    public PostPending next;

    public PostPending(Object eventType, Subscription subscription) {
        this.eventType = eventType;
        this.subscription = subscription;
    }

    public static PostPending obtainPostPending(Object eventType, Subscription subscription) {
        synchronized (sPostPendings) {
            if (sPostPendings.size() > 0) {
                //用了就要及时清空一下
                PostPending postPending=sPostPendings.remove(sPostPendings.size()-1);
                if(postPending!=null){
                    postPending.subscription=subscription;
                    postPending.eventType=eventType;
                    postPending.next=null;
                    return postPending;
                }

            }
        }
        return new PostPending(eventType,subscription);
    }

    public static void release(PostPending postPending) {
        postPending.eventType = null;
        postPending.subscription = null;
        postPending.next = null;
        synchronized (sPostPendings) {
           if(sPostPendings.size()<1000){
               sPostPendings.add(postPending);
           }
        }

    }
}
