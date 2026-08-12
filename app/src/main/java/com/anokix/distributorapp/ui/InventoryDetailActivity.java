package com.anokix.distributorapp.ui;

import android.content.Context;
import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.network.ApiCallback;
import com.anokix.distributorapp.network.ApiClient;
import com.anokix.distributorapp.network.dto.StockHistoryData;
import com.anokix.distributorapp.network.dto.StockMovement;
import com.anokix.distributorapp.ui.views.RobotoBoldTextView;
import com.anokix.distributorapp.ui.views.RobotoLightTextView;
import com.anokix.distributorapp.ui.views.RobotoTextView;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Inventory item drawer: the product's stock facts (Details) and its ledger (History),
 * ported from the web side panel. Opened from the eye icon or the "View History" action.
 */
public class InventoryDetailActivity extends BaseSecondaryActivity {

    private static final String EXTRA_PRODUCT_ID = "product_id";
    private static final String EXTRA_NAME = "name";
    private static final String EXTRA_SKU = "sku";
    private static final String EXTRA_BARCODE = "barcode";
    private static final String EXTRA_CATEGORY = "category";
    private static final String EXTRA_BRAND = "brand";
    private static final String EXTRA_WAREHOUSE = "warehouse";
    private static final String EXTRA_UNITS = "units";
    private static final String EXTRA_STOCK_VALUE = "stock_value";
    private static final String EXTRA_BATCH = "batch";
    private static final String EXTRA_EXPIRY = "expiry";
    private static final String EXTRA_UPDATED = "updated";
    private static final String EXTRA_BREAKDOWN = "breakdown";
    private static final String EXTRA_START_HISTORY = "start_history";

    private static final int HISTORY_LIMIT = 100;
    private static final String[] BREAKDOWN_KEYS =
            {"Available", "Reserved", "Packed", "Damaged", "Expired"};

    private String productId;
    private boolean showingHistory;
    private boolean historyLoaded;

    private View detailsPane, historyPane, historyEmpty;
    private TextView tabDetails, tabHistory;
    private View tabDetailsIndicator, tabHistoryIndicator;

    /** Builds the intent from a page row — the drawer shows what the list already knows. */
    public static Intent intent(Context context,
                                com.anokix.distributorapp.network.dto.InventoryPageData.Row row,
                                boolean startOnHistory) {
        Intent intent = new Intent(context, InventoryDetailActivity.class);
        intent.putExtra(EXTRA_PRODUCT_ID, String.valueOf(row.productId));
        intent.putExtra(EXTRA_NAME, row.name);
        intent.putExtra(EXTRA_SKU, row.sku);
        intent.putExtra(EXTRA_BARCODE, row.barcode);
        intent.putExtra(EXTRA_CATEGORY, row.category);
        intent.putExtra(EXTRA_BRAND, row.brand);
        intent.putExtra(EXTRA_WAREHOUSE, row.warehouse);
        intent.putExtra(EXTRA_UNITS, row.units);
        intent.putExtra(EXTRA_STOCK_VALUE, row.stockValue);
        intent.putExtra(EXTRA_BATCH, row.batchNumber);
        intent.putExtra(EXTRA_EXPIRY, row.expiryDate);
        intent.putExtra(EXTRA_UPDATED, row.updatedAt);
        intent.putExtra(EXTRA_START_HISTORY, startOnHistory);

        int[] breakdown = new int[BREAKDOWN_KEYS.length];
        for (int i = 0; i < BREAKDOWN_KEYS.length; i++) {
            breakdown[i] = row.breakdownValue(BREAKDOWN_KEYS[i]);
        }
        intent.putExtra(EXTRA_BREAKDOWN, breakdown);
        return intent;
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_inventory_detail;
    }

    @Override
    protected String getScreenTitle() {
        String name = getIntent().getStringExtra(EXTRA_NAME);
        return name == null ? getString(R.string.inventory_overview) : name;
    }

    @Override
    protected void bindViews() {
        productId = getIntent().getStringExtra(EXTRA_PRODUCT_ID);

        detailsPane = findViewById(R.id.detailsPane);
        historyPane = findViewById(R.id.historyPane);
        historyEmpty = findViewById(R.id.historyEmpty);
        tabDetails = findViewById(R.id.tabDetails);
        tabHistory = findViewById(R.id.tabHistory);
        tabDetailsIndicator = findViewById(R.id.tabDetailsIndicator);
        tabHistoryIndicator = findViewById(R.id.tabHistoryIndicator);

        findViewById(R.id.tabDetailsWrap).setOnClickListener(v -> showTab(false));
        findViewById(R.id.tabHistoryWrap).setOnClickListener(v -> showTab(true));

        buildDetails();
        buildBreakdown();
        showTab(getIntent().getBooleanExtra(EXTRA_START_HISTORY, false));
    }

    @Override
    protected void loadData() {
        // History is fetched the first time that tab is opened (see showTab).
    }

    // ---- Details ---------------------------------------------------------

