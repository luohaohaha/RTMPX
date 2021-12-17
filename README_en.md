# RTMPX


RTMPX is an android rtmp streaming library. It uses camerax for acquisition and supports 60fps. The encoding uses mediacodec hard coding, and the streaming uses librtmp.



https://user-images.githubusercontent.com/3376376/145600890-4abd9b30-ba70-4f04-b1ca-1409be121838.mp4


https://user-images.githubusercontent.com/3376376/145600918-9c88f7b4-5ed5-4bde-9ec9-36c10fa07d81.mp4


### Features
- [x] 60-frame preview and streaming (theoretically support higher, as long as the phone supports it, the current pixel2 is set to 240fps, but it has no effect)
- [x] Streaming while recording (save to local)

### Known issues
- [x] Some models have insufficient performance when pushing 60fps in the vertical screen, which causes libyuv rotation to take a long time (for example, pixel2, I frame rotation takes 12ms), resulting in failure to reach 60fps
- [x] Waiting for you to find out

### To be optimized
- [ ] add notes
- [ ] support filters
- [ ] optimization of vertical screen libyuv rotation

### How to use(Refer to sample app)
#### 1. Depend

##### 1) Download aar (this way of integrating aar files, the camerax library is a must)
```
  def camerax_version = "1.0.0"
// CameraX core library using camera2 implementation
    implementation "androidx.camera:camera-camera2:$camerax_version"
// CameraX Lifecycle Library
    implementation "androidx.camera:camera-lifecycle:$camerax_version"
// CameraX View class
    implementation "androidx.camera:camera-view:1.0.0-alpha24"
```
or 

##### 2) mavenCentral()
```
implementation 'io.github.luohaohaha:rtmpx:latest'
```


#### 2. Put CameraXImplView in the xml layout

```
...
 <com.rtmpx.library.camera.widget.CameraXImplView
        android:id="@+id/preview"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
...
```

#### 3. set publish config
```
  Config config = new Config.ConfigBuilder()
                .withBitRate(1000 * 5000) //bitrate
                .withPublishUrl("rtmp://192.168.50.170:18888/test/live") //publish url
                .withFrameRate(60)//fps
                .withVideoWidth(1080)//width 
                .withVideoHeight(1920) //height
                .withRecordVideo(false)//record video
                .withRecordVideoPath("sdcard/dump.mp4")//path
                .build();
```

#### 4. bind camera preview

```
 CameraXImplView mPreview = findViewById(R.id.preview);
 mPreview.setPreviewRange(mConfig.getFrameRate(),mConfig.getFrameRate());
 mPreview.setTargetResolution(mConfig.getVideoWidth(), mConfig.getVideoHeight());
 
 PublisherX mPublisher = new PublisherX(mConfig);
 mPublisher.bindCamera(mPreview);
 mPublisher.setPublishListener(this);
 
```

#### 5. start preview & start publish 

```
mPreview.startPreview();
mPublisher.startPublish();
```

or

```
mPreview.stopPreview();
mPublisher.stopPublish();
```




### IPublishListener Callback

```
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
```



## Thanks
[LibRtmp-Client-for-Android][1]

[1]: https://github.com/ant-media/LibRtmp-Client-for-Android

