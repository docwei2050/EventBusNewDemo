package com.docwei.eventbusnewdemo.app;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.docwei.eventbusnewdemo.EventBus;
import com.docwei.eventbusnewdemo.R;
import com.docwei.annotation.Subscriber;
import com.docwei.annotation.ThreadMode;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView tv = findViewById(R.id.tv);
        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("eventBus", "show:    点击事件 ");
                EventBus.getDefualt().post(new NewMessage("i catch you"));

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefualt().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefualt().unregister(this);
    }
    @Subscriber(threadMode = ThreadMode.POSTING)
    public void showPost(NewMessage message) {

        //接收到的消息
        Log.e("eventBus", "showPost:   " + message.value + "-----" + Thread.currentThread().getName());
    }

    @Subscriber(threadMode = ThreadMode.MAIN)
    public void showMain(NewMessage message) {

        //接收到的消息
        Log.e("eventBus", "showMain:   " + message.value + "-----" + Thread.currentThread().getName());
    }

    @Subscriber(threadMode = ThreadMode.ASYN)
    public void showAsyn(NewMessage message) {

        //接收到的消息
        Log.e("eventBus", "showAsyn     " + message.value + "-----" + Thread.currentThread().getName());
    }
    @Subscriber(threadMode = ThreadMode.ASYN)
    public void showAsyn2(NewMessage message) {

        //接收到的消息
        Log.e("eventBus", "showAsyn2     " + message.value + "-----" + Thread.currentThread().getName());
    }

    @Subscriber(threadMode = ThreadMode.BACKGROUND)
    public void showBg1(NewMessage message) {

        //接收到的消息
        Log.e("eventBus", "showBg1     " + message.value + "-----" + Thread.currentThread().getName());
    }

    @Subscriber(threadMode = ThreadMode.BACKGROUND)
    public void showBg2(NewMessage message) {

        //接收到的消息
        Log.e("eventBus", "showBg2    " + message.value + "-----" + Thread.currentThread().getName());
    }
}
