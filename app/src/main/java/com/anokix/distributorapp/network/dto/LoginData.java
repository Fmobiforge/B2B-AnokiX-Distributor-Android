package com.anokix.distributorapp.network.dto;

/**
 * data block of POST /api/common/login. Only the fields the app consumes are modelled.
 */
public class LoginData {
    public String token;
    public User user;
    public String portal_type;
    public PortalInfo portal_info;

    public static class User {
        public String id;
        public String first_name;
        public String last_name;
        public String email;
        public String phone_number;
        public String role_type;
        public String role_label;
    }

    /**
     * Branding for the logged-in distributor company. {@code company_logo_url} is an
     * absolute URL to the uploaded company logo (may be null/empty when none was set).
     */
    public static class PortalInfo {
        public String company_name;
        public String company_logo;
        public String company_logo_url;
    }
}
