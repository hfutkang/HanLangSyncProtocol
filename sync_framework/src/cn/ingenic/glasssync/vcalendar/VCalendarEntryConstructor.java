package cn.ingenic.glasssync.vcalendar;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;

import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;

public class VCalendarEntryConstructor implements VCalendarInterpreter {

	private static String LOG_TAG = "VCalendarEntryConstructor";
	private VCalendarEntry.Property mCurrentProperty = new VCalendarEntry.Property();
	private VCalendarEntry mCurrentContactStruct;
	private String mParamType;
	 
	private final String mCharsetForDecodeBytes;
	private final boolean mStrictLineBreakParsing ;
	
	private String mInputCharset;
	
	private ArrayList<Uri> mCreateUris = new ArrayList<Uri>();
	private ContentResolver mContentResolver;
	
	private long calendar_id =-1;
	
	public VCalendarEntryConstructor(final String charsetForDetodedBytes,final String inputCharset,
			ContentResolver contentResolver,final boolean strickLineBreakParsing,final long calendar_id) {
		this.mContentResolver = contentResolver;
		if(inputCharset != null){
			mInputCharset = inputCharset;
		}else{
			mInputCharset = "UTF-8";
		}
		if(charsetForDetodedBytes != null){
			mCharsetForDecodeBytes = charsetForDetodedBytes;
		}else{
			mCharsetForDecodeBytes = "UTF-8";
		}
		mStrictLineBreakParsing = strickLineBreakParsing;
		
		this.calendar_id =calendar_id;
	}
	
	public void end() {

	}


	public void endEntry() {
		//mCurrentContactStruct.consolidateFields();
		onEntryCreaded(mCurrentContactStruct,calendar_id);
		mCurrentContactStruct = null;
		mCurrentProperty.clear();
	}
	public void onEntryCreaded(final VCalendarEntry contactStruct,long calendarId){
		Uri pushIntoContentResolver = contactStruct.pushIntoContentResolver(mContentResolver,calendarId);
		mCreateUris.add(pushIntoContentResolver);
	}

	public void endProperty() {
		mCurrentContactStruct.addProperty(mCurrentProperty);
		
	}

	public void propertyGroup(String group) {
		// TODO Auto-generated method stub

	}

	public void propertyName(String name) {
		mCurrentProperty.setPropertyName(name);
	}

	public void propertyParamType(String type) {
		if(mParamType != null){
			Log.e(LOG_TAG, "propertyParamType() is called more than once " +
            "before propertyParamValue() is called");
		}
		mParamType = type;
	}

	public void propertyParamValue(String value) {
		if(mParamType == null){
			mParamType = "TYPE";
		}
		mCurrentProperty.addParameter(mParamType, value);
		mParamType = null;
	}

	public void propertyValue(String value) {
		String charsetForDecodeBytes = mCharsetForDecodeBytes;
		String encodeing = "QUOTED-PRINTABLE";
		String handleValue = handleValue(value,charsetForDecodeBytes,encodeing);
		mCurrentProperty.setPropertyValue(handleValue);

	}

