package sopnopriyo.mymusicplayer.util;

import android.os.Environment;

import java.io.File;

/**
 * Created by sopnopriyo on 2014/5/4.
 */
public class FileUtil {
    private static final String SDPATH = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator;
    private static final String ROOTPATH = "/storage/";

    public static String getRootPath() {
        return ROOTPATH;
    }

    public static String getSDPath() {
        return SDPATH;
    }

    public static boolean isDir(String dirPath) {
        File file = new File(dirPath);
        return file.isDirectory();
    }

    public static boolean isMusicFile(String fileName) {
        String endStr = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length());
        return endStr.equals("mp3") || endStr.equals("ape") || endStr.equals("flac") || endStr.equals("wav");
    }

    public static boolean isAccessDir(String dirPath) {
        //If not sdcard directory (neither a cell phone store directory, nor is sd card storage directory), and returns false
        if (!dirPath.contains("sdcard")) {
            return false;
        }
        //System folder returns false
        if (dirPath.contains(".android_secure")) {
            return false;
        }
        return true;
    }

    public static enum SortKey {
        All, Folder, Album, Artist, PlayList, FavoriteList
    }
}
