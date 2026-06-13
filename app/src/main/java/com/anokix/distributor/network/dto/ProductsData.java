package com.anokix.distributor.network.dto;

import java.util.List;

/**
 * data block of GET /api/distributor/products.
 */
public class ProductsData {
    public List<Product> products;
    public int total_products;
    public int active_products;
    public int out_of_stock;
    public int low_stock;

    public static class Product {
        public String id;
        public String name;
        public String sku;
        public String barcode;
        public String selling_price;
        public String unit_cost;
        public String stock;
        public String status;
        public String tag_list;
        public String image_url;
        public Category category;
        public Brand brand;
    }

    public static class Category {
        public long id;
        public String title;
    }

    public static class Brand {
        public long id;
        public String name;
    }
}
