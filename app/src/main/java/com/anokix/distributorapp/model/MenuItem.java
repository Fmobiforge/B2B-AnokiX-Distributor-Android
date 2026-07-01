package com.anokix.distributorapp.model;

public class MenuItem {
    public final String key;
    public final String title;
    public final int iconRes;

    public MenuItem(String key, String title, int iconRes) {
        this.key = key;
        this.title = title;
        this.iconRes = iconRes;
    }
}
