package com.leixing.lyricview;

import android.os.Build;
import android.support.annotation.NonNull;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

/**
 * 工具类
 *
 * @author leixing
 */
public class Util {
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
}
