package com.anokix.distributor.ui;

import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.anokix.distributor.R;
import com.anokix.distributor.ui.adapter.NotificationAdapter;

public class NotificationsActivity extends BaseSecondaryActivity {

    private NotificationAdapter adapter;
    private String currentFilter = "all";
    private TextView unreadCount;
    private TextView tabAll, tabUnread, tabOrders;
    private View tabAllIndicator, tabUnreadIndicator, tabOrdersIndicator;
    private View tabAllWrap, tabUnreadWrap, tabOrdersWrap;

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
        unreadCount = findViewById(R.id.unreadCount);
        tabAll = findViewById(R.id.tabAll);
        tabUnread = findViewById(R.id.tabUnread);
        tabOrders = findViewById(R.id.tabOrders);
        tabAllIndicator = findViewById(R.id.tabAllIndicator);
        tabUnreadIndicator = findViewById(R.id.tabUnreadIndicator);
        tabOrdersIndicator = findViewById(R.id.tabOrdersIndicator);
        tabAllWrap = findViewById(R.id.tabAllWrap);
        tabUnreadWrap = findViewById(R.id.tabUnreadWrap);
        tabOrdersWrap = findViewById(R.id.tabOrdersWrap);

        RecyclerView list = findViewById(R.id.notificationsList);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter();
        list.setAdapter(adapter);

        findViewById(R.id.markAllRead).setOnClickListener(v -> {
            repository.markAllNotificationsRead();
            refreshList();
            Toast.makeText(this, "All notifications marked as read", Toast.LENGTH_SHORT).show();
        });

        tabAllWrap.setOnClickListener(v -> selectTab("all"));
        tabUnreadWrap.setOnClickListener(v -> selectTab("unread"));
        tabOrdersWrap.setOnClickListener(v -> selectTab("orders"));
    }

    @Override
    protected void loadData() {
        selectTab("all");
    }

    private void refreshList() {
        int count = repository.getUnreadNotificationCount();
        unreadCount.setText(getString(R.string.unread_count, count));
        adapter.setItems(repository.getNotifications(currentFilter));
    }

    private void selectTab(String filter) {
        currentFilter = filter;

        int primary = getResources().getColor(R.color.purple_primary);
        int secondary = getResources().getColor(R.color.text_secondary);

        tabAll.setTextColor(filter.equals("all") ? primary : secondary);
        tabUnread.setTextColor(filter.equals("unread") ? primary : secondary);
        tabOrders.setTextColor(filter.equals("orders") ? primary : secondary);

        tabAllIndicator.setVisibility(filter.equals("all") ? View.VISIBLE : View.INVISIBLE);
        tabUnreadIndicator.setVisibility(filter.equals("unread") ? View.VISIBLE : View.INVISIBLE);
        tabOrdersIndicator.setVisibility(filter.equals("orders") ? View.VISIBLE : View.INVISIBLE);

        refreshList();
    }
}
