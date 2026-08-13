package com.anokix.distributorapp.network.dto;

/**
 * data block of GET /api/distributor/traders/get-by-id.
 *
 * <p>The trader block matches the list row, but this call also returns the linked
 * {@code user} account — the owner's name, email and mobile number, which the list
 * response leaves out. The Edit wizard needs both to fill in step 1.
 */
public class TraderDetailData {
    public TradersData.Trader trader;
    public User user;

    /** The owner's login account (role_id 4 = trader). */
    public static class User {
        public String id;
        public String first_name;
        public String last_name;
        public String email;
        public String phone_number;
        public String avatar;
        public String status;
    }
}
