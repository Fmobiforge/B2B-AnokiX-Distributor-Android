package com.anokix.distributorapp.ui;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
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
import com.anokix.distributorapp.data.MockData;
import com.anokix.distributorapp.network.ApiCallback;
import com.anokix.distributorapp.network.ApiClient;
import com.anokix.distributorapp.network.dto.TradersData;
import com.anokix.distributorapp.ui.adapter.TraderAdapter;
import com.anokix.distributorapp.ui.util.ContextMenuPopup;
import com.anokix.distributorapp.ui.util.FilterUiHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Trader Management (New-Design/Traders/1.Traders-Dashboard.png).
 *
 * <p>Keyword search plus the Status and Credit Status dropdowns all go to the server,
 * so the list, the KPI cards and the result count always describe the same query. The
 * web layout's tab strip is intentionally not ported — the two dropdowns already cover
 * it, and a second scrolling row would cost a thumb-height of phone screen for nothing.
 *
 * <p>Each card's overflow button opens Edit / Delete (2.Trader-Item-Menu.png); deleting
 * asks for confirmation first (3.Confirm-Deletion.png).
 */
public class TradersActivity extends BaseSecondaryActivity {

    private static final String ALL_STATUS = "All Status";
    private static final String ALL_CREDIT_STATUS = "All Credit Status";
    /** Wait for a pause in typing before spending a request on the keyword. */
    private static final long SEARCH_DEBOUNCE_MS = 350;

    private TraderAdapter adapter;
    private TextView countLabel;
    private EditText searchInput;
    private View searchClear;

    private final List<TradersData.Trader> traders = new ArrayList<>();
    private TradersData.Summary summary;

    // Server-side filters: empty string means "no filter".
    private String search = "";
    private String selectedStatus = "";
    private String selectedCreditStatus = "";

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch;

