package com.anokix.distributor.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributor.R;
import com.anokix.distributor.model.NotificationPreference;
import com.anokix.distributor.model.SettingsProfile;
import com.anokix.distributor.session.SessionManager;
import com.anokix.distributor.ui.adapter.SettingsMenuAdapter;
import com.anokix.distributor.ui.views.RobotoSwitchCompat;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends BaseSecondaryActivity {

    private final List<NotificationPreference> preferences = new ArrayList<>();
    private final List<RobotoSwitchCompat> switches = new ArrayList<>();

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
        RecyclerView menuList = findViewById(R.id.settingsMenuList);
        menuList.setLayoutManager(new LinearLayoutManager(this));
        SettingsMenuAdapter menuAdapter = new SettingsMenuAdapter();
        menuList.setAdapter(menuAdapter);
        menuAdapter.setItems(repository.getSettingsMenuEntries());

        findViewById(R.id.saveButton).setOnClickListener(v -> savePreferences());
    }

    @Override
    protected void loadData() {
        SettingsProfile profile = repository.getSettingsProfile();
        SessionManager session = SessionManager.get(this);

        String name = !session.getFullName().isEmpty() ? session.getFullName() : profile.name;
        String email = !session.getEmail().isEmpty() ? session.getEmail() : profile.email;
        ((TextView) findViewById(R.id.profileInitials)).setText(initialsOf(name, profile.initials));
        ((TextView) findViewById(R.id.profileName)).setText(name);
        ((TextView) findViewById(R.id.profileEmail)).setText(email);
        findViewById(R.id.verifiedBadge).setVisibility(profile.verified ? View.VISIBLE : View.GONE);

        preferences.clear();
        preferences.addAll(repository.getNotificationPreferences());
        buildPreferenceRows();
    }

    private void buildPreferenceRows() {
        LinearLayout container = findViewById(R.id.preferencesContainer);
        int childCount = container.getChildCount();
        for (int i = childCount - 1; i >= 1; i--) {
            container.removeViewAt(i);
        }
        switches.clear();

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < preferences.size(); i++) {
            NotificationPreference pref = preferences.get(i);
            View row = inflater.inflate(R.layout.item_settings_preference, container, false);
            ((TextView) row.findViewById(R.id.prefTitle)).setText(pref.title);
            ((TextView) row.findViewById(R.id.prefSubtitle)).setText(pref.subtitle);
            RobotoSwitchCompat switchView = row.findViewById(R.id.prefSwitch);
            switchView.setChecked(pref.enabled);
            switches.add(switchView);

            View divider = row.findViewById(R.id.prefDivider);
            divider.setVisibility(i < preferences.size() - 1 ? View.VISIBLE : View.GONE);

            container.addView(row);
        }
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

    private void savePreferences() {
        List<NotificationPreference> updated = new ArrayList<>();
        for (int i = 0; i < preferences.size(); i++) {
            NotificationPreference pref = preferences.get(i);
            updated.add(new NotificationPreference(pref.key, pref.title, pref.subtitle,
                    switches.get(i).isChecked()));
        }
        repository.saveNotificationPreferences(updated);
        preferences.clear();
        preferences.addAll(updated);
        Toast.makeText(this, R.string.save_changes, Toast.LENGTH_SHORT).show();
    }
}
