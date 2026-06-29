package com.anokix.distributor.ui;

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

import com.anokix.distributor.R;
import com.anokix.distributor.model.OrderFormat;
import com.anokix.distributor.network.ApiCallback;
import com.anokix.distributor.network.ApiClient;
import com.anokix.distributor.network.dto.ReturnDetailData;
import com.anokix.distributor.network.dto.ReturnsData;
import com.anokix.distributor.ui.adapter.ReturnAdapter;
import com.anokix.distributor.ui.util.ContextMenuPopup;
import com.anokix.distributor.ui.util.FilterUiHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Returns list — wired to GET {@code api/distributor/grns}. Shows a KPI summary, status tabs
 * with live counts, a date range that drives the API call, and local search + reason filtering.
 * Each row opens the detail screen and, when pending, exposes an Approve / Reject overflow menu.
 */
public class ReturnsActivity extends BaseSecondaryActivity {

    private static final String[] TAB_KEYS = {"all", "pending", "approved", "rejected"};

    /** Reason filter labels mirror the web dropdown; "" = All Reasons. */
    private static final String[] REASON_LABELS = {
            "Damaged Items", "Expired Items", "Incorrect Item", "Overstock", "Supplier Recall", "Other"};

    private ReturnAdapter adapter;
    private RecyclerView list;
    private TextView emptyView;
    private View loading;
    private TextView[] tabs;
    private TextView reasonFilterLabel;

    private final List<ReturnsData.Return> allItems = new ArrayList<>();

