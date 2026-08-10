package com.anokix.distributorapp.network.dto;

/**
 * {@code data} block of {@code POST api/common/vas/onboard-customer}. The endpoint upserts
 * by ID number, so a retry reports {@code already_exists} rather than failing.
 */
public class VasOnboardCustomerData {
    public Customer customer;

    public static class Customer {
        public String id;
        public String user_id;
        public String status;
        public boolean already_exists;
        public String local_id;
    }
}
