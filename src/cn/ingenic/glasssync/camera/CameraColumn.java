package cn.ingenic.glasssync.camera;

import cn.ingenic.glasssync.Column;

public enum CameraColumn implements Column {

    watchRequest(Integer.class),
	maxScreen(Integer.class),

	phoneResponseState(Integer.class),
	openCameraResult(Integer.class),
	previewData(byte[].class),
	takePictureResult(Boolean.class),
	picturePath(String.class),
	pictureData(byte[].class),
	exit(String.class);

    private final Class<?> mClass;

    CameraColumn(Class<?> c) {
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
