package com.anokix.distributor.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.anokix.distributor.network.dto.AutocompleteData;
import com.anokix.distributor.network.dto.BaseInfoData;
import com.anokix.distributor.network.dto.BusinessProfileData;
import com.anokix.distributor.network.dto.CoordinateData;
import com.anokix.distributor.network.dto.CreateTraderData;
import com.anokix.distributor.network.dto.DashboardData;
import com.anokix.distributor.network.dto.DeliveriesData;
import com.anokix.distributor.network.dto.DeliveryDetailData;
import com.anokix.distributor.network.dto.NotificationsData;
import com.anokix.distributor.network.dto.PreferencesData;
import com.anokix.distributor.network.dto.ProfileUpdateData;
import com.anokix.distributor.network.dto.ReturnDetailData;
import com.anokix.distributor.network.dto.ReturnsData;
import com.anokix.distributor.network.dto.UnreadCountData;
import com.anokix.distributor.network.dto.InventorySummaryData;
import com.anokix.distributor.network.dto.LoginData;
import com.anokix.distributor.network.dto.OrderDetailData;
import com.anokix.distributor.network.dto.OrdersData;
import com.anokix.distributor.network.dto.ProductDetailData;
import com.anokix.distributor.network.dto.ProductsData;
import com.anokix.distributor.network.dto.PromotionDetailData;
import com.anokix.distributor.network.dto.PromotionsData;
import com.anokix.distributor.network.dto.ReferenceData;
import com.anokix.distributor.network.dto.RegisterDistributorData;
import com.anokix.distributor.network.dto.StoreDetailData;
import com.anokix.distributor.network.dto.StoresData;
import com.anokix.distributor.network.dto.TradersData;
import com.anokix.distributor.session.SessionManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Typed facade over {@link Http}. Each method runs the call on a background thread and
 * delivers the parsed {@code data} block (or an error message) back on the main thread.
 *
 * Flow per call:
 *   1. build path + params
 *   2. {@link Http} performs the request (logged under "AxApi")
 *   3. Gson parses the {@link ApiResponse} envelope
 *   4. callback fires on the UI thread
 */
public final class ApiClient {

    public static final String BASE_URL = "http://155.117.20.51/ax/";

    private static ApiClient instance;

    private final ExecutorService io = Executors.newCachedThreadPool();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Gson gson = new Gson();
    private final SessionManager session;

    private ApiClient(Context context) {
        this.session = SessionManager.get(context.getApplicationContext());
    }

    public static synchronized ApiClient get(Context context) {
        if (instance == null) {
            instance = new ApiClient(context);
        }
        return instance;
    }

    // ---- Endpoints -------------------------------------------------------

    public void login(String email, String password, ApiCallback<LoginData> cb) {
        Map<String, String> form = new HashMap<>();
        form.put("email", email);
        form.put("password", password);
        form.put("portal_type", "distributor");
        io.execute(() -> {
            // login itself needs no token
            Http.Result r = Http.postForm(BASE_URL, "api/common/login", form, null);
            deliver(r, LoginData.class, cb);
        });
    }

    /**
     * Request a password reset email. Public endpoint (no token). The backend always
     * returns a generic success message and never reveals whether the email exists, so
     * only the status/message matter here.
     */
    public void forgotPassword(String email, ApiCallback<Void> cb) {
        Map<String, String> form = new HashMap<>();
        form.put("email", email);
        form.put("portal_type", "distributor");
        io.execute(() -> {
            Http.Result r = Http.postForm(BASE_URL, "api/common/forgot-password", form, null);
            deliverStatusOnly(r, cb);
        });
    }

    // ---- Push / device tokens (FCM) -------------------------------------

