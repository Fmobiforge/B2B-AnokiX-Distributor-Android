package com.anokix.distributorapp.network.dto;

import java.util.List;

/**
 * data block of GET {@code api/distributor/deliveries}. Holds the delivery list plus a
 * {@code summary} block (5 KPI stats), {@code status_counts} that drive the status tabs,
 * the distinct {@code areas} for the Area filter, and a {@code pagination} block.
 *
 * Each {@link Delivery} carries both a computed {@code status} (delivered / delayed /
 * in_transit / failed / returned — used for the tabs + badge) and a {@code raw_status}
 * (ReadyForDelivery / OutForDelivery / Delivered — used to decide the row action).
 */
public class DeliveriesData {
    public List<Delivery> deliveries;
    public Summary summary;
    public StatusCounts status_counts;
    public List<String> areas;
    public Pagination pagination;

    public static class Delivery {
        public long id;
        public String delivery_number;
        public String order_number;
        public long trader_id;
        public String trader_name;
        public String trader_phone;
        public String area;
        public String driver;
        public String raw_status;       // ReadyForDelivery / OutForDelivery / Delivered ...
        public String delivery_date;    // "yyyy-MM-dd"
        public String delivery_at;      // "yyyy-MM-dd HH:mm"
        public String updated_at;
        public int item_count;
        public double total_amount;
        public String status;           // delivered / delayed / in_transit / failed / returned
        public boolean on_time;

        public String traderName() {
            return trader_name == null || trader_name.trim().isEmpty() ? "Trader" : trader_name;
        }
    }

    public static class Summary {
        public int total_deliveries;
        public int on_time;
        public double on_time_pct;
        public int in_transit;
        public double in_transit_pct;
        public int delayed;
        public double delayed_pct;
        public int failed;
        public double failed_pct;
        public int delivered;
    }

    public static class StatusCounts {
        public int all;
        public int in_transit;
        public int delivered;
        public int delayed;
        public int failed;
        public int returned;

        public int forKey(String key) {
            if (key == null) return 0;
            switch (key) {
                case "all": return all;
                case "in_transit": return in_transit;
                case "delivered": return delivered;
                case "delayed": return delayed;
                case "failed": return failed;
                case "returned": return returned;
                default: return 0;
            }
        }
    }

    public static class Pagination {
        public int page;
        public int per_page;
        public int total;
        public int total_pages;
    }
}
