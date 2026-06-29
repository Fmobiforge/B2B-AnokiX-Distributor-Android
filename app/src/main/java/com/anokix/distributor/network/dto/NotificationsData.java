package com.anokix.distributor.network.dto;

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

    /** The {@code data} object — only the fields used for navigation are modelled. */
    public static class Payload {
        public String route;        // "order" | "return" | "inventory" | "delivery" | …
        public long order_id;
        public String order_number;
        public long grn_id;
        public String grn_number;
        public long delivery_id;
        public long product_id;
        public String status;
        public String decision;
    }

    public static class Pagination {
        public int page;
        public int per_page;
        public int total;
        public int total_pages;
    }
}
