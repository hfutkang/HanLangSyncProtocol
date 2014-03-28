package cn.ingenic.glasssync.services.mid;

public final class KeyColumn implements Column {

	private final String mMappedName;
	private final String mMappedType;
	private final int mType;

	private void checkArgs(String name, String dbType, int type) {
		if (name == null || dbType == null) {
			throw new IllegalArgumentException("Args can not be null");
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

	public KeyColumn(String mappedName, String mappedType, int type) {
		checkArgs(mappedName, mappedType, type);

		mMappedName = mappedName;
		mMappedType = mappedType;
		mType = type;
	}
	
	public KeyColumn(String mappedName, int type) {
		String mappedType = "";
		switch  (type) {
		case Column.INTEGER:
		case Column.LONG:
			mappedType = Column.DB_TYPE_INTEGER;
			break;
		case Column.STRING:
			mappedType = Column.DB_TYPE_TEXT;
			break;
		}
		checkArgs(mappedName, mappedType, type);
		mMappedName = mappedName;
		mMappedType = mappedType;
		mType = type;
	}

	@Override
	public final String getName() {
		return Mid.COLUMN_KEY;
	}

	@Override
	public final String getDbType() {
		return mMappedType;
	}

	final String getMappedName() {
		return mMappedName;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("DefaultColumn:{");
		sb.append("Name:").append(getName()).append(" DBType:")
				.append(getDbType()).append(" MappedName:")
				.append(getMappedName()).append(" Type:")
				.append(DefaultColumn.convertStrType(mType)).append("}");
		return sb.toString();
	}

	@Override
	public int getType() {
		return mType;
	}

}
