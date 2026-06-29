package com.anokix.distributor.ui;

import android.view.View;
import android.widget.Toast;

import com.anokix.distributor.R;
import com.anokix.distributor.network.ApiCallback;
import com.anokix.distributor.network.ApiClient;
import com.anokix.distributor.network.dto.PreferencesData;
import com.anokix.distributor.ui.views.RobotoSwitchCompat;

/**
 * Notification Preferences — the 5 toggles, loaded from GET api/common/preferences and saved
 * via POST api/common/preferences/update (3-Settings-Notifications.png). When the server has
 * no notification settings yet, every toggle defaults to on.
 */
public class SettingsNotificationsActivity extends BaseSettingsFormActivity {

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_settings_notifications;
    }

    @Override
    protected String getScreenTitle() {
        return getString(R.string.notification_preferences);
    }

    @Override
    protected String getScreenSubtitle() {
        return getString(R.string.settings_notifications_sub);
    }

    @Override
    protected void loadData() {
        ApiClient.get(this).getPreferences(new ApiCallback<PreferencesData>() {
            @Override
            public void onSuccess(PreferencesData data) {
                PreferencesData.Preferences p = data == null ? null : data.preferences;
                setChecked(R.id.switchOrderAlerts, p == null || p.notif("orderAlerts"));
                setChecked(R.id.switchLowStock, p == null || p.notif("lowStockAlerts"));
                setChecked(R.id.switchDeliveryUpdates, p == null || p.notif("deliveryUpdates"));
                setChecked(R.id.switchFinanceReminders, p == null || p.notif("financeReminders"));
                setChecked(R.id.switchReportReady, p == null || p.notif("reportReady"));
            }

            @Override
            public void onError(String message) {
                // Default everything on (matches "no settings yet").
                setChecked(R.id.switchOrderAlerts, true);
                setChecked(R.id.switchLowStock, true);
                setChecked(R.id.switchDeliveryUpdates, true);
                setChecked(R.id.switchFinanceReminders, true);
                setChecked(R.id.switchReportReady, true);
            }
        });
    }

    private void setChecked(int switchId, boolean checked) {
        RobotoSwitchCompat switchView = findViewById(switchId);
        if (switchView != null) switchView.setChecked(checked);
    }

    private boolean isChecked(int switchId) {
        RobotoSwitchCompat switchView = findViewById(switchId);
        return switchView != null && switchView.isChecked();
    }

    @Override
    protected void onSave() {
        final View save = findViewById(R.id.saveButton);
        save.setEnabled(false);
        ApiClient.get(this).updateNotificationPrefs(
                isChecked(R.id.switchOrderAlerts),
                isChecked(R.id.switchLowStock),
                isChecked(R.id.switchDeliveryUpdates),
                isChecked(R.id.switchFinanceReminders),
                isChecked(R.id.switchReportReady),
                new ApiCallback<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        toastSaved();
                        finish();
                    }

                    @Override
                    public void onError(String message) {
                        save.setEnabled(true);
                        Toast.makeText(SettingsNotificationsActivity.this,
                                message != null ? message : getString(R.string.error_generic),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