    /**
     * Register this device's FCM token so the backend can target it with pushes
     * (api/common/device-tokens). Best-effort: only status/message matter, and a
     * failure (e.g. the endpoint not yet live) is surfaced via {@code onError}
     * but never crashes the app.
     */
    public void registerDeviceToken(String token, ApiCallback<Void> cb) {
        // Field is `device_token` (not `token` — that key is reserved by the API gateway).
        Map<String, String> form = new HashMap<>();
        form.put("device_token", token == null ? "" : token);
        form.put("platform", "android");
        io.execute(() -> deliverStatusOnly(
                Http.postForm(BASE_URL, "api/common/device-tokens", form, session.getToken()), cb));
    }

    /** Remove this device's FCM token on logout (api/common/device-tokens/delete). */
    public void unregisterDeviceToken(String token, ApiCallback<Void> cb) {
        Map<String, String> form = new HashMap<>();
        form.put("device_token", token == null ? "" : token);
        io.execute(() -> deliverStatusOnly(
                Http.postForm(BASE_URL, "api/common/device-tokens/delete", form, session.getToken()), cb));
    }

    public void getDashboard(ApiCallback<DashboardData> cb) {
        getAuthed("api/distributor/dashboard", null, DashboardData.class, cb);
    }

    public void getStores(String keyword, ApiCallback<StoresData> cb) {
        Map<String, String> q = new HashMap<>();
        q.put("offset", "0");
        q.put("page_size", "50");
        q.put("keyword", keyword == null ? "" : keyword);
        getAuthed("api/distributor/stores/all", q, StoresData.class, cb);
    }

    public void getTraders(String search, ApiCallback<TradersData> cb) {
        getTraders(search, "", "", cb);
    }

    public void getTraders(String search, String status, String registrationStatus,
                           ApiCallback<TradersData> cb) {
        Map<String, String> q = new HashMap<>();
        q.put("search", search == null ? "" : search);
        q.put("status", status == null ? "" : status);
        q.put("registration_status", registrationStatus == null ? "" : registrationStatus);
        q.put("credit_status", "");
        getAuthed("api/distributor/traders", q, TradersData.class, cb);
    }

    public void getProducts(ApiCallback<ProductsData> cb) {
        getAuthed("api/distributor/products", null, ProductsData.class, cb);
    }

    /** Promotions/campaigns list + KPI summary. Pass "" for all campaign types. */
    public void getPromotions(String campaignType, ApiCallback<PromotionsData> cb) {
        Map<String, String> q = new HashMap<>();
        q.put("campaign_type", campaignType == null ? "" : campaignType);
        getAuthed("api/distributor/promotions", q, PromotionsData.class, cb);
    }

    /** Create a campaign (multipart: text fields + optional banner image / video). */
    public void createPromotion(Map<String, String> form, List<Http.FilePart> files,
                                ApiCallback<PromotionDetailData> cb) {
        io.execute(() -> {
            Http.Result r = Http.postMultipart(BASE_URL, "api/distributor/promotions/create",
                    form, files, session.getToken());
            deliver(r, PromotionDetailData.class, cb);
        });
    }

    /** Full campaign detail (+ selected_products, media, analytics) for the edit/view flow. */
    public void getPromotionById(String id, ApiCallback<PromotionDetailData> cb) {
        Map<String, String> q = new HashMap<>();
        q.put("id", id == null ? "" : id);
        getAuthed("api/distributor/promotions/get-by-id", q, PromotionDetailData.class, cb);
    }

    /** Update an existing campaign (partial — only the sent fields change). */
    public void updatePromotion(Map<String, String> form, List<Http.FilePart> files,
                                ApiCallback<PromotionDetailData> cb) {
        io.execute(() -> {
            Http.Result r = Http.postMultipart(BASE_URL, "api/distributor/promotions/update",
                    form, files, session.getToken());
            deliver(r, PromotionDetailData.class, cb);
        });
    }

