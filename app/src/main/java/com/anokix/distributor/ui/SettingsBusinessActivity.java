package com.anokix.distributor.ui;

import android.text.TextUtils;

import com.anokix.distributor.R;
import com.anokix.distributor.model.BusinessSettings;

public class SettingsBusinessActivity extends BaseSettingsFormActivity {

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_settings_business;
    }

    @Override
    protected String getScreenTitle() {
        return getString(R.string.settings_business_title);
    }

    @Override
    protected String getScreenSubtitle() {
        return getString(R.string.settings_business_sub);
    }

    @Override
    protected void loadData() {
        BusinessSettings data = repository.getBusinessSettings();
        SettingsUiHelper.setText(findViewById(R.id.etBusinessName), data.businessName);
        SettingsUiHelper.setText(findViewById(R.id.etRegistrationNumber), data.registrationNumber);
        SettingsUiHelper.setText(findViewById(R.id.etVatNumber), data.vatNumber);
        SettingsUiHelper.setText(findViewById(R.id.etAddress), data.address);
        SettingsUiHelper.setText(findViewById(R.id.etCity), data.city);
    }

    @Override
    protected void onSave() {
        String businessName = SettingsUiHelper.textOf(findViewById(R.id.etBusinessName));
        if (TextUtils.isEmpty(businessName)) {
            toast(R.string.err_business_name);
            return;
        }

        BusinessSettings updated = new BusinessSettings(
                businessName,
                SettingsUiHelper.textOf(findViewById(R.id.etRegistrationNumber)),
                SettingsUiHelper.textOf(findViewById(R.id.etVatNumber)),
                SettingsUiHelper.textOf(findViewById(R.id.etAddress)),
                SettingsUiHelper.textOf(findViewById(R.id.etCity))
        );
        repository.saveBusinessSettings(updated);
        toastSaved();
        finish();
    }
}
