package com.anokix.distributorapp.model;

public class MenuItem {
    public final String key;
    public final String title;
    public final int iconRes;
    /** Optional second line under the title ("Powered by IMB"); null hides it. */
    public final String subtitle;

    public MenuItem(String key, String title, int iconRes) {
        this(key, title, iconRes, null);
    }

    public MenuItem(String key, String title, int iconRes, String subtitle) {
        this.key = key;
        this.title = title;
        this.iconRes = iconRes;
        this.subtitle = subtitle;
    }
}
