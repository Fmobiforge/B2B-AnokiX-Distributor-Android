package com.anokix.distributorapp.ui;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.model.FinanceFormat;
import com.anokix.distributorapp.network.ApiCallback;
import com.anokix.distributorapp.network.ApiClient;
import com.anokix.distributorapp.network.dto.FinancesData;
import com.anokix.distributorapp.ui.adapter.FinanceTransactionAdapter;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Finances ledger — the distributor's anokiX wallet (GET api/distributor/finances). Shows a KPI
 * strip from the API summary plus the transaction list. The endpoint returns the full ledger, so
 * the search box, Status / Type / Payment-Method dropdowns and the date range all filter the
 * loaded list locally. Each row opens a detail bottom sheet (View / tap) or the ⋮ menu.
 * UI modelled on the Trader app's Finances screen.
 */
public class FinancesActivity extends BaseSecondaryActivity {

    private static final String[] STATUS_KEYS   = {"all", "paid", "pending", "overdue", "partial"};
    private static final String[] STATUS_LABELS = {"All Statuses", "Paid", "Pending", "Overdue", "Partial"};
    private static final String[] TYPE_KEYS      = {"all", "invoice", "payment", "payout", "credit_note"};
    private static final String[] TYPE_LABELS    = {"All Types", "Invoice", "Payment", "Payout", "Credit Note"};
    private static final String[] METHOD_KEYS    = {"all", "eft", "card", "cash", "credit"};
    private static final String[] METHOD_LABELS  = {"All Methods", "EFT", "Card", "Cash", "Credit"};

    private final List<FinancesData.Transaction> all = new ArrayList<>();
    private final List<FinancesData.Transaction> filtered = new ArrayList<>();
    private FinancesData.Summary summary;
    private FinanceTransactionAdapter adapter;

