package com.anokix.distributor.network.dto;

/**
 * data block of POST /api/common/login. Only the fields the app consumes are modelled.
 */
public class LoginData {
    public String token;
    public User user;
    public String portal_type;

    public static class User {
        public String id;
        public String first_name;
        public String last_name;
        public String email;
        public String phone_number;
        public String role_type;
        public String role_label;
    }
}
