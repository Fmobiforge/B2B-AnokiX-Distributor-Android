package com.anokix.distributorapp.model;

public class TraderItem {
    public final String id;
    public final String name;
    public final String traderCode;
    public final String location;
    public final String phone;
    public final String segment;
    public final String area;
    public final String initials;
    public final int avatarColorRes;
    public final boolean isNew;

    public TraderItem(String id, String name, String traderCode, String location, String phone, String segment,
                      String area, String initials, int avatarColorRes, boolean isNew) {
        this.id = id;
        this.name = name;
        this.traderCode = traderCode;
        this.location = location;
        this.phone = phone;
        this.segment = segment;
        this.area = area;
        this.initials = initials;
        this.avatarColorRes = avatarColorRes;
        this.isNew = isNew;
    }
}
