LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := sync_framework
LOCAL_STATIC_JAVA_LIBRARIES := guava android-common libsync_protocol cn.ingenic.glasssync.services ingenicspp
LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res

include $(BUILD_STATIC_JAVA_LIBRARY)
###################################
include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := libsync_protocol:libs/sync_protocol.jar
include $(BUILD_MULTI_PREBUILT)
