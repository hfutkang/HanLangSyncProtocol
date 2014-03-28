package cn.ingenic.glasssync.services.mid;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentProviderOperation;
import android.net.Uri;
import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncException;

public interface DataDestination {
	KeyColumn getDestKeyColumn();

	List<Column> getDestColumnList();

	ArrayList<ContentProviderOperation> applySyncDatas(SyncData[] deletes,
			SyncData[] updates, SyncData[] inserts, SyncData[] appends)
			throws MidException;
	
	String getDestAuthorityName();
	
	Uri getDestMidUri();
	
	void onDatasClear();

	DestTransactionManager getTransactionManager();

	interface DestTransactionManager {
		void processRequest(SyncData data);

		void sendReflect() throws SyncException;

		void processRespSent(boolean success, Object obj);
		
		void processCleaar(String address);
	}
}
