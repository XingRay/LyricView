package com.leixing.lyricview;

/**
 * 触摸监听器
 *
 * @author leixing
 */
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