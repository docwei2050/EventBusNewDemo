package com.docwei.eventbusnewdemo;

import com.docwei.annotation.Subscriber;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SubscriberMethodFinder {
    public boolean ignoreGeneratedIndex;
    private static final int POOL_SIZE = 4;
    private static final FindState[] FIND_STATE_POOL = new FindState[POOL_SIZE];
    private static final int BRIDGE = 0x40;
    private static final int SYNTHETIC = 0x1000;
    private static final int MODIFIERS_IGNORE = Modifier.ABSTRACT | Modifier.STATIC | BRIDGE | SYNTHETIC;

    public SubscriberMethodFinder(boolean ignoreGeneratedIndex) {
        this.ignoreGeneratedIndex = ignoreGeneratedIndex;
    }

    private static final Map<Class<?>, List<SubscribeMethod>> METHOD_CACHE = new ConcurrentHashMap<>();

    //通过订阅者去找订阅方法
    public List<SubscribeMethod> findSubscriberMethods(Class<?> subscriberClass, List<ISubscriberMethodIndex> subscriberMethodIndexList) {
        List<SubscribeMethod> subscriberMethods = METHOD_CACHE.get(subscriberClass);
        if (subscriberMethods != null) {
            return subscriberMethods;
        }
        if (ignoreGeneratedIndex || subscriberMethodIndexList == null || subscriberMethodIndexList.size() == 0) {
            //反射的方式去找吧
            subscriberMethods = findUsingReflection(subscriberClass);
        } else {
            //走apt的方式去拿订阅方法
            subscriberMethods = findUsingApt(subscriberClass, subscriberMethodIndexList);
        }
        if (subscriberMethods.isEmpty()) {
            throw new RuntimeException("cannot find subscribers");
        }
        METHOD_CACHE.put(subscriberClass, subscriberMethods);
        return subscriberMethods;

    }

    private List<SubscribeMethod> findUsingApt(Class<?> subscriberClass, List<ISubscriberMethodIndex> subscriberMethodIndexList) {
        //注意EventBus使用Apt只是用于注册时归类用的，不参与最后的调用
        //注解生成的类的目的是
        //从复用池对象里面去取
        FindState findState = prepareFindState();
        //更新订阅者class
        findState.initForSubscriber(subscriberClass);
        for (ISubscriberMethodIndex subscriberMethodIndex : subscriberMethodIndexList) {
            SubscribeMethodApt[] apts = subscriberMethodIndex.getSubScriberMethod(subscriberClass);
            if (apts != null) {
                for (SubscribeMethodApt methodApt : apts) {
                    SubscribeMethod subscribeMethod = createSubscribeMethod(methodApt, subscriberClass);
                    if (subscribeMethod != null) {
                        findState.subscriberMethods.add(subscribeMethod);
                    }
                }
            }
        }

        return getMethodsAndRelease(findState);
    }

    private SubscribeMethod createSubscribeMethod(SubscribeMethodApt methodApt, Class<?> subscriberClass) {
        try {
            Method method = subscriberClass.getDeclaredMethod(methodApt.methodName, methodApt.eventType);
            return new SubscribeMethod(method, methodApt.eventType, methodApt.mode);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }


    private List<SubscribeMethod> findUsingReflection(Class<?> subscriberClass) {
        //从复用池对象里面去取
        FindState findState = prepareFindState();
        //更新订阅者class
        findState.initForSubscriber(subscriberClass);
        findUsingReflectionInSingleClass(findState);
        return getMethodsAndRelease(findState);
    }

    private void findUsingReflectionInSingleClass(FindState findState) {
        Method[] methods = findState.subscriberClass.getDeclaredMethods();
        for (Method method : methods) {
            int modifier = method.getModifiers();
            //只能是Public修饰符 且不能是抽象Flag修饰
            if (((modifier & Modifier.PUBLIC) != 0) && ((modifier & MODIFIERS_IGNORE) == 0)) {

                Subscriber annotation = method.getAnnotation(Subscriber.class);

                if (annotation != null) {
                    //获取到参数类型
                    Class[] clazz = method.getParameterTypes();
                    if (clazz.length != 1) {
                        throw new IllegalStateException("the size of params must be  one");
                    }
                    //参数就是eventType 一个方法对应一个事件
                    SubscribeMethod subscribeMethod = new SubscribeMethod(method, clazz[0], annotation.threadMode());
                    findState.subscriberMethods.add(subscribeMethod);
                }
            }
        }
    }


    private List<SubscribeMethod> getMethodsAndRelease(FindState findState) {
        //取出来也是非常好
        List<SubscribeMethod> subscriberMethods = new ArrayList<>(findState.subscriberMethods);
        findState.recycle();
        synchronized (FIND_STATE_POOL) {
            for (int i = 0; i < POOL_SIZE; i++) {
                if (FIND_STATE_POOL[i] == null) {
                    FIND_STATE_POOL[i] = findState;
                    break;
                }
            }
        }
        return subscriberMethods;
    }


    //复用对象池中的对象
    private FindState prepareFindState() {
        synchronized (FIND_STATE_POOL) {
            for (int i = 0; i < POOL_SIZE; i++) {
                FindState state = FIND_STATE_POOL[i];
                if (state != null) {
                    FIND_STATE_POOL[i] = null;
                    return state;
                }
            }
        }
        return new FindState();
    }


    static class FindState {
        //提供的是订阅者 以及订阅者具备的订阅方法
        final List<SubscribeMethod> subscriberMethods = new ArrayList<>();
        Class<?> subscriberClass;


        void initForSubscriber(Class<?> subscriberClass) {
            this.subscriberClass = subscriberClass;
        }

        void recycle() {
            subscriberMethods.clear();
            subscriberClass = null;
        }

    }
}
