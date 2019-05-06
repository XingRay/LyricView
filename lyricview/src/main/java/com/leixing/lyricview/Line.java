package com.leixing.lyricview;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 歌词行
 *
 * @author leixing
 */
@SuppressWarnings("WeakerAccess")
public class Line implements Parcelable {
    private final long startMills;
    private final long endMills;
    private final String content;

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

    public long getEndMills() {
        return endMills;
    }

    public String getContent() {
        return content;
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