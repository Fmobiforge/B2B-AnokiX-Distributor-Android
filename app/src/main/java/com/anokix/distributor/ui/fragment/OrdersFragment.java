package com.anokix.distributor.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributor.R;
import com.anokix.distributor.data.MockData;
import com.anokix.distributor.ui.NotificationsActivity;
import com.anokix.distributor.ui.adapter.OrderAdapter;
import com.anokix.distributor.ui.util.FilterUiHelper;

public class OrdersFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_orders, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView list = view.findViewById(R.id.ordersList);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(new OrderAdapter(MockData.getOrders(), true));

        TextView tabNew = view.findViewById(R.id.tabNewOrders);
        TextView tabProcessing = view.findViewById(R.id.tabProcessing);
        TextView tabDelivered = view.findViewById(R.id.tabDelivered);

        tabNew.setOnClickListener(v -> selectStatusTab(tabNew, tabProcessing, tabDelivered));
        tabProcessing.setOnClickListener(v -> selectStatusTab(tabProcessing, tabNew, tabDelivered));
        tabDelivered.setOnClickListener(v -> selectStatusTab(tabDelivered, tabNew, tabProcessing));

        TextView dateLabel = view.findViewById(R.id.dateRangeLabel);
        Pair<Long, Long> defaultRange = FilterUiHelper.defaultWeekRangeUtc();
        FilterUiHelper.bindDateRangePicker(
                requireActivity(),
                view.findViewById(R.id.dateRangeButton),
                dateLabel,
                defaultRange.first,
                defaultRange.second,
                (start, end, formatted) -> { /* ready for API date filtering */ });
    }

    private void selectStatusTab(TextView selected, TextView other1, TextView other2) {
        selected.setTextColor(getResources().getColor(R.color.warning));
        other1.setTextColor(getResources().getColor(R.color.text_secondary));
        other2.setTextColor(getResources().getColor(R.color.text_secondary));
    }
}
