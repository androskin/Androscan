package com.androskin.androscan.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class CameraUtil {
    private static final int CAMERA_REQUEST_CODE = 100;

    public static boolean isCameraPermission(Context context) {
        return (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED);
    }

    public static void askCameraPermission(Activity context) {
        ActivityCompat.requestPermissions(context, new String[] {Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
    }

    public static boolean hasFlash(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }
}
