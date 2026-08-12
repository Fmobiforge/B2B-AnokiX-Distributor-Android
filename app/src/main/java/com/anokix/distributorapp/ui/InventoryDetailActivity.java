package com.anokix.distributorapp.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.network.ApiCallback;
import com.anokix.distributorapp.network.ApiClient;
import com.anokix.distributorapp.network.dto.InventoryPageData;
import com.anokix.distributorapp.network.dto.StockHistoryData;
import com.anokix.distributorapp.network.dto.StockMovement;
import com.anokix.distributorapp.ui.adapter.InventoryAdapter;
import com.anokix.distributorapp.ui.util.StockActionSheets;
import com.anokix.distributorapp.ui.views.RobotoBoldTextView;
import com.anokix.distributorapp.ui.views.RobotoLightTextView;
import com.anokix.distributorapp.ui.views.RobotoTextView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Inventory item drawer: a stock summary with the adjust / transfer actions, the product's
 * facts (Details) and its ledger (History) — the mobile take on the web side panel.
 */
public class InventoryDetailActivity extends BaseSecondaryActivity {

    private static final String EXTRA_ROW = "row";
    private static final String EXTRA_WAREHOUSES = "warehouses";
    private static final String EXTRA_START_HISTORY = "start_history";

    private static final int HISTORY_LIMIT = 100;
    private static final String[] BREAKDOWN_KEYS =
            {"Available", "Reserved", "Packed", "Damaged", "Expired"};
    private static final int[] BREAKDOWN_COLORS =
            {R.color.success, R.color.info, R.color.purple_primary, R.color.warning, R.color.danger};

    private final Gson gson = new Gson();

    private InventoryPageData.Row row;
    private List<InventoryPageData.Warehouse> warehouses = new ArrayList<>();
    private boolean showingHistory;
    /** A history fetch has been started (so we don't ask twice). */
    private boolean historyLoaded;
    /** A history response has been rendered, and whether it came back empty. */
    private boolean historyRendered;
    private boolean historyIsEmpty;

    private View detailsPane, historyPane, historyEmpty;
    private TextView tabDetails, tabHistory;
    private View tabDetailsIndicator, tabHistoryIndicator;

    public static Intent intent(Context context, InventoryPageData.Row row,
                                List<InventoryPageData.Warehouse> warehouses,
                                boolean startOnHistory) {
        Gson gson = new Gson();
        Intent intent = new Intent(context, InventoryDetailActivity.class);
        intent.putExtra(EXTRA_ROW, gson.toJson(row));
        intent.putExtra(EXTRA_WAREHOUSES, gson.toJson(warehouses));
        intent.putExtra(EXTRA_START_HISTORY, startOnHistory);
        return intent;
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_inventory_detail;
    }

    @Override
    protected String getScreenTitle() {
        return getString(R.string.inventory_item_title);
    }

    @Override
    protected void bindViews() {
        row = gson.fromJson(getIntent().getStringExtra(EXTRA_ROW), InventoryPageData.Row.class);
        List<InventoryPageData.Warehouse> parsed = gson.fromJson(
                getIntent().getStringExtra(EXTRA_WAREHOUSES),
                new TypeToken<List<InventoryPageData.Warehouse>>() { }.getType());
        if (parsed != null) {
            warehouses = parsed;
        }
        if (row == null) {
            finish();
            return;
        }

        detailsPane = findViewById(R.id.detailsPane);
        historyPane = findViewById(R.id.historyPane);
        historyEmpty = findViewById(R.id.historyEmpty);
        tabDetails = findViewById(R.id.tabDetails);
        tabHistory = findViewById(R.id.tabHistory);
        tabDetailsIndicator = findViewById(R.id.tabDetailsIndicator);
        tabHistoryIndicator = findViewById(R.id.tabHistoryIndicator);

        findViewById(R.id.tabDetailsWrap).setOnClickListener(v -> showTab(false));
        findViewById(R.id.tabHistoryWrap).setOnClickListener(v -> showTab(true));
        findViewById(R.id.heroAdjust).setOnClickListener(v ->
                StockActionSheets.showAdjustStock(this, row, this::onStockChanged));
        findViewById(R.id.heroTransfer).setOnClickListener(v ->
                StockActionSheets.showTransfer(this, row, warehouses, this::onStockChanged));

        render();
        showTab(getIntent().getBooleanExtra(EXTRA_START_HISTORY, false));
    }