    /** Upload / replace the banner and/or video for an existing campaign. */
    public void uploadPromotionMedia(String id, List<Http.FilePart> files,
                                     ApiCallback<PromotionDetailData> cb) {
        Map<String, String> form = new HashMap<>();
        form.put("id", id == null ? "" : id);
        io.execute(() -> {
            Http.Result r = Http.postMultipart(BASE_URL, "api/distributor/promotions/upload-media",
                    form, files, session.getToken());
            deliver(r, PromotionDetailData.class, cb);
        });
    }

    public void deletePromotion(String id, ApiCallback<PromotionDetailData> cb) {
        Map<String, String> form = new HashMap<>();
        form.put("id", id == null ? "" : id);
        io.execute(() -> {
            Http.Result r = Http.postForm(BASE_URL, "api/distributor/promotions/delete",
                    form, session.getToken());
            // The delete endpoint returns "data": [] (an empty list); only status/message matter.
            deliverStatusOnly(r, cb);
        });
    }

    public void getProductById(String id, ApiCallback<ProductDetailData> cb) {
        Map<String, String> q = new HashMap<>();
        q.put("id", id == null ? "" : id);
        getAuthed("api/distributor/products/get-by-id", q, ProductDetailData.class, cb);
    }

    /** Create a product (multipart: text fields + primary image, additional images, datasheet). */
    public void createProduct(Map<String, String> form, List<Http.FilePart> files,
                              ApiCallback<ProductDetailData> cb) {
        io.execute(() -> {
            Http.Result r = Http.postMultipart(BASE_URL, "api/distributor/products/create",
                    form, files, session.getToken());
            deliver(r, ProductDetailData.class, cb);
        });
    }

    /** Update an existing product (partial — only the sent fields change). */
    public void updateProduct(Map<String, String> form, List<Http.FilePart> files,
                              ApiCallback<ProductDetailData> cb) {
        io.execute(() -> {
            Http.Result r = Http.postMultipart(BASE_URL, "api/distributor/products/update",
                    form, files, session.getToken());
            deliver(r, ProductDetailData.class, cb);
        });
    }

    public void deleteProduct(String id, ApiCallback<ProductDetailData> cb) {
        Map<String, String> form = new HashMap<>();
        form.put("id", id == null ? "" : id);
        io.execute(() -> {
            Http.Result r = Http.postForm(BASE_URL, "api/distributor/products/delete",
                    form, session.getToken());
            // The delete endpoint returns "data": [] (an empty list), so it must not be
            // parsed into a ProductDetailData object — only the status/message matter.
            deliverStatusOnly(r, cb);
        });
    }

    public void getInventorySummary(ApiCallback<InventorySummaryData> cb) {
        getAuthed("api/distributor/inventory/summary", null, InventorySummaryData.class, cb);
    }

    // ---- Orders ----------------------------------------------------------

    /**
     * Orders list for a status tab. Empty/null filters are omitted; {@code status}
     * "all" is also omitted so the backend returns every status.
     */
    public void getOrders(String traderId, String search, String dateFrom, String dateTo,
                          String status, int page, int perPage, ApiCallback<OrdersData> cb) {
        Map<String, String> q = new HashMap<>();
        if (status != null && !status.isEmpty() && !"all".equals(status)) q.put("status", status);
        if (traderId != null && !traderId.isEmpty()) q.put("trader_id", traderId);
        if (search != null && !search.trim().isEmpty()) q.put("search", search.trim());
        if (dateFrom != null && !dateFrom.isEmpty()) q.put("date_from", dateFrom);
        if (dateTo != null && !dateTo.isEmpty()) q.put("date_to", dateTo);
        q.put("page", String.valueOf(page));
        q.put("per_page", String.valueOf(perPage));
        getAuthed("api/distributor/orders", q, OrdersData.class, cb);
    }

    /** Full order detail (+ items) for the tracking screen. */
    public void getOrderById(String id, ApiCallback<OrderDetailData> cb) {
        Map<String, String> q = new HashMap<>();
        q.put("id", id == null ? "" : id);
        getAuthed("api/distributor/orders/get-by-id", q, OrderDetailData.class, cb);
    }

