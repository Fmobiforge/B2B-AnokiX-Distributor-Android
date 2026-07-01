package com.anokix.distributorapp.messaging;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.network.ApiCallback;
import com.anokix.distributorapp.network.ApiClient;
import com.anokix.distributorapp.session.SessionManager;
import com.anokix.distributorapp.ui.DeliveriesActivity;
import com.anokix.distributorapp.ui.MainActivity;
import com.anokix.distributorapp.ui.NotificationsActivity;
import com.anokix.distributorapp.ui.OrderDetailActivity;
import com.anokix.distributorapp.ui.ProductsActivity;
import com.anokix.distributorapp.ui.ReturnDetailActivity;
import com.anokix.distributorapp.ui.ReturnsActivity;
import com.google.firebase.messaging.FirebaseMessaging;

/**
 * Central helper for Firebase Cloud Messaging:
 *  - creates the notification channel (Android 8+),
 *  - fetches the current FCM token and registers it with the backend (only when it
 *    changed, and only while signed in),
 *  - unregisters the token on logout,
 *  - resolves where a notification tap should navigate.
 *
 * The push payload (a data message handled in {@link AxFirebaseMessagingService}, or a
 * notification message opened by the system) is forwarded as intent extras keyed by
 * {@link #EXTRA_TYPE} / {@link #EXTRA_ROUTE} / {@link #EXTRA_ORDER_ID} / {@link #EXTRA_GRN_ID}.
 * Routing mirrors {@code NotificationsActivity.openDetail}.
 */
public final class PushManager {

    private static final String TAG = "AxPush";

    /** Extras carried from a push into the opened activity (mirrors notification.data). */
    public static final String EXTRA_TYPE = "ax_type";
    public static final String EXTRA_ROUTE = "ax_route";
    public static final String EXTRA_ORDER_ID = "ax_order_id";
    public static final String EXTRA_GRN_ID = "ax_grn_id";

    private PushManager() {}

    // ---- Channel ---------------------------------------------------------

    /** Creates the default notification channel once (no-op below Android 8). */
    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null) return;
        String id = context.getString(R.string.fcm_default_channel_id);
        if (nm.getNotificationChannel(id) != null) return;
        NotificationChannel channel = new NotificationChannel(
                id,
                context.getString(R.string.fcm_default_channel_name),
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(context.getString(R.string.fcm_default_channel_desc));
        nm.createNotificationChannel(channel);
    }

    // ---- Token registration ---------------------------------------------

    /**
     * Fetches the current FCM token and registers it with the backend if it is new.
     * Safe to call on every app start / after login — it only POSTs when the cached
     * token differs from the one the server already has.
     */
    public static void syncToken(Context context) {
        final Context app = context.getApplicationContext();
        if (!SessionManager.get(app).isLoggedIn()) return;
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) {
                        Log.w(TAG, "Fetching FCM token failed", task.getException());
                        return;
                    }
                    registerToken(app, task.getResult());
                });
    }

    /** Registers a freshly-issued token (called from {@code onNewToken} and {@link #syncToken}). */
    public static void registerToken(Context context, String token) {
        final Context app = context.getApplicationContext();
        SessionManager session = SessionManager.get(app);
        if (!session.isLoggedIn() || TextUtils.isEmpty(token)) return;
        if (session.isFcmTokenRegistered(token)) return;   // already on the server

        session.saveFcmToken(token, false);
        ApiClient.get(app).registerDeviceToken(token, new ApiCallback<Void>() {
            @Override public void onSuccess(Void unused) {
                SessionManager.get(app).saveFcmToken(token, true);
                Log.d(TAG, "Device token registered");
            }
            @Override public void onError(String message) {
                // Endpoint may not be live yet — keep the token cached as unregistered
                // so the next syncToken() retries.
                Log.w(TAG, "Device token registration failed: " + message);
            }
        });
    }

    /** Removes this device's token from the backend (call before clearing the session). */
    public static void unregister(Context context) {
        final Context app = context.getApplicationContext();
        String token = SessionManager.get(app).getFcmToken();
        if (TextUtils.isEmpty(token)) return;
        ApiClient.get(app).unregisterDeviceToken(token, new ApiCallback<Void>() {
            @Override public void onSuccess(Void unused) {}
            @Override public void onError(String message) {}
        });
        SessionManager.get(app).saveFcmToken("", false);
    }

    // ---- Tap routing -----------------------------------------------------

    /**
     * Builds the intent that opens the screen a notification points at. Always lands
     * inside {@link MainActivity} (so the back stack stays sane), then chains the
     * detail screen on top when the payload identifies one.
     */
    @NonNull
    public static Intent routingIntent(Context context, String type, String route,
                                       String orderId, String grnId) {
        Intent main = new Intent(context, MainActivity.class);
        main.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        main.putExtra(EXTRA_TYPE, type == null ? "" : type);
        main.putExtra(EXTRA_ROUTE, route == null ? "" : route);
        main.putExtra(EXTRA_ORDER_ID, orderId == null ? "" : orderId);
        main.putExtra(EXTRA_GRN_ID, grnId == null ? "" : grnId);
        return main;
    }

    /**
     * Applies the navigation described by a push's extras, once {@link MainActivity} is
     * showing. Mirrors {@code NotificationsActivity.openDetail}: order → order detail,
     * return → GRN detail, delivery → deliveries, stock → products, else → notifications.
     */
    public static void handleRouting(Context context, Intent intent) {
        if (intent == null) return;
        String type = intent.getStringExtra(EXTRA_TYPE);
        String route = intent.getStringExtra(EXTRA_ROUTE);
        String orderId = intent.getStringExtra(EXTRA_ORDER_ID);
        String grnId = intent.getStringExtra(EXTRA_GRN_ID);
        if (TextUtils.isEmpty(type) && TextUtils.isEmpty(route)) return;
        type = type == null ? "" : type;
        route = route == null ? "" : route;

        if (type.startsWith("order") || "order".equals(route)) {
            if (!isBlank(orderId)) {
                context.startActivity(new Intent(context, OrderDetailActivity.class)
                        .putExtra(OrderDetailActivity.EXTRA_ID, orderId));
            } else {
                context.startActivity(new Intent(context, NotificationsActivity.class));
            }
        } else if (type.startsWith("return") || "return".equals(route)) {
            if (!isBlank(grnId)) {
                context.startActivity(new Intent(context, ReturnDetailActivity.class)
                        .putExtra(ReturnDetailActivity.EXTRA_ID, grnId));
            } else {
                context.startActivity(new Intent(context, ReturnsActivity.class));
            }
        } else if (type.startsWith("delivery") || "delivery".equals(route)) {
            context.startActivity(new Intent(context, DeliveriesActivity.class));
        } else if (type.startsWith("stock") || "inventory".equals(route)) {
            context.startActivity(new Intent(context, ProductsActivity.class));
        } else {
            context.startActivity(new Intent(context, NotificationsActivity.class));
        }
    }

    /** Clears the routing extras so the same tap isn't re-applied on rotation/onNewIntent. */
    public static void clearRouting(Intent intent) {
        if (intent == null) return;
        intent.removeExtra(EXTRA_TYPE);
        intent.removeExtra(EXTRA_ROUTE);
        intent.removeExtra(EXTRA_ORDER_ID);
        intent.removeExtra(EXTRA_GRN_ID);
    }

    private static boolean isBlank(String value) {
        return TextUtils.isEmpty(value) || "0".equals(value);
    }
}
