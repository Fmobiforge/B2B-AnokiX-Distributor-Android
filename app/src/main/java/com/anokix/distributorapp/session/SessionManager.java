package com.anokix.distributorapp.session;

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
    private static final String KEY_PHONE = "phone_number";
    private static final String KEY_ROLE_LABEL = "role_label";
    private static final String KEY_COMPANY_NAME = "company_name";
    private static final String KEY_COMPANY_LEGAL_NAME = "company_legal_name";
    private static final String KEY_COMPANY_LOGO_URL = "company_logo_url";
    private static final String KEY_FCM_TOKEN = "fcm_token";
    private static final String KEY_FCM_TOKEN_REGISTERED = "fcm_token_registered";

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

    /**
     * The account's mobile number. Kept apart from {@link #setUser} because login and the
     * profile editor learn it at different moments, and screens that pre-fill a form from the
     * signed-in operator (Airtime &amp; VAS › Onboard customer) need it on its own.
     */
    public void setPhone(String phone) {
        prefs.edit().putString(KEY_PHONE, phone == null ? "" : phone.trim()).apply();
    }

    public String getPhone() {
        return prefs.getString(KEY_PHONE, "");
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

    /** Stores the distributor company branding returned in the login {@code portal_info}. */
    public void setCompany(String companyName, String companyLogoUrl) {
        prefs.edit()
                .putString(KEY_COMPANY_NAME, companyName == null ? "" : companyName)
                .putString(KEY_COMPANY_LOGO_URL, companyLogoUrl == null ? "" : companyLogoUrl)
                .apply();
    }

    public String getCompanyName() {
        return prefs.getString(KEY_COMPANY_NAME, "");
    }

    /** Absolute URL of the distributor company logo, or "" when none is available. */
    public String getCompanyLogoUrl() {
        return prefs.getString(KEY_COMPANY_LOGO_URL, "");
    }

    /**
     * The registered entity name from {@code base-info}'s {@code portal_info}. Login only
     * returns {@code company_name}, so this is cached separately once base-info answers
     * and the dashboard can paint it immediately on the next launch.
     */
    public void setCompanyLegalName(String legalName) {
        prefs.edit().putString(KEY_COMPANY_LEGAL_NAME,
                legalName == null ? "" : legalName.trim()).apply();
    }

    public String getCompanyLegalName() {
        return prefs.getString(KEY_COMPANY_LEGAL_NAME, "");
    }

    /**
     * What to call the distributor on screen: the registered legal name when we know it,
     * otherwise the display name login returned.
     */
    public String getDistributorName() {
        String legal = getCompanyLegalName();
        return legal.isEmpty() ? getCompanyName() : legal;
    }

    // ---- FCM push token --------------------------------------------------

    /**
     * Caches the latest FCM token plus whether it has been registered with the
     * backend, so {@code syncToken} only POSTs when the token actually changed.
     */
    public void saveFcmToken(String token, boolean registered) {
        prefs.edit()
                .putString(KEY_FCM_TOKEN, token == null ? "" : token)
                .putBoolean(KEY_FCM_TOKEN_REGISTERED, registered)
                .apply();
    }

    public String getFcmToken() {
        return prefs.getString(KEY_FCM_TOKEN, "");
    }

    /** True when {@code token} is the cached token and it is already on the server. */
    public boolean isFcmTokenRegistered(String token) {
        if (token == null) return false;
        return token.equals(getFcmToken()) && prefs.getBoolean(KEY_FCM_TOKEN_REGISTERED, false);
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}
