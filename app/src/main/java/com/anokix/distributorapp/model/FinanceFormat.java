package com.anokix.distributorapp.model;

import com.anokix.distributorapp.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Shared formatting for the Finances ledger — maps a transaction {@code type}
 * (order_payment / invoice / payout / credit_note) and {@code status}
 * (paid / pending / overdue / partial) to display labels, badge colours and background drawables,
 * and normalises the {@code created_at} timestamp for the list.
 */
public final class FinanceFormat {

    // Filter categories mirror the web dropdown; used for both the Type filter and the row badge.
    public static final String TYPE_INVOICE = "invoice";
    public static final String TYPE_PAYMENT = "payment";
    public static final String TYPE_PAYOUT = "payout";
    public static final String TYPE_CREDIT_NOTE = "credit_note";

    private FinanceFormat() {}

    // ---- Type -----------------------------------------------------------

    /** Normalises the raw {@code type} to one of the four filter categories. */
    public static String typeCategory(String type) {
        String k = key(type);
        if (k.contains("invoice")) return TYPE_INVOICE;
        if (k.contains("payout")) return TYPE_PAYOUT;
        if (k.contains("credit")) return TYPE_CREDIT_NOTE;
        return TYPE_PAYMENT;   // order_payment, payment, ...
    }

    public static String typeLabel(String type) {
        switch (typeCategory(type)) {
            case TYPE_INVOICE:     return "Invoice";
            case TYPE_PAYOUT:      return "Payout";
            case TYPE_CREDIT_NOTE: return "Credit Note";
            default:               return "Payment";
        }
    }

    public static int typeBadgeBg(String type) {
        switch (typeCategory(type)) {
            case TYPE_INVOICE:     return R.drawable.bg_badge_info;
            case TYPE_PAYOUT:      return R.drawable.bg_badge_purple;
            case TYPE_CREDIT_NOTE: return R.drawable.bg_badge_warning;
            default:               return R.drawable.bg_badge_success;
        }
    }

    public static int typeColorRes(String type) {
        switch (typeCategory(type)) {
            case TYPE_INVOICE:     return R.color.info;
            case TYPE_PAYOUT:      return R.color.purple_primary;
            case TYPE_CREDIT_NOTE: return R.color.warning;
            default:               return R.color.success;
        }
    }

    // ---- Status ---------------------------------------------------------

    public static String statusLabel(String status) {
        switch (key(status)) {
            case "paid":     return "Paid";
            case "pending":  return "Pending";
            case "overdue":  return "Overdue";
            case "partial":  return "Partial";
            case "failed":   return "Failed";
            case "reversed": return "Reversed";
            default:         return key(status).isEmpty() ? "—" : capitalize(status);
        }
    }

    public static int statusColorRes(String status) {
        switch (key(status)) {
            case "paid":     return R.color.success;
            case "overdue":  return R.color.danger;
            case "failed":   return R.color.danger;
            case "partial":  return R.color.info;
            case "pending":  return R.color.warning;
            case "reversed": return R.color.text_secondary;
            default:         return R.color.text_secondary;
        }
    }

    public static int statusBadgeBg(String status) {
        switch (key(status)) {
            case "paid":     return R.drawable.bg_badge_success;
            case "overdue":  return R.drawable.bg_badge_danger;
            case "failed":   return R.drawable.bg_badge_danger;
            case "partial":  return R.drawable.bg_badge_info;
            case "pending":  return R.drawable.bg_badge_warning;
            case "reversed": return R.drawable.bg_badge_muted;
            default:         return R.drawable.bg_badge_muted;
        }
    }

    // ---- Date -----------------------------------------------------------

    /** "2026-07-15 13:58:09" → "15 Jul 2026". Falls back to the raw value. */
    public static String date(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "";
        String value = raw.trim();
        Date d = parse(value, "yyyy-MM-dd HH:mm:ss");
        if (d == null) d = parse(value, "yyyy-MM-dd HH:mm");
        if (d == null) d = parse(value, "dd/MM/yyyy HH:mm:ss");
        if (d == null) d = parse(value, "yyyy-MM-dd");
        if (d == null) return value;
        return new SimpleDateFormat("dd MMM yyyy", Locale.US).format(d);
    }

    private static String key(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.US);
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
