package com.connectycube.sample.chat.utils;

import android.os.Environment;

import com.connectycube.sample.chat.App;

import java.io.File;

public class StorageUtils {
    public static String getAppExternalDataDirectoryPath() {
        StringBuilder sb = new StringBuilder();
        sb.append(Environment.getExternalStorageDirectory())
                .append(File.separator)
                .append("Android")
                .append(File.separator)
                .append("data")
                .append(File.separator)
                .append(App.getInstance().getPackageName())
                .append(File.separator);

        return sb.toString();
    }

    public static File getAppExternalDataDirectoryFile() {
        File dataDirectoryFile = new File(getAppExternalDataDirectoryPath());
        dataDirectoryFile.mkdirs();

        return dataDirectoryFile;
    }
}
