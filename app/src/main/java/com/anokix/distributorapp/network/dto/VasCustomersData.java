package com.anokix.distributorapp.network.dto;

import java.util.List;

/** {@code data} block of {@code GET api/common/vas/customers} — end-customers onboarded into Limes. */
public class VasCustomersData {
    public List<Customer> customers;
    public int total;

    public static class Customer {
        public String id;
        public String limes_account_id;
        public String title;
        public String firstname;
        public String lastname;
        public String id_type;
        public String id_number;
        public String email;
        public String phone;
        public String city;
        public String status;
        public String created_at;

        /** "Mr Tam Ho" — title is optional, so it is only prefixed when present. */
        public String displayName() {
            StringBuilder sb = new StringBuilder();
            if (title != null && !title.trim().isEmpty()) sb.append(title.trim()).append(' ');
            if (firstname != null) sb.append(firstname.trim()).append(' ');
            if (lastname != null) sb.append(lastname.trim());
            String out = sb.toString().trim();
            return out.isEmpty() ? "—" : out;
        }
    }
}
