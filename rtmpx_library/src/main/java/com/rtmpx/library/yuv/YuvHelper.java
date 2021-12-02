package com.rtmpx.library.yuv;

import android.media.Image;
import android.util.Log;

import java.nio.ByteBuffer;

public class YuvHelper {
    private static YuvFrame mYuvFrame;
    private static ByteBuffer y, u, v;

    static {
        System.loadLibrary("rtmpx");
    }


    public static YuvFrame createYuvFrame(int width, int height) {
        if (null == mYuvFrame || (mYuvFrame.getWidth() != width || mYuvFrame.getHeight() != height)) {
            mYuvFrame = new YuvFrame();
            y = u = v = null;
        }
        int ySize = width * height;
        int uvSize = width * height / 4;
        if (null == y) {
            y = ByteBuffer.allocateDirect(ySize);
        } else {
            y.clear();
        }
        if (null == u) {
            u = ByteBuffer.allocateDirect(uvSize);
        } else {
            u.clear();
        }
        if (null == v) {
            v = ByteBuffer.allocateDirect(uvSize);
        } else {
            v.clear();
        }
        int extra = (width % 2 == 0) ? 0 : 1;
        mYuvFrame.fill(y, u, v, width, width / 2 + extra, width / 2 + extra, width, height);
        return mYuvFrame;
    }

    public static YuvFrame createYuvFrame(int width, int height, int rotationMode) {
        int outWidth = (rotationMode == 90 || rotationMode == 270) ? height : width;
        int outHeight = (rotationMode == 90 || rotationMode == 270) ? width : height;
        return createYuvFrame(outWidth, outHeight);
    }

    public static YuvFrame rotate(Image image, int rotationMode) {
        assert (rotationMode == 0 || rotationMode == 90 || rotationMode == 180 || rotationMode == 270);
        YuvFrame outFrame = createYuvFrame(image.getWidth(), image.getHeight(), rotationMode);
        rotate(image.getPlanes()[0].getBuffer(), image.getPlanes()[1].getBuffer(), image.getPlanes()[2].getBuffer(), image.getPlanes()[0].getRowStride(), image.getPlanes()[1].getRowStride(), image.getPlanes()[2].getRowStride(), outFrame.getY(), outFrame.getU(), outFrame.getV(), outFrame.getyStride(), outFrame.getuStride(), outFrame.getvStride(), image.getWidth(), image.getHeight(), rotationMode);
        return outFrame;
    }

    public static YuvFrame rotate(YuvFrame yuvFrame, int rotationMode) {
        assert (rotationMode == 0 || rotationMode == 90 || rotationMode == 180 || rotationMode == 270);
        YuvFrame outFrame = createYuvFrame(yuvFrame.getWidth(), yuvFrame.getHeight(), rotationMode);
        rotate(yuvFrame.getY(), yuvFrame.getU(), yuvFrame.getV(), yuvFrame.getyStride(), yuvFrame.getuStride(), yuvFrame.getvStride(), outFrame.getY(), outFrame.getU(), outFrame.getV(), outFrame.getyStride(), outFrame.getuStride(), outFrame.getvStride(), yuvFrame.getWidth(), yuvFrame.getHeight(), rotationMode);
        return outFrame;
    }

    public static YuvFrame convertToI420(Image image) {
        YuvFrame outFrame = createYuvFrame(image.getWidth(), image.getHeight());
        convertToI420(image.getPlanes()[0].getBuffer(), image.getPlanes()[1].getBuffer(), image.getPlanes()[2].getBuffer(), image.getPlanes()[0].getRowStride(), image.getPlanes()[1].getRowStride(), image.getPlanes()[2].getRowStride(), image.getPlanes()[2].getPixelStride(), outFrame.getY(), outFrame.getU(), outFrame.getV(), outFrame.getyStride(), outFrame.getuStride(), outFrame.getvStride(), image.getWidth(), image.getHeight());
        return outFrame;
    }

    public static YuvFrame convertToI420(YuvFrame yuvFrame, int uvPixelStride) {
        YuvFrame outFrame = createYuvFrame(yuvFrame.getWidth(), yuvFrame.getHeight());
        convertToI420(yuvFrame.getY(), yuvFrame.getU(), yuvFrame.getV(), yuvFrame.getyStride(), yuvFrame.getuStride(), yuvFrame.getvStride(), uvPixelStride, outFrame.getY(), outFrame.getU(), outFrame.getV(), outFrame.getyStride(), outFrame.getuStride(), outFrame.getvStride(), yuvFrame.getWidth(), yuvFrame.getHeight());
        return outFrame;
    }

    public static native void rotate(ByteBuffer y, ByteBuffer u, ByteBuffer v, int yStride, int uStride, int vStride, ByteBuffer yOut, ByteBuffer uOut, ByteBuffer vOut, int yOutStride, int uOutStride, int vOutStride, int width, int height, int rotationMode);

    public static native void convertToI420(ByteBuffer y, ByteBuffer u, ByteBuffer v, int yStride, int uStride, int vStride, int srcPixelStrideUv, ByteBuffer yOut, ByteBuffer uOut, ByteBuffer vOut, int yOutStride, int uOutStride, int vOutStride, int width, int height);

    public static native void I420ToNV12(byte[] i420Src, int width, int height, byte[] nv12Dst);
}
