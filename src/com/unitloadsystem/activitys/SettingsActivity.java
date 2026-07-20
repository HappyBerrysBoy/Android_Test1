package com.unitloadsystem.activitys;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends LocalizedActivity {
    private static final String[] LANGUAGE_TAGS = {
            AppLocaleManager.LANGUAGE_SYSTEM, "en", "ko", "ja", "zh", "hi", "es", "pt"
    };

    private TextView currentLanguageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_layout);

        currentLanguageView = (TextView) findViewById(R.id.currentLanguage);
        ((TextView) findViewById(R.id.versionInfo)).setText(getString(
                R.string.settingsVersionFormat, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
        updateLanguageSummary();
    }

    public void chooseLanguage(View view) {
        final String[] labels = getLanguageLabels();
        ModernChoiceDialog.show(this, R.string.settingsChooseLanguage, labels,
                getSelectedLanguageIndex(), new ModernChoiceDialog.OnChoiceSelected() {
                    @Override
                    public void onChoiceSelected(int index) {
                        String selectedTag = LANGUAGE_TAGS[index];
                        if (selectedTag.equals(AppLocaleManager.getLanguageTag(SettingsActivity.this))) {
                            return;
                        }
                        AppLocaleManager.setLanguageTag(SettingsActivity.this, selectedTag);
                        Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                });
    }

    public void goBack(View view) {
        finish();
    }

    public void openDeveloperSupport(View view) {
        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse(getString(R.string.developerSupportKakaoUrl)));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(this, R.string.developerSupportUnavailable, Toast.LENGTH_LONG).show();
        }
    }

    private void updateLanguageSummary() {
        String[] labels = getLanguageLabels();
        currentLanguageView.setText(getString(
                R.string.settingsCurrentLanguage, labels[getSelectedLanguageIndex()]));
    }

    private int getSelectedLanguageIndex() {
        String selectedTag = AppLocaleManager.getLanguageTag(this);
        for (int i = 0; i < LANGUAGE_TAGS.length; i++) {
            if (LANGUAGE_TAGS[i].equals(selectedTag)) {
                return i;
            }
        }
        return 0;
    }

    private String[] getLanguageLabels() {
        return new String[] {
                getString(R.string.settingsLanguageSystem),
                getString(R.string.languageEnglish),
                getString(R.string.languageKorean),
                getString(R.string.languageJapanese),
                getString(R.string.languageChinese),
                getString(R.string.languageHindi),
                getString(R.string.languageSpanish),
                getString(R.string.languagePortuguese)
        };
    }
}