	private String handleValue(String value, String charsetForDecodedBytes,
			String encodeing) {
		if(encodeing.equals("QUOTED-PRINTABLE")){
			StringBuilder builder = new StringBuilder();
			int length = value.length();
			for(int i = 0; i < length; i++){
				char ch = value.charAt(i);
				if(ch == '=' && i < length - 1){
					char nextCh = value.charAt(i + 1);
					if(nextCh == ' ' || nextCh == '\t'){
						builder.append(nextCh);
						i++;
						continue;
					}
				}
				builder.append(ch);
			}
			String quotedPrintable = builder.toString();
			String[] lines;
			if(mStrictLineBreakParsing){
				lines = quotedPrintable.split("\r\n");
			}else{
				 builder = new StringBuilder();
                 length = quotedPrintable.length();
                 ArrayList<String> list = new ArrayList<String>();
                 for (int i = 0; i < length; i++) {
                     char ch = quotedPrintable.charAt(i);
                     if (ch == '\n') {
                         list.add(builder.toString());
                         builder = new StringBuilder();
                     } else if (ch == '\r') {
                         list.add(builder.toString());
                         builder = new StringBuilder();
                         if (i < length - 1) {
                             char nextCh = quotedPrintable.charAt(i + 1);
                             if (nextCh == '\n') {
                                 i++;
                             }
                         }
                     } else {
                         builder.append(ch);
                     }
                 }
                 String finalLine = builder.toString();
                 if (finalLine.length() > 0) {
                     list.add(finalLine);
                 }
                 lines = list.toArray(new String[0]);
			}
			builder = new StringBuilder();
			for(String line: lines){
				if(line.endsWith("=")){
					line = line.substring(0,line.length() - 1);
				}
				builder.append(line);
			}
			byte[] bytes;
			try {
				bytes = builder.toString().getBytes(mInputCharset);
			} catch (UnsupportedEncodingException e) {
				Log.e(LOG_TAG, "Failed to encode: charset=" + mInputCharset);
                bytes = builder.toString().getBytes();
			}
			//note by hhyuan :when decode the rrule value ,it throw DecoderException 
			// because the rrule value contains '=' character ,and this decodeQuotedPrintable
			//like useness here ,if note it results new error decode ,can use it again.
//			try {
//				bytes = QuotedPrintableCodecPort.decodeQuotedPrintable(bytes);
//			} catch (DecoderException e) {
//				Log.e(LOG_TAG, "Failed to decode quoted-printable: " + e);
//                return "";
//			}
			try {
				return new String(bytes,charsetForDecodedBytes);
			} catch (UnsupportedEncodingException e) {
				Log.e(LOG_TAG, "Failed to encode: charset=" + charsetForDecodedBytes);
                return new String(bytes);
			}
		}
		return encodeString(value,charsetForDecodedBytes);
	}


	private String encodeString(String originalString, String charsetForDecodedBytes) {
		if (mInputCharset.equalsIgnoreCase(charsetForDecodedBytes)) {
            return originalString;
        }
		Charset charset = Charset.forName(mInputCharset);
        ByteBuffer byteBuffer = charset.encode(originalString);
        // byteBuffer.array() "may" return byte array which is larger than
        // byteBuffer.remaining(). Here, we keep on the safe side.
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        try {
            return new String(bytes, charsetForDecodedBytes);
        } catch (UnsupportedEncodingException e) {
            Log.e(LOG_TAG, "Failed to encode: charset=" + charsetForDecodedBytes);
            return null;
        }
	}


	public void start() {
		
	}

	public void startEntry() {
		if(mCurrentContactStruct != null){
			Log.i(LOG_TAG,"Nested VCalendar code is not supported now.");
		}
		mCurrentContactStruct = new VCalendarEntry();
	}

	public void startProperty() {
		Log.i(LOG_TAG,"in startProperty()");
		mCurrentProperty.clear();
	}
	
	public ArrayList<Uri> getCreatedUris(){
		return mCreateUris;
	}
	
	
	/**
     * See org.apache.commons.codec.DecoderException
     */
    private static class DecoderException extends Exception {
        public DecoderException(String pMessage) {
            super(pMessage);
        }
    }

    /**
     * See org.apache.commons.codec.net.QuotedPrintableCodec
     */
    private static class QuotedPrintableCodecPort {
        private static byte ESCAPE_CHAR = '=';
        public static final byte[] decodeQuotedPrintable(byte[] bytes)
                throws DecoderException {
            if (bytes == null) {
                return null;
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            for (int i = 0; i < bytes.length; i++) {
                int b = bytes[i];
                if (b == ESCAPE_CHAR) {
                    try {
                        int u = Character.digit((char) bytes[++i], 16);
                        int l = Character.digit((char) bytes[++i], 16);
                        if (u == -1 || l == -1) {
                            throw new DecoderException("Invalid quoted-printable encoding");
                        }
                        buffer.write((char) ((u << 4) + l));
                    } catch (ArrayIndexOutOfBoundsException e) {
                        throw new DecoderException("Invalid quoted-printable encoding");
                    }
                } else {
                    buffer.write(b);
                }
            }
            return buffer.toByteArray();
        }
    }


}
