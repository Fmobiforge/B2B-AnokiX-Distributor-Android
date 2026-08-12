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

    // Extra list metadata — powers the category / brand / status filters and the card.
    public String brand = "";
    public String brandId = "";
    public String categoryId = "";
    public String parentCategoryId = "";
    public String parentCategory = "";
    /** Product record status from the API ("Active" / "Inactive"). */
    public String status = "";
    public String barcode = "";
    public int stockQty;
    public int reorderLevel;

    public ProductItem(String id, String name, String sku, String category,
                       String price, String stockStatus) {
        this.id = id;
        this.name = name;
        this.sku = sku;
        this.category = category;
        this.price = price;
        this.stockStatus = stockStatus;
    }

    public boolean isActive() {
        return status == null || status.isEmpty() || "Active".equalsIgnoreCase(status);
    }

    public boolean isOutOfStock() {
        return stockQty <= 0;
    }

    /** In stock but at or below the reorder level the distributor configured. */
    public boolean isLowStock() {
        return stockQty > 0 && reorderLevel > 0 && stockQty <= reorderLevel;
    }
}
