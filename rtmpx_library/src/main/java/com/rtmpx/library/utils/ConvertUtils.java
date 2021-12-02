package com.rtmpx.library.utils;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.camera.core.ImageProxy;

import com.rtmpx.library.yuv.YuvFrame;
import com.rtmpx.library.yuv.YuvHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ConvertUtils {
    private static final String TAG = "ConvertUtils";
    private static byte[] mPortraitYUV;
    private static byte[] mLandscapeYUV;

    public static byte[] YUV_420_888toNV12(ImageProxy image, int rotation) {
        if (0 == rotation) {
            return YUV_420_888toPortraitNV12(image, rotation);
        } else {
            return YUV_420_888toLandscapeNV12(image, rotation);
        }
    }

    /**
     * YUV_420_888 to Landscape NV21
     *
     * @param image CameraX ImageProxy
     * @return nv12 byte array
     */
    @SuppressLint("UnsafeOptInUsageError")
    public static byte[] YUV_420_888toLandscapeNV12(ImageProxy image, int rotation) {
        byte [] bytes = YuvHelper.convertToI420(image.getImage()).asArray();
        if (null == mLandscapeYUV || mLandscapeYUV.length != bytes.length) {
            mLandscapeYUV = new byte[bytes.length];
        }
        YuvHelper.I420ToNV12(bytes, image.getWidth(), image.getHeight(), mLandscapeYUV);
        return mLandscapeYUV;
    }

    /**
     * YUV_420_888 to Portrait NV12
     * @param image CameraX ImageProxy
     * @param rotation display rotation
     * @return nv12 byte array
     */
    @SuppressLint("UnsafeOptInUsageError")
    public static byte[] YUV_420_888toPortraitNV12(ImageProxy image, int rotation) {
        YuvFrame yuvFrame = YuvHelper.convertToI420(image.getImage());
        //TODO optimization of vertical screen libyuv rotation
        byte[] bytes = YuvHelper.rotate(yuvFrame, 90).asArray();
        if (null == mPortraitYUV || mPortraitYUV.length != bytes.length) {
            mPortraitYUV = new byte[bytes.length];
        }
        YuvHelper.I420ToNV12(bytes, image.getWidth(), image.getHeight(), mPortraitYUV);
        return mPortraitYUV;
    }

    private static void writeFile(ImageProxy mImage, String path) {
        File file = new File(path);
        FileOutputStream output = null;
        ByteBuffer buffer;
        byte[] bytes;
        ByteBuffer prebuffer = ByteBuffer.allocate(16);
        prebuffer.putInt(mImage.getWidth())
                .putInt(mImage.getHeight())
                .putInt(mImage.getPlanes()[1].getPixelStride())
                .putInt(mImage.getPlanes()[1].getRowStride());

        try {
            output = new FileOutputStream(file);
            output.write(prebuffer.array()); // write meta information to file
            // Now write the actual planes.
            for (int i = 0; i < 3; i++) {
                buffer = mImage.getPlanes()[i].getBuffer();
                bytes = new byte[buffer.remaining()]; // makes byte array large enough to hold image
                buffer.get(bytes); // copies image from buffer to byte array
                output.write(bytes);    // write the byte array to file
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
//            mImage.close(); // close this to free up buffer for other images
            if (null != output) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
