package cn.ingenic.contactslite.common;

import java.util.HashMap;
import java.util.Map;

import android.util.Log;


public class ContactSortKeyParse{
	
	
	private static Map<Character,Integer> map=null;
	
	private static ContactSortKeyParse mInstance;
	
	public static ContactSortKeyParse getContactSortKeyParse(){
		if(mInstance==null){
			mInstance=new ContactSortKeyParse();
			
		}
		return mInstance;
	}
	
	
	private Map<Character,Integer> instanceMap(){
		map=new HashMap<Character,Integer>();
		map.put('A', 1); map.put('a', 1);
		map.put('B', 2); map.put('b', 2);
		map.put('C', 3); map.put('c', 3);
		map.put('D', 4); map.put('d', 4);
		map.put('E', 5); map.put('e', 5);
		map.put('F', 6); map.put('f', 6);
		map.put('G', 7); map.put('g', 7);
		map.put('H', 8); map.put('h', 8);
		
		map.put('I', 9); map.put('i', 9);
		map.put('J', 10); map.put('j', 10);
		map.put('K', 11); map.put('k', 11);
		map.put('L', 12); map.put('l', 12);
		map.put('M', 13); map.put('m', 13);
		
		map.put('N', 14); map.put('n', 14);
		map.put('O', 15); map.put('o', 15);
		map.put('P', 16); map.put('p', 16);
		map.put('Q', 17); map.put('q', 17);
		map.put('R', 18); map.put('r', 18);
		map.put('S', 19); map.put('s', 19);
		map.put('T', 20); map.put('t', 20);
		map.put('U', 21); map.put('u', 21);
		map.put('V', 22); map.put('v', 22);
		map.put('W', 23); map.put('w', 23);
		
		map.put('X', 24); map.put('x', 24);
		map.put('Y', 25); map.put('y', 25);
		map.put('Z', 26);map.put('z', 26);
		return map;
	}
	
	public int parseSortKeyTop(String sortKey){
		sortKey.trim();
		if(map==null)map=instanceMap();
		if(map.get(sortKey.charAt(0))!=null){
			return map.get(sortKey.charAt(0));
		}
		Log.e("ContactSortKeyParse","sort Key first char not in A-Z !!!");
		return 0;
	}


}
