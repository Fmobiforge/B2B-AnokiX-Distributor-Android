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
        double d = 0;
        if (value != null) {
            try {
                d = Double.parseDouble(value.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return money(d);
    }
}
