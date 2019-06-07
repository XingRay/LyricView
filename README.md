# LyricView
Widget for showing lyric on android

## features

## attributes and configuration

### distance

 - lineDistance
 
distance from last break line of previous lyric line to first break line of next lyric line.

 - breakLineDistance

distance from previous break line to next break line in one lyric line.

### karaoke mode

karaoke mode enable

## todo list

 - 绘制区域界定, 以当前行为中心，上下范围搜索绘制边界
 - 支持padding，统一坐标系，使用绘制区原点
 - 单字karaoke模式支持
 - 超出边界时拖动时带有弹性效果，并且支持自定义插值器
 - 动态更改属性
 - 细化歌词行自定义设计
 - saveInstanceState补全
 - line中绑定相对位置信息，onDraw中直接根据数据渲染，减少计算量，歌词组切换时只需少量数据变更
 - 细化绘制过程，对歌词行进行过滤，剪切边界歌词行

