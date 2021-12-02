package com.rtmpx.library.encode;

import android.media.MediaCodec;
import android.media.MediaFormat;

import com.rtmpx.library.config.Config;

import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class Encoder<T> extends MediaCodec.Callback {
    protected Config mConfig;
    protected int mEncodedFrameCount = 0;
    protected  long mStartTime;

    /**
     * Construction method
     * @param mConfig publish params config {@link Config}
     */
    public Encoder(Config mConfig) {
        this.mConfig = mConfig;
    }

    /**
     *  encode video or audio
     * @param data the raw data
     */
    public abstract void encode(T data);

    /**
     * base time
     * @param startTime
     */
    public void setStartTime(long startTime){
        this.mStartTime = startTime;
    }

    /**
     * configuration codec
     * @throws Exception
     */
    public abstract void config() throws Exception;

    /**
     * start encode
     */
    public abstract  void start();

    /**
     * stop encode
     */
    public   void stop(){
     mEncodedFrameCount = 0;
    }

    /**
     * freed
     */
    public abstract void release();

    /**
     *  get sps
     * @return {@link MediaCodec.Callback#onOutputFormatChanged(MediaCodec, MediaFormat)} {@link MediaFormat#getByteBuffer(String)} {@link com.rtmpx.library.config.Const#CSD_0}
     */
    public abstract ByteBuffer getSps();

    /**
     * get pps audio is null
     * @return @return {@link MediaCodec.Callback#onOutputFormatChanged(MediaCodec, MediaFormat)} {@link MediaFormat#getByteBuffer(String)} {@link com.rtmpx.library.config.Const#CSD_1}
     */
    public abstract ByteBuffer getPps();
}
