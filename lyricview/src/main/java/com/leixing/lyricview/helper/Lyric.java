package com.leixing.lyricview.helper;

import java.util.List;

/**
 * description : 歌词
 *
 * @author : leixing
 * email : leixing@baidu.com
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
    private List<LyricLine> lyricLines;

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

    public List<LyricLine> getLyricLines() {
        return lyricLines;
    }

    public Lyric setLyricLines(List<LyricLine> lyricLines) {
        this.lyricLines = lyricLines;
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
                + ", \"lyricLines\": \"" + "\n";
        StringBuilder sb = new StringBuilder(str);
        for (LyricLine line : lyricLines) {
            sb.append(line).append("\n");
        }
        sb.append('}');
        return sb.toString();
    }
}
