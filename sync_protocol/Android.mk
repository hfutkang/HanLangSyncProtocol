LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := sync_protocol
# LOCAL_STATIC_JAVA_LIBRARIES := android-common
LOCAL_SRC_FILES := $(call all-java-files-under, src)
# LOCAL_SRC_FILES += src/cn/ingenic/glasssync/services/ISyncService.aidl
# LOCAL_SRC_FILES += src/cn/ingenic/glasssync/services/IModuleCallback.aidl

include $(BUILD_STATIC_JAVA_LIBRARY)