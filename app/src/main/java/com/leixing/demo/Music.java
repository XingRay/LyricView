package com.leixing.demo;

import com.leixing.lyricview.Lyric;

/**
 * description : xxx
 *
 * @author : leixing
 * email : leixing1012@qq.com
 * @date : 2019/1/25 17:25
 */
public class Music {
    private String name;
    private Lyric lyric;
    private int duration;

    public String getName() {
        return name;
    }

    public Music setName(String name) {
        this.name = name;
        return this;
    }

    public Lyric getLyric() {
        return lyric;
    }

    public Music setLyric(Lyric lyric) {
        this.lyric = lyric;
        return this;
    }

    public int getDuration() {
        return duration;
    }

    public Music setDuration(int duration) {
        this.duration = duration;
        return this;
    }

    @Override
    public String toString() {
        return "\"Music\": {"
                + "\"name\": \"" + name + '\"'
                + ", \"lyric\": \"" + lyric
                + ", \"duration\": \"" + duration
                + '}';
    }
}
