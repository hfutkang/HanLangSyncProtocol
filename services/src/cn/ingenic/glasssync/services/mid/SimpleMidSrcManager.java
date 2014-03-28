package cn.ingenic.glasssync.services.mid;

import java.util.List;
import java.util.Set;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncModule;

public abstract class SimpleMidSrcManager extends MidTableManager {

	private final MyDataSourceImpl mMySource;

	public SimpleMidSrcManager(Context ctx, SyncModule module) {
		super(ctx, module);
		mMySource = new MyDataSourceImpl(this);
		init(mMySource, null);
	}

	private class MyDataSourceImpl extends DefaultDataSourceImpl {

		MyDataSourceImpl(MidTableManager mgr) {
			super(mgr);
		}

		@Override
		public Uri[] getSrcObservedUris() {
			return SimpleMidSrcManager.this.getSrcObservedUris();
		}

		@Override
		public List<Column> getSrcColumnList() {
			return SimpleMidSrcManager.this.getSrcColumnList();
		}

		@Override
		public Cursor getSrcDataCursor(Set keys) {
			return SimpleMidSrcManager.this.getSrcDataCursor(keys);
		}

		@Override
		public KeyColumn getSrcKeyColumn() {
			return SimpleMidSrcManager.this.getSrcKeyColumn();
		}

		@Override
		public boolean isSrcMatch(Cursor source, Cursor mid)
				throws MidException {
			return SimpleMidSrcManager.this.isSrcMatch(source, mid);
		}

		boolean superIsSrcMatch(Cursor source, Cursor mid) throws MidException {
			return super.isSrcMatch(source, mid);
		}

		@Override
		public void fillSrcSyncData(SyncData data, Cursor source)
				throws MidException {
			SimpleMidSrcManager.this.fillSrcSyncData(data, source);
		}

		void superFillSrcSyncData(SyncData data, Cursor source)
				throws MidException {
			super.fillSrcSyncData(data, source);
		}

		@Override
		public SyncData[] appendSrcSyncData(Set<Integer> positons, Cursor source)
				throws MidException {
			return SimpleMidSrcManager.this.appendSrcSyncData(positons, source);
		}

		SyncData[] superAppendSrcSyncData(Set<Integer> positons, Cursor source)
				throws MidException {
			return super.appendSrcSyncData(positons, source);
		}

		@Override
		public String getSrcAuthorityName() {
			return SimpleMidSrcManager.this.getMidAuthorityName();
			
		}

		@Override
		public Uri getSrcMidUri() {
//			if(getMidSourceUri()!=null){
//				return getMidSourceUri();
//			}
			return super.getSrcMidUri();
		}
		
		

	}

	protected KeyColumn getSrcKeyColumn() {
		return null;
	}
//	protected Uri getMidSourceUri(){
//		return null;
//	}

	protected abstract Uri[] getSrcObservedUris();

	protected abstract List<Column> getSrcColumnList();

	protected abstract Cursor getSrcDataCursor(Set keys);

	protected boolean isSrcMatch(Cursor source, Cursor mid) throws MidException {
		return mMySource.superIsSrcMatch(source, mid);
	}

	protected void fillSrcSyncData(SyncData data, Cursor source)
			throws MidException {
		mMySource.superFillSrcSyncData(data, source);
	}

	protected SyncData[] appendSrcSyncData(Set<Integer> positons, Cursor source)
			throws MidException {
		return mMySource.superAppendSrcSyncData(positons, source);
	}
	protected abstract String getMidAuthorityName();
	
}
