package com.anokix.distributorapp.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.network.ApiCallback;
import com.anokix.distributorapp.network.ApiClient;
import com.anokix.distributorapp.network.dto.OrdersData;
import com.anokix.distributorapp.ui.OrderDetailActivity;
import com.anokix.distributorapp.ui.adapter.OrderAdapter;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Distributor Orders list — wired to GET {@code api/distributor/orders}. Supports a
 * keyword search, a trader filter (discovered from the returned orders), a date range,
 * status tabs with live counts, pagination, and the per-status fulfilment actions
 * (Accept / Reject / Pick / Pack / Mark Ready / Dispatch / Delivered).
 */
public class OrdersFragment extends Fragment implements OrderAdapter.OnOrderAction {

    private static final String[] STATUS_LABELS = {
            "Order Queue", "Accept", "Picking", "Packing", "Ready For Delivery",
            "Out For Delivery", "Delivered", "Cancelled", "Failed"
    };
    private static final String[] STATUS_KEYS = {
            "pending", "accepted", "picking", "packing", "ready_for_delivery",
            "out_for_delivery", "delivered", "cancelled", "failed"
    };
    private static final int PER_PAGE = 10;

    private ApiClient api;

    private final List<OrdersData.Order> orders = new ArrayList<>();
    private OrderAdapter adapter;
    private final Chip[] statusChips = new Chip[STATUS_KEYS.length];

    // Filter state
    private String search = "";
    private String traderId = null;     // null = All Traders
    private String dateFrom, dateTo;
    private String status = "pending";  // Order Queue is the default tab
    private int page = 1;
    private int totalPages = 1;
    private int total = 0;

    /** Trader options discovered from the loaded orders (id → display name). */
    private final Map<String, String> knownTraders = new LinkedHashMap<>();

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    /** First onResume coincides with the initial load in onViewCreated — skip it to avoid a double call. */
    private boolean skipNextResumeLoad = false;

    private RecyclerView list;
    private ProgressBar progress;
    private View emptyView, paginationBar, btnPrev, btnNext;
    private TextView traderLabel, dateRangeLabel, paginationLabel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_orders, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        api = ApiClient.get(requireContext());

        list = view.findViewById(R.id.ordersList);
        progress = view.findViewById(R.id.ordersProgress);
        emptyView = view.findViewById(R.id.ordersEmpty);
        paginationBar = view.findViewById(R.id.paginationBar);
        btnPrev = view.findViewById(R.id.btnPrevPage);
        btnNext = view.findViewById(R.id.btnNextPage);
        traderLabel = view.findViewById(R.id.traderLabel);
        dateRangeLabel = view.findViewById(R.id.dateRangeLabel);
        paginationLabel = view.findViewById(R.id.paginationLabel);

        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new OrderAdapter(orders, "R");
        adapter.setOnOrderAction(this);
        list.setAdapter(adapter);

        buildStatusChips(view);
        setupSearch(view);
        view.findViewById(R.id.traderDropdown).setOnClickListener(this::showTraderMenu);
        view.findViewById(R.id.dateRangeButton).setOnClickListener(v -> showDateRangePicker());

        btnPrev.setOnClickListener(v -> { if (page > 1) { page--; load(); } });
        btnNext.setOnClickListener(v -> { if (page < totalPages) { page++; load(); } });

        // Default range: last 30 days (matches the web portal's first call).
        Calendar cal = Calendar.getInstance();
        dateTo = apiDate(cal.getTimeInMillis());
        cal.add(Calendar.DAY_OF_MONTH, -30);
        dateFrom = apiDate(cal.getTimeInMillis());
        updateDateLabel();

