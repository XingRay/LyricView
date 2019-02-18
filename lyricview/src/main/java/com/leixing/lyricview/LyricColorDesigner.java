package com.leixing.lyricview;

/**
 * description : xxx
 *
 * @author : leixing
 * email : leixing@baidu.com
 * @date : 2018/12/19 20:24
 */
public class LyricColorDesigner implements ColorDesigner {
    private static final String TAG = LyricColorDesigner.class.getSimpleName();

    private final int mHighlightColor;
    private final int mTextStartColor;
    private final int mTextEndColor;
    private final float mFirstLineDistance;

    public LyricColorDesigner(float highlightTextHeight, float textHeight, float spacing,
                              int highlightColor, int textStartColor, int textEndColor) {
        mHighlightColor = highlightColor;
        mTextStartColor = textStartColor;
        mTextEndColor = textEndColor;

        mFirstLineDistance = spacing + (textHeight + highlightTextHeight) / 2;
    }

    @Override
    public int getColor(float offsetYFromCenter, int height) {
        float absOffsetY = Math.abs(offsetYFromCenter);

        if (absOffsetY == 0) {
            return mHighlightColor;
        } else if (absOffsetY <= mFirstLineDistance) {
            float fraction = absOffsetY / mFirstLineDistance;
            return evaluateInt(mHighlightColor, mTextStartColor, fraction);
        } else if (height > mFirstLineDistance) {
            float fraction = (absOffsetY - mFirstLineDistance) / ((height >> 1) - mFirstLineDistance);
            return evaluateInt(mTextStartColor, mTextEndColor, fraction);
        } else {
            return mTextEndColor;
        }
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

        return ((start3 + (int) (fraction * (end3 - start3))) << 24)
                | ((start2 + (int) (fraction * (end2 - start2))) << 16)
                | ((start1 + (int) (fraction * (end1 - start1))) << 8)
                | (start0 + (int) (fraction * (end0 - start0)));
    }
}
