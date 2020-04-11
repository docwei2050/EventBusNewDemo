package com.docwei.eventbusnewdemo.post;

import android.util.Log;

import com.docwei.eventbusnewdemo.EventBus;
import com.docwei.eventbusnewdemo.Subscription;

public class AsyncPoster implements Runnable {
    public EventBus eventBus;
    public PostPendingQueue queue;

    public AsyncPoster(EventBus eventBus) {
        this.eventBus = eventBus;
        this.queue = new PostPendingQueue();
    }

    public void enqueue(Subscription subscription, Object eventType) {
        PostPending postPending = PostPending.obtainPostPending(eventType, subscription);
        Log.e("eventBus", "AsyncPoster  obtainPostPending " +postPending);
        queue.enqueue(postPending);
        eventBus.executorService.execute(this);
    }

    @Override
    public void run() {
        PostPending postPending = queue.poll();
        Log.e("eventBus", "AsyncPoster  " +postPending);
        if (postPending != null) {
            eventBus.invokeSubscriber(postPending);
        }

    }
}
