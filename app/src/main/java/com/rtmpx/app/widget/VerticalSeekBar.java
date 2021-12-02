package com.rtmpx.app.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.appcompat.widget.AppCompatSeekBar;

/**
 * Project: android_client<br/>
 * Package: com.sqtech.client.widget<br/>
 * ClassName: VerticalSeekBar<br/>
 * Description: TODO<br/>
 * Date: 2020/12/17 11:24 AM <br/>
 * <p>
 * Author  LuoHao<br/>
 * Version 1.0<br/>
 * since JDK 1.6<br/>
 * <p>
 */
public class VerticalSeekBar extends AppCompatSeekBar {

    private int max, min;

    private OnSeekBarChangeListener mSeekBarChangeListener;

    public VerticalSeekBar(Context context) {
        super(context);
    }

    public VerticalSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public VerticalSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(h, w, oldh, oldw);
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(heightMeasureSpec, widthMeasureSpec);
        setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
    }

    protected void onDraw(Canvas c) {
        c.rotate(-90);
        c.translate(-getHeight(), 0);
        super.onDraw(c);
    }

    @Override
    public synchronized void setProgress(int progress) {
        super.setProgress(progress);
        onSizeChanged(getWidth(), getHeight(), 0, 0);
    }

    public synchronized void setSupportMax(int max) {
        this.max = max;
        try {
            super.setMax(max);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void setSupportMin(int min) {
        this.min = min;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                super.setMin(min);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener l) {
        super.setOnSeekBarChangeListener(l);
        this.mSeekBarChangeListener = l;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if(null != mSeekBarChangeListener){
                    mSeekBarChangeListener.onStartTrackingTouch(this);
                }
            case MotionEvent.ACTION_MOVE:
                float y = event.getY();
                int height = getHeight();
                int progress = 0;
                final int range = max - min;
                float scale = y / height;
                progress -= scale * range + min;
                setProgress(progress);
                onSizeChanged(getWidth(), getHeight(), 0, 0);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if(null != mSeekBarChangeListener){
                    mSeekBarChangeListener.onStopTrackingTouch(this);
                }
                break;
        }
        return true;
    }
}
