package cn.ingenic.glasssync.contact;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import cn.ingenic.contactslite.common.ContactPacket;
import cn.ingenic.contactslite.common.ContactPacketUtils;
import cn.ingenic.contactslite.common.ContactSortKeyParse;
import cn.ingenic.contactslite.common.ContactsLiteContract;
import cn.ingenic.contactslite.common.ContactsLiteContract.Contacts;
import cn.ingenic.contactslite.common.ContactsLiteContract.Data;
import cn.ingenic.glasssync.services.SyncData;
import cn.ingenic.glasssync.services.SyncModule;
import cn.ingenic.glasssync.services.mid.Column;
import cn.ingenic.glasssync.services.mid.DefaultColumn;
import cn.ingenic.glasssync.services.mid.KeyColumn;
import cn.ingenic.glasssync.services.mid.Mid;
import cn.ingenic.glasssync.services.mid.MidException;
import cn.ingenic.glasssync.services.mid.MidTableManager;
import cn.ingenic.glasssync.services.mid.SimpleMidDestManager;

public class ContactsLiteMidDestManager extends SimpleMidDestManager {
	private static MidTableManager sInstance;
	private Context mContext;

	private ContactsLiteMidDestManager(Context context, SyncModule module) {
		super(context, module);
		mContext = context;
	}

	public synchronized static MidTableManager getInstance(Context context,
			SyncModule module) {
		if (sInstance == null) {
			sInstance = new ContactsLiteMidDestManager(context, module);
		}
		return sInstance;
	}

	@Override
	protected List<Column> getDestColumnList() {
		List<Column> columns = new ArrayList<Column>();
		columns.add(new DefaultColumn(Contacts.DISPLAY_NAME, Column.STRING));
		columns.add(new DefaultColumn(Contacts.SORT_KEY, Column.STRING));
		columns.add(new DefaultColumn(Contacts.WATCH_SORT_KEY, Column.INTEGER));
		return columns;
	}

	@Override
	protected String getMidAuthorityName() {
		return ContactsLiteContract.AUTHORITY;
	}

	/**
	 * delete the data that doesn't belong any contact. super class
	 * implementation cause data table won't be clean while delete on contacts.
	 */
	// private void doGarbageCleanWork(Context context) {
	// ContentResolver resolver = context.getContentResolver();
	// Uri contactUri = getMidTableUri();
	// List<String> existIdList = new ArrayList<String>();
	// Cursor cursor = resolver.query(contactUri,
	// new String[]{Contacts._ID}, null, null, null);
	// while (cursor != null && cursor.moveToNext()) {
	// String id = cursor.getString(cursor.getColumnIndex(Contacts._ID));
	// existIdList.add(id);
	// }
	//
	// List<String> allIdList = new ArrayList<String>();
	// Cursor dataCursor = resolver.query(Data.CONTENT_URI,
	// new String[]{Data.CONTACT_ID}, null, null, null);
	// while (cursor != null && cursor.moveToNext()) {
	// String id = dataCursor.getString(cursor.getColumnIndex(Data.CONTACT_ID));
	// allIdList.add(id);
	// }
	//
	// List<String> unlinkIdList = new ArrayList<String>();
	// for (String id : allIdList) {
	// if ( !allIdList.contains(id) ) {
	// unlinkIdList.add(id);
	// }
	// }
	//
	// String selectionToDelete = buildSelectionByIdList(Data.CONTACT_ID,
	// unlinkIdList);
	// if ( !TextUtils.isEmpty(selectionToDelete) ) {
	// resolver.delete(Data.CONTENT_URI, selectionToDelete, null);
	// }
	// }

	// get mid table uri
	private Uri getMidTableUri() {
		return Uri.parse("content://" + getMidAuthorityName());
	}

	@Override
	public KeyColumn getDestKeyColumn() {
		return new KeyColumn(Mid.COLUMN_KEY, Column.STRING);
	}

