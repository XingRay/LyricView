package com.leixing.lyricview;

/**
 * 颜色设计器
 */
public interface ColorDesigner {

    /**
     * 根据歌词行的偏移量获取歌词行的颜色
     *
     * @param offsetY         歌词行的偏移量
     * @param highlightOffset 高亮行的偏移量
     * @param height          控件的高度
     * @return 该行歌词的颜色值
     */
    int getColor(float offsetY, int highlightOffset, int height);
}