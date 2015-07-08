package cn.ingenic.glasssync.contact;


import android.content.Context;
import android.util.Log;
import cn.ingenic.glasssync.services.SyncModule;
import cn.ingenic.glasssync.services.mid.MidTableManager;
import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncException;

public class ContactsLiteModule extends SyncModule {
	//TODO this name should in a common library, maybe sync_framework
	private static final String MODULE_NAME = "CONTACTS";
	private static final String TAG = MODULE_NAME;
	private static SyncModule sInstance;
        private static final String SYNC_REQUEST = "sync_request"; //The length of not more than 15
	private ContactsLiteModule(Context context) {
		super(MODULE_NAME, context);
		setSyncEnable(false);
	}
	
	public synchronized static SyncModule getInstance(Context context) {
		if (sInstance == null) {
			sInstance = new ContactsLiteModule(context.getApplicationContext());
		}
		return sInstance;
	}
	
	@Override
	protected void onCreate() {
	   Log.i(TAG,"ContactsModule created .");
	}

	
	@Override
	public MidTableManager getMidTableManager() {
		return ContactsLiteMidDestManager.getInstance(mContext, this);
	}

        @Override
	protected void onRetrive(SyncData data) {
	    boolean enabled = data.getBoolean(SYNC_REQUEST,false);
	    Log.d(TAG, "---onRetrive enabled="+enabled);
	    setSyncEnable(enabled);
	    if(enabled){
		getMidTableManager().onModuleConnectivityChange(true);
	    }
	      //super.onRetrive(data);
	}	
}
