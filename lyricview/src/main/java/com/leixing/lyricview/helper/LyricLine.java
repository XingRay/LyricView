package com.leixing.lyricview.helper;

/**
 * description : 歌词
 *
 * @author : leixing
 * email : leixing@baidu.com
 * @date : 2018/11/10 17:31
 */
public class LyricLine {
    private String content;
    private long startTime;

    public String getContent() {
        return content;
    }

    public LyricLine setContent(String content) {
        this.content = content;
        return this;
    }

    public long getStartTime() {
        return startTime;
    }

    public LyricLine setStartTime(long startTime) {
        this.startTime = startTime;
        return this;
    }

    @Override
    public String toString() {
        return "\"LyricLine\": {"
                + "\"content\": \"" + content + '\"'
                + ", \"startTime\": \"" + startTime
                + '}';
    }
}