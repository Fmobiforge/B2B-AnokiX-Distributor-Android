package com.anokix.distributorapp.ui;

import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.network.ApiCallback;
import com.anokix.distributorapp.network.ApiClient;
import com.anokix.distributorapp.network.dto.DeliveriesData;
import com.anokix.distributorapp.network.dto.DeliveryDetailData;
import com.anokix.distributorapp.ui.adapter.DeliveryAdapter;
import com.anokix.distributorapp.ui.util.ContextMenuPopup;
import com.anokix.distributorapp.ui.util.FilterUiHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Deliveries list — wired to GET {@code api/distributor/deliveries}. Shows a KPI summary,
 * status tabs with live counts, a date range that drives the API call, and local search +
 * area filtering. Each row opens the detail screen and, when actionable, exposes a Dispatch
 * / Mark Delivered overflow menu.
 */
public class DeliveriesActivity extends BaseSecondaryActivity {

    private static final String[] TAB_KEYS = {
            "all", "in_transit", "delivered", "delayed", "failed", "returned"};

    private DeliveryAdapter adapter;
    private RecyclerView list;
    private TextView emptyView;
    private View loading;
    private TextView[] tabs;
    private TextView areaFilterLabel;

    private final List<DeliveriesData.Delivery> allItems = new ArrayList<>();
    private final List<String> areaOptions = new ArrayList<>();

    private String currentTab = "all";
    private String selectedArea = "";   // "" = All Areas
    private String search = "";
    private String dateFrom = "";
    private String dateTo = "";

    private final SimpleDateFormat apiDate = apiDateFormat();

