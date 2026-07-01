package com.anokix.distributorapp.network.dto;

/**
 * data block of POST /api/distributor/traders/create. Only the created
 * trader's identity is modelled – enough to confirm success.
 */
public class CreateTraderData {
    public Trader trader;

    public static class Trader {
        public String id;
        public String business_name;
        public String trading_name;
        public String registration_status;
    }
}
