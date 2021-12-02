package com.rtmpx.app.utils;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import java.lang.reflect.Method;

public class Utils {

    public static int dip2px(Context context, int dp) {
        final float density = context.getResources().getDisplayMetrics().density;
        return (int) ((float) dp * density + 0.5f);
    }

    private static final DisplayMetrics getRealDisplayMetricsForAndroid40(Context context) {
        if (context == null) return null;
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();

        Class c;
        try {
            c = Class.forName("android.view.Display");
            Method method = c.getMethod("getRealMetrics", DisplayMetrics.class);
            method.invoke(display, dm);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dm;
    }

    public static int getScreenWidth(Context context) {
        if (context == null) return -1;
        DisplayMetrics displaymetrics = getRealDisplayMetricsForAndroid40(context);
        return displaymetrics.widthPixels;
    }

    public static int getScreenHeight(Context context) {
        if (context == null) return -1;
        DisplayMetrics displaymetrics = getRealDisplayMetricsForAndroid40(context);
        return displaymetrics.heightPixels;
    }

    public static void autoClickOnce(View view) {
        if (null == view)
            return;
        view.post(() -> autoClickOnce(view, getScreenWidth(view.getContext()) / 2, getScreenHeight(view.getContext()) / 2));
    }

    public static void autoClickOnce(View view, float x, float y) {
        if (null == view)
            return;
        long downTime = System.currentTimeMillis();
        long upTime = downTime + 200;
        view.dispatchTouchEvent(obtainEvent(downTime, upTime, x, y, MotionEvent.ACTION_DOWN));
        view.dispatchTouchEvent(obtainEvent(downTime, upTime, x, y, MotionEvent.ACTION_UP));
    }

    private static MotionEvent obtainEvent(long downTime, long eventTime, float x, float y, int action) {
        return MotionEvent.obtain(downTime, eventTime, action, x, y, 0);
    }
}
