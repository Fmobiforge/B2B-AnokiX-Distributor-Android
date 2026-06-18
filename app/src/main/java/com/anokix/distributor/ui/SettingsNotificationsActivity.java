package com.anokix.distributor.ui;

import com.anokix.distributor.R;
import com.anokix.distributor.model.NotificationPreference;
import com.anokix.distributor.ui.views.RobotoSwitchCompat;

import java.util.Arrays;
import java.util.List;

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
        setSwitchFromRepo(R.id.switchOrderAlerts, "order_alerts", true);
        setSwitchFromRepo(R.id.switchLowStock, "low_stock", true);
        setSwitchFromRepo(R.id.switchDeliveryUpdates, "delivery_updates", true);
        setSwitchFromRepo(R.id.switchFinanceReminders, "finance_reminders", true);
        setSwitchFromRepo(R.id.switchReportReady, "report_ready", false);
    }

    private void setSwitchFromRepo(int switchId, String key, boolean defaultValue) {
        RobotoSwitchCompat switchView = findViewById(switchId);
        for (NotificationPreference pref : repository.getNotificationPreferences()) {
            if (key.equals(pref.key)) {
                switchView.setChecked(pref.enabled);
                return;
            }
        }
        switchView.setChecked(defaultValue);
    }

    @Override
    protected void onSave() {
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
        RobotoSwitchCompat switchView = findViewById(switchId);
        return switchView != null && switchView.isChecked();
    }
}
