package com.anokix.distributorapp.network.dto;

import com.google.gson.JsonElement;

/**
 * data block of the single-promotion endpoints (create / get-by-id / update),
 * shaped as {@code data.promotion}.
 *
 * The create flow only needs the id to confirm success; the edit/view flow
 * reads the full record. Scalar numeric fields are declared as {@code String}
 * because Gson coerces JSON numbers into strings, so prefilling EditTexts works
 * whether the server emits {@code 5000} or {@code "5000"}. {@code trader_segments},
 * {@code selected_products}, and {@code stock_logs} are kept as raw {@link JsonElement}
 * and parsed defensively by the UI, since their exact shape varies.
 */
public class PromotionDetailData {
    public Promotion promotion;

    /** Raw stock log payload at the data root (may be {@code stock_logs} or {@code promotion_stock_logs}). */
    public JsonElement stock_logs;
    public JsonElement promotion_stock_logs;

    public static class Promotion {
        public String id;
        public String name;
        public String campaign_type;
        public String status;
        public String objective;
        public String description;

        public String start_date;       // yyyy-MM-dd
        public String end_date;
        public String budget;
        public String daily_budget;
        public String products_cost;

        public String address;
        public String place_id;
        public String lat;
        public String lng;

        public Media banner_file;
        public Media video_file;

        /** Analytics (get-by-id). */
        public String reach;
        public String clicks;
        public String ctr;
        public String revenue;
        public String attributed_sales;

        /** Raw payloads — shape varies, parsed defensively in the UI. */
        public JsonElement trader_segments;
        public JsonElement selected_products;
        public JsonElement stock_logs;
        public JsonElement promotion_stock_logs;
    }

    public static class Media {
        public String id;
        public String file_url;
        public String url;

        /** Best available URL for previewing the media. */
        public String bestUrl() {
            if (file_url != null && !file_url.isEmpty()) return file_url;
            return url;
        }
    }
}
