package com.anokix.distributor.network.dto;

import java.util.List;

/**
 * data block of GET {@code api/distributor/grns} (Returns). Holds the return list plus a
 * {@code summary} block (KPI stats), {@code status_counts} that drive the status tabs and a
 * {@code pagination} block.
 *
 * Each {@link Return} carries a {@code status} / {@code raw_status} (pending / approved /
 * rejected) that drives the tabs + badge and whether the row is actionable.
 */
public class ReturnsData {
    public List<Return> returns;
    public Summary summary;
    public StatusCounts status_counts;
    public Pagination pagination;

    public static class Return {
        public long id;
        public String grn_number;
        public Long source_grv_id;
        public long trader_id;
        public String trader_name;
        public String trader_phone;
        public String reason;            // damaged / expired / ...
        public String reason_label;      // "Damaged Items"
        public String status;            // pending / approved / rejected
        public String raw_status;
        public int item_count;
        public int total_quantity;
        public String note;
        public String decision_note;
        public String credit_note_number;
        public double credit_note_total;
        public String approved_at;       // "yyyy-MM-dd HH:mm:ss"
        public String created_at;        // "dd/MM/yyyy HH:mm:ss"

        public String traderName() {
            return trader_name == null || trader_name.trim().isEmpty() ? "Trader" : trader_name;
        }
    }

    public static class Summary {
        public int total_returns;
        public int pending;
        public int approved;
        public int rejected;
        public double total_value;
    }

    public static class StatusCounts {
        public int all;
        public int pending;
        public int approved;
        public int rejected;

        public int forKey(String key) {
            if (key == null) return 0;
            switch (key) {
                case "all": return all;
                case "pending": return pending;
                case "approved": return approved;
                case "rejected": return rejected;
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
