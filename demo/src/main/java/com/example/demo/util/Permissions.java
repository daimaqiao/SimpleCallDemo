package com.example.demo.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

public class Permissions {
    public static final int PERMISSION_AUDIO= 0;

    private static boolean checkAudioPermissions(Context context) {
         return ContextCompat.checkSelfPermission(context,
                 Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }
    public static boolean requestAudioPermissions(Activity activity) {
        if (checkAudioPermissions(activity))
            return true;

        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.RECORD_AUDIO},
                PERMISSION_AUDIO);

        return checkAudioPermissions(activity);
    }
}
