package com.lzb.record.effect;

/**
 * Created by lzb on 2018/11/28.
 */
public class EffectUtils {
    //音效类型
    public static final int MODE_NORMAL = 0;
    public static final int MODE_LUOLI = 1;
    public static final int MODE_DASHU = 2;
    public static final int MODE_JINGSONG = 3;
    public static final int MODE_GAOGUAI = 4;
    public static final int MODE_KONGLING = 5;
    public static final int MODE_HECHANG = 6;

    /**
     * 音效处理
     *
     * @param path
     * @param type
     */
    public native static void fix(String path, int type);

    static {
        System.loadLibrary("fmodL");
        System.loadLibrary("fmod");
        System.loadLibrary("voice");
    }
}