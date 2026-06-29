package com.anokix.distributor.ui.fragment;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.anokix.distributor.ui.NotificationBadge;
import com.anokix.distributor.ui.NotificationsActivity;
import com.anokix.distributor.ui.TradersActivity;
import com.anokix.distributor.ui.views.DonutChartView;
import com.anokix.distributor.ui.views.LineChartView;
import com.anokix.distributor.ui.views.SparklineView;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

public class DashboardFragment extends Fragment {

    private final TextView[] kpiValueViews = new TextView[4];
    private final View[] kpiActionViews = new View[4];
    private String currencySymbol = "R";

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

        View recentViewAll = view.findViewById(R.id.recentOrdersViewAll);
        if (recentViewAll != null) {
            recentViewAll.setOnClickListener(v -> selectTab(R.id.nav_orders));
        }

        bindWelcome(view);
        buildKpiGrid(view);
        buildSummaryCards(view, MockData.fallbackSummaryCards());
        loadDashboard(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null) {
            NotificationBadge.refresh(requireContext(), getView().findViewById(R.id.notificationBadge));
        }
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
        List<String> titlesAction = MockData.getKpiActionTitles();
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        int density = (int) getResources().getDisplayMetrics().density;
        int spacing = 8 * density;

        for (int i = 0; i < titles.size(); i++) {
            View card = inflater.inflate(R.layout.item_kpi_card, kpiGrid, false);

            // Title
            ((TextView) card.findViewById(R.id.kpiTitle)).setText(titles.get(i));

            // Temp value
            TextView kpiValue = card.findViewById(R.id.kpiValue);
            kpiValue.setText(values.get(i));

            // Color
            kpiValue.setTextColor(getResources().getColor(valueColors.get(i)));
            if (i < kpiValueViews.length) {
                kpiValueViews[i] = kpiValue;
            }

            // Icon
            ImageView kpiIcon = card.findViewById(R.id.kpiIcon);
            kpiIcon.setImageResource(icons.get(i));
            kpiIcon.setColorFilter(getResources().getColor(iconTints.get(i)));

            // Action Title
            TextView kpiActionTitle = card.findViewById(R.id.kpiActionTitle);
            kpiActionTitle.setText(titlesAction.get(i));

            FrameLayout iconContainer = card.findViewById(R.id.kpiIconContainer);
            iconContainer.setBackgroundResource(iconBackgrounds.get(i));

            View action = card.findViewById(R.id.kpiViewAll);
            action.setOnClickListener(v -> selectTab(R.id.nav_orders));
            if (i < kpiActionViews.length) {
                kpiActionViews[i] = action;
            }

            int column = i % 2;
            int row = i / 2;

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(column, 1f);
            params.rowSpec = GridLayout.spec(row);

            // Only add spacing between items
            int left = (column > 0) ? spacing : 0;
            int top = (row > 0) ? spacing : 0;
            params.setMargins(left, top, 0, 0);
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
                DashboardData.Dashboard d = data.dashboard;
                if (d.meta != null && d.meta.currency != null && d.meta.currency.symbol != null) {
                    currencySymbol = d.meta.currency.symbol;
                }
                if (d.summary_cards != null && !d.summary_cards.isEmpty()) {
                    buildSummaryCards(root, d.summary_cards);
                }
                bindOverview(d.overview);
                bindRevenue(root, d);
                bindCategories(root, d.top_selling_categories);
                bindRecentOrders(root, d.recent_orders);
            }

