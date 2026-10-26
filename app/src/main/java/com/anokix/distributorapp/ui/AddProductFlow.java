package com.anokix.distributorapp.ui;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.anokix.distributorapp.network.ApiClient;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Shared bits of the Add Product flow — method chooser, barcode lookup and the
 * Hirt &amp; Carter library — which mirrors the portal's /products/create page.
 *
 * Every screen of the flow is opened for a result: {@link Activity#RESULT_OK} means a product
 * was saved and each screen passes it up and closes, so the Products list refreshes once.
 */
final class AddProductFlow {

    /** "Not this product" on the Hirt &amp; Carter product screen. */
    static final int RESULT_REJECTED = Activity.RESULT_FIRST_USER + 1;
    /** An explicit Cancel that sends the user back to an empty barcode field. */
    static final int RESULT_CLEAR_BARCODE = Activity.RESULT_FIRST_USER + 2;

    static final String EXTRA_BARCODE = "barcode";
    static final String EXTRA_PRODUCT_JSON = "product_json";
    static final String EXTRA_CAN_REQUEST = "can_request";
    static final String EXTRA_HC_PRODUCT = "hc_product";

    private static final Pattern BARCODE = Pattern.compile("^\\d{8,14}$");

    private AddProductFlow() {
    }

    /** Digits only, capped at {@code max} characters — the portal's barcode sanitiser. */
    static String digits(@Nullable String value, int max) {
        String d = value == null ? "" : value.replaceAll("\\D", "");
        return d.length() > max ? d.substring(0, max) : d;
    }

    /** A GTIN / EAN / UPC: 8 to 14 digits. */
    static boolean isBarcode(@Nullable String value) {
        return value != null && BARCODE.matcher(value.trim()).matches();
    }

    /** 2617 → "2,617". */
    static String count(int n) {
        return String.format(Locale.US, "%,d", n);
    }

    static boolean isBlank(@Nullable String s) {
        return s == null || s.trim().isEmpty();
    }

    /** First non-blank string among {@code keys} of a raw product record. */
    @Nullable
    static String str(@Nullable JsonObject o, String... keys) {
        if (o == null) return null;
        for (String key : keys) {
            JsonElement e = o.get(key);
            if (e != null && e.isJsonPrimitive()) {
                String s = e.getAsString();
                if (!isBlank(s)) return s.trim();
            }
        }
        return null;
    }

    /** Relative upload paths ("uploads/…") are served from the API host. */
    @Nullable
    static String absoluteUrl(@Nullable String url) {
        if (isBlank(url)) return null;
        String u = url.trim();
        if (u.startsWith("http://") || u.startsWith("https://")) return u;
        return ApiClient.BASE_URL + (u.startsWith("/") ? u.substring(1) : u);
    }

    /**
     * Rounded panels draw a banner across their top edge; clipping to the background's
     * outline keeps that banner inside the rounded corners.
     */
    static void clipToOutline(View panel) {
        panel.setClipToOutline(true);
    }

    static void toast(Context context, int res) {
        Toast.makeText(context, res, Toast.LENGTH_SHORT).show();
    }

    static void toast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    static int dp(Context context, float v) {
        return Math.round(v * context.getResources().getDisplayMetrics().density);
    }
}
