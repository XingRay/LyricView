package com.leixing.lyricview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;

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

    public static final float TEXT_SIZE = 40.0f;
    public static final float TEXT_SIZE_HIGHLIGHT = 80.0f;
    public static final float LINE_SPACING = 20.0f;

    public static final int COLOR_TEXT = 0xff000000;
    public static final int COLOR_HIGHLIGHT = 0xffff0000;
    public static final int COLOR_KARAOKE = 0xff0000ff;

    public static final int SCROLL_SLOT = 20;
    public static final int TIME_INTERVAL_SCROLL = 20;
    public static final int TIME_INTERVAL_FLING = 20;
    public static final int SCROLL_TIME = 300;


    // attributes

    private List<Line> mLines = new ArrayList<>();
    private long mCurrentTimeMills = 0;
    private float mLineSpacing = LINE_SPACING;
    private boolean misKaraokeEnable = true;
    private int mKaraokeColor = COLOR_KARAOKE;
    private int mHighlightColor = COLOR_HIGHLIGHT;
    private int mTextColor = COLOR_TEXT;
    private float mTextSize = TEXT_SIZE;
    private float mHighlightTextSize = TEXT_SIZE_HIGHLIGHT;


    // temp values

    private int mWidth;
    private int mHeight;
    private float mTouchStartY;
    private float mScrollToOffsetY;
    private float mScrollFromOffsetY;
    private float mCurrentOffsetY;
    private float mTextHeight;
    private Paint mTextPaint;
    private Paint mHighlightTextPaint;
    private Paint mKaraokePaint;
    private Paint mZoomInPaint;
    private Paint mZoomOutPaint;
    private Paint.FontMetrics mTextFontMetrics;
    private Bitmap mKaraokeBitmap;
    private Canvas mKaraokeCanvas;
    private Rect mSrc = new Rect();
    private RectF mDst = new RectF();
    private State mState = State.IDLE;
    private VelocityTracker mVelocityTracker;
    private float mFlingVelocity;
    private float mFlingAccelerate = -10;
    private int mLastLineIndex = -1;
    private int mCurrentLineIndex = -1;
    private float mScrollVelocity = 0.15f;

    // listeners

    private TouchListener mTouchListener;
    private Paint.FontMetrics mHighlightFontMetrics;
    private float mHighlightTextHeight;

    public LyricView(Context context) {
        this(context, null);
    }

    public LyricView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LyricView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mTextPaint = new Paint();
        mTextPaint.setTextSize(mTextSize);
        mTextPaint.setColor(mTextColor);
        mTextPaint.setAntiAlias(true);
        mTextFontMetrics = mTextPaint.getFontMetrics();
        mTextHeight = mTextFontMetrics.descent - mTextFontMetrics.ascent;

        mHighlightTextPaint = new Paint();
        mHighlightTextPaint.setTextSize(mHighlightTextSize);
        mHighlightTextPaint.setColor(mHighlightColor);
        mHighlightTextPaint.setAntiAlias(true);
        mHighlightFontMetrics = mHighlightTextPaint.getFontMetrics();
        mHighlightTextHeight = mHighlightFontMetrics.descent - mHighlightFontMetrics.ascent;

        mKaraokePaint = new Paint();
        mKaraokePaint.setTextSize(mHighlightTextSize);
        mKaraokePaint.setColor(mKaraokeColor);
        mKaraokePaint.setAntiAlias(true);
        mSrc.bottom = (int) Math.ceil(mHighlightTextHeight);

        mZoomInPaint = new Paint();
        mZoomOutPaint = new Paint();

