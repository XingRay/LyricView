package com.leixing.lyricview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
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
    private static final int SCROLL_TIME_INTERVAL_DEFAULT = 20;
    private static final int FLING_TIME_INTERVAL_DEFAULT = 20;

    private static final int SCROLL_TIME_MAX_DEFAULT = 300;
    private static final float FLING_ACCELERATE_DEFAULT = 0.005f;

    private static final int STOP_TIME_DEFAULT = 2000;
    private static final boolean AUTO_SCROLL_BACK_DEFAULT = true;


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

    private float mLineSpacing;

    private int mScrollTimeInterval;
    private int mScrollTimeMax;

    private boolean mAutoScrollBack;
    private int mStopTime;

    // saved state

    private List<Line> mLines = new ArrayList<>();
    private long mCurrentTimeMills = 0;

    // temp values

    /**
     * 第0行到控件顶部的距离
     */
    private float mCurrentOffsetY;
    private InternalHandler mHandler;
    private int mWidth;
    private int mHeight;
    private int mHalfHeight;
    private float mTouchStartY;
    private float mScrollOffsetYTo;
    private float mScrollOffsetYFrom;
    private float mTextHeight;
    private Paint mTextPaint;
    private Paint mHighlightTextPaint;
    private Paint mKaraokePaint;
    private Paint mZoomOutPaint;
    private Paint mZoomInPaint;
    private Paint mKaraokeZoomInPaint;
    private Paint.FontMetrics mTextFontMetrics;
    private Paint.FontMetrics mHighlightFontMetrics;
    private Bitmap mKaraokeBitmap;
    private Canvas mKaraokeCanvas;
    private Rect mSrc = new Rect();
    private RectF mDst = new RectF();
    private State mState = State.IDLE;
    private VelocityTracker mVelocityTracker;
    /**
     * pixel / mills
     */
    private float mFlingVelocity;
    private int mLastLineIndex = -1;
    private int mCurrentLineIndex = -1;
    private float mScrollVelocity;
    private float mHighlightTextHeight;
    private long mUpdateTimeMills;
    private long mFlingTimeMills;
    private float mFlingMinOffsetY;
    private float mFlingMaxOffsetY;
    private float mTouchMinOffsetY;
    private float mTouchMaxOffsetY;

    // listeners

    private TouchListener mTouchListener;
    private ColorDesigner mColorDesigner;


    public LyricView(Context context) {
        this(context, null);
    }

    public LyricView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LyricView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        applyAttributes(context, attrs);

        mTextPaint = new Paint();
        mTextPaint.setTextSize(mTextSize);
        mTextPaint.setColor(mTextColor);
        mTextPaint.setAntiAlias(true);
        mTextFontMetrics = mTextPaint.getFontMetrics();
        mTextHeight = mTextFontMetrics.descent - mTextFontMetrics.ascent;

        mHighlightTextPaint = new Paint();
        mHighlightTextPaint.setTextSize(mHighlightTextSize);
        mHighlightTextPaint.setColor(mHighlightTextColor);
        mHighlightTextPaint.setAntiAlias(true);
        mHighlightFontMetrics = mHighlightTextPaint.getFontMetrics();
        mHighlightTextHeight = mHighlightFontMetrics.descent - mHighlightFontMetrics.ascent;

        mKaraokePaint = new Paint();
        mKaraokePaint.setTextSize(mHighlightTextSize);
        mKaraokePaint.setColor(mKaraokeTextColor);
        mKaraokePaint.setAntiAlias(true);
        mKaraokePaint.setFakeBoldText(true);
        mSrc.bottom = (int) Math.ceil(mHighlightTextHeight);

        mZoomOutPaint = new Paint();
        mZoomOutPaint.setAntiAlias(true);

        mZoomInPaint = new Paint();
        mZoomInPaint.setAntiAlias(true);

        mKaraokeZoomInPaint = new Paint();
        mKaraokeZoomInPaint.setAntiAlias(true);
        mKaraokeZoomInPaint.setFakeBoldText(true);

        mKaraokeZoomInPaint.setColor(mKaraokeTextColor);
        mZoomInPaint.setColor(mHighlightTextColor);
        mZoomOutPaint.setColor(mTextColor);

        mHandler = new InternalHandler();
    }

    public void setLyric(List<Line> lines) {
        addLines(lines);
        reset();
        initKaraokeVariables();
        invalidate();
    }

    public void setTextSize(float textSize) {
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

        mKaraokePaint.setTextSize(mHighlightTextSize);

        mSrc.bottom = (int) Math.ceil(mHighlightTextHeight);
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
        mKaraokePaint.setColor(mKaraokeTextColor);
        mKaraokeZoomInPaint.setColor(mKaraokeTextColor);
        invalidate();
    }

    public void setKaraokeEnable(boolean enable) {
        if (mKaraokeEnable == enable) {
            return;
        }
        mKaraokeEnable = enable;
        invalidate();
    }

    public void setLineSpacing(float lineSpacing) {
        if (mLineSpacing == lineSpacing) {
            return;
        }
        mLineSpacing = lineSpacing;
        invalidate();
    }

    public void updateTimeMills(final long mills) {
        mCurrentTimeMills = mills;
        int index = getLineIndexByTimeMills(mills);
        if (index != mCurrentLineIndex) {
            mLastLineIndex = mCurrentLineIndex;
            mCurrentLineIndex = index;
        }
        // 第0行到控件顶部的距离
        float offsetY = mHalfHeight - computeOffsetYByIndex(mCurrentLineIndex, mCurrentLineIndex);

        switch (mState) {
            case IDLE:
                if (mCurrentOffsetY == offsetY) {
                    invalidate();
                    break;
                }

                mScrollOffsetYFrom = mCurrentOffsetY;
                mScrollOffsetYTo = offsetY;
                Line line = mLines.get(mCurrentLineIndex);
                long lineMills = line.endMills - mills;
                long scrollTime = Math.min(mScrollTimeMax, lineMills);
                mScrollVelocity = Math.abs(mScrollOffsetYTo - mScrollOffsetYFrom) / scrollTime;
                updateState(State.SCROLLING_WITH_SCALE);
                performScroll();
                break;

            case SCROLLING_WITH_SCALE:
                mUpdateTimeMills = mills;
                return;

            case STOP:
            case TOUCHING:
            case FLINGING:
            case SCROLLING:
                invalidate();
                mUpdateTimeMills = mills;
                break;

            default:
                break;
        }
    }

    public void setTimeMills(long mills) {
        mCurrentTimeMills = mills;
        int index = getLineIndexByTimeMills(mCurrentTimeMills);
        if (index != mCurrentLineIndex) {
            mLastLineIndex = mCurrentLineIndex;
            mCurrentLineIndex = index;
        }
        mCurrentOffsetY = mHalfHeight - computeOffsetYByIndex(mCurrentLineIndex, mCurrentLineIndex);
        Log.i(TAG, "updateTimeMills"
                + "\nmCurrentOffsetY:" + mCurrentOffsetY
                + "");
        invalidate();
    }

    public void setTouchListener(TouchListener listener) {
        mTouchListener = listener;
    }

    public void setColorDesigner(ColorDesigner colorDesigner) {
        mColorDesigner = colorDesigner;
    }

    public LyricView setAutoScrollBack(boolean autoScrollBack) {
        mAutoScrollBack = autoScrollBack;
        return this;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // TODO: 2018/11/21
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (int i = 0, size = mLines.size(); i < size; i++) {
            boolean isHighlight = i == mCurrentLineIndex;
            float textHeight = isHighlight ? mHighlightTextHeight : mTextHeight;
            float ascent = isHighlight ? mHighlightFontMetrics.ascent : mTextFontMetrics.ascent;
            float offsetY = mCurrentOffsetY + computeOffsetYByIndex(i, mCurrentLineIndex);
            float top = offsetY - textHeight / 2;
            float baseLine = top - ascent;
            float bottom = offsetY + textHeight / 2;
            float offsetYFromCenter = offsetY - mHalfHeight;

            if (top > mHeight || bottom < 0) {
                // out of bounds
                continue;
            }

            Line line = mLines.get(i);
            String content = line.content;
            long startMills = line.startMills;
            long endMills = line.endMills;

            if (hasScaleAnimation()) {
                if (i == mLastLineIndex) {
                    float x = ((int) (mWidth - mZoomOutPaint.measureText(content))) >> 1;
                    if (mColorDesigner != null) {
                        mZoomOutPaint.setColor(mColorDesigner.getColor(offsetYFromCenter, mHeight));
                    }
                    canvas.drawText(content, x, baseLine, mZoomOutPaint);
                    continue;
                } else if (i == mCurrentLineIndex) {
                    float x = ((int) (mWidth - mZoomInPaint.measureText(content))) >> 1;
                    if (mColorDesigner != null) {
                        mZoomInPaint.setColor(mColorDesigner.getColor(offsetYFromCenter, mHeight));
                    }
                    canvas.drawText(content, x, baseLine, mZoomInPaint);

                    if (mKaraokeEnable) {
                        drawKaraoke(canvas, mKaraokeZoomInPaint, x, baseLine, content, startMills, endMills);
                    }
                    continue;
                }
            }
            if (isHighlight) {
                float x = ((int) (mWidth - mHighlightTextPaint.measureText(content))) >> 1;
                if (mColorDesigner != null) {
                    mHighlightTextPaint.setColor(mColorDesigner.getColor(offsetYFromCenter, mHeight));
                }
                canvas.drawText(content, x, baseLine, mHighlightTextPaint);

                if (mKaraokeEnable) {
                    drawKaraoke(canvas, mKaraokePaint, x, baseLine, content, startMills, endMills);
                }
            } else {
                float x = ((int) (mWidth - mTextPaint.measureText(content))) >> 1;
                if (mColorDesigner != null) {
                    mTextPaint.setColor(mColorDesigner.getColor(offsetYFromCenter, mHeight));
                }
                canvas.drawText(content, x, baseLine, mTextPaint);
            }
        }
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
                float deltaY = y - mTouchStartY;
                mTouchStartY = y;

                mCurrentOffsetY = limit(mCurrentOffsetY + deltaY, mTouchMinOffsetY, mTouchMaxOffsetY);

                invalidate();
                if (mTouchListener != null) {
                    long timeMills = getTimeMillsByOffsetY(mCurrentOffsetY);
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
                    updateState(State.STOP);
                }
                if (mTouchListener != null) {
                    long timeMills = getTimeMillsByOffsetY(mCurrentOffsetY);
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
        super.onSizeChanged(w, h, oldw, oldh);
        Log.i(TAG, "w:" + w + " h:" + h + " oldw:" + oldw + " oldh:" + oldh);

        mWidth = w;
        mHeight = h;
        mHalfHeight = h >> 1;
        mCurrentOffsetY = mHalfHeight - computeOffsetYByTimeMills(mCurrentTimeMills);
    }

    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        Log.i(TAG, "onSaveInstanceState");

        SavedState savedState = new SavedState(super.onSaveInstanceState());
        savedState.mLines = mLines;
        savedState.mCurrentTimeMills = mCurrentTimeMills;
        savedState.mLineSpacing = mLineSpacing;
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
        mLineSpacing = savedState.mLineSpacing;
        mKaraokeEnable = savedState.mKaraokeEnable;
        mKaraokeTextColor = savedState.mKaraokeTextColor;
        mHighlightTextColor = savedState.mHighlightTextColor;
        mTextColor = savedState.mTextColor;
        mTextSize = savedState.mTextSize;
        mHighlightTextSize = savedState.mHighlightTextSize;
        mFlingAccelerate = savedState.mFlingAccelerate;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // TODO: 2018/11/27
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // TODO: 2018/11/27
    }

    private void applyAttributes(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.LyricView);
        if (typedArray == null) {
            return;
        }

        mTextColor = typedArray.getColor(R.styleable.LyricView_lyric_view_text_color, TEXT_COLOR_DEFAULT);
        mTextSize = typedArray.getDimension(R.styleable.LyricView_lyric_view_text_size, TEXT_SIZE_DEFAULT);

        mHighlightTextColor = typedArray.getColor(R.styleable.LyricView_lyric_view_highlight_text_color, HIGHLIGHT_TEXT_COLOR_DEFAULT);
        mHighlightTextSize = typedArray.getDimension(R.styleable.LyricView_lyric_view_highlight_text_size, HIGHLIGHT_TEXT_SIZE_DEFAULT);

        mKaraokeEnable = typedArray.getBoolean(R.styleable.LyricView_lyric_view_karaoke_enable, KARAOKE_ENABLE_DEFAULT);
        mKaraokeTextColor = typedArray.getColor(R.styleable.LyricView_lyric_view_karaoke_color, KARAOKE_TEXT_COLOR_DEFAULT);

        mLineSpacing = typedArray.getDimension(R.styleable.LyricView_lyric_view_line_spacing, LINE_SPACING_DEFAULT);

        mFlingAccelerate = typedArray.getDimension(R.styleable.LyricView_lyric_view_fling_accelerate, FLING_ACCELERATE_DEFAULT);
        mFlingTimeInterval = typedArray.getInt(R.styleable.LyricView_lyric_view_fling_time_interval, FLING_TIME_INTERVAL_DEFAULT);

        mScrollTimeInterval = typedArray.getInt(R.styleable.LyricView_lyric_view_scroll_time_interval, SCROLL_TIME_INTERVAL_DEFAULT);
        mScrollTimeMax = typedArray.getInt(R.styleable.LyricView_lyric_view_scroll_time_max, SCROLL_TIME_MAX_DEFAULT);

        mStopTime = typedArray.getInt(R.styleable.LyricView_lyric_view_stop_time, STOP_TIME_DEFAULT);
        mAutoScrollBack = typedArray.getBoolean(R.styleable.LyricView_lyric_view_auto_scroll_back, AUTO_SCROLL_BACK_DEFAULT);

        typedArray.recycle();
    }

    private void scrollToLine(int index, boolean smooth) {
        Line line = mLines.get(index);
        mCurrentTimeMills = line.startMills;
        float offsetY = mHalfHeight - computeOffsetYByIndex(index, index);
        if (mCurrentOffsetY == offsetY) {
            return;
        }
        if (smooth) {
            mScrollOffsetYTo = offsetY;
            performScroll();
        } else {
            mCurrentOffsetY = offsetY;
            invalidate();
        }
    }

    /**
     * 绘制卡拉OK效果
     */
    private void drawKaraoke(Canvas canvas, Paint paint, float x, float baseLine, String content, long startMills, long endMills) {
        float textWidth = paint.measureText(content);

        if (endMills - startMills == 0) {
            return;
        }
        int playedWidth = (int) (textWidth * (mCurrentTimeMills - startMills) / (endMills - startMills));
        if (playedWidth > textWidth) {
            playedWidth = (int) textWidth;
        }
        if (playedWidth <= 0) {
            return;
        }

        mKaraokeBitmap.eraseColor(0);
        mKaraokeCanvas.drawText(content, 0, -mHighlightFontMetrics.ascent, paint);

        mSrc.right = playedWidth;

        mDst.left = x;
        mDst.top = baseLine + mHighlightFontMetrics.ascent;
        mDst.right = x + playedWidth;
        mDst.bottom = baseLine + mHighlightFontMetrics.descent;

        canvas.drawBitmap(mKaraokeBitmap, mSrc, mDst, paint);
    }

    private int getLineIndexByTimeMills(long timeMills) {
        if (mLines.isEmpty()) {
            return 0;
        }
        if (timeMills <= mLines.get(0).startMills) {
            return 0;
        }
        if (timeMills >= mLines.get(mLines.size() - 1).endMills) {
            return mLines.size() - 1;
        }
        int index = Collections.binarySearch(mLines, new Line(timeMills, timeMills, ""), new Comparator<Line>() {
            @Override
            public int compare(Line o1, Line o2) {
                if (o2.startMills >= o1.startMills && o2.endMills <= o1.endMills) {
                    return 0;
                } else {
                    long x = o1.startMills;
                    long y = o2.startMills;
                    //noinspection UseCompareMethod
                    return (x < y) ? -1 : ((x == y) ? 0 : 1);
                }
            }
        });
        if (index < 0) {
            index = 0;
        }
        return index;
    }

    private void performScroll() {
        if (mState != State.SCROLLING_WITH_SCALE && mState != State.SCROLLING && mState != State.IDLE) {
            return;
        }

        float deltaY = mScrollOffsetYTo - mCurrentOffsetY;
        float distance = mScrollVelocity * mScrollTimeInterval;

        if (Math.abs(deltaY) > distance) {
            mCurrentOffsetY += deltaY > 0 ? distance : -distance;
        } else {
            mCurrentOffsetY = mScrollOffsetYTo;
        }

        if (hasScaleAnimation()) {
            float ratio = (mCurrentOffsetY - mScrollOffsetYFrom) / (mScrollOffsetYTo - mScrollOffsetYFrom);
            float zoomOutTextSize = calcInterValue(mHighlightTextSize, mTextSize, ratio);
            float zoomInTextSize = calcInterValue(mTextSize, mHighlightTextSize, ratio);

            mZoomOutPaint.setTextSize(zoomOutTextSize);
            mZoomInPaint.setTextSize(zoomInTextSize);
            mKaraokeZoomInPaint.setTextSize(zoomInTextSize);
        }

        invalidate();
        if (mCurrentOffsetY == mScrollOffsetYTo) {
            updateState(State.IDLE);
            return;
        }
        postDelayed(new Runnable() {
            @Override
            public void run() {
                performScroll();
            }
        }, mScrollTimeInterval);
    }

    private boolean hasScaleAnimation() {
        return mState == State.SCROLLING_WITH_SCALE;
    }

    private void initKaraokeVariables() {
        float maxWidth = 0;
        for (Line line : mLines) {
            float size = mHighlightTextPaint.measureText(line.content);
            if (size > maxWidth) {
                maxWidth = size;
            }
        }

        int width = (int) Math.ceil(maxWidth);
        int height = (int) Math.ceil(mHighlightTextHeight);
        mKaraokeBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mKaraokeCanvas = new Canvas(mKaraokeBitmap);
    }

    private long getTimeMillsByOffsetY(float offsetY) {
        int index = (int) ((mHalfHeight - offsetY) / (mTextHeight + mLineSpacing) + 0.5f);
        if (index < 0) {
            index = 0;
        } else if (index >= mLines.size()) {
            index = mLines.size() - 1;
        }
        return mLines.get(index).startMills;
    }

    private float computeOffsetYByTimeMills(long timeMills) {
        int index = getLineIndexByTimeMills(timeMills);
        return computeOffsetYByIndex(index, index);
    }

    private float computeOffsetYByIndex(int index, int highlightIndex) {
        float offsetY;
        if (index == 0) {
            offsetY = 0;
        } else if (index < highlightIndex) {
            offsetY = (mTextHeight + mLineSpacing) * index;
        } else if (index == highlightIndex) {
            offsetY = (mTextHeight + mLineSpacing) * Math.max(0, index - 1)
                    + (mHighlightTextHeight / 2 + mTextHeight / 2 + mLineSpacing);
        } else {
            offsetY = (mTextHeight + mLineSpacing) * Math.max(0, index - 1)
                    + (mHighlightTextHeight + mLineSpacing);
        }
        return offsetY;
    }

    private void performFling() {
        if (mFlingVelocity == 0) {
            updateState(State.STOP);
            Log.i(TAG, "performFling stop");
            mFlingTimeMills = 0;
            return;
        }
        if (mState != State.FLINGING) {
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

            // s = vt+0.5*at^2
            float distance = (velocity * velocity - mFlingVelocity * mFlingVelocity) / (2 * accelerate);

            mCurrentOffsetY += distance;
            mCurrentOffsetY = limit(mCurrentOffsetY, mFlingMinOffsetY, mFlingMaxOffsetY);

            if (mCurrentOffsetY == mFlingMaxOffsetY || mCurrentOffsetY == mFlingMinOffsetY) {
                mFlingVelocity = 0;
                mFlingTimeMills = 0;
            } else {
                mFlingVelocity = velocity;
                mFlingTimeMills = currentTimeMillis;
            }
        }

        postDelayed(new Runnable() {
            @Override
            public void run() {
                performFling();
                invalidate();
            }
        }, mFlingTimeInterval);
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
                if (mUpdateTimeMills != 0) {
                    updateTimeMills(mUpdateTimeMills);
                    mUpdateTimeMills = 0;
                }
                break;

            case STOP:
                mHandler.removeMessages(InternalHandler.STOP_OVER);
                mHandler.sendEmptyMessageDelayed(InternalHandler.STOP_OVER, mStopTime);
                Message message = Message.obtain();
                message.what = InternalHandler.STOP_OVER;
                message.obj = new WeakReference<>(this);
                mHandler.sendMessageDelayed(message, mStopTime);
                break;

            default:
        }
    }

    private void onStopOver() {
        if (mState != State.STOP) {
            return;
        }
        float offsetY = mHalfHeight - computeOffsetYByIndex(mCurrentLineIndex, mCurrentLineIndex);
        if (mCurrentOffsetY == offsetY || !mAutoScrollBack) {
            updateState(State.IDLE);
            return;
        }
        mScrollOffsetYFrom = mCurrentOffsetY;
        mScrollOffsetYTo = offsetY;
        mScrollVelocity = Math.abs(mScrollOffsetYTo - mScrollOffsetYFrom) / mScrollTimeMax;
        updateState(State.SCROLLING);
        performScroll();
    }

    private void addLines(List<Line> lines) {
        mLines.clear();
        if (lines == null || lines.isEmpty()) {
            return;
        }
        for (int i = 0, size = lines.size(); i < size; i++) {
            Line line = lines.get(i);
            String content = line.getContent();
            float lineWidth = mHighlightTextPaint.measureText(content);
            if (lineWidth <= mWidth) {
                mLines.add(line);
                continue;
            }
            mLines.addAll(splitTooLongLineToLines(line, mHighlightTextPaint, mWidth));
        }
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
        updateState(State.IDLE);

        mFlingMinOffsetY = mHalfHeight - computeOffsetYByIndex(mLines.size() - 1, 0);
        mFlingMaxOffsetY = mHalfHeight;

        mTouchMinOffsetY = mHalfHeight - computeOffsetYByIndex(mLines.size() + 3, 0);
        mTouchMaxOffsetY = mHalfHeight + computeOffsetYByIndex(3, 0);

        mCurrentOffsetY = mHalfHeight;
    }

    @SuppressWarnings("WeakerAccess")
    public static class Line implements Parcelable {
        private long startMills;
        private long endMills;
        private String content;

        public Line(long startMills, long endMills, String content) {
            this.startMills = startMills;
            this.endMills = endMills;
            this.content = content;
        }

        Line(Parcel in) {
            this.startMills = in.readLong();
            this.endMills = in.readLong();
            this.content = in.readString();
        }

        public long getStartMills() {
            return startMills;
        }

        public Line setStartMills(long startMills) {
            this.startMills = startMills;
            return this;
        }

        public long getEndMills() {
            return endMills;
        }

        public Line setEndMills(long endMills) {
            this.endMills = endMills;
            return this;
        }

        public String getContent() {
            return content;
        }

        public Line setContent(String content) {
            this.content = content;
            return this;
        }

        @SuppressWarnings("NullableProblems")
        @Override
        public String toString() {
            return "\"Line\": {"
                    + "\"startMills\": \"" + startMills
                    + ", \"endMills\": \"" + endMills
                    + ", \"content\": \"" + content + '\"'
                    + '}';
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(this.startMills);
            dest.writeLong(this.endMills);
            dest.writeString(this.content);
        }

        public static final Parcelable.Creator<Line> CREATOR = new Parcelable.Creator<Line>() {
            @Override
            public Line createFromParcel(Parcel source) {
                return new Line(source);
            }

            @Override
            public Line[] newArray(int size) {
                return new Line[size];
            }
        };
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

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case STOP_OVER:
                    if (msg.obj instanceof WeakReference) {
                        WeakReference reference = (WeakReference) msg.obj;
                        Object o = reference.get();
                        if (o instanceof LyricView) {
                            LyricView view = (LyricView) o;
                            view.onStopOver();
                        }
                    }
                    break;
                default:
            }
        }
    }

    public interface TouchListener {
        /**
         * 用户按下
         *
         * @param timeMills 处于控件中心的歌词的播放起始时间
         */
        void onTouchDown(long timeMills);

        /**
         * 触摸滑动中
         *
         * @param timeMills 处于控件中心的歌词的播放起始时间
         */
        void onTouchMoving(long timeMills);

        /**
         * 触摸结束
         *
         * @param timeMills 处于控件中心的歌词的播放起始时间
         */
        void onTouchUp(long timeMills);
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
         * 停留状态
         */
        STOP,
    }
}
