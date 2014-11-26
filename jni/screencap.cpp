
/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <errno.h>
#include <unistd.h>
#include <stdio.h>
#include <fcntl.h>

#include <linux/fb.h>
#include <sys/ioctl.h>
#include <sys/mman.h>

#include <binder/ProcessState.h>

#include <gui/SurfaceComposerClient.h>
#include <gui/ISurfaceComposer.h>

#include <ui/PixelFormat.h>

#include <SkImageEncoder.h>
#include <SkBitmap.h>
#include <SkData.h>
#include <SkStream.h>

#include <utils/Log.h>

#include "android_runtime/AndroidRuntime.h"
#include "JNIHelp.h"
#include "jni.h"

using namespace android;

static uint32_t DEFAULT_DISPLAY_ID = ISurfaceComposer::eDisplayIdMain;
int fb_width, fb_height;
uint32_t s, f;

static void usage(const char* pname)
{
    fprintf(stderr,
            "usage: %s [-hp] [-d display-id] [FILENAME]\n"
            "   -h: this message\n"
            "   -p: save the file as a png.\n"
            "   -d: specify the display id to capture, default %d.\n"
            "If FILENAME ends with .png it will be saved as a png.\n"
            "If FILENAME is not given, the results will be printed to stdout.\n",
            pname, DEFAULT_DISPLAY_ID
    );
}

static SkBitmap::Config flinger2skia(PixelFormat f)
{
    switch (f) {
        case PIXEL_FORMAT_A_8:
            return SkBitmap::kA8_Config;
        case PIXEL_FORMAT_RGB_565:
            return SkBitmap::kRGB_565_Config;
        case PIXEL_FORMAT_RGBA_4444:
            return SkBitmap::kARGB_4444_Config;
        default:
            return SkBitmap::kARGB_8888_Config;
    }
}

static status_t vinfoToPixelFormat(const fb_var_screeninfo& vinfo,
        uint32_t* bytespp, uint32_t* f)
{

    switch (vinfo.bits_per_pixel) {
        case 16:
            *f = PIXEL_FORMAT_RGB_565;
            *bytespp = 2;
            break;
        case 24:
            *f = PIXEL_FORMAT_RGB_888;
            *bytespp = 3;
            break;
        case 32:
            // TODO: do better decoding of vinfo here
            *f = PIXEL_FORMAT_RGBX_8888;
            *bytespp = 4;
            break;
        default:
            return BAD_VALUE;
    }
    return NO_ERROR;
}

static int copy_and_fix_rgb_order_bpp32(void *src, void *dst, int w, int h, int s, int bytespp=4)
{
    unsigned int * s32;
    unsigned int * d32;
    int size;
    ALOGE("copy_and_fix_rgb_order_bpp32");
    s32 = (unsigned int *) src;
    d32 = (unsigned int *) dst;
    size = h*s;

    while (size--) {
        unsigned int pixel;
        unsigned char r,g,b,x;

        pixel = *s32;
        x = (pixel>>24) & 0xff;
        r = (pixel>>16) & 0xff;
        g = (pixel>>8) & 0xff;
        b = (pixel>>0) & 0xff;
        *d32 = (x<<24) | (b<<16) | (g<<8) | (r<<0);
        s32++; d32++;
    }

    return 0;
}

static int * new_rgbx_base = NULL;
static int frame_size = 0;
void const* mapbase = MAP_FAILED;
static int default_frame_width = 320;
static int default_frame_height = 240;


