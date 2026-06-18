package com.anokix.distributor.ui;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.anokix.distributor.R;
import com.anokix.distributor.model.AppPreferences;
import com.anokix.distributor.model.BusinessSettings;
import com.anokix.distributor.model.NotificationPreference;
import com.anokix.distributor.model.ProfileSettings;
import com.anokix.distributor.session.SessionManager;
import com.anokix.distributor.ui.views.RobotoSwitchCompat;

import java.util.Arrays;
import java.util.List;

/**
 * Detail screen for each settings menu item (Profile, Business, Notifications,
 * Security, Preferences). Matches the web dashboard settings sections.
 */
public class SettingsFormActivity extends BaseSecondaryActivity {

    public static final String EXTRA_SECTION = "section";

    public static final String SECTION_PROFILE = "profile";
    public static final String SECTION_BUSINESS = "business";
    public static final String SECTION_NOTIFICATIONS = "notifications";
    public static final String SECTION_SECURITY = "security";
    public static final String SECTION_PREFERENCES = "preferences";

    private static final List<String> LANGUAGES = Arrays.asList("English", "Afrikaans", "Zulu");
    private static final List<String> CURRENCIES = Arrays.asList(
            "South African Rand (ZAR)", "US Dollar (USD)", "Euro (EUR)");
    private static final List<String> TIMEZONES = Arrays.asList(
            "Africa/Johannesburg", "Africa/Cape_Town", "UTC");
    private static final List<String> DATE_FORMATS = Arrays.asList(
            "19 May 2024", "2024-05-19", "05/19/2024");

    private String section;
    private View sectionView;

    public static Intent intent(Context context, String section) {
        Intent intent = new Intent(context, SettingsFormActivity.class);
        intent.putExtra(EXTRA_SECTION, section);
        return intent;
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_settings_form;
    }

    @Override
    protected String getScreenTitle() {
        switch (sectionKey()) {
            case SECTION_PROFILE:
                return getString(R.string.settings_profile_title);
            case SECTION_BUSINESS:
                return getString(R.string.settings_business_title);
            case SECTION_NOTIFICATIONS:
                return getString(R.string.notification_preferences);
            case SECTION_SECURITY:
                return getString(R.string.settings_security_title);
            case SECTION_PREFERENCES:
                return getString(R.string.settings_preferences_title);
            default:
                return getString(R.string.settings);
        }
    }

    @Override
    protected String getScreenSubtitle() {
        switch (sectionKey()) {
            case SECTION_PROFILE:
                return getString(R.string.settings_profile_sub);
            case SECTION_BUSINESS:
                return getString(R.string.settings_business_sub);
            case SECTION_NOTIFICATIONS:
                return getString(R.string.settings_notifications_sub);
            case SECTION_SECURITY:
                return getString(R.string.settings_security_sub);
            case SECTION_PREFERENCES:
                return getString(R.string.settings_preferences_sub);
            default:
                return null;
        }
    }

    @Override
    protected void bindViews() {
        section = getIntent().getStringExtra(EXTRA_SECTION);
        if (section == null) {
            section = SECTION_PROFILE;
        }

        FrameLayout container = findViewById(R.id.sectionContent);
        sectionView = inflateSection(container);
        findViewById(R.id.cancelButton).setOnClickListener(v -> finish());
        findViewById(R.id.saveButton).setOnClickListener(v -> saveSection());
    }

    @Override
    protected void loadData() {
        if (sectionView == null) {
            return;
        }
        switch (sectionKey()) {
            case SECTION_PROFILE:
                bindProfile();
                break;
            case SECTION_BUSINESS:
                bindBusiness();
                break;
            case SECTION_NOTIFICATIONS:
                bindNotifications();
                break;
            case SECTION_SECURITY:
                bindSecurity();
                break;
            case SECTION_PREFERENCES:
                bindPreferences();
                break;
            default:
                break;
        }
    }

    private String sectionKey() {
        if (section != null) {
            return section;
        }
        String key = getIntent().getStringExtra(EXTRA_SECTION);
        return key == null ? SECTION_PROFILE : key;
    }

    private View inflateSection(FrameLayout container) {
        int layoutRes;
        switch (sectionKey()) {
            case SECTION_BUSINESS:
                layoutRes = R.layout.content_settings_business;
                break;
            case SECTION_NOTIFICATIONS:
                layoutRes = R.layout.content_settings_notifications;
                break;
            case SECTION_SECURITY:
                layoutRes = R.layout.content_settings_security;
                break;
            case SECTION_PREFERENCES:
                layoutRes = R.layout.content_settings_preferences;
                break;
            case SECTION_PROFILE:
            default:
                layoutRes = R.layout.content_settings_profile;
                break;
        }
        return getLayoutInflater().inflate(layoutRes, container, true);
    }

