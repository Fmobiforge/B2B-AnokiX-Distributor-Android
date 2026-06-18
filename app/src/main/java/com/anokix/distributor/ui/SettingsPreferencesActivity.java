package com.anokix.distributor.ui;

import android.widget.Spinner;

import com.anokix.distributor.R;
import com.anokix.distributor.model.AppPreferences;

import java.util.Arrays;
import java.util.List;

public class SettingsPreferencesActivity extends BaseSettingsFormActivity {

    private static final List<String> LANGUAGES = Arrays.asList("English", "Afrikaans", "Zulu");
    private static final List<String> CURRENCIES = Arrays.asList(
            "South African Rand (ZAR)", "US Dollar (USD)", "Euro (EUR)");
    private static final List<String> TIMEZONES = Arrays.asList(
            "Africa/Johannesburg", "Africa/Cape_Town", "UTC");
    private static final List<String> DATE_FORMATS = Arrays.asList(
            "19 May 2024", "2024-05-19", "05/19/2024");

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_settings_preferences;
    }

    @Override
    protected String getScreenTitle() {
        return getString(R.string.settings_preferences_title);
    }

    @Override
    protected String getScreenSubtitle() {
        return getString(R.string.settings_preferences_sub);
    }

    @Override
    protected void loadData() {
        AppPreferences data = repository.getAppPreferences();
        Spinner languageSpinner = findViewById(R.id.languageSpinner);
        Spinner currencySpinner = findViewById(R.id.currencySpinner);
        Spinner timezoneSpinner = findViewById(R.id.timezoneSpinner);
        Spinner dateFormatSpinner = findViewById(R.id.dateFormatSpinner);

        languageSpinner.setAdapter(SettingsUiHelper.simpleAdapter(this, LANGUAGES));
        currencySpinner.setAdapter(SettingsUiHelper.simpleAdapter(this, CURRENCIES));
        timezoneSpinner.setAdapter(SettingsUiHelper.simpleAdapter(this, TIMEZONES));
        dateFormatSpinner.setAdapter(SettingsUiHelper.simpleAdapter(this, DATE_FORMATS));

        SettingsUiHelper.selectSpinnerItem(languageSpinner, LANGUAGES, data.language);
        SettingsUiHelper.selectSpinnerItem(currencySpinner, CURRENCIES, data.currency);
        SettingsUiHelper.selectSpinnerItem(timezoneSpinner, TIMEZONES, data.timezone);
        SettingsUiHelper.selectSpinnerItem(dateFormatSpinner, DATE_FORMATS, data.dateFormat);
    }

    @Override
    protected void onSave() {
        AppPreferences updated = new AppPreferences(
                SettingsUiHelper.selectedItem(findViewById(R.id.languageSpinner)),
                SettingsUiHelper.selectedItem(findViewById(R.id.currencySpinner)),
                SettingsUiHelper.selectedItem(findViewById(R.id.timezoneSpinner)),
                SettingsUiHelper.selectedItem(findViewById(R.id.dateFormatSpinner))
        );
        repository.saveAppPreferences(updated);
        toastSaved();
        finish();
    }
}
