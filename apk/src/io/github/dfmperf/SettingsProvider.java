package io.github.dfmperf;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

/**
 * 设置读取通道：游戏进程里的模块通过 ContentResolver 查询这里，
 * 不依赖 XSharedPreferences（后者在部分 LSPosed 版本/SELinux 策略下读不到）。
 *
 * 用法：content://io.github.dfmperf.settings/<key>  ->  单列单行，值转成字符串。
 * 只暴露画质/帧率/渲染比例/短边/模式这类非敏感设置。
 */
public class SettingsProvider extends ContentProvider {
    public static final String AUTHORITY = "io.github.dfmperf.settings";
    private static final String[] COLUMNS = {"value"};

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        String key = uri.getLastPathSegment();
        MatrixCursor cursor = new MatrixCursor(COLUMNS);
        if (key == null) return cursor;
        SharedPreferences sp = Prefs.get(getContext());
        Object value;
        if (Prefs.MODE.equals(key)) value = sp.getString(Prefs.MODE, "off");
        else if (Prefs.QUALITY.equals(key)) value = sp.getInt(Prefs.QUALITY, 3);
        else if (Prefs.FPS.equals(key)) value = sp.getInt(Prefs.FPS, 2);
        else if (Prefs.RATIO.equals(key)) value = sp.getInt(Prefs.RATIO, 100);
        else if (Prefs.SHORT_EDGE.equals(key)) value = sp.getInt(Prefs.SHORT_EDGE, 1080);
        else if (Prefs.RENDER_CUSTOM.equals(key)) value = sp.getBoolean(Prefs.RENDER_CUSTOM, false) ? 1 : 0;
        else value = null;
        if (value != null) cursor.addRow(new Object[]{String.valueOf(value)});
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
