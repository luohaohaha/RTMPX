package com.rtmpx.library.record.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.rtmpx.library.publish.RTMPPublisher;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.rtmpx.library.config.Const.AUDIO_SAMPLE_RATE;

public class AudioRecorder implements Handler.Callback {
    private static final String TAG = "AudioRecorder";

    private static final int MSG_START_RECORD = 0XFF11;
    private OnAudioRecordListener mAudioRecordListener;
    private AudioRecord mAudioRecorder;
    private HandlerThread mAudioRecordThread;
    private Handler mAudioRecordHandler;
    private byte[] mBuffer ;
    private AtomicBoolean mRecordStarted = new AtomicBoolean(false);


    public AudioRecorder(byte[] mBuffer) {
        this.mBuffer = mBuffer;
        initAudioRecord();
    }

    /**
     * set audio record listener
     * @param mAudioRecordListener
     */
    public void setAudioRecordListener(OnAudioRecordListener mAudioRecordListener) {
        this.mAudioRecordListener = mAudioRecordListener;
    }

    /**
     * Initialize recording
     */
    private void initAudioRecord() {
        if (null == mAudioRecorder) {
            int audioSource = MediaRecorder.AudioSource.MIC;
            int sampleRate = AUDIO_SAMPLE_RATE;
            int channelConfig = AudioFormat.CHANNEL_IN_STEREO;
            int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
            int minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
            mAudioRecorder = new AudioRecord(audioSource, sampleRate, channelConfig, audioFormat, Math.max(minBufferSize, mBuffer.length));
        }
    }

    /**
     * Send a recorded message
     */
    private void sendStartRecordMessage() {
        if (null == mAudioRecordThread || !mAudioRecordThread.isAlive() || mAudioRecordThread.isInterrupted()) {
            mAudioRecordThread = new HandlerThread("AudioRecord") {
                @Override
                protected void onLooperPrepared() {
                    super.onLooperPrepared();
                    mAudioRecordHandler = new Handler(getLooper(), AudioRecorder.this);
                    mAudioRecordHandler.obtainMessage(MSG_START_RECORD).sendToTarget();
                }
            };
            mAudioRecordThread.start();
        } else {
            mAudioRecordHandler.obtainMessage(MSG_START_RECORD).sendToTarget();
        }
    }

    /**
     * Start recording
     */
    private void startRecordPcm() {
        initAudioRecord();
        try {
            mAudioRecorder.startRecording();
            mRecordStarted.set(true);
            while (mRecordStarted.get()) {
                int read = mAudioRecorder.read(mBuffer, 0, mBuffer.length);
                if (read <= 0) {
                    Thread.sleep(10);
                    continue;
                }
                if (null != mAudioRecordListener && RTMPPublisher.getInstance().isWorked()) {
                    mAudioRecordListener.onAudioRecord(mBuffer);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Send stop recording message
     */
    private void sendStopRecordMessage() {
        stopRecordPcm();
    }

    /**
     * Stop recording
     */
    private void stopRecordPcm() {
        mRecordStarted.set(false);
        if (mAudioRecorder != null) {
            mAudioRecorder.stop();
        }
        if (null != mAudioRecordHandler) {
            mAudioRecordHandler.removeCallbacksAndMessages(null);
            mAudioRecordHandler = null;
        }
        if (null != mAudioRecordThread) {
            try {
                mAudioRecordThread.quit();
                mAudioRecordThread.interrupt();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                mAudioRecordThread = null;
            }
        }
    }

    /**
     * Start recording
     */
    public void startRecord() {
        sendStartRecordMessage();
    }

    /**
     * Stop recording
     */
    public void stopRecord() {
        sendStopRecordMessage();
    }

    public void release() {
        if (null != mAudioRecorder) {
            mAudioRecorder.release();
            mAudioRecorder = null;
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_START_RECORD:
                startRecordPcm();
                break;
        }
        return false;
    }

    public interface OnAudioRecordListener {
        /**
         * get a pcm data
         * @param pcm
         */
        void onAudioRecord(byte[] pcm);

        /**
         * record get error
         * @param message
         */
        void onAudioRecordError(String message);
    }
}
