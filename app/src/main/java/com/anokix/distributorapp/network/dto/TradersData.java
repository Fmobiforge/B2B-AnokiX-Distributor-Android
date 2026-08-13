package com.anokix.distributorapp.network.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * data block of GET /api/distributor/traders.
 *
 * <p>The list response carries the full trader record, so the Edit wizard is
 * prefilled straight from the row the user tapped — there is no per-trader
 * detail endpoint.
 */
public class TradersData {
    public List<Trader> traders;
    public int total;
    public Summary summary;

    public static class Trader {
        public String id;
        public String user_id;
        public String trader_code;

        // Owner. Only returned by newer builds of the API; the wizard prefills
        // whatever is present and leaves the rest for the user to fill in.
        public String first_name;
        public String last_name;
        public String owner_full_name;
        public String mobile_number;
        public String phone_number;
        public String email;
        public String country_code;
        public String id_number;
        public String passport_number;

        // Business
        public String name;
        public String business_name;
        public String trading_name;
        public String company_logo_url;
        public String business_registration_number;
        public String business_phone_number;
        public String business_email;

        // Classification
        public String trader_type;
        public String store_type;
        public String primary_product_category_ids;

        // Location
        public String address;
        public Double latitude;
        public Double longitude;

        // Distributor / trading preferences
        public String preferred_distributor_ids;
        public String recommended_distributor_id;
        public String preferred_delivery_days;
        public String payment_method;

        // State
        public String status;
        public String registration_status;
        public String credit_status;
        public String created_at;

        /** Uploaded KYC files keyed by document field name. */
        public Map<String, List<Document>> documents;

        /** Business name first, then trading name, then whatever the API called it. */
        public String displayName() {
            if (notBlank(business_name)) {
                return business_name;
            }
            if (notBlank(trading_name)) {
                return trading_name;
            }
            return notBlank(name) ? name : "";
        }

        /** Owner first name, falling back to the first word of the full name. */
        public String ownerFirstName() {
            if (notBlank(first_name)) {
                return first_name;
            }
            String[] parts = splitFullName();
            return parts.length > 0 ? parts[0] : "";
        }

        /** Owner last name, falling back to everything after the first word. */
        public String ownerLastName() {
            if (notBlank(last_name)) {
                return last_name;
            }
            String[] parts = splitFullName();
            if (parts.length < 2) {
                return "";
            }
            StringBuilder rest = new StringBuilder();
            for (int i = 1; i < parts.length; i++) {
                if (rest.length() > 0) {
                    rest.append(' ');
                }
                rest.append(parts[i]);
            }
            return rest.toString();
        }

        private String[] splitFullName() {
            return notBlank(owner_full_name) ? owner_full_name.trim().split("\\s+") : new String[0];
        }

        /** Trader classification the API returns as either a slug or a numeric id. */
        public String traderTypeValue() {
            return notBlank(trader_type) ? trader_type : (notBlank(store_type) ? store_type : "");
        }

        /** Comma-separated primary_product_category_ids as a list. */
        public List<Long> categoryIds() {
            return parseIds(primary_product_category_ids);
        }

        /** Comma-separated preferred_distributor_ids as a list. */
        public List<Long> distributorIds() {
            return parseIds(preferred_distributor_ids);
        }

        private static List<Long> parseIds(String csv) {
            List<Long> out = new ArrayList<>();
            if (csv == null) {
                return out;
            }
            for (String part : csv.split(",")) {
                String trimmed = part.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                try {
                    out.add(Long.parseLong(trimmed));
                } catch (NumberFormatException ignored) {
                    // Unexpected value — skip it rather than dropping the whole list.
                }
            }
            return out;
        }

        /** First uploaded file name for a document field, or "" when nothing is on file. */
        public String documentName(String field) {
            if (documents == null) {
                return "";
            }
            List<Document> files = documents.get(field);
            if (files == null || files.isEmpty() || files.get(0) == null) {
                return "";
            }
            return files.get(0).original_name == null ? "" : files.get(0).original_name;
        }

        private static boolean notBlank(String value) {
            return value != null && !value.trim().isEmpty() && !"null".equals(value);
        }
    }

    public static class Document {
        public String id;
        public String file_url;
        public String original_name;
    }

    public static class Summary {
        public int total_traders;
        public String total_traders_month_change_label;
        public int active_traders;
        public String active_traders_percent_label;
        public int new_this_month;
        public String new_this_month_change_label;
        public int pending_approval_traders;
        public String pending_approval_traders_percent_label;
        public int high_value_traders;
        public String high_value_traders_percent_label;
    }

    /**
     * Delivery days travel as {@code "1:8.00-18.00/3:8.00-18.00"} where the leading
     * number is the day index (0 = Sunday). Parsed into day → opening hours so the
     * wizard can repaint the schedule table it was saved from.
     */
    public static Map<Integer, String> parseDeliveryDays(String raw) {
        Map<Integer, String> out = new LinkedHashMap<>();
        if (raw == null || raw.trim().isEmpty()) {
            return out;
        }
        // Older records separated entries with "/," instead of "/".
        for (String entry : raw.split("[/,]")) {
            String trimmed = entry.trim();
            int colon = trimmed.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            try {
                out.put(Integer.parseInt(trimmed.substring(0, colon).trim()),
                        trimmed.substring(colon + 1).trim());
            } catch (NumberFormatException ignored) {
                // Malformed entry — keep the rest of the schedule.
            }
        }
        return out;
    }
}