    private String currentTab = "all";
    private String selectedReason = "";   // "" = All Reasons
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
        return R.layout.activity_returns;
    }

    @Override
    protected String getScreenTitle() {
        return getString(R.string.returns);
    }

    @Override
    protected void bindViews() {
        emptyView = findViewById(R.id.emptyView);
        loading = findViewById(R.id.loading);
        reasonFilterLabel = findViewById(R.id.reasonFilterLabel);

        list = findViewById(R.id.returnsList);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setNestedScrollingEnabled(false);
        adapter = new ReturnAdapter();
        adapter.setOnView(this::openDetail);
        adapter.setOnMenu(this::showRowMenu);
        list.setAdapter(adapter);

        tabs = new TextView[]{
                findViewById(R.id.tabAll),
                findViewById(R.id.tabPending),
                findViewById(R.id.tabApproved),
                findViewById(R.id.tabRejected)};
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

        findViewById(R.id.reasonFilter).setOnClickListener(v -> showReasonPicker());

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
        ApiClient.get(this).getReturns(dateFrom, dateTo, "", 1, 100,
                new ApiCallback<ReturnsData>() {
                    @Override
                    public void onSuccess(ReturnsData data) {
                        loading.setVisibility(View.GONE);
                        bindSummary(data == null ? null : data.summary);
                        bindTabCounts(data == null ? null : data.status_counts);
                        allItems.clear();
                        if (data != null && data.returns != null) allItems.addAll(data.returns);
                        applyFilter();
                    }

                    @Override
                    public void onError(String message) {
                        loading.setVisibility(View.GONE);
                        allItems.clear();
                        applyFilter();
                        Toast.makeText(ReturnsActivity.this,
                                message != null ? message : getString(R.string.returns_load_failed),
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

    private void bindTabCounts(ReturnsData.StatusCounts counts) {
        int[] labels = {R.string.all_returns, R.string.pending, R.string.approved, R.string.rejected};
        for (int i = 0; i < tabs.length; i++) {
            int count = counts == null ? 0 : counts.forKey(TAB_KEYS[i]);
            tabs[i].setText(getString(labels[i]) + " (" + count + ")");
        }
    }

    // ---- Reason filter ---------------------------------------------------

    private void showReasonPicker() {
        List<String> labels = new ArrayList<>();
        labels.add(getString(R.string.all_reasons));
        for (String r : REASON_LABELS) labels.add(r);
        String[] items = labels.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setItems(items, (d, which) -> {
                    selectedReason = which == 0 ? "" : items[which];
                    reasonFilterLabel.setText(items[which]);
                    applyFilter();
                })
                .show();
    }

    // ---- Local search + tab + reason filter ------------------------------

    private void applyFilter() {
        String q = search.trim().toLowerCase(Locale.getDefault());
        List<ReturnsData.Return> out = new ArrayList<>();
        for (ReturnsData.Return r : allItems) {
            if (!"all".equals(currentTab)
                    && !currentTab.equalsIgnoreCase(r.status == null ? "" : r.status)) {
                continue;
            }
            if (!selectedReason.isEmpty()
                    && !selectedReason.equalsIgnoreCase(r.reason_label == null ? "" : r.reason_label)) {
                continue;
            }
            if (!q.isEmpty() && !matchesSearch(r, q)) {
                continue;
            }
            out.add(r);
        }
        adapter.setItems(out);
        boolean empty = out.isEmpty();
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        list.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private boolean matchesSearch(ReturnsData.Return r, String q) {
        return contains(r.grn_number, q)
                || contains(r.trader_name, q)
                || contains(r.trader_phone, q);
    }

    private static boolean contains(String value, String q) {
        return value != null && value.toLowerCase(Locale.getDefault()).contains(q);
    }

    // ---- Row actions -----------------------------------------------------

    private void openDetail(ReturnsData.Return r) {
        detailLauncher.launch(new Intent(this, ReturnDetailActivity.class)
                .putExtra(ReturnDetailActivity.EXTRA_ID, String.valueOf(r.id)));
    }

    private void showRowMenu(View anchor, ReturnsData.Return r) {
        ContextMenuPopup.show(this, anchor,
                ContextMenuPopup.MenuItem.of(getString(R.string.approve),
                        () -> promptAction(r, "approve")),
                ContextMenuPopup.MenuItem.destructive(getString(R.string.reject),
                        () -> promptAction(r, "reject")));
    }

    /** Ask for an optional decision note, then run the approve / reject. */
    private void promptAction(ReturnsData.Return r, String action) {
        final EditText input = new EditText(this);
        input.setHint(R.string.decision_note_hint);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);
        boolean approve = "approve".equals(action);
        new AlertDialog.Builder(this)
                .setTitle(approve ? R.string.approve_return : R.string.reject_return)
                .setView(input)
                .setPositiveButton(approve ? R.string.approve : R.string.reject,
                        (d, w) -> performAction(r, action, input.getText().toString().trim()))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void performAction(ReturnsData.Return r, String action, String note) {
        loading.setVisibility(View.VISIBLE);
        ApiClient.get(this).returnAction(action, r.id, note, new ApiCallback<ReturnDetailData>() {
            @Override
            public void onSuccess(ReturnDetailData data) {
                int msg = "approve".equals(action)
                        ? R.string.return_approved_msg : R.string.return_rejected_msg;
                Toast.makeText(ReturnsActivity.this, msg, Toast.LENGTH_SHORT).show();
                loadData();
            }

            @Override
            public void onError(String message) {
                loading.setVisibility(View.GONE);
                Toast.makeText(ReturnsActivity.this,
                        message != null ? message : getString(R.string.return_action_failed),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    // ---- Summary cards ---------------------------------------------------

    private void bindSummary(ReturnsData.Summary s) {
        if (s == null) return;
        ((TextView) findViewById(R.id.valTotalReturns)).setText(String.valueOf(s.total_returns));
        ((TextView) findViewById(R.id.valApproved)).setText(String.valueOf(s.approved));
        ((TextView) findViewById(R.id.valPending)).setText(String.valueOf(s.pending));
        ((TextView) findViewById(R.id.valRejected)).setText(String.valueOf(s.rejected));
        ((TextView) findViewById(R.id.valCreditIssued)).setText(OrderFormat.money(s.total_value, "R"));
    }

    private static SimpleDateFormat apiDateFormat() {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f;
    }
}
