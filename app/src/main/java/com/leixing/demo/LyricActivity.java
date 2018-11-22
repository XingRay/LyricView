package com.leixing.demo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.leixing.demo.util.TaskExecutor;
import com.leixing.demo.util.TimeUtil;
import com.leixing.lyricview.LyricView;
import com.leixing.lyricview.helper.Lyric;
import com.leixing.lyricview.helper.LyricLine;
import com.leixing.lyricview.helper.LyricUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * description : xxx
 *
 * @author : leixing
 * email : leixing@baidu.com
 * @date : 2018/11/15 14:10
 */
public class LyricActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();

    public static final int DELAY_MILLIS = 200;
    private LyricView lvLyric;
    private Runnable updateLyric;
    private TextView tvTime;
    private View vLine;
    private Lyric mLyric;
    private EditText etTime;


    public static void start(Context context) {
        Intent intent = new Intent(context, LyricActivity.class);
        if (!(context instanceof Activity)) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_lyric);

        lvLyric = findViewById(R.id.lv_lyric);
        tvTime = findViewById(R.id.tv_time);
        vLine = findViewById(R.id.v_line);
        etTime = findViewById(R.id.et_time);

        lvLyric.setTouchListener(new LyricView.TouchListener() {
            @Override
            public void onTouchDown(long timeMills) {
                Log.i(TAG, "onTouchDown timeMills:" + timeMills);
                vLine.setVisibility(View.VISIBLE);
                tvTime.setVisibility(View.VISIBLE);
                showTimeMills(timeMills);
            }

            @Override
            public void onTouchMoving(long timeMills) {
                Log.i(TAG, "onTouchMoving timeMills:" + timeMills);
                showTimeMills(timeMills);
            }

            @Override
            public void onTouchUp(long timeMills) {
                Log.i(TAG, "onTouchUp timeMills:" + timeMills);
//                vLine.setVisibility(View.GONE);
//                tvTime.setVisibility(View.GONE);
            }
        });

        findViewById(R.id.bt_set).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long timeMills = Long.parseLong(etTime.getText().toString());
                lvLyric.updateTime(timeMills, false);
            }
        });

        TaskExecutor.io(new Runnable() {
            @Override
            public void run() {
                try {
                    InputStream inputStream = getAssets().open("一个人的北京.lyric");
                    mLyric = LyricUtil.parseLyric(inputStream);
//                    mLyric = makeLyric();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showLyric(mLyric);
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private Lyric makeLyric() {
        Lyric lyric = new Lyric();
        List<LyricLine> lines = new ArrayList<>();

        for (int i = 0; i < 1000; i++) {
            LyricLine line = new LyricLine();
            line.setContent("测试测试测试测试测试测试测试" + i);
            line.setStartTime(i * 1000);
            lines.add(line);
        }

        lyric.setLyricLines(lines);
        return lyric;
    }

    private void showTimeMills(long mills) {
        long[] times = TimeUtil.parseMills(mills);
        tvTime.setText(times[3] + ":" + times[2] + ":" + times[1] + "." + times[0]);
    }

    private void showLyric(final Lyric lyric) {
        lvLyric.setLyric(LyricUtil.toLines(lyric));
        final long startTimeMills = System.currentTimeMillis();
        updateLyric = new Runnable() {
            @Override
            public void run() {
                long currentTimeMillis = System.currentTimeMillis() - startTimeMills;
                lvLyric.updateTime(currentTimeMillis + 30000, true);
                List<LyricLine> lyricLines = lyric.getLyricLines();
                if (currentTimeMillis < lyricLines.get(lyricLines.size() - 1).getStartTime()) {
                    lvLyric.postDelayed(updateLyric, DELAY_MILLIS);
                }
            }
        };
        lvLyric.postDelayed(updateLyric, DELAY_MILLIS);
    }
}
