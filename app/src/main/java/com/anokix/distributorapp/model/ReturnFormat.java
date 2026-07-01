package com.anokix.distributorapp.model;

import com.anokix.distributorapp.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Shared formatting helpers for the Returns list + detail screens — the return {@code status}
 * (pending / approved / rejected) maps to a badge label, text colour and background drawable,
 * and the {@code created_at} / {@code approved_at} timestamps are rendered for display.
 */
public final class ReturnFormat {

    private ReturnFormat() {}

    public static String label(String status) {
        switch (key(status)) {
            case "pending":  return "Pending";
            case "approved": return "Approved";
            case "rejected": return "Rejected";
            default:         return capitalize(status);
        }
    }

    /** R.color.* for the status badge text. */
    public static int colorRes(String status) {
        switch (key(status)) {
            case "approved": return R.color.success;
            case "rejected": return R.color.danger;
            case "pending":  return R.color.warning;
            default:         return R.color.warning;
        }
    }

    /** R.drawable.* badge background for the status. */
    public static int badgeBg(String status) {
        switch (key(status)) {
            case "approved": return R.drawable.bg_badge_success;
            case "rejected": return R.drawable.bg_badge_danger;
            case "pending":  return R.drawable.bg_badge_warning;
            default:         return R.drawable.bg_badge_warning;
        }
    }

    /** "25/06/2026 18:51:50" or "2026-06-25 18:51:52" → "25 Jun 2026". Falls back to the raw value. */
    public static String date(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "";
        String value = raw.trim();
        Date d = parse(value, "dd/MM/yyyy HH:mm:ss");
        if (d == null) d = parse(value, "yyyy-MM-dd HH:mm:ss");
        if (d == null) d = parse(value, "yyyy-MM-dd HH:mm");
        if (d == null) d = parse(value, "dd/MM/yyyy");
        if (d == null) return value;
        return new SimpleDateFormat("dd MMM yyyy", Locale.US).format(d);
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
