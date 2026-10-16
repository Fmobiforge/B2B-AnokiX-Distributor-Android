package com.anokix.distributorapp.model;

import android.graphics.Color;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Shared formatting helpers for the Orders list + detail screens
 * (status colours/labels, currency, and the API date formats).
 */
public final class OrderFormat {

    private OrderFormat() {}

    /** Brand colour for an order status key. */
    public static int statusColor(String key) {
        if (key == null) return Color.parseColor("#5C6B7A");   // text_secondary
        switch (key) {
            case "pending":            return Color.parseColor("#F57F17");   // stat_expiring
            case "accepted":           return Color.parseColor("#0D47A1");   // purple_primary (brand blue)
            case "picking":            return Color.parseColor("#1565C0");   // info
            case "packing":            return Color.parseColor("#0891B2");
            case "ready_for_delivery": return Color.parseColor("#0D9488");
            case "out_for_delivery":   return Color.parseColor("#E65100");   // warning
            case "delivered":          return Color.parseColor("#2E7D32");   // success
            case "cancelled":          return Color.parseColor("#C62828");   // danger
            case "failed":             return Color.parseColor("#B91C1C");
            default:                   return Color.parseColor("#5C6B7A");   // text_secondary
        }
    }

    /** "out_for_delivery" → "Out For Delivery". */
    public static String humanize(String key) {
        if (key == null || key.isEmpty()) return "";
        String[] parts = key.replace('_', ' ').split(" ");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }

    public static String money(double value, String currency) {
        String symbol = currency != null && !currency.isEmpty() ? currency : "R";
        return symbol + String.format(Locale.US, "%,.2f", value);
    }

    /** "24/06/2026 21:13:56" → "24 Jun 2026, 21:13". Falls back to the raw value. */
    public static String createdAt(String raw) {
        Date d = parse(raw, "dd/MM/yyyy HH:mm:ss");
        if (d == null) return raw == null ? "" : raw;
        return new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(d);
    }

    /** "2026-06-26" → "26 Jun 2026". Falls back to the raw value. */
    public static String deliveryDate(String raw) {
        Date d = parse(raw, "yyyy-MM-dd");
        if (d == null) return raw == null ? "" : raw;
        return new SimpleDateFormat("dd MMM yyyy", Locale.US).format(d);
    }

    /** "08:00:00" → "08:00". */
    public static String trimSeconds(String t) {
        if (t == null) return "";
        return t.length() >= 5 ? t.substring(0, 5) : t;
    }

    private static Date parse(String raw, String pattern) {
        if (raw == null || raw.trim().isEmpty()) return null;
        try {
            return new SimpleDateFormat(pattern, Locale.US).parse(raw.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
