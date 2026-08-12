package com.anokix.distributorapp.network.dto;

import java.util.List;

/**
 * data block of GET /api/common/pos/payments — every payment pushed to a card machine,
 * newest first, with its pagination envelope.
 */
public class PosPaymentsData {
    public List<Payment> payments;
    public Pagination pagination;

    public static class Payment {
        public long id;
        public String merchant_order_no;
        public String terminal_sn;
        public double amount;
        public double tip_amount;
        public String currency;
        /** "paid" / "failed" / "pending" / … */
        public String status;
        public String trans_status;
        public String trans_no;
        public String card_no;
        public String auth_code;
        public String entry_mode;
        public String pay_scenario;
        public String description;
        public String source;
        public String last_error;
        public String paid_at;
        public String created_at;
        public boolean is_open;
        public boolean can_close;
    }

    public static class Pagination {
        public int page;
        public int per_page;
        public int total;
        public int total_pages;
    }
}