    /**
     * Advance an order through the fulfilment flow. {@code action} is one of
     * accept / reject / pick / pack / ready / dispatch / deliver. Only the
     * envelope status/message matter, so the (variable) data block is ignored.
     */
    public void orderAction(String action, long orderId, ApiCallback<Void> cb) {
        Map<String, String> form = new HashMap<>();
        form.put("order_id", String.valueOf(orderId));
        io.execute(() -> {
            Http.Result r = Http.postForm(BASE_URL, "api/distributor/orders/" + action,
                    form, session.getToken());
            deliverStatusOnly(r, cb);
        });
    }

    // ---- Deliveries ------------------------------------------------------

    /**
     * Deliveries list + KPI summary + status counts for a date range. Empty filters are
     * omitted. Search/tab/area filtering is then applied locally over the returned set.
     */
    public void getDeliveries(String dateFrom, String dateTo, String search,
                              int page, int perPage, ApiCallback<DeliveriesData> cb) {
        Map<String, String> q = new HashMap<>();
        if (dateFrom != null && !dateFrom.isEmpty()) q.put("date_from", dateFrom);
        if (dateTo != null && !dateTo.isEmpty()) q.put("date_to", dateTo);
        if (search != null && !search.trim().isEmpty()) q.put("search", search.trim());
        q.put("page", String.valueOf(page));
        q.put("per_page", String.valueOf(perPage));
        getAuthed("api/distributor/deliveries", q, DeliveriesData.class, cb);
    }

    /** Full delivery detail (+ items) for the detail screen. */
    public void getDeliveryById(String id, ApiCallback<DeliveryDetailData> cb) {
        Map<String, String> q = new HashMap<>();
        q.put("id", id == null ? "" : id);
        getAuthed("api/distributor/deliveries/get-by-id", q, DeliveryDetailData.class, cb);
    }

    /**
     * Advance a delivery: {@code action} is {@code dispatch} (ReadyForDelivery →
     * OutForDelivery) or {@code deliver} (OutForDelivery → Delivered). The endpoint
     * returns the updated delivery (same shape as get-by-id).
     */
    public void deliveryAction(String action, long deliveryId, ApiCallback<DeliveryDetailData> cb) {
        Map<String, String> form = new HashMap<>();
        form.put("order_id", String.valueOf(deliveryId));
        io.execute(() -> {
            Http.Result r = Http.postForm(BASE_URL, "api/distributor/deliveries/" + action,
                    form, session.getToken());
            deliver(r, DeliveryDetailData.class, cb);
        });
    }

    // ---- Returns (GRNs) --------------------------------------------------

    /**
     * Returns list + KPI summary + status counts for a date range. Empty filters are
     * omitted. Search / status tab / reason filtering is then applied locally over the
     * returned set.
     */
    public void getReturns(String dateFrom, String dateTo, String search,
                           int page, int perPage, ApiCallback<ReturnsData> cb) {
        Map<String, String> q = new HashMap<>();
        if (dateFrom != null && !dateFrom.isEmpty()) q.put("date_from", dateFrom);
        if (dateTo != null && !dateTo.isEmpty()) q.put("date_to", dateTo);
        if (search != null && !search.trim().isEmpty()) q.put("search", search.trim());
        q.put("page", String.valueOf(page));
        q.put("per_page", String.valueOf(perPage));
        getAuthed("api/distributor/grns", q, ReturnsData.class, cb);
    }

    /** Full return detail (+ items) for the detail screen. */
    public void getReturnById(String id, ApiCallback<ReturnDetailData> cb) {
        Map<String, String> q = new HashMap<>();
        q.put("id", id == null ? "" : id);
        getAuthed("api/distributor/grns/get-by-id", q, ReturnDetailData.class, cb);
    }

