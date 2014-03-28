package cn.ingenic.glasssync.services.mid;

import java.util.Set;

import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import cn.ingenic.glasssync.services.SyncData;

abstract class DefaultDataSourceImpl implements DataSource {
	private final MidTableManager mMidMgr;
	private final SrcTransactionManager mTranMgr;

	DefaultDataSourceImpl(MidTableManager mgr) {
		if (mgr == null) {
			throw new IllegalArgumentException(
					"arg:MidTablemanager can not bu null");
		}

		KeyColumn keyColumn = getSrcKeyColumn();
		if (keyColumn == null || Column.LONG == keyColumn.getType()) {
			mTranMgr = new DefaultDataSourceTransactionManager<Integer>(mgr,
					this);
		} else if (Column.STRING == keyColumn.getType()) {
			mTranMgr = new DefaultDataSourceTransactionManager<String>(mgr,
					this);
		} else {
			throw new RuntimeException("not supported DB type");
		}
		mMidMgr = mgr;
	}

	@Override
	public String getSrcAuthorityName() {
		return null;
	}

	@Override
	public Uri getSrcMidUri() {
		return Uri.parse("content://" + getSrcAuthorityName());
	}

	@Override
	public KeyColumn getSrcKeyColumn() {
		return null;
	}

	@Override
	public boolean isSrcMatch(Cursor source, Cursor mid) throws MidException {
		for (Column column : mMidMgr.getSrcTableList()) {
			Object so = MidTools.getValue(source, column);
			Object mo = MidTools.getValue(mid, column);
			if (so == null || mo == null) {
				loge("can not find Column:" + column + " in cursor.");
				continue;
			}
			if (!so.equals(mo)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void fillSrcSyncData(SyncData data, Cursor source)
			throws MidException {
		MidTools.fillSyncData(data, source, mMidMgr.getSrcTableList());
	}

	@Override
	public SyncData[] appendSrcSyncData(Set<Integer> positons, Cursor source)
			throws MidException {
		return null;
	}

	@Override
	public SrcTransactionManager getTransactionManager() {
		return mTranMgr;
	}

	private static final String PRE = "<SIMPL>";

	// private static void logv(String msg) {
	// Log.v(Mid.SRC, PRE + msg);
	// }
	//
	// private static void logd(String msg) {
	// Log.d(Mid.SRC, PRE + msg);
	// }
	//
	// private static void logi(String msg) {
	// Log.i(Mid.SRC, PRE + msg);
	// }
	//
	// private static void logw(String msg) {
	// Log.w(Mid.SRC, PRE + msg);
	// }

	private static void loge(String msg) {
		Log.e(Mid.SRC, PRE + msg);
	}
}
