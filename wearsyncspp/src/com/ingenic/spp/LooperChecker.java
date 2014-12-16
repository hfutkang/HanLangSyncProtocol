package com.ingenic.spp;

import android.os.Looper;

class LooperChecker {
    // Debugging

    private static Looper sLooper;
    private static String TAG = "LooperChecker";

    private LooperChecker() {
    }

    public static LooperChecker makeLooperChecker(String tag) {
        TAG = tag;
        sLooper = Looper.myLooper();
        if (sLooper == null) {
            throw new RuntimeException(TAG+" must be called from Looper thread");
        }
        return new LooperChecker();
    }

    protected static void checkLooper() {
        if (sLooper != Looper.myLooper()) {
            throw new RuntimeException(TAG+" is being used by another Looper thread");
        }
    }

}


