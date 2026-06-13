package com.anokix.distributor.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.anokix.distributor.network.dto.BaseInfoData;
import com.anokix.distributor.network.dto.DashboardData;
import com.anokix.distributor.network.dto.InventorySummaryData;
import com.anokix.distributor.network.dto.LoginData;
import com.anokix.distributor.network.dto.ProductsData;
import com.anokix.distributor.network.dto.StoreDetailData;
import com.anokix.distributor.network.dto.StoresData;
import com.anokix.distributor.network.dto.TradersData;
import com.anokix.distributor.session.SessionManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
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
        io.execute(() -> {
            // login itself needs no token
            Http.Result r = Http.postForm(BASE_URL, "api/common/login", form, null);
            deliver(r, LoginData.class, cb);
        });
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
        Map<String, String> q = new HashMap<>();
        q.put("search", search == null ? "" : search);
        q.put("status", "");
        q.put("registration_status", "");
        q.put("credit_status", "");
        getAuthed("api/distributor/traders", q, TradersData.class, cb);
    }

    public void getProducts(ApiCallback<ProductsData> cb) {
        getAuthed("api/distributor/products", null, ProductsData.class, cb);
    }

    public void getInventorySummary(ApiCallback<InventorySummaryData> cb) {
        getAuthed("api/distributor/inventory/summary", null, InventorySummaryData.class, cb);
    }

    public void getBaseInfo(ApiCallback<BaseInfoData> cb) {
        getAuthed("api/common/base-info", null, BaseInfoData.class, cb);
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
}
