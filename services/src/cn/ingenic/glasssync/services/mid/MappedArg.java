package cn.ingenic.glasssync.services.mid;

class MappedArg<T> {
	final int sourcePos;
	final T key;
	final int sync;

	MappedArg(int sourcePos, T key, int sync) {
		this.sourcePos = sourcePos;
		this.key = key;
		this.sync = sync;
	}
}
