package com.anokix.distributorapp.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.network.ApiCallback;
import com.anokix.distributorapp.network.ApiClient;
import com.anokix.distributorapp.network.dto.InventoryPageData;
import com.anokix.distributorapp.network.dto.StockMovement;
import com.anokix.distributorapp.ui.InventoryDetailActivity;
import com.anokix.distributorapp.ui.NotificationBadge;
import com.anokix.distributorapp.ui.NotificationsActivity;
import com.anokix.distributorapp.ui.adapter.InventoryAdapter;
import com.anokix.distributorapp.ui.util.ContextMenuPopup;
import com.anokix.distributorapp.ui.util.StockActionSheets;
import com.anokix.distributorapp.ui.views.DonutChartView;
import com.anokix.distributorapp.ui.views.RobotoBoldTextView;
import com.anokix.distributorapp.ui.views.RobotoTextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Inventory management (New-Design/Inventory/1.Inventory-page.png): KPI cards, the stock
 * mix donut, stock tabs, keyword search plus category / brand filters, and stock cards
 * with view / more actions.
 *
 * Everything renders from a single GET /inventory/page call and is filtered locally; the
 * stock mutations (adjust, transfer, receive) reload that call on success.
 */
public class InventoryFragment extends Fragment {

    private static final int TAB_ALL = 0;
    private static final int TAB_FAST = 1;
    private static final int TAB_SLOW = 2;
    private static final int TAB_LOW = 3;
    private static final int TAB_OUT = 4;
    private static final int TAB_DAMAGED = 5;
    private static final int TAB_EXPIRED = 6;

