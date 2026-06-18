package com.anokix.distributor.ui;

import android.widget.Toast;

import com.anokix.distributor.R;

/**
 * Shared footer actions for settings detail screens.
 */
abstract class BaseSettingsFormActivity extends BaseSecondaryActivity {

    @Override
    protected void bindViews() {
        findViewById(R.id.cancelButton).setOnClickListener(v -> finish());
        findViewById(R.id.saveButton).setOnClickListener(v -> onSave());
    }

    protected abstract void onSave();

    protected void toast(int resId) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show();
    }

    protected void toastSaved() {
        Toast.makeText(this, R.string.settings_saved_msg, Toast.LENGTH_SHORT).show();
    }
}
