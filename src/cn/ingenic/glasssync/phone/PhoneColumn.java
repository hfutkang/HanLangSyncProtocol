package cn.ingenic.glasssync.phone;

import cn.ingenic.glasssync.Column;
/**
 * the data about phone(call) to be send , 
 * */
public enum PhoneColumn implements Column {

    state(Integer.class),       // phone state, 0(idle); 1(ring); 2(offhook) ;20+ is operator code
                                // 21 end call, 22 accept call
    name(String.class),         // if ringing, name maybe need
    phoneNumber(String.class);  // if ringing, number will be need
    

    private final Class<?> mClass;

    PhoneColumn(Class<?> c) {
        mClass = c;
    }

    @Override
    public String key() {
        return name();
    }

    @Override
    public Class<?> type() {
        return mClass;
    }

}
