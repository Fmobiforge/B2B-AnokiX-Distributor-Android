package com.anokix.distributor.ui;

import android.widget.TextView;

import androidx.core.util.Pair;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributor.R;
import com.anokix.distributor.data.MockData;
import com.anokix.distributor.model.DeliveryItem;
import com.anokix.distributor.ui.adapter.DeliveryAdapter;
import com.anokix.distributor.ui.util.FilterUiHelper;

import java.util.ArrayList;
import java.util.List;

public class DeliveriesActivity extends BaseSecondaryActivity {

    private DeliveryAdapter adapter;
    private String currentFilter = "all";
    private String selectedDriver = "All Drivers";
    private TextView tabAll, tabInTransit, tabDelivered, tabDelayed;
    private List<DeliveryItem> statusFilteredItems = new ArrayList<>();

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
        RecyclerView list = findViewById(R.id.deliveriesList);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DeliveryAdapter();
        list.setAdapter(adapter);

        tabAll = findViewById(R.id.tabAll);
        tabInTransit = findViewById(R.id.tabInTransit);
        tabDelivered = findViewById(R.id.tabDelivered);
        tabDelayed = findViewById(R.id.tabDelayed);

        tabAll.setOnClickListener(v -> selectTab("all", tabAll, tabInTransit, tabDelivered, tabDelayed));
        tabInTransit.setOnClickListener(v -> selectTab("in_transit", tabInTransit, tabAll, tabDelivered, tabDelayed));
        tabDelivered.setOnClickListener(v -> selectTab("delivered", tabDelivered, tabAll, tabInTransit, tabDelayed));
        tabDelayed.setOnClickListener(v -> selectTab("delayed", tabDelayed, tabAll, tabInTransit, tabDelivered));

        TextView dateLabel = findViewById(R.id.dateRangeLabel);
        TextView driverLabel = findViewById(R.id.driverFilterLabel);
        Pair<Long, Long> defaultRange = FilterUiHelper.defaultWeekRangeUtc();

        FilterUiHelper.bindDateRangePicker(
                this,
                findViewById(R.id.dateRangeButton),
                dateLabel,
                defaultRange.first,
                defaultRange.second,
                (start, end, formatted) -> { /* ready for API date filtering */ });

        FilterUiHelper.setDropdownContentDescription(driverLabel, getString(R.string.driver_label));
        FilterUiHelper.bindDropdown(
                findViewById(R.id.driverFilterButton),
                driverLabel,
                MockData.getFilterDrivers(),
                0,
                (index, value) -> {
                    selectedDriver = value;
                    applyDriverFilter();
                });
    }

    @Override
    protected void loadData() {
        selectTab("all", tabAll, tabInTransit, tabDelivered, tabDelayed);
    }

    private void selectTab(String filter, TextView selected, TextView... others) {
        currentFilter = filter;
        styleSegmentTab(selected, true);
        for (TextView tab : others) {
            styleSegmentTab(tab, false);
        }
        statusFilteredItems = repository.getDeliveries(currentFilter);
        applyDriverFilter();
    }

    private void applyDriverFilter() {
        if ("All Drivers".equals(selectedDriver)) {
            adapter.setItems(statusFilteredItems);
            return;
        }
        List<DeliveryItem> filtered = new ArrayList<>();
        for (DeliveryItem item : statusFilteredItems) {
            if (selectedDriver.equals(item.driverName)) {
                filtered.add(item);
            }
        }
        adapter.setItems(filtered);
    }

    private void styleSegmentTab(TextView tab, boolean selected) {
        if (selected) {
            tab.setBackgroundResource(R.drawable.bg_segment_tab_selected);
            tab.setTextColor(getResources().getColor(android.R.color.white));
        } else {
            tab.setBackgroundResource(android.R.color.transparent);
            tab.setTextColor(getResources().getColor(R.color.text_primary));
        }
    }
}