    private void bindProfile() {
        ProfileSettings data = repository.getProfileSettings();
        SessionManager session = SessionManager.get(this);

        String firstName = !session.getFirstName().isEmpty() ? session.getFirstName() : data.firstName;
        String lastName = !session.getLastName().isEmpty() ? session.getLastName() : data.lastName;
        String email = !session.getEmail().isEmpty() ? session.getEmail() : data.email;

        String displayName = (firstName + " " + lastName).trim();
        if (displayName.isEmpty()) {
            displayName = data.firstName + " " + data.lastName;
        }

        ((TextView) sectionView.findViewById(R.id.profileInitials))
                .setText(initialsOf(displayName, "?"));
        ((TextView) sectionView.findViewById(R.id.profileDisplayName)).setText(displayName.trim());
        ((TextView) sectionView.findViewById(R.id.profileCompanyName)).setText(data.companyName);

        setText(sectionView.findViewById(R.id.etFirstName), firstName);
        setText(sectionView.findViewById(R.id.etLastName), lastName);
        setText(sectionView.findViewById(R.id.etEmail), email);
        setText(sectionView.findViewById(R.id.etPhone), data.phone);
    }

    private void bindBusiness() {
        BusinessSettings data = repository.getBusinessSettings();
        setText(sectionView.findViewById(R.id.etBusinessName), data.businessName);
        setText(sectionView.findViewById(R.id.etRegistrationNumber), data.registrationNumber);
        setText(sectionView.findViewById(R.id.etVatNumber), data.vatNumber);
        setText(sectionView.findViewById(R.id.etAddress), data.address);
        setText(sectionView.findViewById(R.id.etCity), data.city);
    }

    private void bindSecurity() {
        setupPasswordToggle(sectionView.findViewById(R.id.etCurrentPassword),
                sectionView.findViewById(R.id.toggleCurrentPassword));
        setupPasswordToggle(sectionView.findViewById(R.id.etNewPassword),
                sectionView.findViewById(R.id.toggleNewPassword));
        setupPasswordToggle(sectionView.findViewById(R.id.etConfirmPassword),
                sectionView.findViewById(R.id.toggleConfirmPassword));
    }

    private void bindPreferences() {
        AppPreferences data = repository.getAppPreferences();
        Spinner languageSpinner = sectionView.findViewById(R.id.languageSpinner);
        Spinner currencySpinner = sectionView.findViewById(R.id.currencySpinner);
        Spinner timezoneSpinner = sectionView.findViewById(R.id.timezoneSpinner);
        Spinner dateFormatSpinner = sectionView.findViewById(R.id.dateFormatSpinner);

        languageSpinner.setAdapter(simpleAdapter(LANGUAGES));
        currencySpinner.setAdapter(simpleAdapter(CURRENCIES));
        timezoneSpinner.setAdapter(simpleAdapter(TIMEZONES));
        dateFormatSpinner.setAdapter(simpleAdapter(DATE_FORMATS));

        selectSpinnerItem(languageSpinner, LANGUAGES, data.language);
        selectSpinnerItem(currencySpinner, CURRENCIES, data.currency);
        selectSpinnerItem(timezoneSpinner, TIMEZONES, data.timezone);
        selectSpinnerItem(dateFormatSpinner, DATE_FORMATS, data.dateFormat);
    }

    private void bindNotifications() {
        setSwitchFromRepo(R.id.switchOrderAlerts, "order_alerts", true);
        setSwitchFromRepo(R.id.switchLowStock, "low_stock", true);
        setSwitchFromRepo(R.id.switchDeliveryUpdates, "delivery_updates", true);
        setSwitchFromRepo(R.id.switchFinanceReminders, "finance_reminders", true);
        setSwitchFromRepo(R.id.switchReportReady, "report_ready", false);
    }

    private void setSwitchFromRepo(int switchId, String key, boolean defaultValue) {
        RobotoSwitchCompat switchView = sectionView.findViewById(switchId);
        for (NotificationPreference pref : repository.getNotificationPreferences()) {
            if (key.equals(pref.key)) {
                switchView.setChecked(pref.enabled);
                return;
            }
        }
        switchView.setChecked(defaultValue);
    }

    private void saveNotifications() {
        List<NotificationPreference> updated = Arrays.asList(
                new NotificationPreference("order_alerts",
                        getString(R.string.notif_order_alerts_title),
                        getString(R.string.notif_order_alerts_sub),
                        isSwitchChecked(R.id.switchOrderAlerts)),
                new NotificationPreference("low_stock",
                        getString(R.string.notif_low_stock_title),
                        getString(R.string.notif_low_stock_sub),
                        isSwitchChecked(R.id.switchLowStock)),
                new NotificationPreference("delivery_updates",
                        getString(R.string.notif_delivery_title),
                        getString(R.string.notif_delivery_sub),
                        isSwitchChecked(R.id.switchDeliveryUpdates)),
                new NotificationPreference("finance_reminders",
                        getString(R.string.notif_finance_title),
                        getString(R.string.notif_finance_sub),
                        isSwitchChecked(R.id.switchFinanceReminders)),
                new NotificationPreference("report_ready",
                        getString(R.string.notif_report_title),
                        getString(R.string.notif_report_sub),
                        isSwitchChecked(R.id.switchReportReady))
        );
        repository.saveNotificationPreferences(updated);
        toastSaved();
        finish();
    }

