package com.rtmpx.library.rtmp;

import android.media.MediaCodec;

import static android.media.MediaCodec.BUFFER_FLAG_KEY_FRAME;

public class RTMPFrame {
    private byte [] data;
    private int type;
    private long presentationTimeUs;
    private MediaCodec.BufferInfo mBufferInfo;

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public long getPresentationTimeUs() {
        return presentationTimeUs;
    }

    public void setPresentationTimeUs(long presentationTimeUs) {
        this.presentationTimeUs = presentationTimeUs;
    }

    public boolean isKeyFrame(){
        return  null != mBufferInfo && BUFFER_FLAG_KEY_FRAME == mBufferInfo.flags;
    }

    public MediaCodec.BufferInfo getBufferInfo() {
        return mBufferInfo;
    }

    public void setBufferInfo(MediaCodec.BufferInfo mBufferInfo) {
        this.mBufferInfo = mBufferInfo;
    }
}
