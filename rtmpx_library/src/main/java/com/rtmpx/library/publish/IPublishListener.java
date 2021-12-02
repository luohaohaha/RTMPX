package com.rtmpx.library.publish;

public interface IPublishListener {

    /**
     * rtmp is connecting
     */
    void onConnecting();

    /**
     * rtmp connection is successful
     */
    void onConnected();

    /**
     * rtmp connection failed
     * @param code error code
     */
    void onConnectedFailed(int code);

    /**
     * Start publishing
     */
    void onStartPublish();

    /**
     * Stop publishing
     */
    void onStopPublish();

    /**
     * Start recording
     */
    void onStartRecord();

    /**
     * Stop recording
     */
    void onStopRecord();

    /**
     * fps statistics
     * @param fps avg fps
     */
    void onFpsStatistic(int fps);

    /**
     * rtmp disconnect
     */
    void onRtmpDisconnect();
}
