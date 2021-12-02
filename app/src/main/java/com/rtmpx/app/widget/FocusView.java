package com.rtmpx.app.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.rtmpx.app.R;

import static com.rtmpx.app.utils.Utils.getScreenHeight;
import static com.rtmpx.app.utils.Utils.getScreenWidth;


/**
 * Project: publisher<br/>
 * Package: com.sqtech.vr.client.app.widget<br/>
 * ClassName: FocusView<br/>
 * Description: TODO<br/>
 * Date: 2021-07-12 12:24 下午 <br/>
 * <p>
 * Author luohao<br/>
 * Version 1.0<br/>
 * since JDK 1.6<br/>
 * <p>
 */
public class FocusView extends LinearLayout implements Handler.Callback, SeekBar.OnSeekBarChangeListener, View.OnClickListener {
    private static final String TAG = "FocusView";
    private static final int MSG_HIDE_FOCUS = 0XFF33;//隐藏对焦view
    private static final int MSG_HIDE_FOCUS_INTERVAL = 2000;//焦点view无操作2s后隐藏
    private ImageView mFocusRect;
    private ImageView mFocusSolid;
    private VerticalSeekBar mExposureSlider;
    private ImageView mExposureLocked;
    private LinearLayout mExposureContainer;
    private AnimatorSet mFocusAnimatorSet;
    private AnimatorSet mGoneAnimatorSet;
    private Handler mHandler;
    /**
     * 屏幕宽&高
     */
    private int mScreenWidth, mScreenHeight;

    private OnExposureHandleListener mExposureSelectListener;

    public FocusView(Context context) {
        this(context, null);
    }

    public FocusView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FocusView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(context, attrs);
    }

    private void initAttrs(Context context, AttributeSet attr) {
        View contentView = View.inflate(context, R.layout.vr_view_focus, this);
        setOrientation(LinearLayout.HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        mFocusRect = contentView.findViewById(R.id.focus_rect);
        mFocusSolid = contentView.findViewById(R.id.focus_solid);
        mExposureSlider = findViewById(R.id.exposure_slider);
        mExposureLocked = findViewById(R.id.exposure_locked);
        mExposureContainer = findViewById(R.id.exposure_container);

        mHandler = new Handler(Looper.getMainLooper(), this);
        mExposureSlider.setOnSeekBarChangeListener(this);
        mExposureLocked.setOnClickListener(this);
        mScreenWidth = getScreenWidth(context);
        mScreenHeight = getScreenHeight(context);
    }

    public void setExposureSelectListener(OnExposureHandleListener mExposureSelectListener) {
        this.mExposureSelectListener = mExposureSelectListener;
    }

    public void setExposureRange(int max, int min) {
        if (null == mExposureSlider) return;
        try {
            mExposureSlider.setSupportMin(min);
            mExposureSlider.setSupportMax(max);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setExposureLocked(boolean locked){
        if(null == mExposureLocked) return;
        mExposureLocked.setSelected(locked);
    }

    public void setExposureValue(int value) {
        if (null == mExposureSlider) return;
        mExposureSlider.setProgress(value);
    }

    public void revokeHideFocus() {
        if (null == mHandler) return;
        mHandler.removeMessages(MSG_HIDE_FOCUS);
    }

    public void hideFocus() {
        sendHideFocusViewMessage();
    }

    /**
     * 发送隐藏对焦view消息
     */
    private void sendHideFocusViewMessage() {
        if (null == mHandler) return;
        Log.i(TAG, "=========sendHideFocusMessage===========");
        mHandler.removeMessages(MSG_HIDE_FOCUS);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_HIDE_FOCUS), MSG_HIDE_FOCUS_INTERVAL);
    }

    /**
     * 生成对焦view动画
     */
    private void buildFocusViewAnimation() {
        if (null == mFocusAnimatorSet) {
            mFocusAnimatorSet = new AnimatorSet();
            ObjectAnimator scaleRectX = ObjectAnimator.ofFloat(mFocusRect, "scaleX", 1.7f, 1.0f);
            ObjectAnimator scaleRectY = ObjectAnimator.ofFloat(mFocusRect, "scaleY", 1.7f, 1.0f);
            ObjectAnimator scaleSolidX = ObjectAnimator.ofFloat(mFocusSolid, "scaleX", .3f, 1.0f);
            ObjectAnimator scaleSolidY = ObjectAnimator.ofFloat(mFocusSolid, "scaleY", .3f, 1.0f);
            ObjectAnimator alphaFocus = ObjectAnimator.ofFloat(this, "alpha", .0f, 1.0f);
            mFocusAnimatorSet.play(alphaFocus).with(scaleRectX).with(scaleRectY).with(scaleSolidX).with(scaleSolidY);
            mFocusAnimatorSet.setDuration(300);
            mFocusAnimatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    FocusView.this.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    sendHideFocusViewMessage();
                }

            });
        }
    }

    /**
     * 执行对焦动画
     *
     * @param event
     */
    public void animateFocusView(MotionEvent event,int orientation) {
        int realWidth = mScreenWidth, realHeight = mScreenHeight;
        if(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE == orientation){
            int tmpWidth = realWidth;
            realWidth = realHeight;
            realHeight = tmpWidth;
        }
        if (null != mFocusAnimatorSet && mFocusAnimatorSet.isRunning()) return;
        int width = getWidth();
        int height = getHeight();
        float x = event.getX() - width / 2;
        float y = event.getY() - height / 2;
        if (x > realWidth) {
            x = realWidth - width;
        }
        if (x < 0) {
            x = -width / 5;
        }
        if (y > realHeight) {
            y = realHeight - height;
        }
        if (y < 0) {
            y = -height / 5;
        }

        setX(x);
        setY(y);
        buildFocusViewAnimation();
        if (null != mFocusAnimatorSet) {
            mFocusAnimatorSet.start();
            if (null != mGoneAnimatorSet && mGoneAnimatorSet.isRunning()) {
                mGoneAnimatorSet.cancel();
            }
        }
    }

    private void hideFocusView() {
        if (null == mGoneAnimatorSet) {
            mGoneAnimatorSet = new AnimatorSet();
            ObjectAnimator goneFocusAnimator = ObjectAnimator.ofFloat(this, "alpha", 1.0f, .1f);
            mGoneAnimatorSet.play(goneFocusAnimator);
            mGoneAnimatorSet.setDuration(500);
            mGoneAnimatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    setVisibility(View.GONE);
                }
            });
        }
        mGoneAnimatorSet.start();
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        switch (msg.what) {
            case MSG_HIDE_FOCUS:
                hideFocusView();
                break;
        }
        return false;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (null == mHandler) return;
        mHandler.removeCallbacksAndMessages(null);
        mHandler = null;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (null == mExposureSelectListener) return;
        mExposureSelectListener.onExposureSelect(progress);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        revokeHideFocus();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        hideFocus();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(R.id.exposure_locked == id){
            if(null != mExposureSelectListener){
                mExposureSelectListener.onExposureLock(mExposureLocked.isSelected());
            }
        }
    }

    public interface OnExposureHandleListener {
        void onExposureSelect(int progress);
        void onExposureLock(boolean lock);
    }
}
