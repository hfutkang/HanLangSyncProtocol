package cn.ingenic.glasssync.services.mid;

import java.util.ArrayList;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.net.Uri;
import android.util.Log;
import cn.ingenic.glasssync.services.SyncData;

abstract class DefaultDataDestinationImpl implements DataDestination {
	private final MidTableManager mMidMgr;
	private final DefaultDataDestTransactionManager mTranMgr;

	DefaultDataDestinationImpl(MidTableManager mgr) {
		if (mgr == null) {
			throw new IllegalArgumentException(
					"arg:MidTablemanager can not bu null");
		}
		mMidMgr = mgr;
		mTranMgr = new DefaultDataDestTransactionManager(mgr, this);
	}

	@Override
	public KeyColumn getDestKeyColumn() {
		return null;
	}

	@Override
	public void onDatasClear() {
		mMidMgr.mContext.getContentResolver().delete(mMidMgr.getDestUri(),
				null, null);
	}

	@Override
	public DestTransactionManager getTransactionManager() {
		return mTranMgr;
	}

	protected final ContentProviderOperation createDeleteOperation(
			SyncData delete) throws MidException {
		Object o = MidTools.getKeyValue(delete, mMidMgr.getDestKey(), false);
		if (o == null) {
			loge("Key valus lost in the Delete SyncData");
			return null;
		}
		return ContentProviderOperation.newDelete(mMidMgr.getDestUri())
				.withSelection(Mid.COLUMN_KEY + "=" + "'"+o+"'", null).build();
	}

	protected final ContentProviderOperation createUpdateOperation(
			SyncData update) throws MidException {
		Object o = MidTools.getKeyValue(update, mMidMgr.getDestKey(), false);
		if (o == null) {
			loge("Key valus lost in the Update SyncData");
			return null;
		}
		ContentValues values = MidTools.getValues(update, getDestColumnList());
		return ContentProviderOperation.newUpdate(mMidMgr.getDestUri())
				.withValues(values)
				.withSelection(Mid.COLUMN_KEY + "=" + "'"+o+"'", null).build();
	}

	protected final ContentProviderOperation createInsertOperation(
			SyncData insert) throws MidException {
		Object o = MidTools.getKeyValue(insert, mMidMgr.getDestKey(), false);
		if (o == null) {
			loge("Key valus lost in the Insert SyncData");
			return null;
		}
		ContentValues values = MidTools.getValues(insert, getDestColumnList());
		return ContentProviderOperation.newInsert(mMidMgr.getDestUri())
				.withValues(values).withValue(Mid.COLUMN_KEY, o).build();
	}

	@Override
	public ArrayList<ContentProviderOperation> applySyncDatas(
			SyncData[] deletes, SyncData[] updates, SyncData[] inserts,
			SyncData[] appends) throws MidException {
		if ((deletes == null || deletes.length == 0)
				&& (updates == null || updates.length == 0)
				&& (inserts == null || inserts.length == 0)
				&& (appends == null || appends.length == 0)) {
			logw("no applyDatas found");
			return null;
		}

		ArrayList<ContentProviderOperation> results = new ArrayList<ContentProviderOperation>();
		if (deletes != null) {
			for (SyncData d : deletes) {
				ContentProviderOperation opt = createDeleteOperation(d);
				if (opt != null) {
					results.add(opt);
				}
			}
		}

		if (updates != null) {
			for (SyncData d : updates) {
				ContentProviderOperation opt = createUpdateOperation(d);
				if (opt != null) {
					results.add(opt);
				}
			}
		}

		if (inserts != null) {
			for (SyncData d : inserts) {
				ContentProviderOperation opt = createInsertOperation(d);
				if (opt != null) {
					results.add(opt);
				}
			}
		}
		return results;
	}

	@Override
	public Uri getDestMidUri() {
		return Uri.parse("content://" + getDestAuthorityName());
	}
	
	private static final String PRE = "<DIMPL>";
//	private static void logv(String msg) {
//		Log.v(Mid.DEST, PRE + msg);
//	}
//	
//	private static void logd(String msg) {
//		Log.d(Mid.DEST, PRE + msg);
//	}
//	
//	private static void logi(String msg) {
//		Log.i(Mid.DEST, PRE + msg);
//	}
	
	private static void logw(String msg) {
		Log.w(Mid.DEST, PRE + msg);
	}
	
	private static void loge(String msg) {
		Log.e(Mid.DEST, PRE + msg);
	}

}
