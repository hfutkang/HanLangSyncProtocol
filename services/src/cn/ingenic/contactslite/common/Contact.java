package cn.ingenic.contactslite.common;

public class Contact {

	public long mId;
	public String mName;
	private String[] mAddress;
	
	public void setAddress(String address){
		if(mAddress==null||mAddress.length==0){
			mAddress=new String[1];
			mAddress[0]=address;
			return ;
		}
		String[] dest=new String[mAddress.length+1];
		System.arraycopy(mAddress, 0, dest, 0, mAddress.length);
		dest[mAddress.length]=address;
		mAddress=dest;
	}
	
	public String[] getAddress(){
		return mAddress;
	}
}
