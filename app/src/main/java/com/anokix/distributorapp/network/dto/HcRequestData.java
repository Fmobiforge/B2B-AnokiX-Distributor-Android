package com.anokix.distributorapp.network.dto;

/**
 * data block of POST /api/distributor/products/external-request — a request asking
 * Hirt &amp; Carter to add a product the catalogue does not have yet.
 */
public class HcRequestData {
    /** True when this GTIN had already been requested; the existing request is returned. */
    public Boolean already_requested;
    public Request request;

    public static class Request {
        public String id;
        public String gtin;
        public String status;
        public String created_at;   // "yyyy-MM-dd HH:mm:ss"
    }
}
