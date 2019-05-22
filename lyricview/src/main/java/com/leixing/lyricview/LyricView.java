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
import java.util.List;

/**
 * description : 歌词显示控件
 *
 * @author : leixing
 * email : leixing1012@qq.com
 * @date : 2018/11/10 20:04
 * <p>
 * todo 测试嵌套滑动，是否有滑动冲突
 * todo 绘制区域界定
 * todo 支持padding，统一坐标系，使用绘制区原点
 * todo 单字karaoke模式支持
 * todo 超出边界时拖动时带有弹性效果，并且支持自定义插值器
 * todo 动态更改属性
 * todo 细化歌词行自定义设计
 * todo saveInstanceState
 * todo line中绑定相对位置信息，onDraw中直接根据数据渲染，减少计算量，歌词组切换时只需少量数据变更
 * todo 细化绘制过程，对歌词行进行过滤，剪切边界歌词行
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
    private static final boolean KARAOKE_ENABLE_DEFAULT = false;

    private static final float LINE_SPACING_DEFAULT = 10.0f;
    private static final float BREAK_LINE_SPACING_DEFAULT = 10.0f;
    private static final int SCROLL_TIME_INTERVAL_DEFAULT = 20;
    private static final int FLING_TIME_INTERVAL_DEFAULT = 20;

    private static final int SCROLL_TIME_MAX_DEFAULT = 300;
    private static final float FLING_ACCELERATE_DEFAULT = 0.02f;

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
    private long mFlingTimeInterval;

    private float mGroupLineDistance;
    private float mBreakLineDistance;

    private long mScrollTimeInterval;

    private long mMinScrollTimeMills = 300;
    private long mMaxScrollTimeMills;

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
     * 歌词在没有滑动时控件顶部到第0行歌词顶部的偏移量，向下为正
     * 取值由歌词数据、当前播放的时间和高亮歌词显示的位置决定
     * 与{@link #mTranslationY}共同决定歌词绘制的位置
     */
    private float mLyricTop;

    /**
     * 歌词在Y方向上的偏移量，向下为正
     * 取值由当前的滑动、滚动状态决定
     * 与{@link #mLyricTop}共同决定歌词绘制的位置
     */
    private float mTranslationY;

    /**
     * 歌词绘制的位置，即歌词第0行顶部在控件中的位置
     * 由{@link #mLyricTop}和{@link #mTranslationY}共同决定
     * 取值为： {@link #mLyricTop} + {@link #mTranslationY};
     */
    private float mTranslatedLyricTop;

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
     * 在绘制区域的歌词行组
     */
    private List<LineGroup> mDrawLineGroups = new ArrayList<>();

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

    /**
     * 高亮行的偏移量，与{@link #mHighlightGravity}共同决定高亮行所在的位置
     */
    private int mHighlightOffset;

    /**
     * 高量行的位置，取值为
     * {@link #HIGHLIGHT_GRAVITY_CENTER_VERTICAL} 高亮行位于控件中心
     * {@link #HIGHLIGHT_GRAVITY_TOP} 高亮行位于控件顶部
     * {@link #HIGHLIGHT_GRAVITY_BOTTOM} 高亮行位于控件底部
     */
    private int mHighlightGravity;

    private float mTouchStartY;

    private float mTextHeight;
    /**
     * 普通行的画笔
     */
    private TextPaint mTextPaint;
    /**
     * 高亮行的画笔
     */
    private TextPaint mHighlightTextPaint;
    /**
     * 上一行在逐渐缩小时的画笔
     */
    private TextPaint mZoomOutPaint;
    /**
     * 当前行逐渐放大过程中的画笔
     */
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
    private long mFlingStartTimeMills;

    private float mFlingMinOffsetY;
    private float mFlingMaxOffsetY;

    private float mTouchMinOffsetY;
    private float mTouchMaxOffsetY;

    private float mHighlightGroupCenter;

    // listeners

    private TouchListener mTouchListener;
    private LineDesigner mLineDesigner;
    private Layout.Alignment mAlignment;
    private float mLyricTopToHighlightGroupCenter;
    /**
     * 一次滑动的总距离
     */
    private float mScrollingTranslationY;

    /**
     * 歌词首行中线到尾行中线的距离
     */
    private float mLyricFirstLineToLastLineDistance;
    private float mScrollFrom;


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
        mHighlightGroupCenter = highlightMarginTop;
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
        mMaxScrollTimeMills = scrollTimeMax;
    }

    public void setScrollTimeInterval(long scrollTimeInterval) {
        mScrollTimeInterval = scrollTimeInterval;
    }

    public void setFlingTimeInterval(long flingTimeInterval) {
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
            updateTranslationY(0);
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
        if (mGroupLineDistance == lineSpacing) {
            return;
        }
        mGroupLineDistance = lineSpacing;
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

        int index = Util.getGroupIndexByTimeMills(mLineGroups, mCurrentTimeMills);
        if (index != mCurrentGroupIndex) {
            mLastGroupIndex = mCurrentGroupIndex;
            mCurrentGroupIndex = index;
            calcOffset();
            mTranslationY = mTranslatedLyricTop - mLyricTop;
        }

        switch (mState) {
            case IDLE:
                // 第0行到控件顶部的距离
                if (mTranslationY == 0) {
                    invalidate();
                    break;
                }

                updateState(State.SCROLLING_WITH_SCALE);
                startScroll();
                break;

            case STAY:
            case TOUCHING:
            case FLINGING:
            case SCROLLING:
            case SCROLLING_WITH_SCALE:
                invalidate();
                break;

            default:
                break;
        }
    }

    private void calcOffset() {
        mHighlightGroupCenter = getHighlightGroupCenter();
        mLyricTopToHighlightGroupCenter = getLyricTopToHighlightGroupCenter();
        mLyricTop = mHighlightGroupCenter - mLyricTopToHighlightGroupCenter;
    }

    /**
     * 获取从歌词顶部到高量行中线的距离
     *
     * @return 距离值
     */
    private float getLyricTopToHighlightGroupCenter() {
        int breakNum = 0;
        for (int i = 0; i < mCurrentGroupIndex; i++) {
            breakNum += Math.max(0, mLineGroups.get(i).getLines().length - 1);
        }
        int groupSize = getGroupSize(mCurrentGroupIndex);
        float groupHeight = getGroupHeight(mCurrentGroupIndex == 0 ? mHighlightTextHeight : mTextHeight, groupSize);

        return mCurrentGroupIndex * mGroupLineDistance + breakNum * mBreakLineDistance + groupHeight * 0.5f;
    }

    /**
     * 计算没有偏移的情况下，控件顶部到高亮行中线的距离
     *
     * @return 距离值
     */
    private float getHighlightGroupCenter() {
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

    public void setLineDesigner(LineDesigner lineDesigner) {
        mLineDesigner = lineDesigner;
    }

    public void setAutoScrollBack(boolean autoScrollBack) {
        mAutoScrollBack = autoScrollBack;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mLineGroups.isEmpty()) {
            return;
        }
        float offset = mTranslatedLyricTop;
        int breakLineNum = 0;
        int lineNum = 0;
        LineGroup currentGroup = mLineGroups.get(mCurrentGroupIndex);

        for (int i = 0, size = mLineGroups.size(); i < size; i++) {
            LineGroup group = mLineGroups.get(i);
            float groupTop = offset;
            float groupHeight = getGroupHeight(i == mCurrentGroupIndex ? mHighlightTextHeight : mTextHeight, group.getLines().length);
            float groupBottom = groupTop + groupHeight;
            if (groupTop < mDrawRegionBottom && groupBottom > mDrawRegionTop) {
                if (i == mCurrentGroupIndex) {
                    Paint paint = hasScaleAnimation() ? mZoomInPaint : mHighlightTextPaint;
                    drawHighlightGroup(canvas, paint, group, offset);
                } else if (i == mLastGroupIndex && hasScaleAnimation()) {
                    drawGroup(canvas, mZoomOutPaint, mTextFontMetrics.ascent, group, offset);
                } else {
                    if (mLineDesigner != null) {
                        float lineCenterOffsetY = offset + groupHeight * 0.5f - mDrawRegionTop;
                        float highlightOffset = mHighlightGroupCenter - mDrawRegionTop;
                        float mHighlightHeight = getGroupHeight(mHighlightTextHeight, currentGroup.getLines().length);
                        mLineDesigner.designLine(mTextPaint, lineCenterOffsetY, highlightOffset, mDrawRegionHeight);
                    }
                    drawGroup(canvas, mTextPaint, mTextFontMetrics.ascent, group, offset);
                }
            }
            if (groupTop >= mDrawRegionBottom) {
                break;
            }

            breakLineNum += Math.max(0, group.getLines().length - 1);
            lineNum++;

            // 避免float累加误差
            offset = mTranslatedLyricTop + breakLineNum * mBreakLineDistance + lineNum * mGroupLineDistance;
        }
    }

    private void drawHighlightGroup(Canvas canvas, Paint paint, LineGroup group, float offset) {
        Line[] lines = group.getLines();
        if (mCurrentGroupIndex != 0) {
            offset -= (mHighlightTextHeight - mTextHeight) * 0.5;
        }

        int index = Util.getLineIndexByTimeMills(lines, mCurrentTimeMills);
        Line line;
        String content;
        float ascent;
        float x;
        float y;

        if (mKaraokeEnable) {
            // 绘制已经播放过的行
            for (int i = 0; i < index; i++) {
                line = lines[i];
                content = line.getContent();
                ascent = mHighlightFontMetrics.ascent;
                x = mDrawRegionLeft + (mDrawRegionWidth - paint.measureText(content)) * 0.5f;
                paint.setColor(mKaraokeTextColor);
                y = offset - ascent;
                canvas.drawText(content, x, y, paint);
                paint.setColor(mHighlightTextColor);
                offset += mBreakLineDistance;
            }

            // 绘制正在播放的行
            line = lines[index];
            content = line.getContent();
            ascent = mHighlightFontMetrics.ascent;
            float textWidth = paint.measureText(content);
            x = mDrawRegionLeft + (mDrawRegionWidth - textWidth) * 0.5f;
            y = offset - ascent;
            drawKaraoke(canvas, paint, mKaraokeTextColor, mHighlightTextColor, textWidth, x, y, line.getContent(), line.getStartMills(), line.getEndMills());
            paint.setColor(mHighlightTextColor);
            offset += mBreakLineDistance;

            // 绘制未播放的行
            for (int i = index + 1, size = lines.length; i < size; i++) {
                line = lines[i];
                content = line.getContent();
                ascent = mHighlightFontMetrics.ascent;
                x = mDrawRegionLeft + (mDrawRegionWidth - paint.measureText(content)) * 0.5f;
                y = offset - ascent;
                canvas.drawText(content, x, y, paint);
                offset += mBreakLineDistance;
            }
        } else {
            for (int i = 0, size = lines.length; i < size; i++) {
                line = lines[i];
                content = line.getContent();
                ascent = mHighlightFontMetrics.ascent;
                x = mDrawRegionLeft + (mDrawRegionWidth - paint.measureText(content)) * 0.5f;
                y = offset - ascent;
                canvas.drawText(content, x, y, paint);
                offset += mBreakLineDistance;
            }
        }
    }

    private void drawGroup(Canvas canvas, Paint paint, float ascent, LineGroup group, float offset) {
        for (Line line : group.getLines()) {
            String content = line.getContent();
            float x = mDrawRegionLeft + (mDrawRegionWidth - paint.measureText(content)) * 0.5f;
            float y = offset - ascent;
            canvas.drawText(content, x, y, paint);
            offset += mBreakLineDistance;
        }
    }

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
                float distance = y - mTouchStartY;
                mTouchStartY = y;
                distance = getTouchScrollDeltaY(mLyricTop, mTranslationY, distance, mTouchMinOffsetY, mTouchMaxOffsetY);
                updateTranslationY(mTranslationY + distance);
                invalidate();
                if (mTouchListener != null) {
                    long timeMills = getTimeMillsByOffsetY(mLyricTop);
                    mTouchListener.onTouchMoving(timeMills);
                }
                return true;

            case MotionEvent.ACTION_UP:
                mVelocityTracker.computeCurrentVelocity(1);
                mFlingVelocity = mVelocityTracker.getYVelocity();
                mVelocityTracker.recycle();
                mVelocityTracker = null;

                int minFlingVelocity = ViewConfiguration.get(getContext()).getScaledMinimumFlingVelocity() / 1000;

                Log.i(TAG, "MotionEvent.ACTION_UP: "
                        + "\nmFlingVelocity: " + mFlingVelocity
                        + "\nminFlingVelocity: " + minFlingVelocity);

                if (Math.abs(mFlingVelocity) > minFlingVelocity) {
                    startFling();
                } else {
                    mFlingVelocity = 0;
                    updateState(State.STAY);
                }

                if (mTouchListener != null) {
                    long timeMills = getTimeMillsByOffsetY(mLyricTop);
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

    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        SavedState savedState = new SavedState(super.onSaveInstanceState());
        savedState.mLines = mLines;
        savedState.mCurrentTimeMills = mCurrentTimeMills;
        savedState.mLineSpacing = mGroupLineDistance;
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
        mGroupLineDistance = savedState.mLineSpacing;
        mKaraokeEnable = savedState.mKaraokeEnable;
        mKaraokeTextColor = savedState.mKaraokeTextColor;
        mHighlightTextColor = savedState.mHighlightTextColor;
        mTextColor = savedState.mTextColor;
        mTextSize = savedState.mTextSize;
        mHighlightTextSize = savedState.mHighlightTextSize;
        mFlingAccelerate = savedState.mFlingAccelerate;
    }

    private void updateLinesVariables() {
        splitLongLineToLineGroup();

        mLyricFirstLineToLastLineDistance = calcLyricFirstLineToLastLineDistance();
        calcOffset();

        mFlingMinOffsetY = mHighlightGroupCenter - mLyricFirstLineToLastLineDistance - mHighlightTextHeight;
        mFlingMaxOffsetY = mHighlightGroupCenter;

        mTouchMinOffsetY = mHighlightGroupCenter - mLyricFirstLineToLastLineDistance - mHighlightTextHeight;
        mTouchMaxOffsetY = mHighlightGroupCenter;
    }

    /**
     * 计算歌词首行中线到尾行中线的距离
     *
     * @return 距离
     */
    private float calcLyricFirstLineToLastLineDistance() {
        int breakNum = 0;
        int size = mLineGroups.size();
        for (int i = 0; i < size; i++) {
            breakNum += Math.max(0, mLineGroups.get(i).getLines().length - 1);
        }

        return Math.max(0, size - 1) * mGroupLineDistance + breakNum * mBreakLineDistance;
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

    private void startScroll() {
        Line line = mLines.get(mCurrentGroupIndex);
        long lineMills = line.getEndMills() - mCurrentTimeMills;
        if (lineMills < mMinScrollTimeMills) {
            updateTranslationY(0);
            updateState(State.IDLE);
            invalidate();
            return;
        }

        mScrollFrom = mTranslationY;
        mScrollingTranslationY = mTranslationY;
        long scrollTime = Util.limit(lineMills, mMinScrollTimeMills, mMaxScrollTimeMills);
        mScrollVelocity = mScrollFrom / scrollTime;

        performScroll();
    }

    private void performScroll() {
        if (mState != State.SCROLLING_WITH_SCALE && mState != State.SCROLLING) {
            return;
        }

        float distance = mScrollVelocity * mScrollTimeInterval;
        mScrollingTranslationY = Util.approach(mScrollingTranslationY, 0, Math.abs(distance));
        updateTranslationY(mScrollingTranslationY);

        if (hasScaleAnimation()) {
            float ratio = 1 - (mScrollingTranslationY / mScrollFrom);

            float zoomOutTextSize = Util.calcInterValue(mHighlightTextSize, mTextSize, ratio);
            int zoomOutTextColor = Util.evaluateInt(mKaraokeEnable ? mKaraokeTextColor : mHighlightTextColor, mTextColor, ratio);
            mZoomOutPaint.setTextSize(zoomOutTextSize);
            mZoomOutPaint.setColor(zoomOutTextColor);

            float zoomInTextSize = Util.calcInterValue(mTextSize, mHighlightTextSize, ratio);
            int zoomInTextColor = Util.evaluateInt(mTextColor, mHighlightTextColor, ratio);
            mZoomInPaint.setTextSize(zoomInTextSize);
            mZoomInPaint.setColor(zoomInTextColor);
        }

        invalidate();
        if (mScrollingTranslationY == 0) {
            updateState(State.IDLE);
            return;
        }

        sendMessage(InternalHandler.SCROLL, mScrollTimeInterval);
    }

    private void updateTranslationY(float translationY) {
        mTranslationY = translationY;
        mTranslatedLyricTop = mLyricTop + mTranslationY;
    }

    private boolean hasScaleAnimation() {
        return mState == State.SCROLLING_WITH_SCALE;
    }

    private long getTimeMillsByOffsetY(float offsetY) {
        int index = (int) ((mHighlightOffset - offsetY) / (mTextHeight + mGroupLineDistance) + 0.5f);
        if (index < 0) {
            index = 0;
        } else if (index >= mLines.size()) {
            index = mLines.size() - 1;
        }
        return mLines.get(index).getStartMills();
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

    private void startFling() {
        updateState(State.FLINGING);
        mFlingStartTimeMills = System.currentTimeMillis();

        sendMessage(InternalHandler.FLING, mFlingTimeInterval);
    }

    private void performFling() {
        if (mState != State.FLINGING) {
            return;
        }

        if (mFlingVelocity == 0) {
            updateState(State.STAY);
            mFlingStartTimeMills = 0;
            return;
        }

        float accelerate = mFlingVelocity > 0 ? -mFlingAccelerate : mFlingAccelerate;
        long currentTimeMillis = System.currentTimeMillis();
        long timeMills = currentTimeMillis - mFlingStartTimeMills;
        float velocity;
        if (mFlingVelocity > 0) {
            velocity = Math.max(0, mFlingVelocity + accelerate * timeMills);
        } else {
            velocity = Math.min(0, mFlingVelocity + accelerate * timeMills);
        }

        // s = (vt^2-v0^2)/2a
        float distance = (velocity * velocity - mFlingVelocity * mFlingVelocity) / (2 * accelerate);

        Log.i(TAG, "performFling"
                + "\nmLyricTop: " + mLyricTop
                + "\nmTranslationY: " + mTranslationY
                + "\ndistance: " + distance
                + "\nmFlingMinOffsetY: " + mFlingMinOffsetY
                + "\nmFlingMaxOffsetY: " + mFlingMaxOffsetY);
        distance = getFlingDistance(mLyricTop, mTranslationY, distance, mFlingMinOffsetY, mFlingMaxOffsetY);
        Log.i(TAG, "getFlingDistance: " + distance);
        updateTranslationY(mTranslationY + distance);

        invalidate();

        if (mTranslatedLyricTop <= mFlingMinOffsetY || mTranslatedLyricTop >= mFlingMaxOffsetY) {
            mFlingVelocity = 0;
            mFlingStartTimeMills = 0;
            updateState(State.STAY);
        } else {
            mFlingVelocity = velocity;
            mFlingStartTimeMills = currentTimeMillis;
            sendMessage(InternalHandler.FLING, mFlingTimeInterval);
        }
    }

    private float getTouchScrollDeltaY(float lyricTop, float translationY, float distance, float minOffsetY, float maxOffsetY) {
        float target = Util.limit(lyricTop + translationY + distance, minOffsetY, maxOffsetY);
        return target - lyricTop - translationY;
    }

    private float getFlingDistance(float lyricTop, float translationY, float distance, float minOffsetY, float maxOffsetY) {
        float target = Util.limit(lyricTop + translationY + distance, minOffsetY, maxOffsetY);
        return target - lyricTop - translationY;
    }

    private float limit(float value, float min, float max) {
        if (min > max) {
            throw new IllegalArgumentException();
        }
        return Math.min(Math.max(min, value), max);
    }

    private void updateState(State state) {
        Log.i(TAG, "state: " + mState + "==>" + state);
        mState = state;
        switch (mState) {
            case STAY:
                mHandler.removeMessages(InternalHandler.STOP_OVER);
                sendMessage(InternalHandler.STOP_OVER, mStopTime);
                break;
            case IDLE:
            default:
        }
    }

    private void sendMessage(int what, long delayMills) {
        Message message = mHandler.obtainMessage(what);
        message.obj = new WeakReference<>(this);
        mHandler.sendMessageDelayed(message, delayMills);
    }

    private void onStopOver() {
        if (mState != State.STAY) {
            return;
        }

        if (mTranslationY == 0 || !mAutoScrollBack) {
            updateState(State.IDLE);
            return;
        }

        updateState(State.SCROLLING);
        startScroll();
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
        mFlingStartTimeMills = 0;

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
        return mGroupLineDistance;
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
