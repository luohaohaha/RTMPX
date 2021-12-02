package com.rtmpx.library.camera;

import android.graphics.Rect;
import android.util.Size;

import java.io.File;

public interface ICamera {
    /**
     * Start preview
     */
    void startPreview();

    /**
     * Stop preview
     */
    void stopPreview();

    /**
     * Whether to support zoom
     *
     * @return camera support zoom or not
     */
    default boolean supportZoom() {
        return true;
    }

    /**
     * Get the camera's maximum variable focal length
     *
     * @return camera max zoom level
     */
    float getMaxZoom();

    /**
     * Obtain the minimum variable focal length of the camera
     *
     * @return camera min zoom level
     */
    float getMinZoom();

    /**
     * Get the current focal length of the camera
     *
     * @return camera current zoom level
     */
    float getZoom();

    /**
     * Set zoom
     *
     * @param level 变焦等级  0 ~  {@link #getMaxZoom()}
     */
    void setZoom(float level);

    /**
     * Get the minimum exposure level
     *
     * @return camera min exposure level
     */
    int getMinExposureCompensation();

    /**
     * Get the maximum exposure level
     *
     * @return camera max exposure level
     */
    int getMaxExposureCompensation();

    /**
     *Get exposure level
     *
     * @return camera current exposure level
     */
    int getExposureCompensation();

    /**
     * Set the exposure level
     *
     * @param value {@link #getMinExposureCompensation()} ~ {@link #getMaxExposureCompensation()}
     */
    void setExposureCompensation(int value);

    /**
     * Set auto exposure
     *
     * @param toggle true ——> auto     false——>manual
     */
    void setAutoExposure(boolean toggle);

    /**
     * Auto exposure {@link #setAutoExposure(boolean)}
     *
     * @return camera auto exposure or not
     */
    boolean autoExposure();

    /**
     * auto focus
     */
    void autoFocus();

    /**
     * Manual focus
     *
     * @param rect Focus area
     */
    void manualFocus(Rect rect);

    /**
     * Get camera id
     *
     * @return current camera id
     */
    int getCameraId();

    /**
     * Switch camera
     *
     * @param cameraId 相机id {@link #getCameraId()}
     */
    void switchCamera(int cameraId);

    /**
     * Set preview callback
     *
     * @param previewCallback
     */
    void setPreviewCallback(ICameraPreviewCallback previewCallback);

    /**
     * freed
     */
    void release();

}
