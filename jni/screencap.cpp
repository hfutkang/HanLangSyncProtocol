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

#include "jzasm.h"
#include "jzmedia.h"

using namespace android;

//#define MACRO_USE_FOR_NON_COLDWAVE

#define JZFB_GET_BUFFER            0x40044720
#define JZFB_LOCK_BUFFER           0x11
#define JZFB_UNLOCK_BUFFER         0x22

#define TIME_COST_TEST
#include <sys/time.h>
#ifdef TIME_COST_TEST
// Returns current time in microseconds
static unsigned int GetTimer(void) {
    struct timeval tv;
    gettimeofday(&tv,NULL);
    
    return tv.tv_sec * 1000000 + tv.tv_usec;
}
#endif

static int init_rgb_tab = 0;
int frameNum = 0;
int fb_width, fb_height;
int bits_per_pixel= 0;
int changed_width, changed_height;
int is_shrink = 0;
int is_multi16 = 0;
unsigned int costtime = 0;
unsigned int s, f;

static status_t vinfoToPixelFormat(const fb_var_screeninfo& vinfo,
        uint32_t* bytespp, uint32_t* f) {
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

static int frame_size = 0;
ssize_t mapsize = -1;
static int map_offset = 0;
void const* mapbase = MAP_FAILED;
static int default_frame_width = 320;
static int default_frame_height = 240;


int *getfb(void) {
    void const* base = 0;
    uint32_t w, h;
   
    const char* fbpath = "/dev/graphics/fb0";
    int fb = open(fbpath, O_RDONLY);
  
    int *new_rgbx_base = NULL;
    if (fb >= 0) {
	struct fb_var_screeninfo vinfo;
	struct fb_fix_screeninfo finfo;
	if (ioctl(fb, FBIOGET_VSCREENINFO, &vinfo) == 0) {
	    uint32_t bytespp;
	    if (vinfoToPixelFormat(vinfo, &bytespp, &f) == NO_ERROR) {
		bits_per_pixel = vinfo.bits_per_pixel;
		ALOGE("bits_per_pixel = %d,bytespp=%d",bits_per_pixel,bytespp);
	        size_t offset = (vinfo.xoffset + vinfo.yoffset*vinfo.xres) * bytespp;
		if (ioctl(fb, FBIOGET_FSCREENINFO, &finfo) == 0) {
		    s = finfo.line_length/bytespp;
		    frame_size = vinfo.yres*s*bytespp;
		}else {
		    s = vinfo.xres;
		    frame_size = vinfo.xres*vinfo.yres*bytespp;
		}

		int is_use_yoffset = 0;
		unsigned int index = JZFB_LOCK_BUFFER;
		if (ioctl(fb, JZFB_GET_BUFFER, &index) == 0) {
		}else{
		    is_use_yoffset = 1;
		}

		if (is_use_yoffset) {
		    offset = (vinfo.xoffset + vinfo.yoffset*s) * bytespp;
		}else{
		    offset = (vinfo.xoffset + index*vinfo.yres*s) * bytespp;
		}
		mapsize = offset + frame_size;
		mapbase = mmap(0, mapsize, PROT_READ, MAP_PRIVATE, fb, 0);
		if (mapbase != MAP_FAILED) {
		  new_rgbx_base = (int *)((void const *)((char const *)mapbase + offset));
		}
	    }
	}
        close(fb);
    }
#ifdef TIME_COST_TESTN
    int *address_base = (int *)malloc(fb_width * fb_height * 2 + 64);
    unsigned int time = GetTimer();
    memcpy(address_base, new_rgbx_base, frame_size);
    unsigned int time1 = GetTimer() - time;
    ALOGE("memcpy Time = %u us", time1);
    free(address_base);
#endif
    return new_rgbx_base;
}


void RGB565_to_YUV420SP_c (
    unsigned char *y_dst,
    unsigned char *uv_dst,
    unsigned char *rgb_src,
    int width,
    int height)
{
    unsigned int i, j;
    unsigned int tmp;

    unsigned int R, G, B;
    unsigned int Y, U, V;

    unsigned short int *pSrc = (unsigned short int *)rgb_src;

    unsigned char *pDstY = (unsigned char *)y_dst;
    unsigned char *pDstUV = (unsigned char *)uv_dst;

    unsigned int yIndex = 0;
    unsigned int uvIndex = 0;

    for (j = 0; j < height; j++) {
        for (i = 0; i < width; i++) {
            tmp = pSrc[j * width + i];

            R = (tmp & 0x0000F800) >> 11;
            R = R * 8;
            G = (tmp & 0x000007E0) >> 5;
            G = G * 4;
            B = (tmp & 0x0000001F);
            B = B * 8;

            Y = ((66 * R) + (129 * G) + (25 * B) + 128);
            Y = Y >> 8;
            Y += 16;
            pDstY[yIndex++] = (unsigned char)Y;
            if ((j % 2) == 0 && (i % 2) == 0) {
                U = ((-38 * R) - (74 * G) + (112 * B) + 128);
                U = U >> 8;
                U += 128;
                V = ((112 * R) - (94 * G) - (18 * B) + 128);
                V = V >> 8;
                V += 128;

                pDstUV[uvIndex++] = (unsigned char)U;
                pDstUV[uvIndex++] = (unsigned char)V;
            }
        }
    }
}


void RGB565_to_YUV420SP_CHANGED (
    unsigned char *y_dst,
    unsigned char *uv_dst,
    unsigned char *rgb_src,
    int width,
    int height)
{
    unsigned int i, j;
    unsigned short int *src;
    unsigned short int *pSrc = (unsigned short int *)rgb_src;
    unsigned int *dest = (unsigned int *)y_dst;
    unsigned int *uvdest = (unsigned int *)uv_dst;

    dest -= 1;
    uvdest -= 1;
    S32I2M(xr15,0x42424242);
    S32I2M(xr14, 0x80808080);
    S32I2M(xr13,0x81818181);
    S32I2M(xr12,0x19191919);
    S32I2M(xr11,0x01010101);
    S32I2M(xr10, 0x26264A4A); //U 74,74,38,38
    S32I2M(xr9, 0x01017070); //U 1,1 112,112

    for (j = 0; j < height; j+=2) {
        src = pSrc + j * fb_width - 2;
	for (i = 0; i < width; i+=8) {
  	    S16LDI(xr1, src, 4, 0);  
	    S16LDI(xr1, src, 4, 1); //xr1: R2G2B2, R1G1B1
	    S16LDI(xr2, src, 4, 0);  
	    S16LDI(xr2, src, 4, 1); //xr2: R4G4B4, R3G3B3

	    Q16SAR(xr3,xr1,xr2,xr4,11);  //xr3:R2,R1 , xr4:R4,R3
	    Q16SLL(xr5,xr1,xr2,xr6,5);
	    Q16SAR(xr7,xr5,xr6,xr8,10);  //xr7:G2,G1, XR8:G4,G3

	    Q16SLL(xr5,xr1,xr2,xr6,11);
	    Q16SAR(xr5,xr5,xr6,xr6,11);  //xr5:B2,B1, xr6:B4,B3

	    Q16SLL(xr3,xr3,xr4,xr4,3);   //xr3:R2<<3,R1<<3
	    Q16SLL(xr7,xr7,xr8,xr8,2);   //xr7:G2<<2,G1<<2
	    Q16SLL(xr5,xr5,xr6,xr6,3);

	    S32SFL(xr0,xr4,xr3,xr3,1);  //xr3:R4R3R2R1
	    S32SFL(xr0,xr8,xr7,xr7,1);  //xr7:G4G3G2G1
	    S32SFL(xr0,xr6,xr5,xr5,1);  //xr5:B4B3B2B1

	    Q8MUL(xr1,xr3,xr15,xr2);  //xr1:R4*66, R2*66 , xr2:R3*66, R1*66
	    Q8MAC_AA(xr1,xr13,xr7,xr2); //xr1:R4*66+G4*129,R2*66+G2*129  xr2: R3*66+G3*129,R1*66+G1*129
	    Q8MAC_AA(xr1,xr5,xr12,xr2); //xr1:R4*66+G4*129+B4*25,R2*66+G2*129+B2*25  xr2: R3*66+G3*129+B3*25,R1*66+G1*129+B1*25
	    Q8MAC_AA(xr1,xr11,xr14,xr2); //xr1:R4*66+G4*129+B4*25+80,R2*66+G2*129+B2*25+80  xr2: R3*66+G3*129+B3*25+80,R1*66+G1*129+B1*25+80

	    S32I2M(xr6, 0x00100010); //U 1,1 112,112
	    Q16SAR(xr1,xr1,xr2,xr2,8); //>>8
	    Q16ACCM_AA(xr1,xr6,xr6,xr2); //Y+16

	    S32SFL(xr0,xr1,xr2,xr1,1);
	    S32SDI(xr1, dest, 0x4);
	    
	    if ((j % 4) == 0) {
	        S32I2M(xr8, 0x12125E5E); //V 18, 18, 94,94
		S32SFL(xr0,xr3,xr7,xr3,1);  //xr3:R3R1G3G1
		S32SFL(xr0,xr14,xr5,xr5,1);  //xr9:8080B3B1
		
		Q8MUL(xr1,xr5,xr9,xr2); //xr1:80*1,80*1, xr2:B3*112,B1*112
		Q8MAC_SS(xr1,xr3,xr10,xr2); //xr1:80-R3*38,80-R1*38, xr2:B3*112-G3*74,B1*112-G1*74
		
		S32SFL(xr5,xr5,xr3,xr3,3);  //xr5:8080R3R1, xr3:B3B1G3G1
		Q8MUL(xr4,xr5,xr9,xr6); //xr4:80*1,80*1,xr5:R3*112, R1*112
		Q8MAC_SS(xr4,xr3,xr8,xr6); //xr4:80*1-B3*18,80*1-B1*18, xr5:R3*112-G3*94, R1*112-G1*94
		Q16ACCM_AA(xr1,xr2,xr6,xr4); //xr1:80-R3*38+B3*112-G3*74(U2),80-R1*38+B1*112-G1*74(U1)  //xr4:80-B3*18+R3*112-G3*94,80-B1*18+R1*112-G1*94 
		Q16SAR(xr1,xr1,xr4,xr4,8); //>>8
		Q16ACCM_AA(xr1,xr14,xr14,xr4); //UV+128
		S32SFL(xr0,xr4,xr1,xr1,2);
		S32SDI(xr1, uvdest,4);
	    }

	}
    }
}

void ARGB8888_to_YUV420SP_c(
    unsigned char *y_dst,
    unsigned char *uv_dst,
    unsigned char *rgb_src,
    unsigned int width,
    unsigned int height)
{
    unsigned int i, j;
    unsigned int tmp;

    unsigned int R, G, B;
    unsigned int Y, U, V;

    unsigned int *pSrc = (unsigned int *)rgb_src;
    unsigned char *pDstY = (unsigned char *)y_dst;
    unsigned char *pDstUV = (unsigned char *)uv_dst;

    unsigned int yIndex = 0;
    unsigned int uvIndex = 0;

    for (j = 0; j < height; j++) {
        for (i = 0; i < width; i++) {
            tmp = pSrc[j * width + i];
	    B = (tmp & 0x00FF0000) >> 16;
            G = (tmp & 0x0000FF00) >> 8;
            R = (tmp & 0x000000FF);

	    Y = ((66 * R) + (129 * G) + (25 * B) + 128);
            Y = Y >> 8;
	    Y += 16;

            pDstY[yIndex++] = (unsigned char)Y;
            if ((j % 2) == 0 && (i % 2) == 0) {
	        U = ((-38 * R) - (74 * G) + (112 * B) + 128);
                U = U >> 8;
                U += 128;
		V = ((112 * R) - (94 * G) - (18 * B) + 128);
                V = V >> 8;
                V += 128;

                pDstUV[uvIndex++] = (unsigned char)U;
                pDstUV[uvIndex++] = (unsigned char)V;
            }
        }
    }
}


void RGB565_to_YUV420SP(
    unsigned char *y_dst,
    unsigned char *uv_dst,
    unsigned char *rgb_src,
    int width,
    int height)
{
    unsigned int i, j;
    unsigned short int *src;
    unsigned short int *pSrc = (unsigned short int *)rgb_src;
    unsigned int *dest = (unsigned int *)y_dst;
    unsigned int *uvdest = (unsigned int *)uv_dst;

    dest -= 1;
    uvdest -= 1;

    S32I2M(xr15,0x42424242);
    S32I2M(xr14, 0x80808080);
    S32I2M(xr13,0x81818181);
    S32I2M(xr12,0x19191919);
    S32I2M(xr11,0x01010101);
    S32I2M(xr10, 0x26264A4A); //U 74,74,38,38
    S32I2M(xr9, 0x01017070); //U 1,1 112,112

    for (j = 0; j < height; j++) {
        src = pSrc + j * fb_width - 1;
        for (i = 0; i < width; i+=4) {
	    S16LDI(xr1, src, 2, 0);  
	    S16LDI(xr1, src, 2, 1); //xr1: R2G2B2, R1G1B1
	    S16LDI(xr2, src, 2, 0);  
	    S16LDI(xr2, src, 2, 1); //xr2: R4G4B4, R3G3B3

	    Q16SAR(xr3,xr1,xr2,xr4,11);  //xr3:R2,R1 , xr4:R4,R3
	    Q16SLL(xr5,xr1,xr2,xr6,5);
	    Q16SAR(xr7,xr5,xr6,xr8,10);  //xr7:G2,G1, XR8:G4,G3

	    Q16SLL(xr5,xr1,xr2,xr6,11);
	    Q16SAR(xr5,xr5,xr6,xr6,11);  //xr5:B2,B1, xr6:B4,B3

	    Q16SLL(xr3,xr3,xr4,xr4,3);   //xr3:R2<<3,R1<<3
	    Q16SLL(xr7,xr7,xr8,xr8,2);   //xr7:G2<<2,G1<<2
	    Q16SLL(xr5,xr5,xr6,xr6,3);

	    S32SFL(xr0,xr4,xr3,xr3,1);  //xr3:R4R3R2R1
	    S32SFL(xr0,xr8,xr7,xr7,1);  //xr7:G4G3G2G1
	    S32SFL(xr0,xr6,xr5,xr5,1);  //xr5:B4B3B2B1

	    Q8MUL(xr1,xr3,xr15,xr2);  //xr1:R4*66, R2*66 , xr2:R3*66, R1*66
	    Q8MAC_AA(xr1,xr13,xr7,xr2); //xr1:R4*66+G4*129,R2*66+G2*129  xr2: R3*66+G3*129,R1*66+G1*129
	    Q8MAC_AA(xr1,xr5,xr12,xr2); //xr1:R4*66+G4*129+B4*25,R2*66+G2*129+B2*25  xr2: R3*66+G3*129+B3*25,R1*66+G1*129+B1*25
	    Q8MAC_AA(xr1,xr11,xr14,xr2); //xr1:R4*66+G4*129+B4*25+80,R2*66+G2*129+B2*25+80  xr2: R3*66+G3*129+B3*25+80,R1*66+G1*129+B1*25+80

	    S32I2M(xr6, 0x00100010); //U 1,1 112,112
	    Q16SAR(xr1,xr1,xr2,xr2,8); //>>8
	    Q16ACCM_AA(xr1,xr6,xr6,xr2); //Y+16

	    S32SFL(xr0,xr1,xr2,xr1,1);
	    S32SDI(xr1, dest, 0x4);

	    if ((j % 2) == 0) {
	        S32I2M(xr8, 0x12125E5E); //V 18, 18, 94,94
		S32SFL(xr0,xr3,xr7,xr3,1);  //xr3:R3R1G3G1
		S32SFL(xr0,xr14,xr5,xr5,1);  //xr9:8080B3B1
		
		Q8MUL(xr1,xr5,xr9,xr2); //xr1:80*1,80*1, xr2:B3*112,B1*112
		Q8MAC_SS(xr1,xr3,xr10,xr2); //xr1:80-R3*38,80-R1*38, xr2:B3*112-G3*74,B1*112-G1*74
		
		S32SFL(xr5,xr5,xr3,xr3,3);  //xr5:8080R3R1, xr3:B3B1G3G1
		Q8MUL(xr4,xr5,xr9,xr6); //xr4:80*1,80*1,xr5:R3*112, R1*112
		Q8MAC_SS(xr4,xr3,xr8,xr6); //xr4:80*1-B3*18,80*1-B1*18, xr5:R3*112-G3*94, R1*112-G1*94
		Q16ACCM_AA(xr1,xr2,xr6,xr4); //xr1:80-R3*38+B3*112-G3*74(U2),80-R1*38+B1*112-G1*74(U1)  //xr4:80-B3*18+R3*112-G3*94,80-B1*18+R1*112-G1*94 
		Q16SAR(xr1,xr1,xr4,xr4,8); //>>8
		Q16ACCM_AA(xr1,xr14,xr14,xr4); //UV+128
		S32SFL(xr0,xr4,xr1,xr1,2);
		S32SDI(xr1, uvdest,4);
	    }
        }
    }
}


inline void ARGB8888_to_YUV420SP(
    unsigned char *y_dst,
    unsigned char *uv_dst,
    unsigned char *rgb_src,
    unsigned int width,
    unsigned int height)
{
    unsigned int i, j;
    unsigned int *src;

    unsigned int *pSrc = (unsigned int *)rgb_src;
    unsigned char *pDstY = (unsigned char *)y_dst;
    unsigned char *pDstUV = (unsigned char *)uv_dst;
    unsigned int *dest = (unsigned int *)pDstY;
    unsigned int *uvdest = (unsigned int *)pDstUV;


    dest -= 1;
    uvdest -= 1;

    S32I2M(xr15,0x42424242);
    S32I2M(xr14, 0x80808080);
    S32I2M(xr13,0x81818181);
    S32I2M(xr12,0x19191919);
    S32I2M(xr11,0x01010101);
    S32I2M(xr10, 0x26264A4A); //U 74,74,38,38
    S32I2M(xr9, 0x01017070); //U 1,1 112,112

    for (j = 0; j < height; j++) {
        src = pSrc + j * fb_width - 1;
        for (i = 0; i < width; i+=4) {
	    S32LDI(xr1, src, 0x4);  //xr1:0xFFRGB
	    S32LDI(xr2, src, 0x4);  //xr2:0xFFRGB
	    S32LDI(xr3, src, 0x4);  //xr3:0xFFRGB
	    S32LDI(xr4, src, 0x4);  //xr4:0xFFRGB

	    S32SFL(xr5,xr2,xr1,xr6,0);  //xr5:FFFFR2R1  //xr6:G2G1B2B1
	    S32SFL(xr7,xr4,xr3,xr8,0);  //xr7:FFFFR4R3  //xr8:G4G3B4B3

	    S32SFL(xr0,xr7,xr5,xr3,3);  //xr0:FFFFFFFF  xr3:R4R3R2R1
	    S32SFL(xr7,xr8,xr6,xr5,3);  //xr7:G4G3G2G1  xr5:B4B3B2B1
	    
	    Q8MUL(xr1,xr3,xr15,xr2);  //xr1:R4*66, R2*66 , xr2:R3*66, R1*66
	    Q8MAC_AA(xr1,xr13,xr7,xr2); //xr1:R4*66+G4*129,R2*66+G2*129  xr2: R3*66+G3*129,R1*66+G1*129
	    Q8MAC_AA(xr1,xr5,xr12,xr2); //xr1:R4*66+G4*129+B4*25,R2*66+G2*129+B2*25  xr2: R3*66+G3*129+B3*25,R1*66+G1*129+B1*25
	    Q8MAC_AA(xr1,xr11,xr14,xr2); //xr1:R4*66+G4*129+B4*25+80,R2*66+G2*129+B2*25+80  xr2: R3*66+G3*129+B3*25+80,R1*66+G1*129+B1*25+80

	    S32I2M(xr6, 0x00100010); //U 1,1 112,112
	    Q16SAR(xr1,xr1,xr2,xr2,8); //>>8
	    Q16ACCM_AA(xr1,xr6,xr6,xr2); //Y+16

	    S32SFL(xr0,xr1,xr2,xr1,1);
	    S32SDI(xr1, dest, 0x4);

	    if ((j % 2) == 0) {
	        S32I2M(xr8, 0x12125E5E); //V 18, 18, 94,94
		S32SFL(xr0,xr3,xr7,xr3,1);  //xr3:R3R1G3G1
		S32SFL(xr0,xr14,xr5,xr5,1);  //xr9:8080B3B1
		
		Q8MUL(xr1,xr5,xr9,xr2); //xr1:80*1,80*1, xr2:B3*112,B1*112
		Q8MAC_SS(xr1,xr3,xr10,xr2); //xr1:80-R3*38,80-R1*38, xr2:B3*112-G3*74,B1*112-G1*74
		
		S32SFL(xr5,xr5,xr3,xr3,3);  //xr5:8080R3R1, xr3:B3B1G3G1
		Q8MUL(xr4,xr5,xr9,xr6); //xr4:80*1,80*1,xr5:R3*112, R1*112
		Q8MAC_SS(xr4,xr3,xr8,xr6); //xr4:80*1-B3*18,80*1-B1*18, xr5:R3*112-G3*94, R1*112-G1*94
		Q16ACCM_AA(xr1,xr2,xr6,xr4); //xr1:80-R3*38+B3*112-G3*74(U2),80-R1*38+B1*112-G1*74(U1)  //xr4:80-B3*18+R3*112-G3*94,80-B1*18+R1*112-G1*94 
		Q16SAR(xr1,xr1,xr4,xr4,8); //>>8
		Q16ACCM_AA(xr1,xr14,xr14,xr4); //UV+128
		S32SFL(xr0,xr4,xr1,xr1,2);
		S32SDI(xr1, uvdest,4);
	    }
	}
    }
}

inline void ARGB8888_to_YUV420SP_CHANGED(
    unsigned char *y_dst,
    unsigned char *uv_dst,
    unsigned char *rgb_src,
    unsigned int width,
    unsigned int height)
{
    unsigned int i, j;
    unsigned int *src;

    unsigned int *pSrc = (unsigned int *)rgb_src;
    unsigned char *pDstY = (unsigned char *)y_dst;
    unsigned char *pDstUV = (unsigned char *)uv_dst;
    unsigned int *dest = (unsigned int *)pDstY;
    unsigned int *uvdest = (unsigned int *)pDstUV;


    dest -= 1;
    uvdest -= 1;

    S32I2M(xr15,0x42424242);
    S32I2M(xr14, 0x80808080);
    S32I2M(xr13,0x81818181);
    S32I2M(xr12,0x19191919);
    S32I2M(xr11,0x01010101);
    S32I2M(xr10, 0x26264A4A); //U 74,74,38,38
    S32I2M(xr9, 0x01017070); //U 1,1 112,112

    for (j = 0; j < height; j+=2) {
        src = pSrc + j * fb_width - 2;
        for (i = 0; i < width; i+=8) {
	    S32LDI(xr1, src, 0x8);  //xr1:0xFFRGB
	    S32LDI(xr2, src, 0x8);  //xr2:0xFFRGB
	    S32LDI(xr3, src, 0x8);  //xr3:0xFFRGB
	    S32LDI(xr4, src, 0x8);  //xr4:0xFFRGB

	    S32SFL(xr5,xr2,xr1,xr6,0);  //xr5:FFFFR2R1  //xr6:G2G1B2B1
	    S32SFL(xr7,xr4,xr3,xr8,0);  //xr7:FFFFR4R3  //xr8:G4G3B4B3

	    S32SFL(xr0,xr7,xr5,xr3,3);  //xr0:FFFFFFFF  xr3:R4R3R2R1
	    S32SFL(xr7,xr8,xr6,xr5,3);  //xr7:G4G3G2G1  xr5:B4B3B2B1
	    
	    Q8MUL(xr1,xr3,xr15,xr2);  //xr1:R4*66, R2*66 , xr2:R3*66, R1*66
	    Q8MAC_AA(xr1,xr13,xr7,xr2); //xr1:R4*66+G4*129,R2*66+G2*129  xr2: R3*66+G3*129,R1*66+G1*129
	    Q8MAC_AA(xr1,xr5,xr12,xr2); //xr1:R4*66+G4*129+B4*25,R2*66+G2*129+B2*25  xr2: R3*66+G3*129+B3*25,R1*66+G1*129+B1*25
	    Q8MAC_AA(xr1,xr11,xr14,xr2); //xr1:R4*66+G4*129+B4*25+80,R2*66+G2*129+B2*25+80  xr2: R3*66+G3*129+B3*25+80,R1*66+G1*129+B1*25+80

	    S32I2M(xr6, 0x00100010); //U 1,1 112,112
	    Q16SAR(xr1,xr1,xr2,xr2,8); //>>8
	    Q16ACCM_AA(xr1,xr6,xr6,xr2); //Y+16

	    S32SFL(xr0,xr1,xr2,xr1,1);
	    S32SDI(xr1, dest, 0x4);

	    if ((j % 2) == 0) {
	        S32I2M(xr8, 0x12125E5E); //V 18, 18, 94,94
		S32SFL(xr0,xr3,xr7,xr3,1);  //xr3:R3R1G3G1
		S32SFL(xr0,xr14,xr5,xr5,1);  //xr9:8080B3B1
		
		Q8MUL(xr1,xr5,xr9,xr2); //xr1:80*1,80*1, xr2:B3*112,B1*112
		Q8MAC_SS(xr1,xr3,xr10,xr2); //xr1:80-R3*38,80-R1*38, xr2:B3*112-G3*74,B1*112-G1*74
		
		S32SFL(xr5,xr5,xr3,xr3,3);  //xr5:8080R3R1, xr3:B3B1G3G1
		Q8MUL(xr4,xr5,xr9,xr6); //xr4:80*1,80*1,xr5:R3*112, R1*112
		Q8MAC_SS(xr4,xr3,xr8,xr6); //xr4:80*1-B3*18,80*1-B1*18, xr5:R3*112-G3*94, R1*112-G1*94
		Q16ACCM_AA(xr1,xr2,xr6,xr4); //xr1:80-R3*38+B3*112-G3*74(U2),80-R1*38+B1*112-G1*74(U1)  //xr4:80-B3*18+R3*112-G3*94,80-B1*18+R1*112-G1*94 
		Q16SAR(xr1,xr1,xr4,xr4,8); //>>8
		Q16ACCM_AA(xr1,xr14,xr14,xr4); //UV+128
		S32SFL(xr0,xr4,xr1,xr1,2);
		S32SDI(xr1, uvdest,4);
	    }
	}
    }
}



inline void ABGR8888_to_YUV420SP(
    unsigned char *y_dst,
    unsigned char *uv_dst,
    unsigned char *rgb_src,
    unsigned int width,
    unsigned int height)
{
    unsigned int i, j;
    unsigned int *src;

    unsigned int *pSrc = (unsigned int *)rgb_src;
    unsigned char *pDstY = (unsigned char *)y_dst;
    unsigned char *pDstUV = (unsigned char *)uv_dst;
    unsigned int *dest = (unsigned int *)pDstY;
    unsigned int *uvdest = (unsigned int *)pDstUV;


    dest -= 1;
    uvdest -= 1;
    S32I2M(xr15, 0x19814201); //Y bgr80
    S32I2M(xr12, 0x704A2601); //U bgr80
    S32I2M(xr11, 0x125E7001); //V bgr80
    S32I2M(xr14, 0x80000000);
    S32I2M(xr13, 0x00100010); //for Y


    for (j = 0; j < height; j++) {
        src = pSrc + j * fb_width - 1;
        for (i = 0; i < width; i+=4) {
	    S32LDI(xr1, src, 0x4);  //xr1:0xFFBGR:  xr1 and xr3 use for UV
	    S32LDI(xr2, src, 0x4);  //xr2:0xFFBGR
	    S32LDI(xr3, src, 0x4);  //xr3:0xFFBGR
	    S32LDI(xr4, src, 0x4);  //xr4:0xFFBGR

	    S32ALN(xr1, xr1, xr14, 1);  //xr1:BGR80  
	    S32ALN(xr2, xr2, xr14, 1);  //xr2:BGR80
	    S32ALN(xr3, xr3, xr14, 1);  //xr3:BGR80
	    S32ALN(xr4, xr4, xr14, 1);  //xr4:BGR80

	    Q8MUL(xr5,xr1,xr15,xr6);  //xr5: B*25,G*129, xr6:R*66,0x80 first word
	    Q8MUL(xr2,xr2,xr15,xr7);  //xr7: B*25,G*129, xr8:R*66,0x80 second word
	    Q8MUL(xr8,xr3,xr15,xr9);  //xr9: B*25,G*129, xr10:R*66,0x80 third word
	    Q8MUL(xr4,xr4,xr15,xr10);  //xr13: B*25,G*129, xr14:R*66,0x80 fourth word

	    Q16ACCM_AA(xr5,xr6,xr7,xr2); //xr5:255+G*129, B*25+R*66(first), xr2:255+G*129, B*25+R*66(second)
	    Q16ACCM_AA(xr8,xr9,xr10,xr4); //xr8:255+G*129, B*25+R*66(third), xr4:255+G*129, B*25+R*66(fourth)

	    S32SFL(xr2,xr2,xr5,xr5, 3); //xr2:255+G*129(Y2),255+G*129(Y1) xr5:B*25+R*66(Y2),B*25+R*66(Y1) (second)
	    S32SFL(xr4,xr4,xr8,xr8,3);  //xr4:255+G*129(Y4),255+G*129(Y3), xr8:B*25+R*66(Y4),B*25+R*66(Y3)(second)

	    Q16ACCM_AA(xr2,xr5,xr8,xr4); //xr2:Y2+Y2, Y1+Y1, xr4:Y4+Y4, Y3+Y3

	    Q16SLR(xr2,xr2,xr4,xr4,8);  //Y
	    Q16ACCM_AA(xr2,xr13,xr13,xr4); //Y + 16
	    Q16SAT(xr8,xr4,xr2);
	    S32SDI(xr8, dest, 0x4);

	    //xr2, xr4, xr5, xr6, xr7, xr8, xr9, xr10 not use
	    //here for UV 
	    if ((j % 2) == 0){
	        Q8MUL(xr2,xr1,xr12,xr4);  //xr2: B*112,G*74, xr4:R*38,0x80 U1
		Q8MUL(xr1,xr1,xr11,xr6);  //xr5: B*18,G*94, xr6:R*112,0x80 V1
		Q8MUL(xr7,xr3,xr12,xr8);  //xr8: B*112,G*74, xr9:R*38,0x80 U2
		Q8MUL(xr3,xr3,xr11,xr9);  //xr10: B*18,G*94, xr14:R*112,0x80 V2
		
		S32ALN(xr5, xr4, xr4, 2);  //xr1:0x80, R*38 reverse xr4 for U1
		S32ALN(xr10, xr8, xr8, 2);  //xr3:0x80, R*38 reverse xr9 for U2
		S32SFL(xr2,xr2,xr5,xr5, 3);  //xr2:B*112,0x80, xr1:G*74,R*38
		S32SFL(xr7,xr7,xr10,xr10, 3);  //xr8:B*112,0x80, xr3:G*74,R*38
		Q16ACCM_SS(xr6,xr1,xr3,xr9); //xr6:B*112-R*38,-G*74+0x80, V1 xr14: V2
		Q16ACCM_SS(xr2,xr5,xr10,xr7); //xr2:B*112-G*74, 0x80-R*38, U1 xr8: U2
		S32SFL(xr9,xr9,xr6,xr6,3);
		S32SFL(xr7,xr7,xr2,xr2,3);
		Q16ACCM_AA(xr9,xr6,xr7,xr2);  //xr14:V2V1 xr2:U2U1
		
		S32I2M(xr4, 0x00800080); //for UV
		
		S32SFL(xr9,xr9,xr2,xr2,3);  //xr14:V2U2 xr2:V1U1  add
		Q16SLR(xr9,xr9,xr2,xr2,8);  
		S32I2M(xr10, 0x00FF00FF);
		Q16ACCM_AA(xr9,xr4,xr4,xr2); //V+128
		
		S32AND(xr2,xr2,xr10);
		S32AND(xr9,xr9,xr10);
		Q16SAT(xr8,xr9,xr2);
		S32SDI(xr8,uvdest,0x4);
	    }
	}
    }
}


inline void ABGR8888_to_YUV420SP_CHANGED(
    unsigned char *y_dst,
    unsigned char *uv_dst,
    unsigned char *rgb_src,
    unsigned int width,
    unsigned int height)
{
    unsigned int i, j;
    unsigned int *src;

    unsigned int *pSrc = (unsigned int *)rgb_src;
    unsigned char *pDstY = (unsigned char *)y_dst;
    unsigned char *pDstUV = (unsigned char *)uv_dst;
    unsigned int *dest = (unsigned int *)pDstY;
    unsigned int *uvdest = (unsigned int *)pDstUV;


    dest -= 1;
    uvdest -= 1;

    S32I2M(xr15, 0x19814201); //Y bgr80
    S32I2M(xr12, 0x704A2601); //U bgr80
    S32I2M(xr11, 0x125E7001); //V bgr80
    S32I2M(xr14, 0x80000000);
    S32I2M(xr13, 0x00100010); //for Y

    for (j = 0; j < height; j+=2) {
        src = pSrc + j * fb_width - 2;
        for (i = 0; i < width; i+=8) {
	    S32LDI(xr1, src, 0x8);  //xr1:0xFFBGR:  xr1 and xr3 use for UV
	    S32LDI(xr2, src, 0x8);  //xr2:0xFFBGR
	    S32LDI(xr3, src, 0x8);  //xr3:0xFFBGR
	    S32LDI(xr4, src, 0x8);  //xr4:0xFFBGR
	    S32ALN(xr1, xr1, xr14, 1);  //xr1:BGR80  
	    S32ALN(xr2, xr2, xr14, 1);  //xr2:BGR80
	    S32ALN(xr3, xr3, xr14, 1);  //xr3:BGR80
	    S32ALN(xr4, xr4, xr14, 1);  //xr4:BGR80

	    Q8MUL(xr5,xr1,xr15,xr6);  //xr5: B*25,G*129, xr6:R*66,0x80 first word
	    Q8MUL(xr2,xr2,xr15,xr7);  //xr7: B*25,G*129, xr8:R*66,0x80 second word
	    Q8MUL(xr8,xr3,xr15,xr9);  //xr9: B*25,G*129, xr10:R*66,0x80 third word
	    Q8MUL(xr4,xr4,xr15,xr10);  //xr13: B*25,G*129, xr14:R*66,0x80 fourth word

	    Q16ACCM_AA(xr5,xr6,xr7,xr2); //xr5:255+G*129, B*25+R*66(first), xr2:255+G*129, B*25+R*66(second)
	    Q16ACCM_AA(xr8,xr9,xr10,xr4); //xr8:255+G*129, B*25+R*66(third), xr4:255+G*129, B*25+R*66(fourth)

	    S32SFL(xr2,xr2,xr5,xr5, 3); //xr2:255+G*129(Y2),255+G*129(Y1) xr5:B*25+R*66(Y2),B*25+R*66(Y1) (second)
	    S32SFL(xr4,xr4,xr8,xr8,3);  //xr4:255+G*129(Y4),255+G*129(Y3), xr8:B*25+R*66(Y4),B*25+R*66(Y3)(second)

	    Q16ACCM_AA(xr2,xr5,xr8,xr4); //xr2:Y2+Y2, Y1+Y1, xr4:Y4+Y4, Y3+Y3

	    Q16SLR(xr2,xr2,xr4,xr4,8);  //Y
	    Q16ACCM_AA(xr2,xr13,xr13,xr4); //Y + 16
	    Q16SAT(xr8,xr4,xr2);
	    S32SDI(xr8, dest, 0x4);

	    //xr2, xr4, xr5, xr6, xr7, xr8, xr9, xr10 not use
	    //here for UV 
	    if ((j % 4) == 0){
	        Q8MUL(xr2,xr1,xr12,xr4);  //xr2: B*112,G*74, xr4:R*38,0x80 U1
		Q8MUL(xr1,xr1,xr11,xr6);  //xr5: B*18,G*94, xr6:R*112,0x80 V1
		Q8MUL(xr7,xr3,xr12,xr8);  //xr8: B*112,G*74, xr9:R*38,0x80 U2
		Q8MUL(xr3,xr3,xr11,xr9);  //xr10: B*18,G*94, xr14:R*112,0x80 V2
		
		S32ALN(xr5, xr4, xr4, 2);  //xr1:0x80, R*38 reverse xr4 for U1
		S32ALN(xr10, xr8, xr8, 2);  //xr3:0x80, R*38 reverse xr9 for U2
		S32SFL(xr2,xr2,xr5,xr5, 3);  //xr2:B*112,0x80, xr1:G*74,R*38
		S32SFL(xr7,xr7,xr10,xr10, 3);  //xr8:B*112,0x80, xr3:G*74,R*38
		Q16ACCM_SS(xr6,xr1,xr3,xr9); //xr6:B*112-R*38,-G*74+0x80, V1 xr14: V2
		Q16ACCM_SS(xr2,xr5,xr10,xr7); //xr2:B*112-G*74, 0x80-R*38, U1 xr8: U2
		S32SFL(xr9,xr9,xr6,xr6,3);
		S32SFL(xr7,xr7,xr2,xr2,3);
		Q16ACCM_AA(xr9,xr6,xr7,xr2);  //xr14:V2V1 xr2:U2U1
		
		S32I2M(xr4, 0x00800080); //for UV
		
		S32SFL(xr9,xr9,xr2,xr2,3);  //xr14:V2U2 xr2:V1U1  add
		Q16SLR(xr9,xr9,xr2,xr2,8);  
		S32I2M(xr10, 0x00FF00FF);
		Q16ACCM_AA(xr9,xr4,xr4,xr2); //V+128
		
		S32AND(xr2,xr2,xr10);
		S32AND(xr9,xr9,xr10);
		Q16SAT(xr8,xr9,xr2);
		S32SDI(xr8,uvdest,0x4);
	    }
	}
    }
}


 
static void get_framebuffer_data(JNIEnv* env, jobject thiz,jbyteArray data) {
    jbyte* readBuff = NULL;
    readBuff = (jbyte *)env->GetByteArrayElements(data, NULL);

    if (readBuff == NULL) {
        ALOGE("Error retrieving destination for data");
	return;
    }

#ifdef TIME_COST_TESTN
    unsigned int pre_time1 = GetTimer();
#endif

    int *p = NULL;
    p = getfb();

    if (p == NULL) {
        ALOGE("framebuffer is null");
	return;
    }

#ifdef TIME_COST_TESTN
    frameNum++;
    unsigned int time1 = GetTimer() - pre_time1;
    costtime += time1;
    ALOGE("getfb costTime = %u us", time1);
    ALOGE("average getfb costTime = %u us, frameNum = %d", costtime/frameNum,frameNum);
#endif

    
    unsigned char *rgb_src = (unsigned char *)p;
    unsigned char *yuv_dst = (unsigned char *)readBuff;
    unsigned char *uv_dst = yuv_dst + changed_width * changed_height;
    
#ifdef TIME_COST_TESTN
    unsigned int pre_time2 = GetTimer();
#endif

    if (bits_per_pixel == 32) {
#ifdef MACRO_USE_FOR_NON_COLDWAVE
	if (is_shrink) {
	    ARGB8888_to_YUV420SP_CHANGED(yuv_dst, uv_dst, rgb_src, changed_width*2, changed_height*2);
	}else{
	    if (is_multi16 == 0){
	        ARGB8888_to_YUV420SP(yuv_dst, uv_dst, rgb_src, fb_width, fb_height);
	    }else{
	        ARGB8888_to_YUV420SP(yuv_dst, uv_dst, rgb_src, changed_width, changed_height);
	    }
	}
#else
	if (is_shrink) {
	    ABGR8888_to_YUV420SP_CHANGED(yuv_dst, uv_dst, rgb_src, changed_width*2, changed_height*2);
	}else{
	    if (is_multi16 == 0){
	        ABGR8888_to_YUV420SP(yuv_dst, uv_dst, rgb_src, fb_width, fb_height);
	    }else{
	        ABGR8888_to_YUV420SP(yuv_dst, uv_dst, rgb_src, changed_width, changed_height);
	  }
	}
#endif
    }else if (bits_per_pixel == 16){
	if (is_shrink) {
	    RGB565_to_YUV420SP_CHANGED(yuv_dst, uv_dst, rgb_src, changed_width*2, changed_height*2);
	}else{
	  if (is_multi16 == 0){
	    RGB565_to_YUV420SP(yuv_dst, uv_dst, rgb_src, fb_width, fb_height);
	  }else{
	    RGB565_to_YUV420SP(yuv_dst, uv_dst, rgb_src, changed_width, changed_height);
	  }
	}
    }else{
        ALOGE("neither ARGB8888 or RGB565, unsupported");
    }

    const char* fbpath = "/dev/graphics/fb0";
    int fb = open(fbpath, O_RDONLY);
    if (fb > 0) {
        unsigned int index = JZFB_UNLOCK_BUFFER;
	if (ioctl(fb, JZFB_GET_BUFFER, &index) == 0) {
	}else{
	    ALOGE("unlock buffer failed");
	}
    }
    close(fb);

#ifdef TIME_COST_TESTN
    unsigned int time2 = GetTimer() - pre_time2;
    frameNum++;
    costtime += time2;
    ALOGE("rgbtoyuv costTime = %u us", time2);
    ALOGE("average getfb costTime = %u us, frameNum = %d", costtime/frameNum,frameNum);
#endif

    
    if (mapbase != MAP_FAILED) {
        munmap((void *)mapbase, mapsize);
    }

    env->ReleaseByteArrayElements(data, readBuff, 0);
#ifdef TIME_COST_TESTN
    unsigned int time1 = GetTimer() - pre_time1;
    ALOGE("rgbtoyuv costTime = %u us", time1);
#endif
}


static jint get_frame_width(JNIEnv* env, jobject thiz) {
    return (jint)changed_width;
}

static jint  get_frame_height(JNIEnv* env, jobject thiz) {
    return (jint)changed_height;
}


static void get_picture_size(JNIEnv* env, jobject thiz) {
    unsigned int f1, bytespp;
    const char* fbpath = "/dev/graphics/fb0";
    frameNum = 0;
    is_shrink = 0;
    is_multi16 = 0;
    int fb = open(fbpath, O_RDONLY);

    costtime = 0;
    if (fb >= 0) {
        struct fb_var_screeninfo vinfo;
	struct fb_fix_screeninfo finfo;
	if (ioctl(fb, FBIOGET_VSCREENINFO, &vinfo) == 0) {
	    uint32_t bytespp;
	    if (vinfoToPixelFormat(vinfo, &bytespp, &f1) == NO_ERROR) {
	        size_t offset = (vinfo.xoffset + vinfo.yoffset*vinfo.xres) * bytespp;
		fb_width = vinfo.xres;
		fb_height = vinfo.yres;
		if (ioctl(fb, FBIOGET_FSCREENINFO, &finfo) == 0) {
		    s = finfo.line_length/bytespp;
		    frame_size = fb_height*s*bytespp;
		}else {
		    s = vinfo.xres;
		    frame_size = fb_width*fb_height*bytespp;
		}
		map_offset = (vinfo.xoffset + vinfo.yoffset*s) * bytespp;
		mapsize = map_offset + frame_size;
	    }else{
	        fb_width = default_frame_width;
		fb_height = default_frame_height;
	    }
	}
    }else{
        fb_width = default_frame_width;
	fb_height = default_frame_height;
    }

    ALOGE("fb_width = %d,fb_height=%d",fb_width,fb_height);

    if (fb_width >= 640) {
        changed_width = fb_width / 2;
	changed_height = fb_height / 2;
	is_shrink = 1;
	if (changed_width % 16) {
	    is_multi16 = 1;
	    changed_width = changed_width / 16 * 16;  
	}
	if (changed_height % 16) {
	    is_multi16 = 1;
	    changed_height = changed_height / 16 * 16;  
	}
    }else{
        changed_width = fb_width;
	changed_height = fb_height;
	if (changed_width % 16) {
	    is_multi16 = 1;
	    changed_width = changed_width / 16 * 16;  
	}
	if (changed_height % 16) {
	    is_multi16 = 1;
	    changed_height = changed_height / 16 * 16;  
	}
    }

    close(fb);
    if (init_rgb_tab == 0) {
        S32I2M(xr16, 0x7);
	init_rgb_tab = 1;
    }
}

   
static const char* const kClassPathName = "cn/ingenic/glasssync/screen/screenshare/AvcEncode";

static JNINativeMethod gMethods[] = {
    {"get_width","()I",(void *)get_frame_width},
    {"get_height","()I",(void *)get_frame_height},
    {"get_picture","()V",(void *)get_picture_size},
    {"get_frameData","([B)V",(void *)get_framebuffer_data},
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
    
    if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        ALOGE("ERROR: GetEnv failed\n");
	return result;
    }
    assert(env != NULL);
    
    if (register_android_mmi(env) < 0) {
        ALOGE("ERROR: colorspace changed native registration failed\n");
	return result;
    }

    result = JNI_VERSION_1_4;
	
    ALOGE("JNI_OnLoad end\n");
    return result;
}
 
 


