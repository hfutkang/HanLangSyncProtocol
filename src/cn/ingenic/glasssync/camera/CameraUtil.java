/*
 * Copyright (C) 2009 The Android Open Source Project
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

package cn.ingenic.glasssync.camera;

import cn.ingenic.glasssync.R;

import android.app.Activity;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.util.Log;
import android.widget.Toast;
import android.view.Gravity;
import android.view.OrientationEventListener;
import android.view.Surface;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.NoSuchMethodError;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Collection of utility functions used in this package.
 */
public class CameraUtil {
    private static final String TAG = "CameraUtil";

    private static final boolean CHECK_RATIO = false;
    public static final String SDCARD =
	Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString();

    private CameraUtil() {
    }

    public static int getDisplayRotation(Activity activity) {
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        switch (rotation) {
            case Surface.ROTATION_0: return 0;
            case Surface.ROTATION_90: return 90;
            case Surface.ROTATION_180: return 180;
            case Surface.ROTATION_270: return 270;
        }
        return 0;
    }

    public static int getDisplayOrientation(int degrees, int cameraId) {
        // See android.hardware.Camera.setDisplayOrientation for
        // documentation.
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    public static int getJpegRotation(int cameraId, int orientation) {
        // See android.hardware.Camera.Parameters.setRotation for
        // documentation.
        int rotation = 0;
        if (orientation != OrientationEventListener.ORIENTATION_UNKNOWN) {
            CameraInfo info  = new CameraInfo();
	    android.hardware.Camera.getCameraInfo(cameraId, info);
            if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                rotation = (info.orientation - orientation + 360) % 360;
            } else {  // back-facing camera
                rotation = (info.orientation + orientation) % 360;
            }
        }
        return rotation;
    }

    public static Bitmap rotateBitmap(Bitmap b, int degrees) {
        if (degrees != 0 && b != null) {
            Matrix m = new Matrix();
            m.setRotate(degrees,
                    (float) b.getWidth() / 2, (float) b.getHeight() / 2);
            try {
                Bitmap newBitmap = Bitmap.createBitmap(
                        b, 0, 0, b.getWidth(), b.getHeight(), m, true);
                if (b != newBitmap) {
                    b.recycle();
                    b = newBitmap;
                }
            } catch (OutOfMemoryError ex) {
                
	    }
	}
	return b;
    }

    public static Size getOptimalPreviewSize(int w, int h, List<Size> sizes) {
        if (sizes == null) return null;

        Size optimalSize = null;

	if (CHECK_RATIO){
	    double minDiff = Double.MAX_VALUE;
	    int targetHeight = Math.min(w, h);

	    // Use a very small tolerance because we want an exact match.
	    final double ASPECT_TOLERANCE = 0.001;
	    double targetRatio = (double)4.0 / 3;
	    // Try to find an size match aspect ratio and size
	    for (Size size : sizes) {
		double ratio = (double) size.width / size.height;
		if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
		if (Math.abs(size.height - targetHeight) < minDiff) {
		    optimalSize = size;
		    minDiff = Math.abs(size.height - targetHeight);
		}
	    }
	}

        // Don't check aspect ratio, or cannot find the one match the aspect ratio.
        // select min one.
        if (optimalSize == null) {
	    return getMinPreviewSize(sizes);
	}

	return optimalSize;
    }

    public static Size getMinPreviewSize(List<Size> sizes) {
	if (sizes == null) return null;

	Size minSize = sizes.get(0);
	for (Size size : sizes) {
	    if (size.height < minSize.height || size.width < minSize.width) {
		minSize = size;
	    }
	}
        return minSize;
    }

    public static boolean saveJpegPicture(Activity mAcitvity, byte[] jpeg, long dateTaken,
					  int pictureWidth, int pictureHeight) {
	
	SimpleDateFormat mFormat = new SimpleDateFormat(mAcitvity.getString(R.string.image_file_name_format));
	Date date = new Date(dateTaken);
	String title = mFormat.format(date);

	String mPath = "nopath";

	if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())){
	    mPath = SDCARD + '/' + title + ".jpg";
	    Log.i(TAG,"external storage:"+mPath);
	}else{
	    Toast mStorageToast = Toast.makeText(mAcitvity, R.string.no_storage,
						 Toast.LENGTH_SHORT);
	    mStorageToast.setGravity(Gravity.CENTER,0,0);
	    mStorageToast.show();
	    return false;
	}

	if (mPath.equals("nopath")) return false;

        String tmpPath = mPath + ".tmp";
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(tmpPath);
            out.write(jpeg);
            out.close();
            new File(tmpPath).renameTo(new File(mPath));
        } catch (Exception e) {
            Log.e(TAG, "Failed to write image", e);
            return false;
        } finally {
            try {
                out.close();
            } catch (Exception e) {
            }
        }

	int orientation = Exif.getOrientation(jpeg);
	int width, height;
	if ((// mJpegRotation +
	     orientation) % 180 == 0) {
	    width = pictureWidth;
	    height = pictureHeight;
	} else {
	    width = pictureHeight;
	    height = pictureWidth;
	}

        // Insert into MediaStore.
        ContentValues values = new ContentValues(7);
        values.put(ImageColumns.TITLE, title);
        values.put(ImageColumns.DISPLAY_NAME, title + ".jpg");
        values.put(ImageColumns.MIME_TYPE, "image/jpeg");
        // Clockwise rotation in degrees. 0, 90, 180, or 270.
        values.put(ImageColumns.ORIENTATION, 90);
        values.put(ImageColumns.SIZE, jpeg.length);
        values.put(ImageColumns.WIDTH, width);
        values.put(ImageColumns.HEIGHT, height);

        try {
            mAcitvity.getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values);
        } catch (Throwable th) {
            Log.e(TAG, "Failed to insert image" + th);
            return false;
        }
	
	Toast mToast = Toast.makeText(mAcitvity,
				      String.format(mAcitvity.getString(R.string.picture_path_message), mPath),
				      Toast.LENGTH_SHORT);
	mToast.setGravity(Gravity.CENTER,0,0);
	mToast.show();
        return true;
    }
}
