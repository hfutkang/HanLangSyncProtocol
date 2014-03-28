package cn.ingenic.glasssync.camera;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.util.Log;
import android.widget.Toast;

import java.util.UUID;

public class CameraBase extends Activity{
    public static final String TAG = "CameraBase";

    public static final int expectFrameRate = 5;

    public static final UUID PREVIEW_UUID = UUID.fromString("010100d5-afac-11de-8a39-0800200c9a66");

    public int defaultAngle = 90;
    
    public void showToast(int message){
	showToast(getString(message));
    }

    public void showToast(String message){
	Toast mToast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
	mToast.setGravity(Gravity.CENTER,0,0);
	mToast.show();
    }

    public void setDefaultAngle(int rotation){
	defaultAngle = rotation;
    }

    public int getDefaultAngle(){
	return defaultAngle;
    }

    public void recoverAngle(){
	defaultAngle = 90;
    }

    public void setFrameToDisplay(byte[] data, SurfaceHolder holder, Rect dst){
	Log.i(TAG,"setFrameToDisplay==>data.length:"+data.length);
	byte[] newData = data;
	Bitmap bitmap = BitmapFactory.decodeByteArray(newData, 0, newData.length);
	bitmap = CameraUtil.rotateBitmap(bitmap, getDefaultAngle());

	Canvas canvas = holder.lockCanvas(dst);
	if (canvas != null){
	    Rect src = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
	    canvas.drawBitmap(bitmap, src, dst, null);
	    holder.unlockCanvasAndPost(canvas);
	}
    }
}
