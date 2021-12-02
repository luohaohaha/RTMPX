# RTMPX


This is a project of librtmp & camerax & mediacodec for rtmp publish


### Features
- [x] camerax 60 fps preview and record and publish(only camera2)
- [x] Publish and record
- [x] android mediacodec encode

### Known issues
- [x] In some devices, when the vertical screen is 60fps, the libyuv rotation time is too long(More than 16ms), which causes the fps to reach 60 (e.g. Google Pixel2)
- [x] Waiting for you to find out

### To be optimized
- [ ] add notes
- [ ] support filters
- [ ] optimization of vertical screen libyuv rotation

### How to use(Refer to sample app)
#### 1. Necessary dependence camerax
```
  def camerax_version = "1.0.0"
// CameraX core library using camera2 implementation
    api "androidx.camera:camera-camera2:$camerax_version"
// CameraX Lifecycle Library
    api "androidx.camera:camera-lifecycle:$camerax_version"
// CameraX View class
    api "androidx.camera:camera-view:1.0.0-alpha24"
```

#### 2.Put CameraXImplView in the xml layout

```
...
 <com.rtmpx.library.camera.widget.CameraXImplView
        android:id="@+id/preview"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
...
```

#### 3.set publish config
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

#### 4.bind camera preview

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








## Thanks
[LibRtmp-Client-for-Android][1]

[1]: https://github.com/ant-media/LibRtmp-Client-for-Android

