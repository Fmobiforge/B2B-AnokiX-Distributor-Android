package com.anokix.distributorapp.model;

/**
 * A product chosen for a promotion. Carries the catalogue fields shown in the
 * picker plus the editable {@code campaign_selling_price} used at submit time.
 */
public class CampaignProduct {
    public final String productId;
    public final String name;
    public final String sku;
    public final String category;
    public final String brand;
    public final String imageUrl;
    public final double originalPrice;
    public final int availableStock;
    public String sellingPrice;   // editable; defaults to the original price

    public CampaignProduct(String productId, String name, String sku, String category,
                           String brand, String imageUrl, double originalPrice, int availableStock) {
        this.productId = productId;
        this.name = name;
        this.sku = sku;
        this.category = category;
        this.brand = brand;
        this.imageUrl = imageUrl;
        this.originalPrice = originalPrice;
        this.availableStock = availableStock;
        this.sellingPrice = trimPrice(originalPrice);
    }

    /** A fresh copy (each selection is independent so prices can differ per deal). */
    public CampaignProduct copy() {
        CampaignProduct c = new CampaignProduct(productId, name, sku, category, brand,
                imageUrl, originalPrice, availableStock);
        c.sellingPrice = sellingPrice;
        return c;
    }

    public double sellingPriceValue() {
        try {
            return Double.parseDouble(sellingPrice.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private static String trimPrice(double v) {
        if (v == Math.floor(v)) {
            return String.valueOf((long) v);
        }
        return String.valueOf(v);
    }
}
