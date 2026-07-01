package com.anokix.distributorapp.network.dto;

import java.util.List;

/**
 * Distributor Orders list/detail payload — GET {@code api/distributor/orders}.
 *
 * The list endpoint returns {@code orders[]} with a {@code products} string list,
 * a nested {@code trader} object, a {@code pagination} block and {@code status_counts}
 * that drive the status tabs. The detail endpoint (same {@code Order} type, fetched by
 * {@code id}) additionally populates {@code items[]}.
 */
public class OrdersData {
    public List<Order> orders;
    public Pagination pagination;
    public StatusCounts status_counts;

    public static class Order {
        public long id;
        public String order_number;
        public long trader_id;
        public long distributor_id;
        public String status;           // display e.g. "Pending"
        public String status_key;       // e.g. "pending"
        public double subtotal;
        public double total_amount;
        public int item_count;
        public int line_count;
        public String delivery_date;          // "yyyy-MM-dd"
        public String delivery_start_time;    // "HH:mm" / "HH:mm:ss"
        public String delivery_end_time;
        public String notes;
        public Trader trader;
        public List<String> products;   // list endpoint only
        public List<Item> items;        // detail endpoint only
        public String created_at;       // "dd/MM/yyyy HH:mm:ss"
        public String updated_at;

        public String traderName() {
            if (trader != null && trader.name != null && !trader.name.trim().isEmpty()) {
                return trader.name;
            }
            return "Trader";
        }

        public String statusKey() {
            if (status_key != null && !status_key.isEmpty()) return status_key;
            return status != null ? status.toLowerCase() : "pending";
        }
    }

    public static class Trader {
        public long id;
        public String name;
        public String phone;
        public String location;
        public String segment;
    }

    /**
     * Line item on the detail response ({@code orders/get-by-id}). The fields are flat —
     * {@code name} and {@code image_url} sit directly on the item, not under a nested product.
     */
    public static class Item {
        public long id;
        public long product_id;
        public String name;
        public int quantity;
        public double unit_price;
        public double line_total;
        public String status;
        public String image_url;
    }

    public static class Pagination {
        public int page;
        public int per_page;
        public int total;
        public int total_pages;
    }

    public static class StatusCounts {
        public int all;
        public int pending;
        public int accepted;
        public int picking;
        public int packing;
        public int ready_for_delivery;
        public int out_for_delivery;
        public int delivered;
        public int cancelled;
        public int failed;

        public int forKey(String key) {
            if (key == null) return 0;
            switch (key) {
                case "all": return all;
                case "pending": return pending;
                case "accepted": return accepted;
                case "picking": return picking;
                case "packing": return packing;
                case "ready_for_delivery": return ready_for_delivery;
                case "out_for_delivery": return out_for_delivery;
                case "delivered": return delivered;
                case "cancelled": return cancelled;
                case "failed": return failed;
                default: return 0;
            }
        }
    }
}
