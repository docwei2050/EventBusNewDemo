package com.docwei.eventbusnewdemo.app;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.docwei.annotation.Subscriber;
import com.docwei.annotation.ThreadMode;
import com.docwei.eventbusnewdemo.EventBus;
import com.docwei.eventbusnewdemo.R;

public class SecondAct extends AppCompatActivity {

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
    @Subscriber(threadMode = ThreadMode.MAIN)
    public void showMain(NewMessage2 message) {
        //接收到的消息
        Log.e("eventBus", "showMain:   " + message.value + "-----" + Thread.currentThread().getName());
    }
}
