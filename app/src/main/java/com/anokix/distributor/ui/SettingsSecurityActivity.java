package com.anokix.distributor.ui;

import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.anokix.distributor.R;

public class SettingsSecurityActivity extends BaseSettingsFormActivity {

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_settings_security;
    }

    @Override
    protected String getScreenTitle() {
        return getString(R.string.settings_security_title);
    }

    @Override
    protected String getScreenSubtitle() {
        return getString(R.string.settings_security_sub);
    }

    @Override
    protected void bindViews() {
        super.bindViews();
        SettingsUiHelper.setupPasswordToggle(
                (EditText) findViewById(R.id.etCurrentPassword),
                (ImageButton) findViewById(R.id.toggleCurrentPassword));
        SettingsUiHelper.setupPasswordToggle(
                (EditText) findViewById(R.id.etNewPassword),
                (ImageButton) findViewById(R.id.toggleNewPassword));
        SettingsUiHelper.setupPasswordToggle(
                (EditText) findViewById(R.id.etConfirmPassword),
                (ImageButton) findViewById(R.id.toggleConfirmPassword));
    }

    @Override
    protected void loadData() {
        // Password fields start empty.
    }

    @Override
    protected void onSave() {
        String current = SettingsUiHelper.textOf(findViewById(R.id.etCurrentPassword));
        String newPassword = SettingsUiHelper.textOf(findViewById(R.id.etNewPassword));
        String confirm = SettingsUiHelper.textOf(findViewById(R.id.etConfirmPassword));

        String error = repository.changePassword(current, newPassword, confirm);
        if (error != null) {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            return;
        }

        toastSaved();
        finish();
    }
}
