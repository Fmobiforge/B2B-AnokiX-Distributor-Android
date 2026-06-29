package com.anokix.distributor.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributor.R;
import com.anokix.distributor.data.MockData;
import com.anokix.distributor.model.MenuItem;
import com.anokix.distributor.ui.DeliveriesActivity;
import com.anokix.distributor.ui.NotificationBadge;
import com.anokix.distributor.ui.FinancesActivity;
import com.anokix.distributor.ui.NotificationsActivity;
import com.anokix.distributor.ui.ProductsActivity;
import com.anokix.distributor.ui.PromotionsActivity;
import com.anokix.distributor.ui.ReportsActivity;
import com.anokix.distributor.ui.ReturnsActivity;
import com.anokix.distributor.ui.SettingsActivity;
import com.anokix.distributor.ui.StoresActivity;
import com.anokix.distributor.ui.TradersActivity;
import com.anokix.distributor.ui.adapter.MenuAdapter;

public class MoreFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_more, container, false);
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

        RecyclerView list = view.findViewById(R.id.moreMenuList);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(new MenuAdapter(MockData.getMoreMenuItems(), this::openScreen));
    }

    private void openScreen(MenuItem item) {
        Class<?> target;
        switch (item.key) {
            case "stores":
                target = StoresActivity.class;
                break;
            case "traders":
                target = TradersActivity.class;
                break;
            case "products":
                target = ProductsActivity.class;
                break;
            case "promotions":
                target = PromotionsActivity.class;
                break;
            case "finances":
                target = FinancesActivity.class;
                break;
            case "reports":
                target = ReportsActivity.class;
                break;
            case "deliveries":
                target = DeliveriesActivity.class;
                break;
            case "returns":
                target = ReturnsActivity.class;
                break;
            case "notifications":
                target = NotificationsActivity.class;
                break;
            case "settings":
                target = SettingsActivity.class;
                break;
            default:
                return;
        }
        startActivity(new Intent(requireContext(), target));
    }
}
