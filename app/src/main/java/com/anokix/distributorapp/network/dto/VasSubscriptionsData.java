package com.anokix.distributorapp.network.dto;

import java.util.List;

/**
 * {@code data} block of {@code GET api/common/vas/subscriptions} — the SIMs this merchant
 * has activated. {@code msisdn} may be null: Limes allocates the number from a tenant pool
 * and reports it once, so a SIM activated while that pool was empty has none until a sync.
 */
public class VasSubscriptionsData {
    public List<Subscription> subscriptions;

    public static class Subscription {
        public String id;
        public String limes_subscription_id;
        public String product_id;
        public String iccid;
        public String msisdn;
        public String status;
        public String created_at;
    }
}
