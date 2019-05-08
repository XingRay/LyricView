package com.leixing.lyricview;

import java.util.Arrays;

class LineGroup {
    private final Line[] lines;
    private final long startMills;
    private final long endMills;

    LineGroup(Line[] lines, long startMills, long endMills) {
        this.lines = lines;
        this.startMills = startMills;
        this.endMills = endMills;
    }

    public Line[] getLines() {
        return lines;
    }

    public long getStartMills() {
        return startMills;
    }

    public long getEndMills() {
        return endMills;
    }

    @Override
    public String toString() {
        return "LineGroup{" +
                "lines=" + Arrays.toString(lines) +
                ", startMills=" + startMills +
                ", endMills=" + endMills +
                '}';
    }
}
