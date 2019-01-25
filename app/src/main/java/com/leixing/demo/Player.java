package com.leixing.demo;

/**
 * description : xxx
 *
 * @author : leixing
 * email : leixing@baidu.com
 * @date : 2019/1/25 21:41
 */
public class Player {
    private int progress;
    private int duration;

    public Player() {
    }

    public int getProgress() {
        return progress;
    }

    public Player setProgress(int progress) {
        this.progress = progress;
        return this;
    }

    public int getDuration() {
        return duration;
    }

    public Player setDuration(int duration) {
        this.duration = duration;
        return this;
    }

    public void start(){

    }

    public void stop(){

    }

    public interface PlayerListener{
        void onProgress(int progress);
    }

}
