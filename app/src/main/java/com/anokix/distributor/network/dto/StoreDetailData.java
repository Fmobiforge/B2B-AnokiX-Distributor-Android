package com.anokix.distributor.network.dto;

/**
 * data block of GET /api/distributor/stores?id= and the create/update responses.
 * Holds the full store record used to prefill the edit form.
 */
public class StoreDetailData {
    public Store store;

    public static class Store {
        public long id;
        public String name;
        public String address;
        public String street;
        public String tag_list;
        public String contact_number;
        public String description;
        public String building_number;
        public String vat_number;
        public String address_street1;
        public String address_street2;
        public String address_suburb;
        public String address_city;
        public String address_state;
        public String address_country;
        public String address_postal_code;
        public Double latitude;
        public Double longitude;
        public boolean is_click_collect;
        public boolean is_click_deliver;
        public String working_days;
        public String trading_time;
        public String time_zone;
        public long time_offset;
        public String currency_id;
        public int follower_count;
        public int review_count;
    }
}
