package com.anokix.distributorapp.ui;

import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.network.ApiCallback;
import com.anokix.distributorapp.network.ApiClient;
import com.anokix.distributorapp.network.dto.BaseInfoData;
import com.anokix.distributorapp.network.dto.ProfileUpdateData;
import com.anokix.distributorapp.session.SessionManager;

/**
 * Profile Settings — loads the account from GET api/common/base-info and saves edits via
 * POST api/common/profile/update (1-Settings-Profile.png).
 */
public class SettingsProfileActivity extends BaseSettingsFormActivity {

    private String companyName = "";
    private com.hbb20.CountryCodePicker ccpPhone;

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_settings_profile;
    }

    @Override
    protected void bindViews() {
        super.bindViews();
        ccpPhone = findViewById(R.id.ccpPhone);
        ccpPhone.registerCarrierNumberEditText(findViewById(R.id.etPhone));
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
        SessionManager session = SessionManager.get(this);
        // Show what we already know instantly, then refresh from the server.
        bindFields(session.getFirstName(), session.getLastName(), session.getEmail(), "");

        ApiClient.get(this).getBaseInfo(new ApiCallback<BaseInfoData>() {
            @Override
            public void onSuccess(BaseInfoData data) {
                if (data == null) return;
                if (data.portal_info != null) {
                    companyName = data.portal_info.company_legal_name != null
                            ? data.portal_info.company_legal_name : "";
                }
                if (data.user != null) {
                    bindFields(data.user.first_name, data.user.last_name,
                            data.user.email, data.user.phone_number);
                } else {
                    ((TextView) findViewById(R.id.profileCompanyName)).setText(companyName);
                }
            }

            @Override
            public void onError(String message) {
                // Keep the session-backed values already shown.
            }
        });
    }

    private void bindFields(String first, String last, String email, String phone) {
        first = first == null ? "" : first;
        last = last == null ? "" : last;
        String displayName = (first + " " + last).trim();

        ((TextView) findViewById(R.id.profileInitials))
                .setText(SettingsUiHelper.initialsOf(displayName, "?"));
        ((TextView) findViewById(R.id.profileDisplayName)).setText(displayName);
        ((TextView) findViewById(R.id.profileCompanyName)).setText(companyName);

        SettingsUiHelper.setText(findViewById(R.id.etFirstName), first);
        SettingsUiHelper.setText(findViewById(R.id.etLastName), last);
        SettingsUiHelper.setText(findViewById(R.id.etEmail), email == null ? "" : email);
        if (phone != null && !phone.trim().isEmpty()) {
            ccpPhone.setFullNumber(phone.trim());
        }
    }

    @Override
    protected void onSave() {
        final String firstName = SettingsUiHelper.textOf(findViewById(R.id.etFirstName));
        final String lastName = SettingsUiHelper.textOf(findViewById(R.id.etLastName));
        final String email = SettingsUiHelper.textOf(findViewById(R.id.etEmail));
        final String phone = SettingsUiHelper.textOf(findViewById(R.id.etPhone)).isEmpty()
                ? "" : ccpPhone.getFullNumberWithPlus();

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

        final View save = findViewById(R.id.saveButton);
        save.setEnabled(false);
        ApiClient.get(this).updateProfile(firstName, lastName, email, phone,
                new ApiCallback<ProfileUpdateData>() {
                    @Override
                    public void onSuccess(ProfileUpdateData data) {
                        SessionManager session = SessionManager.get(SettingsProfileActivity.this);
                        session.setUser(firstName, lastName, email, session.getRoleLabel());
                        toastSaved();
                        finish();
                    }

                    @Override
                    public void onError(String message) {
                        save.setEnabled(true);
                        Toast.makeText(SettingsProfileActivity.this,
                                message != null ? message : getString(R.string.error_generic),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
