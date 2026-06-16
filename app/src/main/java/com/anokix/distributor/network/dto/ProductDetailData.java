package com.anokix.distributor.network.dto;

import java.util.List;

/**
 * data block of the product endpoints that return a single record:
 * GET /api/distributor/products/get-by-id and the create/update responses,
 * all shaped as {@code data.product}.
 *
 * Field names mirror the create payload so the Create Product wizard can both
 * read a record back (for editing) and know what it submitted.
 */
public class ProductDetailData {
    public Product product;

    public static class Product {
        public String id;
        public String name;
        public String brand_id;
        public String parent_category_id;
        public String category_id;
        public String barcode;
        public String sku;
        public String description;

        public String unit_id;
        public String units_per_case;
        public String dim_type;
        public String measurements_type;   // server alias of dim_type (e.g. "kilo_centi")
        public String dim_weight;
        public String weight;
        public String dim_length;
        public String dim_width;
        public String dim_height;

        public String selling_price;
        public String cost_price;
        public String stock;
        public String stock_quantity;
        public String reorder_level;
        public String promotion_price;
        public String promotion_start_date;
        public String promotion_end_date;
        public String available_for_ordering;
        public String status;

        // File URLs returned by the server (prefixed with the app base_url).
        public String primary_product_image_url;
        public String image_url;
        public String product_datasheet_url;
        public String datasheet_url;
        public List<AdditionalImage> additional_images;
    }

    public static class AdditionalImage {
        public String id;
        public String file_url;
        public String original_name;
    }
}
