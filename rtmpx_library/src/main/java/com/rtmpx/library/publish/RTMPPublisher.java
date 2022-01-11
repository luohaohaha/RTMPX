package com.rtmpx.library.publish;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import com.rtmpx.library.config.Config;
import com.rtmpx.library.encode.AudioEncoder;
import com.rtmpx.library.encode.Encoder;
import com.rtmpx.library.encode.VideoEncoder;
import com.rtmpx.library.record.video.VideoRecorder;
import com.rtmpx.library.rtmp.RTMPFrame;
import com.rtmpx.library.rtmp.RTMPMuxer;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.media.MediaCodec.BUFFER_FLAG_CODEC_CONFIG;
import static com.rtmpx.library.config.Const.FRAME_TYPE_AUDIO;
import static com.rtmpx.library.config.Const.FRAME_TYPE_VIDEO;
import static com.rtmpx.library.rtmp.RtmpClient.RTMP_READ_DONE;


public class RTMPPublisher implements Handler.Callback {
    private static final String TAG = "RTMPVRPublisher";

    private static final int MSG_WORK_START = 0xff00;
    private static final int MSG_START_PUBLISH = 0xff01;

    private static final int MSG_RTMP_CONNECTING = 0x00;
    private static final int MSG_RTMP_CONNECTED = 0x01;
    private static final int MSG_RTMP_CONNECTED_FAILED = 0x02;
    private static final int MSG_RTMP_START_PUBLISH = 0x03;
    private static final int MSG_RTMP_STOP_PUBLISH = 0x04;
    private static final int MSG_RTMP_START_RECORD = 0x05;
    private static final int MSG_RTMP_STOP_RECORD = 0x06;
    private static final int MSG_RTMP_FPS_STATISTIC = 0x07;
    private static final int MSG_RTMP_RELEASE = 0x08;
    private static final int MSG_RTMP_DISCONNECT = 0x09;

    private static volatile RTMPPublisher mInstance;
    private Config mConfig;
    private Queue<RTMPFrame> mFrameQueue = new ConcurrentLinkedDeque<>();
    private HandlerThread mWorkThread;
    private Handler mWorkHandler;
    private Handler mCallbackHandler;
    private AtomicBoolean mThreadWorked = new AtomicBoolean(false);
    private AtomicBoolean mRTMPConnected = new AtomicBoolean(false);

    private RTMPMuxer mRtmpMuxer;
    private Encoder<byte[]> mVideoEncoder;
    private Encoder<byte[]> mAudioEncoder;
    private VideoRecorder mVideoRecorder;
    private long mLastTimestamp = 0;

    private IPublishListener mPublishListener;
    private AtomicBoolean mInvokedPublish = new AtomicBoolean(false);
    private long mStartTime;
    private int mWriteCount;

    private RTMPPublisher() {
    }

    public static RTMPPublisher getInstance() {
        if (mInstance == null) {
            synchronized (RTMPPublisher.class) {
                if (mInstance == null) {
                    mInstance = new RTMPPublisher();
                }
            }
        }
        return mInstance;
    }

    void setPublishListener(IPublishListener mPublishListener) {
        this.mPublishListener = mPublishListener;
    }

    private void checkWorkThread() {
        checkWorkThread(-1, null);
    }

    private void checkWorkThread(int otherMessage, Object obj) {
        if (null == mCallbackHandler) {
            mCallbackHandler = new Handler(Looper.getMainLooper(), this);
        }
        if (null == mWorkThread || !mWorkThread.isAlive() || mWorkThread.isInterrupted()) {
            mThreadWorked.set(false);
            mWorkThread = new HandlerThread("RTMPPublisher") {
                @Override
                protected void onLooperPrepared() {
                    super.onLooperPrepared();
                    mWorkHandler = new Handler(getLooper(), RTMPPublisher.this);
                    mWorkHandler.obtainMessage(MSG_WORK_START).sendToTarget();
                    if (otherMessage > 0) {
                        mWorkHandler.obtainMessage(otherMessage, obj).sendToTarget();
                    }
                }
            };
            mWorkThread.start();
        } else {
            if (otherMessage > 0) {
                mWorkHandler.obtainMessage(otherMessage, obj).sendToTarget();
            }
        }
    }

    private int checkRtmpConnection(Config vrConfig) {
        if (null == mRtmpMuxer) {
            mRtmpMuxer = new RTMPMuxer();
        }
        String publishUrl = vrConfig.getPublishUrl();
        if (TextUtils.isEmpty(publishUrl)) {
            Log.i(TAG, "publish url is null");
            stopPublish();
            return RTMP_READ_DONE;
        }
        Log.i(TAG, "publishUrl is " + publishUrl);
        if (!mRTMPConnected.get()) {
            int status = mRtmpMuxer.open(publishUrl, vrConfig.getVideoWidth(), vrConfig.getVideoHeight(), mConfig.getFrameRate());
            return status;
        }
        return RTMP_READ_DONE;
    }


