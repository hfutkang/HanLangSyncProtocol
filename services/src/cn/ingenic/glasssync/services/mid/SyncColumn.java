package cn.ingenic.glasssync.services.mid;

public class SyncColumn implements Column {

	@Override
	public String getName() {
		return Mid.COLUMN_SYNC;
	}

	@Override
	public String getDbType() {
		return Column.DB_TYPE_INTEGER;
	}

	@Override
	public int getType() {
		return Column.INTEGER;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("SyncColumn:{");
		sb.append("Name:").append(getName()).append(" ").append("DBType:")
				.append(getDbType()).append(" ").append("Type:")
				.append(DefaultColumn.convertStrType(getType())).append("}");
		return sb.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof SyncColumn) {
			return true;
		}

		return super.equals(o);
	}

}
