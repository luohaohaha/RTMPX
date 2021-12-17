# RTMPX

[English doc][1]

[1]: https://github.com/luohaohaha/RTMPX/blob/dev/README_en.md

RTMPX是一个android的rtmp推流库，采集使用camerax，支持60fps，编码使用mediacodec硬编码，推流使用了librtmp。

https://user-images.githubusercontent.com/3376376/145600890-4abd9b30-ba70-4f04-b1ca-1409be121838.mp4


https://user-images.githubusercontent.com/3376376/145600918-9c88f7b4-5ed5-4bde-9ec9-36c10fa07d81.mp4


### 支持的功能
- [x] 60帧预览、推流(理论上支持更高，只要手机支持，目前pixel2最高设置240fps，但是没效果)
- [x] 边推流边录制(保存到本地)

### 已知问题
- [x] 部分机型在竖屏推流60fps的时候性能不够，导致libyuv旋转需要很长时间(例如pixel2，I帧旋转要12ms)，导致达不到60fps
- [x] 等你发现

### 待优化
- [ ] 添加注释
- [ ] 支持滤镜
- [ ] 竖屏libyuv旋转时长优化

### 怎么使用(参考示例app)
#### 1. 依赖

##### 1) 下载aar(这种集成aar文件的方式，camerax库是必须依赖)
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

##### 2) mavenCentral远程依赖
```
implementation 'io.github.luohaohaha:rtmpx:latest'
```


#### 2. 将CameraXImplView 放入布局

```
...
 <com.rtmpx.library.camera.widget.CameraXImplView
        android:id="@+id/preview"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
...
```

#### 3. 设置推流配置
```
  Config config = new Config.ConfigBuilder()
                .withBitRate(1000 * 5000) //码率
                .withPublishUrl("rtmp://192.168.50.170:18888/test/live") //推流url
                .withFrameRate(60)//帧率
                .withVideoWidth(1080)//视频宽 
                .withVideoHeight(1920) //视频高
                .withRecordVideo(false)//是否录制
                .withRecordVideoPath("sdcard/dump.mp4")//录制文件保存文件
                .build();
```

#### 4. 绑定预览控件

```
 CameraXImplView mPreview = findViewById(R.id.preview);
 mPreview.setPreviewRange(mConfig.getFrameRate(),mConfig.getFrameRate());
 mPreview.setTargetResolution(mConfig.getVideoWidth(), mConfig.getVideoHeight());
 
 PublisherX mPublisher = new PublisherX(mConfig);
 mPublisher.bindCamera(mPreview);
 mPublisher.setPublishListener(this);
 
```

#### 5. 开启预览 & 开始推流 / 停止预览 & 停止推流

```
mPreview.startPreview();
mPublisher.startPublish();
```

or

```
mPreview.stopPreview();
mPublisher.stopPublish();
```




### IPublishListener 回调

```
    /**
     * rtmp连接中
     */
    void onConnecting();

    /**
     * rtmp 连接建立成功
     */
    void onConnected();

    /**
     * rtmp 连接失败
     * @param code error code 失败错误码
     */
    void onConnectedFailed(int code);

    /**
     * 开始推流
     */
    void onStartPublish();

    /**
     * 结束推流
     */
    void onStopPublish();

    /**
     * 开始录制
     */
    void onStartRecord();

    /**
     * 结束录制
     */
    void onStopRecord();

    /**
     * 发送平均帧率统计(带宽不足的情况下会低于设置帧率)
     * @param fps avg fps
     */
    void onFpsStatistic(int fps);

    /**
     * rtmp 断开连接
     */
    void onRtmpDisconnect();
```



## Thanks
[LibRtmp-Client-for-Android][2]

[2]: https://github.com/ant-media/LibRtmp-Client-for-Android

