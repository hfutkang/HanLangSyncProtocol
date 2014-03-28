package cn.ingenic.glasssync.services.mid;

import java.util.List;
import java.util.Set;

import android.database.Cursor;
import android.net.Uri;
import cn.ingenic.glasssync.services.SyncData;

public interface DataSource {
	Uri[] getSrcObservedUris();

	KeyColumn getSrcKeyColumn();
	
	String getSrcAuthorityName();
	
	Uri getSrcMidUri();

	List<Column> getSrcColumnList();

	Cursor getSrcDataCursor(Set keys);

	boolean isSrcMatch(Cursor source, Cursor mid) throws MidException;

	void fillSrcSyncData(final SyncData data, Cursor source)
			throws MidException;

	SyncData[] appendSrcSyncData(Set<Integer> positons, Cursor source)
			throws MidException;

	SrcTransactionManager getTransactionManager();

	interface SrcTransactionManager {
		void applyMidDiff();
		
		void sendDiffSyncRequest();
		
		void processRequestSent(boolean success, Object obj);
		
		void processResponse(SyncData data);
		
		void processReflect(SyncData data);
		
		void processClear(String address);
	}
}
