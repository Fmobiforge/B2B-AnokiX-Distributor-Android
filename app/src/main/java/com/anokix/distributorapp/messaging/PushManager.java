package com.anokix.distributorapp.messaging;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.anokix.distributorapp.R;
import com.anokix.distributorapp.network.ApiCallback;
import com.anokix.distributorapp.network.ApiClient;
import com.anokix.distributorapp.session.SessionManager;
import com.anokix.distributorapp.ui.AirtimeActivity;
import com.anokix.distributorapp.ui.DeliveriesActivity;
import com.anokix.distributorapp.ui.DeliveryDetailActivity;
import com.anokix.distributorapp.ui.FinancesActivity;
import com.anokix.distributorapp.ui.MainActivity;
import com.anokix.distributorapp.ui.NotificationsActivity;
import com.anokix.distributorapp.ui.OrderDetailActivity;
import com.anokix.distributorapp.ui.ProductsActivity;
import com.anokix.distributorapp.ui.PromotionsActivity;
import com.anokix.distributorapp.ui.ReportsActivity;
import com.anokix.distributorapp.ui.ReturnDetailActivity;
import com.anokix.distributorapp.ui.ReturnsActivity;
import com.anokix.distributorapp.ui.SettingsSecurityActivity;
import com.anokix.distributorapp.ui.TradersActivity;
import com.anokix.distributorapp.ui.ViewPromotionActivity;
import com.anokix.distributorapp.ui.WalletActivity;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Central helper for Firebase Cloud Messaging:
 *  - creates the notification channel (Android 8+),
 *  - fetches the current FCM token and registers it with the backend (only when it
 *    changed, and only while signed in),
 *  - unregisters the token on logout,
 *  - resolves where a notification tap should navigate.
 *
 * <h3>Route resolution</h3>
 * A payload identifies its destination with {@code route} (explicit) and/or {@code type}
 * (semantic, e.g. {@code "order.status"}). {@link #routeKey} prefers an explicit, known
 * {@code route} and otherwise derives one from the {@code type} prefix, so the backend can
 * send either. Kept deliberately in step with the Trader app's copy of this class — see
 * {@code FCM-settings/3-Event-Catalogue.md} for the shared contract.
 *
 * The push payload (a data message handled in {@link AxFirebaseMessagingService}, or a
 * notification message opened by the system) is forwarded as intent extras keyed by
 * {@link #EXTRA_TYPE} / {@link #EXTRA_ROUTE} and the {@link Ids} fields.
 *
 * <h3>Two ways a payload reaches us</h3>
 * The server sends a combined {@code notification} + {@code data} message, so the delivery
 * path depends on the app's state:
 * <ol>
 *   <li><b>Foreground</b> — {@link AxFirebaseMessagingService#onMessageReceived} fires and we
 *       draw the notification ourselves, with the payload copied into {@code ax_*} extras by
 *       {@link #routingIntent}.</li>
 *   <li><b>Background / killed</b> — {@code onMessageReceived} is never called. The system
 *       tray draws the notification, and tapping it launches {@code SplashActivity} with the
 *       {@code data} entries as <em>raw</em> extras ({@code type}, {@code order_id}, …).
 *       {@link #dataFromIntent} reads those back, so both paths converge on the same
 *       {@link #navigate} call.</li>
 * </ol>
 */
public final class PushManager {

    private static final String TAG = "AxPush";

    /** Extras carried from a push into the opened activity (mirrors notification.data). */
    public static final String EXTRA_TYPE = "ax_type";
    public static final String EXTRA_ROUTE = "ax_route";
    public static final String EXTRA_ORDER_ID = "ax_order_id";
    public static final String EXTRA_GRN_ID = "ax_grn_id";
    public static final String EXTRA_GRV_ID = "ax_grv_id";
    public static final String EXTRA_DELIVERY_ID = "ax_delivery_id";
    public static final String EXTRA_INVOICE_ID = "ax_invoice_id";
    public static final String EXTRA_PRODUCT_ID = "ax_product_id";
    public static final String EXTRA_PROMOTION_ID = "ax_promotion_id";
    public static final String EXTRA_TRADER_ID = "ax_trader_id";
    public static final String EXTRA_TRANSACTION_ID = "ax_transaction_id";
    public static final String EXTRA_NOTIFICATION_ID = "ax_notification_id";

    /**
     * The {@code data} keys the server sends, in their raw form. These are the extra names
     * the FCM SDK puts on the launcher intent when the user taps a tray notification that
     * the system (not this app) drew — see {@link #dataFromIntent}.
     */
    private static final String[] DATA_KEYS = {
            "type", "route", "order_id", "order_number", "grn_id", "grn_number",
            "grv_id", "grv_number", "delivery_id", "invoice_id", "invoice_number",
            "product_id", "promotion_id", "trader_id", "transaction_id", "notification_id",
            "status", "status_key", "decision", "on_hand", "deep_link", "sent_at",
            "title", "body",
    };

    // ---- Canonical destinations ------------------------------------------

    public static final String ROUTE_ORDER = "order";
    public static final String ROUTE_DELIVERY = "delivery";
    public static final String ROUTE_GRV = "grv";
    public static final String ROUTE_RETURN = "return";
    public static final String ROUTE_INVOICE = "invoice";
    public static final String ROUTE_FINANCE = "finance";
    public static final String ROUTE_WALLET = "wallet";
    public static final String ROUTE_INVENTORY = "inventory";
    public static final String ROUTE_VAS = "vas";
    public static final String ROUTE_PROMOTION = "promotion";
    public static final String ROUTE_PRODUCT = "product";
    public static final String ROUTE_MARKETPLACE = "marketplace";
    public static final String ROUTE_REPORT = "report";
    public static final String ROUTE_TRADER = "trader";
    public static final String ROUTE_SETTINGS = "settings";
    public static final String ROUTE_NOTIFICATIONS = "notifications";

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

    // ---- Payload ids -----------------------------------------------------

    /**
     * The id fields a notification payload can carry. Every value is kept as a string:
     * FCM {@code data} values are always strings, and the in-app feed's numeric ids are
     * stringified on the way in.
     */
    public static final class Ids {
        public String orderId = "";
        public String grnId = "";
        public String grvId = "";
        public String deliveryId = "";
        public String invoiceId = "";
        public String productId = "";
        public String promotionId = "";
        public String traderId = "";
        public String transactionId = "";
        public String notificationId = "";

        /** Reads the ids out of an FCM {@code data} map. */
        public static Ids fromData(@Nullable Map<String, String> data) {
            Ids ids = new Ids();
            if (data == null) return ids;
            ids.orderId = str(data.get("order_id"));
            ids.grnId = str(data.get("grn_id"));
            ids.grvId = str(data.get("grv_id"));
            ids.deliveryId = str(data.get("delivery_id"));
            ids.invoiceId = str(data.get("invoice_id"));
            ids.productId = str(data.get("product_id"));
            ids.promotionId = str(data.get("promotion_id"));
            ids.traderId = str(data.get("trader_id"));
            ids.transactionId = str(data.get("transaction_id"));
            ids.notificationId = str(data.get("notification_id"));
            return ids;
        }

        /** Reads the ids back off an intent built by {@link #routingIntent}. */
        public static Ids fromIntent(@Nullable Intent intent) {
            Ids ids = new Ids();
            if (intent == null) return ids;
            ids.orderId = str(intent.getStringExtra(EXTRA_ORDER_ID));
            ids.grnId = str(intent.getStringExtra(EXTRA_GRN_ID));
            ids.grvId = str(intent.getStringExtra(EXTRA_GRV_ID));
            ids.deliveryId = str(intent.getStringExtra(EXTRA_DELIVERY_ID));
            ids.invoiceId = str(intent.getStringExtra(EXTRA_INVOICE_ID));
            ids.productId = str(intent.getStringExtra(EXTRA_PRODUCT_ID));
            ids.promotionId = str(intent.getStringExtra(EXTRA_PROMOTION_ID));
            ids.traderId = str(intent.getStringExtra(EXTRA_TRADER_ID));
            ids.transactionId = str(intent.getStringExtra(EXTRA_TRANSACTION_ID));
            ids.notificationId = str(intent.getStringExtra(EXTRA_NOTIFICATION_ID));
            return ids;
        }

        void putInto(Intent intent) {
            intent.putExtra(EXTRA_ORDER_ID, orderId);
            intent.putExtra(EXTRA_GRN_ID, grnId);
            intent.putExtra(EXTRA_GRV_ID, grvId);
            intent.putExtra(EXTRA_DELIVERY_ID, deliveryId);
            intent.putExtra(EXTRA_INVOICE_ID, invoiceId);
            intent.putExtra(EXTRA_PRODUCT_ID, productId);
            intent.putExtra(EXTRA_PROMOTION_ID, promotionId);
            intent.putExtra(EXTRA_TRADER_ID, traderId);
            intent.putExtra(EXTRA_TRANSACTION_ID, transactionId);
            intent.putExtra(EXTRA_NOTIFICATION_ID, notificationId);
        }

        private static String str(String value) {
            return value == null ? "" : value;
        }
    }

    /** True when an id is missing or the backend's "no id" placeholder. */
    public static boolean isBlank(String id) {
        return TextUtils.isEmpty(id) || "0".equals(id);
    }

    // ---- Launch-intent bridge --------------------------------------------

    /**
     * Reads a push payload back off a launcher intent.
     *
     * <p>When the app is backgrounded or killed the Firebase SDK draws the tray
     * notification itself; {@code onMessageReceived} never runs, and the tap starts the
     * launcher activity with the {@code data} entries as plain string extras under their
     * original names. Reading them here is what makes background taps navigate — without
     * it the app opens on the dashboard and the destination is lost.
     *
     * @return the payload as a {@code data}-shaped map, empty when the intent carries none.
     */
    @NonNull
    public static Map<String, String> dataFromIntent(@Nullable Intent intent) {
        Map<String, String> data = new HashMap<>();
        if (intent == null || intent.getExtras() == null) return data;
        for (String key : DATA_KEYS) {
            Object value = intent.getExtras().get(key);
            if (value instanceof String && !((String) value).isEmpty()) {
                data.put(key, (String) value);
            }
        }
        return data;
    }

    /** True when {@code intent} carries a push payload worth routing on. */
    public static boolean hasPushPayload(@Nullable Intent intent) {
        Map<String, String> data = dataFromIntent(intent);
        return !TextUtils.isEmpty(data.get("type")) || !TextUtils.isEmpty(data.get("route"));
    }

    // ---- Route resolution ------------------------------------------------

    /**
     * Resolves the canonical destination for a payload. An explicit, recognised
     * {@code route} wins; otherwise the {@code type} prefix decides. Unknown payloads
     * fall back to {@link #ROUTE_NOTIFICATIONS}, which opens the feed rather than
     * doing nothing.
     */
    @NonNull
    public static String routeKey(@Nullable String type, @Nullable String route) {
        return routeKey(type, route, new Ids());
    }

    /**
     * As {@link #routeKey(String, String)}, but lets the ids break a tie when {@code route}
     * and {@code type} disagree.
     *
     * <p>The server groups events into broad screen families, so a payload can name a route
     * it cannot actually be placed on: {@code grv.confirmed} arrives as {@code route=delivery}
     * carrying only a {@code grv_id}, and the delivery screens are keyed by order. When the
     * named route needs an id the payload does not carry and the {@code type} resolves to
     * somewhere we <em>can</em> open, the type wins. Both agreeing, or neither being
     * openable, leaves the explicit route in charge.
     */
    @NonNull
    public static String routeKey(@Nullable String type, @Nullable String route, @NonNull Ids ids) {
        String explicit = recognisedRoute(route);
        String derived = routeFromType(type);
        if (explicit.isEmpty()) {
            return derived.isEmpty() ? ROUTE_NOTIFICATIONS : derived;
        }
        if (!derived.isEmpty() && !explicit.equals(derived)
                && !canOpen(explicit, ids) && canOpen(derived, ids)) {
            return derived;
        }
        return explicit;
    }

    /** The route as sent, when it names a destination we know; otherwise "". */
    @NonNull
    private static String recognisedRoute(@Nullable String route) {
        // Locale.US, not the default locale: in Turkish "INVOICE".toLowerCase() is
        // "ınvoıce" (dotless i), which would never match the route constants.
        String r = route == null ? "" : route.trim().toLowerCase(Locale.US);
        switch (r) {
            case ROUTE_ORDER: case ROUTE_DELIVERY: case ROUTE_GRV: case ROUTE_RETURN:
            case ROUTE_INVOICE: case ROUTE_FINANCE: case ROUTE_WALLET: case ROUTE_INVENTORY:
            case ROUTE_VAS: case ROUTE_PROMOTION: case ROUTE_PRODUCT: case ROUTE_MARKETPLACE:
            case ROUTE_REPORT: case ROUTE_TRADER: case ROUTE_SETTINGS: case ROUTE_NOTIFICATIONS:
                return r;
            default:
                return "";
        }
    }

    /**
     * True when this payload can actually land somewhere. Only the destinations that
     * dead-end at the notifications feed without an id are "can't open" — the rest fall
     * back to their own list screen, which is a real answer and must not be overridden.
     */
    private static boolean canOpen(@NonNull String routeKey, @NonNull Ids ids) {
        switch (routeKey) {
            case ROUTE_ORDER:
            case ROUTE_GRV:
                // Goods Received is a trader screen; on this side both open the order.
                return !isBlank(ids.orderId);
            default:
                return true;
        }
    }

    /** The destination implied by the event type, e.g. "order.status" → "order". */
    @NonNull
    private static String routeFromType(@Nullable String type) {
        String t = type == null ? "" : type.trim().toLowerCase(Locale.US);
        if (t.startsWith("order")) return ROUTE_ORDER;
        if (t.startsWith("delivery")) return ROUTE_DELIVERY;
        if (t.startsWith("grv")) return ROUTE_GRV;
        if (t.startsWith("return")) return ROUTE_RETURN;
        if (t.startsWith("invoice")) return ROUTE_INVOICE;
        if (t.startsWith("payment") || t.startsWith("payout") || t.startsWith("finance")) return ROUTE_FINANCE;
        if (t.startsWith("wallet")) return ROUTE_WALLET;
        if (t.startsWith("stock")) return ROUTE_INVENTORY;
        if (t.startsWith("vas")) return ROUTE_VAS;
        if (t.startsWith("promotion")) return ROUTE_PROMOTION;
        if (t.startsWith("product")) return ROUTE_PRODUCT;
        if (t.startsWith("report") || t.startsWith("pos")) return ROUTE_REPORT;
        if (t.startsWith("trader")) return ROUTE_TRADER;
        if (t.startsWith("security")) return ROUTE_SETTINGS;
        return "";   // says nothing, so it never overrides an explicit route
    }

    // ---- Tap routing -----------------------------------------------------

    /**
     * Builds the intent that opens the screen a notification points at. Always lands
     * inside {@link MainActivity} (so the back stack stays sane), then chains the
     * detail screen on top when the payload identifies one.
     */
    @NonNull
    public static Intent routingIntent(Context context, @Nullable Map<String, String> data) {
        String type = data == null ? "" : data.get("type");
        String route = data == null ? "" : data.get("route");
        return routingIntent(context, type, route, Ids.fromData(data));
    }

    /** @see #routingIntent(Context, Map) */
    @NonNull
    public static Intent routingIntent(Context context, @Nullable String type,
                                       @Nullable String route, @NonNull Ids ids) {
        Intent main = new Intent(context, MainActivity.class);
        main.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        main.putExtra(EXTRA_TYPE, type == null ? "" : type);
        main.putExtra(EXTRA_ROUTE, route == null ? "" : route);
        ids.putInto(main);
        return main;
    }

    /**
     * Applies the navigation described by a push's extras, once {@link MainActivity} is
     * showing. Shares {@link #routeKey} with {@code NotificationsActivity.navigate} so a
     * push and its in-app row always land on the same screen.
     */
    public static void handleRouting(Context context, Intent intent) {
        if (intent == null) return;
        String type = intent.getStringExtra(EXTRA_TYPE);
        String route = intent.getStringExtra(EXTRA_ROUTE);
        if (!TextUtils.isEmpty(type) || !TextUtils.isEmpty(route)) {
            navigate(context, type, route, Ids.fromIntent(intent), true);
            return;
        }
        // Not one of our own intents — this is a tray tap the system routed here with the
        // payload still in its raw `data` form.
        Map<String, String> data = dataFromIntent(intent);
        type = data.get("type");
        route = data.get("route");
        if (TextUtils.isEmpty(type) && TextUtils.isEmpty(route)) return;
        navigate(context, type, route, Ids.fromData(data), true);
    }

    /**
     * Opens the screen a notification points at.
     *
     * @param fallbackToFeed when true (a push tap) an unrecognised payload opens the
     *                       Notifications list; when false (a tap inside that list) it
     *                       stays put rather than re-opening the screen you are on.
     */
    public static void navigate(Context context, @Nullable String type, @Nullable String route,
                                @NonNull Ids ids, boolean fallbackToFeed) {
        switch (routeKey(type, route, ids)) {
            case ROUTE_ORDER:
            case ROUTE_GRV:
                // Goods Received lives only on the trader side; a GRV event reaching a
                // distributor is about one of its orders, so show that order.
                if (!isBlank(ids.orderId)) {
                    context.startActivity(new Intent(context, OrderDetailActivity.class)
                            .putExtra(OrderDetailActivity.EXTRA_ID, ids.orderId));
                } else if (fallbackToFeed) {
                    open(context, NotificationsActivity.class);
                }
                break;
            case ROUTE_DELIVERY: {
                // Deliveries are keyed by their order id on this endpoint.
                String id = !isBlank(ids.deliveryId) ? ids.deliveryId : ids.orderId;
                if (!isBlank(id)) {
                    context.startActivity(new Intent(context, DeliveryDetailActivity.class)
                            .putExtra(DeliveryDetailActivity.EXTRA_ID, id));
                } else {
                    open(context, DeliveriesActivity.class);
                }
                break;
            }
            case ROUTE_RETURN:
                if (!isBlank(ids.grnId)) {
                    context.startActivity(new Intent(context, ReturnDetailActivity.class)
                            .putExtra(ReturnDetailActivity.EXTRA_ID, ids.grnId));
                } else {
                    open(context, ReturnsActivity.class);
                }
                break;
            case ROUTE_INVOICE:
            case ROUTE_FINANCE:
                // The distributor has no standalone invoices screen — its ledger covers both.
                open(context, FinancesActivity.class);
                break;
            case ROUTE_WALLET:
                open(context, WalletActivity.class);
                break;
            case ROUTE_INVENTORY:
            case ROUTE_PRODUCT:
            case ROUTE_MARKETPLACE:
                open(context, ProductsActivity.class);
                break;
            case ROUTE_VAS:
                open(context, AirtimeActivity.class);
                break;
            case ROUTE_PROMOTION:
                if (!isBlank(ids.promotionId)) {
                    context.startActivity(new Intent(context, ViewPromotionActivity.class)
                            .putExtra(ViewPromotionActivity.EXTRA_PROMOTION_ID, ids.promotionId));
                } else {
                    open(context, PromotionsActivity.class);
                }
                break;
            case ROUTE_REPORT:
                open(context, ReportsActivity.class);
                break;
            case ROUTE_TRADER:
                open(context, TradersActivity.class);
                break;
            case ROUTE_SETTINGS:
                open(context, SettingsSecurityActivity.class);
                break;
            default:
                if (fallbackToFeed) open(context, NotificationsActivity.class);
                break;
        }
    }

    /** Clears the routing extras so the same tap isn't re-applied on rotation/onNewIntent. */
    public static void clearRouting(Intent intent) {
        if (intent == null) return;
        intent.removeExtra(EXTRA_TYPE);
        intent.removeExtra(EXTRA_ROUTE);
        intent.removeExtra(EXTRA_ORDER_ID);
        intent.removeExtra(EXTRA_GRN_ID);
        intent.removeExtra(EXTRA_GRV_ID);
        intent.removeExtra(EXTRA_DELIVERY_ID);
        intent.removeExtra(EXTRA_INVOICE_ID);
        intent.removeExtra(EXTRA_PRODUCT_ID);
        intent.removeExtra(EXTRA_PROMOTION_ID);
        intent.removeExtra(EXTRA_TRADER_ID);
        intent.removeExtra(EXTRA_TRANSACTION_ID);
        intent.removeExtra(EXTRA_NOTIFICATION_ID);
        for (String key : DATA_KEYS) {
            intent.removeExtra(key);
        }
    }

    private static void open(Context context, Class<?> activity) {
        context.startActivity(new Intent(context, activity));
    }
}
