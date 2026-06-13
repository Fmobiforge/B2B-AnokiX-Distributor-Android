package com.anokix.distributor.model;

public class ListItem {
    public final String title;
    public final String subtitle;
    public final String badge;
    public final String sku;
    public final int stockCount;
    public final int stockPercent;

    public ListItem(String title, String subtitle, String badge) {
        this.title = title;
        this.subtitle = subtitle;
        this.badge = badge;
        this.sku = "";
        this.stockCount = 0;
        this.stockPercent = 50;
    }

    public ListItem(String title, String sku, int stockCount, int stockPercent, String badge) {
        this.title = title;
        this.sku = sku;
        this.stockCount = stockCount;
        this.stockPercent = stockPercent;
        this.badge = badge;
        this.subtitle = sku + " · " + stockCount + " units";
    }
}
