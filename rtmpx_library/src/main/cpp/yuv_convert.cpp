#include <jni.h>
#include <string>
#include <vector>
#include "libyuv.h"


extern "C" JNIEXPORT void JNICALL
Java_com_rtmpx_library_yuv_YuvHelper_rotate(JNIEnv *env, jclass thiz, jobject y, jobject u,
                                            jobject v, jint yStride, jint uStride, jint vStride,
                                            jobject yOut, jobject uOut, jobject vOut,
                                            jint yOutStride, jint uOutStride, jint vOutStride,
                                            jint width, jint height, jint rotationMode) {

    uint8_t *yNative = (uint8_t *) env->GetDirectBufferAddress(y);
    uint8_t *uNative = (uint8_t *) env->GetDirectBufferAddress(u);
    uint8_t *vNative = (uint8_t *) env->GetDirectBufferAddress(v);

    uint8_t *yOutNative = (uint8_t *) env->GetDirectBufferAddress(yOut);
    uint8_t *uOutNative = (uint8_t *) env->GetDirectBufferAddress(uOut);
    uint8_t *vOutNative = (uint8_t *) env->GetDirectBufferAddress(vOut);

    libyuv::I420Rotate(yNative, yStride,
                       uNative, uStride,
                       vNative, vStride,
                       yOutNative, yOutStride,
                       uOutNative, uOutStride,
                       vOutNative, vOutStride,
                       width, height,
                       libyuv::RotationMode(rotationMode));
}

extern "C" JNIEXPORT void JNICALL
Java_com_rtmpx_library_yuv_YuvHelper_convertToI420(JNIEnv *env, jclass thiz, jobject y, jobject u,
                                                  jobject v, jint yStride, jint uStride,
                                                  jint vStride, jint srcPixelStrideUv,
                                                  jobject yOut, jobject uOut, jobject vOut,
                                                  jint yOutStride, jint uOutStride, jint vOutStride,
                                                  jint width, jint height) {

    uint8_t *yNative = (uint8_t *) env->GetDirectBufferAddress(y);
    uint8_t *uNative = (uint8_t *) env->GetDirectBufferAddress(u);
    uint8_t *vNative = (uint8_t *) env->GetDirectBufferAddress(v);

    uint8_t *yOutNative = (uint8_t *) env->GetDirectBufferAddress(yOut);
    uint8_t *uOutNative = (uint8_t *) env->GetDirectBufferAddress(uOut);
    uint8_t *vOutNative = (uint8_t *) env->GetDirectBufferAddress(vOut);

    libyuv::Android420ToI420(yNative, yStride,
                             uNative, uStride,
                             vNative, vStride,
                             srcPixelStrideUv,
                             yOutNative, yOutStride,
                             uOutNative, uOutStride,
                             vOutNative, vOutStride,
                             width, height);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_rtmpx_library_yuv_YuvHelper_I420ToNV12(JNIEnv *env, jclass clazz,
                                                         jbyteArray i420_src, jint width,
                                                         jint height, jbyteArray nv12_dst) {
    jbyte *src_i420_data = env->GetByteArrayElements(i420_src, NULL);
    jbyte *dst_nv12_data = env->GetByteArrayElements(nv12_dst, NULL);
    jint src_y_size = width * height;
    jint src_u_size = (width >> 1) * (height >> 1);

    jbyte *src_nv12_y_data = dst_nv12_data;
    jbyte *src_nv12_uv_data = dst_nv12_data + src_y_size;

    jbyte *src_i420_y_data = src_i420_data;
    jbyte *src_i420_u_data = src_i420_data + src_y_size;
    jbyte *src_i420_v_data = src_i420_data + src_y_size + src_u_size;

    libyuv::I420ToNV12(
            (const uint8_t *) src_i420_y_data, width,
            (const uint8_t *) src_i420_u_data, width >> 1,
            (const uint8_t *) src_i420_v_data, width >> 1,
            (uint8_t *) src_nv12_y_data, width,
            (uint8_t *) src_nv12_uv_data, width,
            width, height);


    env->ReleaseByteArrayElements(i420_src, src_i420_data, 0);
    env->ReleaseByteArrayElements(nv12_dst, dst_nv12_data, 0);

}


