package com.anokix.distributor.model;

public class NotificationItem {
    public final String id;
    public final String title;
    public final String message;
    public final String relativeTime;
    public final String type;
    public final int iconRes;
    public final int iconBgRes;
    public final int iconTintRes;
    public final String actionLabel;
    public final boolean unread;
    public final boolean important;

    public NotificationItem(String id, String title, String message, String relativeTime,
                            String type, int iconRes, int iconBgRes, int iconTintRes,
                            String actionLabel, boolean unread, boolean important) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.relativeTime = relativeTime;
        this.type = type;
        this.iconRes = iconRes;
        this.iconBgRes = iconBgRes;
        this.iconTintRes = iconTintRes;
        this.actionLabel = actionLabel;
        this.unread = unread;
        this.important = important;
    }
}
