package com.anokix.distributorapp.network.dto;

import java.util.List;

/**
 * {@code data} block of {@code GET api/common/vas/subscription-products} — the SIM
 * subscription products the "Assign a SIM" form offers. {@code inHand} products take an
 * ICCID and activate immediately; the rest are "on order" and activate on delivery.
 */
public class VasSubscriptionProductsData {
    public boolean configured;
    public List<Product> products;

    public static class Product {
        public String id;
        public String name;
        public String sku;
        public String description;
        public boolean inHand;
        public String category;

        /** "Mobile Contract Package · SIM in hand" — what the product dropdown shows. */
        public String label() {
            String base = (name == null || name.trim().isEmpty()) ? (id == null ? "—" : id) : name.trim();
            return base + (inHand ? " · SIM in hand" : " · SIM on order");
        }
    }
}
