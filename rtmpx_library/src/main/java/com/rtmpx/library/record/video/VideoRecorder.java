package com.rtmpx.library.record.video;

import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.text.TextUtils;
import android.util.Log;

import com.rtmpx.library.config.Config;
import com.rtmpx.library.rtmp.RTMPFrame;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.rtmpx.library.config.Const.AUDIO_SAMPLE_RATE;
import static com.rtmpx.library.config.Const.CSD_0;
import static com.rtmpx.library.config.Const.CSD_1;

public class VideoRecorder {
    private static final String TAG = "VideoRecorder";
    private MediaMuxer mediaMuxer;
    private Config mConfig;
    private ByteBuffer sps, pps, adts;
    private int mAudioTrackIndex;
    private int mVideoTrackIndex;
    private AtomicBoolean muxerStarted = new AtomicBoolean(false);

    public VideoRecorder(Config mVrConfig, ByteBuffer sps, ByteBuffer pps, ByteBuffer adts) {
        this.mConfig = mVrConfig;
        this.sps = sps;
        this.pps = pps;
        this.adts = adts;
    }

    /**
     * Start recording
     * @param targetPath Recording file save path
     * @throws Exception
     */
    public void startRecord(String targetPath) throws Exception {
        if (TextUtils.isEmpty(targetPath)) {
            Log.d(TAG, "record path is null");
            return;
        }
        startRecord(new File(targetPath));
    }

    /**
     * Start recording
     * @param targetFile Recording file
     * @throws Exception
     */
    public void startRecord(File targetFile) throws Exception {
        File dir = targetFile.getParentFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        if (!targetFile.exists()) {
            targetFile.createNewFile();
        }
        if (null == mediaMuxer) {
            createMuxer(targetFile);
        }
        mediaMuxer.start();
        muxerStarted.set(true);
    }

    /**
     * Stop recording
     */
    public void stopRecord() {
        if (null != mediaMuxer) {
            mediaMuxer.stop();
        }
    }

    /**
     * freed
     */
    public void release() {
        if (null != mediaMuxer) {
            mediaMuxer.release();
        }
        muxerStarted.set(false);
    }

    /**
     * Has it started
     * @return true if started
     */
    public boolean isStart(){
        return  muxerStarted.get();
    }

    /**
     *  create a mp4 muxer
     * @param target
     * @throws Exception
     */
    private void createMuxer(File target) throws Exception {
        mediaMuxer = new MediaMuxer(target.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        MediaFormat videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, mConfig.getVideoWidth(), mConfig.getVideoHeight());
        videoFormat.setByteBuffer(CSD_0, sps);
        videoFormat.setByteBuffer(CSD_1, pps);
        videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar);
        videoFormat.setInteger(MediaFormat.KEY_CAPTURE_RATE, mConfig.getFrameRate());
        MediaFormat audioFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, AUDIO_SAMPLE_RATE, 1);
        audioFormat.setByteBuffer(CSD_0, adts);
        mAudioTrackIndex = mediaMuxer.addTrack(audioFormat);
        mVideoTrackIndex = mediaMuxer.addTrack(videoFormat);
    }

    /**
     * Write video data
     * @param rtmpFrame video frame
     */
    public void writeVideo(RTMPFrame rtmpFrame) {
        if (null == mediaMuxer || !muxerStarted.get()) return;
        mediaMuxer.writeSampleData(mVideoTrackIndex, ByteBuffer.wrap(rtmpFrame.getData()), rtmpFrame.getBufferInfo());
    }

    /**
     * Write audio data
     * @param rtmpFrame audio frame
     */
    public void writeAudio(RTMPFrame rtmpFrame) {
        if (null == mediaMuxer || !muxerStarted.get()) return;
        mediaMuxer.writeSampleData(mAudioTrackIndex, ByteBuffer.wrap(rtmpFrame.getData()), rtmpFrame.getBufferInfo());
    }
}
