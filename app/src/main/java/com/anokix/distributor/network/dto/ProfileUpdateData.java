package com.anokix.distributor.network.dto;

/** data block of POST {@code api/common/profile/update} — the updated user record. */
public class ProfileUpdateData {
    public User user;

    public static class User {
        public String id;
        public String first_name;
        public String last_name;
        public String email;
        public String phone_number;
        public String avatar;
        public String status;
        public String role_id;
    }
}
