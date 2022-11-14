package com.androskin.androscan.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.androskin.androscan.MainActivity;
import com.androskin.androscan.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import jcifs.CIFSContext;
import jcifs.context.SingletonContext;
import jcifs.smb.NtlmPasswordAuthenticator;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbFileOutputStream;

public class FileUtil {
    private final static String TAG = "GA_FileUtil";

    public static final String GOODS_FILE = "Goods.csv";
    public static final String DESTINATION_GOODS_FILE = Environment.getExternalStorageDirectory()+"/Download/"+GOODS_FILE;

    public static final String TEMP_DOCUMENT_FILE = Environment.getExternalStorageDirectory()+"/Download/Temp_document.csv";

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity -
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    public static boolean createFile(String fileName, String fileData) {

        try(OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(fileName), "windows-1251")){
            writer.write(fileData);
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    public static void sambaDownloadFile(Context context, String srcFile, String desFile) throws SmbException {
        SettingsUtil settings = SettingsUtil.getInstance(MainActivity.sharedPreferences);

        String lanUser = settings.getLanUser();
        String lanPassword = settings.getLanPassword();

        try {

            CIFSContext tc = SingletonContext.getInstance();
            SmbFile smbFile;
            if (lanUser != null && lanPassword != null && !lanUser.equals("") && !lanPassword.equals("")) {
                NtlmPasswordAuthenticator passAuthenticator = new NtlmPasswordAuthenticator(null, lanUser, lanPassword);

                CIFSContext authenticator = tc.withCredentials(passAuthenticator);
                smbFile = new SmbFile("smb://" + srcFile, authenticator);
            } else {
                CIFSContext authenticator = tc.withGuestCrendentials();
                smbFile = new SmbFile("smb://" + srcFile, authenticator);
            }
            Log.d(TAG, "smb://" + srcFile);

            if (smbFile.exists()) {
                try (SmbFileInputStream in = new SmbFileInputStream(smbFile);
                     FileOutputStream out = new FileOutputStream(desFile)) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }
                    out.flush();
                } catch (Exception e) {
                    throw new SmbException(e.getMessage());
                }
            } else {
                throw new SmbException(context.getString(R.string.error_could_not_find_file)+"\n"+srcFile);
            }
        } catch (Exception e) {
            throw new SmbException(e.getMessage());
        }
    }

    public static void sambaUploadFile(Context context, String srcFile, String desFile) throws SmbException {
        SettingsUtil settings = SettingsUtil.getInstance(MainActivity.sharedPreferences);

        String lanUser = settings.getLanUser();
        String lanPassword = settings.getLanPassword();

        try {

            CIFSContext tc = SingletonContext.getInstance();
            SmbFile smbFile;
            if (lanUser != null && lanPassword != null && !lanUser.equals("") && !lanPassword.equals("")) {
                NtlmPasswordAuthenticator passAuthenticator = new NtlmPasswordAuthenticator(null, lanUser, lanPassword);

                CIFSContext authenticator = tc.withCredentials(passAuthenticator);
                smbFile = new SmbFile("smb://" + desFile, authenticator);
            } else {
                CIFSContext authenticator = tc.withGuestCrendentials();
                smbFile = new SmbFile("smb://" + desFile, authenticator);
            }
            Log.d(TAG, "smb://" + srcFile);

            try (FileInputStream in = new FileInputStream(srcFile);
                 SmbFileOutputStream out = new SmbFileOutputStream(smbFile)) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                out.flush();
            } catch (Exception e) {
                throw new SmbException(e.getMessage());
            }

        } catch (Exception e) {
            throw new SmbException(e.getMessage());
        }
    }

    public static boolean deleteFile(String filePath) {
        boolean result = false;

        File file = new File(filePath);
        if (file.exists()) {
            result = file.delete();
        }

        return result;
    }
}
