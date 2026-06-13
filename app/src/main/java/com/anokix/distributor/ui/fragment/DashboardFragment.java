package com.anokix.distributor.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.anokix.distributor.R;
import com.anokix.distributor.data.MockData;
import com.anokix.distributor.network.ApiCallback;
import com.anokix.distributor.network.ApiClient;
import com.anokix.distributor.network.dto.DashboardData;
import com.anokix.distributor.session.SessionManager;
import com.anokix.distributor.ui.MainActivity;
import com.anokix.distributor.ui.NotificationsActivity;
import com.anokix.distributor.ui.views.LineChartView;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

public class DashboardFragment extends Fragment {

    private final TextView[] kpiValueViews = new TextView[4];

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.hamburgerButton).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openDrawer();
            }
        });

        view.findViewById(R.id.notificationsButton).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), NotificationsActivity.class)));

        bindWelcome(view);
        buildKpiGrid(view);
        loadDashboard(view);
    }

    private void bindWelcome(View view) {
        TextView welcome = view.findViewById(R.id.welcomeText);
        if (welcome != null) {
            String firstName = SessionManager.get(requireContext()).getFirstName();
            welcome.setText(firstName.isEmpty()
                    ? getString(R.string.welcome_back_generic)
                    : getString(R.string.welcome_back_named, firstName));
        }
    }

    private void buildKpiGrid(View view) {
        GridLayout kpiGrid = view.findViewById(R.id.kpiGrid);
        List<String> titles = MockData.getKpiTitles();
        List<String> values = MockData.getKpiValues();
        List<Integer> icons = MockData.getKpiIcons();
        List<Integer> iconBackgrounds = MockData.getKpiIconBackgrounds();
        List<Integer> valueColors = MockData.getKpiValueColors();
        List<Integer> iconTints = MockData.getKpiIconTints();
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (int i = 0; i < titles.size(); i++) {
            View card = inflater.inflate(R.layout.item_kpi_card, kpiGrid, false);
            ((TextView) card.findViewById(R.id.kpiTitle)).setText(titles.get(i));
            TextView kpiValue = card.findViewById(R.id.kpiValue);
            kpiValue.setText(values.get(i));
            kpiValue.setTextColor(getResources().getColor(valueColors.get(i)));
            if (i < kpiValueViews.length) {
                kpiValueViews[i] = kpiValue;
            }

            ImageView kpiIcon = card.findViewById(R.id.kpiIcon);
            kpiIcon.setImageResource(icons.get(i));
            kpiIcon.setColorFilter(getResources().getColor(iconTints.get(i)));

            FrameLayout iconContainer = card.findViewById(R.id.kpiIconContainer);
            iconContainer.setBackgroundResource(iconBackgrounds.get(i));

            card.findViewById(R.id.kpiViewAll).setOnClickListener(v -> {
                if (getActivity() != null) {
                    BottomNavigationView nav = getActivity().findViewById(R.id.bottomNav);
                    if (nav != null) {
                        nav.setSelectedItemId(R.id.nav_orders);
                    }
                }
            });

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(i % 2, 1f);
            params.rowSpec = GridLayout.spec(i / 2);
            params.setMargins(6, 6, 6, 6);
            card.setLayoutParams(params);
            kpiGrid.addView(card);
        }
    }

    private void loadDashboard(View root) {
        ApiClient.get(requireContext()).getDashboard(new ApiCallback<DashboardData>() {
            @Override
            public void onSuccess(DashboardData data) {
                if (!isAdded() || data == null || data.dashboard == null) {
                    return;
                }
                bindOverview(data.dashboard.overview);
                bindRevenue(root, data.dashboard);
            }

            @Override
            public void onError(String message) {
                // KPI grid keeps its placeholder values; surface nothing intrusive here.
            }
        });
    }

    private void bindOverview(List<DashboardData.OverviewItem> overview) {
        if (overview == null) {
            return;
        }
        for (int i = 0; i < overview.size() && i < kpiValueViews.length; i++) {
            if (kpiValueViews[i] != null) {
                kpiValueViews[i].setText(formatCount(overview.get(i).value));
            }
        }
    }

    private void bindRevenue(View root, DashboardData.Dashboard dashboard) {
        DashboardData.RevenueOverview revenue = dashboard.revenue_overview;
        if (revenue == null) {
            return;
        }
        TextView total = root.findViewById(R.id.revenueTotal);
        if (total != null && revenue.formatted_total != null) {
            total.setText(revenue.formatted_total);
        }
        TextView trend = root.findViewById(R.id.revenueTrend);
        if (trend != null && revenue.trend != null) {
            boolean up = !"down".equalsIgnoreCase(revenue.trend.direction);
            trend.setText(String.format(java.util.Locale.US, "%s %.1f%%",
                    up ? "↑" : "↓", revenue.trend.percentage));
        }
        TextView compare = root.findViewById(R.id.revenueCompare);
        if (compare != null && dashboard.meta != null && dashboard.meta.compare_period_label != null) {
            compare.setText(dashboard.meta.compare_period_label);
        }
        LineChartView chart = root.findViewById(R.id.revenueChart);
        if (chart != null && revenue.chart != null && revenue.chart.values != null
                && !revenue.chart.values.isEmpty()) {
            float[] points = new float[revenue.chart.values.size()];
            for (int i = 0; i < points.length; i++) {
                points[i] = revenue.chart.values.get(i);
            }
            chart.setValues(points);
        }
    }

    private String formatCount(long value) {
        return String.format(java.util.Locale.US, "%,d", value);
    }
}