        skipNextResumeLoad = true;   // the onResume right after this would otherwise re-fire load()
        load();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (skipNextResumeLoad) {
            skipNextResumeLoad = false;
            return;
        }
        // Refresh after returning from the detail screen (a status may have changed).
        if (api != null) load();
    }

    // ---- Filters ---------------------------------------------------------

    private void buildStatusChips(View view) {
        ChipGroup group = view.findViewById(R.id.statusChips);
        for (int i = 0; i < STATUS_LABELS.length; i++) {
            final String key = STATUS_KEYS[i];
            Chip chip = new Chip(requireContext());
            chip.setText(STATUS_LABELS[i]);
            chip.setCheckable(true);
            chip.setChecked(key.equals(status));
            chip.setEnsureMinTouchTargetSize(false);
            chip.setChipBackgroundColor(ContextCompat.getColorStateList(requireContext(), R.color.chip_bg_selector));
            chip.setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.chip_text_selector));
            // Portal filter pill: unselected = white + hairline border, selected = brand blue fill.
            chip.setChipStrokeColor(ContextCompat.getColorStateList(requireContext(), R.color.chip_stroke_selector));
            chip.setChipStrokeWidth(getResources().getDisplayMetrics().density);
            chip.setOnClickListener(v -> {
                if (key.equals(status)) return;
                status = key;
                page = 1;
                load();
            });
            statusChips[i] = chip;
            group.addView(chip);
        }
    }

    private void setupSearch(View view) {
        TextView input = view.findViewById(R.id.searchInput);
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override
            public void afterTextChanged(Editable s) {
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                searchRunnable = () -> {
                    search = s.toString().trim();
                    page = 1;
                    load();
                };
                searchHandler.postDelayed(searchRunnable, 400);
            }
        });
    }

    private void showTraderMenu(View anchor) {
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        menu.getMenu().add(0, -1, 0, getString(R.string.all_traders));
        List<String> ids = new ArrayList<>(knownTraders.keySet());
        for (int i = 0; i < ids.size(); i++) {
            menu.getMenu().add(0, i, i + 1, knownTraders.get(ids.get(i)));
        }
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == -1) {
                traderId = null;
                traderLabel.setText(R.string.all_traders);
            } else {
                String id = ids.get(item.getItemId());
                traderId = id;
                traderLabel.setText(knownTraders.get(id));
            }
            page = 1;
            load();
            return true;
        });
        menu.show();
    }

    private void showDateRangePicker() {
        MaterialDatePicker<androidx.core.util.Pair<Long, Long>> picker =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText(R.string.orders_date_range)
                        .build();
        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection == null) return;
            if (selection.first != null) dateFrom = apiDate(selection.first);
            if (selection.second != null) dateTo = apiDate(selection.second);
            updateDateLabel();
            page = 1;
            load();
        });
        picker.show(getParentFragmentManager(), "order_date_range");
    }

    private void updateDateLabel() {
        dateRangeLabel.setText(displayDate(dateFrom) + "  →  " + displayDate(dateTo));
    }

    // ---- Networking ------------------------------------------------------

    private void load() {
        progress.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        api.getOrders(traderId, search, dateFrom, dateTo, status, page, PER_PAGE,
                new ApiCallback<OrdersData>() {
                    @Override
                    public void onSuccess(OrdersData data) {
                        if (!isAdded()) return;
                        progress.setVisibility(View.GONE);
                        bind(data);
                    }

                    @Override
                    public void onError(String message) {
                        if (!isAdded()) return;
                        progress.setVisibility(View.GONE);
                        orders.clear();
                        adapter.notifyDataSetChanged();
                        emptyView.setVisibility(View.VISIBLE);
                        list.setVisibility(View.GONE);
                        paginationBar.setVisibility(View.GONE);
                        Toast.makeText(requireContext(),
                                message != null ? message : getString(R.string.orders_load_failed),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void bind(OrdersData data) {
        orders.clear();
        if (data != null && data.orders != null) {
            orders.addAll(data.orders);
            mergeTraders(data.orders);
        }
        adapter.notifyDataSetChanged();

        emptyView.setVisibility(orders.isEmpty() ? View.VISIBLE : View.GONE);
        list.setVisibility(orders.isEmpty() ? View.GONE : View.VISIBLE);

        updateStatusCounts(data != null ? data.status_counts : null);
        updatePagination(data != null ? data.pagination : null);
        if (!orders.isEmpty()) list.scrollToPosition(0);
    }

    private void mergeTraders(List<OrdersData.Order> list) {
        for (OrdersData.Order o : list) {
            if (o.trader != null && o.trader.id != 0) {
                knownTraders.put(String.valueOf(o.trader.id), o.traderName());
            }
        }
    }

    private void updateStatusCounts(OrdersData.StatusCounts counts) {
        for (int i = 0; i < statusChips.length; i++) {
            int count = counts != null ? counts.forKey(STATUS_KEYS[i]) : 0;
            statusChips[i].setText(STATUS_LABELS[i] + "  " + count);
        }
    }

    private void updatePagination(OrdersData.Pagination pagination) {
        if (pagination == null || pagination.total == 0) {
            paginationBar.setVisibility(View.GONE);
            return;
        }
        page = pagination.page;
        totalPages = Math.max(1, pagination.total_pages);
        total = pagination.total;

        int from = (page - 1) * PER_PAGE + 1;
        int to = Math.min(page * PER_PAGE, total);
        paginationLabel.setText(getString(R.string.orders_pagination, from, to, total));

        paginationBar.setVisibility(View.VISIBLE);
        boolean multi = totalPages > 1;
        btnPrev.setEnabled(page > 1);
        btnPrev.setAlpha(page > 1 ? 1f : 0.4f);
        btnNext.setEnabled(page < totalPages);
        btnNext.setAlpha(page < totalPages ? 1f : 0.4f);
        btnPrev.setVisibility(multi ? View.VISIBLE : View.GONE);
        btnNext.setVisibility(multi ? View.VISIBLE : View.GONE);
    }

    // ---- Order actions ---------------------------------------------------

    @Override
    public void onView(OrdersData.Order order) {
        Intent intent = new Intent(requireContext(), OrderDetailActivity.class);
        intent.putExtra(OrderDetailActivity.EXTRA_ID, String.valueOf(order.id));
        startActivity(intent);
    }

    @Override
    public void onAction(OrdersData.Order order, String action) {
        progress.setVisibility(View.VISIBLE);
        api.orderAction(action, order.id, new ApiCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), actionDoneMessage(action), Toast.LENGTH_SHORT).show();
                load();
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                progress.setVisibility(View.GONE);
                Toast.makeText(requireContext(),
                        message != null ? message : getString(R.string.order_action_failed),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String actionDoneMessage(String action) {
        switch (action) {
            case "accept":   return getString(R.string.order_msg_accepted);
            case "reject":   return getString(R.string.order_msg_rejected);
            case "pick":     return getString(R.string.order_msg_picking);
            case "pack":     return getString(R.string.order_msg_packing);
            case "ready":    return getString(R.string.order_msg_ready);
            case "dispatch": return getString(R.string.order_msg_dispatched);
            case "deliver":  return getString(R.string.order_msg_delivered);
            default:         return getString(R.string.order_msg_updated);
        }
    }

    // ---- Date helpers ----------------------------------------------------

    /** millis → "yyyy-MM-dd" (UTC, matching the picker selection). */
    private static String apiDate(long millis) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(millis);
    }

    /** "yyyy-MM-dd" → "26 May 2026". */
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
