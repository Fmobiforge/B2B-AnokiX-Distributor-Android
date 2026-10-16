package com.anokix.distributorapp.messaging;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.session.SessionManager;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.LinkedHashSet;
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
 * Backend contract: every push carries <code>type</code>, <code>route</code>,
 * <code>notification_id</code> and whichever ids the event needs (<code>order_id</code>,
 * <code>grn_id</code>, <code>delivery_id</code>, <code>product_id</code>, …) in
 * <code>data</code>. The server also sends a <code>notification</code> block, so this class
 * only runs while the app is in the foreground — the backgrounded case is drawn by the Firebase
 * SDK and picked up in {@code SplashActivity}. See {@code FCM-settings/3-Event-Catalogue.md}.
 */
public class AxFirebaseMessagingService extends FirebaseMessagingService {

    private static final AtomicInteger NOTIFICATION_ID = new AtomicInteger(1000);

    /**
     * Recently-shown {@code notification_id}s. FCM guarantees at-least-once delivery, so the
     * same push can arrive twice; without this the duplicate would re-alert. Bounded because
     * a messaging service is long-lived.
     */
    private static final int SEEN_LIMIT = 64;
    private static final LinkedHashSet<String> SEEN = new LinkedHashSet<>();

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
        if (alreadyShown(data.get("notification_id"))) return;

        String title = firstNonEmpty(n != null ? n.getTitle() : null, data.get("title"),
                getString(R.string.app_name));
        String body = firstNonEmpty(n != null ? n.getBody() : null, data.get("body"), "");

        showNotification(getApplicationContext(), title, body, data);
    }

    private void showNotification(Context context, String title, String body,
                                  Map<String, String> data) {
        PushManager.ensureChannel(context);

        Intent intent = PushManager.routingIntent(context, data);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT
                | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_IMMUTABLE : 0);
        int notificationId = trayId(data.get("notification_id"));
        PendingIntent pending = PendingIntent.getActivity(context, notificationId, intent, flags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, getString(R.string.fcm_default_channel_id))
                .setSmallIcon(R.drawable.ic_notifications)
                .setColor(androidx.core.content.ContextCompat.getColor(context, R.color.purple_primary))
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                // Paired with the notification_id-derived tray id: a redelivery of the same
                // push updates its entry in place instead of buzzing the phone again.
                .setOnlyAlertOnce(true)
                .setContentIntent(pending);

        try {
            // POST_NOTIFICATIONS is requested at runtime in MainActivity; if the user
            // denied it, notify() is a silent no-op rather than a crash.
            NotificationManagerCompat.from(context).notify(notificationId, builder.build());
        } catch (SecurityException ignored) {
        }
    }

    /**
     * Tray id for a push. Keyed off the feed row's id so a duplicate delivery replaces its
     * own entry rather than stacking a second copy; falls back to a running counter when the
     * payload carries no {@code notification_id}.
     */
    private static int trayId(String notificationId) {
        if (notificationId == null || notificationId.isEmpty()) {
            return NOTIFICATION_ID.incrementAndGet();
        }
        return notificationId.hashCode();
    }

    /** True when this {@code notification_id} has already been shown (FCM redelivery). */
    private static boolean alreadyShown(String notificationId) {
        if (notificationId == null || notificationId.isEmpty()) return false;
        synchronized (SEEN) {
            if (!SEEN.add(notificationId)) return true;
            if (SEEN.size() > SEEN_LIMIT) {
                java.util.Iterator<String> it = SEEN.iterator();
                it.next();
                it.remove();
            }
            return false;
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
