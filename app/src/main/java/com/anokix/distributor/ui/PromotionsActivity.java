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
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributor.R;
import com.anokix.distributor.data.ApiMapper;
import com.anokix.distributor.model.PromotionItem;
import com.anokix.distributor.network.ApiCallback;
import com.anokix.distributor.network.ApiClient;
import com.anokix.distributor.network.dto.PromotionDetailData;
import com.anokix.distributor.network.dto.PromotionsData;
import com.anokix.distributor.ui.adapter.PromotionAdapter;
import com.anokix.distributor.ui.util.ContextMenuPopup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PromotionsActivity extends BaseSecondaryActivity {

    private static final String[] STATUS_LABELS = {
            "All Status", "Active", "Pending Approval", "Scheduled", "Draft", "Rejected"};
    private static final String[] STATUS_VALUES = {
            "", "active", "pending_approval", "scheduled", "draft", "rejected"};

    private static final String[] TYPE_LABELS = {
            "All Types", "Best Deal", "Can't Miss Deal", "Exclusive Deal", "Featured Store",
            "Promotion", "Post", "Story"};
    private static final String[] TYPE_VALUES = {
            "", "best_deal", "cant_miss_deal", "exclusive_deal", "featured_store",
            "promotion", "post", "story"};

    private PromotionAdapter adapter;
    private RecyclerView list;
    private TextView emptyView;
    private TextView statusFilterLabel, typeFilterLabel;

    // Full result set from one API call; all search/filter happens locally over this.
    private final List<PromotionItem> allItems = new ArrayList<>();
    private String statusFilter = "";
    private String typeFilter = "";
    private String search = "";

    // Reload the list when the Create wizard returns successfully.
    private final ActivityResultLauncher<Intent> createLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> loadData());

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_promotions;
    }

    @Override
    protected String getScreenTitle() {
        return getString(R.string.promotions);
    }

    @Override
    protected void bindViews() {
        emptyView = findViewById(R.id.emptyView);
        statusFilterLabel = findViewById(R.id.statusFilterLabel);
        typeFilterLabel = findViewById(R.id.typeFilterLabel);

        list = findViewById(R.id.promotionsList);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setNestedScrollingEnabled(false);
        adapter = new PromotionAdapter();
        adapter.setOnItemMenu(this::showRowMenu);
        list.setAdapter(adapter);

        EditText searchInput = findViewById(R.id.searchInput);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                search = s.toString();
                applyFilter();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        findViewById(R.id.statusFilter).setOnClickListener(v -> new AlertDialog.Builder(this)
                .setItems(STATUS_LABELS, (d, which) -> {
                    statusFilter = STATUS_VALUES[which];
                    statusFilterLabel.setText(STATUS_LABELS[which]);
                    applyFilter();
                })
                .show());

        findViewById(R.id.typeFilter).setOnClickListener(v -> new AlertDialog.Builder(this)
                .setItems(TYPE_LABELS, (d, which) -> {
                    typeFilter = TYPE_VALUES[which];
                    typeFilterLabel.setText(TYPE_LABELS[which]);
                    applyFilter();
                })
                .show());

        findViewById(R.id.createPromotionButton).setOnClickListener(v ->
                createLauncher.launch(new Intent(this, CreatePromotionActivity.class)));
    }

    @Override
    protected void loadData() {
        // One fetch (all types); status/type/search filtering is done locally.
        ApiClient.get(this).getPromotions("", new ApiCallback<PromotionsData>() {
            @Override
            public void onSuccess(PromotionsData data) {
                bindSummary(data);
                allItems.clear();
                allItems.addAll(ApiMapper.toPromotionItems(data));
                applyFilter();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(PromotionsActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    // ---- Local search + filter -------------------------------------------

    private void applyFilter() {
        String q = search.trim().toLowerCase(Locale.getDefault());
        List<PromotionItem> out = new ArrayList<>();
        for (PromotionItem item : allItems) {
            if (!statusFilter.isEmpty() && !statusFilter.equalsIgnoreCase(item.status)) {
                continue;
            }
            if (!typeFilter.isEmpty() && !typeFilter.equalsIgnoreCase(item.campaignType)) {
                continue;
            }
            if (!q.isEmpty() && (item.name == null
                    || !item.name.toLowerCase(Locale.getDefault()).contains(q))) {
                continue;
            }
            out.add(item);
        }
        adapter.setItems(out);
        boolean empty = out.isEmpty();
        emptyView.setVisibility(empty ? android.view.View.VISIBLE : android.view.View.GONE);
        list.setVisibility(empty ? android.view.View.GONE : android.view.View.VISIBLE);
    }

    // ---- Row overflow menu: View Details · Edit · Delete -----------------

    private void showRowMenu(View anchor, PromotionItem item) {
        ContextMenuPopup.show(this, anchor,
                ContextMenuPopup.MenuItem.of(getString(R.string.promo_view_details),
                        () -> showDetails(item)),
                ContextMenuPopup.MenuItem.of(getString(R.string.promo_edit),
                        () -> openEditor(item)),
                ContextMenuPopup.MenuItem.destructive(getString(R.string.promo_delete),
                        () -> confirmDelete(item)));
    }

    private void openEditor(PromotionItem item) {
        createLauncher.launch(new Intent(this, CreatePromotionActivity.class)
                .putExtra(CreatePromotionActivity.EXTRA_PROMOTION_ID, item.id));
    }

    private void confirmDelete(PromotionItem item) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.promo_delete_confirm_title)
                .setMessage(getString(R.string.promo_delete_confirm_msg,
                        item.name == null ? "" : item.name))
                .setNegativeButton(R.string.cancel_btn, null)
                .setPositiveButton(R.string.promo_delete, (d, w) -> deletePromotion(item))
                .show();
    }

    private void deletePromotion(PromotionItem item) {
        ApiClient.get(this).deletePromotion(item.id, new ApiCallback<PromotionDetailData>() {
            @Override
            public void onSuccess(PromotionDetailData data) {
                Toast.makeText(PromotionsActivity.this, R.string.promo_deleted_msg,
                        Toast.LENGTH_SHORT).show();
                loadData();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(PromotionsActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    /** Quick read-only summary built from the list row (always available, no extra fetch). */
    private void showDetails(PromotionItem item) {
        StringBuilder sb = new StringBuilder();
        appendDetail(sb, getString(R.string.promo_type_label), item.typeLabel);
        appendDetail(sb, getString(R.string.promo_status_label), item.statusLabel);
        appendDetail(sb, getString(R.string.promo_duration), item.dateRange);
        appendDetail(sb, getString(R.string.budget), item.budget);
        appendDetail(sb, getString(R.string.reach), item.reach);
        appendDetail(sb, getString(R.string.clicks), item.clicks);
        appendDetail(sb, getString(R.string.ctr), item.ctr);
        new AlertDialog.Builder(this)
                .setTitle(item.name)
                .setMessage(sb.toString().trim())
                .setPositiveButton(R.string.promo_edit, (d, w) -> openEditor(item))
                .setNegativeButton(R.string.promo_close, null)
                .show();
    }

    private void appendDetail(StringBuilder sb, String label, String value) {
        if (value == null || value.isEmpty()) return;
        sb.append(label).append(": ").append(value).append('\n');
    }

    // ---- Summary cards (static views, values bound by key) ---------------

    private void bindSummary(PromotionsData data) {
        if (data == null || data.summary == null || data.summary.summary_cards == null) {
            return;
        }
        for (PromotionsData.SummaryCard c : data.summary.summary_cards) {
            int valueId;
            int trendId;
            switch (c.key == null ? "" : c.key) {
                case "active_campaigns": valueId = R.id.valActiveCampaigns; trendId = R.id.trendActiveCampaigns; break;
                case "campaign_revenue": valueId = R.id.valRevenue; trendId = R.id.trendRevenue; break;
                case "campaign_reach": valueId = R.id.valReach; trendId = R.id.trendReach; break;
                case "campaign_clicks": valueId = R.id.valClicks; trendId = R.id.trendClicks; break;
                case "ctr": valueId = R.id.valCtr; trendId = R.id.trendCtr; break;
                case "attributed_sales": valueId = R.id.valSales; trendId = R.id.trendSales; break;
                default: continue;
            }
            ((TextView) findViewById(valueId)).setText(c.formatted_value);
            TextView trend = findViewById(trendId);
            if (c.trend != null) {
                boolean down = "down".equalsIgnoreCase(c.trend.direction);
                trend.setText(String.format(Locale.US, "%s %.1f%%", down ? "↓" : "↑", c.trend.percentage));
                trend.setTextColor(ContextCompat.getColor(this, down ? R.color.danger : R.color.success));
            }
        }
    }
}