    void startPublish(Config config) {
        mConfig = config;
        reset();
        if (null == mVideoEncoder) {
            mVideoEncoder = new VideoEncoder(config);
        }
        if (null == mAudioEncoder) {
            mAudioEncoder = new AudioEncoder(config);
        }
        checkWorkThread(MSG_START_PUBLISH, config);
    }

    void stopPublish() {
        release();
        if (null != mCallbackHandler) {
            mCallbackHandler.obtainMessage(MSG_RTMP_STOP_PUBLISH).sendToTarget();
        }
    }

    private void release() {
        reset();
        if (null != mWorkHandler) {
            mWorkHandler.obtainMessage(MSG_RTMP_RELEASE).sendToTarget();
        }
    }

    private void reset() {
        mStartTime = 0;
        mLastTimestamp = 0;
        mWriteCount = 0;
        mThreadWorked.set(false);
        mInvokedPublish.set(false);
        if (null != mWorkHandler) {
            mWorkHandler.removeCallbacksAndMessages(null);
        }
        mFrameQueue.clear();
    }

    public void addFrame(RTMPFrame vrFrame) {
        mFrameQueue.add(vrFrame);
    }

    public void encodeVideo(byte[] data) {
        if (!isWorked()) return;
        if (null == mVideoEncoder) {
            Log.i(TAG, "mVideoEncoder is null");
            return;
        }
        mVideoEncoder.encode(data);
    }

    public void encodeAudio(byte[] data) {
        if (!isWorked()) return;
        if (null == mAudioEncoder) {
            Log.i(TAG, "mAudioEncoder is null");
            return;
        }
        mAudioEncoder.encode(data);
    }


