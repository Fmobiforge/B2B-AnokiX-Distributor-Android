package com.anokix.distributorapp.network.dto;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import java.util.List;

/**
 * {@code data} block of {@code POST api/common/vas/onboard-customer/preview} — the exact
 * request the real onboard would send to Limes, built by the same server-side code but not
 * dispatched. Used by the "View request body" action so an operator can sanity-check a
 * half-filled form before creating anything.
 */
public class VasOnboardPreviewData {
    public String method;
    public String endpoint;
    public String base_url;
    public List<String> headers;
    public String token_subject;
    public JsonElement body;

    /** The whole preview rendered as the text the dialog shows. */
    public String render() {
        StringBuilder sb = new StringBuilder();
        sb.append(method == null ? "POST" : method).append(' ')
                .append(base_url == null ? "" : base_url)
                .append(endpoint == null ? "" : endpoint).append("\n");
        if (headers != null) {
            for (String h : headers) sb.append(h).append('\n');
        }
        sb.append('\n');
        sb.append(body == null ? "{}" : new GsonBuilder().setPrettyPrinting().create().toJson(body));
        return sb.toString();
    }
}
