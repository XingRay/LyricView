package com.leixing.lyricview;

import android.graphics.Paint;

/**
 * @author leixing
 * 颜色设计器
 */
public interface LineDesigner {

    /**
     * 根据歌词行在绘制区的偏移量设置绘制歌词行的画笔
     *
     * @param paint           画笔
     * @param offsetY         歌词行在绘制区的偏移量
     * @param highlightOffset 高亮行在绘制区的偏移量
     * @param height          绘制区的高度
     */
    void designLine(Paint paint, float offsetY, float highlightOffset, int height);
}