    private boolean isSwitchChecked(int switchId) {
        RobotoSwitchCompat switchView = sectionView.findViewById(switchId);
        return switchView != null && switchView.isChecked();
    }

    private void saveSection() {
        switch (sectionKey()) {
            case SECTION_PROFILE:
                saveProfile();
                break;
            case SECTION_BUSINESS:
                saveBusiness();
                break;
            case SECTION_NOTIFICATIONS:
                saveNotifications();
                break;
            case SECTION_SECURITY:
                saveSecurity();
                break;
            case SECTION_PREFERENCES:
                savePreferences();
                break;
            default:
                break;
        }
    }

    private void saveProfile() {
        String firstName = textOf(sectionView.findViewById(R.id.etFirstName));
        String lastName = textOf(sectionView.findViewById(R.id.etLastName));
        String email = textOf(sectionView.findViewById(R.id.etEmail));
        String phone = textOf(sectionView.findViewById(R.id.etPhone));

        if (TextUtils.isEmpty(firstName)) {
            toast(R.string.err_first_name_settings);
            return;
        }
        if (TextUtils.isEmpty(lastName)) {
            toast(R.string.err_last_name_settings);
            return;
        }
        if (TextUtils.isEmpty(email)) {
            toast(R.string.err_email_settings);
            return;
        }

        ProfileSettings current = repository.getProfileSettings();
        ProfileSettings updated = new ProfileSettings(firstName, lastName, email, phone, current.companyName);
        repository.saveProfileSettings(updated);
        SessionManager.get(this).setUser(firstName, lastName, email, SessionManager.get(this).getRoleLabel());
        toastSaved();
        finish();
    }

    private void saveBusiness() {
        String businessName = textOf(sectionView.findViewById(R.id.etBusinessName));
        if (TextUtils.isEmpty(businessName)) {
            toast(R.string.err_business_name);
            return;
        }

        BusinessSettings updated = new BusinessSettings(
                businessName,
                textOf(sectionView.findViewById(R.id.etRegistrationNumber)),
                textOf(sectionView.findViewById(R.id.etVatNumber)),
                textOf(sectionView.findViewById(R.id.etAddress)),
                textOf(sectionView.findViewById(R.id.etCity))
        );
        repository.saveBusinessSettings(updated);
        toastSaved();
        finish();
    }

    private void saveSecurity() {
        String current = textOf(sectionView.findViewById(R.id.etCurrentPassword));
        String newPassword = textOf(sectionView.findViewById(R.id.etNewPassword));
        String confirm = textOf(sectionView.findViewById(R.id.etConfirmPassword));

        String error = repository.changePassword(current, newPassword, confirm);
        if (error != null) {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            return;
        }

        toastSaved();
        finish();
    }

    private void savePreferences() {
        Spinner languageSpinner = sectionView.findViewById(R.id.languageSpinner);
        Spinner currencySpinner = sectionView.findViewById(R.id.currencySpinner);
        Spinner timezoneSpinner = sectionView.findViewById(R.id.timezoneSpinner);
        Spinner dateFormatSpinner = sectionView.findViewById(R.id.dateFormatSpinner);

        AppPreferences updated = new AppPreferences(
                selectedItem(languageSpinner),
                selectedItem(currencySpinner),
                selectedItem(timezoneSpinner),
                selectedItem(dateFormatSpinner)
        );
        repository.saveAppPreferences(updated);
        toastSaved();
        finish();
    }

    private ArrayAdapter<String> simpleAdapter(List<String> items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                R.layout.item_spinner, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private void selectSpinnerItem(Spinner spinner, List<String> options, String value) {
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).equals(value)) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private String selectedItem(Spinner spinner) {
        Object item = spinner.getSelectedItem();
        return item == null ? "" : item.toString();
    }

    private void setupPasswordToggle(EditText field, ImageButton toggle) {
        toggle.setOnClickListener(v -> {
            boolean visible = field.getTransformationMethod() == null;
            if (visible) {
                field.setTransformationMethod(PasswordTransformationMethod.getInstance());
                toggle.setImageResource(R.drawable.ic_visibility);
            } else {
                field.setTransformationMethod(null);
                toggle.setImageResource(R.drawable.ic_visibility_off);
            }
            field.setSelection(field.getText() != null ? field.getText().length() : 0);
        });
    }

    private String initialsOf(String name, String fallback) {
        if (name == null || name.trim().isEmpty()) {
            return fallback;
        }
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length && sb.length() < 2; i++) {
            if (!parts[i].isEmpty()) {
                sb.append(Character.toUpperCase(parts[i].charAt(0)));
            }
        }
        return sb.length() == 0 ? fallback : sb.toString();
    }

    private static void setText(View view, String value) {
        if (view instanceof EditText) {
            ((EditText) view).setText(value == null ? "" : value);
        }
    }

    private static String textOf(View view) {
        if (view instanceof EditText && ((EditText) view).getText() != null) {
            return ((EditText) view).getText().toString().trim();
        }
        return "";
    }

    private void toast(int resId) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show();
    }

    private void toastSaved() {
        Toast.makeText(this, R.string.settings_saved_msg, Toast.LENGTH_SHORT).show();
    }
}
