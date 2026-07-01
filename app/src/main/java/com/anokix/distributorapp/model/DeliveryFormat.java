package com.anokix.distributorapp.model;

import com.anokix.distributorapp.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Shared formatting helpers for the Deliveries list + detail screens — the computed
 * delivery {@code status} (delivered / delayed / in_transit / failed / returned) maps to
 * a badge label, text colour and background drawable, and {@code delivery_at} is rendered
 * for display.
 */
public final class DeliveryFormat {

    private DeliveryFormat() {}

    public static String label(String status) {
        switch (key(status)) {
            case "delivered":  return "Delivered";
            case "in_transit": return "In Transit";
            case "delayed":    return "Delayed";
            case "failed":     return "Failed";
            case "returned":   return "Returned";
            default:           return capitalize(status);
        }
    }

    /** R.color.* for the status badge text. */
    public static int colorRes(String status) {
        switch (key(status)) {
            case "delivered":  return R.color.success;
            case "in_transit": return R.color.info;
            case "delayed":    return R.color.warning;
            case "failed":     return R.color.danger;
            case "returned":   return R.color.purple_primary;
            default:           return R.color.info;
        }
    }

    /** R.drawable.* badge background for the status. */
    public static int badgeBg(String status) {
        switch (key(status)) {
            case "delivered":  return R.drawable.bg_badge_success;
            case "in_transit": return R.drawable.bg_badge_info;
            case "delayed":    return R.drawable.bg_badge_warning;
            case "failed":     return R.drawable.bg_badge_danger;
            case "returned":   return R.drawable.bg_badge_purple;
            default:           return R.drawable.bg_badge_info;
        }
    }

    /** "2026-06-25 22:00" → "25 Jun 2026, 10:00 PM". Falls back to the raw value. */
    public static String deliveryAt(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "";
        Date d = parse(raw.trim(), "yyyy-MM-dd HH:mm");
        if (d == null) d = parse(raw.trim(), "yyyy-MM-dd HH:mm:ss");
        if (d == null) return raw;
        return new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(d);
    }

    private static String key(String status) {
        return status == null ? "" : status.trim().toLowerCase(Locale.US);
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static Date parse(String raw, String pattern) {
        try {
            return new SimpleDateFormat(pattern, Locale.US).parse(raw);
        } catch (Exception e) {
            return null;
        }
    }
}
