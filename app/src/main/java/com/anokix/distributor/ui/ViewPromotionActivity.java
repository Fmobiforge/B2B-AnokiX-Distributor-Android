package com.anokix.distributor.ui;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import com.anokix.distributor.R;
import com.anokix.distributor.network.ApiCallback;
import com.anokix.distributor.network.ApiClient;
import com.anokix.distributor.network.dto.PromotionDetailData;
import com.anokix.distributor.ui.views.RobotoBoldTextView;
import com.anokix.distributor.ui.views.RobotoTextView;
import com.bumptech.glide.Glide;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Read-only campaign detail screen — mirrors the web portal "Campaign Details" modal.
 * Loads {@code GET /api/distributor/promotions/get-by-id?id=}.
 */
public class ViewPromotionActivity extends AppCompatActivity {

    public static final String EXTRA_PROMOTION_ID = "promotion_id";

    private ProgressBar loading;
    private NestedScrollView scrollContent;
    private View footerActions;
    private LinearLayout summaryContainer;
    private LinearLayout productsContainer;
    private LinearLayout stockLogsContainer;
    private TextView productsEmpty;
    private TextView stockLogsEmpty;
    private ImageView mediaImage;
    private View mediaScrim;
    private ImageView mediaPlayIcon;
    private TextView mediaTitle;
    private TextView mediaDescription;

    private String promotionId;

