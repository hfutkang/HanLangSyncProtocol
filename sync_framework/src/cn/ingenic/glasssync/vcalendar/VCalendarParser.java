package cn.ingenic.glasssync.vcalendar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import android.util.Log;

public class VCalendarParser {

	private static final String LOG_TAG = "VCalendarParser";
	protected BufferedReader mReader;
	protected VCalendarInterpreter mBuilder = null;
	
	protected String mEncoding = null;
	//private int mNestCount;
	 // Used only for parsing END:VCALENDAR.
    private String mPreviousLine;
    protected boolean mCanceled;
    
    static private final int STATE_GROUP_OR_PROPNAME = 0;
    static private final int STATE_PARAMS = 1;
	
    
    private boolean parseOneVCalendar(boolean firstReading) throws IOException, VCalendarException{
    	boolean allowGarbage = false;
    	if(firstReading){
    		/*Log.i(LOG_TAG, "in if");
    				if(!readBeginVCalendar(allowGarbage)){
    					Log.i(LOG_TAG, "in if if");
    					return false;
    				}
    				allowGarbage = true;*/
    	}
    	if(!readBeginVCalendar(allowGarbage)){
    		return false;
    	}
    	if(mBuilder != null){
    		mBuilder.startEntry();
    	}
    	Log.i(LOG_TAG, "befor parseItems()");
    	parseItems();
    	Log.i(LOG_TAG, "after parseItems()");
    	readEndVCalendar(true,false);
    	Log.i(LOG_TAG, "after readEndVCalendar()");
    	if(mBuilder != null){
    		mBuilder.endEntry();
    	}
    	return true;
    }
    
	protected boolean readBeginVCalendar(boolean allowGarbage)throws IOException, VCalendarException{
		String line;
		do{
			while(true){
				line = getLine();
				if(line == null){
					return false;
				}else if(line.trim().length() > 0){
					break;
				}
			}
			String[] strArray = line.split(":", 2);
			int length = strArray.length;
			if(length == 2 &&
					strArray[0].trim().equalsIgnoreCase("BEGIN")&&
					strArray[1].trim().equalsIgnoreCase("VCALENDAR")){
				return true;
			}else if(!allowGarbage){
				//mPreviousLine = line;
				//Log.i("readBegin", "in else if mPreviousLine =  "+ mPreviousLine );
			}
		}while(allowGarbage);
		throw new VCalendarException("Reached where must not be reached.");
	}
	
    protected void readEndVCalendar(boolean useCache, boolean allowGarbage)
    					throws IOException, VCalendarException {
    	String line;
		do {
		    if (useCache) {
		        // Though vCard specification does not allow lower cases,
		        // some data may have them, so we allow it.
		        line = mPreviousLine;
		    } else {
		        while (true) {
		            line = getLine();
		            if (line == null) {
		                throw new VCalendarException("Expected END:VCALENDAR was not found.");
		            } else if (line.trim().length() > 0) {
		                break;
		            }
		        }
		    }
		    String[] strArray = line.split(":", 2);
		    
		    if (strArray.length == 2 &&
		            strArray[0].trim().equalsIgnoreCase("END") &&
		            strArray[1].trim().equalsIgnoreCase("VEVENT")) {
		    	getLine();
		        return;
		    } else if (!allowGarbage) {
		        throw new VCalendarException("END:VCALENDAR != \"" + mPreviousLine + "\"");
		    }
		    useCache = false;
		} while (allowGarbage);
}
	protected String getLine() throws IOException{
		return mReader.readLine();
	}
	protected String getNonEmptyLine() throws IOException, VCalendarException{
		String line;
		while(true){
			line = getLine();
			if(line == null){
				throw new VCalendarException("Reached end of buffer.");
			}else if(line.trim().length() > 0){
				return line;
			}
		}
	}
	protected void parseItems() throws IOException, VCalendarException{
		boolean ended = false;
		
		if(mBuilder != null){
			mBuilder.startProperty();
		}
		ended = parseItem();
		if(mBuilder != null && !ended){
			mBuilder.endProperty();
		}
		while(!ended){
			if(mBuilder != null){
				mBuilder.startProperty();
			}
			try {
				ended = parseItem();
			} catch (Exception e) {
				Log.e(LOG_TAG, "Invalid line which looks like some comment was found. Ignored.");
				ended = false;
			}
			if(mBuilder != null && !ended){
				mBuilder.endProperty();
			}
			
		}
	}
	
	/**
	 * item = [groups "."]name    [params] ":" value CRLF
	 * 
	 */
	protected boolean parseItem() throws IOException,VCalendarException{
		final String line = getNonEmptyLine();
		String[] properyNameAndValue = separateLine(line);
		if(properyNameAndValue == null){
			return true;
		}
		if(properyNameAndValue.length != 2){
			throw new VCalendarException("Invalid line \"" + line + "\"");
		}
		else {return false;}
	}
	
