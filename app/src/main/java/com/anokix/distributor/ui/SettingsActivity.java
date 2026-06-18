package com.anokix.distributor.ui;

import android.view.View;
import android.widget.TextView;

import com.anokix.distributor.R;
import com.anokix.distributor.model.SettingsProfile;
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
                openSection(SettingsFormActivity.SECTION_PROFILE));
        findViewById(R.id.menuBusiness).setOnClickListener(v ->
                openSection(SettingsFormActivity.SECTION_BUSINESS));
        findViewById(R.id.menuNotifications).setOnClickListener(v ->
                openSection(SettingsFormActivity.SECTION_NOTIFICATIONS));
        findViewById(R.id.menuSecurity).setOnClickListener(v ->
                openSection(SettingsFormActivity.SECTION_SECURITY));
        findViewById(R.id.menuPreferences).setOnClickListener(v ->
                openSection(SettingsFormActivity.SECTION_PREFERENCES));
    }

    @Override
    protected void loadData() {
        bindProfileHeader();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindProfileHeader();
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

    private void openSection(String section) {
        startActivity(SettingsFormActivity.intent(this, section));
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
