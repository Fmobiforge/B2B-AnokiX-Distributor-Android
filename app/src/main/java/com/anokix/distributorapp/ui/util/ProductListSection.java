package com.anokix.distributorapp.ui.util;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.model.ProductItem;
import com.anokix.distributorapp.network.dto.BaseInfoData;
import com.anokix.distributorapp.network.dto.ProductsData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Drives the Products screen chrome (include_products_chrome.xml): KPI cards, the
 * stock tabs, keyword search and the category / brand / status filter pills.
 *
 * Filtering runs locally over the product list the API already returned, so switching
 * a tab or a pill never costs a round trip. Categories and brands come from
 * /api/common/base-info; if that call has not landed yet the pickers fall back to the
 * values denormalised onto the products themselves.
 *
 * Shared by the Products tab (ProductsFragment) and the drawer screen (ProductsActivity).
 */
public class ProductListSection {

    /** Receives the list that survived the current tab + search + filter selection. */
    public interface OnFilteredListener {
        void onFiltered(List<ProductItem> items);
    }

    private static final int TAB_ALL = 0;
    private static final int TAB_ACTIVE = 1;
    private static final int TAB_LOW = 2;
    private static final int TAB_OUT = 3;

    private static final String ALL_CATEGORIES = "";
    private static final String ALL_BRANDS = "";
    private static final String ALL_STATUS = "";

    private final Context context;
    private final OnFilteredListener listener;

    private final List<ProductItem> allItems = new ArrayList<>();
    private final List<BaseInfoData.ProductCategory> categories = new ArrayList<>();
    private final List<BaseInfoData.Brand> brands = new ArrayList<>();
    private ProductsData.Summary summary;

    private int selectedTab = TAB_ALL;
    private String query = "";
    private String categoryId = ALL_CATEGORIES;
    private String brandName = ALL_BRANDS;
    private String status = ALL_STATUS;

    // Chrome views
    private TextView kpiTotal, kpiTotalSub, kpiActive, kpiActiveSub, kpiOut, kpiOutSub, kpiLow, kpiLowSub;
    private TextView[] tabLabels;
    private View[] tabIndicators;
    private TextView categoryLabel, brandLabel, statusLabel;
    private TextView emptyView;
    private TextView countLabel;

    public ProductListSection(@NonNull Context context, @NonNull OnFilteredListener listener) {
        this.context = context;
        this.listener = listener;
    }

