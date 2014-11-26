LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	screencap.cpp

LOCAL_SHARED_LIBRARIES := \
	liblog \
	libutils \
	libui \
	libgui \
	libskia \
	libbinder \
	libandroid_runtime \
	libnativehelper 

LOCAL_C_INCLUDES := \
    $(JNI_H_INCLUDE) \
    frameworks/base/core/jni \
	frameworks/base/include \
	external/skia/include/core \
	external/skia/include/effects \
	external/skia/include/images \
	external/skia/src/ports \
	external/skia/include/utils

LOCAL_MODULE:= libscreencap_jni

LOCAL_MODULE_TAGS:= optional

include $(BUILD_SHARED_LIBRARY)