    private final ActivityResultLauncher<Intent> detailLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) loadData();
            });

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_deliveries;
    }

    @Override
    protected String getScreenTitle() {
        return getString(R.string.deliveries);
    }

    @Override
    protected void bindViews() {
        emptyView = findViewById(R.id.emptyView);
        loading = findViewById(R.id.loading);
        areaFilterLabel = findViewById(R.id.areaFilterLabel);

        list = findViewById(R.id.deliveriesList);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setNestedScrollingEnabled(false);
        adapter = new DeliveryAdapter();
        adapter.setOnView(this::openDetail);
        adapter.setOnMenu(this::showRowMenu);
        list.setAdapter(adapter);

        tabs = new TextView[]{
                findViewById(R.id.tabAll),
                findViewById(R.id.tabInTransit),
                findViewById(R.id.tabDelivered),
                findViewById(R.id.tabDelayed),
                findViewById(R.id.tabFailed),
                findViewById(R.id.tabReturned)};
        for (int i = 0; i < tabs.length; i++) {
            final int index = i;
            tabs[i].setOnClickListener(v -> selectTab(index));
        }

        EditText searchInput = findViewById(R.id.searchInput);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                search = s.toString();
                applyFilter();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        findViewById(R.id.areaFilter).setOnClickListener(v -> showAreaPicker());

        // Default range: the last 90 days drives the initial API call.
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        long end = cal.getTimeInMillis();
        cal.add(Calendar.DAY_OF_YEAR, -90);
        long start = cal.getTimeInMillis();
        dateFrom = apiDate.format(start);
        dateTo = apiDate.format(end);

        FilterUiHelper.bindDateRangePicker(
                this,
                findViewById(R.id.dateRangeButton),
                findViewById(R.id.dateRangeLabel),
                start,
                end,
                (startUtc, endUtc, formatted) -> {
                    dateFrom = apiDate.format(startUtc);
                    dateTo = apiDate.format(endUtc);
                    loadData();
                });
    }

    @Override
    protected void loadData() {
        loading.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        ApiClient.get(this).getDeliveries(dateFrom, dateTo, "", 1, 100,
                new ApiCallback<DeliveriesData>() {
                    @Override
                    public void onSuccess(DeliveriesData data) {
                        loading.setVisibility(View.GONE);
                        bindSummary(data == null ? null : data.summary);
                        bindTabCounts(data == null ? null : data.status_counts);
                        bindAreas(data == null ? null : data.areas);
                        allItems.clear();
                        if (data != null && data.deliveries != null) allItems.addAll(data.deliveries);
                        applyFilter();
                    }

                    @Override
                    public void onError(String message) {
                        loading.setVisibility(View.GONE);
                        allItems.clear();
                        applyFilter();
                        Toast.makeText(DeliveriesActivity.this,
                                message != null ? message : getString(R.string.deliveries_load_failed),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ---- Tabs ------------------------------------------------------------

    private void selectTab(int index) {
        currentTab = TAB_KEYS[index];
        for (int i = 0; i < tabs.length; i++) {
            styleTab(tabs[i], i == index);
        }
        applyFilter();
    }

    private void styleTab(TextView tab, boolean selected) {
        if (selected) {
            tab.setBackgroundResource(R.drawable.bg_segment_tab_selected);
            tab.setTextColor(getResources().getColor(android.R.color.white));
        } else {
            tab.setBackgroundResource(android.R.color.transparent);
            tab.setTextColor(getResources().getColor(R.color.text_primary));
        }
    }

    private void bindTabCounts(DeliveriesData.StatusCounts counts) {
        int[] labels = {R.string.tab_all, R.string.in_transit, R.string.delivered,
                R.string.delayed, R.string.failed_status, R.string.returned_status};
        for (int i = 0; i < tabs.length; i++) {
            int count = counts == null ? 0 : counts.forKey(TAB_KEYS[i]);
            tabs[i].setText(getString(labels[i]) + " (" + count + ")");
        }
    }

    // ---- Area filter -----------------------------------------------------

    private void bindAreas(List<String> areas) {
        areaOptions.clear();
        if (areas != null) {
            for (String a : areas) {
                if (a != null && !a.trim().isEmpty()) areaOptions.add(a);
            }
        }
        // Reset the selection if the chosen area is no longer present.
        if (!selectedArea.isEmpty() && !areaOptions.contains(selectedArea)) {
            selectedArea = "";
            areaFilterLabel.setText(R.string.all_areas);
        }
    }

    private void showAreaPicker() {
        List<String> labels = new ArrayList<>();
        labels.add(getString(R.string.all_areas));
        labels.addAll(areaOptions);
        String[] items = labels.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setItems(items, (d, which) -> {
                    selectedArea = which == 0 ? "" : items[which];
                    areaFilterLabel.setText(items[which]);
                    applyFilter();
                })
                .show();
    }

    // ---- Local search + tab + area filter --------------------------------

    private void applyFilter() {
        String q = search.trim().toLowerCase(Locale.getDefault());
        List<DeliveriesData.Delivery> out = new ArrayList<>();
        for (DeliveriesData.Delivery d : allItems) {
            if (!"all".equals(currentTab)
                    && !currentTab.equalsIgnoreCase(d.status == null ? "" : d.status)) {
                continue;
            }
            if (!selectedArea.isEmpty() && !selectedArea.equalsIgnoreCase(d.area == null ? "" : d.area)) {
                continue;
            }
            if (!q.isEmpty() && !matchesSearch(d, q)) {
                continue;
            }
            out.add(d);
        }
        adapter.setItems(out);
        boolean empty = out.isEmpty();
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        list.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private boolean matchesSearch(DeliveriesData.Delivery d, String q) {
        return contains(d.delivery_number, q)
                || contains(d.order_number, q)
                || contains(d.trader_name, q);
    }

    private static boolean contains(String value, String q) {
        return value != null && value.toLowerCase(Locale.getDefault()).contains(q);
    }

    // ---- Row actions -----------------------------------------------------

    private void openDetail(DeliveriesData.Delivery d) {
        detailLauncher.launch(new Intent(this, DeliveryDetailActivity.class)
                .putExtra(DeliveryDetailActivity.EXTRA_ID, String.valueOf(d.id)));
    }

    private void showRowMenu(View anchor, DeliveriesData.Delivery d) {
        String action = DeliveryAdapter.actionFor(d.raw_status);
        if (action == null) return;
        String label = "dispatch".equals(action)
                ? getString(R.string.dispatch)
                : getString(R.string.mark_delivered);
        ContextMenuPopup.show(this, anchor,
                ContextMenuPopup.MenuItem.of(label, () -> performAction(d, action)));
    }

    private void performAction(DeliveriesData.Delivery d, String action) {
        loading.setVisibility(View.VISIBLE);
        ApiClient.get(this).deliveryAction(action, d.id, new ApiCallback<DeliveryDetailData>() {
            @Override
            public void onSuccess(DeliveryDetailData data) {
                int msg = "dispatch".equals(action)
                        ? R.string.delivery_dispatched_msg
                        : R.string.delivery_delivered_msg;
                Toast.makeText(DeliveriesActivity.this, msg, Toast.LENGTH_SHORT).show();
                loadData();
            }

            @Override
            public void onError(String message) {
                loading.setVisibility(View.GONE);
                Toast.makeText(DeliveriesActivity.this,
                        message != null ? message : getString(R.string.deliveries_load_failed),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    // ---- Summary cards ---------------------------------------------------

    private void bindSummary(DeliveriesData.Summary s) {
        if (s == null) return;
        ((TextView) findViewById(R.id.valTotalDeliveries)).setText(String.valueOf(s.total_deliveries));
        ((TextView) findViewById(R.id.valOnTime)).setText(String.valueOf(s.on_time));
        ((TextView) findViewById(R.id.subOnTime)).setText(pct(s.on_time_pct));
        ((TextView) findViewById(R.id.valInTransit)).setText(String.valueOf(s.in_transit));
        ((TextView) findViewById(R.id.subInTransit)).setText(pct(s.in_transit_pct));
        ((TextView) findViewById(R.id.valDelayed)).setText(String.valueOf(s.delayed));
        ((TextView) findViewById(R.id.subDelayed)).setText(pct(s.delayed_pct));
        ((TextView) findViewById(R.id.valFailed)).setText(String.valueOf(s.failed));
        ((TextView) findViewById(R.id.subFailed)).setText(pct(s.failed_pct));
    }

    private static String pct(double value) {
        if (value == Math.rint(value)) {
            return String.format(Locale.US, "%d%%", (long) value);
        }
        return String.format(Locale.US, "%.1f%%", value);
    }

    private static SimpleDateFormat apiDateFormat() {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f;
    }
}
