package com.anokix.distributor.model;

public class ReportTypeItem {
    public final String id;
    public final String title;
    public final String description;
    public final int iconRes;

    public ReportTypeItem(String id, String title, String description, int iconRes) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.iconRes = iconRes;
    }
}
