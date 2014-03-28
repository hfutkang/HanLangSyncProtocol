package cn.ingenic.glasssync.services.mid;

import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;
import cn.ingenic.glasssync.services.SyncData;

public class MidTools {
	private static final String TAG = "MidTools";

	static Object getValue(SyncData data, Column column) throws MidException {
		return getValueInternal(data, column.getName(), column.getType());
	}

	private static Object getValueInternal(SyncData data, String key, int type)
			throws MidException {
		if (data == null || key == null) {
			return null;
		}

		Object o;
		switch (type) {
		case Column.INTEGER:
			o = data.getInt(key);
			break;
		case Column.LONG:
			o = data.getLong(key);
			break;
		case Column.STRING:
			o = data.getString(key);
			break;
		default:
			throw new MidException("not suppported values type:" + type);
		}
		return o;
	}

	static Object getKeyValue(SyncData data, KeyColumn column, boolean isMid)
			throws MidException {
		return isMid ? getValueInternal(data, column.getName(),
				column.getType()) : getValueInternal(data,
				column.getMappedName(), column.getType());
	}

	static Object getValue(Cursor c, Column column) throws MidException {
		return getValueInternal(c, c.getColumnIndex(column.getName()),
				column.getType());
	}

	private static Object getValueInternal(Cursor c, int index, int type)
			throws MidException {
		if (index < 0) {
			throw new MidException("can not find c:" + c
					+ " from source cursor");
		}

		Object o;
		switch (type) {
		case Column.INTEGER:
			o = c.getInt(index);
			break;
		case Column.LONG:
			o = c.getLong(index);
			break;
		case Column.STRING:
			o = c.getString(index);
			break;
		default:
			throw new MidException("not suppported values type:" + type);
		}
		return o;
	}

	static Object getKeyValue(Cursor c, KeyColumn keyColumn, boolean isMid)
			throws MidException {
		int index = c.getColumnIndex(isMid ? keyColumn.getName() : keyColumn
				.getMappedName());
		return getValueInternal(c, index, keyColumn.getType());
	}

	static ContentValues getValues(Cursor source, List<Column> columns)
			throws MidException {
		if (source == null || columns == null) {
			throw new MidException("args can not be null in getValues()");
		}

		ContentValues values = new ContentValues();
		for (Column column : columns) {
			Object o = getValue(source, column);
			switch (column.getType()) {
			case Column.INTEGER:
				values.put(column.getName(), (Integer) o);
				break;
			case Column.LONG:
				values.put(column.getName(), (Long) o);
				break;
			case Column.STRING:
				values.put(column.getName(), (String) o);
				break;
			default:
				throw new MidException("unsupported value type:"
						+ column.getType());
			}
		}
		return values;
	}

	static ContentValues getValues(SyncData data, List<Column> columns)
			throws MidException {
		if (data == null || columns == null) {
			throw new MidException("args can not be null in getValues()");
		}

		ContentValues values = new ContentValues();
		for (Column column : columns) {
			Object o = getValue(data, column);
			switch (column.getType()) {
			case Column.INTEGER:
				values.put(column.getName(), (Integer) o);
				break;
			case Column.LONG:
				values.put(column.getName(), (Long) o);
				break;
			case Column.STRING:
				values.put(column.getName(), (String) o);
				break;
			default:
				throw new MidException("unsupported value type:"
						+ column.getType());
			}
		}
		return values;
	}

	static void fillSyncData(SyncData data, Cursor c, List<Column> columns)
			throws MidException {
		for (Column column : columns) {
			fillSyncData(data, c, column);
		}
	}

	static void fillSyncData(SyncData data, Cursor c, Column column)
			throws MidException {
		Object o = getValue(c, column);

		switch (column.getType()) {
		case Column.INTEGER:
			data.putInt(column.getName(), (Integer) o);
			break;
		case Column.LONG:
			data.putLong(column.getName(), (Long) o);
			break;
		case Column.STRING:
			data.putString(column.getName(), (String) o);
			break;
		default:
			throw new MidException("unsupported value type:" + column.getType());
		}
	}
}
