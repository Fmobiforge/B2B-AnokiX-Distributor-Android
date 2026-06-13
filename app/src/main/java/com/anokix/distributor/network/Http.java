package com.anokix.distributor.network;

import android.net.Uri;
import android.util.Log;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Thin wrapper over a shared OkHttp client. All requests flow through {@link #execute},
 * which attaches the JSON Accept header and Bearer token. The logging interceptor prints
 * full requests/responses to Logcat under the "AxApi" tag, so calls are easy to follow.
 *
 * Callers get a plain {@link Result} (status code + raw body); no proxies or annotations.
 */
public final class Http {

    public static final String TAG = "AxApi";

    private static final OkHttpClient client = buildClient();

    private Http() {}

    private static OkHttpClient buildClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor(message -> Log.d(TAG, message));
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        return new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build();
    }

    /** Raw outcome of a call. {@link #ok} is true only for 2xx responses. */
    public static class Result {
        public int code;
        public String body;
        public boolean ok;

        Result(int code, String body) {
            this.code = code;
            this.body = body;
            this.ok = code >= 200 && code < 300;
        }
    }

    public static Result get(String baseUrl, String path, Map<String, String> query, String token) {
        Uri.Builder uri = Uri.parse(baseUrl + path).buildUpon();
        if (query != null) {
            for (Map.Entry<String, String> e : query.entrySet()) {
                uri.appendQueryParameter(e.getKey(), e.getValue() == null ? "" : e.getValue());
            }
        }
        Request.Builder request = new Request.Builder().url(uri.build().toString()).get();
        return execute(request, token);
    }

    public static Result postForm(String baseUrl, String path, Map<String, String> form, String token) {
        FormBody.Builder body = new FormBody.Builder();
        if (form != null) {
            for (Map.Entry<String, String> e : form.entrySet()) {
                body.add(e.getKey(), e.getValue() == null ? "" : e.getValue());
            }
        }
        Request.Builder request = new Request.Builder().url(baseUrl + path).post(body.build());
        return execute(request, token);
    }

    private static Result execute(Request.Builder request, String token) {
        request.header("Accept", "application/json");
        if (token != null && !token.isEmpty()) {
            request.header("Authorization", "Bearer " + token);
        }
        try (Response response = client.newCall(request.build()).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            return new Result(response.code(), body);
        } catch (Exception e) {
            Log.e(TAG, "request failed: " + e, e);
            return new Result(-1, null);
        }
    }
}
