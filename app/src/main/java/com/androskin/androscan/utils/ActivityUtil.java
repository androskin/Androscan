package com.androskin.androscan.utils;

import android.app.Activity;
import android.content.pm.ActivityInfo;

public class ActivityUtil {

    public static void setPortraitOrientation(Activity activity) {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

}