    /** Wires every chrome view found under {@code root} and applies the initial state. */
    public void bind(@NonNull View root) {
        kpiTotal = root.findViewById(R.id.kpiTotalValue);
        kpiTotalSub = root.findViewById(R.id.kpiTotalSub);
        kpiActive = root.findViewById(R.id.kpiActiveValue);
        kpiActiveSub = root.findViewById(R.id.kpiActiveSub);
        kpiOut = root.findViewById(R.id.kpiOutValue);
        kpiOutSub = root.findViewById(R.id.kpiOutSub);
        kpiLow = root.findViewById(R.id.kpiLowValue);
        kpiLowSub = root.findViewById(R.id.kpiLowSub);

        tabLabels = new TextView[]{
                root.findViewById(R.id.tabAllLabel),
                root.findViewById(R.id.tabActiveLabel),
                root.findViewById(R.id.tabLowLabel),
                root.findViewById(R.id.tabOutLabel)};
        tabIndicators = new View[]{
                root.findViewById(R.id.tabAllIndicator),
                root.findViewById(R.id.tabActiveIndicator),
                root.findViewById(R.id.tabLowIndicator),
                root.findViewById(R.id.tabOutIndicator)};
        for (int i = 0; i < tabLabels.length; i++) {
            final int index = i;
            View tab = (View) tabLabels[i].getParent();
            tab.setOnClickListener(v -> {
                selectedTab = index;
                applyFilters();
            });
        }

        categoryLabel = root.findViewById(R.id.categoryFilterLabel);
        brandLabel = root.findViewById(R.id.brandFilterLabel);
        statusLabel = root.findViewById(R.id.statusFilterLabel);
        root.findViewById(R.id.categoryFilter).setOnClickListener(v -> showCategoryPicker());
        root.findViewById(R.id.brandFilter).setOnClickListener(v -> showBrandPicker());
        root.findViewById(R.id.statusFilter).setOnClickListener(v -> showStatusPicker());

        emptyView = root.findViewById(R.id.emptyView);
        countLabel = root.findViewById(R.id.productsCount);

        EditText search = root.findViewById(R.id.searchInput);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                query = s == null ? "" : s.toString().trim().toLowerCase(Locale.getDefault());
                applyFilters();
            }
        });

        applyFilters();
    }

    /** New product page from the API (also refreshes the KPI cards and tab counts). */
    public void setProducts(@Nullable List<ProductItem> items, @Nullable ProductsData.Summary summary) {
        allItems.clear();
        if (items != null) {
            allItems.addAll(items);
        }
        this.summary = summary;
        applyFilters();
    }

    /** Lookup lists for the category / brand pickers (/api/common/base-info). */
    public void setBaseInfo(@Nullable BaseInfoData data) {
        categories.clear();
        brands.clear();
        if (data != null) {
            if (data.product_categories != null) {
                categories.addAll(data.product_categories);
            }
            if (data.brands != null) {
                brands.addAll(data.brands);
            }
        }
    }

    // ---- Filtering -------------------------------------------------------

    private void applyFilters() {
        List<ProductItem> filtered = new ArrayList<>();
        for (ProductItem item : allItems) {
            if (matchesTab(item) && matchesQuery(item) && matchesCategory(item)
                    && matchesBrand(item) && matchesStatus(item)) {
                filtered.add(item);
            }
        }
        bindKpis();
        bindTabs();
        bindCount(filtered.size());
        bindEmptyState(filtered.isEmpty());
        listener.onFiltered(filtered);
    }

    private boolean matchesTab(ProductItem item) {
        switch (selectedTab) {
            case TAB_ACTIVE:
                return item.isActive();
            case TAB_LOW:
                return item.isLowStock();
            case TAB_OUT:
                return item.isOutOfStock();
            default:
                return true;
        }
    }

    private boolean matchesQuery(ProductItem item) {
        if (query.isEmpty()) {
            return true;
        }
        return contains(item.name) || contains(item.sku) || contains(item.barcode)
                || contains(item.category) || contains(item.brand);
    }

    private boolean matchesCategory(ProductItem item) {
        if (ALL_CATEGORIES.equals(categoryId)) {
            return true;
        }
        // A parent category also matches every product filed under one of its children.
        return categoryId.equals(item.categoryId) || categoryId.equals(item.parentCategoryId);
    }

    private boolean matchesBrand(ProductItem item) {
        return ALL_BRANDS.equals(brandName) || brandName.equalsIgnoreCase(item.brand);
    }

    private boolean matchesStatus(ProductItem item) {
        return ALL_STATUS.equals(status) || status.equalsIgnoreCase(item.status);
    }

    private boolean contains(String value) {
        return value != null && value.toLowerCase(Locale.getDefault()).contains(query);
    }

    // ---- Chrome binding --------------------------------------------------

    private void bindKpis() {
        int total = summary != null && summary.total_products != null
                ? summary.total_products.value : allItems.size();
        int active = summary != null && summary.active_products != null
                ? summary.active_products.value : count(TAB_ACTIVE);
        int out = summary != null && summary.out_of_stock != null
                ? summary.out_of_stock.value : count(TAB_OUT);
        int low = summary != null && summary.low_stock != null
                ? summary.low_stock.value : count(TAB_LOW);

        kpiTotal.setText(String.valueOf(total));
        kpiActive.setText(String.valueOf(active));
        kpiOut.setText(String.valueOf(out));
        kpiLow.setText(String.valueOf(low));

        kpiTotalSub.setText(totalTrendLabel());
        kpiActiveSub.setText(percentLabel(active, total));
        kpiOutSub.setText(percentLabel(out, total));
        kpiLowSub.setText(percentLabel(low, total));
    }

    private String totalTrendLabel() {
        if (summary == null || summary.total_products == null
                || summary.total_products.compare_label == null) {
            return "";
        }
        ProductsData.Summary.Metric metric = summary.total_products;
        String change = String.format(Locale.getDefault(), "%+.0f%%", metric.change_percent);
        return context.getString(R.string.kpi_change_vs, change, metric.compare_label);
    }

    private String percentLabel(int value, int total) {
        int percent = total > 0 ? Math.round(value * 100f / total) : 0;
        return context.getString(R.string.kpi_percent_of_total, percent);
    }

    private void bindTabs() {
        int[] labels = {R.string.tab_all_products, R.string.active,
                R.string.low_stock, R.string.out_of_stock};
        for (int i = 0; i < tabLabels.length; i++) {
            tabLabels[i].setText(context.getString(R.string.tab_label_count,
                    context.getString(labels[i]), count(i)));
            boolean selected = i == selectedTab;
            tabLabels[i].setTextColor(ContextCompat.getColor(context,
                    selected ? R.color.purple_primary : R.color.text_secondary));
            tabIndicators[i].setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private int count(int tab) {
        int total = 0;
        for (ProductItem item : allItems) {
            switch (tab) {
                case TAB_ACTIVE:
                    if (item.isActive()) total++;
                    break;
                case TAB_LOW:
                    if (item.isLowStock()) total++;
                    break;
                case TAB_OUT:
                    if (item.isOutOfStock()) total++;
                    break;
                default:
                    total++;
            }
        }
        return total;
    }

    private void bindCount(int shown) {
        if (countLabel != null) {
            countLabel.setText(context.getString(R.string.products_shown_count, shown, allItems.size()));
        }
    }

    private void bindEmptyState(boolean empty) {
        if (emptyView == null) {
            return;
        }
        emptyView.setText(allItems.isEmpty() ? R.string.no_products_yet : R.string.no_products_match);
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    // ---- Pickers ---------------------------------------------------------

    /**
     * Category sheet: parent categories with their children indented underneath,
     * mirroring the web filter. Falls back to the categories present on the loaded
     * products when base-info has not arrived.
     */
    private void showCategoryPicker() {
        List<String> labels = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        labels.add(context.getString(R.string.all_categories));
        ids.add(ALL_CATEGORIES);

        if (!categories.isEmpty()) {
            for (BaseInfoData.ProductCategory parent : categories) {
                if (parent.parent_id != null && parent.parent_id != 0) {
                    continue;
                }
                labels.add(parent.title);
                ids.add(String.valueOf(parent.id));
                for (BaseInfoData.ProductCategory child : categories) {
                    if (child.parent_id != null && child.parent_id == parent.id) {
                        labels.add(context.getString(R.string.child_category_prefix, child.title));
                        ids.add(String.valueOf(child.id));
                    }
                }
            }
        } else {
            Map<String, String> fromProducts = new LinkedHashMap<>();
            for (ProductItem item : allItems) {
                if (!item.categoryId.isEmpty() && !item.category.isEmpty()) {
                    fromProducts.put(item.categoryId, item.category);
                }
            }
            for (Map.Entry<String, String> entry : fromProducts.entrySet()) {
                labels.add(entry.getValue());
                ids.add(entry.getKey());
            }
        }

        showPicker(R.string.category_filter_title, labels, which -> {
            categoryId = ids.get(which);
            categoryLabel.setText(which == 0
                    ? context.getString(R.string.all_categories)
                    : labels.get(which).replace("— ", ""));
            applyFilters();
        });
    }

    private void showBrandPicker() {
        List<String> labels = new ArrayList<>();
        labels.add(context.getString(R.string.all_brands));
        if (!brands.isEmpty()) {
            for (BaseInfoData.Brand brand : brands) {
                if (brand.name != null && !brand.name.isEmpty() && !labels.contains(brand.name)) {
                    labels.add(brand.name);
                }
            }
        } else {
            for (ProductItem item : allItems) {
                if (!item.brand.isEmpty() && !labels.contains(item.brand)) {
                    labels.add(item.brand);
                }
            }
        }

        showPicker(R.string.brand_filter_title, labels, which -> {
            brandName = which == 0 ? ALL_BRANDS : labels.get(which);
            brandLabel.setText(labels.get(which));
            applyFilters();
        });
    }

    private void showStatusPicker() {
        List<String> labels = new ArrayList<>();
        labels.add(context.getString(R.string.all_status));
        labels.add(context.getString(R.string.active));
        labels.add(context.getString(R.string.inactive));
        for (ProductItem item : allItems) {
            if (!item.status.isEmpty() && !labels.contains(item.status)) {
                labels.add(item.status);
            }
        }

        showPicker(R.string.status_filter_title, labels, which -> {
            status = which == 0 ? ALL_STATUS : labels.get(which);
            statusLabel.setText(labels.get(which));
            applyFilters();
        });
    }

    private void showPicker(int titleRes, List<String> labels, OnPicked callback) {
        new AlertDialog.Builder(context)
                .setTitle(titleRes)
                .setItems(labels.toArray(new String[0]), (dialog, which) -> callback.onPicked(which))
                .show();
    }

    private interface OnPicked {
        void onPicked(int which);
    }
}