int *getfb(void) {
    ProcessState::self()->startThreadPool();
    int32_t displayId = DEFAULT_DISPLAY_ID;

    ssize_t mapsize = -1;
    void const* base = 0;
    uint32_t w, h;
   
    ScreenshotClient screenshot;
    sp<IBinder> display = SurfaceComposerClient::getBuiltInDisplay(displayId);

    const char* fbpath = "/dev/graphics/fb0";
    int fb = open(fbpath, O_RDONLY);
    if (fb >= 0) {
        ALOGE("FB opened");
	struct fb_var_screeninfo vinfo;
	struct fb_fix_screeninfo finfo;
	if (ioctl(fb, FBIOGET_VSCREENINFO, &vinfo) == 0) {
	    uint32_t bytespp;
	    if (vinfoToPixelFormat(vinfo, &bytespp, &f) == NO_ERROR) {
	        size_t offset = (vinfo.xoffset + vinfo.yoffset*vinfo.xres) * bytespp;
		w = vinfo.xres;
		h = vinfo.yres;
		if (ioctl(fb, FBIOGET_FSCREENINFO, &finfo) == 0) {
		    ALOGE("%d %s(), finfo.line_length=%d", __LINE__, __FUNCTION__, finfo.line_length);
		    s = finfo.line_length/bytespp;
		    frame_size = h*s*bytespp;
		}else {
		    s = vinfo.xres;
		    frame_size = w*h*bytespp;
		}
	  
		offset = (vinfo.xoffset + vinfo.yoffset*s) * bytespp;
		mapsize = offset + frame_size;
		mapbase = mmap(0, mapsize, PROT_READ, MAP_PRIVATE, fb, 0);
		if (mapbase != MAP_FAILED) {
		    base = (void const *)((char const *)mapbase + offset);
	    
		    ALOGE("%d %s() vinfo.bits_per_pixel=%d, vinfo.red.offset=%d", __LINE__, __FUNCTION__, vinfo.bits_per_pixel, vinfo.red.offset);
		    if (vinfo.bits_per_pixel == 32 && vinfo.red.offset == 16 ) {
		        new_rgbx_base = (int *)malloc(frame_size);
			// fix rgb order
			copy_and_fix_rgb_order_bpp32((void*)base, new_rgbx_base, w, h, s, bytespp);
		    }else{
		        new_rgbx_base = (int *)base;
		    }
		}
	    }
	    fb_width = w;
	    fb_height = h;
	}
        close(fb);
    }
    return new_rgbx_base;
}


#define RGB_SIZE 256
static uint8_t Y_R[RGB_SIZE], Y_G[RGB_SIZE], Y_B[RGB_SIZE];
static uint8_t U_R[RGB_SIZE], U_G[RGB_SIZE], U_B[RGB_SIZE];
static uint8_t V_R[RGB_SIZE], V_G[RGB_SIZE], V_B[RGB_SIZE];
static int init_rgb_tab = 0;

void init_rgb_table() {
  int i = 0; 
  while (i < RGB_SIZE) {
    //Y
    Y_R[i] = (i * 1224) >> 12;
    Y_G[i] = (i * 2404) >> 12;
    Y_B[i] = (i * 467) >> 12;
    //U
    U_R[i] = (i * 690) >> 12;
    U_G[i] = (i * 1358) >> 12;
    U_B[i] = (i * 2048) >> 12;
    //V
    V_R[i] = (i * 2048) >> 12;
    V_G[i] = (i * 1715) >> 12;
    V_B[i] = (i * 338) >> 12;
    i++;
  }
}


void ARGB8888_to_YUV420P(uint8_t *dst_ptr_, uint8_t *src_ptr_, uint32_t width, uint32_t height) {
  if (init_rgb_tab == 0) {
    init_rgb_table();
    init_rgb_tab = 1;
  }
  
  uint32_t i = 0, j = 0;
  uint32_t size = width * height;
  uint8_t *y_ptr = dst_ptr_;
  uint8_t *u_ptr = (uint8_t *)malloc(size);
  uint8_t *v_ptr = (uint8_t *)malloc(size);

  uint8_t *src_ptr = src_ptr_;
  uint8_t *y = dst_ptr_;
  uint8_t *u = u_ptr;
  uint8_t *v = v_ptr;

  uint8_t *r = src_ptr, *g, *b;

  while (i < size) {
    g = r + 1;
    b = r + 2;
    *y++ = Y_R[*r] + Y_G[*g] + Y_B[*b];
    *u++ = -U_R[*r] - U_G[*g] + U_B[*b] + 128;
    *v++ = V_R[*r] - V_G[*g] - V_B[*b] + 128;
    r += 4;
    i++;
  }

  uint8_t *subu_ptr = dst_ptr_ + size;
  uint8_t *subv_ptr = dst_ptr_ + size * 5 / 4;
  uint8_t *psu, *psv, *pu1, *pu2, *pv1, *pv2;
  uint32_t halfH = height >> 1;
  uint32_t halfW = width >> 1;
  uint32_t W2 = width << 1;

  int halfW_index = 0, w2_index = 0, w_1_index = width;
  i = 0;

  while(i < halfH){
        psu = subu_ptr + halfW_index;
        psv = subv_ptr + halfW_index;
        halfW_index+=halfW;
        pu1 = u_ptr + w2_index;//i*w2;
        pu2 = u_ptr + w_1_index;//(2 * i + 1)*width;
        pv1 = v_ptr + w2_index;//i*w2;
        pv2 = v_ptr + w_1_index;//(2 * i + 1)*width;
        w2_index += W2;
        w_1_index += W2;
        j = 0;
        while(j < halfW){
            *psu = (*pu1 + *(pu1+1) + *pu2 + *(pu2+1))>>2;
            *psv = (*pv1 + *(pv1+1) + *pv2 + *(pv2+1))>>2 ;
             psu ++;
             psv ++;
             pu1 += 2;
             pu2 += 2;
             pv1 += 2;
             pv2 += 2;
             j++;
        }
        i++;
    }
    free(u_ptr);
    free(v_ptr);
}

