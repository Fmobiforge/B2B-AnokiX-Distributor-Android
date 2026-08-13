package com.anokix.distributorapp.ui.fragment;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
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

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.data.MockData;
import com.anokix.distributorapp.network.ApiCallback;
import com.anokix.distributorapp.network.ApiClient;
import com.anokix.distributorapp.network.dto.BaseInfoData;
import com.anokix.distributorapp.network.dto.DashboardData;
import com.anokix.distributorapp.network.dto.WalletData;
import com.anokix.distributorapp.session.SessionManager;
import com.anokix.distributorapp.ui.MainActivity;
import com.anokix.distributorapp.ui.NotificationBadge;
import com.anokix.distributorapp.ui.NotificationsActivity;
import com.anokix.distributorapp.ui.TradersActivity;
import com.anokix.distributorapp.ui.views.DonutChartView;
import com.anokix.distributorapp.ui.views.LineChartView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

public class DashboardFragment extends Fragment {

    private final TextView[] kpiValueViews = new TextView[4];
    private final View[] kpiActionViews = new View[4];
    private String currencySymbol = "R";

    private WalletData wallet;
    private BottomSheetDialog walletSheet;
    private View walletSheetContent;

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

        view.findViewById(R.id.walletCard).setOnClickListener(v -> showWalletSheet());

