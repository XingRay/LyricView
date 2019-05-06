package com.leixing.lyricview;

import android.text.TextUtils;

import java.util.List;

/**
 * description : 歌词
 *
 * @author : leixing
 * email : leixing1012@qq.com
 * @date : 2018/11/10 17:31
 */
public class Lyric {

    /**
     * 标题
     */
    private String title;

    /**
     * 作者
     */
    private String artist;

    /**
     * 专辑名称
     */
    private String album;

    /**
     * 制作
     */
    private String by;

    /**
     * 偏移量
     */
    private long offset;

    /**
     * 歌词
     */
    private List<Line> lines;

    public String getTitle() {
        return title;
    }

    public Lyric setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getArtist() {
        return artist;
    }

    public Lyric setArtist(String artist) {
        this.artist = artist;
        return this;
    }

    public String getAlbum() {
        return album;
    }

    public Lyric setAlbum(String album) {
        this.album = album;
        return this;
    }

    public String getBy() {
        return by;
    }

    public Lyric setBy(String by) {
        this.by = by;
        return this;
    }

    public long getOffset() {
        return offset;
    }

    public Lyric setOffset(long offset) {
        this.offset = offset;
        return this;
    }

    public List<Line> getLines() {
        return lines;
    }

    public Lyric setLines(List<Line> lines) {
        this.lines = lines;
        return this;
    }

    @Override
    public String toString() {
        String str = "\"Lyric\": {"
                + "\"title\": \"" + title + '\"' + "\n"
                + ", \"artist\": \"" + artist + '\"' + "\n"
                + ", \"album\": \"" + album + '\"' + "\n"
                + ", \"by\": \"" + by + '\"' + "\n"
                + ", \"offset\": \"" + offset + "\n"
                + ", \"lines\": \"" + "\n";
        StringBuilder sb = new StringBuilder(str);
        for (Line line : lines) {
            sb.append(line).append("\n");
        }
        sb.append('}');
        return sb.toString();
    }

    public boolean isEmpty(){
        if (lines == null || lines.isEmpty()) {
            return true;
        }
        for (Line line : lines) {
            if (!TextUtils.isEmpty(line.getContent())) {
                return false;
            }
        }

        return true;
    }

    /**
     * description : 歌词
     *
     * @author : leixing
     * email : leixing1012@qq.com
     * @date : 2018/11/10 17:31
     */
    public static class Line {
        private final String content;
        private final long startTime;

        public Line(String content, long startTime) {
            this.content = content;
            this.startTime = startTime;
        }

        public String getContent() {
            return content;
        }

        public long getStartTime() {
            return startTime;
        }

        @Override
        public String toString() {
            return "\"Line\": {"
                    + "\"content\": \"" + content + '\"'
                    + ", \"startTime\": \"" + startTime
                    + '}';
        }
    }
}
