package com.anokix.distributorapp.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.anokix.distributorapp.network.dto.AutocompleteData;
import com.anokix.distributorapp.network.dto.BaseInfoData;
import com.anokix.distributorapp.network.dto.BusinessProfileData;
import com.anokix.distributorapp.network.dto.CoordinateData;
import com.anokix.distributorapp.network.dto.CreateTraderData;
import com.anokix.distributorapp.network.dto.DashboardData;
import com.anokix.distributorapp.network.dto.FinancesData;
import com.anokix.distributorapp.network.dto.DeliveriesData;
import com.anokix.distributorapp.network.dto.DeliveryDetailData;
import com.anokix.distributorapp.network.dto.NotificationsData;
import com.anokix.distributorapp.network.dto.PosPaymentsData;
import com.anokix.distributorapp.network.dto.PosTerminalsData;
import com.anokix.distributorapp.network.dto.PreferencesData;
import com.anokix.distributorapp.network.dto.ProfileUpdateData;
import com.anokix.distributorapp.network.dto.ReturnDetailData;
import com.anokix.distributorapp.network.dto.ReturnsData;
import com.anokix.distributorapp.network.dto.UnreadCountData;
import com.anokix.distributorapp.network.dto.InventoryPageData;
import com.anokix.distributorapp.network.dto.InventorySummaryData;
import com.anokix.distributorapp.network.dto.StaffData;
import com.anokix.distributorapp.network.dto.StockHistoryData;
import com.anokix.distributorapp.network.dto.LoginData;
import com.anokix.distributorapp.network.dto.OrderDetailData;
import com.anokix.distributorapp.network.dto.OrdersData;
import com.anokix.distributorapp.network.dto.ProductDetailData;
import com.anokix.distributorapp.network.dto.ProductsData;
import com.anokix.distributorapp.network.dto.PromotionDetailData;
import com.anokix.distributorapp.network.dto.PromotionsData;
import com.anokix.distributorapp.network.dto.ReferenceData;
import com.anokix.distributorapp.network.dto.RegisterDistributorData;
import com.anokix.distributorapp.network.dto.ReportsData;
import com.anokix.distributorapp.network.dto.StoreDetailData;
import com.anokix.distributorapp.network.dto.StoresData;
import com.anokix.distributorapp.network.dto.TopupCreateData;
import com.anokix.distributorapp.network.dto.TopupStatusData;
import com.anokix.distributorapp.network.dto.TraderDetailData;
import com.anokix.distributorapp.network.dto.TradersData;
import com.anokix.distributorapp.network.dto.VasCategoriesData;
import com.anokix.distributorapp.network.dto.VasCustomersData;
import com.anokix.distributorapp.network.dto.VasOnboardCustomerData;
import com.anokix.distributorapp.network.dto.VasOnboardPreviewData;
import com.anokix.distributorapp.network.dto.VasProductsData;
import com.anokix.distributorapp.network.dto.VasSubscriptionProductsData;
import com.anokix.distributorapp.network.dto.VasSubscriptionsData;
import com.anokix.distributorapp.network.dto.VasTransactionsData;
import com.anokix.distributorapp.network.dto.WalletData;
import com.anokix.distributorapp.session.SessionManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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

    /**
     * Report files are served by their own service — a different port from the JSON API,
     * with no {@code /ax/} prefix — which authenticates from the request body instead of
     * an Authorization header. It is the only endpoint that does not sit under BASE_URL.
     */
    public static final String DOWNLOAD_URL = "http://155.117.20.51:5721/api/report-download";

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

    /** The distributor's anokiX (IMB) wallet — balance shown on the dashboard. */
    public void getWallet(ApiCallback<WalletData> cb) {
        getWallet(false, cb);
    }

    /**
     * The anokiX (IMB) wallet. Pass {@code refresh=true} to bypass the short-lived
     * balance cache — used by the manual refresh button on the wallet screen.
     */
    public void getWallet(boolean refresh, ApiCallback<WalletData> cb) {
        Map<String, String> q = null;
        if (refresh) {
            q = new HashMap<>();
            q.put("refresh", "1");
        }
        getAuthed("api/common/wallet", q, WalletData.class, cb);
    }

    /**
     * Open a PayCloud Hosted Checkout to fund the wallet (api/common/topup/create).
     * Returns the {@code pay_url} the app must open plus the {@code merchant_order_no}
     * to poll afterwards. {@code returnUrl} is where PayCloud sends the user back.
     */
    public void createTopup(double amount, String returnUrl, ApiCallback<TopupCreateData> cb) {
        Map<String, String> form = new HashMap<>();
        form.put("amount", String.format(Locale.US, "%.2f", amount));
        form.put("return_url", returnUrl);
        form.put("portal", "distributor");
        postAuthed("api/common/topup/create", form, TopupCreateData.class, cb);
    }

    /**
     * Poll a top-up after the user comes back from PayCloud (api/common/topup/status).
     * The money has landed only when {@code credit_status=credited}.
     */
    public void getTopupStatus(String merchantOrderNo, ApiCallback<TopupStatusData> cb) {
        Map<String, String> q = new HashMap<>();
        q.put("merchant_order_no", merchantOrderNo == null ? "" : merchantOrderNo);
        getAuthed("api/common/topup/status", q, TopupStatusData.class, cb);
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

    public void getTraders(String search, String status, String creditStatus,
                           ApiCallback<TradersData> cb) {
        Map<String, String> q = new HashMap<>();
        q.put("search", search == null ? "" : search);
        q.put("status", status == null ? "" : status);
        q.put("credit_status", creditStatus == null ? "" : creditStatus);
        q.put("registration_status", "");
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

    /** Everything the Inventory screen needs: summary, rows, chart, movements, warehouses. */
    public void getInventoryPage(ApiCallback<InventoryPageData> cb) {
        getAuthed("api/distributor/inventory/page", null, InventoryPageData.class, cb);
    }

    /** Stock ledger for one product (Inventory → item → History). */
    public void getStockHistory(String productId, int limit, ApiCallback<StockHistoryData> cb) {
        Map<String, String> q = new HashMap<>();
        q.put("product_id", productId == null ? "" : productId);
        q.put("limit", String.valueOf(limit));
        getAuthed("api/distributor/inventory/history", q, StockHistoryData.class, cb);
    }

    /**
     * Sets the counted quantity for a product (Adjust Stock). The callback receives the
     * server's confirmation message, e.g. "Stock updated from 1000 to 950.".
     */
    public void setStock(String productId, long quantity, String notes, ApiCallback<String> cb) {
        Map<String, String> form = new HashMap<>();
        form.put("product_id", productId == null ? "" : productId);
        form.put("quantity", String.valueOf(quantity));
        if (notes != null && !notes.isEmpty()) {
            form.put("notes", notes);
        }
        io.execute(() -> deliverMessageOnly(
                Http.postForm(BASE_URL, "api/distributor/inventory/set-stock", form, session.getToken()), cb));
    }

    /** Moves units between warehouses; the total on hand does not change. */
    public void transferStock(String productId, String fromWarehouseId, String toWarehouseId,
                              long quantity, String notes, ApiCallback<String> cb) {
        Map<String, String> form = new HashMap<>();
        form.put("product_id", productId == null ? "" : productId);
        form.put("from_warehouse_id", fromWarehouseId == null ? "" : fromWarehouseId);
        form.put("to_warehouse_id", toWarehouseId == null ? "" : toWarehouseId);
        form.put("quantity", String.valueOf(quantity));
        if (notes != null && !notes.isEmpty()) {
            form.put("notes", notes);
        }
        io.execute(() -> deliverMessageOnly(
                Http.postForm(BASE_URL, "api/distributor/inventory/transfer", form, session.getToken()), cb));
    }

    /** Books new stock into a warehouse (Add Stock / Receive). */
    public void receiveStock(String warehouseId, String productId, long quantity, String unitCost,
                             String batchNumber, String expiryDate, ApiCallback<String> cb) {
        Map<String, String> form = new HashMap<>();
        form.put("warehouse_id", warehouseId == null ? "" : warehouseId);
        form.put("product_id", productId == null ? "" : productId);
        form.put("quantity", String.valueOf(quantity));
        if (unitCost != null && !unitCost.isEmpty()) {
            form.put("unit_cost", unitCost);
        }
        if (batchNumber != null && !batchNumber.isEmpty()) {
            form.put("batch_number", batchNumber);
        }
        if (expiryDate != null && !expiryDate.isEmpty()) {
            form.put("expiry_date", expiryDate);
        }
        io.execute(() -> deliverMessageOnly(
                Http.postForm(BASE_URL, "api/distributor/inventory/receive", form, session.getToken()), cb));
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

    /**
     * Finances ledger — GET {@code api/distributor/finances}. Optionally scoped to a date range
     * (the screen drives it from the range picker); returns the transaction list + KPI summary.
     */
    public void getFinances(String dateFrom, String dateTo, ApiCallback<FinancesData> cb) {
        Map<String, String> q = new HashMap<>();
        if (dateFrom != null && !dateFrom.isEmpty()) q.put("date_from", dateFrom);
        if (dateTo != null && !dateTo.isEmpty()) q.put("date_to", dateTo);
        getAuthed("api/distributor/finances", q.isEmpty() ? null : q, FinancesData.class, cb);
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

    // ---- Warehouse staff -------------------------------------------------

    /** Warehouse staff who can sign in to pick, pack and handle stock. */
    public void getStaff(ApiCallback<StaffData> cb) {
        getAuthed("api/distributor/staff", null, StaffData.class, cb);
    }

    /** Creates a staff login (api/distributor/staff). */
    public void addStaff(String firstName, String lastName, String email, String password,
                         String phone, String jobTitle, ApiCallback<String> cb) {
        Map<String, String> form = new HashMap<>();
        form.put("first_name", firstName == null ? "" : firstName);
        form.put("last_name", lastName == null ? "" : lastName);
        form.put("email", email == null ? "" : email);
        form.put("password", password == null ? "" : password);
        if (phone != null && !phone.isEmpty()) {
            form.put("phone_number", phone);
        }
        if (jobTitle != null && !jobTitle.isEmpty()) {
            form.put("job_title", jobTitle);
        }
        io.execute(() -> deliverMessageOnly(
                Http.postForm(BASE_URL, "api/distributor/staff", form, session.getToken()), cb));
    }

    /** Sets a new sign-in password for a staff member (they are not emailed). */
    public void setStaffPassword(long staffId, String password, ApiCallback<String> cb) {
        Map<String, String> form = new HashMap<>();
        form.put("staff_id", String.valueOf(staffId));
        form.put("password", password == null ? "" : password);
        io.execute(() -> deliverMessageOnly(
                Http.postForm(BASE_URL, "api/distributor/staff/password", form, session.getToken()), cb));
    }

    /** Turns a staff member's sign-in on ("active") or off ("disabled"). */
    public void updateStaffStatus(long staffId, String status, ApiCallback<String> cb) {
        Map<String, String> form = new HashMap<>();
        form.put("staff_id", String.valueOf(staffId));
        form.put("status", status == null ? "" : status);
        io.execute(() -> deliverMessageOnly(
                Http.postForm(BASE_URL, "api/distributor/staff/update", form, session.getToken()), cb));
    }

    // ---- Card machine (PayCloud POS) -------------------------------------

    /** Card machines linked to this account (api/common/pos/terminals). */
    public void getPosTerminals(ApiCallback<PosTerminalsData> cb) {
        getAuthed("api/common/pos/terminals", null, PosTerminalsData.class, cb);
    }

    /** Payments pushed to a card machine, newest first (api/common/pos/payments). */
    public void getPosPayments(int page, int perPage, ApiCallback<PosPaymentsData> cb) {
        Map<String, String> q = new HashMap<>();
        q.put("page", String.valueOf(page));
        q.put("per_page", String.valueOf(perPage));
        getAuthed("api/common/pos/payments", q, PosPaymentsData.class, cb);
    }

    /** Links a card machine by the serial printed on the device. */
    public void addPosTerminal(String terminalSn, String label, ApiCallback<String> cb) {
        Map<String, String> form = new HashMap<>();
        form.put("terminal_sn", terminalSn == null ? "" : terminalSn);
        if (label != null && !label.isEmpty()) {
            form.put("label", label);
        }
        io.execute(() -> deliverMessageOnly(
                Http.postForm(BASE_URL, "api/common/pos/terminals", form, session.getToken()), cb));
    }

    /** Unlinks a card machine (api/common/pos/terminals/remove). */
    public void removePosTerminal(long terminalId, ApiCallback<String> cb) {
        Map<String, String> form = new HashMap<>();
        form.put("terminal_id", String.valueOf(terminalId));
        io.execute(() -> deliverMessageOnly(
                Http.postForm(BASE_URL, "api/common/pos/terminals/remove", form, session.getToken()), cb));
    }

    /**
     * Pushes an amount to a card machine so the customer can tap / insert / swipe there.
     * Card details never reach the app.
     */
    public void sendPosPayment(String amount, long terminalId, String description,
                               ApiCallback<String> cb) {
        Map<String, String> form = new HashMap<>();
        form.put("amount", amount == null ? "" : amount);
        form.put("terminal_id", String.valueOf(terminalId));
        if (description != null && !description.isEmpty()) {
            form.put("description", description);
        }
        io.execute(() -> deliverMessageOnly(
                Http.postForm(BASE_URL, "api/common/pos/payments", form, session.getToken()), cb));
    }

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

    /**
     * Update the 6 notification toggles (api/common/preferences/update, JSON body). The key
     * set matches the Trader app exactly so one backend gate serves both portals — see
     * {@code FCM-settings/3-Event-Catalogue.md} §7.
     */
    public void updateNotificationPrefs(boolean orderAlerts, boolean lowStockAlerts,
                                        boolean deliveryUpdates, boolean promotionAlerts,
                                        boolean financeReminders, boolean reportReady,
                                        ApiCallback<Void> cb) {
        Map<String, Object> notifications = new HashMap<>();
        notifications.put("orderAlerts", orderAlerts);
        notifications.put("lowStockAlerts", lowStockAlerts);
        notifications.put("deliveryUpdates", deliveryUpdates);
        notifications.put("promotionAlerts", promotionAlerts);
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

    /**
     * Full trader record for the Edit wizard. Unlike the list row this also returns the
     * linked user account, which carries the owner's name, email and mobile number.
     */
    public void getTraderById(String id, ApiCallback<TraderDetailData> cb) {
        Map<String, String> q = new HashMap<>();
        q.put("id", id == null ? "" : id);
        getAuthed("api/distributor/traders/get-by-id", q, TraderDetailData.class, cb);
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

    /**
     * Update an existing trader. Same multipart shape as create plus an {@code id};
     * documents and the logo are only re-sent when the user picked a new file.
     */
    public void updateTrader(Map<String, String> form, List<Http.FilePart> files,
                             ApiCallback<CreateTraderData> cb) {
        io.execute(() -> {
            Http.Result r = Http.postMultipart(BASE_URL, "api/distributor/traders/update",
                    form, files, session.getToken());
            deliver(r, CreateTraderData.class, cb);
        });
    }

    /**
     * Delete a trader. Like products/delete the response carries no trader object,
     * so only the status/message matter.
     */
    public void deleteTrader(String id, ApiCallback<String> cb) {
        Map<String, String> form = new HashMap<>();
        form.put("id", id == null ? "" : id);
        io.execute(() -> deliverMessageOnly(
                Http.postForm(BASE_URL, "api/distributor/traders/delete", form,
                        session.getToken()), cb));
    }

    // ---- Reports ---------------------------------------------------------

    /**
     * Reports dashboard: the catalogue of reports that can be generated, everything
     * already generated, and the KPI summary. Takes no parameters — the screen's search
     * and category filters run over the returned list.
     */
    public void getReports(ApiCallback<ReportsData> cb) {
        getAuthed("api/distributor/reports", null, ReportsData.class, cb);
    }

    /**
     * Build a report for a period — {@code report_key} comes from {@code available[]}.
     *
     * <p>The response echoes the new report and a refreshed summary, but the screen
     * reloads the whole list straight afterwards, so only the envelope's message
     * ("Stock Levels ready — 10 rows.") is worth carrying back.
     */
    public void generateReport(String reportKey, String from, String to, ApiCallback<String> cb) {
        Map<String, String> form = new HashMap<>();
        form.put("report_key", reportKey == null ? "" : reportKey);
        form.put("from", from == null ? "" : from);
        form.put("to", to == null ? "" : to);
        io.execute(() -> deliverMessageOnly(
                Http.postForm(BASE_URL, "api/distributor/reports/generate", form,
                        session.getToken()), cb));
    }

    /** Delete a generated report. The response only carries the refreshed summary. */
    public void deleteReport(String id, ApiCallback<String> cb) {
        Map<String, String> form = new HashMap<>();
        form.put("id", id == null ? "" : id);
        io.execute(() -> deliverMessageOnly(
                Http.postForm(BASE_URL, "api/distributor/reports/delete", form,
                        session.getToken()), cb));
    }

    /**
     * Fetch a generated report's file.
     *
     * <p>Downloads are served by a separate host and port from the JSON API, and that
     * service authenticates from the request body rather than a Bearer header — so this
     * is the one call that does not go through {@link #BASE_URL}.
     */
    public void downloadReport(String id, ApiCallback<Http.Download> cb) {
        String json = "{\"id\":" + safeNumber(id) + ",\"token\":\""
                + escapeJson(session.getToken()) + "\"}";
        io.execute(() -> {
            Http.Download download = Http.postJsonForFile(
                    DOWNLOAD_URL, json, null);
            main.post(() -> {
                if (download.ok && !looksLikeJson(download)) {
                    cb.onSuccess(download);
                } else {
                    cb.onError(downloadError(download));
                }
            });
        });
    }

    /** A JSON body from the download service means an error, not a report. */
    private boolean looksLikeJson(Http.Download download) {
        return download.mimeType != null
                && download.mimeType.toLowerCase(Locale.ROOT).contains("json");
    }

    /** Surface the download service's own message when it sent one. */
    private String downloadError(Http.Download download) {
        if (download.content != null && download.content.length > 0 && looksLikeJson(download)) {
            try {
                ApiResponse<?> resp = gson.fromJson(new String(download.content, "UTF-8"),
                        ApiResponse.class);
                if (resp != null && resp.message != null && !resp.message.isEmpty()) {
                    return resp.message;
                }
            } catch (Exception ignored) {
                // fall through to the generic message
            }
        }
        return "Could not download the report (HTTP " + download.code + ")";
    }

    private static String safeNumber(String id) {
        if (id == null || !id.trim().matches("\\d+")) {
            return "0";
        }
        return id.trim();
    }

    private static String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
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

    /** Soft-deletes a store (is_closed=1) — api/distributor/stores/delete. */
    public void deleteStore(String id, ApiCallback<StoreDetailData> cb) {
        Map<String, String> form = new HashMap<>();
        form.put("id", id == null ? "" : id);
        io.execute(() -> {
            Http.Result r = Http.postForm(BASE_URL, "api/distributor/stores/delete",
                    form, session.getToken());
            // Like products/delete, the response carries no store object — status only.
            deliverStatusOnly(r, cb);
        });
    }

    // ---- Airtime & VAS (Limes, common) ----------------------------------

    /**
     * VAS dashboard: recent transactions, KPI summary (spend/count today, commission this
     * month, wallet balance) and pagination (api/common/vas/transactions).
     */
    public void getVasTransactions(int page, int perPage, ApiCallback<VasTransactionsData> cb) {
        Map<String, String> q = new HashMap<>();
        q.put("page", String.valueOf(page));
        q.put("per_page", String.valueOf(perPage));
        getAuthed("api/common/vas/transactions", q, VasTransactionsData.class, cb);
    }

    /** The Limes VAS category tree (api/common/vas/categories). Flatten via {@code leaves()}. */
    public void getVasCategories(ApiCallback<VasCategoriesData> cb) {
        getAuthed("api/common/vas/categories", null, VasCategoriesData.class, cb);
    }

    /** Purchasable products for one leaf category (api/common/vas/products). */
    public void getVasProducts(String category, int page, int limit,
                               ApiCallback<VasProductsData> cb) {
        Map<String, String> q = new HashMap<>();
        q.put("category", category == null ? "" : category);
        q.put("page", String.valueOf(page));
        q.put("limit", String.valueOf(limit));
        getAuthed("api/common/vas/products", q, VasProductsData.class, cb);
    }

    /**
     * Buy a VAS product (api/common/vas/purchase). Paid from the anokiX wallet; the
     * server debits and forwards to Limes. Only status/message decide success — the
     * failure path carries the network's reason (e.g. inactive subscriber).
     */
    public void purchaseVas(String productId, String msisdn, double amount, String sku,
                            String name, String category, ApiCallback<Void> cb) {
        Map<String, String> form = new HashMap<>();
        form.put("product_id", productId == null ? "" : productId);
        form.put("msisdn", msisdn == null ? "" : msisdn);
        form.put("amount", String.valueOf(amount));
        form.put("sku", sku == null ? "" : sku);
        form.put("name", name == null ? "" : name);
        form.put("category", category == null ? "" : category);
        // Postman collection sends earn_commission=1 so the distributor accrues their VAS commission.
        form.put("earn_commission", "1");
        io.execute(() -> deliverStatusOnly(
                Http.postForm(BASE_URL, "api/common/vas/purchase", form, session.getToken()), cb));
    }

    /**
     * Register an end-customer in the Limes CRM (api/common/vas/onboard-customer). Step 1 of
     * signing someone up to a SIM subscription; upserts by ID number, so a retry reports
     * {@code already_exists} rather than creating a duplicate.
     */
    public void onboardVasCustomer(Map<String, String> form, ApiCallback<VasOnboardCustomerData> cb) {
        postAuthed("api/common/vas/onboard-customer", form, VasOnboardCustomerData.class, cb);
    }

    /**
     * Build — but do not send — the JSON body an onboard would POST to Limes
     * (api/common/vas/onboard-customer/preview). Runs no validation, so a half-filled form
     * still previews.
     */
    public void previewVasOnboard(Map<String, String> form, ApiCallback<VasOnboardPreviewData> cb) {
        postAuthed("api/common/vas/onboard-customer/preview", form, VasOnboardPreviewData.class, cb);
    }

    /** End-customers this distributor has onboarded (api/common/vas/customers). */
    public void getVasCustomers(String search, int limit, ApiCallback<VasCustomersData> cb) {
        Map<String, String> q = new HashMap<>();
        q.put("search", search == null ? "" : search);
        q.put("limit", String.valueOf(limit));
        getAuthed("api/common/vas/customers", q, VasCustomersData.class, cb);
    }

    /** SIM subscription products offered by the "Assign a SIM" form (never hardcode these). */
    public void getVasSubscriptionProducts(ApiCallback<VasSubscriptionProductsData> cb) {
        getAuthed("api/common/vas/subscription-products", null, VasSubscriptionProductsData.class, cb);
    }

    /** SIMs already activated by this distributor, with the MSISDN Limes assigned to each. */
    public void getVasSubscriptions(ApiCallback<VasSubscriptionsData> cb) {
        getAuthed("api/common/vas/subscriptions", null, VasSubscriptionsData.class, cb);
    }

    /**
     * Activate a SIM / create a subscription (api/common/vas/subscribe). An eSIM carries no
     * ICCID, so {@code e_sim=1} is sent instead of the serial; a physical "SIM in hand"
     * sends its ICCID and activates immediately.
     */
    public void subscribeVas(String productId, String iccid, boolean eSim, ApiCallback<Void> cb) {
        Map<String, String> form = new HashMap<>();
        form.put("product_id", productId == null ? "" : productId);
        if (eSim) {
            form.put("e_sim", "1");
        } else {
            form.put("iccid", iccid == null ? "" : iccid);
        }
        io.execute(() -> deliverStatusOnly(
                Http.postForm(BASE_URL, "api/common/vas/subscribe", form, session.getToken()), cb));
    }

    /**
     * Provision custom/variable-value bundles straight onto a number
     * (api/common/vas/dynamic-services) — the path Limes recommends over order/create for
     * amounts the fixed catalog cannot express. Expects a JSON body, so the service lines go
     * through {@link Http#postJson}; the charge is the sum of {@code priceInCents}.
     *
     * Each entry of {@code services} carries {@code definitionCode}, {@code value},
     * {@code priceInCents} and an optional {@code expiryDate}; Gson serialises them so no
     * caller has to hand-build JSON.
     */
    public void provisionVasDynamicServices(String msisdn, List<Map<String, Object>> services,
                                            ApiCallback<Void> cb) {
        Map<String, Object> body = new HashMap<>();
        body.put("msisdn", msisdn == null ? "" : msisdn);
        // Postman collection sends earn_commission=1 so the distributor accrues their commission.
        body.put("earn_commission", 1);
        body.put("services", services == null ? new java.util.ArrayList<>() : services);

        final String json = gson.toJson(body);
        io.execute(() -> deliverStatusOnly(
                Http.postJson(BASE_URL, "api/common/vas/dynamic-services", json,
                        session.getToken()), cb));
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
    /**
     * Like {@link #deliverStatusOnly} but hands the envelope's {@code message} to the
     * callback — the inventory mutations echo what changed ("100 units moved.").
     */
    private void deliverMessageOnly(Http.Result r, ApiCallback<String> cb) {
        String error = null;
        String message = null;
        try {
            if (r.body == null || r.body.isEmpty()) {
                error = "Network error (HTTP " + r.code + ")";
            } else {
                ApiResponse<?> resp = gson.fromJson(r.body, ApiResponse.class);
                if (resp == null || !resp.status) {
                    error = (resp != null && resp.message != null) ? resp.message : "Request failed";
                } else {
                    message = resp.message;
                }
            }
        } catch (Exception e) {
            Log.e(Http.TAG, "parse failed: " + e, e);
            error = "Unexpected response from server";
        }

        final String errorMessage = error;
        final String successMessage = message;
        main.post(() -> {
            if (errorMessage == null) {
                cb.onSuccess(successMessage);
            } else {
                cb.onError(errorMessage);
            }
        });
    }

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
