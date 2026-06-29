package com.anokix.distributor.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributor.R;
import com.anokix.distributor.data.MockData;
import com.anokix.distributor.model.ListItem;
import com.anokix.distributor.ui.NotificationBadge;
import com.anokix.distributor.ui.NotificationsActivity;
import com.anokix.distributor.ui.adapter.InventoryAdapter;
import com.anokix.distributor.ui.views.DonutChartView;

import java.util.ArrayList;
import java.util.List;

public class InventoryFragment extends Fragment {

    private InventoryAdapter adapter;
    private String currentFilter = "all";
    private TextView tabAll, tabLowStock, tabExpired;
    private View tabAllIndicator, tabLowStockIndicator, tabExpiredIndicator;

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

        tabAll = view.findViewById(R.id.tabAll);
        tabLowStock = view.findViewById(R.id.tabLowStock);
        tabExpired = view.findViewById(R.id.tabExpired);
        tabAllIndicator = view.findViewById(R.id.tabAllIndicator);
        tabLowStockIndicator = view.findViewById(R.id.tabLowStockIndicator);
        tabExpiredIndicator = view.findViewById(R.id.tabExpiredIndicator);

        view.findViewById(R.id.tabAllWrap).setOnClickListener(v -> selectTab("all"));
        view.findViewById(R.id.tabLowStockWrap).setOnClickListener(v -> selectTab("low"));
        view.findViewById(R.id.tabExpiredWrap).setOnClickListener(v -> selectTab("expired"));

        RecyclerView list = view.findViewById(R.id.inventoryList);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new InventoryAdapter(new ArrayList<>());
        list.setAdapter(adapter);

        DonutChartView chart = view.findViewById(R.id.inventoryDonutChart);
        chart.setData(
                new float[]{1047f, 86f, 12f},
                new int[]{
                        ContextCompat.getColor(requireContext(), R.color.success),
                        ContextCompat.getColor(requireContext(), R.color.warning),
                        ContextCompat.getColor(requireContext(), R.color.danger)
                }
        );

        selectTab("all");
    }

    private void selectTab(String filter) {
        currentFilter = filter;

        int primary = ContextCompat.getColor(requireContext(), R.color.purple_primary);
        int secondary = ContextCompat.getColor(requireContext(), R.color.text_secondary);

        styleTab(tabAll, tabAllIndicator, filter.equals("all"), primary, secondary);
        styleTab(tabLowStock, tabLowStockIndicator, filter.equals("low"), primary, secondary);
        styleTab(tabExpired, tabExpiredIndicator, filter.equals("expired"), primary, secondary);

        adapter.setItems(filterInventory(MockData.getInventory(), filter));
    }

    private void styleTab(TextView tab, View indicator, boolean selected,
                          int primary, int secondary) {
        tab.setTextColor(selected ? primary : secondary);
        if (selected) {
            tab.setTypeface(tab.getTypeface(), android.graphics.Typeface.BOLD);
        } else {
            tab.setTypeface(tab.getTypeface(), android.graphics.Typeface.NORMAL);
        }
        indicator.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
    }

    private List<ListItem> filterInventory(List<ListItem> all, String filter) {
        if ("all".equals(filter)) {
            return all;
        }
        List<ListItem> filtered = new ArrayList<>();
        for (ListItem item : all) {
            if ("low".equals(filter) && "Low Stock".equals(item.badge)) {
                filtered.add(item);
            } else if ("expired".equals(filter) && "Expired".equals(item.badge)) {
                filtered.add(item);
            }
        }
        return filtered;
    }
}
