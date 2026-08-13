package com.anokix.distributorapp.network;

import android.net.Uri;
import android.util.Log;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
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

    /** application/json POST — used by endpoints that expect a JSON body (e.g. preferences/update). */
    public static Result postJson(String baseUrl, String path, String json, String token) {
        RequestBody body = RequestBody.create(
                json == null ? "{}" : json, MediaType.parse("application/json; charset=utf-8"));
        Request.Builder request = new Request.Builder().url(baseUrl + path).post(body);
        return execute(request, token);
    }

    /** A downloaded file: raw bytes plus whatever the server said they are. */
    public static class Download {
        public final int code;
        public final byte[] content;
        public final String fileName;
        public final String mimeType;
        public final boolean ok;

        Download(int code, byte[] content, String fileName, String mimeType) {
            this.code = code;
            this.content = content;
            this.fileName = fileName;
            this.mimeType = mimeType;
            this.ok = code >= 200 && code < 300 && content != null && content.length > 0;
        }
    }

    /**
     * JSON POST whose response is a file rather than an envelope — the report download
     * service. The body is read as bytes, and the filename is taken from
     * Content-Disposition when the server offers one.
     *
     * <p>An error here still arrives as JSON, so a JSON content-type is reported back and
     * the caller can surface the server's message instead of saving a broken file.
     */
    public static Download postJsonForFile(String url, String json, String token) {
        RequestBody body = RequestBody.create(
                json == null ? "{}" : json, MediaType.parse("application/json; charset=utf-8"));
        Request.Builder request = new Request.Builder().url(url).post(body);
        request.header("Accept", "*/*");
        if (token != null && !token.isEmpty()) {
            request.header("Authorization", "Bearer " + token);
        }
        try (Response response = client.newCall(request.build()).execute()) {
            byte[] content = response.body() != null ? response.body().bytes() : null;
            String mimeType = response.header("Content-Type");
            return new Download(response.code(), content,
                    fileNameFrom(response.header("Content-Disposition")), mimeType);
        } catch (Exception e) {
            Log.e(TAG, "download failed: " + e, e);
            return new Download(-1, null, null, null);
        }
    }

    /** Pulls {@code filename="stock_levels-16.csv"} out of a Content-Disposition header. */
    private static String fileNameFrom(String contentDisposition) {
        if (contentDisposition == null) {
            return null;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("filename\\*?=(?:UTF-8'')?\"?([^\";]+)\"?", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(contentDisposition);
        if (!matcher.find()) {
            return null;
        }
        String name = matcher.group(1);
        if (name == null) {
            return null;
        }
        // Never let the server's name escape the directory we chose to write into.
        name = name.trim().replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        return name.isEmpty() ? null : name;
    }

    /** A single file part for a multipart upload (content already read into memory). */
    public static class FilePart {
        public final String field;
        public final String fileName;
        public final String mimeType;
        public final byte[] content;

        public FilePart(String field, String fileName, String mimeType, byte[] content) {
            this.field = field;
            this.fileName = fileName;
            this.mimeType = mimeType;
            this.content = content;
        }
    }

    /** multipart/form-data POST with text fields and optional file parts. */
    public static Result postMultipart(String baseUrl, String path, Map<String, String> form,
                                       List<FilePart> files, String token) {
        MultipartBody.Builder body = new MultipartBody.Builder().setType(MultipartBody.FORM);
        if (form != null) {
            for (Map.Entry<String, String> e : form.entrySet()) {
                body.addFormDataPart(e.getKey(), e.getValue() == null ? "" : e.getValue());
            }
        }
        if (files != null) {
            for (FilePart part : files) {
                if (part == null || part.content == null) {
                    continue;
                }
                MediaType type = MediaType.parse(
                        part.mimeType != null ? part.mimeType : "application/octet-stream");
                String name = part.fileName != null ? part.fileName : part.field;
                body.addFormDataPart(part.field, name, RequestBody.create(part.content, type));
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
