package com.anokix.distributor.ui;

import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.anokix.distributor.R;
import com.anokix.distributor.network.ApiCallback;
import com.anokix.distributor.network.ApiClient;

/**
 * Security — change the account password via POST api/common/profile/password
 * (4-Settings-Security.png). New password must be at least 8 characters and match the
 * confirmation.
 */
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
        final String current = SettingsUiHelper.textOf(findViewById(R.id.etCurrentPassword));
        final String newPassword = SettingsUiHelper.textOf(findViewById(R.id.etNewPassword));
        String confirm = SettingsUiHelper.textOf(findViewById(R.id.etConfirmPassword));

        if (TextUtils.isEmpty(current)) {
            toast(R.string.err_current_password);
            return;
        }
        if (newPassword.length() < 8) {
            toast(R.string.err_password_length);
            return;
        }
        if (!newPassword.equals(confirm)) {
            toast(R.string.err_password_match);
            return;
        }

        final View save = findViewById(R.id.saveButton);
        save.setEnabled(false);
        ApiClient.get(this).changePassword(current, newPassword, new ApiCallback<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Toast.makeText(SettingsSecurityActivity.this,
                        R.string.settings_password_changed, Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String message) {
                save.setEnabled(true);
                Toast.makeText(SettingsSecurityActivity.this,
                        message != null ? message : getString(R.string.error_generic),
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}
