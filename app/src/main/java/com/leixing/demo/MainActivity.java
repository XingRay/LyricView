package com.leixing.demo;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    private Activity mActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = this;

        setContentView(R.layout.activity_main);

        findViewById(R.id.bt_lyric).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LyricActivity.start(mActivity);
            }
        });
    }
}
