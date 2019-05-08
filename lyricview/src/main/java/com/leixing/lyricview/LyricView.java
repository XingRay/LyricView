package com.leixing.lyricview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * description : xxx
 *
 * @author : leixing
 * email : leixing1012@qq.com
 * @date : 2018/11/10 20:04
 */
@SuppressWarnings("unused")
public class LyricView extends View {

    private static final String TAG = LyricView.class.getSimpleName();

    // default values

    private static final int TEXT_COLOR_DEFAULT = 0xff000000;
    private static final float TEXT_SIZE_DEFAULT = 60.0f;

    private static final int HIGHLIGHT_TEXT_COLOR_DEFAULT = 0xffff0000;
    private static final float HIGHLIGHT_TEXT_SIZE_DEFAULT = 80.0f;

    private static final int KARAOKE_TEXT_COLOR_DEFAULT = 0xff0000ff;
    private static final boolean KARAOKE_ENABLE_DEFAULT = true;

    private static final float LINE_SPACING_DEFAULT = 10.0f;
    private static final float BREAK_LINE_SPACING_DEFAULT = 10.0f;
    private static final int SCROLL_TIME_INTERVAL_DEFAULT = 20;
    private static final int FLING_TIME_INTERVAL_DEFAULT = 20;

    private static final int SCROLL_TIME_MAX_DEFAULT = 300;
    private static final float FLING_ACCELERATE_DEFAULT = 0.005f;

    private static final int STOP_TIME_DEFAULT = 2000;
    private static final boolean AUTO_SCROLL_BACK_DEFAULT = true;

    private static final int HIGHLIGHT_MARGIN_TOP_DEFAULT = 0;

    public static final int HIGHLIGHT_GRAVITY_CENTER_VERTICAL = 0;
    public static final int HIGHLIGHT_GRAVITY_TOP = 1;
    public static final int HIGHLIGHT_GRAVITY_BOTTOM = 2;

    public static final int LINE_GRAVITY_CENTER_HORIZONTAL = 0;
    public static final int LINE_GRAVITY_LEFT = 1;
    public static final int LINE_GRAVITY_RIGHT = 2;
    public static final int INDEX_NONE = -1;

    // attributes

    private int mTextColor;
    private float mTextSize;

    private int mHighlightTextColor;
    private float mHighlightTextSize;

    private boolean mKaraokeEnable;
    private int mKaraokeTextColor;

    /**
     * pixel / mills^2
     */
    private float mFlingAccelerate;
    private int mFlingTimeInterval;

    private float mLineDistance;
    private float mBreakLineDistance;

    private int mScrollTimeInterval;
    private int mScrollTimeMax;

    private boolean mAutoScrollBack;
    private int mStopTime;

    // saved state

    private List<Line> mLines = new ArrayList<>();

    /**
     * 歌词当前播放到的时间，单位为 mills
     */
    private long mCurrentTimeMills = 0;

    // helpers

    private InternalHandler mHandler;

    // temp values

    /**
     * 控件顶部(不是)到第0行控件顶部的偏移量，向下为正
     * 与{@link #mTranslationY}共同决定歌词绘制的位置
     */
    private float mViewToLyric;

    /**
     * 控件在Y方向上的偏移量，向下为正
     * 与{@link #mViewToLyric}共同决定歌词绘制的位置
     */
    private float mTranslationY;

    /**
     * 歌词绘制区域左边界
     */
    private int mDrawRegionLeft;
    /**
     * 歌词绘制区域上边界
     */
    private int mDrawRegionTop;
    /**
     * 歌词绘制区域右边界
     */
    private int mDrawRegionRight;
    /**
     * 歌词绘制区域下边界
     */
    private int mDrawRegionBottom;
    /**
     * 歌词绘制区域宽度
     */
    private int mDrawRegionWidth;
    /**
     * 歌词绘制区域高度
     */
    private int mDrawRegionHeight;

    /**
     * 原始的歌词行经过换行分割后生成的歌词行组
     */
    private List<LineGroup> mLineGroups = new ArrayList<>();

    /**
     * 通过{@link #mCurrentTimeMills}在{@link #mLineGroups}中查找到匹配的{@link LineGroup}
     * 对象的下标。改行即为当前播放的歌词。
     * 当{@link #mCurrentTimeMills}小于第0行歌词的起始时间时取值为0
     * 当{@link #mCurrentTimeMills}大于最后一行歌词的终止时间时取值为{@code mLineGroups.size()-1}
     */
    private int mCurrentGroupIndex;

    /**
     * 记录之前的播放行的下标，用于在做歌词行切换的时候，确定该行要执行高亮行到普通行的动画
     */
    private int mLastGroupIndex = -1;

    private int mHighlightOffset;

    private float mTouchStartY;

    private float mTextHeight;
    private TextPaint mTextPaint;
    private TextPaint mHighlightTextPaint;
    private TextPaint mZoomOutPaint;
    private TextPaint mZoomInPaint;
    private Paint.FontMetrics mTextFontMetrics;
    private Paint.FontMetrics mHighlightFontMetrics;
    private RectF mPlayedRegion = new RectF();
    private RectF mUnPlayedRegion = new RectF();
    private State mState = State.IDLE;
    private VelocityTracker mVelocityTracker;

    /**
     * pixel / mills
     */
    private float mFlingVelocity;
    private float mScrollVelocity;
    private float mHighlightTextHeight;
    private long mFlingTimeMills;
    private float mFlingMinOffsetY;
    private float mFlingMaxOffsetY;
    private float mTouchMinOffsetY;
    private float mTouchMaxOffsetY;

    private int mHighlightGravity;
    private float mViewToHighlightGroup;

    // listeners

    private TouchListener mTouchListener;
    private ColorDesigner mColorDesigner;
    private Layout.Alignment mAlignment;
    private float mLyricToHighlightGroup;
    private float mTranslatedViewToLyric;


