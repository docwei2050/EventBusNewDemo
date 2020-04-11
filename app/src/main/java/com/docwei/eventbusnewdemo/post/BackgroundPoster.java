package com.docwei.eventbusnewdemo.post;

import com.docwei.eventbusnewdemo.EventBus;
import com.docwei.eventbusnewdemo.Subscription;

//1，线程切换到子线程
//2.保证串行，利用队列
public class BackgroundPoster implements Runnable {
    public EventBus eventBus;
    public PostPendingQueue queue;

    public BackgroundPoster(EventBus eventBus) {
        this.eventBus = eventBus;
        this.queue = new PostPendingQueue();
    }

    private volatile boolean executorRunning;

    public void enqueue(Subscription subscription,Object eventType) {
        PostPending postPending=PostPending.obtainPostPending(eventType,subscription);
        synchronized (this) {
            queue.enqueue(postPending);
            if (!executorRunning) {
                executorRunning = true;
                eventBus.executorService.execute(this);
            }
        }
    }

    @Override
    public void run() {
        try {
            try {
                while (true) {
                    PostPending postPending = null;
                    postPending = queue.poll(1000);
                    if (postPending == null) {
                        synchronized (this) {
                            postPending = queue.poll();
                            if (postPending == null) {
                                executorRunning = false;
                                return;
                            }
                        }
                    }

                    eventBus.invokeSubscriber(postPending);

                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        } finally {
            executorRunning = false;
        }
    }
}

