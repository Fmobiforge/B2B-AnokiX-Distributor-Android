package com.anokix.distributorapp.ui;

import androidx.annotation.Nullable;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * South-African money formatting for the VAS screens — space-grouped thousands and a
 * comma decimal, e.g. {@code R 5 050,00}, matching the web portal's "Order summary".
 */
public final class VasFormat {

    private static final Locale ZA = new Locale("en", "ZA");

    private VasFormat() {
    }

    public static String money(double value) {
        NumberFormat nf = NumberFormat.getInstance(ZA);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return "R " + nf.format(value);
    }

    public static String money(@Nullable String value) {
        return money(parse(value));
    }

    /**
     * Lenient number parse for API strings and free-text input that may be null, blank or
     * non-numeric. A lone comma is treated as the decimal separator, because the SA locale
     * writes "10,50" and the numeric keyboard offers whichever the device is set to.
     */
    public static double parse(@Nullable String value) {
        if (value == null) return 0;
        String raw = value.trim();
        if (raw.indexOf(',') >= 0 && raw.indexOf('.') < 0) {
            raw = raw.replace(',', '.');
        }
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    /**
     * 27821234567 / 0821234567 → "082 123 4567" when it resolves to a 10-digit local
     * number; anything else is handed back untouched.
     */
    public static String msisdn(@Nullable String raw) {
        if (raw == null) return "";
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.startsWith("27") && digits.length() == 11) {
            digits = "0" + digits.substring(2);
        }
        if (digits.length() == 10) {
            return digits.substring(0, 3) + " " + digits.substring(3, 6) + " " + digits.substring(6);
        }
        return raw;
    }

    /** Space-grouped byte count for the Dynamic services "= N bytes" hint. */
    public static String bytes(long value) {
        NumberFormat nf = NumberFormat.getInstance(ZA);
        nf.setMaximumFractionDigits(0);
        return nf.format(value);
    }
}
