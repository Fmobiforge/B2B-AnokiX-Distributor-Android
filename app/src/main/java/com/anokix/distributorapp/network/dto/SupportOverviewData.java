package com.anokix.distributorapp.network.dto;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * {@code GET api/common/support/overview} — the whole Support Centre in one call: which
 * contact channels are switched on, the help-article catalogue, and the tickets this
 * account has raised.
 */
public class SupportOverviewData {

    public Channels channels;
    public List<Category> categories;
    public List<Article> popular;
    public List<Ticket> tickets;
    public int inbox_awaiting;

    /** Which ways of reaching support are switched on for this portal. */
    public static class Channels {
        public Channel tickets;
        public Channel callback;
        public Whatsapp whatsapp;
        public Channel live_chat;
        public Channel atlas;
        public Email email;
        public Phone phone;
        /** Free-text opening hours ("Mon–Fri 08:00–17:00"), or empty when not published. */
        public String hours;
    }

    public static class Channel {
        public boolean enabled;
    }

    public static class Whatsapp extends Channel {
        /** Pre-composed wa.me link with the greeting already in it. */
        public String link;
        /** The number as it should be shown ("+1 5674987536"). */
        public String display;
    }

    public static class Email extends Channel {
        public String address;
    }

    public static class Phone extends Channel {
        public String number;
    }

    /** A ticket topic. Empty for now, so the Create Ticket sheet hides its Topic field. */
    public static class Category {
        public String id;
        public String name;
        public String slug;
    }

    public static class Article {
        public String id;
        public String title;
        public String excerpt;
        public String url;
    }

    /** One support ticket. {@link #messages} is only populated by the detail call. */
    public static class Ticket {
        public String id;
        public String ticket_number;
        public String subject;
        public String distributor_name;
        public String category;
        public String channel;
        public String status;
        public String priority;
        public String callback_phone;
        public String created_at;
        public String last_reply_at;
        public String last_reply_by;
        public String resolved_at;
        public boolean is_open;
        public List<Message> messages;

        public String statusLabel() {
            if (status == null || status.trim().isEmpty()) {
                return is_open ? "Open" : "Closed";
            }
            String trimmed = status.trim();
            return trimmed.substring(0, 1).toUpperCase(Locale.getDefault()) + trimmed.substring(1);
        }

        /** "13 Aug, 20:39" — when the ticket was raised. */
        public String openedLabel() {
            Date when = parseCreated(created_at);
            return when == null ? safe(created_at) : format(when, "d MMM, HH:mm");
        }

        /** "13 Aug, 20:19" — the timestamp the list row shows on the right. */
        public String lastActivityLabel() {
            Date when = parseIso(last_reply_at);
            return when == null ? openedLabel() : format(when, "d MMM, HH:mm");
        }

        /**
         * The one-line state under the subject. An open ticket whose last word came from
         * us is waiting on support; if support replied last, the ball is in our court.
         */
        public boolean isWaitingForSupport() {
            return is_open && (last_reply_by == null || "merchant".equalsIgnoreCase(last_reply_by.trim()));
        }
    }

    /** One entry in a ticket's conversation. */
    public static class Message {
        public String id;
        public String author_type;
        public String author_name;
        public String body;
        public boolean is_internal;
        public String created_at;

        /** Our own messages sit on the right of the thread. */
        public boolean isMine() {
            return author_type == null || "merchant".equalsIgnoreCase(author_type.trim());
        }

        /** Status notes ("Closed by the merchant.") are centred, not attributed. */
        public boolean isSystem() {
            return author_type != null && "system".equalsIgnoreCase(author_type.trim());
        }

        /** "20:39" — the time in the bubble's corner. */
        public String timeLabel() {
            Date when = parseCreated(created_at);
            return when == null ? "" : format(when, "HH:mm");
        }
    }

    // ---- Date parsing ----------------------------------------------------
    //
    // The API mixes two shapes: created_at comes back as "13/08/2026 20:33:41" while
    // last_reply_at / resolved_at use "2026-08-13 20:33:41". Each has its own parser and
    // both fall back to the raw string rather than showing nothing.

    private static Date parseCreated(String raw) {
        Date exact = parse(raw, "dd/MM/yyyy HH:mm:ss");
        return exact != null ? exact : parseIso(raw);
    }

    private static Date parseIso(String raw) {
        Date exact = parse(raw, "yyyy-MM-dd HH:mm:ss");
        return exact != null ? exact : parse(raw, "yyyy-MM-dd");
    }

    private static Date parse(String raw, String pattern) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return new SimpleDateFormat(pattern, Locale.US).parse(raw.trim());
        } catch (ParseException e) {
            return null;
        }
    }

    private static String format(Date date, String pattern) {
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(date);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
