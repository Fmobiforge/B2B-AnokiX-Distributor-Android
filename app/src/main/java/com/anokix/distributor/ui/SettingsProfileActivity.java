package com.anokix.distributor.ui;

import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import com.anokix.distributor.R;
import com.anokix.distributor.model.ProfileSettings;
import com.anokix.distributor.session.SessionManager;

public class SettingsProfileActivity extends BaseSettingsFormActivity {

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_settings_profile;
    }

    @Override
    protected String getScreenTitle() {
        return getString(R.string.settings_profile_title);
    }

    @Override
    protected String getScreenSubtitle() {
        return getString(R.string.settings_profile_sub);
    }

    @Override
    protected void loadData() {
        ProfileSettings data = repository.getProfileSettings();
        SessionManager session = SessionManager.get(this);

        String firstName = !session.getFirstName().isEmpty() ? session.getFirstName() : data.firstName;
        String lastName = !session.getLastName().isEmpty() ? session.getLastName() : data.lastName;
        String email = !session.getEmail().isEmpty() ? session.getEmail() : data.email;

        String displayName = (firstName + " " + lastName).trim();
        if (displayName.isEmpty()) {
            displayName = (data.firstName + " " + data.lastName).trim();
        }

        ((TextView) findViewById(R.id.profileInitials))
                .setText(SettingsUiHelper.initialsOf(displayName, "?"));
        ((TextView) findViewById(R.id.profileDisplayName)).setText(displayName);
        ((TextView) findViewById(R.id.profileCompanyName)).setText(data.companyName);

        SettingsUiHelper.setText(findViewById(R.id.etFirstName), firstName);
        SettingsUiHelper.setText(findViewById(R.id.etLastName), lastName);
        SettingsUiHelper.setText(findViewById(R.id.etEmail), email);
        SettingsUiHelper.setText(findViewById(R.id.etPhone), data.phone);
    }

    @Override
    protected void onSave() {
        String firstName = SettingsUiHelper.textOf(findViewById(R.id.etFirstName));
        String lastName = SettingsUiHelper.textOf(findViewById(R.id.etLastName));
        String email = SettingsUiHelper.textOf(findViewById(R.id.etEmail));
        String phone = SettingsUiHelper.textOf(findViewById(R.id.etPhone));

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
}
