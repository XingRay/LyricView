package com.leixing.demo.util;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.TypedValue;

public class ViewUtil {
    public static float sp2px(Context context, float spValue) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, spValue, getDisplayMetrics(context));
    }

    private static DisplayMetrics getDisplayMetrics(Context context) {
        Resources resources;

        if (context == null) {
            resources = Resources.getSystem();
        } else {
            resources = context.getResources();
        }
        return resources.getDisplayMetrics();
    }
}
