# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
        rtmpx

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        flvmuxer/xiecc_rtmp.c
        flvmuxer/xiecc_rtmp.c
        librtmp/amf.c
        librtmp/hashswf.c
        librtmp/log.c
        librtmp/parseurl.c
        librtmp/rtmp.c
        librtmp-jni.c
        rtmpmuxer.c
        yuv_convert.cpp
       )


include_directories(
        ${CMAKE_SOURCE_DIR}/flvmuxer
        ${CMAKE_SOURCE_DIR}/librtmp
        ${CMAKE_SOURCE_DIR}/yuv/libyuv
        ${CMAKE_SOURCE_DIR}/yuv
        ${CMAKE_SOURCE_DIR}
) #包含的头文件,依赖的so库都会提供头文件共使用，必须列出来

add_definitions(-DNO_CRYPTO)


# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

add_library(yuv SHARED IMPORTED)
set_target_properties(yuv PROPERTIES IMPORTED_LOCATION  ${PROJECT_SOURCE_DIR}/../jniLibs/${CMAKE_ANDROID_ARCH_ABI}/libyuv.so)

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.


target_link_libraries( # Specifies the target library.
        rtmpx
        yuv
        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})
