package com.anokix.distributor.ui;

import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import com.anokix.distributor.R;
import com.anokix.distributor.model.SettingsProfile;
import com.anokix.distributor.network.ApiCallback;
import com.anokix.distributor.network.ApiClient;
import com.anokix.distributor.network.dto.BaseInfoData;
import com.anokix.distributor.session.SessionManager;

public class SettingsActivity extends BaseSecondaryActivity {

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_settings;
    }

    @Override
    protected String getScreenTitle() {
        return getString(R.string.settings);
    }

    @Override
    protected void bindViews() {
        findViewById(R.id.menuProfile).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsProfileActivity.class)));
        findViewById(R.id.menuBusiness).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsBusinessActivity.class)));
        findViewById(R.id.menuNotifications).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsNotificationsActivity.class)));
        findViewById(R.id.menuSecurity).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsSecurityActivity.class)));
        findViewById(R.id.menuPreferences).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsPreferencesActivity.class)));
    }

    @Override
    protected void loadData() {
        bindProfileHeader();
        // Refresh the header from the live account (base data).
        ApiClient.get(this).getBaseInfo(new ApiCallback<BaseInfoData>() {
            @Override
            public void onSuccess(BaseInfoData data) {
                if (data == null || data.user == null) return;
                String name = ((safe(data.user.first_name) + " " + safe(data.user.last_name))).trim();
                String email = safe(data.user.email);
                if (!name.isEmpty()) {
                    ((TextView) findViewById(R.id.profileName)).setText(name);
                    ((TextView) findViewById(R.id.profileInitials)).setText(initialsOf(name, "?"));
                }
                if (!email.isEmpty()) {
                    ((TextView) findViewById(R.id.profileEmail)).setText(email);
                }
            }

            @Override
            public void onError(String message) {
                // Header keeps the session-backed values.
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindProfileHeader();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private void bindProfileHeader() {
        SettingsProfile profile = repository.getSettingsProfile();
        SessionManager session = SessionManager.get(this);

        String name = !session.getFullName().isEmpty() ? session.getFullName() : profile.name;
        String email = !session.getEmail().isEmpty() ? session.getEmail() : profile.email;
        ((TextView) findViewById(R.id.profileInitials)).setText(initialsOf(name, profile.initials));
        ((TextView) findViewById(R.id.profileName)).setText(name);
        ((TextView) findViewById(R.id.profileEmail)).setText(email);
        findViewById(R.id.verifiedBadge).setVisibility(profile.verified ? View.VISIBLE : View.GONE);
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
}
