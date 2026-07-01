package com.anokix.distributorapp.network.dto;

import java.util.List;

/**
 * data block of GET {@code api/distributor/business/profile} — the distributor's full business
 * record used to pre-fill the 7-step Settings → Business editor. Numeric fields that the API
 * sometimes returns as numbers and sometimes as strings are typed as String (Gson coerces).
 */
public class BusinessProfileData {
    public Profile profile;

    public static class Profile {
        public long id;
        public String distributor_code;
        public String company_legal_name;
        public String trading_name;
        public String company_logo_url;
        public String company_registration_number;
        public String vat_number;
        public String company_email;
        public String company_phone_number;

        public String business_type;
        public String business_established_year;
        public String employee_count;
        public String annual_turnover;

        public String bank_name;
        public String account_holder_name;
        public String account_number;
        public String account_type;
        public String branch_code;

        public List<Contact> contact_persons;

        public String address;
        public Double latitude;
        public Double longitude;

        public String distribution_coverage;
        public String primary_product_category_ids;
        public String active_clients_count;
        public String delivery_fleet_size;
        public String preferred_delivery_days;

        public Documents documents;
        public String status;
        public String registration_status;
    }

    public static class Contact {
        public String full_name;
        public String position;
        public String mobile_number;
        public String email_address;
    }

    public static class Documents {
        public List<DocFile> company_registration_certificate;
        public List<DocFile> proof_of_address;
        public List<DocFile> bank_confirmation_letter;
        public List<DocFile> tax_clearance_certificate;
        public List<DocFile> vat_registration_certificate;
    }

    public static class DocFile {
        public long id;
        public String file_url;
        public String original_name;
    }
}