    @Override
    protected void loadData() {
        // The row travels with the intent; history is fetched when that tab is opened.
    }

    private void render() {
        bindHero();
        buildDetails();
        buildBreakdown();
    }

    // ---- Hero ------------------------------------------------------------

    private void bindHero() {
        View swatch = findViewById(R.id.heroSwatch);
        int color = InventoryAdapter.parseColor(row.imageColor,
                ContextCompat.getColor(this, R.color.purple_primary));
        if (swatch.getBackground() instanceof GradientDrawable) {
            GradientDrawable shape = (GradientDrawable) swatch.getBackground().mutate();
            shape.setColor(color);
            swatch.setBackground(shape);
        }

        ((TextView) findViewById(R.id.heroName)).setText(row.name);
        ((TextView) findViewById(R.id.heroMeta)).setText(joinMeta(row.sku, row.brand));
        ((TextView) findViewById(R.id.heroUnits))
                .setText(String.format(Locale.getDefault(), "%,d", row.units));
        ((TextView) findViewById(R.id.heroValue))
                .setText(String.format(Locale.US, "R%,.0f", row.stockValue));

        TextView badge = findViewById(R.id.heroStatusBadge);
        badge.setText(row.statusLabel());
        if (row.isOutOfStock()) {
            styleBadge(badge, R.color.danger, R.drawable.bg_badge_danger);
        } else if (row.isLowStock()) {
            styleBadge(badge, R.color.warning, R.drawable.bg_badge_warning);
        } else {
            styleBadge(badge, R.color.success, R.drawable.bg_badge_success);
        }
    }

    private void styleBadge(TextView badge, @ColorRes int color, @DrawableRes int background) {
        badge.setTextColor(ContextCompat.getColor(this, color));
        badge.setBackgroundResource(background);
    }

