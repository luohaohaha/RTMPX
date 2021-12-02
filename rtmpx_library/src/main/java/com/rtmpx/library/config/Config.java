package com.rtmpx.library.config;

import static com.rtmpx.library.config.Const.RTMP_DEFAULT_BITRATE;
import static com.rtmpx.library.config.Const.RTMP_DEFAULT_FRAME_RATE;
import static com.rtmpx.library.config.Const.RTMP_DEFAULT_VIDEO_HEIGHT;
import static com.rtmpx.library.config.Const.RTMP_DEFAULT_VIDEO_WIDTH;
import static com.rtmpx.library.config.Const.RTMP_I_FRAME_INTERVAL;
import static com.rtmpx.library.config.Const.RTMP_TEST_PUSH_URL;

public class Config {
    /**
     * bitrate
     */
    private int mBitRate = RTMP_DEFAULT_BITRATE;//default 2m
    /**
     * fps frame rate
     */
    private int mFrameRate = RTMP_DEFAULT_FRAME_RATE; //default 30fps
    /**
     *  GOP
     */
    private int mIFrameInterval = RTMP_I_FRAME_INTERVAL; //default 2s
    /**
     * preview width
     */
    private int mVideoWidth = RTMP_DEFAULT_VIDEO_WIDTH;

    /**
     * preview height
     */
    private int mVideoHeight = RTMP_DEFAULT_VIDEO_HEIGHT;

    /**
     * publish url
     */
    private String mPublishUrl = RTMP_TEST_PUSH_URL;

    /**
     * video record
     */
    private boolean mRecordVideo = false;
    /**
     * video save path
     */
    private String mRecordVideoPath;


    private Config() {

    }

    public int getBitRate() {
        return mBitRate;
    }

    public int getFrameRate() {
        return mFrameRate;
    }

    public int getIFrameInterval() {
        return mIFrameInterval;
    }

    public int getVideoWidth() {
        return mVideoWidth;
    }

    public int getVideoHeight() {
        return mVideoHeight;
    }

    public String getPublishUrl() {
        return mPublishUrl;
    }

    public boolean isRecordVideo() {
        return mRecordVideo;
    }

    public String getRecordVideoPath() {
        return mRecordVideoPath;
    }


    public static final class ConfigBuilder {
        /**
         * bitrate
         */
        private int mBitRate = RTMP_DEFAULT_BITRATE;//default 2m
        /**
         * fps frame rate
         */
        private int mFrameRate = RTMP_DEFAULT_FRAME_RATE; //default 30fps
        /**
         *  GOP
         */
        private int mIFrameInterval = RTMP_I_FRAME_INTERVAL; //default 10s
        /**
         * preview width
         */
        private int mVideoWidth = RTMP_DEFAULT_VIDEO_WIDTH;

        /**
         * preview height
         */
        private int mVideoHeight = RTMP_DEFAULT_VIDEO_HEIGHT;

        /**
         * publish url
         */
        private String mPublishUrl = RTMP_TEST_PUSH_URL;

        /**
         * video record
         */
        private boolean mRecordVideo = false;
        /**
         * video save path
         */
        private String mRecordVideoPath;

        public ConfigBuilder() {
        }

        public ConfigBuilder withBitRate(int mBitRate) {
            this.mBitRate = mBitRate;
            return this;
        }

        public ConfigBuilder withFrameRate(int mFrameRate) {
            this.mFrameRate = mFrameRate;
            return this;
        }

        public ConfigBuilder withIFrameInterval(int mIFrameInterval) {
            this.mIFrameInterval = mIFrameInterval;
            return this;
        }

        public ConfigBuilder withVideoWidth(int mVideoWidth) {
            this.mVideoWidth = mVideoWidth;
            return this;
        }

        public ConfigBuilder withVideoHeight(int mVideoHeight) {
            this.mVideoHeight = mVideoHeight;
            return this;
        }

        public ConfigBuilder withPublishUrl(String mPublishUrl) {
            this.mPublishUrl = mPublishUrl;
            return this;
        }
        public ConfigBuilder withRecordVideo(boolean mRecordVideo) {
            this.mRecordVideo = mRecordVideo;
            return this;
        }
        public ConfigBuilder withRecordVideoPath(String mRecordVideoPath) {
            this.mRecordVideoPath = mRecordVideoPath;
            return this;
        }

        public Config build() {
            Config vRConfig = new Config();
            vRConfig.mIFrameInterval = this.mIFrameInterval;
            vRConfig.mFrameRate = this.mFrameRate;
            vRConfig.mVideoWidth = this.mVideoWidth;
            vRConfig.mVideoHeight = this.mVideoHeight;
            vRConfig.mPublishUrl = this.mPublishUrl;
            vRConfig.mBitRate = this.mBitRate;
            vRConfig.mRecordVideo = this.mRecordVideo;
            vRConfig.mRecordVideoPath = this.mRecordVideoPath;

            return vRConfig;
        }
    }

    @Override
    public String toString() {
        return "Config{" + "mBitRate=" + mBitRate + ", mFrameRate=" + mFrameRate + ", mIFrameInterval=" + mIFrameInterval + ", mVideoWidth=" + mVideoWidth + ", mVideoHeight=" + mVideoHeight + ", mPublishUrl='" + mPublishUrl + '\'' + ", mRecordVideo=" + mRecordVideo + ", mRecordVideoPath='" + mRecordVideoPath + '\'' + '}';
    }
}