    @Override
    public boolean handleMessage(Message msg) {
        Log.i(TAG, "===========handleMessage============" + msg.what);
        switch (msg.what) {
            case MSG_WORK_START:
                mThreadWorked.set(true);
                break;
            case MSG_START_PUBLISH:
                mCallbackHandler.obtainMessage(MSG_RTMP_CONNECTING).sendToTarget();
                mFrameQueue.clear();
                Config config = (Config) msg.obj;
                Log.i(TAG, "thread is " + Thread.currentThread());
                int status = checkRtmpConnection(config);
                boolean connected = status > 0;
                mRTMPConnected.set(connected);
                if (!connected) {
                    Log.i(TAG, "===========checkRtmpConnection not connected============" + status);
                }
                if (connected) {
                    mCallbackHandler.obtainMessage(MSG_RTMP_CONNECTED).sendToTarget();
                    long time = SystemClock.uptimeMillis();
                    mVideoEncoder.setStartTime(time);
                    mAudioEncoder.setStartTime(time);
                    mVideoEncoder.start();
                    mAudioEncoder.start();
                    startWrite();
                } else {
                    mCallbackHandler.obtainMessage(MSG_RTMP_CONNECTED_FAILED, status).sendToTarget();
                }
                break;
            case MSG_RTMP_CONNECTING:
                if (null != mPublishListener) {
                    mPublishListener.onConnecting();
                }
                break;
            case MSG_RTMP_CONNECTED:
                if (null != mPublishListener) {
                    mPublishListener.onConnected();
                }
                break;
            case MSG_RTMP_CONNECTED_FAILED:
                if (null != mPublishListener) {
                    mPublishListener.onConnectedFailed((Integer) msg.obj);
                }
                stopPublish();
                break;
            case MSG_RTMP_START_PUBLISH:
                if (null != mPublishListener) {
                    mPublishListener.onStartPublish();
                }
                break;
            case MSG_RTMP_STOP_PUBLISH:
                if (null != mPublishListener) {
                    mPublishListener.onStopPublish();
                }
                break;
            case MSG_RTMP_START_RECORD:
                if (null != mPublishListener) {
                    mPublishListener.onStartRecord();
                }
                break;
            case MSG_RTMP_STOP_RECORD:
                if (null != mPublishListener) {
                    mPublishListener.onStopRecord();
                }
                mCallbackHandler.removeCallbacksAndMessages(null);
//                mCallbackHandler = null;
                break;
            case MSG_RTMP_FPS_STATISTIC:
                if (null != mPublishListener) {
                    mPublishListener.onFpsStatistic((Integer) msg.obj);
                }
                break;
            case MSG_RTMP_DISCONNECT:
                if (null != mPublishListener) {
                    mPublishListener.onRtmpDisconnect();
                }
                break;
            case MSG_RTMP_RELEASE:
                try {
                    mVideoEncoder.stop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    mAudioEncoder.stop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                stopRecord();
                try {
                    if (mRTMPConnected.get()) {
                        mRtmpMuxer.close();
                        mRtmpMuxer = null;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    if (null != mWorkThread) {
                        mWorkThread.quit();
                        mWorkThread.interrupt();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    mRTMPConnected.set(false);
                    mWorkThread = null;
                    mWorkHandler = null;
                    mVideoEncoder = null;
                    mAudioEncoder = null;
                    mVideoRecorder = null;
                }
                break;
        }
        return false;
    }


    private void startRecord() {
        if (null != mAudioEncoder.getSps() && null != mVideoEncoder.getSps() && null != mVideoEncoder.getPps()) {
            try {
                String recordVideoPath = mConfig.getRecordVideoPath();
                if (null == mVideoRecorder) {
                    mVideoRecorder = new VideoRecorder(mConfig, mVideoEncoder.getSps(), mVideoEncoder.getPps(), mAudioEncoder.getSps());
                }
                if (!mVideoRecorder.isStart()) {
                    mVideoRecorder.startRecord(recordVideoPath);
                    if (null != mCallbackHandler) {
                        mCallbackHandler.obtainMessage(MSG_RTMP_START_RECORD).sendToTarget();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void stopRecord() {
        try {
            if (null == mVideoRecorder)
                return;
            mVideoRecorder.stopRecord();
            mVideoRecorder.release();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != mCallbackHandler) {
                mVideoRecorder = null;
                mCallbackHandler.obtainMessage(MSG_RTMP_STOP_RECORD).sendToTarget();
            }
        }
    }


    private void startWrite() {
        if (!isWorked()) {
            Log.i(TAG, "rtmp not connected");
            return;
        }
        for (; ; ) {
            if (!isWorked()) {
                Log.i(TAG, "rtmp not worked");
                break;
            }
            if (mFrameQueue.isEmpty()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    stopPublish();
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    continue;
                }
            }
            RTMPFrame frame = mFrameQueue.poll();
            writeSample(frame);
        }
    }

    public boolean isWorked() {
        return mThreadWorked.get() && null != mRtmpMuxer && mRTMPConnected.get();
    }

    public void writeSample(RTMPFrame frame) {
        if (null == frame || !isWorked()) return;
        if (null != mRtmpMuxer && !mRtmpMuxer.isConnected()) {
            Log.i(TAG, "=========disconnect reconnect  =========");
            mCallbackHandler.obtainMessage(MSG_RTMP_STOP_PUBLISH).sendToTarget();
            mCallbackHandler.obtainMessage(MSG_RTMP_DISCONNECT).sendToTarget();
            return;
        }
        byte[] data = frame.getData();
        int type = frame.getType();
        long offset = frame.getPresentationTimeUs() ;
        if (offset < mLastTimestamp && frame.getBufferInfo().flags != BUFFER_FLAG_CODEC_CONFIG) {
            offset = mLastTimestamp;
        }
        Log.i(TAG, "=========offset=========" + offset + " size is " + data.length);
        if (mConfig.isRecordVideo()) {
            startRecord();
        } else {
            if (null != mVideoRecorder) {
                stopRecord();
            }
        }
        if (FRAME_TYPE_VIDEO == type) {
            if (0 == mStartTime) {
                mStartTime = System.currentTimeMillis();
            }
            if (!mInvokedPublish.get()) {
                mCallbackHandler.obtainMessage(MSG_RTMP_START_PUBLISH).sendToTarget();
                mInvokedPublish.set(true);
            }
            Log.i(TAG, "=========writeVideo=========" + offset);
            mWriteCount++;
            if (mWriteCount % mConfig.getFrameRate() == 0) {
                long countTime = System.currentTimeMillis() - mStartTime;
                long normalTime = (1000 / mConfig.getFrameRate()) * mWriteCount;
                float scale = (float) normalTime / (float) countTime;
                Log.i(TAG, "avgFPS scale == " + scale);
                int avgFPS = Math.round(mConfig.getFrameRate() * scale);
                mCallbackHandler.obtainMessage(MSG_RTMP_FPS_STATISTIC, avgFPS).sendToTarget();
            }
            if (null != mVideoRecorder) {
                mVideoRecorder.writeVideo(frame);
            }
            mRtmpMuxer.writeVideo(data, 0, data.length, offset);
        }
        if (FRAME_TYPE_AUDIO == type) {
            Log.i(TAG, "=========writeAudio=========" + offset);
            if (null != mVideoRecorder) {
                mVideoRecorder.writeAudio(frame);
            }
            mRtmpMuxer.writeAudio(data, 0, data.length, offset);
        }
        if (frame.getBufferInfo().flags != BUFFER_FLAG_CODEC_CONFIG) {
            mLastTimestamp = offset;
        }
    }

}