    /** Reloads the row from the page API so the drawer shows the post-change numbers. */
    private void onStockChanged(String message) {
        if (message != null && !message.isEmpty()) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
        setResult(RESULT_OK);
        ApiClient.get(this).getInventoryPage(new ApiCallback<InventoryPageData>() {
            @Override
            public void onSuccess(InventoryPageData data) {
                if (data == null || data.rows == null) {
                    return;
                }
                for (InventoryPageData.Row updated : data.rows) {
                    if (updated.productId == row.productId) {
                        row = updated;
                        break;
                    }
                }
                if (data.warehouses != null) {
                    warehouses = data.warehouses;
                }
                render();
                // The ledger just changed, so drop what we rendered before re-asking.
                historyLoaded = false;
                historyRendered = false;
                updateHistoryEmptyState();
                if (showingHistory) {
                    loadHistory();
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(InventoryDetailActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    // ---- Details ---------------------------------------------------------

    private void buildDetails() {
        LinearLayout container = findViewById(R.id.detailRows);
        container.removeAllViews();

        Map<Integer, String> rows = new LinkedHashMap<>();
        rows.put(R.string.label_sku, row.sku);
        rows.put(R.string.label_barcode, row.barcode);
        rows.put(R.string.label_category, row.category);
        rows.put(R.string.label_brand, row.brand);
        rows.put(R.string.label_warehouse, row.warehouse);
        rows.put(R.string.label_batch, row.batchNumber);
        rows.put(R.string.label_expires, row.expiryDate);
        rows.put(R.string.label_last_updated, row.updatedAt);

        boolean first = true;
        for (Map.Entry<Integer, String> entry : rows.entrySet()) {
            if (!first) {
                container.addView(divider());
            }
            container.addView(detailRow(getString(entry.getKey()), entry.getValue()));
            first = false;
        }
    }

    private View detailRow(String label, String value) {
        LinearLayout rowView = new LinearLayout(this);
        rowView.setOrientation(LinearLayout.HORIZONTAL);
        rowView.setGravity(Gravity.CENTER_VERTICAL);
        rowView.setPadding(0, dp(13), 0, dp(13));

        RobotoTextView left = new RobotoTextView(this);
        left.setText(label);
        left.setTextSize(13);
        left.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        left.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        rowView.addView(left);

        RobotoBoldTextView right = new RobotoBoldTextView(this);
        right.setText(isBlank(value) ? "—" : value);
        right.setTextSize(13);
        right.setGravity(Gravity.END);
        right.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        right.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1.3f));
        rowView.addView(right);
        return rowView;
    }

    /** "Where those units sit" — one colour-coded chip per inventory bucket. */
    private void buildBreakdown() {
        LinearLayout container = findViewById(R.id.breakdownContainer);
        container.removeAllViews();
        for (int i = 0; i < BREAKDOWN_KEYS.length; i++) {
            int value = row.breakdownValue(BREAKDOWN_KEYS[i]);
            int color = ContextCompat.getColor(this, BREAKDOWN_COLORS[i]);

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.bg_stat_card);
            card.setPadding(dp(12), dp(10), dp(12), dp(10));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(104),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dp(8));
            card.setLayoutParams(lp);

            LinearLayout header = new LinearLayout(this);
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setGravity(Gravity.CENTER_VERTICAL);

            View dot = new View(this);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(8), dp(8));
            dlp.setMarginEnd(dp(6));
            dot.setLayoutParams(dlp);
            dot.setBackgroundResource(R.drawable.bg_legend_dot);
            dot.getBackground().mutate().setTint(color);
            header.addView(dot);

            RobotoLightTextView label = new RobotoLightTextView(this);
            label.setText(BREAKDOWN_KEYS[i]);
            label.setTextSize(10);
            label.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            header.addView(label);
            card.addView(header);

            RobotoBoldTextView count = new RobotoBoldTextView(this);
            count.setText(String.format(Locale.getDefault(), "%,d", value));
            count.setTextSize(18);
            count.setPadding(0, dp(4), 0, 0);
            count.setTextColor(ContextCompat.getColor(this,
                    value > 0 ? R.color.text_primary : R.color.text_tertiary));
            card.addView(count);

            container.addView(card);
        }
    }

    // ---- History ---------------------------------------------------------

    private void showTab(boolean history) {
        showingHistory = history;
        detailsPane.setVisibility(history ? View.GONE : View.VISIBLE);
        historyPane.setVisibility(history ? View.VISIBLE : View.GONE);
        updateHistoryEmptyState();

        int primary = ContextCompat.getColor(this, R.color.purple_primary);
        int secondary = ContextCompat.getColor(this, R.color.text_secondary);
        tabDetails.setTextColor(history ? secondary : primary);
        tabHistory.setTextColor(history ? primary : secondary);
        tabDetailsIndicator.setVisibility(history ? View.INVISIBLE : View.VISIBLE);
        tabHistoryIndicator.setVisibility(history ? View.VISIBLE : View.INVISIBLE);

        if (history && !historyLoaded) {
            loadHistory();
        }
    }

    private void loadHistory() {
        historyLoaded = true;
        ApiClient.get(this).getStockHistory(String.valueOf(row.productId), HISTORY_LIMIT,
                new ApiCallback<StockHistoryData>() {
                    @Override
                    public void onSuccess(StockHistoryData data) {
                        renderHistory(data == null ? null : data.movements);
                    }

                    @Override
                    public void onError(String message) {
                        historyLoaded = false;
                        Toast.makeText(InventoryDetailActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void renderHistory(List<StockMovement> movements) {
        LinearLayout container = findViewById(R.id.historyPane);
        container.removeAllViews();
        historyRendered = true;
        historyIsEmpty = movements == null || movements.isEmpty();
        if (!historyIsEmpty) {
            for (StockMovement movement : movements) {
                container.addView(historyCard(movement));
            }
        }
        updateHistoryEmptyState();
    }

    /**
     * The "nothing has moved yet" panel belongs to the History tab only, and only once a
     * response has actually come back empty — so it survives switching tabs and never
     * flashes while the ledger is still loading.
     */
    private void updateHistoryEmptyState() {
        historyEmpty.setVisibility(showingHistory && historyRendered && historyIsEmpty
                ? View.VISIBLE : View.GONE);
    }

    private View historyCard(StockMovement movement) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setBackgroundResource(R.drawable.bg_stat_card);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(10);
        card.setLayoutParams(lp);

        // Left rail: coloured dot keyed to the movement type.
        View rail = new View(this);
        LinearLayout.LayoutParams railLp = new LinearLayout.LayoutParams(dp(8), dp(8));
        railLp.topMargin = dp(5);
        railLp.setMarginEnd(dp(10));
        rail.setLayoutParams(railLp);
        rail.setBackgroundResource(R.drawable.bg_legend_dot);
        rail.getBackground().mutate().setTint(ContextCompat.getColor(this, movementColor(movement)));
        card.addView(rail);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        RobotoBoldTextView type = new RobotoBoldTextView(this);
        type.setText(movement.movementLabel());
        type.setTextSize(13);
        type.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        type.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(type);

        RobotoBoldTextView units = new RobotoBoldTextView(this);
        units.setText(signedQuantity(movement));
        units.setTextSize(14);
        units.setTextColor(ContextCompat.getColor(this, movementColor(movement)));
        top.addView(units);
        body.addView(top);

        RobotoTextView when = new RobotoTextView(this);
        when.setText(joinMeta(movement.created_at, movement.warehouse_name));
        when.setTextSize(11);
        when.setPadding(0, dp(4), 0, 0);
        when.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        body.addView(when);

        String note = joinMeta(movement.notes, movement.user_email);
        if (!isBlank(note)) {
            RobotoLightTextView noteView = new RobotoLightTextView(this);
            noteView.setText(note);
            noteView.setTextSize(11);
            noteView.setPadding(0, dp(2), 0, 0);
            noteView.setTextColor(ContextCompat.getColor(this, R.color.text_tertiary));
            body.addView(noteView);
        }

        card.addView(body);
        return card;
    }

    @ColorRes
    private int movementColor(StockMovement movement) {
        String type = movement.movement_type == null ? "" : movement.movement_type.toLowerCase(Locale.US);
        if (type.contains("receive") || type.contains("in")) {
            return R.color.success;
        }
        if (type.contains("transfer")) {
            return R.color.purple_primary;
        }
        if (type.contains("damage") || type.contains("expire") || type.contains("out")) {
            return R.color.danger;
        }
        return R.color.info;
    }

    /** Receipts read as "+10", write-offs as "-10", everything else plain. */
    private String signedQuantity(StockMovement movement) {
        String quantity = isBlank(movement.quantity) ? "0" : movement.quantity.trim();
        String type = movement.movement_type == null ? "" : movement.movement_type.toLowerCase(Locale.US);
        if (quantity.startsWith("-") || quantity.startsWith("+")) {
            return quantity;
        }
        if (type.contains("receive") || type.endsWith("_in")) {
            return "+" + quantity;
        }
        if (type.endsWith("_out") || type.contains("damage") || type.contains("expire")) {
            return "-" + quantity;
        }
        return quantity;
    }

    private String joinMeta(String first, String second) {
        boolean hasFirst = !isBlank(first);
        boolean hasSecond = !isBlank(second);
        if (hasFirst && hasSecond) {
            return first + "  ·  " + second;
        }
        if (hasFirst) {
            return first;
        }
        return hasSecond ? second : "";
    }

    private View divider() {
        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1));
        divider.setBackgroundColor(ContextCompat.getColor(this, R.color.border_light));
        return divider;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty() || "null".equals(value);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
