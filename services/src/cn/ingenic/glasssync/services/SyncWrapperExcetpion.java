package cn.ingenic.glasssync.services;

public class SyncWrapperExcetpion extends SyncException {

	private static final long serialVersionUID = 9111433217452357522L;
	private Exception mInner;
	
	public SyncWrapperExcetpion(Exception inner) {
		super(inner.getMessage());
		mInner = inner;
	}
	
	public Exception getInnerException() {
		return mInner;
	}

}
