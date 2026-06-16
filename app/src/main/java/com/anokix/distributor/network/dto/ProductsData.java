package com.anokix.distributor.network.dto;

import java.util.List;

/**
 * data block of GET /api/distributor/products. The list returns flat product
 * rows (category / brand / unit are denormalised onto each record as
 * {@code *_title} / {@code *_name} fields rather than nested objects).
 */
public class ProductsData {
    public List<Product> products;
    public int total;

    public static class Product {
        public String id;
        public String name;
        public String sku;
        public String barcode;
        public String selling_price;
        public String cost_price;
        public String stock;            // numeric in JSON; Gson reads it as a string
        public String stock_quantity;
        public String reorder_level;
        public String status;

        // Denormalised lookups
        public String brand_name;
        public String category_title;
        public String parent_category_title;
        public String unit_name;

        // File URLs (already prefixed with the app base_url by the server)
        public String image_url;
        public String primary_product_image_url;

        /** Best available stock figure as a string. */
        public String stockValue() {
            return stock_quantity != null ? stock_quantity : stock;
        }

        /** Best available image URL for the list thumbnail. */
        public String imageUrl() {
            return image_url != null ? image_url : primary_product_image_url;
        }
    }
}
