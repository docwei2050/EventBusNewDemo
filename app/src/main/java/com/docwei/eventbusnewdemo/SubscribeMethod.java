package com.docwei.eventbusnewdemo;

import com.docwei.annotation.ThreadMode;

import java.lang.reflect.Method;
import java.util.Objects;

public class SubscribeMethod {
    public Method method;
    public Class<?> eventType;
    public ThreadMode mode;

    public SubscribeMethod(Method method, Class<?> eventType, ThreadMode mode) {
        this.method = method;
        this.eventType = eventType;
        this.mode = mode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SubscribeMethod)) {
            return false;
        }
        SubscribeMethod that = (SubscribeMethod) o;
        return Objects.equals(method, that.method) &&
                Objects.equals(eventType, that.eventType) &&
                mode == that.mode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, eventType, mode);
    }
}
