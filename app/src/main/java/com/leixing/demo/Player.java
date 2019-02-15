package com.leixing.demo;

import com.leixing.lib.handlercounter.CountListener;
import com.leixing.lib.handlercounter.CounterStatus;
import com.leixing.lib.handlercounter.CounterStatusListener;
import com.leixing.lib.handlercounter.HandlerCounter;

/**
 * description : xxx
 *
 * @author : leixing
 * email : leixing@baidu.com
 * @date : 2019/1/25 21:41
 */
public class Player {
    /**
     * mills
     */
    private long mDuration;

    private PlayerListener mPlayerListener;
    private final HandlerCounter mCounter;
    private boolean mIsPlaying;

    public Player() {
        mCounter = new HandlerCounter()
                .countInterval(200)
                .stepSize(200)
                .countListener(new CountListener() {
                    @Override
                    public void onCount(long l) {
                        if (mPlayerListener != null) {
                            mPlayerListener.onProgress(l, mDuration);
                        }
                    }
                })
                .counterStatusListener(new CounterStatusListener() {
                    @Override
                    public void onNewStatus(CounterStatus counterStatus) {
                        boolean isPlaying = mIsPlaying;
                        mIsPlaying = counterStatus == CounterStatus.RUNNING;
                        if (mIsPlaying != isPlaying && mPlayerListener != null) {
                            mPlayerListener.onPlayingChanged(mIsPlaying);
                        }
                    }
                });

        mIsPlaying = false;
    }

    public void play(long duration) {
        play(0, duration);
    }

    public void play(long progress, long duration) {
        if (progress < 0) {
            throw new IllegalArgumentException();
        }
        if (duration < progress) {
            throw new IllegalArgumentException();
        }

        mDuration = duration;
        mCounter.stop();
        mCounter.startValue(progress)
                .endValue(duration)
                .start();
    }

    public void setPlayerListener(PlayerListener listener) {
        mPlayerListener = listener;
    }

    public void setProgress(int progress) {
        mCounter.pause();
        mCounter.currentValue(progress);
        mCounter.restart();
    }

    public void playOrPause() {
        if (mIsPlaying) {
            mCounter.pause();
        } else {
            mCounter.restart();
        }
    }

    public interface PlayerListener {
        void onProgress(long progress, long duration);

        void onPlayingChanged(boolean isPlaying);
    }
}
