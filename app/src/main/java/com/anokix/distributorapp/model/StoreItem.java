package com.anokix.distributorapp.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StoreItem {
    public final String id;
    public final String name;
    public final String address;
    public final int followerCount;
    public final int reviewCount;
    public final String status;
    public final String area;
    public final List<String> tags;

    public StoreItem(String id, String name, String address, int followerCount, int reviewCount,
                     String status, String area) {
        this(id, name, address, followerCount, reviewCount, status, area, null);
    }

    public StoreItem(String id, String name, String address, int followerCount, int reviewCount,
                     String status, String area, List<String> tags) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.followerCount = followerCount;
        this.reviewCount = reviewCount;
        this.status = status;
        this.area = area;
        this.tags = tags == null ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<>(tags));
    }
}