    /**
     * Decide a return: {@code action} is {@code approve} or {@code reject}; {@code note} is
     * the optional decision note. The endpoint returns the updated return (same shape as
     * get-by-id).
     */
    public void returnAction(String action, long grnId, String note,
                             ApiCallback<ReturnDetailData> cb) {
        Map<String, String> form = new HashMap<>();
        form.put("grn_id", String.valueOf(grnId));
        if (note != null) form.put("note", note);
        io.execute(() -> {
            Http.Result r = Http.postForm(BASE_URL, "api/distributor/grns/" + action,
                    form, session.getToken());
            deliver(r, ReturnDetailData.class, cb);
        });
    }

    // ---- Notifications (common) ------------------------------------------

    /** Live unread-notification count (api/common/notifications/unread-count). */
    public void getUnreadCount(ApiCallback<UnreadCountData> cb) {
        getAuthed("api/common/notifications/unread-count", null, UnreadCountData.class, cb);
    }

    /**
     * Notification feed (api/common/notifications). {@code filter} is "all" (omitted)
     * or "unread"; paged by {@code page}/{@code perPage}.
     */
    public void getNotifications(String filter, int page, int perPage,
                                ApiCallback<NotificationsData> cb) {
        Map<String, String> q = new HashMap<>();
        if (filter != null && !filter.isEmpty() && !"all".equals(filter)) q.put("filter", filter);
        q.put("page", String.valueOf(page));
        q.put("per_page", String.valueOf(perPage));
        getAuthed("api/common/notifications", q, NotificationsData.class, cb);
    }

    /** Mark one notification read (api/common/notifications/read, id=). */
    public void markNotificationRead(String id, ApiCallback<Void> cb) {
        Map<String, String> form = new HashMap<>();
        form.put("id", id == null ? "" : id);
        io.execute(() -> {
            Http.Result r = Http.postForm(BASE_URL, "api/common/notifications/read", form, session.getToken());
            deliverStatusOnly(r, cb);
        });
    }

    /** Mark every notification read (api/common/notifications/read, all=true). */
    public void markAllNotificationsRead(ApiCallback<Void> cb) {
        Map<String, String> form = new HashMap<>();
        form.put("all", "true");
        io.execute(() -> {
            Http.Result r = Http.postForm(BASE_URL, "api/common/notifications/read", form, session.getToken());
            deliverStatusOnly(r, cb);
        });
    }

    // ---- Settings: profile / preferences ---------------------------------

    /** Portal preferences + notification toggles (api/common/preferences). */
    public void getPreferences(ApiCallback<PreferencesData> cb) {
        getAuthed("api/common/preferences", null, PreferencesData.class, cb);
    }

    /** Update the personal profile (api/common/profile/update). */
    public void updateProfile(String firstName, String lastName, String email, String phone,
                              ApiCallback<ProfileUpdateData> cb) {
        Map<String, String> form = new HashMap<>();
        form.put("first_name", firstName == null ? "" : firstName);
        form.put("last_name", lastName == null ? "" : lastName);
        form.put("email", email == null ? "" : email);
        form.put("phone_number", phone == null ? "" : phone);
        io.execute(() -> {
            Http.Result r = Http.postForm(BASE_URL, "api/common/profile/update", form, session.getToken());
            deliver(r, ProfileUpdateData.class, cb);
        });
    }

    /** Change the account password (api/common/profile/password). */
    public void changePassword(String currentPassword, String newPassword, ApiCallback<Void> cb) {
        Map<String, String> form = new HashMap<>();
        form.put("current_password", currentPassword == null ? "" : currentPassword);
        form.put("new_password", newPassword == null ? "" : newPassword);
        io.execute(() -> {
            Http.Result r = Http.postForm(BASE_URL, "api/common/profile/password", form, session.getToken());
            deliverStatusOnly(r, cb);
        });
    }

