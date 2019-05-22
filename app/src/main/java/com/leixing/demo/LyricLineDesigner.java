package com.leixing.demo;

import android.graphics.Paint;

import com.leixing.lyricview.LineDesigner;

/**
 * description : xxx
 *
 * @author : leixing
 * email : leixing1012@qq.com
 * @date : 2018/12/19 20:24
 */
public class LyricLineDesigner implements LineDesigner {
    private static final String TAG = LyricLineDesigner.class.getSimpleName();

    private final int mTextStartColor;
    private final int mTextEndColor;
    private final float mStartTextSize;
    private final float mEndTextSize;

    public LyricLineDesigner(int textStartColor, int textEndColor, float startTextSize, float endTextSize) {
        mTextStartColor = textStartColor;
        mTextEndColor = textEndColor;
        mStartTextSize = startTextSize;
        mEndTextSize = endTextSize;
    }

    @Override
    public void designLine(Paint paint, float offsetY, float highlightOffset, int height) {
        float absOffsetY = Math.abs(offsetY - highlightOffset);
        float gradientHeight = Math.max(height - highlightOffset, highlightOffset);
        float fraction = absOffsetY / gradientHeight;

        int color = evaluateInt(mTextStartColor, mTextEndColor, fraction);
        float textSize = calcInterValue(mStartTextSize, mEndTextSize, fraction);

        paint.setColor(color);
        paint.setTextSize(textSize);
    }

    private static int evaluateInt(int startValue, int endValue, float fraction) {
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

    static float calcInterValue(float from, float to, float ratio) {
        return (1 - ratio) * from + ratio * to;
    }
}