    private final ActivityResultLauncher<Intent> editLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    setResult(RESULT_OK);
                    loadPromotion();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_promotion);

        promotionId = getIntent().getStringExtra(EXTRA_PROMOTION_ID);
        if (promotionId == null || promotionId.isEmpty()) {
            Toast.makeText(this, R.string.err_promo_name, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loading = findViewById(R.id.loading);
        scrollContent = findViewById(R.id.scrollContent);
        footerActions = findViewById(R.id.footerActions);
        summaryContainer = findViewById(R.id.summaryContainer);
        productsContainer = findViewById(R.id.productsContainer);
        stockLogsContainer = findViewById(R.id.stockLogsContainer);
        productsEmpty = findViewById(R.id.productsEmpty);
        stockLogsEmpty = findViewById(R.id.stockLogsEmpty);
        mediaImage = findViewById(R.id.mediaImage);
        mediaScrim = findViewById(R.id.mediaScrim);
        mediaPlayIcon = findViewById(R.id.mediaPlayIcon);
        mediaTitle = findViewById(R.id.mediaTitle);
        mediaDescription = findViewById(R.id.mediaDescription);

        View.OnClickListener close = v -> finish();
        findViewById(R.id.btnCloseHeader).setOnClickListener(close);
        findViewById(R.id.btnClose).setOnClickListener(close);
        findViewById(R.id.btnEdit).setOnClickListener(v ->
                editLauncher.launch(new Intent(this, CreatePromotionActivity.class)
                        .putExtra(CreatePromotionActivity.EXTRA_PROMOTION_ID, promotionId)));

        loadPromotion();
    }

    private void loadPromotion() {
        loading.setVisibility(View.VISIBLE);
        scrollContent.setVisibility(View.GONE);
        footerActions.setVisibility(View.GONE);

        ApiClient.get(this).getPromotionById(promotionId, new ApiCallback<PromotionDetailData>() {
            @Override
            public void onSuccess(PromotionDetailData data) {
                loading.setVisibility(View.GONE);
                if (data == null || data.promotion == null) {
                    Toast.makeText(ViewPromotionActivity.this,
                            R.string.err_promo_name, Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                bindPromotion(data);
                scrollContent.setVisibility(View.VISIBLE);
                footerActions.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(String message) {
                loading.setVisibility(View.GONE);
                Toast.makeText(ViewPromotionActivity.this, message, Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void bindPromotion(PromotionDetailData data) {
        PromotionDetailData.Promotion p = data.promotion;
        bindMedia(p);
        bindSummary(p);
        bindProducts(p.selected_products);
        bindStockLogs(resolveStockLogs(data));
    }

    private List<StockLogRow> resolveStockLogs(PromotionDetailData data) {
        if (data == null) return new ArrayList<>();
        // API returns stock_logs nested under promotion (see get-by-id sample).
        if (data.promotion != null) {
            List<StockLogRow> rows = parseStockLogs(data.promotion.stock_logs);
            if (!rows.isEmpty()) return rows;
            rows = parseStockLogs(data.promotion.promotion_stock_logs);
            if (!rows.isEmpty()) return rows;
        }
        List<StockLogRow> rows = parseStockLogs(data.stock_logs);
        if (!rows.isEmpty()) return rows;
        return parseStockLogs(data.promotion_stock_logs);
    }

    /** Defensive parser for stock log arrays (shape varies across server builds). */
    private List<StockLogRow> parseStockLogs(@Nullable JsonElement raw) {
        raw = unwrap(raw);
        if (raw == null) return new ArrayList<>();

        JsonArray array;
        if (raw.isJsonArray()) {
            array = raw.getAsJsonArray();
        } else if (raw.isJsonObject()) {
            JsonObject obj = raw.getAsJsonObject();
            if (obj.has("items") && obj.get("items").isJsonArray()) {
                array = obj.getAsJsonArray("items");
            } else if (obj.has("data") && obj.get("data").isJsonArray()) {
                array = obj.getAsJsonArray("data");
            } else {
                array = new JsonArray();
                array.add(obj);
            }
        } else {
            return new ArrayList<>();
        }

        List<StockLogRow> rows = new ArrayList<>();
        for (JsonElement el : array) {
            if (!el.isJsonObject()) continue;
            rows.add(stockLogFromObject(el.getAsJsonObject()));
        }
        return rows;
    }

    private StockLogRow stockLogFromObject(JsonObject o) {
        StockLogRow row = new StockLogRow();
        row.productLabel = firstNonEmpty(
                jStr(o, "product_name"),
                jStr(o, "name"),
                jStr(o, "product_title"),
                nestedStr(o, "product", "name"),
                jStr(o, "product_id"),
                jStr(o, "id"),
                nestedStr(o, "product", "id"),
                "—");
        row.action = jStr(o, "action");
        String qty = jStr(o, "quantity");
        if (qty.isEmpty() && o.has("quantity") && !o.get("quantity").isJsonNull()) {
            qty = o.get("quantity").getAsString();
        }
        row.quantity = qty;
        row.createdAt = firstNonEmpty(
                jStr(o, "created_at"),
                jStr(o, "date"),
                jStr(o, "logged_at"));
        return row;
    }

    private static final class StockLogRow {
        String productLabel;
        String action;
        String quantity;
        String createdAt;
    }

    private void bindStockLogs(List<StockLogRow> logs) {
        stockLogsContainer.removeAllViews();
        if (logs == null || logs.isEmpty()) {
            stockLogsEmpty.setVisibility(View.VISIBLE);
            return;
        }
        stockLogsEmpty.setVisibility(View.GONE);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (StockLogRow log : logs) {
            View row = inflater.inflate(R.layout.item_promo_stock_log_row, stockLogsContainer, false);
            ((TextView) row.findViewById(R.id.productName)).setText(safe(log.productLabel));
            ((TextView) row.findViewById(R.id.action)).setText(safe(log.action));
            ((TextView) row.findViewById(R.id.quantity)).setText(
                    log.quantity == null || log.quantity.isEmpty() ? "" : "Qty: " + log.quantity);
            ((TextView) row.findViewById(R.id.date)).setText(safe(log.createdAt));
            stockLogsContainer.addView(row);
        }
    }

    private void bindMedia(PromotionDetailData.Promotion p) {
        mediaTitle.setText(safe(p.name));
        mediaDescription.setText(safe(p.description));

        String bannerUrl = p.banner_file != null ? p.banner_file.bestUrl() : null;
        String videoUrl = p.video_file != null ? p.video_file.bestUrl() : null;
        boolean hasBanner = !TextUtils.isEmpty(bannerUrl);
        boolean hasVideo = !TextUtils.isEmpty(videoUrl);

        if (hasBanner) {
            mediaImage.setVisibility(View.VISIBLE);
            mediaScrim.setVisibility(View.VISIBLE);
            mediaPlayIcon.setVisibility(hasVideo ? View.VISIBLE : View.GONE);
            Glide.with(this).load(bannerUrl).centerCrop().into(mediaImage);
        } else if (hasVideo) {
            mediaImage.setVisibility(View.GONE);
            mediaScrim.setVisibility(View.GONE);
            mediaPlayIcon.setVisibility(View.VISIBLE);
            findViewById(R.id.mediaHero).setBackgroundResource(R.drawable.bg_promo_hero_default);
        } else {
            mediaImage.setVisibility(View.GONE);
            mediaScrim.setVisibility(View.GONE);
            mediaPlayIcon.setVisibility(View.GONE);
            findViewById(R.id.mediaHero).setBackgroundResource(R.drawable.bg_promo_hero_default);
        }
    }

    private void bindSummary(PromotionDetailData.Promotion p) {
        summaryContainer.removeAllViews();
        addStatusRow(p.status);
        addSummaryRow(getString(R.string.promo_type_label), campaignTypeLabel(p.campaign_type));
        addSummaryRow(getString(R.string.promo_objective), prettify(p.objective));
        addSummaryRow(getString(R.string.promo_duration), formatDateRange(p.start_date, p.end_date));
        addSummaryRow(getString(R.string.budget), formatBudget(p.budget, p.daily_budget));
        addSummaryRow(getString(R.string.promo_targeting), formatTargeting(p));
        addSummaryRow(getString(R.string.promo_products_summary), summarizeProducts(p.selected_products));
        addSummaryRow(getString(R.string.promo_products_cost), formatMoney(p.products_cost));
        addSummaryRow(getString(R.string.reach), formatCount(p.reach));
        addSummaryRow(getString(R.string.clicks), formatCount(p.clicks));
        addSummaryRow(getString(R.string.ctr), formatPercent(p.ctr));
        addSummaryRow(getString(R.string.promo_sales_impact), formatMoney(p.attributed_sales));
    }

    private void addSummaryRow(String label, CharSequence value) {
        if (value == null || value.toString().trim().isEmpty()) return;
        View row = LayoutInflater.from(this).inflate(R.layout.item_promo_detail_kv,
                summaryContainer, false);
        ((TextView) row.findViewById(R.id.label)).setText(label);
        ((TextView) row.findViewById(R.id.value)).setText(value);
        summaryContainer.addView(row);
    }

    private void addStatusRow(String status) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_promo_detail_status,
                summaryContainer, false);
        ((TextView) row.findViewById(R.id.label)).setText(R.string.promo_status_label);
        TextView value = row.findViewById(R.id.value);
        value.setText(prettify(status));
        value.setGravity(Gravity.END);
        value.setPadding(dp(10), dp(4), dp(10), dp(4));
        styleStatusBadge(value, status);
        summaryContainer.addView(row);
    }

    private void styleStatusBadge(TextView badge, String status) {
        String s = status == null ? "" : status.toLowerCase(Locale.US);
        int color;
        int bg;
        switch (s) {
            case "active":
                color = R.color.success;
                bg = R.drawable.bg_badge_success;
                break;
            case "rejected":
            case "inactive":
            case "expired":
                color = R.color.danger;
                bg = R.drawable.bg_badge_danger;
                break;
            case "pending_approval":
            case "paused":
            case "draft":
                color = R.color.warning;
                bg = R.drawable.bg_badge_warning;
                break;
            default:
                color = R.color.info;
                bg = R.drawable.bg_badge_info;
                break;
        }
        badge.setTextColor(ContextCompat.getColor(this, color));
        badge.setBackgroundResource(bg);
        badge.setTypeface(null, Typeface.BOLD);
        badge.setTextSize(11);
    }

    private void bindProducts(JsonElement selectedProducts) {
        productsContainer.removeAllViews();
        JsonElement root = unwrap(selectedProducts);
        if (root == null || !root.isJsonObject()) {
            productsEmpty.setVisibility(View.VISIBLE);
            return;
        }
        JsonObject obj = root.getAsJsonObject();
        int sections = 0;

        JsonArray singles = optArray(obj, "single_products");
        if (!singles.isEmpty()) {
            sections++;
            addProductSection(getString(R.string.promo_single_products), null, singles, false);
        }

        JsonArray combos = optArray(obj, "combo_deals");
        for (int i = 0; i < combos.size(); i++) {
            JsonElement dealEl = combos.get(i);
            if (!dealEl.isJsonObject()) continue;
            JsonObject deal = dealEl.getAsJsonObject();
            JsonArray lines = optArray(deal, "products");
            if (lines.isEmpty() && dealEl.isJsonArray()) {
                lines = dealEl.getAsJsonArray();
            }
            if (lines.isEmpty()) continue;
            sections++;
            addProductSection(getString(R.string.promo_combo_deals),
                    comboDealSubtitle(deal), lines, false);
        }

        JsonArray b1g1 = optArray(obj, "buy1_get1_deals");
        for (int i = 0; i < b1g1.size(); i++) {
            if (!b1g1.get(i).isJsonObject()) continue;
            JsonObject deal = b1g1.get(i).getAsJsonObject();
            sections++;
            addDealHeader(getString(R.string.promo_b1g1_deals), b1g1DealSubtitle(deal));
            appendProductLines(optArray(deal, "buy_products"), getString(R.string.promo_role_buy));
            appendProductLines(optArray(deal, "free_products"), getString(R.string.promo_role_free));
        }

        productsEmpty.setVisibility(sections == 0 ? View.VISIBLE : View.GONE);
    }

    private void addProductSection(String title, @Nullable String subtitle,
                                   JsonArray lines, boolean showRole) {
        addDealHeader(title, subtitle);
        appendProductLines(lines, null);
    }

    private void addDealHeader(String title, @Nullable String subtitle) {
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = productsContainer.getChildCount() > 0 ? dp(14) : 0;
        head.setLayoutParams(lp);

        RobotoBoldTextView t = new RobotoBoldTextView(this);
        t.setText(title);
        t.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        t.setTextSize(13);
        head.addView(t);

        if (subtitle != null && !subtitle.trim().isEmpty()) {
            RobotoTextView sub = new RobotoTextView(this);
            sub.setText(subtitle);
            sub.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            sub.setTextSize(11);
            sub.setPadding(0, dp(2), 0, 0);
            head.addView(sub);
        }

        View divider = new View(this);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        dlp.topMargin = dp(8);
        divider.setLayoutParams(dlp);
        divider.setBackgroundColor(ContextCompat.getColor(this, R.color.border));
        head.addView(divider);

        productsContainer.addView(head);
    }

    private void appendProductLines(JsonArray lines, @Nullable String role) {
        for (JsonElement el : lines) {
            if (!el.isJsonObject()) continue;
            JsonObject o = el.getAsJsonObject();
            View row = LayoutInflater.from(this).inflate(R.layout.item_promo_detail_product,
                    productsContainer, false);

            ImageView thumb = row.findViewById(R.id.thumb);
            TextView name = row.findViewById(R.id.name);
            TextView meta = row.findViewById(R.id.meta);
            TextView extra = row.findViewById(R.id.extra);
            TextView roleBadge = row.findViewById(R.id.roleBadge);

            name.setText(firstNonEmpty(jStr(o, "name"), jStr(o, "product_name"), "Product"));
            meta.setText(buildProductMeta(o));
            String price = firstNonEmpty(jStr(o, "campaign_selling_price"), jStr(o, "selling_price"));
            String stock = firstNonEmpty(jStr(o, "available_stock"), jStr(o, "stock"));
            if (!price.isEmpty() || !stock.isEmpty()) {
                extra.setVisibility(View.VISIBLE);
                String priceText = price.isEmpty() ? "" : formatMoney(price);
                extra.setText(priceText + (!stock.isEmpty() ? " · Stock: " + stock : ""));
            }

            String imageUrl = firstNonEmpty(jStr(o, "image_url"),
                    jStr(o, "primary_product_image_url"));
            if (!imageUrl.isEmpty()) {
                Glide.with(this).load(imageUrl).centerCrop().into(thumb);
            }

            if (role != null) {
                roleBadge.setVisibility(View.VISIBLE);
                roleBadge.setText(role);
            }

            productsContainer.addView(row);
        }
    }

    private String buildProductMeta(JsonObject o) {
        String category = firstNonEmpty(jStr(o, "category_title"), nestedStr(o, "category", "title"));
        String brand = firstNonEmpty(jStr(o, "brand_name"), nestedStr(o, "brand", "name"));
        if (!category.isEmpty() && !brand.isEmpty()) return category + " · " + brand;
        return !category.isEmpty() ? category : brand;
    }

    /** Combo deals in get-by-id often omit deal-level price/stock — derive from line items. */
    @Nullable
    private String comboDealSubtitle(JsonObject deal) {
        String price = jStr(deal, "campaign_selling_price");
        String stock = jStr(deal, "available_stock");
        if (price.isEmpty() || stock.isEmpty()) {
            DealMetrics metrics = metricsFromLines(optArray(deal, "products"));
            if (price.isEmpty() && metrics.sumPrice > 0) {
                price = String.valueOf(metrics.sumPrice);
            }
            if (stock.isEmpty() && metrics.minStock >= 0) {
                stock = String.valueOf(metrics.minStock);
            }
        }
        return dealSubtitle(price, stock);
    }

    @Nullable
    private String b1g1DealSubtitle(JsonObject deal) {
        String price = jStr(deal, "campaign_selling_price");
        String stock = jStr(deal, "available_stock");
        if (stock.isEmpty()) {
            DealMetrics metrics = metricsFromLines(optArray(deal, "buy_products"));
            if (metrics.minStock >= 0) stock = String.valueOf(metrics.minStock);
        }
        return dealSubtitle(price, stock);
    }

    @Nullable
    private String dealSubtitle(String price, String stock) {
        String priceText = formatMoney(price);
        if (priceText.isEmpty() && (stock == null || stock.isEmpty())) return null;
        return getString(R.string.promo_deal_price_stock,
                priceText.isEmpty() ? "—" : priceText,
                stock == null || stock.isEmpty() ? "—" : stock);
    }

    private static final class DealMetrics {
        double sumPrice;
        int minStock = Integer.MAX_VALUE;
    }

    private DealMetrics metricsFromLines(JsonArray lines) {
        DealMetrics m = new DealMetrics();
        for (JsonElement el : lines) {
            if (!el.isJsonObject()) continue;
            JsonObject o = el.getAsJsonObject();
            String cp = jStr(o, "campaign_selling_price");
            if (!cp.isEmpty()) {
                try { m.sumPrice += Double.parseDouble(cp); } catch (NumberFormatException ignored) {}
            }
            String st = firstNonEmpty(jStr(o, "available_stock"), jStr(o, "original_stock"));
            if (!st.isEmpty()) {
                try {
                    int s = (int) Math.round(Double.parseDouble(st));
                    m.minStock = Math.min(m.minStock, s);
                } catch (NumberFormatException ignored) {}
            }
        }
        if (m.minStock == Integer.MAX_VALUE) m.minStock = -1;
        return m;
    }

    // ---- Formatting / parsing helpers ------------------------------------

    private String formatTargeting(PromotionDetailData.Promotion p) {
        StringBuilder sb = new StringBuilder();
        if (!TextUtils.isEmpty(p.address)) sb.append(p.address.trim());
        String segments = formatSegments(p.trader_segments);
        if (!segments.isEmpty()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(getString(R.string.promo_trader_segments_label)).append(": ").append(segments);
        }
        return sb.toString();
    }

    private String formatSegments(JsonElement el) {
        el = unwrap(el);
        if (el == null || !el.isJsonArray()) return "";
        List<String> names = new ArrayList<>();
        for (JsonElement e : el.getAsJsonArray()) {
            if (e.isJsonPrimitive()) {
                names.add(e.getAsString());
            } else if (e.isJsonObject()) {
                String name = firstNonEmpty(jStr(e.getAsJsonObject(), "name"),
                        jStr(e.getAsJsonObject(), "title"));
                if (!name.isEmpty()) names.add(name);
            }
        }
        return TextUtils.join(", ", names);
    }

    private String summarizeProducts(JsonElement el) {
        el = unwrap(el);
        if (el == null || !el.isJsonObject()) return "";
        JsonObject root = el.getAsJsonObject();
        List<String> parts = new ArrayList<>();
        int singles = optArray(root, "single_products").size();
        int combos = optArray(root, "combo_deals").size();
        int b1g1 = optArray(root, "buy1_get1_deals").size();
        if (singles > 0) parts.add(singles + " Single Product" + (singles > 1 ? "s" : ""));
        if (combos > 0) parts.add(combos + " Combo Deal" + (combos > 1 ? "s" : ""));
        if (b1g1 > 0) parts.add(b1g1 + " Buy1Get1 Deal" + (b1g1 > 1 ? "s" : ""));
        return TextUtils.join(", ", parts);
    }

    private String formatBudget(String budget, String daily) {
        String b = formatMoney(budget);
        String d = formatMoney(daily);
        if (b.isEmpty()) return d;
        if (d.isEmpty()) return b;
        return b + " (" + d + " daily)";
    }

    private String formatMoney(String value) {
        if (value == null || value.trim().isEmpty()) return "";
        String v = value.trim();
        if (v.startsWith("R")) return v;
        try {
            double num = Double.parseDouble(v);
            if (num == Math.rint(num)) {
                return "R" + String.format(Locale.US, "%,.0f", num).replace(',', ' ');
            }
            return "R" + String.format(Locale.US, "%,.2f", num).replace(',', ' ');
        } catch (NumberFormatException e) {
            return "R" + v;
        }
    }

    private String formatCount(String value) {
        if (value == null || value.trim().isEmpty()) return "0";
        try {
            double num = Double.parseDouble(value.trim());
            return String.format(Locale.US, "%,.0f", num).replace(',', ' ');
        } catch (NumberFormatException e) {
            return value;
        }
    }

    private String formatPercent(String value) {
        if (value == null || value.trim().isEmpty()) return "0.0%";
        String v = value.trim();
        if (v.endsWith("%")) return v;
        try {
            double num = Double.parseDouble(v);
            return String.format(Locale.US, "%.1f%%", num);
        } catch (NumberFormatException e) {
            return v;
        }
    }

    private String formatDateRange(String start, String end) {
        if (TextUtils.isEmpty(start) && TextUtils.isEmpty(end)) return "";
        if (TextUtils.isEmpty(start)) return formatDisplayDate(end);
        if (TextUtils.isEmpty(end)) return formatDisplayDate(start);
        return formatDisplayDate(start) + " – " + formatDisplayDate(end);
    }

    private String formatDisplayDate(String api) {
        if (api == null || api.isEmpty()) return "";
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat out = new SimpleDateFormat("dd MMM yyyy", Locale.US);
            Date d = in.parse(api);
            return d == null ? api : out.format(d);
        } catch (Exception e) {
            return api;
        }
    }

    private String campaignTypeLabel(String type) {
        if (type == null || type.isEmpty()) return "";
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

    private String prettify(String value) {
        if (value == null || value.isEmpty()) return "";
        String spaced = value.replace('_', ' ').trim();
        if (spaced.isEmpty()) return "";
        return spaced.substring(0, 1).toUpperCase(Locale.US) + spaced.substring(1);
    }

    @Nullable
    private JsonElement unwrap(@Nullable JsonElement el) {
        if (el == null || el.isJsonNull()) return null;
        if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
            try {
                return com.google.gson.JsonParser.parseString(el.getAsString());
            } catch (Exception e) {
                return null;
            }
        }
        return el;
    }

    private JsonArray optArray(JsonObject o, String key) {
        if (o != null && o.has(key) && o.get(key).isJsonArray()) {
            return o.getAsJsonArray(key);
        }
        return new JsonArray();
    }

    private String jStr(JsonObject o, String key) {
        if (o != null && o.has(key) && !o.get(key).isJsonNull()) {
            JsonElement el = o.get(key);
            if (el.isJsonPrimitive()) {
                return el.getAsString();
            }
        }
        return "";
    }

    private String nestedStr(JsonObject o, String parent, String key) {
        if (o != null && o.has(parent) && o.get(parent).isJsonObject()) {
            return jStr(o.getAsJsonObject(parent), key);
        }
        return "";
    }

    private String firstNonEmpty(String... values) {
        for (String v : values) {
            if (v != null && !v.isEmpty()) return v;
        }
        return "";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