	private String[] separateLine(String line) throws VCalendarException, IOException{
		int state = STATE_GROUP_OR_PROPNAME;
		int nameIndex = 0;
		final String[] propertyNameAndValue = new String[2];
		final int length = line.length();
		Log.i(LOG_TAG, "length " + length);
		if(length > 0 && line.charAt(0) == '#'){
			throw new VCalendarException();
		}
		Log.i(LOG_TAG, "in separateLine()  line " + line);
		for(int i = 0; i <length ; i++){
			char ch = line.charAt(i);
			switch(state){
			case STATE_GROUP_OR_PROPNAME:
				if(ch == ':'){
					final String propertyName = line.substring(nameIndex,i);
					Log.i(LOG_TAG, "propertyName " + propertyName);
					if(propertyName.equalsIgnoreCase("END")){
						mPreviousLine = line;
						Log.i(LOG_TAG, "mPreviousLine " + mPreviousLine);
						return null;
					}
					if(mBuilder != null){
						mBuilder.propertyName(propertyName);
					}
					propertyNameAndValue[0] = propertyName;
					if(i < length - 1){
						propertyNameAndValue[1] = line.substring(i + 1);
					}else{
						propertyNameAndValue[1] = "";
					}
					// add by hhyuan for PROPERTY_RRULE
					// RRULE value like: FREQ=MONTHLY\;WKST=SU\;BYMONTHDAY=13
					// the '\' character is added when we build it before
					// because the ';' is item end tag
					// so we should distinct it,'\' must be replaced to ''.
					if (propertyName.equals(VCalendarConstants.PROPERTY_RRULE)) {
						int rrule_length = propertyNameAndValue[1].length();
						StringBuilder sb = new StringBuilder();
						for (int k = 0; k < rrule_length; k++) {
							char rr_ch = propertyNameAndValue[1].charAt(k);
							if (k < rrule_length - 1) {
								char next_rr_ch = propertyNameAndValue[1]
										.charAt(k + 1);
								if (rr_ch == '\\' && next_rr_ch == ';') {
									continue;
								}
							}
							sb.append(rr_ch);
						}
						propertyNameAndValue[1] = sb.toString();
					}
					mBuilder.propertyValue(propertyNameAndValue[1]);
					
					return propertyNameAndValue;
				}else if(ch == ';'){
					String propertyName = line.substring(nameIndex, i);
					if(propertyName.equalsIgnoreCase("END")){
						mPreviousLine = line;
						Log.i(LOG_TAG, "mPreviousLine " + mPreviousLine);
						return null;
						
					}
					if(mBuilder != null){
						mBuilder.propertyName(propertyName);
					}
					propertyNameAndValue[0] = propertyName;
					nameIndex = i + 1;
					state = STATE_PARAMS;
				}
				break;
			case STATE_PARAMS:
				if(ch == ';'){
					handleParams(line.substring(nameIndex,i));
					nameIndex = i +1;
				}else if(ch == ':'){
					handleParams(line.substring(nameIndex, i));
					if(i < length - 1){
						propertyNameAndValue[1] = line.substring(i + 1);
					}else{
						propertyNameAndValue[1] = "";
					}
					mBuilder.propertyValue(propertyNameAndValue[1]);
					return propertyNameAndValue;
				}
				break;
			}
		}
		return null;
	}

	private void handleParams(String params) throws VCalendarException{
		String[] strArray = params.split("=",2);
		if(strArray.length == 2){
			final String paramName = strArray[0].trim().toUpperCase();
			String paramValue = strArray[1].trim();
			if(paramName.equals("ENCODING")){
				handleEncoding(paramValue);
			}else if(paramName.equals("CHARSET")){
				handleCharset(paramValue);
			}else if(paramName.equals("LANGUAGE")){
				handleLanguage(paramValue);
			}else{
				throw new VCalendarException("Unknown type \"" + paramName + "\"");
			}
		}
	}

	private void handleLanguage(String langval) throws VCalendarException{
		String[] strArray = langval.split("-");
        if (strArray.length != 2) {
            throw new VCalendarException("Invalid Language: \"" + langval + "\"");
        }
        String tmp = strArray[0];
        int length = tmp.length();
        for (int i = 0; i < length; i++) {
            if (!isLetter(tmp.charAt(i))) {
                throw new VCalendarException("Invalid Language: \"" + langval + "\"");
            }
        }
        tmp = strArray[1];
        length = tmp.length();
        for (int i = 0; i < length; i++) {
            if (!isLetter(tmp.charAt(i))) {
                throw new VCalendarException("Invalid Language: \"" + langval + "\"");
            }
        }
        if (mBuilder != null) {
            mBuilder.propertyParamType("LANGUAGE");
            mBuilder.propertyParamValue(langval);
        }
	}

	private boolean isLetter(char ch) {
		if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) {
            return true;
        }
        return false;
	}

	private void handleCharset(String charsetval) {
		if (mBuilder != null) {
            mBuilder.propertyParamType("CHARSET");
            mBuilder.propertyParamValue(charsetval);
        }
		
	}

	private void handleEncoding(String pencodingval) {
		if(mBuilder != null){
			mBuilder.propertyParamType("ENCODING");
            mBuilder.propertyParamValue(pencodingval);
		}
		mEncoding = pencodingval;
	}

	public void parse(InputStream is,String charset,VCalendarInterpreter builder) throws IOException,VCalendarException{
		if(charset == null){
			charset = "utf-8";
		}
		final InputStreamReader tmpReader = new InputStreamReader(is,charset);
		mReader = new BufferedReader(tmpReader);
		mBuilder = builder;
		if(mBuilder != null){
			mBuilder.start();
		}
		Log.i(LOG_TAG, "tmpReader = " + tmpReader);
		Log.i(LOG_TAG, "mReader = " + mReader.toString());
		if(mBuilder != null){
			mBuilder.start();
		}
		parseVCalendarFile();
		if(mBuilder != null){
			mBuilder.end();
		}
	}
	
	protected void parseVCalendarFile() throws IOException,VCalendarException{
		boolean firstReading = true;
		while(true){
			boolean parseOneVCalendar = parseOneVCalendar(firstReading);
			Log.i(LOG_TAG, "parseOneVCard(firstReading) = " + parseOneVCalendar);
			if(!parseOneVCalendar){
				break;
			}
			firstReading = false;
		}
		boolean useCache = true;
		readEndVCalendar(useCache,true);
		useCache = false;
	}
	
	public void cancel() {
        mCanceled = true;
    }
	
}
