package com.anokix.distributorapp.data;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.model.ProductItem;
import com.anokix.distributorapp.model.PromotionItem;
import com.anokix.distributorapp.model.StoreItem;
import com.anokix.distributorapp.network.dto.ProductsData;
import com.anokix.distributorapp.network.dto.PromotionsData;
import com.anokix.distributorapp.network.dto.StoresData;
import com.anokix.distributorapp.network.dto.TradersData;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Converts network DTOs into the UI model objects the existing adapters already render.
 * Keeps the screens free of field-by-field mapping and JSON quirks.
 */
public final class ApiMapper {

    private static final int[] AVATAR_COLORS = {
            R.color.danger,
            R.color.warning,
            R.color.purple_primary,
            R.color.info,
            R.color.success,
    };

    private ApiMapper() {}

    // ---- Stores ----------------------------------------------------------

    public static List<StoreItem> toStoreItems(StoresData data) {
        List<StoreItem> out = new ArrayList<>();
        if (data == null || data.stores == null) {
            return out;
        }
        for (StoresData.Store s : data.stores) {
            out.add(new StoreItem(
                    String.valueOf(s.id),
                    safe(s.name),
                    safe(s.address),
                    s.follower_count,
                    s.review_count,
                    "Active",
                    safe(s.address),
                    splitTags(s.tag_list)));
        }
        return out;
    }

    /** Splits the comma-separated tag_list the API returns into display chips. */
    private static List<String> splitTags(String tagList) {
        List<String> tags = new ArrayList<>();
        if (isEmpty(tagList)) {
            return tags;
        }
        for (String part : tagList.split(",")) {
            String tag = part.trim();
            if (!tag.isEmpty() && !tags.contains(tag)) {
                tags.add(tag);
            }
        }
        return tags;
    }

    // ---- Traders ---------------------------------------------------------

    /**
     * Traders render straight from {@link TradersData.Trader} — the list response is the
     * same record the Edit wizard prefills from, so flattening it into a view model would
     * only throw away the fields the wizard needs.
     */
    public static int traderAvatarColor(int index) {
        return AVATAR_COLORS[Math.abs(index) % AVATAR_COLORS.length];
    }

    /** Up to two uppercase initials for the avatar circle. */
    public static String traderInitials(String name) {
        return initials(name);
    }

    // ---- Products --------------------------------------------------------

    public static List<ProductItem> toProductItems(ProductsData data) {
        List<ProductItem> out = new ArrayList<>();
        if (data == null || data.products == null) {
            return out;
        }
        for (ProductsData.Product p : data.products) {
            int stock = parseInt(p.stockValue());
            int reorder = parseInt(p.reorder_level);
            ProductItem item = new ProductItem(
                    safe(p.id),
                    safe(p.name),
                    safe(p.sku),
                    safe(p.category_title),
                    formatPrice(p.selling_price),
                    stockStatus(stock, reorder));
            item.imageUrl = p.imageUrl();
            item.brand = safe(p.brand_name);
            item.brandId = safe(p.brand_id);
            item.categoryId = safe(p.category_id);
            item.parentCategoryId = safe(p.parent_category_id);
            item.parentCategory = safe(p.parent_category_title);
            item.status = isEmpty(p.status) ? "Active" : p.status;
            item.barcode = safe(p.barcode);
            item.stockQty = stock;
            item.reorderLevel = reorder;
            out.add(item);
        }
        return out;
    }

    // ---- Promotions ------------------------------------------------------

    public static List<PromotionItem> toPromotionItems(PromotionsData data) {
        List<PromotionItem> out = new ArrayList<>();
        if (data == null || data.promotions == null) {
            return out;
        }
        String symbol = data.summary != null && data.summary.currency != null
                && data.summary.currency.symbol != null ? data.summary.currency.symbol : "R";
        for (PromotionsData.Promotion p : data.promotions) {
            out.add(new PromotionItem(
                    safe(p.id),
                    safe(p.name),
                    safe(p.campaign_type),
                    campaignTypeLabel(p.campaign_type),
                    safe(p.status),
                    prettify(p.status),
                    dateRange(p.start_date, p.end_date),
                    formatMoney(symbol, p.budget),
                    formatCount(p.reach),
                    formatCount(p.clicks),
                    formatPercent(p.ctr)));
        }
        return out;
    }

    private static String campaignTypeLabel(String type) {
        if (isEmpty(type)) {
            return "";
        }
        switch (type) {
            case "best_deal": return "Best Deal";
            case "cant_miss_deal": return "Can't Miss Deal";
            case "exclusive_deal": return "Exclusive Deal";
            case "featured_store": return "Featured Store";
            case "promotion": return "Promotion";
            case "post": return "Post";
            case "story": return "Story";
            default: return prettify(type);
        }
    }

    /** "pending_approval" → "Pending Approval". */
    private static String prettify(String value) {
        if (isEmpty(value)) {
            return "";
        }
        String[] parts = value.trim().split("[_\\s]+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    private static final SimpleDateFormat IN_DATE = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    /** "2026-06-15" + "2026-06-30" → "15 Jun – 30 Jun 2026". */
    private static String dateRange(String start, String end) {
        Date s = parseDate(start);
        Date e = parseDate(end);
        if (s == null || e == null) {
            return (safe(start) + " – " + safe(end)).trim();
        }
        SimpleDateFormat dm = new SimpleDateFormat("d MMM", Locale.US);
        SimpleDateFormat dmy = new SimpleDateFormat("d MMM yyyy", Locale.US);
        SimpleDateFormat y = new SimpleDateFormat("yyyy", Locale.US);
        if (y.format(s).equals(y.format(e))) {
            return dm.format(s) + " – " + dmy.format(e);
        }
        return dmy.format(s) + " – " + dmy.format(e);
    }

    private static Date parseDate(String value) {
        if (isEmpty(value)) {
            return null;
        }
        try {
            return IN_DATE.parse(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static String formatMoney(String symbol, double value) {
        return symbol + String.format(Locale.US, "%,.0f", value);
    }

    private static String formatCount(long value) {
        return String.format(Locale.US, "%,d", value);
    }

    private static String formatPercent(double value) {
        return String.format(Locale.US, "%.1f%%", value);
    }

    // ---- helpers ---------------------------------------------------------

    private static String formatPrice(String value) {
        try {
            return String.format(java.util.Locale.US, "R%,.2f", Double.parseDouble(value));
        } catch (Exception e) {
            return "R" + safe(value);
        }
    }

    private static String stockStatus(String stockValue) {
        return stockStatus(parseInt(stockValue), 0);
    }

    /**
     * Stock label for a product row. The distributor's own reorder level decides what
     * counts as low stock; without one, fall back to a flat threshold of 10 units.
     */
    private static String stockStatus(int stock, int reorderLevel) {
        if (stock <= 0) {
            return "Out of Stock";
        }
        int threshold = reorderLevel > 0 ? reorderLevel : 10;
        if (stock <= threshold) {
            return "Low Stock";
        }
        return "In Stock";
    }

    private static int parseInt(String value) {
        try {
            return (int) Double.parseDouble(value);
        } catch (Exception e) {
            return 0;
        }
    }

    private static String initials(String name) {
        if (isEmpty(name)) {
            return "?";
        }
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length && sb.length() < 2; i++) {
            if (!parts[i].isEmpty()) {
                sb.append(Character.toUpperCase(parts[i].charAt(0)));
            }
        }
        return sb.length() == 0 ? "?" : sb.toString();
    }

    private static String capitalize(String value) {
        if (isEmpty(value)) {
            return "";
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