	/**
	 * delete contacts that have been deleted on Phone
	 * 
	 * @param deletes
	 *            TODO
	 * @param updates
	 *            TODO
	 */
	// private List<ContentProviderOperation> deleteObsoleteContacts(SyncData[]
	// deletes, SyncData[] updates) {
	// ArrayList<ContentProviderOperation> ops = new
	// ArrayList<ContentProviderOperation>();
	//
	// List<String> lookupKeyListToDelete = new ArrayList<String>();
	// if (isNotEmpty(deletes)) {
	// for (SyncData one : deletes) {
	// String lookupKey = one.getString(Mid.COLUMN_KEY);
	// lookupKeyListToDelete.add(lookupKey);
	// }
	// }
	// if (isNotEmpty(updates)) {
	// for (SyncData one : updates) {
	// String lookupKey = one.getString(Mid.COLUMN_KEY);
	// lookupKeyListToDelete.add(lookupKey);
	// }
	// }
	// // nothing to delete
	// if (lookupKeyListToDelete.size() == 0) return ops;
	//
	// Uri midUri = getMidTableUri();
	// String selectionToDelete = buildSelectionByLookupKeyList(Mid.COLUMN_KEY,
	// lookupKeyListToDelete);
	// ContentResolver resolver = mContext.getContentResolver();
	// Cursor idCursor = resolver.query(midUri,
	// new String[]{Contacts._ID},
	// selectionToDelete, null, null);
	// /*
	// * get contact id for deleting datas in table data
	// * TODO: normally should use trigger
	// */
	// List<String> contactIdList = null;
	//
	// if(idCursor.getCount()!=0){
	// contactIdList=new ArrayList<String>();
	// idCursor.moveToFirst();
	// do{
	// contactIdList.add(idCursor.getString(0));
	// }while(idCursor.moveToNext());
	// }
	// idCursor.close();
	//
	// //
	// // while (idCursor != null && idCursor.moveToNext()) {
	// // contactIdList.add(idCursor.getString(0));
	// //
	// Log.e("ContactsListMidDestManager","delete id is :"+idCursor.getString(0));
	// // }
	// ContentProviderOperation.Builder builder =
	// ContentProviderOperation.newDelete(midUri);
	// builder.withSelection(selectionToDelete, null);
	// ops.add(builder.build());
	// if(contactIdList==null)return ops;
	// builder = ContentProviderOperation.newDelete(Data.CONTENT_URI);
	// builder.withSelection(buildSelectionByIdList(Data.CONTACT_ID,
	// contactIdList), null);
	// ops.add(builder.build());
	//
	// return ops;
	// }

	private Map<String, Long> getIdAndMidkeyMap() {
		ContentResolver resolver = mContext.getContentResolver();
		Uri midUri = getMidTableUri();
		Cursor cursor = resolver.query(midUri, new String[] { Contacts._ID,
				Mid.COLUMN_KEY }, null, null, null);
		if (cursor.getCount() == 0) {
			cursor.close();
			return null;
		}
		Map<String, Long> idAndMidKeyMap = new HashMap<String, Long>();
		cursor.moveToFirst();
		do {
			long id = cursor.getLong(cursor.getColumnIndex(Contacts._ID));
			String mid_key = cursor.getString(cursor
					.getColumnIndex(Mid.COLUMN_KEY));
			idAndMidKeyMap.put(mid_key, id);
		} while (cursor.moveToNext());
		cursor.close();
		return idAndMidKeyMap;
	}

	private void doDelete(ArrayList<ContentProviderOperation> list,
			SyncData[] deletes, Map<String, Long> map) throws MidException {
		if (deletes != null) {
			for (int l = 0; l < deletes.length; l++) {
				ContentProviderOperation operation = super
						.createDeleteOperation(deletes[l]);
				if (operation != null)
					list.add(operation);
			}
		}
		// delete table data delete datas
		if (map != null)
			doAppendDelete(list, deletes, map);

	}

	private void doInsert(ArrayList<ContentProviderOperation> list,
			SyncData[] inserts) throws MidException {
		if (inserts != null) {

			for (int l = 0; l < inserts.length; l++) {
				ContentProviderOperation operation = createContactInsertOperation(inserts[l]);
				if (operation != null)
					list.add(operation);
				doOneInsertAppend(list, inserts[l], list.size() - 1);
			}
		}
	}

	private ContentProviderOperation createContactInsertOperation(
			SyncData insert) {

		return ContentProviderOperation.newInsert(getMidTableUri())
				.withValues(getValues(insert))
				.withValue(Mid.COLUMN_KEY, insert.getString(Mid.COLUMN_KEY))
				.build();
	}

	private ContentProviderOperation createContactUpdateOperation(
			SyncData update) {
		return ContentProviderOperation
				.newUpdate(getMidTableUri())
				.withValues(getValues(update))
				.withSelection(
						Mid.COLUMN_KEY + "='"
								+ update.getString(Mid.COLUMN_KEY) + "'", null)
				.build();
	}

	private ContentValues getValues(SyncData d) {
		ContentValues cv = new ContentValues();
		String name = d.getString(Contacts.DISPLAY_NAME);
		cv.put(Contacts.DISPLAY_NAME, name);
		// String sortKey = d.getString(Contacts.SORT_KEY);
		
		ArrayList<HanziToPinyin.Token> list = HanziToPinyin.getInstance().get(name);

		String sortKey = getSortKey(list);
		
		cv.put(Contacts.SORT_KEY, sortKey);
		int sortValue = ContactSortKeyParse.getContactSortKeyParse()
				.parseSortKeyTop(sortKey);
		cv.put(Contacts.WATCH_SORT_KEY, sortValue);

		return cv;
	}

	private String getSortKey(ArrayList<HanziToPinyin.Token> list) {
		String sortKey = "";
		for (HanziToPinyin.Token token : list) {
			sortKey = sortKey + token.target + token.source;
		}
		return sortKey;
	}