        bindWelcome(view);
        loadDistributorName(view);
        bindProfileBadge(view);
        buildKpiGrid(view);
        buildSummaryCards(view, MockData.fallbackSummaryCards());
        loadDashboard(view);
        loadWallet(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null) {
            NotificationBadge.refresh(requireContext(), getView().findViewById(R.id.notificationBadge));
        }
    }

    /**
     * Header identity block, matching the web dashboard's "ApplePay / Tam Ho": the
     * distributor's registered legal name on top and the signed-in user under it.
     *
     * <p>Login only returns {@code portal_info.company_name}; the legal name comes from
     * {@code base-info} and is cached in the session, so on later launches it paints
     * before that call lands. Until any company name is known the greeting takes the top
     * line, so the header is never blank.
     */
    private void bindWelcome(View view) {
        TextView title = view.findViewById(R.id.welcomeText);
        TextView subtitle = view.findViewById(R.id.dashboardSubtitle);
        if (title == null) {
            return;
        }

        SessionManager session = SessionManager.get(requireContext());
        String firstName = session.getFirstName();
        String greeting = firstName.isEmpty()
                ? getString(R.string.welcome_back_generic)
                : getString(R.string.welcome_back_named, firstName);
        String distributor = session.getDistributorName();

        title.setText(distributor.isEmpty() ? greeting : distributor);
        if (subtitle != null) {
            subtitle.setText(distributor.isEmpty() ? getString(R.string.dashboard_subtitle) : greeting);
        }
    }

    /**
     * Fills in the distributor's registered legal name, which login does not return.
     * Cached in the session so the header paints from it immediately next time.
     */
    private void loadDistributorName(View view) {
        ApiClient.get(requireContext()).getBaseInfo(new ApiCallback<BaseInfoData>() {
            @Override
            public void onSuccess(BaseInfoData data) {
                if (data == null || data.portal_info == null || !isAdded()) {
                    return;
                }
                String legalName = data.portal_info.company_legal_name;
                if (legalName == null || legalName.trim().isEmpty()) {
                    // Fall back to the trading name rather than leaving the header on the
                    // login-time display name, which may be neither.
                    legalName = data.portal_info.trading_name;
                }
                SessionManager.get(requireContext()).setCompanyLegalName(legalName);
                bindWelcome(view);
            }

            @Override
            public void onError(String message) {
                // The header already shows the login-time name; nothing to report here.
            }
        });
    }

    /**
     * Fills the header avatar circle: shows the distributor company logo (from the login
     * {@code portal_info}) when a valid URL is available, otherwise the user's initial.
     */
    private void bindProfileBadge(View view) {
        TextView initials = view.findViewById(R.id.profileInitials);
        ImageView logo = view.findViewById(R.id.profileLogo);
        if (initials == null || logo == null) {
            return;
        }

        SessionManager session = SessionManager.get(requireContext());
        initials.setText(initialOf(session.getFirstName()));

        String logoUrl = session.getCompanyLogoUrl();
        if (!isLoadableUrl(logoUrl)) {
            logo.setVisibility(View.GONE);
            initials.setVisibility(View.VISIBLE);
            return;
        }

        Glide.with(this)
                .load(logoUrl)
                // Decode at original size so loading does not depend on the (initially gone)
                // view being measured — otherwise onResourceReady never fires.
                .override(Target.SIZE_ORIGINAL)
                .circleCrop()
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                @NonNull Target<Drawable> target, boolean isFirstResource) {
                        logo.setVisibility(View.GONE);
                        initials.setVisibility(View.VISIBLE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(@NonNull Drawable resource, @NonNull Object model,
                                                   Target<Drawable> target, @NonNull DataSource dataSource,
                                                   boolean isFirstResource) {
                        initials.setVisibility(View.GONE);
                        logo.setVisibility(View.VISIBLE);
                        return false;
                    }
                })
                .into(logo);
    }

    private static String initialOf(String name) {
        return (name == null || name.trim().isEmpty())
                ? "" : name.trim().substring(0, 1).toUpperCase(java.util.Locale.US);
    }

    private static boolean isLoadableUrl(String url) {
        return !TextUtils.isEmpty(url)
                && (url.startsWith("http://") || url.startsWith("https://"));
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

            TextView compare = card.findViewById(R.id.summaryCompare);
            compare.setText(c.compare_label != null ? c.compare_label : "");

            // Coloured accent underline (replaces the now-empty sparkline).
            View underline = card.findViewById(R.id.summaryUnderline);
            GradientDrawable bar = new GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{accent, (accent & 0x00FFFFFF) | 0x66000000});
            bar.setCornerRadius(2 * density);
            underline.setBackground(bar);

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
            chart.setCurrencySymbol(currencySymbol);
            chart.setValues(toFloatArray(revenue.chart.values));
            if (revenue.chart.labels != null && !revenue.chart.labels.isEmpty()) {
                chart.setLabels(revenue.chart.labels.toArray(new String[0]));
            }
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
            ((TextView) row.findViewById(R.id.orderDate)).setText(friendlyOrderDate(order));
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

    // ---- Wallet -----------------------------------------------------------

    private void loadWallet(View root) {
        ApiClient.get(requireContext()).getWallet(new ApiCallback<WalletData>() {
            @Override
            public void onSuccess(WalletData data) {
                if (!isAdded() || data == null) {
                    return;
                }
                wallet = data;
                bindWalletCard(root);
                if (walletSheet != null && walletSheet.isShowing()) {
                    bindWalletSheet(walletSheetContent);
                }
            }

            @Override
            public void onError(String message) {
                // Wallet is best-effort — leave the card hidden if it can't be loaded.
            }
        });
    }

    /** Reveal + fill the top wallet card once a balance is available. */
    private void bindWalletCard(View root) {
        View card = root.findViewById(R.id.walletCard);
        TextView value = root.findViewById(R.id.walletBalanceValue);
        if (card == null || value == null || wallet == null || wallet.balance == null) {
            return;
        }
        value.setText(formatMoney(wallet.balance.available));
        card.setVisibility(View.VISIBLE);
    }

    private void showWalletSheet() {
        if (wallet == null || wallet.balance == null) {
            return;
        }
        walletSheet = new BottomSheetDialog(requireContext());
        View content = LayoutInflater.from(requireContext())
                .inflate(R.layout.sheet_wallet, null, false);
        walletSheetContent = content;
        walletSheet.setContentView(content);

        content.findViewById(R.id.walletRefresh).setOnClickListener(v -> refreshWallet(content));

        bindWalletSheet(content);
        walletSheet.show();
    }

    private void bindWalletSheet(View content) {
        if (content == null || wallet == null || wallet.balance == null) {
            return;
        }
        setText(content, R.id.walletAvailable, formatMoney(wallet.balance.available));
        setText(content, R.id.walletCurrent, formatMoney(wallet.balance.current));
        setText(content, R.id.walletPending, formatMoney(wallet.balance.pending));

        String asOf = parseTime(wallet.balance.as_of);
        TextView asOfView = content.findViewById(R.id.walletAsOf);
        if (asOfView != null) {
            if (asOf.isEmpty()) {
                asOfView.setVisibility(View.GONE);
            } else {
                asOfView.setVisibility(View.VISIBLE);
                asOfView.setText(getString(R.string.wallet_as_of, asOf));
            }
        }
    }

    private void refreshWallet(View content) {
        final ImageView icon = content.findViewById(R.id.walletRefreshIcon);
        if (icon != null) {
            icon.animate().rotationBy(360f).setDuration(600).start();
        }
        ApiClient.get(requireContext()).getWallet(new ApiCallback<WalletData>() {
            @Override
            public void onSuccess(WalletData data) {
                if (!isAdded() || data == null) {
                    return;
                }
                wallet = data;
                bindWalletSheet(content);
                if (getView() != null) {
                    bindWalletCard(getView());
                }
            }

            @Override
            public void onError(String message) {
                // Keep the previous values on a refresh failure.
            }
        });
    }

    private void setText(View parent, int id, String text) {
        TextView view = parent.findViewById(id);
        if (view != null) {
            view.setText(text);
        }
    }

    private String formatMoney(double value) {
        return currencySymbol + String.format(java.util.Locale.US, "%,.2f", value);
    }

    /** Recent-order timestamp: the API's {@code formatted_date} is a broken epoch, so
     * format the raw {@code date} ("dd/MM/yyyy HH:mm:ss") into a readable label. */
    private String friendlyOrderDate(DashboardData.RecentOrder order) {
        if (order.date != null && !order.date.isEmpty()) {
            try {
                java.text.SimpleDateFormat in =
                        new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.US);
                java.text.SimpleDateFormat out =
                        new java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.US);
                return out.format(in.parse(order.date));
            } catch (Exception ignored) {
                return order.date;
            }
        }
        return order.formatted_date == null ? "" : order.formatted_date;
    }

    /** Extract "HH:mm" (device-local) from an ISO-8601 timestamp; "" if unparseable. */
    private String parseTime(String iso) {
        if (iso == null || iso.isEmpty()) {
            return "";
        }
        try {
            java.text.SimpleDateFormat in =
                    new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.US);
            java.text.SimpleDateFormat out =
                    new java.text.SimpleDateFormat("HH:mm", java.util.Locale.US);
            return out.format(in.parse(iso));
        } catch (Exception ignored) {
            return "";
        }
    }
}
