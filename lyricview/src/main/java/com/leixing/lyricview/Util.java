package com.leixing.lyricview;

import android.os.Build;

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
}
