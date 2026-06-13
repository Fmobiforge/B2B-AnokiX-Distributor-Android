package com.anokix.distributor.session;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Stores the auth token and basic user profile in SharedPreferences so the
 * Bearer token survives across screens and app restarts.
 */
public class SessionManager {

    private static final String PREFS = "ax_session";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_FIRST_NAME = "first_name";
    private static final String KEY_LAST_NAME = "last_name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_ROLE_LABEL = "role_label";

    private static SessionManager instance;

    private final SharedPreferences prefs;

    private SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static synchronized SessionManager get(Context context) {
        if (instance == null) {
            instance = new SessionManager(context);
        }
        return instance;
    }

    public void setToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, "");
    }

    public boolean isLoggedIn() {
        return !getToken().isEmpty();
    }

    public void setUser(String firstName, String lastName, String email, String roleLabel) {
        prefs.edit()
                .putString(KEY_FIRST_NAME, firstName)
                .putString(KEY_LAST_NAME, lastName)
                .putString(KEY_EMAIL, email)
                .putString(KEY_ROLE_LABEL, roleLabel)
                .apply();
    }

    public String getFirstName() {
        return prefs.getString(KEY_FIRST_NAME, "");
    }

    public String getLastName() {
        return prefs.getString(KEY_LAST_NAME, "");
    }

    public String getFullName() {
        String name = (getFirstName() + " " + getLastName()).trim();
        return name.isEmpty() ? "" : name;
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }

    public String getRoleLabel() {
        return prefs.getString(KEY_ROLE_LABEL, "");
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}
