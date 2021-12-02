package com.rtmpx.app;

import android.Manifest;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.PermissionChecker;

import com.rtmpx.app.widget.FocusView;
import com.rtmpx.library.camera.widget.CameraXImplView;
import com.rtmpx.library.config.Config;
import com.rtmpx.library.publish.IPublishListener;
import com.rtmpx.library.publish.PublisherX;

import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity implements FocusView.OnExposureHandleListener, View.OnClickListener, IPublishListener {

    private static final String TAG = "MainActivity";
    private static final String[] PERMISSIONS = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
    private static final int REQUEST_PERMISSION_CODE = 0XFF00;
    private int mFingerStep = 48;
    /**
     * 曝光量
     */
    private int mExposure = 0;
    private CameraXImplView mPreview;
    private Button mToggle;
    private AtomicBoolean mPreviewFlag = new AtomicBoolean(false);
    private AtomicBoolean mPublishFlag = new AtomicBoolean(false);
    private FocusView mFocusView;
    private View.OnTouchListener mFocusTouchListener = new View.OnTouchListener() {
        private boolean isMove = false;
        private float dX, dY;
        private long downClick;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            final int minTouch = ViewConfiguration.get(MainActivity.this).getScaledEdgeSlop();
            int action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    dX = mPreview.getX() - event.getRawX();
                    dY = mPreview.getY() - event.getRawY();
                    downClick = System.currentTimeMillis();
                    isMove = false;
                    break;
                case MotionEvent.ACTION_MOVE:
                    float xo = event.getRawX() + dX;
                    float yo = event.getRawY() + dY;
                    if (Math.abs(xo - mPreview.getX()) >= minTouch || Math.abs(yo - mPreview.getY()) >= minTouch) {
                        isMove = true;
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    if (System.currentTimeMillis() - downClick <= 200 && !isMove) {
                        int x = (int) event.getRawX();
                        int y = (int) event.getRawY();
                        Rect rect = new Rect(x, y, (x + mFingerStep), (y + mFingerStep));
                        mPreview.manualFocus(rect);
                        mFocusView.animateFocusView(event, getRequestedOrientation());
                        autoExposure();
                    }
                    break;
            }
            return true;
        }
    };
    private PublisherX mPublisher;
    private Config mConfig;
    private Button mStartPublish;
    private Button mStartPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        setContentView(R.layout.activity_main);
        bindViews();
        bindPublisher();
    }

    private void bindPublisher() {
        mPublisher = new PublisherX(mConfig);
        mPublisher.bindCamera(mPreview);
        mPublisher.setPublishListener(this);
    }

    private void bindViews() {
        mConfig = buildConfig();
        if(mConfig.getVideoWidth() > mConfig.getVideoHeight()){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        mPreview = findViewById(R.id.preview);
        mFocusView = findViewById(R.id.focus_container);
        mStartPublish = findViewById(R.id.start_publish);
        mStartPreview = findViewById(R.id.start_preview);

        mPreview.setPreviewRange(mConfig.getFrameRate(),mConfig.getFrameRate());
        mPreview.setTargetResolution(mConfig.getVideoWidth(), mConfig.getVideoHeight());
        mStartPublish.setOnClickListener(this);
        mStartPreview.setOnClickListener(this);
        mPreview.setOnTouchListener(mFocusTouchListener);
        mFocusView.setExposureSelectListener(this);
    }
    private void autoExposure() {
        if (mPreview.autoExposure()) {
            mFocusView.setExposureValue(0);
            mFocusView.setExposureValue(0);
        }
    }

    private void startPreview() {
        if (hasPermission()) {
            togglePreview();
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, REQUEST_PERMISSION_CODE);
        }
    }

    private boolean hasPermission() {
        boolean hasPermission = true;
        for (String permission : PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(this, permission) == PermissionChecker.PERMISSION_DENIED) {
                hasPermission = false;
                break;
            }
        }
        return hasPermission;
    }

    private void togglePreview() {
        if (mPreviewFlag.get()) {
            mPreview.stopPreview();
            mStartPreview.setText("START PREVIEW");
        } else {
            mPreview.startPreview();
            setExposureRange();
            mStartPreview.setText("STOP PREVIEW");
        }
        mPreviewFlag.set(!mPreviewFlag.get());
    }

    private void togglePublish() {
        if (mPublishFlag.get()) {
            stopPublish();
        } else {
            startPublish();
        }
    }

    private void stopPublish() {
        mPublisher.stopPublish();
        mStartPublish.setText("START PUBLISH");
        mPublishFlag.set(false);
    }

    private void startPublish() {
        mPublisher.startPublish();
        mStartPublish.setText("STOP PUBLISH");
        mPublishFlag.set(true);
    }

    private void setExposureRange() {
        if (null == mPreview) return;
        mFocusView.setExposureRange(mPreview.getMaxExposureCompensation(), mPreview.getMinExposureCompensation());
        mFocusView.setExposureValue(mPreview.getExposureCompensation());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPublisher.release();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        startPreview();
    }

    @Override
    public void onExposureSelect(int progress) {
        mExposure = progress;
        mPreview.setExposureCompensation(mExposure);
    }

    @Override
    public void onExposureLock(boolean lock) {
        boolean toggle = !mPreview.autoExposure();
        mPreview.setAutoExposure(toggle);
        mFocusView.setExposureLocked(!toggle);
    }

    private Config buildConfig() {
        Config config = new Config.ConfigBuilder()
                .withBitRate(1000 * 5000)
                .withPublishUrl("rtmp://192.168.50.170:18888/test/live")
                .withFrameRate(60).withVideoWidth(1080).withVideoHeight(1920)
                .withRecordVideo(false).withRecordVideoPath("sdcard/dump.mp4")
                .build();
        return config;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_preview:
                startPreview();
                break;
            case R.id.start_publish:
                togglePublish();
                break;
        }
    }

    @Override
    public void onConnecting() {
        Log.d(TAG,"========onConnecting========");
    }

    @Override
    public void onConnected() {
        Log.d(TAG,"========onConnected========");
    }

    @Override
    public void onConnectedFailed(int code) {
        Log.d(TAG,"========onConnectedFailed========"+code);
    }

    @Override
    public void onStartPublish() {
        Log.d(TAG,"========onStartPublish========");
    }

    @Override
    public void onStopPublish() {
        Log.d(TAG,"========onStopPublish========");
        stopPublish();
    }

    @Override
    public void onStartRecord() {
        Log.d(TAG,"========onStartRecord========");
    }

    @Override
    public void onStopRecord() {
        Log.d(TAG,"========onStopRecord========");
    }

    @Override
    public void onFpsStatistic(int fps) {
        Log.d(TAG,"========onFpsStatistic========"+fps);
    }

    @Override
    public void onRtmpDisconnect() {
        Log.d(TAG,"========onRtmpDisconnect========");
    }
}