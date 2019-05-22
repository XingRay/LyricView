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
import com.leixing.demo.util.ViewUtil;
import com.leixing.lyricview.Line;
import com.leixing.lyricview.Lyric;
import com.leixing.lyricview.LyricParser;
import com.leixing.lyricview.LyricView;
import com.leixing.lyricview.TouchListener;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * description : xxx
 *
 * @author : leixing
 * email : leixing1012@qq.com
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
    private Player mPlayer;
    private Context mContext;

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
        mContext = getApplicationContext();
        mLyrics = new ArrayList<>();
        mPosition = 0;

        mPlayer = new Player();
        mPlayer.setPlayerListener(new Player.PlayerListener() {
            @Override
            public void onProgress(long progress, long duration) {
                Log.i(TAG, "progress: " + progress);
                lvLyric.setTime(progress);
                sbSeekBar.setProgress((int) progress);
                showProgress(progress);
            }

            @Override
            public void onPlayingChanged(boolean isPlaying) {
                ivPlayOrPause.setImageResource(isPlaying
                        ? R.mipmap.icon_pause :
                        R.mipmap.icon_play);
            }
        });
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
        lvLyric.setTouchListener(new TouchListener() {
            @Override
            public void onTouchDown(long timeMills) {
                Log.i(TAG, "onTouchDown timeMills:" + timeMills);
                vLine.setVisibility(View.VISIBLE);
                showProgress(timeMills);
            }

            @Override
            public void onTouchMoving(long timeMills) {
                Log.i(TAG, "onTouchMoving timeMills:" + timeMills);
                showProgress(timeMills);
            }

            @Override
            public void onTouchUp(long timeMills) {
                Log.i(TAG, "onTouchUp timeMills:" + timeMills);
            }
        });

        lvLyric.setLineDesigner(new LyricLineDesigner(
                0xffffffff,
                0x33ffffff,
                ViewUtil.sp2px(mContext, 16.0f),
                ViewUtil.sp2px(mContext, 12.0f)));

        sbSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    showProgress(progress);
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

        ivNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playNext();
            }
        });

        ivPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrevious();
            }
        });

        ivPlayOrPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPlayer.playOrPause();
            }
        });
    }

    private void playNext() {
        playPosition((mPosition + 1) % mLyrics.size());
    }

    private void playPrevious() {
        int size = mLyrics.size();
        playPosition((mPosition + size - 1) % size);
    }

    private void loadData() {
        TaskExecutor.io(new Runnable() {
            @Override
            public void run() {
                try {
                    String[] filenames = getAssets().list("lyric");
                    if (filenames == null) {
                        return;
                    }
                    for (String filename : filenames) {
                        Lyric lyric = LyricParser.parseLyric(getAssets().open("lyric" + File.separator + filename));
                        mLyrics.add(lyric);
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            playPosition(0);
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void playPosition(int position) {
        mPosition = position;
        Lyric lyric = mLyrics.get(mPosition);
        List<Line> lines = LyricParser.toLines(lyric);
        lvLyric.setLyric(lines);
        long duration = lines.get(lines.size() - 1).getEndMills();
        mPlayer.play(duration);
        showDuration(duration);
        sbSeekBar.setMax((int) duration);
    }

    private void showProgress(long mills) {
        tvProgress.setText(getFormattedTime(mills));
    }

    private void showDuration(long mills) {
        tvDuration.setText(getFormattedTime(mills));
    }

    private void setPlayerProgress(int progress) {
        mPlayer.setProgress(progress);
    }

    private String getFormattedTime(long mills) {
        long[] times = TimeUtil.parseMills(mills);
        StringBuilder builder = new StringBuilder();
        for (int i = 3; i >= 1; i--) {
            long time = times[i];
            if (time < 10) {
                builder.append("0");
            }
            builder.append(time);
            if (i > 1) {
                builder.append(":");
            }
        }
        return builder.toString();
    }
}
