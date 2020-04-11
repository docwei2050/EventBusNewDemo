package com.docwei.eventbusnewdemo;


import com.docwei.annotation.ThreadMode;

//因为api的类无法获取到Method对象
// 只能获取到method的方法名
//最后要从methodName转换成Method对象
public class SubscribeMethodApt {
    public String methodName;
    public Class<?> eventType;
    public ThreadMode mode;

    public SubscribeMethodApt(String methodName, Class<?> eventType, ThreadMode mode) {
        this.methodName = methodName;
        this.eventType = eventType;
        this.mode = mode;
    }
}
