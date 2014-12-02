package cn.ingenic.glasssync.sms;

import android.net.Uri;

public class Sms {

	public static boolean DEBUG = true;
	public static String TAG = "<sms_watch>";

	public static String THREAD_NAME = "thread";
	public static String SMS_NAME="mid_dest";

	public static int UN_READ = 0;
	public static int NO_ERROR = 0;
	
	public static String NEW_SMS_ACTION="indroid.new.sms.action";

	public static final int MESSAGE_TYPE_ALL = 0;
	public static final int MESSAGE_TYPE_INBOX = 1;
	public static final int MESSAGE_TYPE_SENT = 2;
	public static final int MESSAGE_TYPE_DRAFT = 3;
	public static final int MESSAGE_TYPE_OUTBOX = 4;
	public static final int MESSAGE_TYPE_FAILED = 5; // for failed outgoing
														// messages
	public static final int MESSAGE_TYPE_QUEUED = 6; // for messages to send
														// later

	public interface ThreadColumns {
		public static String ID = "_id";
		public static String DATE = "date";
//		public static String MESSAGE_COUNT = "message_count";
//		public static String SNIPPET = "snippet";
//		public static String RECIPIENT_ADS = "recipient_ads";
//		public static String READ = "read";
//		public static String TYPE = "type";
//		public static String ERROR = "error";
		public static String PHONE_THREAD_ID = "phone_thread_id";
	}

	public interface SmsColumns {
		public static String ID = "_id";
		public static String ADDRESS = "address";
		public static String DATE = "date";
		public static String READ = "read";
		public static String TYPE = "type";
		public static String BODY = "body";
		public static String ERROR_CODE = "error_code";
		public static String SEEN = "seen";
		public static String PHONE_THREAD_ID = "phone_thread_id";
	}

	public static String MID_KEY_COLUMN = "mid_key";

	public static Uri getSmsNotifyUri() {
		return SmsEntry.SmsUri.SMS_URI;
	}

	public static Uri getThreadNotifyUri() {
		return ThreadEntry.ThreadUri.THREAD_URI;
	}

	// public interface SmsKey{
	// public static String ID_KEY="s_id";
	// public static String ADDRESS_KEY="s_address";
	// public static String BODY_KEY="s_body";
	// public static String DATE_KEY="s_data";
	// public static String READ_KEY="s_read_key";
	// public static String TYPE_KEY="s_type_key";
	// public static String ERROR_CODE_KEY="s_error_code";
	// public static String SEEN_KEY="s_seen";
	// }
	//
	// public interface ThreadKey{
	// public static String ID_KEY="t_id";
	// public static String DATE_KEY="t_date_key";
	// public static String MESSAGE_COUNT_KEY="t_message_count_key";
	// public static String SNIPPET_KEY="t_snippet";
	// public static String RECIPIENT_ADS_KEY="t_recipient_ids";
	// public static String READ_KEY="t_read";
	// public static String TYPE_KEY="t_type";
	// public static String ERROR_KEY="t_error_key";
	// public static String SMS_ID="t_sms_id";
	// public static String PHONE_THREAD_ID_KEY="phone_thread_id_key";
	// }

}