    public LyricView(Context context) {
        this(context, null);
    }

    public LyricView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LyricView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mHandler = new InternalHandler();

        mTextPaint = new TextPaint();
        mTextPaint.setAntiAlias(true);

        mHighlightTextPaint = new TextPaint();
        mHighlightTextPaint.setAntiAlias(true);

        mZoomOutPaint = new TextPaint();
        mZoomOutPaint.setAntiAlias(true);

        mZoomInPaint = new TextPaint();
        mZoomInPaint.setAntiAlias(true);


        applyAttributes(context, attrs);
    }

    private void applyAttributes(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.LyricView);
        if (typedArray == null) {
            return;
        }

        setTextColor(typedArray.getColor(R.styleable.LyricView_lyric_view_text_color,
                TEXT_COLOR_DEFAULT));

        setTextSize(typedArray.getDimension(R.styleable.LyricView_lyric_view_text_size,
                TEXT_SIZE_DEFAULT));

        setHighlightColor(typedArray.getColor(R.styleable.LyricView_lyric_view_highlight_text_color,
                HIGHLIGHT_TEXT_COLOR_DEFAULT));

        setHighlightTextSize(typedArray.getDimension(R.styleable.LyricView_lyric_view_highlight_text_size,
                HIGHLIGHT_TEXT_SIZE_DEFAULT));

        setKaraokeEnable(typedArray.getBoolean(R.styleable.LyricView_lyric_view_karaoke_enable,
                KARAOKE_ENABLE_DEFAULT));

        setKaraokeColor(typedArray.getColor(R.styleable.LyricView_lyric_view_karaoke_color,
                KARAOKE_TEXT_COLOR_DEFAULT));

        setLineDistance(typedArray.getDimension(R.styleable.LyricView_lyric_view_line_distance,
                LINE_SPACING_DEFAULT));

        setBreakLineDistance(typedArray.getDimension(R.styleable.LyricView_lyric_view_break_line_distance,
                BREAK_LINE_SPACING_DEFAULT));

        setFlingAccelerate(typedArray.getFloat(R.styleable.LyricView_lyric_view_fling_accelerate,
                FLING_ACCELERATE_DEFAULT));

        setFlingTimeInterval(typedArray.getInt(R.styleable.LyricView_lyric_view_fling_time_interval,
                FLING_TIME_INTERVAL_DEFAULT));

        setScrollTimeInterval(typedArray.getInt(R.styleable.LyricView_lyric_view_scroll_time_interval,
                SCROLL_TIME_INTERVAL_DEFAULT));

        setScrollTimeMax(typedArray.getInt(R.styleable.LyricView_lyric_view_scroll_time_max,
                SCROLL_TIME_MAX_DEFAULT));

        setStopTime(typedArray.getInt(R.styleable.LyricView_lyric_view_stop_time, STOP_TIME_DEFAULT));

        setAutoScrollBack(typedArray.getBoolean(R.styleable.LyricView_lyric_view_auto_scroll_back,
                AUTO_SCROLL_BACK_DEFAULT));

        setHighlightGravity(parseHighlightGravity(typedArray.getString(R.styleable.LyricView_lyric_view_highlight_gravity)));

        setLineGravity(parseLineGravity(typedArray.getString(R.styleable.LyricView_lyric_view_line_gravity)));

        setHighlightOffset(typedArray.getDimension(R.styleable.LyricView_lyric_view_highlight_offset,
                HIGHLIGHT_MARGIN_TOP_DEFAULT));

        typedArray.recycle();
    }

    private int parseHighlightGravity(String gravityString) {
        int gravity = HIGHLIGHT_GRAVITY_CENTER_VERTICAL;
        if (gravityString != null) {
            try {
                gravity = Integer.parseInt(gravityString);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return gravity;
    }

    private int parseLineGravity(String gravityString) {
        int gravity = LINE_GRAVITY_CENTER_HORIZONTAL;
        if (gravityString != null) {
            try {
                gravity = Integer.parseInt(gravityString);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return gravity;
    }

    private void setLineGravity(int lineGravity) {
        switch (lineGravity) {
            case LINE_GRAVITY_LEFT:
                mAlignment = Layout.Alignment.ALIGN_LEFT;
                break;
            case LINE_GRAVITY_RIGHT:
                mAlignment = Layout.Alignment.ALIGN_RIGHT;
                break;
            case LINE_GRAVITY_CENTER_HORIZONTAL:
            default:
                mAlignment = Layout.Alignment.ALIGN_CENTER;

        }
    }

    private void setHighlightOffset(float highlightMarginTop) {
        mViewToHighlightGroup = highlightMarginTop;
        invalidate();
    }

    private void setHighlightGravity(int gravity) {
        mHighlightGravity = gravity;
        invalidate();
    }

    public void setStopTime(int stopTime) {
        mStopTime = stopTime;
    }

    public void setScrollTimeMax(int scrollTimeMax) {
        mScrollTimeMax = scrollTimeMax;
    }

    public void setScrollTimeInterval(int scrollTimeInterval) {
        mScrollTimeInterval = scrollTimeInterval;
    }

    public void setFlingTimeInterval(int flingTimeInterval) {
        mFlingTimeInterval = flingTimeInterval;
    }

    public void setFlingAccelerate(float flingAccelerate) {
        mFlingAccelerate = flingAccelerate;
    }

    public void setLyric(List<Line> lines) {
        reset();
        mLines.clear();
        if (lines != null) {
            mLines.addAll(lines);
        }

        if (mDrawRegionWidth > 0) {
            updateLinesVariables();
            mTranslationY = 0;
            updateTranslatedViewToLyric();
            invalidate();
        }
    }

    public void setTextSize(float textSize) {
        if (mTextSize == textSize) {
            return;
        }
        mTextSize = textSize;
        mTextPaint.setTextSize(mTextSize);

        mTextFontMetrics = mTextPaint.getFontMetrics();
        mTextHeight = mTextFontMetrics.descent - mTextFontMetrics.ascent;

        invalidate();
    }

    public void setTextColor(int color) {
        if (mTextColor == color) {
            return;
        }
        mTextColor = color;
        mTextPaint.setColor(mTextColor);
        mZoomOutPaint.setColor(mTextColor);
        invalidate();
    }

    public void setHighlightTextSize(float textSize) {
        if (mHighlightTextSize == textSize) {
            return;
        }
        mHighlightTextSize = textSize;
        mHighlightTextPaint.setTextSize(mHighlightTextSize);
        mHighlightFontMetrics = mHighlightTextPaint.getFontMetrics();
        mHighlightTextHeight = mHighlightFontMetrics.descent - mHighlightFontMetrics.ascent;
    }

    public void setHighlightColor(int color) {
        if (mHighlightTextColor == color) {
            return;
        }
        mHighlightTextColor = color;
        mHighlightTextPaint.setColor(mHighlightTextColor);
        mZoomInPaint.setColor(mHighlightTextColor);

        invalidate();
    }

    public void setKaraokeColor(int color) {
        if (mKaraokeTextColor == color) {
            return;
        }
        mKaraokeTextColor = color;
        invalidate();
    }

    public void setKaraokeEnable(boolean enable) {
        if (mKaraokeEnable == enable) {
            return;
        }
        mKaraokeEnable = enable;
        invalidate();
    }

    public void setLineDistance(float lineSpacing) {
        if (mLineDistance == lineSpacing) {
            return;
        }
        mLineDistance = lineSpacing;
        invalidate();
    }

    public void setBreakLineDistance(float breakLineSpacing) {
        if (mBreakLineDistance == breakLineSpacing) {
            return;
        }
        mBreakLineDistance = breakLineSpacing;
        invalidate();
    }

    public void setTime(final long mills) {
        if (mCurrentTimeMills == mills) {
            return;
        }
        mCurrentTimeMills = mills;

        int index = getGroupIndexByTimeMills(mCurrentTimeMills);
        if (index != mCurrentGroupIndex) {
            mLastGroupIndex = mCurrentGroupIndex;
            mCurrentGroupIndex = index;
            calcOffset();
            mTranslationY = mTranslatedViewToLyric - mViewToLyric;
        }

        switch (mState) {
            case IDLE:
                // 第0行到控件顶部的距离
                if (mTranslationY == 0) {
                    invalidate();
                    break;
                }

                Line line = mLines.get(mCurrentGroupIndex);
                long lineMills = line.getEndMills() - mills;
                long scrollTime = Math.min(mScrollTimeMax, lineMills);
                mScrollVelocity = Math.abs(mTranslationY) / scrollTime;
                updateState(State.SCROLLING_WITH_SCALE);
                performScroll();
                break;

            case SCROLLING_WITH_SCALE:
                return;

            case STAY:
            case TOUCHING:
            case FLINGING:
            case SCROLLING:
                invalidate();
                break;

            default:
                break;
        }
    }

    private void calcOffset() {
        mViewToHighlightGroup = getViewToHighlightGroup();
        mLyricToHighlightGroup = getLyricToHighlightGroup();
        mViewToLyric = mViewToHighlightGroup - mLyricToHighlightGroup;

        Log.i(TAG, "setTime"
                + "\nmCurrentGroupIndex: " + mCurrentGroupIndex
                + "\nmLastGroupIndex: " + mLastGroupIndex
                + "\nmViewToHighlightGroup: " + mViewToHighlightGroup
                + "\nmLyricToHighlightGroup: " + mLyricToHighlightGroup
                + "\nmViewToLyric: " + mViewToLyric
                + "\nmTranslatedViewToLyric: " + mTranslatedViewToLyric
                + "\nmDrawRegionHeight: " + mDrawRegionHeight
                + "\nmDrawRegionTop: " + mDrawRegionTop
                + "\nmLineDistance: " + mLineDistance
                + "\nmBreakLineDistance: " + mBreakLineDistance
                + "\nmTextHeight: " + mTextHeight
                + "\nmHighlightTextHeight: " + mHighlightTextHeight);
    }

    /**
     * 获取从歌词顶部到高量行中线的距离
     *
     * @return 距离值
     */
    private float getLyricToHighlightGroup() {
        int breakNum = 0;
        for (int i = 0; i < mCurrentGroupIndex; i++) {
            breakNum += Math.max(0, mLineGroups.get(i).getLines().length - 1);
        }
        int groupSize = getGroupSize(mCurrentGroupIndex);
        float groupHeight = getGroupHeight(mCurrentGroupIndex == 0 ? mHighlightTextHeight : mTextHeight, groupSize);

        return mCurrentGroupIndex * mLineDistance + breakNum * mBreakLineDistance + groupHeight * 0.5f;
    }

    /**
     * 计算没有偏移的情况下，控件顶部到高亮行中线的距离
     *
     * @return 距离值
     */
    private float getViewToHighlightGroup() {
        float offset;
        switch (mHighlightGravity) {
            case HIGHLIGHT_GRAVITY_TOP:
                offset = mDrawRegionTop + getGroupHeight(mHighlightTextHeight, getGroupSize(mCurrentGroupIndex)) * 0.5f;
                break;

            case HIGHLIGHT_GRAVITY_BOTTOM:
                offset = mDrawRegionBottom - getGroupHeight(mHighlightTextHeight, getGroupSize(mCurrentGroupIndex)) * 0.5f;
                break;

            case HIGHLIGHT_GRAVITY_CENTER_VERTICAL:
            default:
                offset = mDrawRegionTop + (mDrawRegionHeight * 0.5f);
        }
        return offset + mHighlightOffset;
    }

    private int getGroupSize(int groupIndex) {
        int groupSize;
        if (mLineGroups.isEmpty()) {
            groupSize = 1;
        } else {
            groupSize = mLineGroups.get(groupIndex).getLines().length;
        }
        return groupSize;
    }

    /**
     * 计算歌词组的高度
     *
     * @param textHeight 歌词组中换行歌词的高度
     * @param groupSize  歌词组中换行歌词的数量
     * @return 个词组的高度
     */
    private float getGroupHeight(float textHeight, int groupSize) {
        if (groupSize == 0) {
            return 0;
        }
        return textHeight + Math.max(0, groupSize - 1) * mBreakLineDistance;
    }

    public void setTouchListener(TouchListener listener) {
        mTouchListener = listener;
    }

    public void setColorDesigner(ColorDesigner colorDesigner) {
        mColorDesigner = colorDesigner;
    }

    public void setAutoScrollBack(boolean autoScrollBack) {
        mAutoScrollBack = autoScrollBack;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int fromIndex = getFromIndex();
        int toIndex = getToIndex();
        float offset = mTranslatedViewToLyric;

        int breakLineNum = 0;
        int lineNum = 0;

        for (int i = 0; i <= toIndex; i++) {
            LineGroup group = mLineGroups.get(i);
            if (i == mCurrentGroupIndex) {
                drawHighlightGroup(canvas, group, offset);
            } else {
                drawGroup(canvas, group, offset);
            }

            breakLineNum += Math.max(0, group.getLines().length - 1);
            lineNum++;

            // 避免float累加误差
            offset = mTranslatedViewToLyric + breakLineNum * mBreakLineDistance + lineNum * mLineDistance;
        }
    }

    private void drawHighlightGroup(Canvas canvas, LineGroup group, float offset) {
        Line[] lines = group.getLines();
        if (mCurrentGroupIndex != 0) {
            offset -= (mHighlightTextHeight - mTextHeight) * 0.5;
        }

        int index = getLineIndexByTimeMills(mCurrentTimeMills, lines);
        Line line;
        String content;
        float ascent;
        float x;

        // 绘制已经播放过的行
        for (int i = 0; i < index; i++) {
            line = lines[i];
            content = line.getContent();
            ascent = mHighlightFontMetrics.ascent;
            x = mDrawRegionLeft + (mDrawRegionWidth - mHighlightTextPaint.measureText(content)) * 0.5f;
            mHighlightTextPaint.setColor(mKaraokeTextColor);
            canvas.drawText(content, x, offset - ascent, mHighlightTextPaint);
            mHighlightTextPaint.setColor(mHighlightTextColor);
            offset += mBreakLineDistance;
        }

        // 绘制正在播放的行
        line = lines[index];
        content = line.getContent();
        ascent = mHighlightFontMetrics.ascent;
        float textWidth = mHighlightTextPaint.measureText(content);
        x = mDrawRegionLeft + (mDrawRegionWidth - textWidth) * 0.5f;
        drawKaraoke(canvas, mHighlightTextPaint, mKaraokeTextColor, mHighlightTextColor, textWidth, x, offset - ascent, line.getContent(), line.getStartMills(), line.getEndMills());
        mHighlightTextPaint.setColor(mHighlightTextColor);
        offset += mBreakLineDistance;

        // 绘制未播放的行
        for (int i = index + 1, size = lines.length; i < size; i++) {
            line = lines[i];
            content = line.getContent();
            ascent = mHighlightFontMetrics.ascent;
            x = mDrawRegionLeft + (mDrawRegionWidth - mHighlightTextPaint.measureText(content)) * 0.5f;
            canvas.drawText(content, x, offset - ascent, mHighlightTextPaint);
            offset += mBreakLineDistance;
        }
    }

    private void drawGroup(Canvas canvas, LineGroup group, float offset) {
        Line[] lines = group.getLines();
        for (Line line : lines) {
            String content = line.getContent();
            float ascent = mTextFontMetrics.ascent;
            float x = mDrawRegionLeft + (mDrawRegionWidth - mTextPaint.measureText(content)) * 0.5f;
            canvas.drawText(content, x, offset - ascent, mTextPaint);
            offset += mBreakLineDistance;
        }
    }

    private void drawLine(Canvas canvas, Line line, float offset, boolean isHighlight) {
        String content = line.getContent();
        TextPaint paint = isHighlight ? mHighlightTextPaint : mTextPaint;
        float ascent = isHighlight ? mHighlightFontMetrics.ascent : mTextFontMetrics.ascent;
        float x = mDrawRegionLeft + (mDrawRegionWidth - paint.measureText(content)) * 0.5f;

        canvas.drawText(content, x, offset - ascent, paint);
    }

    private int getFromIndex() {
        return 0;
    }

    private int getToIndex() {
        return mLineGroups.size() - 1;
    }
//
//    @Override
//    protected void onDraw(Canvas canvas) {
//        for (int i = 0, size = mLineGroups.size(); i < size; i++) {
//            boolean isHighlight = i == mCurrentGroupIndex;
//            float textHeight = isHighlight ? mHighlightTextHeight : mTextHeight;
//            float ascent = isHighlight ? mHighlightFontMetrics.ascent : mTextFontMetrics.ascent;
//            float offsetY = mViewToLyric + computeOffsetYByIndex(i, mCurrentGroupIndex);
//            float top = offsetY - textHeight / 2;
//            float baseLine = top - ascent;
//            float bottom = offsetY + textHeight / 2;
//
//            if (top > mDrawRegionHeight || bottom < 0) {
//                // out of bounds
//                continue;
//            }
//
//            Line line = mLines.get(i);
//            String content = line.getContent();
//            long startMills = line.getStartMills();
//            long endMills = line.getEndMills();
//
//            if (hasScaleAnimation()) {
//                if (i == mLastGroupIndex) {
//                    float x = ((int) (mDrawRegionWidth - mZoomOutPaint.measureText(content))) >> 1;
//                    if (mColorDesigner != null) {
//                        mZoomOutPaint.setColor(mColorDesigner.getColor(offsetY, mHighlightOffset, mDrawRegionHeight));
//                    }
//                    canvas.drawText(content, x, baseLine, mZoomOutPaint);
//                    continue;
//                } else if (i == mCurrentGroupIndex) {
//                    float x = ((int) (mDrawRegionWidth - mZoomInPaint.measureText(content))) >> 1;
//                    if (mColorDesigner != null) {
//                        mZoomInPaint.setColor(mColorDesigner.getColor(offsetY, mHighlightOffset, mDrawRegionHeight));
//                    }
//
//                    if (mKaraokeEnable) {
//                        drawKaraoke(canvas, mKaraokeZoomInPaint, mZoomInPaint, x, baseLine, content, startMills, endMills);
//                    } else {
//                        canvas.drawText(content, x, baseLine, mZoomInPaint);
//                    }
//                    continue;
//                }
//            }
//            if (isHighlight) {
//                float x = ((int) (mDrawRegionWidth - mHighlightTextPaint.measureText(content))) >> 1;
//                if (mColorDesigner != null) {
//                    mHighlightTextPaint.setColor(mColorDesigner.getColor(offsetY, mHighlightOffset, mDrawRegionHeight));
//                }
//                if (mKaraokeEnable) {
//                    drawKaraoke(canvas, mKaraokePaint, mHighlightTextPaint, x, baseLine, content, startMills, endMills);
//                } else {
//                    canvas.drawText(content, x, baseLine, mHighlightTextPaint);
//                }
//            } else {
//                float x = ((int) (mDrawRegionWidth - mTextPaint.measureText(content))) >> 1;
//                if (mColorDesigner != null) {
//                    mTextPaint.setColor(mColorDesigner.getColor(offsetY, mHighlightOffset, mDrawRegionHeight));
//                }
//                canvas.drawText(content, x, baseLine, mTextPaint);
//            }
//        }
//    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                updateState(State.TOUCHING);
                mTouchStartY = event.getY();
                if (mVelocityTracker == null) {
                    mVelocityTracker = VelocityTracker.obtain();
                } else {
                    mVelocityTracker.clear();
                }
                if (mTouchListener != null) {
                    mTouchListener.onTouchDown(mCurrentTimeMills);
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                mVelocityTracker.addMovement(event);
                float y = event.getY();
                float deltaY = y - mTouchStartY;
                mTouchStartY = y;
                mViewToLyric = limit(mViewToLyric + deltaY, mTouchMinOffsetY, mTouchMaxOffsetY);
                invalidate();
                if (mTouchListener != null) {
                    long timeMills = getTimeMillsByOffsetY(mViewToLyric);
                    mTouchListener.onTouchMoving(timeMills);
                }
                return true;

            case MotionEvent.ACTION_UP:
                mVelocityTracker.computeCurrentVelocity(1);
                mFlingVelocity = mVelocityTracker.getYVelocity();
                int minFlingVelocity = ViewConfiguration.get(getContext()).getScaledMinimumFlingVelocity() / 1000;
                if (Math.abs(mFlingVelocity) > minFlingVelocity) {
                    updateState(State.FLINGING);
                    performFling();
                } else {
                    mFlingVelocity = 0;
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                    updateState(State.STAY);
                }
                if (mTouchListener != null) {
                    long timeMills = getTimeMillsByOffsetY(mViewToLyric);
                    mTouchListener.onTouchUp(timeMills);
                }
                return true;

            case MotionEvent.ACTION_CANCEL:
                mVelocityTracker.recycle();
                mVelocityTracker = null;
            default:
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mDrawRegionLeft = getPaddingLeft();
        mDrawRegionRight = w - getPaddingRight();
        mDrawRegionTop = getPaddingTop();
        mDrawRegionBottom = h - getPaddingBottom();

        mDrawRegionWidth = mDrawRegionRight - mDrawRegionLeft;
        mDrawRegionHeight = mDrawRegionBottom - mDrawRegionTop;

        updateLinesVariables();
    }

    protected int getHighlightOffset(int h) {
        int offset;
        switch (mHighlightGravity) {
            case HIGHLIGHT_GRAVITY_TOP:
                offset = (int) (mHighlightTextHeight * 0.5);
                break;
            case HIGHLIGHT_GRAVITY_BOTTOM:
                offset = mHighlightOffset = h - (int) (mHighlightTextHeight * 0.5);
                break;

            case HIGHLIGHT_GRAVITY_CENTER_VERTICAL:
            default:
                offset = mHighlightOffset = h >> 1;
        }

        offset += (mViewToHighlightGroup + getPaddingTop());
        return offset;
    }

    private void updateLinesVariables() {
        splitLongLineToLineGroup();

        calcOffset();

        mFlingMinOffsetY = mHighlightOffset - computeOffsetYByIndex(mLines.size() - 1, 0);
        mFlingMaxOffsetY = mHighlightOffset;

        mTouchMinOffsetY = mHighlightOffset - computeOffsetYByIndex(mLines.size() + 3, 0);
        mTouchMaxOffsetY = mHighlightOffset + computeOffsetYByIndex(3, 0);
    }

    private void splitLongLineToLineGroup() {
        mLineGroups.clear();
        if (mLines.isEmpty()) {
            return;
        }

        for (Line line : mLines) {
            String content = line.getContent();
            long startMills = line.getStartMills();
            long endMills = line.getEndMills();
            float lineWidth = mHighlightTextPaint.measureText(content);
            if (lineWidth > mDrawRegionWidth) {
                StaticLayout layout = Util.getStaticLayout(content, mHighlightTextPaint, mDrawRegionWidth, mAlignment);
                int lineCount = layout.getLineCount();
                Line[] lines = new Line[lineCount];

                int length = content.length();

                for (int i = 0; i < lineCount; i++) {
                    int lineStart = layout.getLineStart(i);
                    int lineEnd = layout.getLineEnd(i);
                    long startTime = Util.valueOfRatio(startMills, endMills, ((double) lineStart) / length);
                    long endTime = Util.valueOfRatio(startMills, endMills, ((double) lineEnd) / length);
                    lines[i] = new Line(startTime, endTime, content.substring(lineStart, lineEnd));
                }
                mLineGroups.add(new LineGroup(lines, startMills, endMills));
            } else {
                mLineGroups.add(new LineGroup(new Line[]{line}, startMills, endMills));
            }
        }
    }

    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        SavedState savedState = new SavedState(super.onSaveInstanceState());
        savedState.mLines = mLines;
        savedState.mCurrentTimeMills = mCurrentTimeMills;
        savedState.mLineSpacing = mLineDistance;
        savedState.mKaraokeEnable = mKaraokeEnable;
        savedState.mKaraokeTextColor = mKaraokeTextColor;
        savedState.mHighlightTextColor = mHighlightTextColor;
        savedState.mTextColor = mTextColor;
        savedState.mTextSize = mTextSize;
        savedState.mHighlightTextSize = mHighlightTextSize;
        savedState.mFlingAccelerate = mFlingAccelerate;

        return savedState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());

        mLines = savedState.mLines;
        mCurrentTimeMills = savedState.mCurrentTimeMills;
        mLineDistance = savedState.mLineSpacing;
        mKaraokeEnable = savedState.mKaraokeEnable;
        mKaraokeTextColor = savedState.mKaraokeTextColor;
        mHighlightTextColor = savedState.mHighlightTextColor;
        mTextColor = savedState.mTextColor;
        mTextSize = savedState.mTextSize;
        mHighlightTextSize = savedState.mHighlightTextSize;
        mFlingAccelerate = savedState.mFlingAccelerate;
    }

//    private void scrollToLine(int index, boolean smooth) {
//        Line line = mLines.get(index);
//        mCurrentTimeMills = line.getStartMills();
//        float offsetY = mHighlightOffset - computeOffsetYByIndex(index, index);
//        if (mViewToLyric == offsetY) {
//            return;
//        }
//        if (smooth) {
//            mScrollOffsetYTo = offsetY;
//            performScroll();
//        } else {
//            mViewToLyric = offsetY;
//            invalidate();
//        }
//    }


    private void drawKaraoke(Canvas canvas, Paint textPaint, int karaokeColor, int color, float textWidth, float x,
                             float baseLine, String content, long startMills, long endMills) {
        float playedWidth;

        if (endMills == startMills) {
            playedWidth = mCurrentTimeMills < startMills ? 0 : (int) textWidth;
        } else {
            playedWidth = textWidth * (mCurrentTimeMills - startMills) / (endMills - startMills);
        }

        if (playedWidth > textWidth) {
            playedWidth = textWidth;
        }

        if (playedWidth < 0) {
            playedWidth = 0;
        }

        // 绘制已经播放的部分
        mPlayedRegion.left = x;
        mPlayedRegion.top = baseLine + mHighlightFontMetrics.ascent;
        mPlayedRegion.right = x + playedWidth;
        mPlayedRegion.bottom = baseLine + mHighlightFontMetrics.descent;
        canvas.save();
        canvas.clipRect(mPlayedRegion);
        textPaint.setColor(karaokeColor);
        canvas.drawText(content, x, baseLine, textPaint);
        canvas.restore();

        //绘制未播放的部分
        mUnPlayedRegion.left = x + playedWidth;
        mUnPlayedRegion.top = baseLine + mHighlightFontMetrics.ascent;
        mUnPlayedRegion.right = x + textWidth;
        mUnPlayedRegion.bottom = baseLine + mHighlightFontMetrics.descent;
        canvas.save();
        canvas.clipRect(mUnPlayedRegion);
        textPaint.setColor(color);
        canvas.drawText(content, x, baseLine, textPaint);
        canvas.restore();
    }

    /**
     * @param timeMills 待匹配的时间戳
     * @return 匹配的歌词行的下标
     * @see #mCurrentGroupIndex
     */
    private int getGroupIndexByTimeMills(long timeMills) {
        if (mLineGroups.isEmpty()) {
            return 0;
        }
        if (timeMills <= mLineGroups.get(0).getStartMills()) {
            return 0;
        }
        if (timeMills >= mLineGroups.get(mLineGroups.size() - 1).getEndMills()) {
            return mLineGroups.size() - 1;
        }
        int index = Collections.binarySearch(mLineGroups, new LineGroup(null, timeMills, timeMills), new Comparator<LineGroup>() {
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

    private int getLineIndexByTimeMills(long timeMills, Line[] lines) {
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

    private void performScroll() {
        if (mState != State.SCROLLING_WITH_SCALE && mState != State.SCROLLING) {
            return;
        }

        float distance = mScrollVelocity * mScrollTimeInterval;

        if (Math.abs(mTranslationY) > distance) {
            mTranslationY += mTranslationY > 0 ? -distance : distance;
        } else {
            mTranslationY = 0;
        }
        updateTranslatedViewToLyric();

//        if (hasScaleAnimation()) {
//            float ratio = (mViewToLyric - mScrollOffsetYFrom) / (mScrollOffsetYTo - mScrollOffsetYFrom);
//            float zoomOutTextSize = calcInterValue(mHighlightTextSize, mTextSize, ratio);
//            float zoomInTextSize = calcInterValue(mTextSize, mHighlightTextSize, ratio);
//
//            mZoomOutPaint.setTextSize(zoomOutTextSize);
//            mZoomInPaint.setTextSize(zoomInTextSize);
//            mKaraokeZoomInPaint.setTextSize(zoomInTextSize);
//        }

        invalidate();
        if (mTranslationY == 0) {
            updateState(State.IDLE);
            return;
        }

        sendMessage(InternalHandler.SCROLL, mScrollTimeInterval);
    }

    private void updateTranslatedViewToLyric() {
        mTranslatedViewToLyric = mViewToLyric + mTranslationY;
    }

    private boolean hasScaleAnimation() {
        return mState == State.SCROLLING_WITH_SCALE;
    }

    private long getTimeMillsByOffsetY(float offsetY) {
        int index = (int) ((mHighlightOffset - offsetY) / (mTextHeight + mLineDistance) + 0.5f);
        if (index < 0) {
            index = 0;
        } else if (index >= mLines.size()) {
            index = mLines.size() - 1;
        }
        return mLines.get(index).getStartMills();
    }

    private float computeOffsetYByTimeMills(long timeMills) {
        int index = getGroupIndexByTimeMills(timeMills);
        return computeOffsetYByIndex(index, index);
    }

    /**
     * 计算{@link #mLineGroups}中的{@code index}指向的歌词中线到首行中线的偏移量
     *
     * @param index          指定的歌词行下标
     * @param highlightIndex 当前高亮的行的下标
     * @return 指定行到首行的偏移量
     */
    private float computeOffsetYByIndex(int index, int highlightIndex) {
        float offsetY;
        if (index == 0) {
            offsetY = 0;
        } else if (index < highlightIndex) {
            int num = getLineNum(index);
            offsetY = (mTextHeight + mLineDistance) * num;
        } else if (index == highlightIndex) {
            int num = getLineNum(Math.max(0, index - 1));
            offsetY = (mTextHeight + mLineDistance) * num
                    + (mHighlightTextHeight / 2 + mTextHeight / 2 + mLineDistance);
        } else {
            offsetY = (mTextHeight + mLineDistance) * Math.max(0, index - 1)
                    + (mHighlightTextHeight + mLineDistance);
        }
        return offsetY;
    }

    /**
     * 指定歌词行到首行之间间隔了多少歌词行
     *
     * @param index 指定的歌词行
     * @return 间隔的歌词行数
     */
    private int getLineNum(int index) {
        int num = 0;
        for (int i = 0; i < index; i++) {
            num += mLineGroups.get(i).getLines().length;
        }
        return num;
    }

    private void performFling() {
        if (mState != State.FLINGING) {
            return;
        }

        if (mFlingVelocity == 0) {
            updateState(State.STAY);
            mFlingTimeMills = 0;
            return;
        }

        if (mFlingTimeMills == 0) {
            mFlingTimeMills = System.currentTimeMillis();
        } else {
            long currentTimeMillis = System.currentTimeMillis();
            long timeMills = currentTimeMillis - mFlingTimeMills;
            float accelerate = mFlingVelocity > 0 ? -mFlingAccelerate : mFlingAccelerate;
            float velocity;
            if (mFlingVelocity > 0) {
                velocity = Math.max(0, mFlingVelocity + accelerate * timeMills);
            } else {
                velocity = Math.min(0, mFlingVelocity + accelerate * timeMills);
            }

            // s = vt+0.5*a*t^2
            float distance = (velocity * velocity - mFlingVelocity * mFlingVelocity) / (2 * accelerate);

            mViewToLyric += distance;
            mViewToLyric = limit(mViewToLyric, mFlingMinOffsetY, mFlingMaxOffsetY);

            if (mViewToLyric == mFlingMaxOffsetY || mViewToLyric == mFlingMinOffsetY) {
                mFlingVelocity = 0;
                mFlingTimeMills = 0;
            } else {
                mFlingVelocity = velocity;
                mFlingTimeMills = currentTimeMillis;
            }
        }

        invalidate();

        sendMessage(InternalHandler.FLING, mFlingTimeInterval);
    }

    private static float calcInterValue(float from, float to, float ratio) {
        return (1 - ratio) * from + ratio * to;
    }

    private float limit(float value, float min, float max) {
        if (min > max) {
            throw new IllegalArgumentException();
        }
        return Math.min(Math.max(min, value), max);
    }

    private void updateState(State state) {
        mState = state;
        switch (mState) {
            case IDLE:
//                if (mCurrentMills != 0) {
//                    setTime(mCurrentMills);
//                    mCurrentMills = 0;
//                }
                break;

            case STAY:
                mHandler.removeMessages(InternalHandler.STOP_OVER);
                sendMessage(InternalHandler.STOP_OVER, mStopTime);
                break;

            default:
        }
    }

    private void sendMessage(int what, int delayMills) {
        Message message = mHandler.obtainMessage(what);
        message.obj = new WeakReference<>(this);
        mHandler.sendMessageDelayed(message, delayMills);
    }

    private void onStopOver() {
        if (mState != State.STAY) {
            return;
        }
        float offsetY = mHighlightOffset - computeOffsetYByIndex(mCurrentGroupIndex, mCurrentGroupIndex);
        if (mViewToLyric == offsetY || !mAutoScrollBack) {
            updateState(State.IDLE);
            return;
        }
//        mScrollOffsetYFrom = mViewToLyric;
//        mScrollOffsetYTo = offsetY;
//        mScrollVelocity = Math.abs(mScrollOffsetYTo - mScrollOffsetYFrom) / mScrollTimeMax;
//        updateState(State.SCROLLING);
//        performScroll();
    }

    private List<Line> splitTooLongLineToLines(Line line, Paint paint, int limit) {
        List<Line> lines = new ArrayList<>();

        String content = line.getContent();
        long startMills = line.getStartMills();
        long endMills = line.getEndMills();

        while (paint.measureText(content) > limit) {
            int len = getLen(content, paint, limit);
            String newLineContent = content.substring(0, len);
            long end = (endMills - startMills) * newLineContent.length() / content.length() + startMills;
            Line newLine = new Line(startMills, end, newLineContent);
            lines.add(newLine);

            content = content.substring(len);
            startMills = end + 1;
        }

        lines.add(new Line(startMills, endMills, content));

        return lines;
    }

    private int getLen(String content, Paint paint, int limit) {
        int start = 1;
        int end = content.length();
        while (true) {
            int target = (start + end) >> 1;
            float len1 = paint.measureText(content.substring(0, target));
            float len2 = paint.measureText(content.substring(0, target + 1));
            if (len1 > limit) {
                end = target - 1;
            } else if (len2 <= limit) {
                start = target + 1;
            } else {
                return target;
            }
        }
    }

    private void reset() {
        mState = State.IDLE;
        mCurrentTimeMills = 0;
        mFlingVelocity = 0;
        mLastGroupIndex = INDEX_NONE;
        mCurrentGroupIndex = 0;
        mScrollVelocity = 0;
        mFlingTimeMills = 0;
        mFlingMinOffsetY = 0;
        mFlingMaxOffsetY = 0;
        mTouchMinOffsetY = 0;
        mTouchMaxOffsetY = 0;
        mTranslationY = 0;

        mHandler.removeMessages(InternalHandler.STOP_OVER);
        mHandler.removeMessages(InternalHandler.SCROLL);
        mHandler.removeMessages(InternalHandler.FLING);
    }

    public float getHighlightTextHeight() {
        return mHighlightTextHeight;
    }

    public float getTextHeight() {
        return mTextHeight;
    }

    public float getLineSpacing() {
        return mLineDistance;
    }

    private static class SavedState extends BaseSavedState {

        private int mTextColor;
        private float mTextSize;

        private int mHighlightTextColor;
        private float mHighlightTextSize;

        private boolean mKaraokeEnable;
        private int mKaraokeTextColor;

        private float mFlingAccelerate;
        private int mFlingTimeInterval;

        private float mLineSpacing;

        private int mScrollTimeInterval;
        private int mScrollTimeMax;

        private boolean mAutoScrollBack;
        private int mStopTime;

        private List<Line> mLines;
        private long mCurrentTimeMills;

        SavedState(Parcelable superState) {
            super(superState);
        }

        SavedState(Parcel in) {
            super(in);
            this.mTextColor = in.readInt();
            this.mTextSize = in.readFloat();
            this.mHighlightTextColor = in.readInt();
            this.mHighlightTextSize = in.readFloat();
            this.mKaraokeEnable = in.readByte() != 0;
            this.mKaraokeTextColor = in.readInt();
            this.mFlingAccelerate = in.readFloat();
            this.mFlingTimeInterval = in.readInt();
            this.mLineSpacing = in.readFloat();
            this.mScrollTimeInterval = in.readInt();
            this.mScrollTimeMax = in.readInt();
            this.mAutoScrollBack = in.readByte() != 0;
            this.mStopTime = in.readInt();
            this.mLines = in.createTypedArrayList(Line.CREATOR);
            this.mCurrentTimeMills = in.readLong();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(this.mTextColor);
            dest.writeFloat(this.mTextSize);
            dest.writeInt(this.mHighlightTextColor);
            dest.writeFloat(this.mHighlightTextSize);
            dest.writeByte(this.mKaraokeEnable ? (byte) 1 : (byte) 0);
            dest.writeInt(this.mKaraokeTextColor);
            dest.writeFloat(this.mFlingAccelerate);
            dest.writeInt(this.mFlingTimeInterval);
            dest.writeFloat(this.mLineSpacing);
            dest.writeInt(this.mScrollTimeInterval);
            dest.writeInt(this.mScrollTimeMax);
            dest.writeByte(this.mAutoScrollBack ? (byte) 1 : (byte) 0);
            dest.writeInt(this.mStopTime);
            dest.writeTypedList(this.mLines);
            dest.writeLong(this.mCurrentTimeMills);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel source) {
                return new SavedState(source);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    private static class InternalHandler extends Handler {
        private static final int STOP_OVER = 100;
        private static final int SCROLL = 101;
        private static final int FLING = 102;

        @Override
        public void handleMessage(Message msg) {
            LyricView view;
            switch (msg.what) {
                case STOP_OVER:
                    view = getLyricView(msg);
                    if (view != null) {
                        view.onStopOver();
                    }
                    break;

                case SCROLL:
                    view = getLyricView(msg);
                    if (view != null) {
                        view.performScroll();
                    }
                    break;

                case FLING:
                    view = getLyricView(msg);
                    if (view != null) {
                        view.performFling();
                    }
                    break;

                default:
            }
        }

        private LyricView getLyricView(Message msg) {
            if (!(msg.obj instanceof WeakReference)) {
                return null;
            }

            WeakReference reference = (WeakReference) msg.obj;
            Object o = reference.get();
            if (!(o instanceof LyricView)) {
                return null;
            }

            return (LyricView) o;
        }
    }

    private enum State {
        /**
         * 空闲状态
         */
        IDLE,

        /**
         * 滚动并缩放文字
         */
        SCROLLING_WITH_SCALE,

        /**
         * 滚动
         */
        SCROLLING,

        /**
         * 用户触摸中
         */
        TOUCHING,

        /**
         * 惯性滑动
         */
        FLINGING,

        /**
         * 停留状态，用户滑动控件后的一段时间内，控件进入此状态，保持控件的偏移位置不变
         */
        STAY,
    }
}