            @Override
            public void onError(String message) {
                // Sections keep their placeholder values; surface nothing intrusive here.
            }
        });
    }

    // ---- Summary KPI cards (top, horizontally scrollable) ----------------

    private void buildSummaryCards(View root, List<DashboardData.SummaryCard> cards) {
        LinearLayout row = root.findViewById(R.id.summaryCardsRow);
        if (row == null || cards == null) {
            return;
        }
        row.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        int density = (int) getResources().getDisplayMetrics().density;

        for (int i = 0; i < cards.size(); i++) {
            DashboardData.SummaryCard c = cards.get(i);
            View card = inflater.inflate(R.layout.item_summary_card, row, false);

            int accent = accentColor(c.color);
            int light = lightAccent(c.color);

            ((TextView) card.findViewById(R.id.summaryLabel)).setText(c.label);
            TextView value = card.findViewById(R.id.summaryValue);
            value.setText(c.formatted_value);

            ImageView icon = card.findViewById(R.id.summaryIcon);
            icon.setImageResource(iconForKey(c.key));
            icon.setColorFilter(accent);
            FrameLayout iconContainer = card.findViewById(R.id.summaryIconContainer);
            iconContainer.setBackground(roundedRect(light, 12));

            TextView trend = card.findViewById(R.id.summaryTrend);
            boolean up = c.trend == null || !"down".equalsIgnoreCase(c.trend.direction);
            int trendColor = up ? 0xFF16A34A : 0xFFDC2626;
            trend.setText(c.trend != null ? c.trend.label : "");
            trend.setTextColor(trendColor);
            trend.setBackground(roundedRect((trendColor & 0x00FFFFFF) | 0x1A000000, 20));

            SparklineView spark = card.findViewById(R.id.summarySparkline);
            spark.setLineColor(accent);
            spark.setValues(toFloatArray(c.sparkline));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

            // If not the last item, then add margin end
            if (i < cards.size() - 1) {
                lp.setMarginEnd(10 * density);
            }

            card.setLayoutParams(lp);
            row.addView(card);
        }
    }

    // ---- Overview tiles ---------------------------------------------------

    private void bindOverview(List<DashboardData.OverviewItem> overview) {
        if (overview == null) {
            return;
        }
        for (int i = 0; i < overview.size() && i < kpiValueViews.length; i++) {
            DashboardData.OverviewItem item = overview.get(i);
            if (kpiValueViews[i] != null) {
                kpiValueViews[i].setText(formatCount(item.value));
            }
            if (kpiActionViews[i] != null) {
                final String route = item.action_route;
                kpiActionViews[i].setOnClickListener(v -> navigateRoute(route));
            }
        }
    }

    private void navigateRoute(String route) {
        if (route == null) {
            selectTab(R.id.nav_orders);
            return;
        }
        switch (route) {
            case "inventory":
                selectTab(R.id.nav_inventory);
                break;
            case "traders":
                startActivity(new Intent(requireContext(), TradersActivity.class));
                break;
            case "orders":
            default:
                selectTab(R.id.nav_orders);
                break;
        }
    }

    private void selectTab(int navId) {
        if (getActivity() != null) {
            BottomNavigationView nav = getActivity().findViewById(R.id.bottomNav);
            if (nav != null) {
                nav.setSelectedItemId(navId);
            }
        }
    }

    // ---- Revenue overview -------------------------------------------------

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
            int trendColor = up ? 0xFF16A34A : 0xFFDC2626;
            trend.setTextColor(trendColor);
            trend.setBackground(roundedRect((trendColor & 0x00FFFFFF) | 0x1A000000, 20));
        }
        TextView compare = root.findViewById(R.id.revenueCompare);
        if (compare != null && dashboard.meta != null && dashboard.meta.compare_period_label != null) {
            compare.setText(dashboard.meta.compare_period_label);
        }
        LineChartView chart = root.findViewById(R.id.revenueChart);
        if (chart != null && revenue.chart != null && revenue.chart.values != null
                && !revenue.chart.values.isEmpty()) {
            chart.setValues(toFloatArray(revenue.chart.values));
        }
    }

    // ---- Top selling categories ------------------------------------------

    private void bindCategories(View root, DashboardData.TopCategories categories) {
        DonutChartView donut = root.findViewById(R.id.categoriesDonut);
        LinearLayout legend = root.findViewById(R.id.categoriesLegend);
        if (donut == null || legend == null || categories == null || categories.items == null
                || categories.items.isEmpty()) {
            return;
        }
        List<DashboardData.CategoryItem> items = categories.items;
        float[] values = new float[items.size()];
        int[] colors = new int[items.size()];
        for (int i = 0; i < items.size(); i++) {
            values[i] = (float) Math.max(0.0001, items.get(i).percentage);
            colors[i] = parseColor(items.get(i).color, 0xFF7C3AED);
        }
        donut.setStrokeWidthDp(15);
        donut.setData(values, colors);
        donut.setCenterText(compactCurrency(categories.total), getString(R.string.dashboard_total_caption));

        legend.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (DashboardData.CategoryItem item : items) {
            View row = inflater.inflate(R.layout.item_category_legend, legend, false);
            View dot = row.findViewById(R.id.legendDot);
            GradientDrawable dotBg = (GradientDrawable) dot.getBackground().mutate();
            dotBg.setColor(parseColor(item.color, 0xFF7C3AED));
            ((TextView) row.findViewById(R.id.legendName)).setText(item.name);
            ((TextView) row.findViewById(R.id.legendAmount)).setText(item.formatted_amount);
            legend.addView(row);
        }
    }

    // ---- Recent orders ----------------------------------------------------

    private void bindRecentOrders(View root, DashboardData.RecentOrders recentOrders) {
        LinearLayout list = root.findViewById(R.id.recentOrdersList);
        if (list == null || recentOrders == null || recentOrders.items == null) {
            return;
        }
        list.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (int i = 0; i < recentOrders.items.size(); i++) {
            DashboardData.RecentOrder order = recentOrders.items.get(i);

            View row = inflater.inflate(R.layout.item_recent_order, list, false);
            ((TextView) row.findViewById(R.id.orderId)).setText(order.order_id);
            ((TextView) row.findViewById(R.id.orderTrader)).setText(order.trader);
            ((TextView) row.findViewById(R.id.orderDate)).setText(order.formatted_date);
            ((TextView) row.findViewById(R.id.orderAmount)).setText(order.formatted_amount);

            TextView status = row.findViewById(R.id.orderStatus);
            int statusColor = parseColor(order.status_color, 0xFF7C3AED);
            status.setText(order.status_label);
            status.setTextColor(statusColor);
            status.setBackground(roundedRect((statusColor & 0x00FFFFFF) | 0x1A000000, 20));
            list.addView(row);

            // Add separator except after the last item
            if (i < recentOrders.items.size() - 1) {
                View separator = new View(requireContext());

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, (int) (1 * getResources().getDisplayMetrics().density));

                separator.setLayoutParams(params);
                separator.setBackgroundColor(0xFFE5E7EB); // divider color

                list.addView(separator);
            }
        }
    }

    // ---- Helpers ----------------------------------------------------------

    private int iconForKey(String key) {
        if (key == null) {
            return R.drawable.ic_wallet;
        }
        switch (key) {
            case "total_orders":
                return R.drawable.ic_orders;
            case "active_traders":
                return R.drawable.ic_traders;
            case "products_sold":
                return R.drawable.ic_package;
            case "total_revenue":
            default:
                return R.drawable.ic_wallet;
        }
    }

    private int accentColor(String name) {
        if (name == null) {
            return 0xFF7C3AED;
        }
        switch (name) {
            case "blue":
                return 0xFF2563EB;
            case "green":
                return 0xFF16A34A;
            case "orange":
                return 0xFFEA580C;
            case "purple":
            default:
                return 0xFF7C3AED;
        }
    }

    private int lightAccent(String name) {
        return (accentColor(name) & 0x00FFFFFF) | 0x22000000;
    }

    private GradientDrawable roundedRect(int color, float radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setColor(color);
        d.setCornerRadius(radiusDp * getResources().getDisplayMetrics().density);
        return d;
    }

    private int parseColor(String hex, int fallback) {
        if (hex == null || hex.isEmpty()) {
            return fallback;
        }
        try {
            return Color.parseColor(hex);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private String compactCurrency(double value) {
        double abs = Math.abs(value);
        if (abs >= 1_000_000) {
            return String.format(java.util.Locale.US, "%s%.1fM", currencySymbol, value / 1_000_000);
        }
        if (abs >= 1_000) {
            return String.format(java.util.Locale.US, "%s%.0fK", currencySymbol, value / 1_000);
        }
        return String.format(java.util.Locale.US, "%s%.0f", currencySymbol, value);
    }

    private float[] toFloatArray(List<Float> list) {
        if (list == null || list.isEmpty()) {
            return new float[]{0, 0};
        }
        float[] arr = new float[list.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = list.get(i) == null ? 0f : list.get(i);
        }
        return arr;
    }

    private String formatCount(long value) {
        return String.format(java.util.Locale.US, "%,d", value);
    }
}
