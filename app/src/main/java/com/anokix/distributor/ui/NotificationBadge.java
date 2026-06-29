package com.anokix.distributor.ui;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.anokix.distributor.network.ApiCallback;
import com.anokix.distributor.network.ApiClient;
import com.anokix.distributor.network.dto.UnreadCountData;

/**
 * Shared helper that fills a small header bell badge from the live unread count
 * (GET api/common/notifications/unread-count). Hides the badge at zero, shows
 * "9+" beyond nine. Used by the home/products/inventory/more headers; call from
 * {@code onResume} so it refreshes after the user visits the Notifications screen.
 */
public final class NotificationBadge {

    private NotificationBadge() {}

    public static void refresh(Context context, final TextView badge) {
        if (badge == null || context == null) return;
        ApiClient.get(context).getUnreadCount(new ApiCallback<UnreadCountData>() {
            @Override
            public void onSuccess(UnreadCountData data) {
                int count = data == null ? 0 : data.unread_count;
                if (count <= 0) {
                    badge.setVisibility(View.GONE);
                } else {
                    badge.setVisibility(View.VISIBLE);
                    badge.setText(count > 9 ? "9+" : String.valueOf(count));
                }
            }

            @Override
            public void onError(String message) {
                badge.setVisibility(View.GONE);
            }
        });
    }
}
