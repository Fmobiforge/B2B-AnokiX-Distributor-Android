package com.anokix.distributorapp.ui;

import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.messaging.PushManager;
import com.anokix.distributorapp.model.NotificationItem;
import com.anokix.distributorapp.network.ApiCallback;
import com.anokix.distributorapp.network.ApiClient;
import com.anokix.distributorapp.network.dto.NotificationsData;
import com.anokix.distributorapp.ui.adapter.NotificationAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Notifications — live feed from GET api/common/notifications (filter all/unread, paged).
 * KPI cards (total/read/unread/important), All/Unread tabs, "Mark all as read", and
 * tap-to-open: tapping a row marks it read (POST .../read) and routes to the related
 * screen via {@link PushManager#navigate}, which is the same resolver a push tap uses
 * (see {@code FCM-settings/3-Event-Catalogue.md}).
 */
public class NotificationsActivity extends BaseSecondaryActivity {

    private static final int PER_PAGE = 8;

    private final List<NotificationsData.Notification> current = new ArrayList<>();
    private NotificationAdapter adapter;

    private String filter = "all";   // "all" | "unread"
    private int page = 1;
    private int totalPages = 1;
    private int total = 0;
    private int unreadCount = 0;

    private TextView statAll, statRead, statUnread, statImportant, lblRead, lblUnread;
    private TextView tabAll, tabUnread, pageInfo;
    private View loading, emptyView, paginationBar, btnPrev, btnNext;

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_notifications;
    }

    @Override
    protected String getScreenTitle() {
        return getString(R.string.notifications);
    }

    @Override
    protected void bindViews() {
        statAll = findViewById(R.id.statAll);
        statRead = findViewById(R.id.statRead);
        statUnread = findViewById(R.id.statUnread);
        statImportant = findViewById(R.id.statImportant);
        lblRead = findViewById(R.id.lblRead);
        lblUnread = findViewById(R.id.lblUnread);
        tabAll = findViewById(R.id.tabAll);
        tabUnread = findViewById(R.id.tabUnread);
        pageInfo = findViewById(R.id.pageInfo);
        loading = findViewById(R.id.loading);
        emptyView = findViewById(R.id.emptyView);
        paginationBar = findViewById(R.id.paginationBar);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);

        RecyclerView list = findViewById(R.id.notificationsList);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter();
        adapter.setOnNotificationClick(this::onItemClicked);
        list.setAdapter(adapter);

        tabAll.setOnClickListener(v -> selectFilter("all"));
        tabUnread.setOnClickListener(v -> selectFilter("unread"));
        findViewById(R.id.markAllRead).setOnClickListener(v -> markAllRead());
        btnPrev.setOnClickListener(v -> { if (page > 1) load(page - 1); });
        btnNext.setOnClickListener(v -> { if (page < totalPages) load(page + 1); });

        applyTabStyles();
    }

    @Override
    protected void loadData() {
        load(1);
    }

    // ---- Data ------------------------------------------------------------

    private void selectFilter(String f) {
        if (f.equals(filter)) return;
        filter = f;
        applyTabStyles();
        load(1);
    }

    private void load(int requestedPage) {
        loading.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        ApiClient.get(this).getNotifications(filter, requestedPage, PER_PAGE,
                new ApiCallback<NotificationsData>() {
                    @Override
                    public void onSuccess(NotificationsData data) {
                        loading.setVisibility(View.GONE);
                        current.clear();
                        if (data != null && data.notifications != null) current.addAll(data.notifications);
                        if (data != null) {
                            unreadCount = data.unread_count;
                            if (data.pagination != null) {
                                page = data.pagination.page;
                                totalPages = Math.max(1, data.pagination.total_pages);
                                total = data.pagination.total;
                            }
                        }
                        bindList();
                        bindSummary();
                        bindPagination();
                    }

                    @Override
                    public void onError(String message) {
                        loading.setVisibility(View.GONE);
                        Toast.makeText(NotificationsActivity.this,
                                message != null ? message : getString(R.string.notifications_load_failed),
                                Toast.LENGTH_SHORT).show();
                        bindList();
                    }
                });
    }

    private void bindList() {
        List<NotificationItem> items = new ArrayList<>();
        for (NotificationsData.Notification n : current) items.add(toItem(n));
        adapter.setItems(items);
        emptyView.setVisibility(current.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void bindSummary() {
        int read = Math.max(0, total - unreadCount);
        int important = 0;
        for (NotificationsData.Notification n : current) if (isImportant(n)) important++;

        statAll.setText(String.valueOf(total));
        statRead.setText(String.valueOf(read));
        statUnread.setText(String.valueOf(unreadCount));
        statImportant.setText(String.valueOf(important));
        lblRead.setText(percent(read, total));
        lblUnread.setText(percent(unreadCount, total));

        tabUnread.setText(unreadCount > 0
                ? getString(R.string.unread) + " (" + unreadCount + ")"
                : getString(R.string.unread));
    }

    private void bindPagination() {
        if (total <= 0) {
            paginationBar.setVisibility(View.GONE);
            return;
        }
        paginationBar.setVisibility(View.VISIBLE);
        int from = (page - 1) * PER_PAGE + 1;
        int to = Math.min(page * PER_PAGE, total);
        pageInfo.setText(getString(R.string.showing_range, from, to, total));
        btnPrev.setEnabled(page > 1);
        btnPrev.setAlpha(page > 1 ? 1f : 0.3f);
        btnNext.setEnabled(page < totalPages);
        btnNext.setAlpha(page < totalPages ? 1f : 0.3f);
    }

    private void applyTabStyles() {
        boolean all = "all".equals(filter);
        tabAll.setTextColor(getColor(all ? R.color.purple_primary : R.color.text_secondary));
        tabUnread.setTextColor(getColor(all ? R.color.text_secondary : R.color.purple_primary));
    }

    // ---- Actions ---------------------------------------------------------

    private void onItemClicked(int position) {
        if (position < 0 || position >= current.size()) return;
        NotificationsData.Notification n = current.get(position);

        if (!n.is_read) {
            n.is_read = true;
            if (unreadCount > 0) unreadCount--;
            ApiClient.get(this).markNotificationRead(n.id, new ApiCallback<Void>() {
                @Override public void onSuccess(Void unused) {}
                @Override public void onError(String message) {}
            });
            // Reflect the change immediately (dot + KPIs).
            if ("unread".equals(filter)) {
                current.remove(position);
                bindList();
            } else {
                adapter.notifyItemChanged(position);
            }
            bindSummary();
        }
        navigate(n);
    }

    private void markAllRead() {
        ApiClient.get(this).markAllNotificationsRead(new ApiCallback<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Toast.makeText(NotificationsActivity.this, R.string.all_marked_read,
                        Toast.LENGTH_SHORT).show();
                load(1);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(NotificationsActivity.this,
                        message != null ? message : getString(R.string.notifications_load_failed),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Open the screen related to the notification's type/route. Delegates to
     * {@link PushManager#navigate} so a row tap and a push tap for the same payload
     * always land on the same screen. {@code fallbackToFeed} is false — we are already
     * on the feed, so an unroutable row simply does nothing.
     */
    private void navigate(NotificationsData.Notification n) {
        String route = n.data != null ? n.data.route : null;
        PushManager.navigate(this, n.type, route, idsOf(n), false);
    }

    /** Maps the in-app row's numeric ids onto the string-keyed {@link PushManager.Ids}. */
    private static PushManager.Ids idsOf(NotificationsData.Notification n) {
        PushManager.Ids ids = new PushManager.Ids();
        if (n.data == null) return ids;
        ids.orderId = id(n.data.order_id);
        ids.grnId = id(n.data.grn_id);
        ids.grvId = id(n.data.grv_id);
        ids.deliveryId = id(n.data.delivery_id);
        ids.invoiceId = id(n.data.invoice_id);
        ids.productId = id(n.data.product_id);
        ids.promotionId = id(n.data.promotion_id);
        ids.traderId = id(n.data.trader_id);
        ids.transactionId = id(n.data.transaction_id);
        ids.notificationId = n.id == null ? "" : n.id;
        return ids;
    }

    private static String id(long value) {
        return value > 0 ? String.valueOf(value) : "";
    }

    // ---- Mapping / helpers ----------------------------------------------

    /**
     * Icon, accent and action label per destination — keyed off the same
     * {@link PushManager#routeKey} the tap uses, so the row's affordance and where it
     * actually goes can never disagree.
     */
    private NotificationItem toItem(NotificationsData.Notification n) {
        int icon, bg, tint;
        String action;
        String type = n.type == null ? "" : n.type;
        String route = n.data != null ? n.data.route : null;

        // Same ids as the tap passes, so the row's icon and its destination are resolved
        // from identical inputs and cannot disagree.
        switch (PushManager.routeKey(type, route, idsOf(n))) {
            case PushManager.ROUTE_RETURN:
                icon = R.drawable.ic_returns; bg = R.drawable.bg_kpi_icon_orange;
                tint = R.color.warning; action = getString(R.string.view_returns);
                break;
            case PushManager.ROUTE_DELIVERY:
                icon = R.drawable.ic_deliveries; bg = R.drawable.bg_kpi_icon_blue;
                tint = R.color.info; action = getString(R.string.view_delivery);
                break;
            case PushManager.ROUTE_INVENTORY:
            case PushManager.ROUTE_PRODUCT:
            case PushManager.ROUTE_MARKETPLACE:
                icon = R.drawable.ic_inventory; bg = R.drawable.bg_kpi_icon_blue;
                tint = R.color.info; action = getString(R.string.view_inventory);
                break;
            case PushManager.ROUTE_ORDER:
            case PushManager.ROUTE_GRV:
                icon = R.drawable.ic_orders; bg = R.drawable.bg_kpi_icon_purple;
                tint = R.color.purple_primary; action = getString(R.string.view_order);
                break;
            case PushManager.ROUTE_INVOICE:
            case PushManager.ROUTE_FINANCE:
                icon = R.drawable.ic_finances; bg = R.drawable.bg_kpi_icon_green;
                tint = R.color.success; action = getString(R.string.view_finances);
                break;
            case PushManager.ROUTE_WALLET:
                icon = R.drawable.ic_wallet; bg = R.drawable.bg_kpi_icon_green;
                tint = R.color.success; action = getString(R.string.view_wallet);
                break;
            case PushManager.ROUTE_VAS:
                icon = R.drawable.ic_vas; bg = R.drawable.bg_kpi_icon_blue;
                tint = R.color.info; action = getString(R.string.view_airtime);
                break;
            case PushManager.ROUTE_PROMOTION:
                icon = R.drawable.ic_promotions; bg = R.drawable.bg_kpi_icon_purple;
                tint = R.color.purple_primary; action = getString(R.string.view_promotion);
                break;
            case PushManager.ROUTE_REPORT:
                icon = R.drawable.ic_reports; bg = R.drawable.bg_kpi_icon_blue;
                tint = R.color.info; action = getString(R.string.view_report);
                break;
            case PushManager.ROUTE_TRADER:
                icon = R.drawable.ic_traders; bg = R.drawable.bg_kpi_icon_purple;
                tint = R.color.purple_primary; action = getString(R.string.view_trader);
                break;
            default:
                icon = R.drawable.ic_notifications; bg = R.drawable.bg_kpi_icon_purple;
                tint = R.color.purple_primary; action = "";
                break;
        }
        return new NotificationItem(n.id, safe(n.title), safe(n.body), relativeTime(n.created_at),
                type, icon, bg, tint, action, !n.is_read, isImportant(n));
    }

    /**
     * Important = any return decision, or an order whose status is Failed/Cancelled.
     * Mirrors the web portal:
     *   important = type=="return.decision"
     *            || (type=="order.status" && status in {"Failed","Cancelled"})
     *
     * <p>The comparison is case-insensitive and also reads {@code status_key}: the push
     * contract uses machine values ("cancelled", "out_for_delivery") while the portal uses
     * display case, and a row built from either source has to raise the same flag.
     */
    private static boolean isImportant(NotificationsData.Notification n) {
        if (n.type == null) return false;
        if ("return.decision".equals(n.type)) return true;
        if ("order.status".equals(n.type) && n.data != null) {
            return isFailedOrCancelled(n.data.status) || isFailedOrCancelled(n.data.status_key);
        }
        return false;
    }

    private static boolean isFailedOrCancelled(String status) {
        if (status == null) return false;
        String s = status.trim().toLowerCase(Locale.US);
        return "failed".equals(s) || "cancelled".equals(s) || "canceled".equals(s);
    }

    private static String percent(int part, int whole) {
        if (whole <= 0) return "0%";
        return String.format(Locale.US, "%.1f%%", 100.0 * part / whole);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    /** Recent (< 24h) → "x hours/minutes ago"; older → "dd MMM yyyy, HH:mm". */
    private static String relativeTime(String raw) {
        Date d = parse(raw);
        if (d == null) return raw == null ? "" : raw;
        long diff = System.currentTimeMillis() - d.getTime();
        if (diff >= 0 && diff < 24L * 60 * 60 * 1000) {
            long hours = diff / (60L * 60 * 1000);
            if (hours >= 1) return hours + (hours == 1 ? " hour ago" : " hours ago");
            long mins = diff / (60L * 1000);
            if (mins >= 1) return mins + (mins == 1 ? " minute ago" : " minutes ago");
            return "Just now";
        }
        return new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(d);
    }

    private static Date parse(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        try {
            return new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US).parse(raw);
        } catch (Exception e) {
            return null;
        }
    }
}
