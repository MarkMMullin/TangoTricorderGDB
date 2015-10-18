
DISTRO_ROOT:=/Dev/tango/c/tango-examples-c-master-xiaotong
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
SHARED:=C:/Dev/tango/c/tango-examples-c-master-xiaotong
LOCAL_MODULE    := libTangoWrangler
LOCAL_SHARED_LIBRARIES := tango_client_api
LOCAL_CFLAGS    := -Werror -std=c++11 -I$(SHARED)/tango-gl/include
LOCAL_C_INCLUDES := $(SHARED)/tango-gl/include \
                    $(SHARED)/third-party/glm/

LOCAL_SRC_FILES := JNIInterface.cpp PointCloudRenderer.cpp TangoDriver.cpp Tricorder.cpp VideoOverlay.cpp\
					TangoImage.cpp \
                    $(SHARED)/tango-gl/util.cpp \
                    $(SHARED)/tango-gl/camera.cpp \
                    $(SHARED)/tango-gl/drawable_object.cpp \
                    $(SHARED)/tango-gl/transform.cpp \
                    $(SHARED)/tango-gl/shaders.cpp
LOCAL_LDLIBS    := -llog -lGLESv2 -L$(SYSROOT)/usr/lib
include $(BUILD_SHARED_LIBRARY)

$(call import-add-path, $(DISTRO_ROOT))
$(call import-module,tango_client_api)
