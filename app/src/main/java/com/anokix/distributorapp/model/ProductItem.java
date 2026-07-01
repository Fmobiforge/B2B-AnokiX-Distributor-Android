package com.anokix.distributorapp.model;

public class ProductItem {
    public final String id;
    public final String name;
    public final String sku;
    public final String category;
    public final String price;
    public final String stockStatus;
    /** Remote image URL; null/empty falls back to the placeholder icon. */
    public String imageUrl;

    public ProductItem(String id, String name, String sku, String category,
                       String price, String stockStatus) {
        this.id = id;
        this.name = name;
        this.sku = sku;
        this.category = category;
        this.price = price;
        this.stockStatus = stockStatus;
    }
}
