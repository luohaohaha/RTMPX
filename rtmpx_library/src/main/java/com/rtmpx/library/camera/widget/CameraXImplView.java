package com.rtmpx.library.camera.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.display.DisplayManager;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.camera2.interop.Camera2CameraControl;
import androidx.camera.camera2.interop.Camera2Interop;
import androidx.camera.camera2.interop.CaptureRequestOptions;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.FocusMeteringResult;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.rtmpx.library.camera.ICamera;
import com.rtmpx.library.camera.ICameraPreviewCallback;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.rtmpx.library.config.Const.RTMP_DEFAULT_FRAME_RATE;
import static com.rtmpx.library.config.Const.RTMP_DEFAULT_VIDEO_HEIGHT;
import static com.rtmpx.library.config.Const.RTMP_DEFAULT_VIDEO_WIDTH;


public class CameraXImplView extends FrameLayout implements ICamera {

    private static final String TAG = "VRCameraViewCameraXImpl";

    /**
     * ViewFinder
     */
    private PreviewView mFinderView;
    private int mDisplayId = -1;
    /**
     * the back camera or front
     * {@link CameraSelector#LENS_FACING_BACK } or {@link CameraSelector#LENS_FACING_FRONT}
     */
    private int mLensFacing = CameraSelector.LENS_FACING_BACK;
    private DisplayManager mDisplayManager;
    /**
     * thread pool
     */
    private ExecutorService mCameraExecutor;
    /**
     * camera rotation listener
     */
    private DisplayManager.DisplayListener mDisplayListener = new DisplayManager.DisplayListener() {
        @Override
        public void onDisplayAdded(int displayId) {

        }

        @Override
        public void onDisplayRemoved(int displayId) {

        }

        @Override
        public void onDisplayChanged(int displayId) {
            if (displayId == mDisplayId) {
                Log.i(TAG, "Rotation changed: " + getDisplay().getRotation());
                if (mImageAnalyzer != null) {
                    mImageAnalyzer.setTargetRotation(getDisplay().getRotation());
                }
            }
        }
    };

    private ProcessCameraProvider mCameraProvider = null;

    /**
     * Preview
     */
    private Preview mPreview = null;
    /**
     * yuv raw data callback
     */
    private ImageAnalysis mImageAnalyzer = null;

    /**
     * camera
     */
    private Camera mCamera = null;

    /** ============================ default ================================ **/

    private Size mTargetResolution = new Size(RTMP_DEFAULT_VIDEO_HEIGHT, RTMP_DEFAULT_VIDEO_WIDTH); //default resolution 1080p
    private Range<Integer> DEFAULT_PREVIEW_RANGE = new Range(RTMP_DEFAULT_FRAME_RATE, RTMP_DEFAULT_FRAME_RATE); // default preview range 30 fps
    private Range<Integer> mPreviewRange = DEFAULT_PREVIEW_RANGE;

    /** ============================ default ================================ **/
    private CameraSelector mCameraSelector;
    private ICameraPreviewCallback mPreviewCallback;
    private AtomicBoolean mAutoExposure = new AtomicBoolean(true);
    private AtomicBoolean mAutoFocus = new AtomicBoolean(true);

    public CameraXImplView(@NonNull Context context) {
        this(context, null);
    }

