package com.anokix.distributorapp.network.dto;

import java.util.List;

/**
 * Response for GET {@code api/common/notifications} — the user's notification feed.
 * {@code notifications[]} plus the live {@code unread_count} and {@code pagination}.
 */
public class NotificationsData {

    public List<Notification> notifications;
    public int unread_count;
    public Pagination pagination;

    /** One notification. {@code type} drives the icon + which screen a tap opens. */
    public static class Notification {
        public String id;
        public String user_id;
        /** e.g. "order.status", "return.decision", "stock.low". */
        public String type;
        public String title;
        public String body;
        /** Routing payload (route + order_id/grn_id/product_id…). */
        public Payload data;
        public String read_at;
        /** "dd/MM/yyyy HH:mm:ss". */
        public String created_at;
        public boolean is_read;
    }

    /**
     * The {@code data} object — only the fields used for navigation and the "important"
     * flag are modelled. See {@code FCM-settings/3-Event-Catalogue.md} §3.1 for the
     * full field dictionary; unmodelled keys are ignored.
     */
    public static class Payload {
        /** "order" | "delivery" | "grv" | "return" | "invoice" | "finance" | "wallet"
         *  | "inventory" | "vas" | "promotion" | "product" | "marketplace" | "report"
         *  | "trader" | "settings" | "notifications". */
        public String route;
        public long order_id;
        public String order_number;
        public long grn_id;
        public String grn_number;
        public long grv_id;
        public String grv_number;
        public long delivery_id;
        public long invoice_id;
        public String invoice_number;
        public long product_id;
        public long promotion_id;
        public long trader_id;
        public long transaction_id;
        /** Order status — "Cancelled" or the machine form "cancelled"; drives {@code isImportant}. */
        public String status;
        /** Machine form of {@link #status} when the server sends both, e.g. "out_for_delivery". */
        public String status_key;
        public String decision;
    }

    public static class Pagination {
        public int page;
        public int per_page;
        public int total;
        public int total_pages;
    }
}
