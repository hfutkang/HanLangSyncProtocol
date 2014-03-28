package cn.ingenic.contactslite.common;

import java.util.ArrayList;
import java.util.List;


import cn.ingenic.contactslite.common.ContactPacket.DataEntity;
import cn.ingenic.glasssync.services.SyncData;

/**
 * Utility class for handling transformer of ContactPacket 
 *
 */
public class ContactPacketUtils {
	public static void fillWith(SyncData syncData, ContactPacket contact) {
		String lookupKey = contact.getLookupKey();
		String displayName = contact.mDisplayName;
		String sortKey = contact.mSortKey;
		String version = contact.getVersion();
		syncData.putString(ContactsLiteContract.Contacts.LOOKUP_KEY, lookupKey);
		syncData.putString(ContactsLiteContract.Contacts.DISPLAY_NAME, displayName);
		syncData.putString(ContactsLiteContract.Contacts.SORT_KEY, sortKey);
		
		List<SyncData> datas = new ArrayList<SyncData>();
		for (DataEntity data : contact.getDatas()) {
			SyncData temp = new SyncData();
			temp.putInt(ContactsLiteContract.Data.TYPE, data.getType());
			temp.putString(ContactsLiteContract.Data.LABEL, data.getLabel());
			temp.putString(ContactsLiteContract.Data.MIMETYPE, data.getMimeType());
			temp.putString(ContactsLiteContract.Data.DATA1, data.getData());
			datas.add(temp);
		}
		if (datas != null && datas.size() > 0) {
			syncData.putDataArray("datas", datas.toArray(new SyncData[datas.size()]));
		}
	}
	
	public static ContactPacket createContactPacket(SyncData syncData) {
		String lookupKey = syncData.getString(ContactsLiteContract.Contacts.LOOKUP_KEY);
		ContactPacket packet = new ContactPacket(lookupKey);
		String displayName = syncData.getString(ContactsLiteContract.Contacts.DISPLAY_NAME);
		packet.mDisplayName = displayName;
		String sortKey = syncData.getString(ContactsLiteContract.Contacts.SORT_KEY);
		packet.mSortKey = sortKey;
		
		for (SyncData data : syncData.getDataArray("datas")) {
			int type = data.getInt(ContactsLiteContract.Data.TYPE);
			String label = data.getString(ContactsLiteContract.Data.LABEL);
			String mimetype = data.getString(ContactsLiteContract.Data.MIMETYPE);
			String cData = data.getString(ContactsLiteContract.Data.DATA1);
			packet.addDataEntity(new DataEntity(mimetype, cData, type, label));
		}
		
		return packet;
	}
}
