package com.rtmpx.library.encode;


import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.SystemClock;
import android.util.Log;

import com.rtmpx.library.config.Config;
import com.rtmpx.library.rtmp.RTMPFrame;
import com.rtmpx.library.publish.RTMPPublisher;

import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.rtmpx.library.config.Const.AUDIO_BITRATE;
import static com.rtmpx.library.config.Const.AUDIO_SAMPLE_RATE;
import static com.rtmpx.library.config.Const.CSD_0;
import static com.rtmpx.library.config.Const.FRAME_TYPE_AUDIO;
import static com.rtmpx.library.config.Const.MAX_BUFFER_SIZE;


public class AudioEncoder extends Encoder<byte[]>{
    private static final String TAG = "AudioEncoder";
    private MediaCodec mAsyncAudioCodec;
    private AtomicBoolean mEncodeStarted = new AtomicBoolean(false);
    private Queue<Integer> mIndexQueue = new ConcurrentLinkedDeque<>();
    private ByteBuffer mSps;


    public AudioEncoder(Config mConfig) {
        super(mConfig);
    }


    @Override
    public void config() throws Exception {
        try {
            mAsyncAudioCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            MediaFormat format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, AUDIO_SAMPLE_RATE, 2);
            format.setInteger(MediaFormat.KEY_BIT_RATE, AUDIO_BITRATE);//比特率
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, MAX_BUFFER_SIZE);
            mAsyncAudioCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mAsyncAudioCodec.setCallback(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void start() {
        mEncodedFrameCount = 0;
        if (null == mAsyncAudioCodec) {
            try {
                config();
                startEncoder();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            startEncoder();
        }
    }

    @Override
    public void encode(byte[] data) {
        if (null == mAsyncAudioCodec || !mEncodeStarted.get()) {
            Log.i(TAG, "====mAsyncAudioCodec not start====");
            return;
        }
        try {
            enCodeFrame(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void enCodeFrame(byte[] encodeBuffer) throws Exception {
        Integer index = mIndexQueue.poll();
        if (index == null) return;
        ByteBuffer inputBuffer = mAsyncAudioCodec.getInputBuffer(index);
        inputBuffer.clear();
        int length = 0;
        if (null != encodeBuffer) {
            inputBuffer.put(encodeBuffer);
            length = encodeBuffer.length;
            mEncodedFrameCount++;
            mAsyncAudioCodec.queueInputBuffer(index, 0, length, SystemClock.uptimeMillis()*1000, 0);
        }
    }

    private void startEncoder() {
        if (null == mAsyncAudioCodec || mEncodeStarted.get()) return;
        mAsyncAudioCodec.start();
        mEncodeStarted.set(true);
        Log.i(TAG, "====mAsyncAudioCodec.start====");
    }

    @Override
    public void stop() {
        super.stop();
        mIndexQueue.clear();
        try {
            if (null != mAsyncAudioCodec) {
                mAsyncAudioCodec.stop();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mAsyncAudioCodec = null;
        mEncodeStarted.set(false);
        mStartTime = 0;
    }

    @Override
    public void release() {
        mAsyncAudioCodec = null;
    }

    @Override
    public ByteBuffer getSps() {
        return mSps;
    }

    @Override
    public ByteBuffer getPps() {
        return null;
    }

    @Override
    public void onInputBufferAvailable(MediaCodec codec, int index) {
        mIndexQueue.add(index);
    }

    @Override
    public void onOutputBufferAvailable(MediaCodec codec, int index, MediaCodec.BufferInfo info) {
        try {
            handleOutput(codec, index, info);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleOutput(MediaCodec codec, int index, MediaCodec.BufferInfo info) throws Exception {
        ByteBuffer outputBuffer = codec.getOutputBuffer(index);
        if (null != outputBuffer && info.size > 0) {
            byte[] tmpBuffer = new byte[outputBuffer.remaining()];
            outputBuffer.get(tmpBuffer);
            RTMPFrame frame = new RTMPFrame();
            frame.setType(FRAME_TYPE_AUDIO);
            frame.setData(tmpBuffer);
            frame.setPresentationTimeUs(SystemClock.uptimeMillis() * 1000 - mStartTime);
            frame.setBufferInfo(info);
            RTMPPublisher.getInstance().addFrame(frame);
        }
        codec.releaseOutputBuffer(index, true);
    }

    @Override
    public void onError(MediaCodec codec, MediaCodec.CodecException e) {
        Log.i(TAG, "=======onError=======" + e.toString());
    }

    @Override
    public void onOutputFormatChanged(MediaCodec codec, MediaFormat format) {
        Log.i(TAG, "=======onOutputFormatChanged=======" + format.toString());
        mSps = format.getByteBuffer(CSD_0);
    }

}