    private String search = "";
    private String statusFilter = "all";
    private String typeFilter = "all";
    private String methodFilter = "all";
    private String dateFrom, dateTo; // "yyyy-MM-dd" local filter, null = no range

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView list;
    private View loading, emptyView;
    private TextView statusLabel, typeLabel, methodLabel, dateRangeLabel, dateClear, resultCount;

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_finances;
    }

    @Override
    protected String getScreenTitle() {
        return getString(R.string.finances);
    }

    @Override
    protected String getScreenSubtitle() {
        return getString(R.string.finances_subtitle);
    }

    @Override
    protected void bindViews() {
        loading = findViewById(R.id.loading);
        emptyView = findViewById(R.id.emptyView);
        statusLabel = findViewById(R.id.statusLabel);
        typeLabel = findViewById(R.id.typeLabel);
        methodLabel = findViewById(R.id.methodLabel);
        dateRangeLabel = findViewById(R.id.dateRangeLabel);
        dateClear = findViewById(R.id.dateClear);
        resultCount = findViewById(R.id.resultCount);

        list = findViewById(R.id.txnList);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FinanceTransactionAdapter();
        adapter.setOnView(this::showDetail);
        adapter.setOnRowClick(this::showDetail);
        adapter.setOnMenu(this::showRowMenu);
        list.setAdapter(adapter);

        swipeRefresh = findViewById(R.id.swipeRefresh);
        swipeRefresh.setColorSchemeResources(R.color.purple_primary);
        swipeRefresh.setOnRefreshListener(() -> load(false));

        setupKpiIcons();
        setupSearch();
        setupFilters();
    }

    @Override
    protected void loadData() {
        load(true);
    }

    // ---- KPI cards -----------------------------------------------------------

    private void setupKpiIcons() {
        styleKpi(R.id.kpiRevenue,    R.drawable.ic_finances,       R.color.chart_violet,   R.color.chart_violet_bg);
        styleKpi(R.id.kpiCommission, R.drawable.ic_finance_report, R.color.warning,        R.color.warning_bg);
        styleKpi(R.id.kpiReceived,   R.drawable.ic_wallet,         R.color.success,        R.color.success_bg);
        styleKpi(R.id.kpiPending,    R.drawable.ic_clock,          R.color.info,           R.color.info_bg);
        styleKpi(R.id.kpiOverdue,    R.drawable.ic_warning_box,    R.color.danger,         R.color.danger_bg);
    }

    private void styleKpi(int includeId, @DrawableRes int icon, @ColorRes int tint, @ColorRes int bg) {
        View card = findViewById(includeId);
        ImageView iv = card.findViewById(R.id.kpiIcon);
        iv.setImageResource(icon);
        iv.setImageTintList(ContextCompat.getColorStateList(this, tint));
        card.findViewById(R.id.kpiIconTile)
                .setBackgroundTintList(ContextCompat.getColorStateList(this, bg));
    }

    private void bindKpi(int includeId, String value, String label, String sub) {
        View card = findViewById(includeId);
        ((TextView) card.findViewById(R.id.kpiValue)).setText(value);
        ((TextView) card.findViewById(R.id.kpiLabel)).setText(label);
        ((TextView) card.findViewById(R.id.kpiSub)).setText(sub);
    }

    private void bindSummary() {
        double revenueMonth = summary != null ? summary.received_month : 0;
        double commission = summary != null ? summary.commission_month : 0;
        double received = summary != null ? summary.received : 0;
        double pending = summary != null ? summary.pending : 0;
        double overdue = summary != null ? summary.overdue : 0;
        int count = summary != null ? summary.count : all.size();

        bindKpi(R.id.kpiRevenue, money(revenueMonth), "Revenue (This Month)", "Received this month");
        bindKpi(R.id.kpiCommission, money(commission), "Commission (This Month)", "Platform fee this month");
        bindKpi(R.id.kpiReceived, money(received), "Received Payments",
                count + (count == 1 ? " transaction" : " transactions"));
        bindKpi(R.id.kpiPending, money(pending), "Pending", "Awaiting settlement");
        bindKpi(R.id.kpiOverdue, money(overdue), "Overdue Amount",
                overdue <= 0 ? "None overdue" : "Awaiting payment");
    }

    // ---- Filters -------------------------------------------------------------

    private void setupSearch() {
        EditText input = findViewById(R.id.searchInput);
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                search = s.toString().trim().toLowerCase(Locale.US);
                applyFilters();
            }
        });
    }

    private void setupFilters() {
        findViewById(R.id.statusDropdown).setOnClickListener(v ->
                showFilterMenu(v, STATUS_KEYS, STATUS_LABELS, statusFilter, key -> {
                    statusFilter = key;
                    statusLabel.setText(labelFor(key, STATUS_KEYS, STATUS_LABELS, getString(R.string.finance_status_label)));
                    setPillActive(R.id.statusDropdown, R.id.statusLabel, R.id.statusChevron, !"all".equals(key));
                    applyFilters();
                }));
        findViewById(R.id.typeDropdown).setOnClickListener(v ->
                showFilterMenu(v, TYPE_KEYS, TYPE_LABELS, typeFilter, key -> {
                    typeFilter = key;
                    typeLabel.setText(labelFor(key, TYPE_KEYS, TYPE_LABELS, getString(R.string.finance_type_label)));
                    setPillActive(R.id.typeDropdown, R.id.typeLabel, R.id.typeChevron, !"all".equals(key));
                    applyFilters();
                }));
        findViewById(R.id.methodDropdown).setOnClickListener(v ->
                showFilterMenu(v, METHOD_KEYS, METHOD_LABELS, methodFilter, key -> {
                    methodFilter = key;
                    methodLabel.setText(labelFor(key, METHOD_KEYS, METHOD_LABELS, getString(R.string.finance_method_label)));
                    setPillActive(R.id.methodDropdown, R.id.methodLabel, R.id.methodChevron, !"all".equals(key));
                    applyFilters();
                }));

        findViewById(R.id.dateRangePill).setOnClickListener(v -> showDateRangePicker());
        dateClear.setOnClickListener(v -> {
            dateFrom = null;
            dateTo = null;
            dateRangeLabel.setText(R.string.finance_date_range);
            dateClear.setVisibility(View.GONE);
            findViewById(R.id.dateRangePill).setBackgroundResource(R.drawable.bg_filter_pill);
            applyFilters();
        });
    }

    /** Toggle a filter pill between the neutral (white) and active (purple) look. */
    private void setPillActive(int pillId, int labelId, int chevronId, boolean active) {
        findViewById(pillId).setBackgroundResource(
                active ? R.drawable.bg_filter_pill_active : R.drawable.bg_filter_pill);
        ((TextView) findViewById(labelId)).setTextColor(ContextCompat.getColor(this,
                active ? R.color.purple_primary : R.color.text_primary));
        ((ImageView) findViewById(chevronId)).setImageTintList(ContextCompat.getColorStateList(this,
                active ? R.color.purple_primary : R.color.text_secondary));
    }

    private interface FilterPick { void onPick(String key); }

    private void showFilterMenu(View anchor, String[] keys, String[] labels, String selected, FilterPick cb) {
        PopupMenu menu = new PopupMenu(this, anchor);
        for (int i = 0; i < keys.length; i++) {
            menu.getMenu().add(0, i, i, labels[i]).setCheckable(true).setChecked(keys[i].equals(selected));
        }
        menu.setOnMenuItemClickListener(item -> {
            cb.onPick(keys[item.getItemId()]);
            return true;
        });
        menu.show();
    }

    /** For a non-"all" key show its short label on the pill; otherwise the default header. */
    private static String labelFor(String key, String[] keys, String[] labels, String fallback) {
        if ("all".equals(key)) return fallback;
        for (int i = 0; i < keys.length; i++) {
            if (keys[i].equals(key)) return labels[i];
        }
        return fallback;
    }

    private void showDateRangePicker() {
        com.google.android.material.datepicker.MaterialDatePicker.Builder<androidx.core.util.Pair<Long, Long>> b =
                com.google.android.material.datepicker.MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText(R.string.finance_date_range);
        com.google.android.material.datepicker.MaterialDatePicker<androidx.core.util.Pair<Long, Long>> picker = b.build();
        picker.addOnPositiveButtonClickListener(sel -> {
            if (sel == null) return;
            if (sel.first != null) dateFrom = apiDate(sel.first);
            if (sel.second != null) dateTo = apiDate(sel.second);
            dateRangeLabel.setText(displayDate(dateFrom) + "  →  " + displayDate(dateTo));
            dateClear.setVisibility(View.VISIBLE);
            findViewById(R.id.dateRangePill).setBackgroundResource(R.drawable.bg_filter_pill_active);
            applyFilters();
        });
        picker.show(getSupportFragmentManager(), "finance_date_range");
    }

    /** Rebuild {@link #filtered} from {@link #all} using the current filter state. */
    private void applyFilters() {
        filtered.clear();
        for (FinancesData.Transaction t : all) {
            if (!matchesSearch(t)) continue;
            if (!"all".equals(statusFilter) && !statusFilter.equalsIgnoreCase(nz(t.status))) continue;
            if (!"all".equals(typeFilter) && !typeFilter.equals(FinanceFormat.typeCategory(t.type))) continue;
            // Payment method has no API field yet — a specific pick only excludes rows that
            // actually carry a (different) method, so the list stays useful until the backend adds it.
            if (!"all".equals(methodFilter) && t.payment_method != null && !t.payment_method.isEmpty()
                    && !methodFilter.equalsIgnoreCase(t.payment_method)) continue;
            if (!withinDateRange(t)) continue;
            filtered.add(t);
        }
        adapter.setItems(filtered);
        int n = filtered.size();
        resultCount.setText("Showing " + n + (n == 1 ? " transaction" : " transactions"));
        boolean empty = filtered.isEmpty();
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        list.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private boolean matchesSearch(FinancesData.Transaction t) {
        if (search.isEmpty()) return true;
        return contains(t.reference) || contains(t.order_number)
                || contains(t.trader_name) || contains(t.description)
                || contains(t.trader_phone);
    }

    private boolean contains(String field) {
        return field != null && field.toLowerCase(Locale.US).contains(search);
    }

    private boolean withinDateRange(FinancesData.Transaction t) {
        if (dateFrom == null && dateTo == null) return true;
        String day = dayOf(t.created_at);
        if (day == null) return false;
        if (dateFrom != null && day.compareTo(dateFrom) < 0) return false;
        if (dateTo != null && day.compareTo(dateTo) > 0) return false;
        return true;
    }

    // ---- Data ----------------------------------------------------------------

    private void load(boolean showSpinner) {
        if (showSpinner) {
            loading.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            list.setVisibility(View.GONE);
        }
        ApiClient.get(this).getFinances("", "", new ApiCallback<FinancesData>() {
            @Override
            public void onSuccess(FinancesData data) {
                loading.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                all.clear();
                if (data != null && data.transactions != null) all.addAll(data.transactions);
                summary = data != null ? data.summary : null;
                bindSummary();
                applyFilters();
            }

            @Override
            public void onError(String message) {
                loading.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(FinancesActivity.this,
                        message != null ? message : getString(R.string.finances_load_failed),
                        Toast.LENGTH_SHORT).show();
                bindSummary();
                applyFilters();
            }
        });
    }

    // ---- Row menu + detail sheet --------------------------------------------

    private void showRowMenu(View anchor, FinancesData.Transaction t) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(0, 0, 0, getString(R.string.finance_view_details));
        menu.getMenu().add(0, 1, 1, getString(R.string.download_pdf));
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 0) {
                showDetail(t);
            } else {
                Toast.makeText(this, R.string.finance_pdf_coming_soon, Toast.LENGTH_SHORT).show();
            }
            return true;
        });
        menu.show();
    }

    private void showDetail(FinancesData.Transaction t) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View content = LayoutInflater.from(this).inflate(R.layout.sheet_finance_detail, null, false);

        TextView type = content.findViewById(R.id.detailType);
        type.setText(FinanceFormat.typeLabel(t.type));
        type.setBackgroundResource(FinanceFormat.typeBadgeBg(t.type));
        type.setTextColor(ContextCompat.getColor(this, FinanceFormat.typeColorRes(t.type)));

        TextView status = content.findViewById(R.id.detailStatus);
        status.setText(FinanceFormat.statusLabel(t.status));
        status.setBackgroundResource(FinanceFormat.statusBadgeBg(t.status));
        status.setTextColor(ContextCompat.getColor(this, FinanceFormat.statusColorRes(t.status)));

        TextView amount = content.findViewById(R.id.detailAmount);
        amount.setText(FinanceTransactionAdapter.signedMoney(t.amount, t.currency));
        amount.setTextColor(ContextCompat.getColor(this, t.amount < 0 ? R.color.danger : R.color.success));

        ((TextView) content.findViewById(R.id.detailDescription)).setText(nzDash(t.description));

        ViewGroup rows = content.findViewById(R.id.detailRows);
        addRow(rows, "Reference", nzDash(t.reference));
        addRow(rows, "Order", nzDash(t.order_number));
        addRow(rows, "Trader", nzDash(t.trader_name));
        if (t.trader_phone != null && !t.trader_phone.isEmpty()) {
            addRow(rows, "Phone", t.trader_phone);
        }
        addRow(rows, "Method", methodDisplay(t));
        addRow(rows, "Date", formatDateTime(t.created_at));

        content.findViewById(R.id.btnDownloadPdf).setOnClickListener(v -> {
            dialog.dismiss();
            Toast.makeText(this, R.string.finance_pdf_coming_soon, Toast.LENGTH_SHORT).show();
        });

        dialog.setContentView(content);
        dialog.show();
    }

    private void addRow(ViewGroup parent, String label, String value) {
        View row = LayoutInflater.from(this).inflate(R.layout.sheet_finance_detail_row, parent, false);
        ((TextView) row.findViewById(R.id.rowLabel)).setText(label);
        ((TextView) row.findViewById(R.id.rowValue)).setText(value);
        parent.addView(row);
    }

    private String methodDisplay(FinancesData.Transaction t) {
        if (t.payment_method != null && !t.payment_method.isEmpty()) return t.payment_method;
        return getString(R.string.finance_wallet_method);
    }

    // ---- Helpers -------------------------------------------------------------

    private static String nz(String s) { return s == null ? "" : s; }

    private static String nzDash(String s) { return s == null || s.trim().isEmpty() ? "—" : s; }

    private String money(double value) {
        return String.format(Locale.US, "R%,.2f", value);
    }

    private static Date parse(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(raw);
        } catch (Exception e) {
            return null;
        }
    }

    /** created_at → "yyyy-MM-dd" for range comparison (null on failure). */
    private static String dayOf(String raw) {
        Date d = parse(raw);
        if (d == null) return null;
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(d);
    }

    /** created_at → "15 Jul 2026, 13:58". */
    private static String formatDateTime(String raw) {
        Date d = parse(raw);
        if (d == null) return raw == null || raw.isEmpty() ? "—" : raw;
        return new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(d);
    }

    /** picker millis (UTC midnight) → "yyyy-MM-dd". */
    private static String apiDate(long millis) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(millis);
    }

    /** "yyyy-MM-dd" → "15 Jul 2026". */
    private static String displayDate(String raw) {
        if (raw == null) return "";
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            in.setTimeZone(TimeZone.getTimeZone("UTC"));
            SimpleDateFormat out = new SimpleDateFormat("dd MMM yyyy", Locale.US);
            out.setTimeZone(TimeZone.getTimeZone("UTC"));
            return out.format(in.parse(raw));
        } catch (Exception e) {
            return raw;
        }
    }
}
