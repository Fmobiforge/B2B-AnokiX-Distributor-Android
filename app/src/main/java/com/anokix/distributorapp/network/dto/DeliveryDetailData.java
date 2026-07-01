package com.anokix.distributorapp.network.dto;

import java.util.List;

/**
 * Detail wrapper for {@code api/distributor/deliveries/get-by-id} (and the same shape
 * returned by the dispatch / deliver actions) — the delivery with its {@code items[]}
 * and nested {@code trader}.
 */
public class DeliveryDetailData {
    public Delivery delivery;

    public static class Delivery {
        public long id;
        public String order_number;
        public String delivery_number;
        public long trader_id;
        public long distributor_id;
        public String status;            // raw e.g. "Delivered" / "ReadyForDelivery" / "OutForDelivery"
        public String status_key;        // "delivered"
        public String delivery_status;   // "delivered" / "delayed" ...
        public double subtotal;
        public double total_amount;
        public int item_count;
        public int line_count;
        public String delivery_date;     // "yyyy-MM-dd"
        public String delivery_start_time;
        public String delivery_end_time;
        public String delivery_at;       // "yyyy-MM-dd HH:mm"
        public String area;
        public String notes;
        public Trader trader;
        public List<Item> items;
        public List<String> products;
        public String created_at;
        public String updated_at;

        public String traderName() {
            if (trader != null && trader.name != null && !trader.name.trim().isEmpty()) {
                return trader.name;
            }
            return "Trader";
        }
    }

    public static class Trader {
        public long id;
        public String name;
        public String phone;
        public String location;
        public String segment;
    }

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
}
