package com.anokix.distributor.model;

public class SettingsMenuEntry {
    public final String key;
    public final String title;
    public final String subtitle;
    public final int iconRes;

    public SettingsMenuEntry(String key, String title, String subtitle, int iconRes) {
        this.key = key;
        this.title = title;
        this.subtitle = subtitle;
        this.iconRes = iconRes;
    }
}