    private final ActivityResultLauncher<Intent> traderFormLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    loadData(); // refresh after a trader is registered or edited
                }
            });

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_traders;
    }

    @Override
    protected String getScreenTitle() {
        return getString(R.string.traders);
    }

    @Override
    protected String getScreenSubtitle() {
        return getString(R.string.traders_subtitle);
    }

    @Override
    protected void bindViews() {
        RecyclerView list = findViewById(R.id.tradersList);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TraderAdapter();
        adapter.setOnRowActionListener(this::showRowMenu);
        // The KPI cards ride in the list header so the trader cards keep the rest.
        adapter.setHeaderBinder(this::bindSummaryHeader);
        list.setAdapter(adapter);

        countLabel = findViewById(R.id.traderCount);

        findViewById(R.id.addTraderButton).setOnClickListener(v ->
                traderFormLauncher.launch(new Intent(this, CreateTraderActivity.class)));

        bindSearch();
        bindFilters();
    }

    private void bindSearch() {
        searchInput = findViewById(R.id.searchInput);
        searchClear = findViewById(R.id.searchClear);
        searchClear.setOnClickListener(v -> searchInput.setText(""));

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                String keyword = s == null ? "" : s.toString().trim();
                searchClear.setVisibility(keyword.isEmpty() ? View.GONE : View.VISIBLE);
                if (keyword.equals(search)) {
                    return;
                }
                if (pendingSearch != null) {
                    searchHandler.removeCallbacks(pendingSearch);
                }
                pendingSearch = () -> {
                    search = keyword;
                    loadData();
                };
                searchHandler.postDelayed(pendingSearch, SEARCH_DEBOUNCE_MS);
            }
        });
    }

    private void bindFilters() {
        TextView statusLabel = findViewById(R.id.statusFilterLabel);
        TextView creditLabel = findViewById(R.id.creditFilterLabel);
        FilterUiHelper.setDropdownContentDescription(statusLabel, getString(R.string.filter_status));
        FilterUiHelper.setDropdownContentDescription(creditLabel,
                getString(R.string.filter_credit_status));

        // Status filter → ?status=
        FilterUiHelper.bindDropdown(
                findViewById(R.id.statusFilterButton),
                statusLabel,
                MockData.getFilterStatus(),
                0,
                (index, value) -> {
                    selectedStatus = ALL_STATUS.equals(value) ? "" : value;
                    loadData();
                });

        // Credit status filter → ?credit_status=
        FilterUiHelper.bindDropdown(
                findViewById(R.id.creditFilterButton),
                creditLabel,
                MockData.getFilterCreditStatus(),
                0,
                (index, value) -> {
                    selectedCreditStatus = ALL_CREDIT_STATUS.equals(value) ? "" : value;
                    loadData();
                });
    }

    @Override
    protected void loadData() {
        ApiClient.get(this).getTraders(search, selectedStatus, selectedCreditStatus,
                new ApiCallback<TradersData>() {
                    @Override
                    public void onSuccess(TradersData data) {
                        traders.clear();
                        if (data != null && data.traders != null) {
                            traders.addAll(data.traders);
                        }
                        summary = data == null ? null : data.summary;
                        render(data == null ? traders.size() : data.total);
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(TradersActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void render(int total) {
        boolean filtering = !search.isEmpty() || !selectedStatus.isEmpty()
                || !selectedCreditStatus.isEmpty();
        // Rebinds every row, so the KPI header at position 0 picks up the new summary too.
        adapter.setItems(traders, getString(filtering
                ? R.string.traders_empty_filtered : R.string.traders_empty));
        countLabel.setText(getString(R.string.traders_showing_count,
                traders.size(), Math.max(total, traders.size())));
    }

    /** Paints the four KPI cards from the summary block the list call returns. */
    private void bindSummaryHeader(View header) {
        setKpi(header, R.id.kpiTotalTraders, R.id.kpiTotalTradersCaption,
                summary == null ? 0 : summary.total_traders,
                summary == null ? "" : summary.total_traders_month_change_label);
        setKpi(header, R.id.kpiActiveTraders, R.id.kpiActiveTradersCaption,
                summary == null ? 0 : summary.active_traders,
                summary == null ? "" : summary.active_traders_percent_label);
        setKpi(header, R.id.kpiNewThisMonth, R.id.kpiNewThisMonthCaption,
                summary == null ? 0 : summary.new_this_month,
                summary == null ? "" : summary.new_this_month_change_label);
        setKpi(header, R.id.kpiPendingApproval, R.id.kpiPendingApprovalCaption,
                summary == null ? 0 : summary.pending_approval_traders,
                summary == null ? "" : summary.pending_approval_traders_percent_label);
    }

    private void setKpi(View header, int valueId, int captionId, int value, String caption) {
        ((TextView) header.findViewById(valueId))
                .setText(String.format(Locale.getDefault(), "%,d", value));
        TextView captionView = header.findViewById(captionId);
        boolean hasCaption = caption != null && !caption.trim().isEmpty();
        captionView.setText(hasCaption ? caption : "");
        captionView.setVisibility(hasCaption ? View.VISIBLE : View.GONE);
    }

    // ---- Row actions -----------------------------------------------------

    private void showRowMenu(TradersData.Trader trader, View anchor) {
        ContextMenuPopup.show(this, anchor,
                ContextMenuPopup.MenuItem.of(getString(R.string.trader_edit),
                        () -> editTrader(trader)),
                ContextMenuPopup.MenuItem.destructive(getString(R.string.trader_delete),
                        () -> confirmDelete(trader)));
    }

    private void editTrader(TradersData.Trader trader) {
        traderFormLauncher.launch(CreateTraderActivity.editIntent(this, trader));
    }

    private void confirmDelete(TradersData.Trader trader) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.trader_delete_title)
                .setMessage(getString(R.string.trader_delete_message, trader.displayName()))
                .setNegativeButton(R.string.cancel_btn, null)
                .setPositiveButton(R.string.trader_delete, (d, w) -> deleteTrader(trader))
                .show();
    }

    private void deleteTrader(TradersData.Trader trader) {
        ApiClient.get(this).deleteTrader(trader.id, new ApiCallback<String>() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(TradersActivity.this,
                        message == null || message.trim().isEmpty()
                                ? getString(R.string.trader_deleted_msg) : message,
                        Toast.LENGTH_SHORT).show();
                loadData();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(TradersActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (pendingSearch != null) {
            searchHandler.removeCallbacks(pendingSearch);
        }
        super.onDestroy();
    }
}
