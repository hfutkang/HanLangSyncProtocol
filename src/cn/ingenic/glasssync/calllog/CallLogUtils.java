package cn.ingenic.glasssync.calllog;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.database.Cursor;
import android.provider.CallLog.Calls;

public class CallLogUtils {
	
	public static ContentValues json2ContentValues(JSONObject obj){
		ContentValues cv = new ContentValues();
		try {
			putIntIntoContentValue(cv, obj, Calls._ID);
			putStringIntoContentValue(cv, obj, Calls.NUMBER);
			putLongIntoContentValue(cv, obj, Calls.DATE);
			putIntIntoContentValue(cv, obj, Calls.DURATION);
			putIntIntoContentValue(cv, obj, Calls.TYPE);
			putIntIntoContentValue(cv, obj, Calls.NEW);
			putStringIntoContentValue(cv, obj, Calls.CACHED_NAME);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return cv;
	}
	
	private static void putIntIntoContentValue(ContentValues cv,
			JSONObject obj, String name) throws JSONException{
		cv.put(name, obj.getInt(name));
	}

	private static void putStringIntoContentValue(ContentValues cv,
			JSONObject obj,  String name) throws JSONException{
		cv.put(name, obj.getString(name));
	}
	
	private static void putLongIntoContentValue(ContentValues cv,
			JSONObject obj,  String name) throws JSONException{
		cv.put(name, obj.getLong(name));
	}
	
	public static JSONObject cursor2Json(Cursor c){
		JSONObject obj = new JSONObject();
		putIntIntoJson(obj, c, Calls._ID);
		putStringIntoJson(obj, c, Calls.NUMBER);
		putLongIntoJson(obj, c, Calls.DATE);
		putIntIntoJson(obj, c, Calls.DURATION);
		putIntIntoJson(obj, c, Calls.TYPE);
		putIntIntoJson(obj, c, Calls.NEW);
		putStringIntoJson(obj, c, Calls.CACHED_NAME);
		return obj;
	}
	
	private static void putIntIntoJson(JSONObject obj, Cursor c, String column){
		try {
			obj.put(column, c.getInt(c.getColumnIndex(column)));
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}	
	
	private static void putLongIntoJson(JSONObject obj, Cursor c, String column){
		try {
			obj.put(column, c.getLong(c.getColumnIndex(column)));
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	private static void putStringIntoJson(JSONObject obj, Cursor c, String column){
		try {
			String value = c.getString(c.getColumnIndex(column));
			if(value == null){
				value = "";
			}
			obj.put(column, value);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
}