void yuvtonv12(int width, int height, unsigned char *src, unsigned char *dst) {
  int frameSize = width*height;
  int i = 0, j = 0;
  
  for(i=0; i<frameSize; i++ )
    dst[i] = src[i];
  
  for(i=0, j=0; i<frameSize/4; i++, j+=2)
    dst[frameSize + j] = src[frameSize + i];
  
  for(i=0, j=1; i<frameSize/4; i++, j+=2)
    dst[frameSize + j] = src[frameSize +  frameSize / 4 + i];
}
 
static jint get_framebuffer_data(JNIEnv* env, jobject thiz,
				 jbyteArray data, int width, int height) {
  ALOGE("get_framebuffer_data");
  jbyte* readBuff = NULL;
  readBuff = (jbyte *)env->GetByteArrayElements(data, NULL);

  if (readBuff == NULL) {
    ALOGE("Error retrieving destination for data");
    return 0;
  }

  int *p = NULL;
  p = getfb();
  if (p == NULL) {
    ALOGE("framebuffer is null");
    return 0;
  }
  
  uint8_t *rgb_src = (uint8_t *)p;
  uint8_t *yuv_dst = (uint8_t *)malloc(width * height * 3 / 2);

  ARGB8888_to_YUV420P(yuv_dst, rgb_src, width, height);

  unsigned char *dst = (unsigned char *)malloc(width * height * 3 / 2);  
  yuvtonv12(width, height, yuv_dst, dst);
  memcpy(readBuff, dst, width * height * 3 / 2);
  
  free(yuv_dst);
  free(dst);

  if (mapbase != MAP_FAILED) {
    munmap((void *)mapbase, frame_size);
  }

  env->ReleaseByteArrayElements(data, readBuff, 0);
  return (jint)1;
}


static jint get_frame_width(JNIEnv* env, jobject thiz) {
  return (jint)fb_width;
}

static jint  get_frame_height(JNIEnv* env, jobject thiz) {
  return (jint)fb_height;
}


static void get_picture_size(JNIEnv* env, jobject thiz) {
  ALOGE("get_picture_size");
  uint32_t f1, bytespp;
  const char* fbpath = "/dev/graphics/fb0";
  int fb = open(fbpath, O_RDONLY);

  if (fb >= 0) {
     struct fb_var_screeninfo vinfo;
     if (ioctl(fb, FBIOGET_VSCREENINFO, &vinfo) == 0) {
       if (vinfoToPixelFormat(vinfo, &bytespp, &f1) == NO_ERROR) {
	 fb_width = vinfo.xres;
	 fb_height = vinfo.yres;
       }else{
	 fb_width = default_frame_width;
	 fb_height = default_frame_height;
       }
     }
  }else{
    fb_width = default_frame_width;
    fb_height = default_frame_height;
  }
  close(fb);
}


   
static const char* const kClassPathName = "cn/ingenic/glasssync/screen/screenshare/AvcEncode";

static JNINativeMethod gMethods[] = {
    {"get_width","()I",(void *)get_frame_width},
    {"get_height","()I",(void *)get_frame_height},
    {"get_picture","()V",(void *)get_picture_size},
    {"get_frameData","([BII)I",(void *)get_framebuffer_data},
};

static int register_android_mmi(JNIEnv *env)
{
  return AndroidRuntime::registerNativeMethods(env, kClassPathName, gMethods, NELEM(gMethods));
}


jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
	JNIEnv* env = NULL;
	jint result = -1;

	ALOGE("JNI_OnLoad\n");

	if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK)
	{
	  ALOGE("ERROR: GetEnv failed\n");
	  return result;
	}
	assert(env != NULL);

	if (register_android_mmi(env) < 0)
	{
	  ALOGE("ERROR: colorspace changed native registration failed\n");
	  return result;
	}

	result = JNI_VERSION_1_4;
	
	ALOGE("JNI_OnLoad end\n");
	return result;
}
 
 


