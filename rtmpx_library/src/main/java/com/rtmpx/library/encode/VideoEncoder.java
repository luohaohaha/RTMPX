package com.rtmpx.library.encode;


import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.SystemClock;
import android.util.Log;

import com.rtmpx.library.config.Config;
import com.rtmpx.library.rtmp.RTMPFrame;
import com.rtmpx.library.publish.RTMPPublisher;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileHigh;
import static android.media.MediaCodecList.REGULAR_CODECS;
import static android.media.MediaFormat.KEY_LEVEL;
import static android.media.MediaFormat.MIMETYPE_VIDEO_AVC;
import static com.rtmpx.library.config.Const.CSD_0;
import static com.rtmpx.library.config.Const.CSD_1;
import static com.rtmpx.library.config.Const.FRAME_TYPE_VIDEO;

public class VideoEncoder extends Encoder<byte[]>{
    private static final String TAG = "VideoEncoder";
    private MediaCodec mAsyncVideoCodec;
    private AtomicBoolean mEncodeStarted = new AtomicBoolean(false);
    private Queue<Integer> mIndexQueue = new ConcurrentLinkedDeque<>();

    private ByteBuffer mSps;
    private ByteBuffer mPps;


    public VideoEncoder(Config mConfig) {
        super(mConfig);
    }

    @Override
    public void config() throws Exception {
        try {
            mAsyncVideoCodec = MediaCodec.createEncoderByType(MIMETYPE_VIDEO_AVC);
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(MIMETYPE_VIDEO_AVC, mConfig.getVideoWidth(), mConfig.getVideoHeight());
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mConfig.getBitRate());
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mConfig.getFrameRate());
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, getOptimalFormat(MIMETYPE_VIDEO_AVC));
            MediaCodecInfo.CodecCapabilities capabilities = mAsyncVideoCodec.getCodecInfo().getCapabilitiesForType(MIMETYPE_VIDEO_AVC);
            MediaCodecInfo.CodecProfileLevel[] codecProfileLevels = capabilities.profileLevels;
            int leve = -1;
            int profile = -1;
            if (null != codecProfileLevels) {
                List<MediaCodecInfo.CodecProfileLevel> highProfileLevelList = Collections.synchronizedList(new LinkedList<>());
                for (MediaCodecInfo.CodecProfileLevel profileLevel : codecProfileLevels) {
                    if (AVCProfileHigh == profileLevel.profile) {
                        Log.i(TAG, "add high profile level " + "[profile = " + profileLevel.profile + ", level = " + profileLevel.level);
                        highProfileLevelList.add(profileLevel);
                    }
                }
                if (!highProfileLevelList.isEmpty()) {
                    Collections.sort(highProfileLevelList, (o1, o2) -> (o2.profile - o1.profile + o2.level - o1.level));
                    leve = highProfileLevelList.get(0).level;
                    profile = highProfileLevelList.get(0).profile;
                }
            }
            /**There are improvements here, but there are no qualitative changes**/
            if (-1 != leve) {
                Log.i(TAG, "select level is " + leve);
                mediaFormat.setInteger(KEY_LEVEL, leve);
            }
            if (-1 != profile) {
                Log.i(TAG, "select profile is " + profile);
                mediaFormat.setInteger(MediaFormat.KEY_PROFILE, profile);
            }
            /**There are improvements here, but there are no qualitative changes**/
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, mConfig.getIFrameInterval());
            mAsyncVideoCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mAsyncVideoCodec.setCallback(this);
            Log.i(TAG, "mediaFormat is " + mediaFormat.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Obtain the optimal encoding format to prevent the uv data from being disordered after encoding
     * @param mime default {@link MediaFormat#MIMETYPE_VIDEO_AVC}
     * @return
     */
    private int getOptimalFormat(String mime) {
        MediaCodecList list = new MediaCodecList(REGULAR_CODECS);
        MediaCodecInfo[] infos = list.getCodecInfos();
        MediaCodecInfo.CodecCapabilities cap = null;
        for (int i = 0, count = infos.length; i < count; i++) {
            MediaCodecInfo info = infos[i];
            if (!info.isEncoder())
                continue;
            String[] types = info.getSupportedTypes();
            for (int j = 0, jCount = types.length; j < jCount; j++) {
                String type = types[j];
                if (type.equals(mime)) {
                    cap = info.getCapabilitiesForType(mime);
                    break;
                }
            }
        }
        int color_formatYUV420Flexible = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible;
        if (null == cap) {
            Log.i(TAG, "cap is null select default COLOR_FormatYUV420Flexible");
            return color_formatYUV420Flexible;
        }

        for (int i = 0; i < cap.colorFormats.length; i++) {
            if (cap.colorFormats[i] == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) {
                Log.i(TAG, "select COLOR_FormatYUV420SemiPlanar");
                return cap.colorFormats[i];
            }
        }
        Log.i(TAG, "no match , select default COLOR_FormatYUV420Flexible");
        return color_formatYUV420Flexible;
    }

    public void setStartTime(long startTime){
        this.mStartTime = startTime;
    }

    @Override
    public void start() {
        mEncodedFrameCount = 0;
        if (null == mAsyncVideoCodec) {
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
        if (null == mAsyncVideoCodec || !mEncodeStarted.get()) {
            Log.i(TAG, "====mAsyncVideoCodec not start====");
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
        ByteBuffer inputBuffer = mAsyncVideoCodec.getInputBuffer(index);
        inputBuffer.clear();
        int length = 0;
        if (null != encodeBuffer) {
            inputBuffer.put(encodeBuffer);
            length = encodeBuffer.length;
            mEncodedFrameCount++;
            mAsyncVideoCodec.queueInputBuffer(index, 0, length, SystemClock.uptimeMillis()*1000, 0);
        }
    }

    private void startEncoder() {
        if (null == mAsyncVideoCodec || mEncodeStarted.get()) return;
        mAsyncVideoCodec.start();
        mEncodeStarted.set(true);
        Log.i(TAG, "====mAsyncVideoCodec.start====");
    }

    @Override
    public void stop() {
        super.stop();
        mIndexQueue.clear();
        try {
            if (null != mAsyncVideoCodec) {
                mAsyncVideoCodec.stop();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
            mAsyncVideoCodec = null;
            mEncodeStarted.set(false);
            mStartTime = 0;
    }

    @Override
    public void release() {
        mAsyncVideoCodec = null;
    }

    @Override
    public ByteBuffer getSps() {
        return mSps;
    }

    @Override
    public ByteBuffer getPps() {
        return mPps;
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
            frame.setType(FRAME_TYPE_VIDEO);
            frame.setData(tmpBuffer);
            frame.setPresentationTimeUs(SystemClock.uptimeMillis() * 1000  - mStartTime);
            frame.setBufferInfo(info);
            RTMPPublisher.getInstance().addFrame(frame);
            Log.i(TAG, "mPresentationTimeUs == " + info.presentationTimeUs);
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
        mPps = format.getByteBuffer(CSD_1);
    }

}
