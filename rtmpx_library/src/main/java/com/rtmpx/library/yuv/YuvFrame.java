package com.rtmpx.library.yuv;

import android.media.Image;

import java.nio.ByteBuffer;
public class YuvFrame {

    private ByteBuffer y;
    private ByteBuffer u;
    private ByteBuffer v;

    private int yStride;
    private int uStride;
    private int vStride;

    private int width;
    private int height;

    public YuvFrame() {
        super();
    }

    public void fill(ByteBuffer y, ByteBuffer u, ByteBuffer v, int yStride, int uStride, int vStride, int width, int height) {
        this.y = y;
        this.u = u;
        this.v = v;
        this.yStride = yStride;
        this.uStride = uStride;
        this.vStride = vStride;
        this.width = width;
        this.height = height;
    }

    public void fill(Image image) {
        Image.Plane[] planes = image.getPlanes();
        Image.Plane yPlane = planes[0];
        this.y = yPlane.getBuffer();
        this.yStride = yPlane.getRowStride();

        Image.Plane uPlane = planes[1];
        this.u = uPlane.getBuffer();
        this.uStride = uPlane.getRowStride();

        Image.Plane vPlane = planes[2];
        this.v = vPlane.getBuffer();
        this.vStride = vPlane.getRowStride();

        this.width = image.getWidth();
        this.height = image.getHeight();
    }

    public ByteBuffer getY() {
        return y;
    }

    public ByteBuffer getU() {
        return u;
    }

    public ByteBuffer getV() {
        return v;
    }

    public int getyStride() {
        return yStride;
    }

    public int getuStride() {
        return uStride;
    }

    public int getvStride() {
        return vStride;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public byte[] asArray() {
        byte[] array;

        int yPos = y.position();
        int uPos = u.position();
        int vPos = v.position();

        try {
            array = ByteBuffer.allocate(y.capacity() + u.capacity() + v.capacity()).put(y).put(u).put(v).array();
            y.position(yPos);
            u.position(uPos);
            v.position(vPos);
        } catch (Exception e) {

            array = new byte[size()];

            y.get(array, 0, y.remaining());
            y.position(yPos);

            u.get(array, y.remaining(), u.remaining());
            u.position(uPos);

            v.get(array, y.remaining() + u.remaining(), v.remaining());
            v.position(vPos);
        }
        return array;
    }

    public int size() {
        return y.remaining() + u.remaining() + v.remaining();
    }

    public void free() {
        y = ByteBuffer.allocate(1);
        u = ByteBuffer.allocate(1);
        v = ByteBuffer.allocate(1);
        yStride = 0;
        uStride = 0;
        vStride = 0;
        width = 0;
        height = 0;
    }
}
