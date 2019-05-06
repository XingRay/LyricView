package com.leixing.lyricview;

import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * description : xxx
 *
 * @author : leixing
 * email : leixing1012@qq.com
 * @date : 2018/11/10 19:23
 */
public class LyricParser {
    /**
     * 标题
     */
    private static final String PREFIX_TITLE = "[ti:";

    /**
     * 歌手
     */
    private static final String PREFIX_ARTIST = "[ar:";

    /**
     * 专辑
     */
    private static final String PREFIX_ALBUM = "[al:";

    /**
     * 制作
     */
    private static final String PREFIX_BY = "[by:";

    /**
     * 偏移量
     */
    private static final String PREFIX_OFFSET = "[offset:";

    private static final String[] PREFIXES = new String[]{
            PREFIX_TITLE,
            PREFIX_ARTIST,
            PREFIX_ALBUM,
            PREFIX_BY,
            PREFIX_OFFSET
    };

    private LyricParser() {
        throw new UnsupportedOperationException();
    }

    /**
     * 从字符串中获得时间值
     */
    private static long getStartTimeMillis(String str) {
        long minute;
        long second = 0;
        long millis = 0;

        // noinspection ConstantConditions
        do {
            int minuteIndex = str.indexOf(":");
            if (minuteIndex < 0) {
                minute = toLong(str);
                break;
            }

            minute = toLong(str.substring(0, minuteIndex));

            int secondIndex = str.indexOf(".", minuteIndex + 1);
            if (secondIndex < 0) {
                second = toLong(str.substring(minuteIndex + 1));
                break;
            }

            second = toLong(str.substring(minuteIndex + 1, secondIndex));
            millis = toLong(str.substring(secondIndex + 1));
        } while (false);

        return millis + second * 1000 + minute * 60 * 1000;
    }

    @SuppressWarnings("unused")
    public static Lyric parseLyric(String text) {
        String[] lines = text.split("\n");
        return parseLyric(lines);
    }

    private static Lyric parseLyric(String[] lines) {
        Lyric lyric = new Lyric();
        for (String line : lines) {
            parseLyricLine(lyric, line);
        }

        sortLyricLines(lyric);
        return lyric;
    }

    private static void sortLyricLines(Lyric lyric) {
        Collections.sort(lyric.getLines(), new Comparator<Lyric.Line>() {
            @Override
            public int compare(Lyric.Line o1, Lyric.Line o2) {
                return Util.compare(o1.getStartTime(), o2.getStartTime());
            }
        });
    }

    public static Lyric parseLyric(InputStream ins) {
        return parseLyric(ins, "utf-8");
    }

    @SuppressWarnings("WeakerAccess")
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

            sortLyricLines(lyric);

            return lyric;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(reader);
            close(isr);
            close(ins);
        }
        return lyric;
    }

    private static void close(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 逐行解析歌词
     *
     * @param lyric    歌词
     * @param lineText 歌词行内容
     */
    private static void parseLyricLine(Lyric lyric, String lineText) {
        if (TextUtils.isEmpty(lineText)) {
            return;
        }

        if (parsePrefixLine(lyric, lineText)) {
            return;
        }

        // index of "["
        int leftIndex = 0;

        // index of "]"
        int rightIndex = 0;

        List<String> timeStrings = new ArrayList<>();
        int contentStartIndex = 0;

        do {
            leftIndex = lineText.indexOf("[", leftIndex);
            if (leftIndex < 0) {
                contentStartIndex = rightIndex + 1;
                break;
            }
            rightIndex = lineText.indexOf("]", leftIndex + 1);
            if (rightIndex < 0) {
                break;
            }

            // 歌词行时间
            String s = lineText.substring(leftIndex + 1, rightIndex);
            timeStrings.add(s);

            leftIndex = rightIndex + 1;

        } while (true);

        if (timeStrings.isEmpty() || contentStartIndex < 0) {
            return;
        }

        // 歌词内容
        String content = lineText.substring(contentStartIndex);

        for (String timeString : timeStrings) {
            Lyric.Line line = new Lyric.Line(content, getStartTimeMillis(timeString));
            List<Lyric.Line> lyricLines = lyric.getLines();
            if (lyricLines == null) {
                lyricLines = new ArrayList<>();
                lyric.setLines(lyricLines);
            }
            lyricLines.add(line);
        }
    }

    private static boolean parsePrefixLine(Lyric lyric, String lineText) {
        int index = lineText.indexOf("]");

        for (String prefix : PREFIXES) {
            if (!lineText.startsWith(prefix)) {
                continue;
            }
            String text = lineText.substring(prefix.length(), index).trim();
            switch (prefix) {
                case PREFIX_TITLE:
                    lyric.setTitle(text);
                    break;

                case PREFIX_ARTIST:
                    lyric.setArtist(text);
                    break;

                case PREFIX_ALBUM:
                    lyric.setAlbum(text);
                    break;

                case PREFIX_BY:
                    lyric.setBy(text);
                    break;

                case PREFIX_OFFSET:
                    lyric.setOffset(toLong(text));
                    break;

                default:
            }
            return true;
        }
        return false;
    }

    public static List<Line> toLines(Lyric lyric) {
        List<Line> lines = new ArrayList<>();

        List<Lyric.Line> lyricLines = lyric.getLines();
        if (lyricLines == null || lyricLines.isEmpty()) {
            return lines;
        }

        for (int i = 0, size = lyricLines.size() - 1; i < size; i++) {
            Lyric.Line currentLine = lyricLines.get(i);
            Lyric.Line nextLine = lyricLines.get(i + 1);
            lines.add(new Line(currentLine.getStartTime(),
                    nextLine.getStartTime() - 1, currentLine.getContent()));
        }
        Lyric.Line lastLine = lyricLines.get(lyricLines.size() - 1);
        String content = lastLine.getContent();
        if (!TextUtils.isEmpty(content)) {
            lines.add(new Line(lastLine.getStartTime(), lastLine.getStartTime(), content));
        }

        if (isAllContentEmpty(lines)) {
            lines.clear();
        }
        return lines;
    }

    private static boolean isAllContentEmpty(List<Line> lines) {
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

    private static long toLong(String s) {
        long value = 0;
        if (TextUtils.isEmpty(s)) {
            return value;
        }
        String number = s.trim();

        try {
            value = Long.parseLong(number);
        } catch (NumberFormatException e) {
            try {
                value = (long) Double.parseDouble(number);
            } catch (NumberFormatException e1) {
                e1.printStackTrace();
            }
        }

        return value;
    }
}
