package com.rtmpx.library.camera;

import androidx.camera.core.ImageProxy;

public abstract class ICameraPreviewCallback {

    /**
     *  get a preview frame raw data
     * @param image
     * @param rotation
     */
    public void onPreviewFrame(ImageProxy image, int rotation) {
        handleImage(image,rotation);
        image.close();
    }

    /**
     * handle a preview frame  must close image
     * @param image
     * @param rotation
     */
    public abstract void handleImage(ImageProxy image, int rotation);
}