//        LinearGradient highlightShader = new LinearGradient(0, 0, 0, mTextHeight,
//                new int[]{0xffffeaba, 0xffffffff},
//                null, Shader.TileMode.REPEAT);
//        mHighlightTextPaint.setShader(highlightShader);
//
//        LinearGradient karaokeShader = new LinearGradient(0, 0, 0, mTextHeight,
//                new int[]{0xffffffff, 0xff0000ff},
//                null, Shader.TileMode.REPEAT);
//        mKaraokePaint.setShader(karaokeShader);
    }

    public void setLyric(List<Line> lines) {
        mLines.clear();
        if (lines != null) {
            mLines.addAll(lines);
        }
        if (mLines.isEmpty()) {
            return;
        }
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
        if (mHighlightColor == color) {
            return;
        }
        mHighlightColor = color;
        mHighlightTextPaint.setColor(mHighlightColor);

        invalidate();
    }

    public void setKaraokeColor(int color) {
        if (mKaraokeColor == color) {
            return;
        }
        mKaraokeColor = color;
        mKaraokePaint.setColor(mKaraokeColor);
        invalidate();
    }

    public void setKaraokeEnable(boolean enable) {
        if (misKaraokeEnable == enable) {
            return;
        }
        misKaraokeEnable = enable;
        invalidate();
    }

    public void setLineSpacing(float lineSpacing) {
        if (mLineSpacing == lineSpacing) {
            return;
        }
        mLineSpacing = lineSpacing;
        invalidate();
    }

    public void updateTime(long mills, boolean smooth) {
        mCurrentTimeMills = mills;
        int index = mCurrentLineIndex;
        mCurrentLineIndex = getLineIndexByTimeMills(mCurrentTimeMills);
        if (index != mCurrentLineIndex) {
            mLastLineIndex = index;
        }
        float offsetY = mHeight / 2 - computeOffsetYByIndex(mCurrentLineIndex, mCurrentLineIndex);

        if (smooth) {
            if (mState == State.SCROLLING) {
                return;
            }
            mScrollFromOffsetY = mCurrentOffsetY;
            mScrollToOffsetY = offsetY;
            mScrollVelocity = Math.abs(mScrollToOffsetY - mScrollFromOffsetY) / SCROLL_TIME;
            performScroll();
        } else {
            mCurrentOffsetY = offsetY;
            Log.i(TAG, "updateTime"
                    + "\nmCurrentOffsetY:" + mCurrentOffsetY
                    + "\noffsetY:" + offsetY
                    + "");
            invalidate();
        }
    }

    public void scrollToLine(int index, boolean smooth) {
        Line line = mLines.get(index);
        mCurrentTimeMills = line.startMills;
        float offsetY = mHeight / 2 - computeOffsetYByIndex(index, index);
        if (mCurrentOffsetY == offsetY) {
            return;
        }
        if (smooth) {
            mScrollToOffsetY = offsetY;
            performScroll();
        } else {
            mCurrentOffsetY = offsetY;
            invalidate();
        }
    }

    public void setTouchListener(TouchListener listener) {
        mTouchListener = listener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // TODO: 2018/11/21
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Log.i(TAG, "onDraw "
                + "\nmLastLineIndex:" + mLastLineIndex
                + "\nmCurrentLineIndex:" + mCurrentLineIndex);

        for (int i = 0, size = mLines.size(); i < size; i++) {
            boolean isHighlight = i == mCurrentLineIndex;
            float textHeight = isHighlight ? mHighlightTextHeight : mTextHeight;
            float ascent = isHighlight ? mHighlightFontMetrics.ascent : mTextFontMetrics.ascent;
            float offsetY = mCurrentOffsetY + computeOffsetYByIndex(i, mCurrentLineIndex);
            float top = offsetY - textHeight / 2;
            float baseLine = top - ascent;
            float bottom = offsetY + textHeight / 2;

            if (top > mHeight || bottom < 0) {
                // out of bounds
                continue;
            }

            Line line = mLines.get(i);
            String content = line.content;
            long startMills = line.startMills;
            long endMills = line.endMills;

            if (mState == State.SCROLLING) {
                if (i == mLastLineIndex) {
                    float x = ((int) (mWidth - mZoomInPaint.measureText(content))) >> 1;
                    canvas.drawText(content, x, baseLine, mZoomInPaint);
                    continue;
                } else if (i == mCurrentLineIndex) {
                    float x = ((int) (mWidth - mZoomOutPaint.measureText(content))) >> 1;
                    canvas.drawText(content, x, baseLine, mZoomOutPaint);

                    if (misKaraokeEnable) {
                        drawKaraoke(canvas, mZoomOutPaint, x, baseLine, content, startMills, endMills);
                    }
                    continue;
                }
            }
            if (isHighlight) {
                float x = ((int) (mWidth - mHighlightTextPaint.measureText(content))) >> 1;
                canvas.drawText(content, x, baseLine, mHighlightTextPaint);

                if (misKaraokeEnable) {
                    drawKaraoke(canvas, mKaraokePaint, x, baseLine, content, startMills, endMills);
                }
            } else {
                float x = ((int) (mWidth - mTextPaint.measureText(content))) >> 1;
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
                if (mState != State.IDLE && mState != State.FLINGING) {
                    return false;
                }
                mState = State.TOUCHING;
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
                mCurrentOffsetY += y - mTouchStartY;
                mTouchStartY = y;
                invalidate();
                if (mTouchListener != null) {
                    long timeMills = getTimeMillsByOffsetY(mCurrentOffsetY);
                    mTouchListener.onTouchMoving(timeMills);
                }
                return true;

            case MotionEvent.ACTION_UP:
                mState = State.IDLE;
                mVelocityTracker.computeCurrentVelocity(1000);
                mFlingVelocity = mVelocityTracker.getYVelocity();
                int minFlingVelocity = ViewConfiguration.get(getContext()).getScaledMinimumFlingVelocity();
                if (Math.abs(mFlingVelocity) > minFlingVelocity) {
                    mState = State.FLINGING;
                    performFling();
                } else {
                    mFlingVelocity = 0;
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
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
        mCurrentOffsetY = mHeight / 2 - computeOffsetYByTimeMills(mCurrentTimeMills);
    }

    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        Log.i(TAG, "onSaveInstanceState");

        SavedState savedState = new SavedState(super.onSaveInstanceState());
        savedState.mLines = mLines;
        savedState.mCurrentTimeMills = mCurrentTimeMills;
        savedState.mLineSpacing = mLineSpacing;
        savedState.misKaraokeEnable = misKaraokeEnable;
        savedState.mKaraokeColor = mKaraokeColor;
        savedState.mHighlightColor = mHighlightColor;
        savedState.mTextColor = mTextColor;
        savedState.mTextSize = mTextSize;

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
        misKaraokeEnable = savedState.misKaraokeEnable;
        mKaraokeColor = savedState.mKaraokeColor;
        mHighlightColor = savedState.mHighlightColor;
        mTextColor = savedState.mTextColor;
        mTextSize = savedState.mTextSize;
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
        if (mState != State.SCROLLING && mState != State.IDLE) {
            return;
        }
        mState = State.SCROLLING;

        float deltaY = mScrollToOffsetY - mCurrentOffsetY;
        float distance = mScrollVelocity * TIME_INTERVAL_SCROLL;

        if (Math.abs(deltaY) > distance) {
            mCurrentOffsetY += deltaY > 0 ? distance : -distance;
            Log.i(TAG, "performScroll abs(deltaY)>distance "
                    + "\nmScrollToOffsetY:" + mScrollToOffsetY
                    + "\nmCurrentOffsetY:" + mCurrentOffsetY
                    + "\ndeltaY:" + deltaY
                    + "\ndistance:" + distance);
        } else {
            mCurrentOffsetY = mScrollToOffsetY;
            Log.i(TAG, "performScroll abs(deltaY)<distance "
                    + "\nmScrollToOffsetY:" + mScrollToOffsetY
                    + "\nmCurrentOffsetY:" + mCurrentOffsetY
                    + "\ndeltaY:" + deltaY
                    + "\ndistance:" + distance);
        }

        float ratio = (mCurrentOffsetY - mScrollFromOffsetY) / (mScrollToOffsetY - mScrollFromOffsetY);
        float zoomInTextSize = (1 - ratio) * mHighlightTextSize + ratio * mTextSize;
        float zoomOutTextSize = (1 - ratio) * mTextSize + ratio * mHighlightTextSize;

        Log.i(TAG, "performScroll"
                + "\nratio:" + ratio
                + "\nzoomInTextSize:" + zoomInTextSize
                + "\nzoomOutTextSize:" + zoomOutTextSize
                + "\nmCurrentOffsetY:" + mCurrentOffsetY
                + "\nmScrollFromOffsetY:" + mScrollFromOffsetY
                + "\nmScrollToOffsetY:" + mScrollToOffsetY);

        mZoomInPaint.setTextSize(zoomInTextSize);
        mZoomOutPaint.setTextSize(zoomOutTextSize);

        invalidate();
        if (mCurrentOffsetY == mScrollToOffsetY) {
            mState = State.IDLE;
            return;
        }
        postDelayed(new Runnable() {
            @Override
            public void run() {
                performScroll();
            }
        }, TIME_INTERVAL_SCROLL);
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
        int index = (int) (((mHeight >> 1) - offsetY) / (mTextHeight + mLineSpacing) + 0.5f);
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

    private float computeOffsetYByIndex(int index, int currentIndex) {
        float offsetY;
        if (index == 0) {
            offsetY = 0;
        } else if (index < currentIndex) {
            offsetY = (mTextHeight + mLineSpacing) * index;
        } else if (index == currentIndex) {
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
            mState = State.IDLE;
            return;
        }
        if (mState != State.FLINGING) {
            return;
        }

        float deltaY = mFlingVelocity * TIME_INTERVAL_FLING / 1000;
        mCurrentOffsetY += deltaY;

        float deltaV = mFlingAccelerate * TIME_INTERVAL_FLING;
        if (mFlingVelocity > 0) {
            deltaV = Math.max(-mFlingVelocity, deltaV);
        } else {
            deltaV = Math.min(-mFlingVelocity, -deltaV);
        }
        mFlingVelocity += deltaV;

        invalidate();

        postDelayed(new Runnable() {
            @Override
            public void run() {
                performFling();
            }
        }, TIME_INTERVAL_FLING);
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

        private List<Line> mLines;
        private long mCurrentTimeMills;
        private float mLineSpacing;
        private boolean misKaraokeEnable;
        private int mKaraokeColor;
        private int mHighlightColor;
        private int mTextColor;
        private float mTextSize;

        SavedState(Parcelable superState) {
            super(superState);
        }

        SavedState(Parcel in) {
            super(in);
            this.mLines = in.createTypedArrayList(Line.CREATOR);
            this.mCurrentTimeMills = in.readLong();
            this.mLineSpacing = in.readFloat();
            this.misKaraokeEnable = in.readByte() != 0;
            this.mKaraokeColor = in.readInt();
            this.mHighlightColor = in.readInt();
            this.mTextColor = in.readInt();
            this.mTextSize = in.readFloat();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);

            dest.writeTypedList(this.mLines);
            dest.writeLong(this.mCurrentTimeMills);
            dest.writeFloat(this.mLineSpacing);
            dest.writeByte(this.misKaraokeEnable ? (byte) 1 : (byte) 0);
            dest.writeInt(this.mKaraokeColor);
            dest.writeInt(this.mHighlightColor);
            dest.writeInt(this.mTextColor);
            dest.writeFloat(this.mTextSize);
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
         * 滚动中
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
    }
}
