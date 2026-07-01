package com.anokix.distributorapp.network.dto;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * data block of GET {@code api/common/preferences} — the user's portal preferences plus the
 * notification toggles. {@code notifications} can arrive as an object or, when never set, an
 * empty array; it is kept as a raw {@link JsonElement} so either shape parses, and the
 * accessor defaults every toggle to {@code true} when absent.
 */
public class PreferencesData {
    public Preferences preferences;

    public static class Preferences {
        public String language;
        public String currency;
        public String timezone;
        public String date_format;
        public JsonElement notifications;

        /** Toggle value for {@code key}, defaulting to {@code true} when notifications are unset. */
        public boolean notif(String key) {
            if (notifications != null && notifications.isJsonObject()) {
                JsonObject obj = notifications.getAsJsonObject();
                if (obj.has(key) && !obj.get(key).isJsonNull()) {
                    try {
                        return obj.get(key).getAsBoolean();
                    } catch (Exception ignored) {
                        return true;
                    }
                }
            }
            return true;
        }
    }
}
