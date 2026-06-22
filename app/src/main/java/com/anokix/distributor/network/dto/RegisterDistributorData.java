package com.anokix.distributor.network.dto;

/**
 * data block of POST /api/distributor/register. Only the identity and status
 * fields needed to confirm the registration succeeded are modelled.
 */
public class RegisterDistributorData {
    public User user;
    public Distributor distributor;

    public static class User {
        public String id;
        public String first_name;
        public String last_name;
        public String email;
        public String phone_number;
        public String status;
    }

    public static class Distributor {
        public long id;
        public String distributor_code;
        public String company_legal_name;
        public String trading_name;
        public String status;
        public String registration_status;
    }
}
