package com.anokix.distributorapp.ui;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import com.anokix.distributorapp.R;

import java.util.Locale;

/**
 * Maps Limes VAS leaf categories to local icons, keyed off the category id/name (the API
 * carries no artwork). Mirrors the web portal's per-category glyphs (phone, wifi, chat…).
 */
public final class VasIcons {

    private VasIcons() {
    }

    @DrawableRes
    public static int iconFor(@Nullable String id, @Nullable String name) {
        String key = ((id == null ? "" : id) + " " + (name == null ? "" : name)).toLowerCase(Locale.US);

        if (key.contains("whatsapp") || key.contains("sms") || key.contains("message")) {
            return R.drawable.ic_chat;
        }
        if (key.contains("data") || key.contains("fwa") || key.contains("wifi")) {
            return R.drawable.ic_wifi;
        }
        if (key.contains("voice") || key.contains("airtime") || key.contains("call")) {
            return R.drawable.ic_phone;
        }
        if (key.contains("service") || key.contains("fee") || key.contains("wallet")) {
            return R.drawable.ic_wallet;
        }
        if (key.contains("combo") || key.contains("bundle") || key.contains("subscription")
                || key.contains("package") || key.contains("gsm") || key.contains("flte")
                || key.contains("supplementary") || key.contains("choose") || key.contains("recharge")
                || key.contains("convert") || key.contains("day") || key.contains("week")) {
            return R.drawable.ic_products;
        }
        return R.drawable.ic_vas;
    }
}
