package com.docwei.eventbusnewdemo;


import android.os.Looper;
import android.util.Log;

import com.docwei.annotation.ThreadMode;
import com.docwei.eventbusnewdemo.post.AsyncPoster;
import com.docwei.eventbusnewdemo.post.BackgroundPoster;
import com.docwei.eventbusnewdemo.post.HandlerPoster;
import com.docwei.eventbusnewdemo.post.PostPending;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EventBus {
    //key是eventType，value是对应的所有的subscription,
    // subscription必须包含Subscriber和SubscriberMethod,才能反射调用
    public final Map<Class<?>, CopyOnWriteArrayList<Subscription>> subscriptionsByEventType;
    //方便解除注册删除订阅者
    private final Map<Object, Set<Class<?>>> typesBySubscriber;
    private SubscriberMethodFinder mSubscriberMethodFinder;
    public  final ExecutorService executorService;
    public final AsyncPoster mAsyncPoster;
    private final BackgroundPoster mBackgroundPoster;
    private final HandlerPoster mHandlerPoster;
    List<ISubscriberMethodIndex> mISubscriberMethodIndexList;
    private static final EventBusBuilder DEFAULT_BUILDER = new EventBusBuilder();
    public EventBus(){
       this(DEFAULT_BUILDER);
    }
    public EventBus(EventBusBuilder busBuilder) {
        subscriptionsByEventType = new HashMap<>();
        typesBySubscriber = new HashMap<>();
        mSubscriberMethodFinder = new SubscriberMethodFinder(false);
        executorService= busBuilder.executorService;
        mISubscriberMethodIndexList=busBuilder.mISubscriberMethodIndexList;
        mAsyncPoster=new AsyncPoster(this);
        mBackgroundPoster = new BackgroundPoster(this);
        mHandlerPoster = new HandlerPoster(this);
    }
    public static volatile EventBus sInstance;

    public static EventBus getDefualt() {
        if (sInstance == null) {
            synchronized (EventBus.class) {
                if (sInstance == null) {
                    sInstance = new EventBus();
                }
            }
        }
        return sInstance;
    }

    public void post(Object event) {
        //发送事件的方法
        //订阅者要想拿到这些方法，必须获取这些订阅者的对象，这些订阅者是在注册的时候传进来的。Object类型，
        // 然后反射调用他们的方法，Object类型的，你没法直接调用，你没法引用到，你只能获取到他们方法
        //然后反射调用。这个event就是参数
        for (Map.Entry<Class<?>, CopyOnWriteArrayList<Subscription>> entry : subscriptionsByEventType.entrySet()) {
            if (entry.getKey().equals(event.getClass())) {
                CopyOnWriteArrayList<Subscription> subscriptions = entry.getValue();
                for (Subscription subscription : subscriptions) {
                    postSingleEvent(event,subscription);
                }
            }
        }

    }


    //需要处理线程切换的问题
    public void postSingleEvent(Object event,Subscription subscription){
         ThreadMode threadmode=subscription.subscribeMethod.mode;
        PostPending postPending=PostPending.obtainPostPending(event,subscription);
         switch (threadmode){
             case POSTING:
                 //同一个线程呐 默认就可以实现
                 invokeSubscriber(postPending);
                 break;
             case MAIN:
                 if( Looper.myLooper()==Looper.getMainLooper()){
                     //主线程
                     invokeSubscriber(postPending);
                 }else{
                     mHandlerPoster.enqueue(subscription,event);
                 }
                 break;
             case BACKGROUND:
                 if(Looper.myLooper()==Looper.getMainLooper()){
                     //主线程
                     mBackgroundPoster.enqueue(subscription,event);
                 }else{
                     invokeSubscriber(postPending);
                 }
                 break;
             case ASYN:
                mAsyncPoster.enqueue(subscription,event);
                 break;
             default:
                 break;
         }
    }


    //注册一下
    public void register(Object subscriber) {
        Class clazz = subscriber.getClass();
        //搜集起来 同一个订阅者，可能有多个订阅方法，每一个订阅方法，对应一种EventType
        List<SubscribeMethod> subscribeMethods = mSubscriberMethodFinder.findSubscriberMethods(clazz,mISubscriberMethodIndexList);
        Set<Class<?>> eventTypes = typesBySubscriber.get(subscriber);
        if (eventTypes == null) {
            eventTypes = new HashSet<>();
        }
        for (SubscribeMethod subscribeMethod : subscribeMethods) {
            CopyOnWriteArrayList<Subscription> list = subscriptionsByEventType.get(subscribeMethod.eventType);
            if (list == null) {
                list = new CopyOnWriteArrayList<>();
                subscriptionsByEventType.put(subscribeMethod.eventType, list);
            }
            list.add(new Subscription(subscriber, subscribeMethod));

            eventTypes.add(subscribeMethod.eventType);
            typesBySubscriber.put(subscribeMethod, eventTypes);

        }

    }

    //反注册
    public void unregister(Object subscriber) {
        Set<Class<?>> eventTypes = typesBySubscriber.get(subscriber);
        for (Class<?> clazz : eventTypes) {
            CopyOnWriteArrayList<Subscription> subscriptions = subscriptionsByEventType.get(clazz);
            if (subscriptions != null) {
                int size = subscriptions.size();
                for (int i = 0; i < size; i++) {
                    //不走迭代器的遍历删除操作  66666
                    Subscription subscription = subscriptions.get(i);
                    if (subscription.subscriber == subscriber) {
                        subscriptions.remove(i);
                        i--;
                        size--;
                    }
                }
            }
        }
    }


    //真正的调用，使用反射
    public void invokeSubscriber(PostPending postPending){
        Object eventType=postPending.eventType;
        Object subscriber=postPending.subscription.subscriber;
        Method  method=postPending.subscription.subscribeMethod.method;
        try {
            method.invoke(subscriber,eventType);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }
    public static EventBusBuilder builder() {
        return DEFAULT_BUILDER;
    }
}
