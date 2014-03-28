package cn.ingenic.glasssync.services.mid;

import android.util.Log;

public class DefaultColumn implements Column {
	private static final String TAG = "DefaultColumn";

	private final String mName;
	private final String mDbType;
	private final int mType;

	static String convertStrType(int type) {
		switch (type) {
		case Column.INTEGER:
			return "Integer";
		case Column.LONG:
			return "Long";
		case Column.STRING:
			return "String";
		default:
			Log.w(TAG, "Invalid type:" + type);
			return "UNKNOW";
		}
	}

	private void checkArgs(String name, String dbType, int type) {
		if (name == null || dbType == null) {
			throw new IllegalArgumentException("Args can not be null:name"
					+ name + " dbType:" + dbType);
		}

		if (MidTableManager.sReservedNames.contains(name)) {
			StringBuilder sb = new StringBuilder();
			for (String n : MidTableManager.sReservedNames) {
				sb.append(n);
				sb.append(" ");
			}
			throw new IllegalArgumentException("Invalid name:" + name
					+ ", reserved names:(" + sb.toString() + ")");
		}

		if (!MidTableManager.sTypesMap.containsKey(dbType)) {
			StringBuilder sb = new StringBuilder();
			for (String t : MidTableManager.sTypesMap.keySet()) {
				sb.append(t);
				sb.append(" ");
			}
			throw new IllegalArgumentException("Invalid dbType:" + dbType
					+ ", MidManager only supports TYPES:(" + sb.toString()
					+ ").");
		} else {
			if (!MidTableManager.sTypesMap.get(dbType).contains(type)) {
				throw new IllegalArgumentException("DbType:" + dbType
						+ " and type:" + type + " not matched");
			}
		}
	}

	public DefaultColumn(String name, String dbType, int type) {
		checkArgs(name, dbType, type);
		mName = name;
		mDbType = dbType;
		mType = type;
	}

	public DefaultColumn(String name, int type) {
		String dbType = "";
		switch (type) {
		case Column.INTEGER:
		case Column.LONG:
			dbType = DB_TYPE_INTEGER;
			break;
		case STRING:
			dbType = DB_TYPE_TEXT;
			break;
		}

		checkArgs(name, dbType, type);
		mName = name;
		mDbType = dbType;
		mType = type;
	}

	@Override
	public final String getName() {
		return mName;
	}

	@Override
	public final String getDbType() {
		return mDbType;
	}

	@Override
	public final int getType() {
		return mType;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("DefaultColumn:{");
		sb.append("Name:").append(getName()).append(" ").append("DBType:")
				.append(getDbType()).append(" ").append("Type:")
				.append(convertStrType(mType)).append("}");
		return sb.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof DefaultColumn) {
			DefaultColumn d = (DefaultColumn) o;
			return mName.equals(d.getName()) && mDbType.equals(d.getDbType())
					&& mType == d.getType();
		}

		return super.equals(o);
	}
}