package com.rtmpx.library.config;

public class Const {

    public static final int FRAME_TYPE_VIDEO = 0;
    public static final int FRAME_TYPE_AUDIO = 1;

    public static final int MAX_BUFFER_SIZE = 8192;
    public static final int AUDIO_SAMPLE_RATE = 44100;
    public static final int AUDIO_BITRATE = 96000;

    public static final int RTMP_DEFAULT_BITRATE = 2 * 1024 * 1000;//default 2m
    public static final int RTMP_DEFAULT_FRAME_RATE = 30;//default 30fps
    public static final int RTMP_I_FRAME_INTERVAL = 2;//default 10s
    public static final int RTMP_DEFAULT_VIDEO_WIDTH = 1920;
    public static final int RTMP_DEFAULT_VIDEO_HEIGHT = 1080;
    public static final String RTMP_TEST_PUSH_URL = "";

    public static final String CSD_0 = "csd-0";
    public static final String CSD_1 = "csd-1";
}
