package cn.ingenic.glasssync.services.mid;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentProviderOperation;
import android.content.Context;
import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncModule;

public abstract class SimpleMidDestManager extends MidTableManager {

	private final MyDataDestinationImpl mDest;

	public SimpleMidDestManager(Context ctx, SyncModule module) {
		super(ctx, module);
		mDest = new MyDataDestinationImpl(this);
		init(null, mDest);
	}

	private class MyDataDestinationImpl extends DefaultDataDestinationImpl {

		MyDataDestinationImpl(MidTableManager mgr) {
			super(mgr);
		}

		@Override
		public List<Column> getDestColumnList() {
			return SimpleMidDestManager.this.getDestColumnList();
		}

		@Override
		public ArrayList<ContentProviderOperation> applySyncDatas(
				SyncData[] deletes, SyncData[] updates, SyncData[] inserts,
				SyncData[] appends) throws MidException {
			return SimpleMidDestManager.this.applySyncDatas(deletes, updates,
					inserts, appends);
		}

		ArrayList<ContentProviderOperation> superApplySyncDatas(
				SyncData[] deletes, SyncData[] updates, SyncData[] inserts,
				SyncData[] appends) throws MidException {
			return super.applySyncDatas(deletes, updates, inserts, appends);
		}

		@Override
		public String getDestAuthorityName() {
			return SimpleMidDestManager.this.getMidAuthorityName();
		}

		@Override
		public KeyColumn getDestKeyColumn() {
			return SimpleMidDestManager.this.getDestKeyColumn();
		}

		@Override
		public void onDatasClear() {
			super.onDatasClear();
			SimpleMidDestManager.this.onDatasClear();
		}

	}

	protected abstract List<Column> getDestColumnList();
	
	protected KeyColumn getDestKeyColumn() {
		return null;
	}

	protected ArrayList<ContentProviderOperation> applySyncDatas(
			SyncData[] deletes, SyncData[] updates, SyncData[] inserts,
			SyncData[] appends) throws MidException {
		return mDest.superApplySyncDatas(deletes, updates, inserts, appends);
	}

	protected final ContentProviderOperation createDeleteOperation(
			SyncData delete) throws MidException {
		return mDest.createDeleteOperation(delete);
	}

	protected final ContentProviderOperation createUpdateOperation(
			SyncData update) throws MidException {
		return mDest.createUpdateOperation(update);
	}

	protected final ContentProviderOperation createInsertOperation(
			SyncData insert) throws MidException {
		return mDest.createInsertOperation(insert);
	}

	protected abstract String getMidAuthorityName();
	
	protected void onDatasClear() {
	}
}
