package com.rtmpx.library.publish;

import android.util.Log;

import androidx.camera.core.ImageProxy;

import com.rtmpx.library.camera.ICamera;
import com.rtmpx.library.camera.ICameraPreviewCallback;
import com.rtmpx.library.config.Config;
import com.rtmpx.library.record.audio.AudioRecorder;
import com.rtmpx.library.utils.ConvertUtils;

import java.util.concurrent.atomic.AtomicBoolean;

public class PublisherX extends ICameraPreviewCallback implements AudioRecorder.OnAudioRecordListener {
    private static final String TAG = "PublisherX";
    private byte[] mBuff = new byte[2048];
    private Config mConfig;
    private ICamera mCamera;
    private AudioRecorder mAudioRecorder = new AudioRecorder(mBuff);
    private AtomicBoolean mPublishing = new AtomicBoolean(false);

    public PublisherX(Config mConfig, ICamera mCamera) {
        this.mConfig = mConfig;
        bindCamera(mCamera);
    }

    public PublisherX(Config mConfig) {
        this.mConfig = mConfig;
    }

    /**
     * bind a camera
     *
     * @param mCamera {@link ICamera}
     */
    public void bindCamera(ICamera mCamera) {
        this.mCamera = mCamera;
        this.mCamera.setPreviewCallback(this);
        mAudioRecorder.setAudioRecordListener(this);
    }

    /**
     * set a publishing listener
     *
     * @param mPublishListener
     */
    public void setPublishListener(IPublishListener mPublishListener) {
        RTMPPublisher.getInstance().setPublishListener(mPublishListener);
    }

    /**
     * start rtmp publish
     */
    public void startPublish() {
        if (mPublishing.get()) {
            Log.d(TAG, "is publishing no handle startPublish");
            return;
        }
        mAudioRecorder.startRecord();
        RTMPPublisher.getInstance().startPublish(mConfig);
        mPublishing.set(true);
    }

    /**
     * stop rtmp publish
     */
    public void stopPublish() {
        if (!mPublishing.get()) {
            Log.d(TAG, "not  publishing no handle stopPublish");
            return;
        }
        RTMPPublisher.getInstance().stopPublish();
        if (null != mAudioRecorder) {
            mAudioRecorder.stopRecord();
        }
        mPublishing.set(false);
    }

    /**
     * freed
     */
    public void release() {
        if (null != mAudioRecorder) {
            mAudioRecorder.setAudioRecordListener(null);
            mAudioRecorder.release();
        }
        RTMPPublisher.getInstance().setPublishListener(null);
    }

    @Override
    public void onAudioRecord(byte[] pcm) {
        if (!RTMPPublisher.getInstance().isWorked()) {
            Log.d(TAG, "rtmp not  worked  no handle onAudioRecord");
            return;
        }
        RTMPPublisher.getInstance().encodeAudio(pcm);
    }

    @Override
    public void onAudioRecordError(String message) {
        Log.d(TAG, "onAudioRecordError message is " + message);
    }

    @Override
    public void handleImage(ImageProxy image, int rotation) {
        if (!RTMPPublisher.getInstance().isWorked()) {
            Log.d(TAG, "rtmp not  worked  no handle handleImage");
            return;
        }
        byte[] data = ConvertUtils.YUV_420_888toNV12(image, rotation);
        RTMPPublisher.getInstance().encodeVideo(data);
    }
}