    /** Update the 5 notification toggles (api/common/preferences/update, JSON body). */
    public void updateNotificationPrefs(boolean orderAlerts, boolean lowStockAlerts,
                                        boolean deliveryUpdates, boolean financeReminders,
                                        boolean reportReady, ApiCallback<Void> cb) {
        Map<String, Object> notifications = new HashMap<>();
        notifications.put("orderAlerts", orderAlerts);
        notifications.put("lowStockAlerts", lowStockAlerts);
        notifications.put("deliveryUpdates", deliveryUpdates);
        notifications.put("financeReminders", financeReminders);
        notifications.put("reportReady", reportReady);
        Map<String, Object> body = new HashMap<>();
        body.put("notifications", notifications);
        postPreferences(body, cb);
    }

    /** Update the regional preferences (api/common/preferences/update, JSON body). */
    public void updatePreferences(String language, String currency, String timezone,
                                  String dateFormat, ApiCallback<Void> cb) {
        Map<String, Object> body = new HashMap<>();
        body.put("language", language);
        body.put("currency", currency);
        body.put("timezone", timezone);
        body.put("date_format", dateFormat);
        postPreferences(body, cb);
    }

    private void postPreferences(Map<String, Object> body, ApiCallback<Void> cb) {
        final String json = gson.toJson(body);
        io.execute(() -> {
            Http.Result r = Http.postJson(BASE_URL, "api/common/preferences/update", json, session.getToken());
            deliverStatusOnly(r, cb);
        });
    }

    public void getBaseInfo(ApiCallback<BaseInfoData> cb) {
        getAuthed("api/common/base-info", null, BaseInfoData.class, cb);
    }

    // ---- Settings: business profile (7-step) -----------------------------

    /** Full distributor business record for the Business editor (api/distributor/business/profile). */
    public void getBusinessProfile(ApiCallback<BusinessProfileData> cb) {
        getAuthed("api/distributor/business/profile", null, BusinessProfileData.class, cb);
    }

    /**
     * Save one Business step (api/distributor/business/profile/update). Multipart so steps that
     * carry files (logo, documents) share the same path; text-only steps pass an empty file list.
     */
    public void updateBusinessProfile(Map<String, String> form, List<Http.FilePart> files,
                                      ApiCallback<Void> cb) {
        io.execute(() -> {
            Http.Result r = Http.postMultipart(BASE_URL, "api/distributor/business/profile/update",
                    form, files, session.getToken());
            deliverStatusOnly(r, cb);
        });
    }

    /** Lookup data for the Create Trader wizard (categories, distributors, trader types). */
    public void getReference(ApiCallback<ReferenceData> cb) {
        getAuthed("api/common/reference", null, ReferenceData.class, cb);
    }

    /** Google Places autocomplete (proxied) for the business address search. */
    public void getAddressAutocomplete(String keyword, ApiCallback<AutocompleteData> cb) {
        Map<String, String> q = new HashMap<>();
        q.put("keyword", keyword == null ? "" : keyword);
        q.put("str_countries", "country:ZA");
        q.put("search_type", "address");
        getAuthed("api/common/google-auto-complete", q, AutocompleteData.class, cb);
    }

    /** Resolve a place_id from autocomplete into latitude/longitude + formatted address. */
    public void getCoordinateFromPlaceId(String placeId, ApiCallback<CoordinateData> cb) {
        Map<String, String> q = new HashMap<>();
        q.put("place_id", placeId == null ? "" : placeId);
        getAuthed("api/common/get-coordinate-from-placeid", q, CoordinateData.class, cb);
    }

    /**
     * Public distributor self-registration (multipart, no auth). Posts company /
     * banking / contact / coverage fields plus the KYC document files.
     */
    public void registerDistributor(Map<String, String> form, List<Http.FilePart> files,
                                    ApiCallback<RegisterDistributorData> cb) {
        io.execute(() -> {
            // Registration is a public endpoint: send no token.
            Http.Result r = Http.postMultipart(BASE_URL, "api/distributor/register",
                    form, files, null);
            deliver(r, RegisterDistributorData.class, cb);
        });
    }

