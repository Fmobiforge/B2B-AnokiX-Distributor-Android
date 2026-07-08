package com.anokix.distributorapp.network.dto;

import java.util.List;

/** {@code data} block of {@code GET api/common/vas/products}. */
public class VasProductsData {
    public List<Product> products;
    public Pagination pagination;

    public static class Product {
        public String id;
        public String sku;
        public String name;
        public String description;
        public double price;
        public String brand;
        public int displayOrder;
        public boolean isAdHoc;
    }

    public static class Pagination {
        public int page;
        public int limit;
        public int total_pages;
        public int total_items;
        public boolean is_last;
    }
}
