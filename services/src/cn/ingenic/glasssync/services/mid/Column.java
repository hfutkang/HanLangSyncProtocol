package cn.ingenic.glasssync.services.mid;

public interface Column {
	String DB_TYPE_INTEGER = "INTEGER";
	String DB_TYPE_TEXT = "TEXT";
	
	int INTEGER = 1;
	int LONG = 2;
	int STRING = 3;

	String getName();

	String getDbType();

	int getType();
}