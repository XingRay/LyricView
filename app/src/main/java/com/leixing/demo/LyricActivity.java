package com.leixing.demo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.leixing.demo.util.TaskExecutor;
import com.leixing.demo.util.TimeUtil;
import com.leixing.lyricview.LyricColorDesigner;
import com.leixing.lyricview.LyricView;
import com.leixing.lyricview.helper.Lyric;
import com.leixing.lyricview.helper.LyricLine;
import com.leixing.lyricview.helper.LyricUtil;

import java.io.IOException;
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
    private View vLine;
    private TextView tvProgress;
    private AppCompatSeekBar sbSeekBar;
    private TextView tvDuration;
    private ImageView ivPrevious;
    private ImageView ivPlayOrPause;
    private ImageView ivNext;

    private List<Lyric> mLyrics;
    private int mPosition;


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
        initVariables();
        initView();
        loadData();
    }

    private void initVariables() {
        mLyrics = new ArrayList<>();
        mPosition = 0;
    }

    private void initView() {
        setContentView(R.layout.activity_lyric);
        findViews();
        setListeners();
    }

    private void findViews() {
        lvLyric = findViewById(R.id.lv_lyric);
        vLine = findViewById(R.id.v_line);
        sbSeekBar = findViewById(R.id.sb_seek_bar);
        ivPrevious = findViewById(R.id.iv_previous);
        ivPlayOrPause = findViewById(R.id.iv_play_or_pause);
        ivNext = findViewById(R.id.iv_next);
        lvLyric = findViewById(R.id.lv_lyric);
        vLine = findViewById(R.id.v_line);
        tvProgress = findViewById(R.id.tv_progress);
        sbSeekBar = findViewById(R.id.sb_seek_bar);
        tvDuration = findViewById(R.id.tv_duration);
        ivPrevious = findViewById(R.id.iv_previous);
        ivPlayOrPause = findViewById(R.id.iv_play_or_pause);
        ivNext = findViewById(R.id.iv_next);
    }

    private void setListeners() {
        lvLyric.setTouchListener(new LyricView.TouchListener() {
            @Override
            public void onTouchDown(long timeMills) {
                Log.i(TAG, "onTouchDown timeMills:" + timeMills);
                vLine.setVisibility(View.VISIBLE);
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

            }
        });

        lvLyric.setColorDesigner(new LyricColorDesigner(80.0f, 60.0f, 10.0f,
                0xffff00ff, 0xffff0000, 0xff00ff00));

        sbSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    showTimeMills(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                setPlayerProgress(progress);
            }
        });
    }

    private void loadData() {
        TaskExecutor.io(new Runnable() {
            @Override
            public void run() {
                try {
                    String[] filenames = getAssets().list("./");
                    for (String filename : filenames) {
                        mLyrics.add(LyricUtil.parseLyric(getAssets().open(filename)));
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showLyric(mLyrics.get(mPosition));
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
        tvProgress.setText(times[3] + ":" + times[2] + ":" + times[1] + "." + times[0]);
    }

    private void showLyric(final Lyric lyric) {
        lvLyric.setLyric(LyricUtil.toLines(lyric));
    }

    private void setPlayerProgress(int progress) {

    }
}
