package com.anokix.distributor.messaging;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.anokix.distributor.R;
import com.anokix.distributor.session.SessionManager;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Entry point for Firebase Cloud Messaging.
 *
 * <ul>
 *   <li>{@link #onNewToken} — a new/rotated token is registered with the backend.</li>
 *   <li>{@link #onMessageReceived} — a push arrived while the app process is alive
 *       (foreground, or a <em>data</em> message in the background): we build the
 *       notification ourselves so the tap routes to the right screen.</li>
 * </ul>
 *
 * Backend recommendation: send <b>data</b> messages (or notification + data with the
 * routing fields in <code>data</code>) carrying <code>title</code>, <code>body</code>,
 * <code>type</code>, <code>route</code>, <code>order_id</code> and <code>grn_id</code>.
 */
public class AxFirebaseMessagingService extends FirebaseMessagingService {

    private static final AtomicInteger NOTIFICATION_ID = new AtomicInteger(1000);

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        PushManager.registerToken(getApplicationContext(), token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);

        // Don't surface pushes for a signed-out device.
        if (!SessionManager.get(getApplicationContext()).isLoggedIn()) return;

        Map<String, String> data = message.getData();
        RemoteMessage.Notification n = message.getNotification();
        if (data.isEmpty() && n == null) return;

        String title = firstNonEmpty(n != null ? n.getTitle() : null, data.get("title"),
                getString(R.string.app_name));
        String body = firstNonEmpty(n != null ? n.getBody() : null, data.get("body"), "");
        String type = data.get("type");
        String route = data.get("route");
        String orderId = data.get("order_id");
        String grnId = data.get("grn_id");

        showNotification(getApplicationContext(), title, body, type, route, orderId, grnId);
    }

    private void showNotification(Context context, String title, String body,
                                  String type, String route, String orderId, String grnId) {
        PushManager.ensureChannel(context);

        Intent intent = PushManager.routingIntent(context, type, route, orderId, grnId);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT
                | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_IMMUTABLE : 0);
        int notificationId = NOTIFICATION_ID.incrementAndGet();
        PendingIntent pending = PendingIntent.getActivity(context, notificationId, intent, flags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, getString(R.string.fcm_default_channel_id))
                .setSmallIcon(R.drawable.ic_notifications)
                .setColor(Color.parseColor("#7C3AED"))
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pending);

        try {
            // POST_NOTIFICATIONS is requested at runtime in MainActivity; if the user
            // denied it, notify() is a silent no-op rather than a crash.
            NotificationManagerCompat.from(context).notify(notificationId, builder.build());
        } catch (SecurityException ignored) {
        }
    }

    private static String firstNonEmpty(String... values) {
        if (values == null) return "";
        for (String v : values) {
            if (v != null && !v.isEmpty()) return v;
        }
        return "";
    }
}
