package com.anokix.distributorapp.ui;

import android.view.View;
import android.widget.Spinner;
import android.widget.Toast;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.network.ApiCallback;
import com.anokix.distributorapp.network.ApiClient;
import com.anokix.distributorapp.network.dto.BaseInfoData;
import com.anokix.distributorapp.network.dto.PreferencesData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Preferences — language / currency / timezone / date format (5-Settings-Preferences.png).
 * Currencies + timezones come from GET api/common/base-info; the current selection from
 * GET api/common/preferences. Saved via POST api/common/preferences/update. Spinner labels
 * differ from the API values, so selection is tracked by index against parallel value lists.
 */
public class SettingsPreferencesActivity extends BaseSettingsFormActivity {

    private static final List<String> LANGUAGE_LABELS = Arrays.asList("English", "Afrikaans", "Zulu");
    private static final List<String> LANGUAGE_VALUES = Arrays.asList("en", "af", "zu");
    private static final List<String> DATE_LABELS = Arrays.asList("19 May 2024", "05/19/2024", "2024-05-19");
    private static final List<String> DATE_VALUES = Arrays.asList("DD MMM YYYY", "MM/DD/YYYY", "YYYY-MM-DD");

    // Currency + timezone are filled from base-info (with sensible fallbacks).
    private final List<String> currencyLabels = new ArrayList<>();
    private final List<String> currencyValues = new ArrayList<>();   // API value = currency id
    private final List<String> currencyCodes = new ArrayList<>();    // for matching the loaded code
    private final List<String> timezoneLabels = new ArrayList<>();
    private final List<String> timezoneValues = new ArrayList<>();   // API value = IANA name

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
        ApiClient.get(this).getBaseInfo(new ApiCallback<BaseInfoData>() {
            @Override
            public void onSuccess(BaseInfoData data) {
                buildCurrencies(data == null ? null : data.currencies);
                buildTimezones(data == null ? null : data.timezones);
                bindSpinners();
                loadPreferences();
            }

            @Override
            public void onError(String message) {
                buildCurrencies(null);
                buildTimezones(null);
                bindSpinners();
                loadPreferences();
            }
        });
    }

    private void buildCurrencies(List<BaseInfoData.Currency> currencies) {
        currencyLabels.clear();
        currencyValues.clear();
        currencyCodes.clear();
        if (currencies != null && !currencies.isEmpty()) {
            for (BaseInfoData.Currency c : currencies) {
                String symbol = c.symbol != null ? c.symbol : c.code;
                currencyLabels.add((c.name != null ? c.name : c.code) + " (" + symbol + ")");
                currencyValues.add(c.id != null ? c.id : c.code);
                currencyCodes.add(c.code != null ? c.code : "");
            }
        } else {
            currencyLabels.addAll(Arrays.asList("South African Rand (R)", "US Dollar ($)", "Euro (€)"));
            currencyValues.addAll(Arrays.asList("1", "2", "3"));
            currencyCodes.addAll(Arrays.asList("ZAR", "USD", "EUR"));
        }
    }

    private void buildTimezones(List<BaseInfoData.Timezone> timezones) {
        timezoneLabels.clear();
        timezoneValues.clear();
        if (timezones != null && !timezones.isEmpty()) {
            for (BaseInfoData.Timezone t : timezones) {
                String name = t.time_zone != null ? t.time_zone : t.name;
                timezoneLabels.add(name + " (" + gmt(t.time_offset) + ")");
                timezoneValues.add(name);
            }
        } else {
            timezoneLabels.add("Africa/Johannesburg (GMT+2)");
            timezoneValues.add("Africa/Johannesburg");
        }
    }

    private static String gmt(long offsetSeconds) {
        long hours = offsetSeconds / 3600;
        long mins = Math.abs(offsetSeconds % 3600) / 60;
        String sign = hours >= 0 ? "+" : "-";
        if (mins == 0) {
            return String.format(Locale.US, "GMT%s%d", sign, Math.abs(hours));
        }
        return String.format(Locale.US, "GMT%s%d:%02d", sign, Math.abs(hours), mins);
    }

    private void bindSpinners() {
        spinner(R.id.languageSpinner).setAdapter(SettingsUiHelper.simpleAdapter(this, LANGUAGE_LABELS));
        spinner(R.id.currencySpinner).setAdapter(SettingsUiHelper.simpleAdapter(this, currencyLabels));
        spinner(R.id.timezoneSpinner).setAdapter(SettingsUiHelper.simpleAdapter(this, timezoneLabels));
        spinner(R.id.dateFormatSpinner).setAdapter(SettingsUiHelper.simpleAdapter(this, DATE_LABELS));
    }

    private void loadPreferences() {
        ApiClient.get(this).getPreferences(new ApiCallback<PreferencesData>() {
            @Override
            public void onSuccess(PreferencesData data) {
                PreferencesData.Preferences p = data == null ? null : data.preferences;
                selectByValue(R.id.languageSpinner, LANGUAGE_VALUES, p == null ? "en" : p.language, 0);
                selectByValue(R.id.currencySpinner, currencyCodes, p == null ? "ZAR" : p.currency, 0);
                selectByValue(R.id.timezoneSpinner, timezoneValues, p == null ? "" : p.timezone, 0);
                selectByValue(R.id.dateFormatSpinner, DATE_VALUES, p == null ? "DD MMM YYYY" : p.date_format, 0);
            }

            @Override
            public void onError(String message) {
                // Leave spinners at their first entry.
            }
        });
    }

    private void selectByValue(int spinnerId, List<String> values, String value, int fallback) {
        int index = fallback;
        if (value != null) {
            for (int i = 0; i < values.size(); i++) {
                if (value.equalsIgnoreCase(values.get(i))) {
                    index = i;
                    break;
                }
            }
        }
        if (index >= 0 && index < spinner(spinnerId).getCount()) {
            spinner(spinnerId).setSelection(index);
        }
    }

    private Spinner spinner(int id) {
        return findViewById(id);
    }

    @Override
    protected void onSave() {
        final String language = valueAt(R.id.languageSpinner, LANGUAGE_VALUES);
        final String currency = valueAt(R.id.currencySpinner, currencyValues);
        final String timezone = valueAt(R.id.timezoneSpinner, timezoneValues);
        final String dateFormat = valueAt(R.id.dateFormatSpinner, DATE_VALUES);

        final View save = findViewById(R.id.saveButton);
        save.setEnabled(false);
        ApiClient.get(this).updatePreferences(language, currency, timezone, dateFormat,
                new ApiCallback<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        toastSaved();
                        finish();
                    }

                    @Override
                    public void onError(String message) {
                        save.setEnabled(true);
                        Toast.makeText(SettingsPreferencesActivity.this,
                                message != null ? message : getString(R.string.error_generic),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private String valueAt(int spinnerId, List<String> values) {
        int pos = spinner(spinnerId).getSelectedItemPosition();
        return pos >= 0 && pos < values.size() ? values.get(pos) : "";
    }
}