    /** The detail screen can adjust or transfer stock, so refresh when it reports a change. */
    private final ActivityResultLauncher<Intent> detailLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK) {
                    loadPage();
                }
            });

    private InventoryAdapter adapter;
    private final List<InventoryPageData.Row> allRows = new ArrayList<>();
    private final List<InventoryPageData.Warehouse> warehouses = new ArrayList<>();
    private final Set<Long> movedProductIds = new HashSet<>();
    private InventoryPageData.Summary summary;
    private List<InventoryPageData.ChartSlice> chart = new ArrayList<>();

    private int selectedTab = TAB_ALL;
    private String query = "";
    private String category = "";
    private String brand = "";

    private final List<TextView> tabLabels = new ArrayList<>();
    private final List<View> tabIndicators = new ArrayList<>();
    private TextView categoryLabel, brandLabel, countLabel;
    private DonutChartView donutChart;
    private LinearLayout donutLegend;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_inventory, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null) {
            NotificationBadge.refresh(requireContext(), getView().findViewById(R.id.notificationBadge));
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.notificationsButton).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), NotificationsActivity.class)));

        RecyclerView list = view.findViewById(R.id.inventoryList);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new InventoryAdapter();
        adapter.setOnRowActionListener(new InventoryAdapter.OnRowAction() {
            @Override
            public void onView(InventoryPageData.Row row) {
                openDetails(row, false);
            }

            @Override
            public void onMore(InventoryPageData.Row row, View anchor) {
                showRowMenu(row, anchor);
            }
        });
        // The KPI cards and donut ride in the list header so the cards keep the rest.
        adapter.setHeaderBinder(this::bindSummaryHeader);
        list.setAdapter(adapter);

        countLabel = view.findViewById(R.id.inventoryCount);
        categoryLabel = view.findViewById(R.id.categoryFilterLabel);
        brandLabel = view.findViewById(R.id.brandFilterLabel);

        view.findViewById(R.id.categoryFilter).setOnClickListener(v -> showCategoryPicker());
        view.findViewById(R.id.brandFilter).setOnClickListener(v -> showBrandPicker());
        view.findViewById(R.id.addStockButton).setOnClickListener(v -> showReceive(null));

        buildTabs(view.findViewById(R.id.inventoryTabs));

        EditText search = view.findViewById(R.id.searchInput);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                query = s == null ? "" : s.toString().trim().toLowerCase(Locale.getDefault());
                applyFilters();
            }
        });

        loadPage();
    }

    // ---- Data ------------------------------------------------------------

    private void loadPage() {
        ApiClient.get(requireContext()).getInventoryPage(new ApiCallback<InventoryPageData>() {
            @Override
            public void onSuccess(InventoryPageData data) {
                if (!isAdded()) {
                    return;
                }
                allRows.clear();
                warehouses.clear();
                movedProductIds.clear();
                if (data != null) {
                    if (data.rows != null) {
                        allRows.addAll(data.rows);
                    }
                    if (data.warehouses != null) {
                        warehouses.addAll(data.warehouses);
                    }
                    if (data.movements != null) {
                        for (StockMovement movement : data.movements) {
                            long productId = parseLong(movement.product_id);
                            if (productId > 0) {
                                movedProductIds.add(productId);
                            }
                        }
                    }
                    summary = data.summary;
                    chart = data.chart == null ? new ArrayList<>() : data.chart;
                }
                applyFilters();          // rebinds the rows …
                adapter.refreshHeader(); // … and the KPI / donut header above them
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    /** Re-reads the page after a stock mutation so every number on screen stays true. */
    private void reloadAfter(@Nullable String message) {
        if (message != null && !message.isEmpty() && isAdded()) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        }
        loadPage();
    }

    // ---- Chrome ----------------------------------------------------------

    private void buildTabs(LinearLayout container) {
        int[] titles = {R.string.tab_all_inventory, R.string.tab_fast_moving, R.string.tab_slow_moving,
                R.string.low_stock, R.string.out_of_stock, R.string.tab_damaged, R.string.expired};
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (int i = 0; i < titles.length; i++) {
            final int index = i;
            LinearLayout tab = new LinearLayout(requireContext());
            tab.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dp(18));
            tab.setLayoutParams(lp);

            RobotoBoldTextView label = new RobotoBoldTextView(requireContext());
            label.setText(titles[i]);
            label.setTextSize(12);
            label.setPadding(0, 0, 0, dp(6));
            label.setGravity(Gravity.CENTER);
            tab.addView(label);

            View indicator = new View(requireContext());
            LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(2));
            indicator.setLayoutParams(ilp);
            indicator.setBackgroundResource(R.drawable.bg_tab_indicator_purple);
            tab.addView(indicator);

            tab.setOnClickListener(v -> {
                selectedTab = index;
                applyFilters();
            });

            container.addView(tab);
            tabLabels.add(label);
            tabIndicators.add(indicator);
        }
        bindTabs();
    }

    private void bindTabs() {
        for (int i = 0; i < tabLabels.size(); i++) {
            boolean selected = i == selectedTab;
            tabLabels.get(i).setTextColor(ContextCompat.getColor(requireContext(),
                    selected ? R.color.purple_primary : R.color.text_secondary));
            tabIndicators.get(i).setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
        }
    }

    /** Fills the list header (KPI cards + stock-mix donut) whenever it is (re)bound. */
    private void bindSummaryHeader(View header) {
        double stockValue = summary == null ? 0 : summary.total_stock_value;
        long units = summary == null ? 0 : summary.total_units;
        int low = summary == null ? 0 : summary.low_stock_count;
        int out = summary == null ? 0 : summary.out_of_stock_count;
        int movements = summary == null ? 0 : summary.movements_this_week;

        ((TextView) header.findViewById(R.id.kpiStockValue))
                .setText(String.format(Locale.US, "R%,.0f", stockValue));
        ((TextView) header.findViewById(R.id.kpiTotalUnits)).setText(count(units));
        ((TextView) header.findViewById(R.id.kpiLowStock)).setText(String.valueOf(low));
        ((TextView) header.findViewById(R.id.kpiOutOfStock)).setText(String.valueOf(out));
        ((TextView) header.findViewById(R.id.kpiMovements)).setText(String.valueOf(movements));

        donutChart = header.findViewById(R.id.inventoryDonutChart);
        donutLegend = header.findViewById(R.id.donutLegend);
        donutChart.setStrokeWidthDp(11f);
        // The donut hole carries the headline number the cards break down.
        donutChart.setCenterText(count(units), getString(R.string.inventory_total_units_short));
        bindDonut();
    }

    /** Donut + legend from the chart slices the API returns (In Stock / Low / Out / Damaged). */
    private void bindDonut() {
        if (donutChart == null || donutLegend == null) {
            return;
        }
        donutLegend.removeAllViews();
        int emptyColor = ContextCompat.getColor(requireContext(), R.color.border);
        if (chart.isEmpty()) {
            donutChart.setData(new float[]{1f}, new int[]{emptyColor});
            return;
        }

        float[] values = new float[chart.size()];
        int[] colors = new int[chart.size()];
        double total = 0;
        for (InventoryPageData.ChartSlice slice : chart) {
            total += Math.max(0, slice.value);
        }
        for (int i = 0; i < chart.size(); i++) {
            InventoryPageData.ChartSlice slice = chart.get(i);
            values[i] = (float) Math.max(0, slice.value);
            colors[i] = InventoryAdapter.parseColor(slice.color,
                    ContextCompat.getColor(requireContext(), R.color.purple_primary));
            donutLegend.addView(legendRow(slice, total, colors[i]));
        }
        donutChart.setData(total <= 0 ? new float[]{1f} : values,
                total <= 0 ? new int[]{emptyColor} : colors);
    }

    private View legendRow(InventoryPageData.ChartSlice slice, double total, int color) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.topMargin = dp(3);
        row.setLayoutParams(rlp);

        View dot = new View(requireContext());
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(8), dp(8));
        dlp.setMarginEnd(dp(8));
        dot.setLayoutParams(dlp);
        dot.setBackgroundResource(R.drawable.bg_legend_dot);
        dot.getBackground().mutate().setTint(color);
        row.addView(dot);

        RobotoTextView title = new RobotoTextView(requireContext());
        title.setText(slice.type);
        title.setTextSize(12);
        title.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        title.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(title);

        RobotoBoldTextView value = new RobotoBoldTextView(requireContext());
        int percent = total > 0 ? (int) Math.round(slice.value * 100 / total) : 0;
        value.setText(String.format(Locale.getDefault(), "%d%%", percent));
        value.setTextSize(12);
        value.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        row.addView(value);
        return row;
    }

    // ---- Filtering -------------------------------------------------------

    private void applyFilters() {
        List<InventoryPageData.Row> filtered = new ArrayList<>();
        for (InventoryPageData.Row row : allRows) {
            if (matchesTab(row) && matchesQuery(row) && matchesCategory(row) && matchesBrand(row)) {
                filtered.add(row);
            }
        }
        adapter.setItems(filtered, getString(allRows.isEmpty()
                ? R.string.no_inventory_yet : R.string.no_inventory_match));
        bindTabs();
        countLabel.setText(getString(R.string.inventory_count, filtered.size(), allRows.size()));
    }

    private boolean matchesTab(InventoryPageData.Row row) {
        switch (selectedTab) {
            case TAB_FAST:
                // "Fast moving" = the product shows up in the movements this page returned.
                return movedProductIds.contains(row.productId);
            case TAB_SLOW:
                return !movedProductIds.contains(row.productId);
            case TAB_LOW:
                return row.isLowStock();
            case TAB_OUT:
                return row.isOutOfStock();
            case TAB_DAMAGED:
                return row.hasDamaged();
            case TAB_EXPIRED:
                return row.hasExpired();
            default:
                return true;
        }
    }

    private boolean matchesQuery(InventoryPageData.Row row) {
        if (query.isEmpty()) {
            return true;
        }
        return contains(row.name) || contains(row.sku) || contains(row.barcode)
                || contains(row.category) || contains(row.brand);
    }

    private boolean matchesCategory(InventoryPageData.Row row) {
        return category.isEmpty() || category.equalsIgnoreCase(row.category);
    }

    private boolean matchesBrand(InventoryPageData.Row row) {
        return brand.isEmpty() || brand.equalsIgnoreCase(row.brand);
    }

    private boolean contains(String value) {
        return value != null && value.toLowerCase(Locale.getDefault()).contains(query);
    }

    private void showCategoryPicker() {
        List<String> labels = collect(true);
        showPicker(R.string.category_filter_title, labels, which -> {
            category = which == 0 ? "" : labels.get(which);
            categoryLabel.setText(labels.get(which));
            applyFilters();
        });
    }

    private void showBrandPicker() {
        List<String> labels = collect(false);
        showPicker(R.string.brand_filter_title, labels, which -> {
            brand = which == 0 ? "" : labels.get(which);
            brandLabel.setText(labels.get(which));
            applyFilters();
        });
    }

    /** "All …" plus the distinct categories / brands present in the loaded rows. */
    private List<String> collect(boolean categories) {
        Set<String> values = new LinkedHashSet<>();
        values.add(getString(categories ? R.string.all_categories : R.string.all_brands));
        for (InventoryPageData.Row row : allRows) {
            String value = categories ? row.category : row.brand;
            if (value != null && !value.isEmpty()) {
                values.add(value);
            }
        }
        return new ArrayList<>(values);
    }

    private void showPicker(int titleRes, List<String> labels, OnPicked callback) {
        new AlertDialog.Builder(requireContext())
                .setTitle(titleRes)
                .setItems(labels.toArray(new String[0]), (d, which) -> callback.onPicked(which))
                .show();
    }

    private interface OnPicked {
        void onPicked(int which);
    }

    // ---- Row actions -----------------------------------------------------

    private void showRowMenu(InventoryPageData.Row row, View anchor) {
        ContextMenuPopup.show(requireActivity(), anchor,
                ContextMenuPopup.MenuItem.of(getString(R.string.adjust_stock),
                        () -> StockActionSheets.showAdjustStock(requireActivity(), row, this::reloadAfter)),
                ContextMenuPopup.MenuItem.of(getString(R.string.transfer_stock),
                        () -> StockActionSheets.showTransfer(requireActivity(), row, warehouses, this::reloadAfter)),
                ContextMenuPopup.MenuItem.of(getString(R.string.view_history),
                        () -> openDetails(row, true)));
    }

    private void showReceive(@Nullable InventoryPageData.Row row) {
        StockActionSheets.showReceive(requireActivity(), warehouses, allRows, row, this::reloadAfter);
    }

    private void openDetails(InventoryPageData.Row row, boolean startOnHistory) {
        detailLauncher.launch(InventoryDetailActivity.intent(
                requireContext(), row, warehouses, startOnHistory));
    }

    // ---- helpers ---------------------------------------------------------

    private String count(long value) {
        return String.format(Locale.getDefault(), "%,d", value);
    }

    private long parseLong(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
