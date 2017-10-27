package com.wuya.reader.util;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by fujiayi on 2017/5/19.
 */

public class FileUtil {

    public static final int READ_LENGTH = 3000;
    //创建一个临时目录，用于复制临时文件，如assets目录下的离线资源文件
    public static String createTmpDir(Context context) {
        String sampleDir = "baiduTTS";
        String tmpDir = Environment.getExternalStorageDirectory().toString() + "/" + sampleDir;
        if (!FileUtil.makeDir(tmpDir)) {
            tmpDir = context.getExternalFilesDir(sampleDir).getAbsolutePath();
            if (!FileUtil.makeDir(sampleDir)) {
                throw new RuntimeException("create model resources dir failed :" + tmpDir);
            }
        }
        return tmpDir;
    }

    public static boolean makeDir(String dirPath) {
        File file = new File(dirPath);
        if (!file.exists()) {
            return file.mkdirs();
        } else {
            return true;
        }
    }

    public static void copyFromAssets(AssetManager assets, String source, String dest, boolean isCover) throws IOException {
        File file = new File(dest);
        if (isCover || (!isCover && !file.exists())) {
            InputStream is = null;
            FileOutputStream fos = null;
            try {
                is = assets.open(source);
                String path = dest;
                fos = new FileOutputStream(path);
                byte[] buffer = new byte[1024];
                int size = 0;
                while ((size = is.read(buffer, 0, 1024)) >= 0) {
                    fos.write(buffer, 0, size);
                }
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } finally {
                        if (is != null) {
                            is.close();
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("NewApi")
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    public static Map<String, String> openTextFile(String fileName, long skipLength, int length) {
        Map<String,String> result = new HashMap<String, String>();
        try{

            FileInputStream fin = new FileInputStream(fileName);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int len = -1;
            byte[] buf = new byte[length];

            byte[] first3bytes = new byte[3];
            fin.read(first3bytes);//找到文档的前三个字节并自动判断文档类型。
            String encoding = getEncoding(first3bytes);

            fin = new FileInputStream(fileName);
            result.put("fileSize", Long.toString(fin.available()));
            if (skipLength>100) {
                fin.skip(skipLength);
            }
            else {
                fin.skip((fin.available()-fin.available() % READ_LENGTH)*skipLength/100);
            }

            int lengthNew = 0;
            while ((len = fin.read(buf)) != -1 && lengthNew<length)
            {
                lengthNew +=len;
                baos.write(buf, 0, len);
            }
            baos.flush();
            result.put("skip",Long.toString(skipLength+lengthNew));

            result.put("content",baos.toString(encoding));
        }catch(Exception e){

            result.put("msg",e.getMessage());

        }
        return result;
    }

    private static String getEncoding(byte[] first3bytes) {
        if (first3bytes[0] == (byte) 0xEF && first3bytes[1] == (byte) 0xBB
                && first3bytes[2] == (byte) 0xBF) {// utf-8
            return "UTF-8";
        } else if (first3bytes[0] == (byte) 0xFF
                && first3bytes[1] == (byte) 0xFE) {
            return "unicode";
        } else if (first3bytes[0] == (byte) 0xFE
                && first3bytes[1] == (byte) 0xFF) {
            return "utf-16be";
        } else if (first3bytes[0] == (byte) 0xFF
                && first3bytes[1] == (byte) 0xFF) {
            return "utf-16le";
        } else {
            return "GBK";
        }
    }

    public static Map<String, String> searchTextFile(String fileName, long skipLength, String searchContent) {
        Map<String,String> result = new HashMap<String, String>();
        try{

            FileInputStream fin = new FileInputStream(fileName);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int len = -1;
            byte[] buf = new byte[READ_LENGTH];

            byte[] first3bytes = new byte[3];
            fin.read(first3bytes);//找到文档的前三个字节并自动判断文档类型。
            String encoding = getEncoding(first3bytes);

            fin = new FileInputStream(fileName);
            result.put("fileSize", Long.toString(fin.available()));
            if (skipLength>100) {
                fin.skip(skipLength);
            }
            else if(skipLength>0){
                fin.skip((fin.available()-fin.available() % READ_LENGTH)*skipLength/100);
            }
            int lengthNew = 0;
            String content = "";
            boolean found = false;
            while ((len = fin.read(buf)) != -1 && !found)
            {
                baos = new ByteArrayOutputStream();
                baos.write(buf, 0, len);
                baos.flush();
                content = baos.toString(encoding);
                if (content.contains(searchContent)) {
                    found = true;
                }
                else {
                    lengthNew +=len;
                }
            }
            result.put("skip",Long.toString(skipLength+lengthNew));

            result.put("content",content);
            fin.close();
        }catch(Exception e){

            result.put("msg",e.getMessage());

        }
        return result;
    }
}
