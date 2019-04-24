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
    private final int mGradientHeightGap;

    public LyricColorDesigner(float highlightTextHeight, float textHeight, float spacing,
                              int highlightColor, int textStartColor, int textEndColor) {
        mHighlightColor = highlightColor;
        mTextStartColor = textStartColor;
        mTextEndColor = textEndColor;

        mFirstLineDistance = spacing + (textHeight + highlightTextHeight) / 2;
        mGradientHeightGap = (int) (highlightTextHeight / 2 + spacing);
    }

    @Override
    public int getColor(float offsetY, int highlightOffset, int height) {
        float absOffsetY = Math.abs(offsetY - highlightOffset);

        if (absOffsetY == 0) {
            return mHighlightColor;
        } else if (absOffsetY <= mFirstLineDistance) {
            float fraction = absOffsetY / mFirstLineDistance;
            return evaluateInt(mHighlightColor, mTextStartColor, fraction);
        } else if (height > mFirstLineDistance) {
            int gradientHeight = Math.max(highlightOffset, height - highlightOffset) - mGradientHeightGap;
            float fraction = (absOffsetY - mFirstLineDistance) / gradientHeight;
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
