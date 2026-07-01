package com.anokix.distributorapp.network.dto;

import java.util.List;

/**
 * data block of GET /api/common/reference – the lookup lists used by the
 * Create Trader wizard: product categories, distributors, and trader types.
 */
public class ReferenceData {
    public List<ProductCategory> product_categories;
    public List<Distributor> distributors;
    public List<TraderType> trader_types;

    public static class ProductCategory {
        public long id;
        public String title;
        public String slug;
        public String description;
        public Long parent_id;
    }

    public static class Distributor {
        public long id;
        public String distributor_code;
        public String company_legal_name;
        public String trading_name;
        public String company_email;
        public String company_phone_number;
        public String address;

        /** Best label for a distributor in a picker. */
        public String displayName() {
            if (trading_name != null && !trading_name.trim().isEmpty()) {
                return trading_name;
            }
            return company_legal_name != null ? company_legal_name : distributor_code;
        }
    }

    public static class TraderType {
        public long id;
        public String name;
        public String slug;
        public String icon;
        public int sort_order;
    }
}
