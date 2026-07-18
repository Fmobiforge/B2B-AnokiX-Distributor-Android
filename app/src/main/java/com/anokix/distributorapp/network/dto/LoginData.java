package com.anokix.distributorapp.network.dto;

/**
 * data block of POST /api/common/login. Only the fields the app consumes are modelled.
 */
public class LoginData {
    public String token;
    public User user;
    public String portal_type;
    public PortalInfo portal_info;

    // ---- Pending-approval branch -------------------------------------------
    // When the distributor account is still awaiting approval the login call
    // returns status=true but with no token; instead these fields describe the
    // application under review (see PendingApprovalActivity).
    public boolean pending_approval;
    public String account_status;
    public String registration_status;
    public String submitted_at;
    public String reference;
    public String business_name;
    public Wallet wallet;

    public static class Wallet {
        public String status;
        public String account_number;
        public String activated_at;
        public String last_error;
        public boolean configured;
    }

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
