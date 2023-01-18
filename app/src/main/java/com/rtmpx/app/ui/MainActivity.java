package com.rtmpx.app.ui;

import static com.rtmpx.app.config.Const.BITRATE;
import static com.rtmpx.app.config.Const.FRAME_RATE;
import static com.rtmpx.app.config.Const.HEIGHT;
import static com.rtmpx.app.config.Const.PREFERENCE_PUBLISH_CONFIG;
import static com.rtmpx.app.config.Const.PREFERENCE_SAVE_CONFIG;
import static com.rtmpx.app.config.Const.PUBLISH_URL;
import static com.rtmpx.app.config.Const.RECORD;
import static com.rtmpx.app.config.Const.RECORD_PATH;
import static com.rtmpx.app.config.Const.RESULT;
import static com.rtmpx.app.config.Const.WIDTH;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.PermissionChecker;

import com.rtmpx.app.R;
import com.rtmpx.app.widget.FocusView;
import com.rtmpx.library.camera.widget.CameraXImplView;
import com.rtmpx.library.config.Config;
import com.rtmpx.library.publish.IPublishListener;
import com.rtmpx.library.publish.PublisherX;

import org.json.JSONException;
import org.json.JSONObject;

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
    private ImageView mSwitchCamera, mPublishSetting;

    private ActivityResultLauncher<Intent> mResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        setContentView(R.layout.activity_main);
        bindViews();
        mResultLauncher = registerResult();
    }

    private void bindPublisher() {
        mPublisher = new PublisherX(mConfig, mPreview);
//        mPublisher.bindCamera(mPreview);
        mPublisher.setPublishListener(this);
    }

    private void bindViews() {
        mPreview = findViewById(R.id.preview);
        mFocusView = findViewById(R.id.focus_container);
        mStartPublish = findViewById(R.id.start_publish);
        mStartPreview = findViewById(R.id.start_preview);
        mSwitchCamera = findViewById(R.id.switch_camera);
        mPublishSetting = findViewById(R.id.publish_setting);

        mStartPublish.setOnClickListener(this);
        mStartPreview.setOnClickListener(this);
        mSwitchCamera.setOnClickListener(this);
        mPublishSetting.setOnClickListener(this);
        mPreview.setOnTouchListener(mFocusTouchListener);
        mFocusView.setExposureSelectListener(this);

        String savedCong = getSharedPreferences(PREFERENCE_PUBLISH_CONFIG, MODE_PRIVATE).getString(PREFERENCE_SAVE_CONFIG, "");
        if (TextUtils.isEmpty(savedCong)) {
            bindConfig(buildDefaultConfig());
        } else {
            try {
                bindConfig(buildConfigWithJson(savedCong));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void bindConfig(Config config) {
        Log.d(TAG, " ===bindConfig=== " + config.toString());
        mConfig = config;
        if (mConfig.getVideoWidth() > mConfig.getVideoHeight()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        mPreview.setPreviewRange(mConfig.getFrameRate(), mConfig.getFrameRate());
        mPreview.setTargetResolution(mConfig.getVideoWidth(), mConfig.getVideoHeight());

        bindPublisher();
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

    private Config buildDefaultConfig() {
        Config config = new Config.ConfigBuilder()
                .withBitRate(1000 * 2000)
                .withPublishUrl("rtmp://192.168.33.98:1935/rtmplive/live")
                .withFrameRate(30).withVideoWidth(1080).withVideoHeight(1920)
                .withRecordVideo(false).withRecordVideoPath("sdcard/dump.mp4")
                .build();
        return config;
    }

    private Config buildConfigWithJson(String configJson) throws JSONException {
        JSONObject config = new JSONObject(configJson);
        return new Config.ConfigBuilder()
                .withBitRate(config.optInt(BITRATE))
                .withPublishUrl(config.optString(PUBLISH_URL))
                .withFrameRate(config.optInt(FRAME_RATE))
                .withVideoWidth(config.optInt(WIDTH))
                .withVideoHeight(config.optInt(HEIGHT))
                .withRecordVideo(config.optBoolean(RECORD))
                .withRecordVideoPath(config.optString(RECORD_PATH))
                .build();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_preview:
                startPreview();
//                mPreview.switchCamera(0);
                break;
            case R.id.start_publish:
                togglePublish();
                break;
            case R.id.switch_camera:
                mPreview.switchCamera((1 == mPreview.getCameraId()) ? 0 : 1);
                break;
            case R.id.publish_setting:
                Intent intent = new Intent(this, PublishPreferenceActivity.class);
                mResultLauncher.launch(intent);
                break;
        }
    }

    private ActivityResultLauncher<Intent> registerResult() {
        return registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if (result.getResultCode() == RESULT_OK) {
                    String resultConfig = result.getData().getStringExtra(RESULT);
                    Log.d(TAG, " =====resultConfig==== " + resultConfig);
                    try {
                        bindConfig(buildConfigWithJson(resultConfig));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    public void onConnecting() {
        Log.d(TAG, "========onConnecting========");
    }

    @Override
    public void onConnected() {
        Log.d(TAG, "========onConnected========");
    }

    @Override
    public void onConnectedFailed(int code) {
        Log.d(TAG, "========onConnectedFailed========" + code);
    }

    @Override
    public void onStartPublish() {
        Log.d(TAG, "========onStartPublish========");
    }

    @Override
    public void onStopPublish() {
        Log.d(TAG, "========onStopPublish========");
        stopPublish();
    }

    @Override
    public void onStartRecord() {
        Log.d(TAG, "========onStartRecord========");
    }

    @Override
    public void onStopRecord() {
        Log.d(TAG, "========onStopRecord========");
    }

    @Override
    public void onFpsStatistic(int fps) {
        Log.d(TAG, "========onFpsStatistic========" + fps);
    }

    @Override
    public void onRtmpDisconnect() {
        Log.d(TAG, "========onRtmpDisconnect========");
    }
}