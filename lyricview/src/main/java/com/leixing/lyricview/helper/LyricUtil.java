package com.leixing.lyricview.helper;

import android.text.TextUtils;

import com.leixing.lyricview.LyricView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * description : xxx
 *
 * @author : leixing
 * email : leixing@baidu.com
 * @date : 2018/11/10 19:23
 */
public class LyricUtil {
    private LyricUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * 从字符串中获得时间值
     */
    public static long getStartTimeMillis(String str) {
        long minute = Long.parseLong(str.substring(1, 3));
        long second = Long.parseLong(str.substring(4, 6));
        long millisecond = Long.parseLong(str.substring(7, 9));
        return millisecond + second * 1000 + minute * 60 * 1000;
    }

    public static Lyric parseLyric(String text) {
        String[] lines = text.split("\n");
        return parseLyric(lines);
    }

    private static Lyric parseLyric(String[] lines) {
        Lyric lyric = new Lyric();
        for (String line : lines) {
            parseLyricLine(lyric, line);
        }
        return lyric;
    }

    public static Lyric parseLyric(InputStream ins) {
        return parseLyric(ins, "utf-8");
    }

    public static Lyric parseLyric(InputStream ins, String charsetName) {
        Lyric lyric = new Lyric();
        InputStreamReader isr = null;
        BufferedReader reader = null;

        try {
            isr = new InputStreamReader(ins, charsetName);
            reader = new BufferedReader(isr);
            String line;
            while ((line = reader.readLine()) != null) {
                parseLyricLine(lyric, line);
            }
            return lyric;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
                if (isr != null) {
                    isr.close();
                }
                if (ins != null) {
                    ins.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return lyric;
    }

    /**
     * 逐行解析歌词
     *
     * @param lyric    歌词
     * @param lineText 歌词行内容
     */
    private static void parseLyricLine(Lyric lyric, String lineText) {

        int index = lineText.lastIndexOf("]");

        // 标题
        if (!TextUtils.isEmpty(lineText) && lineText.startsWith("[ti:")) {
            lyric.setTitle(lineText.substring(4, index).trim());
            return;
        }

        // 歌手
        if (!TextUtils.isEmpty(lineText) && lineText.startsWith("[ar:")) {
            lyric.setArtist(lineText.substring(4, index).trim());
            return;
        }

        // 专辑
        if (!TextUtils.isEmpty(lineText) && lineText.startsWith("[al:")) {
            lyric.setAlbum(lineText.substring(4, index).trim());
            return;
        }

        // 制作
        if (!TextUtils.isEmpty(lineText) && lineText.startsWith("[by:")) {
            lyric.setBy(lineText.substring(4, index).trim());
            return;
        }

        // 偏移量
        if (!TextUtils.isEmpty(lineText) && lineText.startsWith("[offset:")) {
            lyric.setOffset(Long.parseLong(lineText.substring(8, index).trim()));
            return;
        }

        // 歌词内容
        if (index == 9 && lineText.trim().length() >= 10) {
            LyricLine line = new LyricLine();
            line.setStartTime(getStartTimeMillis(lineText.substring(0, 10)));
            if (lineText.length() == 10) {
                line.setContent("");
            } else {
                line.setContent(lineText.substring(10, lineText.length()));
            }
            List<LyricLine> lyricLines = lyric.getLyricLines();
            if (lyricLines == null) {
                lyricLines = new ArrayList<>();
                lyric.setLyricLines(lyricLines);
            }
            lyricLines.add(line);
        }
    }

    public static List<LyricView.Line> toLines(Lyric lyric) {
        List<LyricView.Line> lines = new ArrayList<>();

        List<LyricLine> lyricLines = lyric.getLyricLines();
        if (lyricLines == null || lyricLines.isEmpty()) {
            return lines;
        }

        for (int i = 0, size = lyricLines.size() - 1; i < size; i++) {
            LyricLine currentLine = lyricLines.get(i);
            LyricLine nextLine = lyricLines.get(i + 1);
            lines.add(new LyricView.Line(currentLine.getStartTime(), nextLine.getStartTime() - 1, currentLine.getContent()));
        }
        LyricLine lastLine = lyricLines.get(lyricLines.size() - 1);
        String content = lastLine.getContent();
        if (!TextUtils.isEmpty(content)) {
            lines.add(new LyricView.Line(lastLine.getStartTime(), lastLine.getStartTime(), content));
        }

        return lines;
    }
}