	// add by yangliu for append table data delete
	private void doAppendDelete(ArrayList<ContentProviderOperation> list,
			SyncData[] delete, Map<String, Long> map) {
		if (isNotEmpty(delete)) {

			for (int l = 0; l < delete.length; l++) {
				String lookup_key = delete[l].getString(Mid.COLUMN_KEY);

				Long idL = map.get(lookup_key);
				if (idL == null) {
					continue;
				}
				ContentProviderOperation operation = ContentProviderOperation
						.newDelete(Data.CONTENT_URI)
						.withSelection(Data.CONTACT_ID + "=" + idL, null)
						.build();
				list.add(operation);
			}

		}

	}

	// add by yangliu added for append data insert
	private void doOneInsertAppend(ArrayList<ContentProviderOperation> list,
			SyncData insert, int previousResult) {

		ContactPacket contact = ContactPacketUtils.createContactPacket(insert);

		List<ContentValues> dataValues = contact.getDataContentValues();
		for (ContentValues oneValue : dataValues) {
			ContentProviderOperation operation = ContentProviderOperation
					.newInsert(Data.CONTENT_URI).withValues(oneValue)
					.withValueBackReference(Data.CONTACT_ID, previousResult)
					.build();
			list.add(operation);
		}
	}

	/**
	 * when update one contact update mid_table by all values delete * from data
	 * where contact_id = contactId; and insert all contact datas in data table.
	 * 
	 * @param update
	 * @param list
	 * @param contactId
	 */
	private void doOneUpdateAppend(SyncData update,
			ArrayList<ContentProviderOperation> list, long contactId) {

		ContentProviderOperation deleteOperation = ContentProviderOperation
				.newDelete(Data.CONTENT_URI)
				.withSelection(Data.CONTACT_ID + "=" + contactId, null).build();
		list.add(deleteOperation);
		ContactPacket contact = ContactPacketUtils.createContactPacket(update);
		List<ContentValues> dataValues = contact.getDataContentValues();

		for (ContentValues oneValue : dataValues) {
			ContentProviderOperation insertOperation = ContentProviderOperation
					.newInsert(Data.CONTENT_URI).withValues(oneValue)
					.withValue(Data.CONTACT_ID, contactId).build();
			list.add(insertOperation);
		}
	}

	private boolean isNotEmpty(Collection<?> list) {
		return list != null && list.size() > 0;
	}

	private <E> boolean isNotEmpty(E[] array) {
		return array != null && array.length > 0;
	}

	@Override
	protected ArrayList<ContentProviderOperation> applySyncDatas(
			SyncData[] deletes, SyncData[] updates, SyncData[] inserts,
			SyncData[] appends) throws MidException {

		ArrayList<ContentProviderOperation> allOperations = new ArrayList<ContentProviderOperation>();

		Map<String, Long> idAndMidKeyMap = getIdAndMidkeyMap();/*
																 * get all exist
																 * id and midkey
																 * map
																 */

		doDelete(allOperations, deletes, idAndMidKeyMap);// delete mid table and
															// data table

		doInsert(allOperations, inserts);// insert

		// update
		if (updates != null && idAndMidKeyMap != null) {
			for (SyncData u : updates) {
				// update mid table
				ContentProviderOperation operation = createContactUpdateOperation(u);
				allOperations.add(operation);
				String midKey = u.getString(Mid.COLUMN_KEY);
				long contactId = idAndMidKeyMap.get(midKey);
				doOneUpdateAppend(u, allOperations, contactId);
			}
		}

		return allOperations;
	}

	// TODO move to common library
	private static String buildSelectionByIdList(String columnName,
			List<String> idList) {
		if (idList == null || idList.size() == 0) {
			return null;
		}

		StringBuilder inBuilder = new StringBuilder();
		int index = 0;
		for (String id : idList) {
			if (index == 0) {
				inBuilder.append("(");
			} else {
				inBuilder.append(",");
			}
			inBuilder.append(id);
			index++;
		}
		inBuilder.append(')');
		return columnName + " IN " + inBuilder.toString();
	}

	// TODO move to common library
	private static String buildSelectionByLookupKeyList(String columnName,
			Collection<String> lookupKeyList) {
		if (lookupKeyList == null || lookupKeyList.size() == 0) {
			return null;
		}
		StringBuilder inBuilder = new StringBuilder();
		int index = 0;
		for (String lookupKey : lookupKeyList) {
			if (index == 0) {
				inBuilder.append("(");
			} else {
				inBuilder.append(",");
			}
			inBuilder.append("'" + lookupKey + "'");
			index++;
		}
		inBuilder.append(')');
		return columnName + " IN " + inBuilder.toString();
	}

	@Override
	protected void onDatasClear() {
		// clear all datas
		ContentResolver resolver = mContext.getContentResolver();
		resolver.delete(ContactsLiteContract.Data.CONTENT_URI, null, null);
	}
}
