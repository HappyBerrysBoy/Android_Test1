package com.unitloadsystem.activitys;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

public abstract class LocalizedActivity extends Activity {
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(AppLocaleManager.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            ActivityInfo activityInfo = getPackageManager().getActivityInfo(getComponentName(), 0);
            if (activityInfo.labelRes != 0) {
                setTitle(activityInfo.labelRes);
            }
        } catch (PackageManager.NameNotFoundException ignored) {
            // The current Activity is always manifest-declared; keep the system title as a fallback.
        }
    }
}
