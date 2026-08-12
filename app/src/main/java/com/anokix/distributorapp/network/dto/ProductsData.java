package com.anokix.distributorapp.network.dto;

import java.util.List;

/**
 * data block of GET /api/distributor/products. The list returns flat product
 * rows (category / brand / unit are denormalised onto each record as
 * {@code *_title} / {@code *_name} fields rather than nested objects) plus a
 * {@code summary} block that feeds the KPI cards on the Products screen.
 */
public class ProductsData {
    public List<Product> products;
    public Summary summary;
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
        public String updated_at;

        // Lookup ids (used by the category / brand filters)
        public String brand_id;
        public String category_id;
        public String parent_category_id;

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

    /** KPI counters shown above the list (total / active / out of stock / low stock). */
    public static class Summary {
        public Metric total_products;
        public Metric active_products;
        public Metric out_of_stock;
        public Metric low_stock;

        public static class Metric {
            public int value;
            public double change_percent;
            public String trend;
            public String compare_label;
            public double percent_of_total;
        }
    }
}
