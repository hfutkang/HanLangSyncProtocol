package cn.ingenic.glasssync.devicemanager;

import cn.ingenic.glasssync.Column;
/**
 *  
 * */
public enum DeviceColumn implements Column {

    command(Integer.class),       
    data(String.class);
    

    private final Class<?> mClass;

    DeviceColumn(Class<?> c) {
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
