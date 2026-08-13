package com.anokix.distributorapp.network.dto;

import java.util.List;

/**
 * data block of GET /api/distributor/staff — the warehouse staff who sign in with their
 * own credentials to pick, pack and handle stock.
 */
public class StaffData {
    public List<Staff> staff;
    public int total;
    /** What the role is allowed to do server-side, e.g. ["fulfil", "stock"]. */
    public List<String> staff_capabilities;

    public static class Staff {
        public long id;
        public long user_id;
        public String name;
        public String first_name;
        public String last_name;
        public String email;
        public String phone;
        public String job_title;
        /** "active" / "disabled". */
        public String status;
        public String role_label;
        public String created_at;
        public boolean can_sign_in;

        public boolean isActive() {
            return "active".equalsIgnoreCase(status);
        }

        /** Falls back to the two name parts when the joined name is missing. */
        public String displayName() {
            if (name != null && !name.trim().isEmpty()) {
                return name;
            }
            String first = first_name == null ? "" : first_name;
            String last = last_name == null ? "" : last_name;
            return (first + " " + last).trim();
        }
    }
}