    public CameraXImplView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraXImplView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initViews(context);
    }

    private void initViews(Context context) {
        mFinderView = new PreviewView(context);
        addView(mFinderView, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        //register display listener
        mDisplayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        if (mDisplayManager != null) {
            mDisplayManager.registerDisplayListener(mDisplayListener, null);
        }
        //init thread pool , from camerax sample
        mCameraExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setUpCamera(getContext());
    }

    private void setUpCamera(Context context) {
        //from camerax sample (java1.8)
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(context);

        cameraProviderFuture.addListener(() -> {
            // CameraProvider
            try {
                mCameraProvider = cameraProviderFuture.get();
                switch (mLensFacing) {
                    case CameraSelector.LENS_FACING_BACK:
                        //check has back camera
                        hasBackCamera();
                        break;
                    case CameraSelector.LENS_FACING_FRONT:
                        //check has front camera
                        hasFrontCamera();
                        break;
                    default:
                        throw new IllegalStateException("Back and front camera are unavailable");
                }
                postDelayed(() -> setAnalyzer(), 500);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }


        }, ContextCompat.getMainExecutor(context));

    }

    /**
     * set preview resolution default 1080p
     * @param targetSize
     */
    public void setTargetResolution(Size targetSize) {
        if (null == targetSize) return;
        if (targetSize.equals(mTargetResolution)) return;
        this.mTargetResolution = targetSize;
    }

    /**
     *set preview resolution default 1080p {@link CameraXImplView#setTargetResolution(Size)}
     * @param targetWidth  preview width
     * @param targetHeight preview height
     */
    public void setTargetResolution(int targetWidth, int targetHeight) {
        setTargetResolution(new Size(targetWidth, targetHeight));
    }

    /**
     *  set preview range default 30 fps
     * @param mPreviewRange {@link CameraXImplView#getOptimalPreviewRange()}
     */
    public void setPreviewRange(Range mPreviewRange) {
        this.mPreviewRange = mPreviewRange;
    }

    /**
     * set preview range default 30 fps {@link CameraXImplView#setPreviewRange(Range)}
     * @param mLower min preview fps
     * @param mUpper max preview fps
     */
    public void setPreviewRange(int mLower, int mUpper) {
        setPreviewRange(new Range(mLower, mUpper));
    }

    @SuppressLint("RestrictedApi")
    private void setAnalyzer() {
        //get rotation angle
        int rotation = getDisplay().getRotation();

        if (mCameraProvider == null) {
            throw new IllegalStateException("Camera initialization failed.");
        }

        createSelector();
        //preview
        mPreview = new Preview.Builder().setTargetResolution(mTargetResolution).setTargetRotation(rotation).build();


        // yuv raw data callback
        ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
        Camera2Interop.Extender ext = new Camera2Interop.Extender<>(builder);
//        ext.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
/**
 In fact, most mobile phones support 60fps preview, but the number of supported preview frames is not 60fps
 I don't have more devices to get data samples
 The number of preview frames commonly obtained is [30, 30], [25, 25], [14, 30], [20, 20], [14, 25], [14, 20], [15, 15], [12, 15]
 Google Pixel2  is  [60, 60], [7, 60], [30, 30], [24, 24], [7, 30], [15, 15]
 HUAWEI P40 is [60, 60], [30, 60], [15, 60], [30, 30], [25, 25], [24, 24], [14, 30], [20, 20], [14, 25], [14, 20], [15, 15], [12, 15]
 HUAWEI Mate30 is [60, 60], [30, 60], [15, 60], [30, 30], [25, 25], [24, 24], [14, 30], [20, 20], [14, 25], [14, 20], [15, 15], [12, 15]
 **/
        //TODO Get the correct number of supported preview frames
//        mPreviewRange = getNiceRange();
        Log.i(TAG, "mPreviewRange is " + mPreviewRange.toString());
        ext.setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, mPreviewRange);
        mImageAnalyzer = builder.setTargetResolution(mTargetResolution).setTargetRotation(rotation).build();
        mImageAnalyzer.setAnalyzer(mCameraExecutor, new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy image) {
                if (null == mPreviewCallback) {
                    image.close();
                    return;
                }
                mPreviewCallback.onPreviewFrame(image,rotation);
            }
        });
    }

    /**
     *  get camera supported preview fps
     *  But the highest obtained is 30 fps
     * @return {@link CameraXImplView#setPreviewRange(Range)}
     */
    private Range<Integer> getOptimalPreviewRange() {
        CameraCharacteristics chars = null;
        try {
            CameraManager mCameraManager = (CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE);
            String[] cameraList = mCameraManager.getCameraIdList();
            if (null == cameraList || 0 == cameraList.length)
                return DEFAULT_PREVIEW_RANGE;
            for (String id : cameraList) {
                chars = mCameraManager.getCameraCharacteristics(id);
                if (mLensFacing == chars.get(CameraCharacteristics.LENS_FACING))
                    break;
            }
            if (null == chars)
                return DEFAULT_PREVIEW_RANGE;
            Range<Integer>[] ranges = chars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
            if (null == ranges || 0 == ranges.length)
                return DEFAULT_PREVIEW_RANGE;
            Arrays.sort(ranges, (o1, o2) -> o2.getLower() - o1.getLower() + o2.getUpper() - o1.getUpper());
            Log.i(TAG, "ranges is " + Arrays.toString(ranges));
            Range<Integer> niceRange = null;
            for (Range r : ranges) {
                if (r.getLower() == r.getUpper() && r.getUpper() == mPreviewRange.getUpper()) {
                    niceRange = r;
                    break;
                }
            }
            if (null == niceRange) {
                return (mPreviewRange.getUpper() > DEFAULT_PREVIEW_RANGE.getUpper()) ? ranges[0] : DEFAULT_PREVIEW_RANGE;
            } else {
                return niceRange;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return DEFAULT_PREVIEW_RANGE;
        }
    }

    private void createSelector() {
        mCameraSelector = new CameraSelector.Builder().requireLensFacing(mLensFacing).build();
    }

    /**
     *  check has back camera id
     * @return true if has
     */
    private boolean hasBackCamera() {
        if (mCameraProvider != null) {
            try {
                mCameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA);
                return true;
            } catch (CameraInfoUnavailableException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    /**
     *  check has front camera id
     * @return  true if has
     */
    private boolean hasFrontCamera() {
        if (mCameraProvider != null) {
            try {
                mCameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA);
                return true;
            } catch (CameraInfoUnavailableException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    private void bindCameraUseCases() {

        //Unbind before binding
        mCameraProvider.unbindAll();
        try {
            //Life cycle binding
//            mCamera = mCameraProvider.bindToLifecycle((LifecycleOwner) getContext(), mCameraSelector, mPreview,mVideoCapture,mImageAnalyzer); //Binding use cases, after testing, most mobile phones do not support 3 or more combined use cases , but Google Pixel2 supported
            mCamera = mCameraProvider.bindToLifecycle((LifecycleOwner) getContext(), mCameraSelector, mPreview, mImageAnalyzer);

            //Preview on ViewFinder
            if (mPreview != null) {
                mPreview.setSurfaceProvider(mFinderView.getSurfaceProvider());
            }

        } catch (Exception exc) {
            Log.e(TAG, "Use case binding failed", exc);
        }
    }


    @Override
    protected void onDetachedFromWindow() {
        stopPreview();
        release();
        super.onDetachedFromWindow();
    }

    @Override
    public void stopPreview() {

        //unbind UseCase
        if (null != mCameraProvider) {
            mCameraProvider.unbind(mImageAnalyzer);
            mCameraProvider.unbind(mPreview);
        }
    }


    @Override
    public void startPreview() {
        if (null == mCameraProvider || null == mCameraSelector) {
            postDelayed(() -> startPreview(), 1000);
            return;
        }
        bindCameraUseCases();
    }

    @Override
    public float getMaxZoom() {
        if (null == mCamera) return 0;
        return mCamera.getCameraInfo().getZoomState().getValue().getMaxZoomRatio();
    }

    @Override
    public float getMinZoom() {
        if (null == mCamera) return 0;
        return mCamera.getCameraInfo().getZoomState().getValue().getMinZoomRatio();
    }

    @Override
    public float getZoom() {
        if (null == mCamera) return 0;
        return mCamera.getCameraInfo().getZoomState().getValue().getZoomRatio();
    }

    @Override
    public void setZoom(float level) {
        if (null == mCamera) return;
        mCamera.getCameraControl().setZoomRatio(level);
    }

    @SuppressLint("UnsafeOptInUsageError")
    @Override
    public int getMinExposureCompensation() {
        if (null == mCamera) return 0;
        return mCamera.getCameraInfo().getExposureState().getExposureCompensationRange().getLower();
    }

    @SuppressLint("UnsafeOptInUsageError")
    @Override
    public int getMaxExposureCompensation() {
        if (null == mCamera) return 0;
        return mCamera.getCameraInfo().getExposureState().getExposureCompensationRange().getUpper();
    }

    @SuppressLint("UnsafeOptInUsageError")
    @Override
    public int getExposureCompensation() {
        if (null == mCamera) return 0;
//        int index = mCamera.getCameraInfo().getExposureState().getExposureCompensationIndex();
        return mCamera.getCameraInfo().getExposureState().getExposureCompensationIndex();
    }

    @SuppressLint("UnsafeOptInUsageError")
    @Override
    public void setExposureCompensation(int value) {
        if (null == mCamera) return;
        mCamera.getCameraControl().setExposureCompensationIndex(value);
    }

    @Override
    public void setAutoExposure(boolean toggle) {
        if (null == mCamera) return;
        mAutoExposure.set(toggle);
        setCaptureRequestOptions();
    }

    @Override
    public boolean autoExposure() {
        return mAutoExposure.get();
    }

    @Override
    public void autoFocus() {
        mAutoFocus.set(true);
        startFocus(new Rect(mFinderView.getWidth() / 2, mFinderView.getHeight() / 2, 0, 0));
    }

    @Override
    public void manualFocus(Rect rect) {
        mAutoFocus.set(false);
        startFocus(rect);
    }

    /**
     *  start focus
     * @param rect the focus rect
     */
    private void startFocus(Rect rect) {
        if (null == mCamera) return;
        MeteringPointFactory pointFactory = mFinderView.getMeteringPointFactory();
        MeteringPoint point = pointFactory.createPoint(rect.left, rect.top);
        FocusMeteringAction action = new FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF).addPoint(point, FocusMeteringAction.FLAG_AE).build();
        ListenableFuture<FocusMeteringResult> future = mCamera.getCameraControl().startFocusAndMetering(action);
        future.addListener(new Runnable() {
            @Override
            public void run() {
                setCaptureRequestOptions();
            }
        }, ContextCompat.getMainExecutor(getContext()));
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void setCaptureRequestOptions() {
        Camera2CameraControl.from(mCamera.getCameraControl()).setCaptureRequestOptions(new CaptureRequestOptions.Builder().setCaptureRequestOption(CaptureRequest.CONTROL_AE_LOCK, !autoExposure()).setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, getAFControlMode()).build());
    }

    @Override
    public int getCameraId() {
        return mLensFacing;
    }

    @Override
    public void switchCamera(int cameraId) {
        mLensFacing = cameraId;
        stopPreview();
        createSelector();
        startPreview();
    }

    @Override
    public void setPreviewCallback(ICameraPreviewCallback previewCallback) {
        mPreviewCallback = previewCallback;
    }


    @Override
    public void release() {
        try {
            stopPreview();
            mCameraExecutor.shutdown();
            mDisplayManager.unregisterDisplayListener(mDisplayListener);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mDisplayListener = null;
        }
    }

    private int getAFControlMode() {
        return mAutoFocus.get() ? CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE : CaptureRequest.CONTROL_AF_MODE_MACRO;
    }

}