    private void buildDetails() {
        LinearLayout container = findViewById(R.id.detailRows);
        container.removeAllViews();

        Map<Integer, String> rows = new LinkedHashMap<>();
        rows.put(R.string.label_sku, getIntent().getStringExtra(EXTRA_SKU));
        rows.put(R.string.label_barcode, getIntent().getStringExtra(EXTRA_BARCODE));
        rows.put(R.string.label_category, getIntent().getStringExtra(EXTRA_CATEGORY));
        rows.put(R.string.label_brand, getIntent().getStringExtra(EXTRA_BRAND));
        rows.put(R.string.label_warehouse, getIntent().getStringExtra(EXTRA_WAREHOUSE));
        rows.put(R.string.label_units_on_hand,
                String.format(Locale.getDefault(), "%,d", getIntent().getLongExtra(EXTRA_UNITS, 0)));
        rows.put(R.string.label_stock_value,
                String.format(Locale.US, "R%,.0f", getIntent().getDoubleExtra(EXTRA_STOCK_VALUE, 0)));
        rows.put(R.string.label_batch, getIntent().getStringExtra(EXTRA_BATCH));
        rows.put(R.string.label_expires, getIntent().getStringExtra(EXTRA_EXPIRY));
        rows.put(R.string.label_last_updated, getIntent().getStringExtra(EXTRA_UPDATED));

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
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(12), dp(12), dp(12));

        RobotoTextView left = new RobotoTextView(this);
        left.setText(label);
        left.setTextSize(13);
        left.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        left.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(left);

        RobotoBoldTextView right = new RobotoBoldTextView(this);
        right.setText(isBlank(value) ? "—" : value);
        right.setTextSize(13);
        right.setGravity(Gravity.END);
        right.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        right.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1.2f));
        row.addView(right);
        return row;
    }

    /** "Where those units sit" — Available / Reserved / Packed / Damaged / Expired chips. */
    private void buildBreakdown() {
        LinearLayout container = findViewById(R.id.breakdownContainer);
        container.removeAllViews();
        int[] values = getIntent().getIntArrayExtra(EXTRA_BREAKDOWN);
        for (int i = 0; i < BREAKDOWN_KEYS.length; i++) {
            int value = values != null && i < values.length ? values[i] : 0;

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.bg_stat_card);
            card.setPadding(dp(14), dp(10), dp(14), dp(10));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dp(8));
            lp.width = dp(96);
            card.setLayoutParams(lp);

            RobotoBoldTextView count = new RobotoBoldTextView(this);
            count.setText(String.format(Locale.getDefault(), "%,d", value));
            count.setTextSize(18);
            count.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            card.addView(count);

            RobotoLightTextView label = new RobotoLightTextView(this);
            label.setText(BREAKDOWN_KEYS[i]);
            label.setTextSize(11);
            label.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            card.addView(label);

            container.addView(card);
        }
    }

    // ---- History ---------------------------------------------------------

    private void showTab(boolean history) {
        showingHistory = history;
        detailsPane.setVisibility(history ? View.GONE : View.VISIBLE);
        historyPane.setVisibility(history ? View.VISIBLE : View.GONE);
        historyEmpty.setVisibility(View.GONE);

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
        ApiClient.get(this).getStockHistory(productId, HISTORY_LIMIT,
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
        if (movements == null || movements.isEmpty()) {
            historyEmpty.setVisibility(showingHistory ? View.VISIBLE : View.GONE);
            return;
        }
        historyEmpty.setVisibility(View.GONE);
        for (StockMovement movement : movements) {
            container.addView(historyCard(movement));
        }
    }

    private View historyCard(StockMovement movement) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_stat_card);
        card.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(10);
        card.setLayoutParams(lp);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        RobotoTextView type = new RobotoTextView(this);
        type.setText(movement.movementLabel());
        type.setTextSize(10);
        type.setPadding(dp(8), dp(3), dp(8), dp(3));
        type.setBackgroundResource(R.drawable.bg_badge_success);
        type.setTextColor(ContextCompat.getColor(this, R.color.success));
        top.addView(type);

        RobotoBoldTextView units = new RobotoBoldTextView(this);
        units.setText(isBlank(movement.quantity) ? "—" : movement.quantity);
        units.setTextSize(14);
        units.setGravity(Gravity.END);
        units.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        units.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(units);
        card.addView(top);

        RobotoTextView when = new RobotoTextView(this);
        when.setText(joinMeta(movement.created_at, movement.warehouse_name));
        when.setTextSize(11);
        when.setPadding(0, dp(6), 0, 0);
        when.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        card.addView(when);

        String note = joinMeta(movement.notes, movement.user_email);
        if (!isBlank(note)) {
            RobotoLightTextView noteView = new RobotoLightTextView(this);
            noteView.setText(note);
            noteView.setTextSize(11);
            noteView.setPadding(0, dp(2), 0, 0);
            noteView.setTextColor(ContextCompat.getColor(this, R.color.text_tertiary));
            card.addView(noteView);
        }
        return card;
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
