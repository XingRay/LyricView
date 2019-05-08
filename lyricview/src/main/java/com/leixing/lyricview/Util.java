package com.leixing.lyricview;

import android.os.Build;
import android.support.annotation.NonNull;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 工具类
 *
 * @author leixing
 */
class Util {
    private Util() {
        throw new UnsupportedOperationException();
    }

    static long valueOfRatio(long start, long end, double ratio) {
        return start + (long) ((end - start) * ratio);
    }

    static int compare(long x, long y) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return Long.compare(x, y);
        } else {
            // noinspection UseCompareMethod
            return (x < y) ? -1 : ((x == y) ? 0 : 1);
        }
    }

    @NonNull
    static StaticLayout getStaticLayout(String content, TextPaint paint, int width, Layout.Alignment alignment) {
        StaticLayout staticLayout;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            staticLayout = StaticLayout.Builder.obtain(content, 0, content.length(), paint, width).setAlignment(alignment).build();
        } else {
            // noinspection deprecation
            staticLayout = new StaticLayout(content, paint, width, alignment, 1f, 0f, false);
        }
        return staticLayout;
    }

    /**
     * 查找指定时间播放的歌词组的下标
     *
     * @param lineGroups 歌词组列表
     * @param timeMills  待匹配的时间戳
     * @return 匹配的歌词组的下标
     */
    static int getGroupIndexByTimeMills(List<LineGroup> lineGroups, long timeMills) {
        if (lineGroups == null || lineGroups.isEmpty()) {
            return 0;
        }
        if (timeMills <= lineGroups.get(0).getStartMills()) {
            return 0;
        }
        if (timeMills >= lineGroups.get(lineGroups.size() - 1).getEndMills()) {
            return lineGroups.size() - 1;
        }
        int index = Collections.binarySearch(lineGroups, new LineGroup(null, timeMills, timeMills), new Comparator<LineGroup>() {
            @Override
            public int compare(LineGroup o1, LineGroup o2) {
                if (o2.getStartMills() >= o1.getStartMills() && o2.getEndMills() <= o1.getEndMills()) {
                    return 0;
                } else {
                    return Util.compare(o1.getStartMills(), o2.getStartMills());
                }
            }
        });
        if (index < 0) {
            index = 0;
        }
        return index;
    }

    /**
     * 查找指定时间播放的歌词行的下标
     *
     * @param lines     歌词行列表
     * @param timeMills 待匹配的时间戳
     * @return 匹配的歌词行的下标
     */
    static int getLineIndexByTimeMills(Line[] lines, long timeMills) {
        if (lines == null || lines.length == 0) {
            return 0;
        }

        if (timeMills <= lines[0].getStartMills()) {
            return 0;
        }
        if (timeMills >= lines[lines.length - 1].getEndMills()) {
            return lines.length - 1;
        }
        int index = Collections.binarySearch(Arrays.asList(lines), new Line(timeMills, timeMills, ""), new Comparator<Line>() {
            @Override
            public int compare(Line o1, Line o2) {
                if (o2.getStartMills() >= o1.getStartMills() && o2.getEndMills() <= o1.getEndMills()) {
                    return 0;
                } else {
                    return Util.compare(o1.getStartMills(), o2.getStartMills());
                }
            }
        });
        if (index < 0) {
            index = 0;
        }
        return index;
    }

    static float calcInterValue(float from, float to, float ratio) {
        return (1 - ratio) * from + ratio * to;
    }

    /**
     * 计算一个从{@code startValue}到{@code endValue}的中间值，取值由{@code fraction}决定
     * 当{@code fraction}为0时，返回{@code startValue}，为1时返回{@code endValue}。
     * 注意：渐变值是将{@code int}型的整数分为4个{@code byte}独立渐变的。
     *
     * @param startValue 起始值
     * @param endValue   终止值
     * @param fraction   比例值
     * @return 中间值
     */
    static int evaluateInt(int startValue, int endValue, float fraction) {
        int start3 = (startValue >> 24) & 0xff;
        int start2 = (startValue >> 16) & 0xff;
        int start1 = (startValue >> 8) & 0xff;
        int start0 = startValue & 0xff;

        int end3 = (endValue >> 24) & 0xff;
        int end2 = (endValue >> 16) & 0xff;
        int end1 = (endValue >> 8) & 0xff;
        int end0 = endValue & 0xff;

        int result3 = (start3 + (int) (fraction * (end3 - start3))) & 0xff;
        int result2 = (start2 + (int) (fraction * (end2 - start2))) & 0xff;
        int result1 = (start1 + (int) (fraction * (end1 - start1))) & 0xff;
        int result0 = (start0 + (int) (fraction * (end0 - start0)));

        return (result3 << 24)
                | (result2 << 16)
                | (result1 << 8)
                | result0;
    }
}
