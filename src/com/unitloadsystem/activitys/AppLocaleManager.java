package com.unitloadsystem.activitys;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import java.util.Locale;

public final class AppLocaleManager {
    public static final String LANGUAGE_SYSTEM = "";

    private static final String PREFERENCES_NAME = "app_settings";
    private static final String LANGUAGE_KEY = "language_tag";

    private AppLocaleManager() {
    }

    public static Context wrap(Context context) {
        String languageTag = getLanguageTag(context);
        if (languageTag.isEmpty()) {
            return context;
        }

        Locale locale = Locale.forLanguageTag(languageTag);
        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        configuration.setLocale(locale);
        configuration.setLayoutDirection(locale);
        return context.createConfigurationContext(configuration);
    }

    public static String getLanguageTag(Context context) {
        return preferences(context).getString(LANGUAGE_KEY, LANGUAGE_SYSTEM);
    }

    public static void setLanguageTag(Context context, String languageTag) {
        SharedPreferences.Editor editor = preferences(context).edit();
        if (languageTag == null || languageTag.isEmpty()) {
            editor.remove(LANGUAGE_KEY);
        } else {
            editor.putString(LANGUAGE_KEY, languageTag);
        }
        editor.apply();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(
                PREFERENCES_NAME, Context.MODE_PRIVATE);
    }
}