    /** Create a trader under the logged-in distributor (multipart, with KYC documents). */
    public void createTrader(Map<String, String> form, List<Http.FilePart> files,
                             ApiCallback<CreateTraderData> cb) {
        io.execute(() -> {
            Http.Result r = Http.postMultipart(BASE_URL, "api/distributor/traders/create",
                    form, files, session.getToken());
            deliver(r, CreateTraderData.class, cb);
        });
    }

    public void getStore(String id, ApiCallback<StoreDetailData> cb) {
        Map<String, String> q = new HashMap<>();
        q.put("id", id);
        getAuthed("api/distributor/stores", q, StoreDetailData.class, cb);
    }

    public void createStore(Map<String, String> form, ApiCallback<StoreDetailData> cb) {
        postAuthed("api/distributor/stores/create", form, StoreDetailData.class, cb);
    }

    public void updateStore(Map<String, String> form, ApiCallback<StoreDetailData> cb) {
        postAuthed("api/distributor/stores/update", form, StoreDetailData.class, cb);
    }

    // ---- Internals -------------------------------------------------------

    private <T> void getAuthed(String path, Map<String, String> query, Class<T> type, ApiCallback<T> cb) {
        io.execute(() -> {
            Http.Result r = Http.get(BASE_URL, path, query, session.getToken());
            deliver(r, type, cb);
        });
    }

    private <T> void postAuthed(String path, Map<String, String> form, Class<T> type, ApiCallback<T> cb) {
        io.execute(() -> {
            Http.Result r = Http.postForm(BASE_URL, path, form, session.getToken());
            deliver(r, type, cb);
        });
    }

    /** Parse the envelope and post the result on the main thread. */
    private <T> void deliver(Http.Result r, Class<T> type, ApiCallback<T> cb) {
        T data = null;
        String error = null;
        try {
            if (r.body == null || r.body.isEmpty()) {
                error = "Network error (HTTP " + r.code + ")";
            } else {
                Type envelope = TypeToken.getParameterized(ApiResponse.class, type).getType();
                ApiResponse<T> resp = gson.fromJson(r.body, envelope);
                if (resp != null && resp.status) {
                    data = resp.data;
                } else {
                    error = (resp != null && resp.message != null) ? resp.message : "Request failed";
                }
            }
        } catch (Exception e) {
            Log.e(Http.TAG, "parse failed: " + e, e);
            error = "Unexpected response from server";
        }

        final T result = data;
        final String errorMessage = error;
        main.post(() -> {
            if (errorMessage == null) {
                cb.onSuccess(result);
            } else {
                cb.onError(errorMessage);
            }
        });
    }

    /**
     * Check only the {@code status}/{@code message} of an envelope, ignoring the shape
     * of {@code data}. Used by endpoints (e.g. products/delete) whose {@code data} block
     * is not a typed object — parsing it into one would throw and surface a false error.
     */
    private <T> void deliverStatusOnly(Http.Result r, ApiCallback<T> cb) {
        String error = null;
        try {
            if (r.body == null || r.body.isEmpty()) {
                error = "Network error (HTTP " + r.code + ")";
            } else {
                ApiResponse<?> resp = gson.fromJson(r.body, ApiResponse.class);
                if (resp == null || !resp.status) {
                    error = (resp != null && resp.message != null) ? resp.message : "Request failed";
                }
            }
        } catch (Exception e) {
            Log.e(Http.TAG, "parse failed: " + e, e);
            error = "Unexpected response from server";
        }

        final String errorMessage = error;
        main.post(() -> {
            if (errorMessage == null) {
                cb.onSuccess(null);
            } else {
                cb.onError(errorMessage);
            }
        });
    }
}
