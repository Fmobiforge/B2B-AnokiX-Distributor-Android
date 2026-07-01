package com.anokix.distributorapp.model;

public class StoreItem {
    public final String id;
    public final String name;
    public final String address;
    public final int followerCount;
    public final int reviewCount;
    public final String status;
    public final String area;

    public StoreItem(String id, String name, String address, int followerCount, int reviewCount,
                     String status, String area) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.followerCount = followerCount;
        this.reviewCount = reviewCount;
        this.status = status;
        this.area = area;
    }
}
