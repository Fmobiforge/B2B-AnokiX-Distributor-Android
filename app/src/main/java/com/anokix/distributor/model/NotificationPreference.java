package com.anokix.distributor.model;

public class NotificationPreference {
    public final String key;
    public final String title;
    public final String subtitle;
    public final boolean enabled;

    public NotificationPreference(String key, String title, String subtitle, boolean enabled) {
        this.key = key;
        this.title = title;
        this.subtitle = subtitle;
        this.enabled = enabled;
    }
}
